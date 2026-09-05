import { useEffect, useState } from 'react';
import { Card, Modal, Spin, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { listarHistorialCajas, obtenerResumenCaja } from '../api/caja';
import { mensajeDeError } from '../api/client';
import type { CajaHistorial, CajaResumen } from '../types';
import { MEDIOS_PAGO } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

function etiquetaMedioPago(medioPago: string) {
  return MEDIOS_PAGO.find((m) => m.value === medioPago)?.label ?? medioPago;
}

export function HistorialCajasPage() {
  const [cajas, setCajas] = useState<CajaHistorial[]>([]);
  const [cargando, setCargando] = useState(true);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [resumen, setResumen] = useState<CajaResumen | null>(null);
  const [cargandoResumen, setCargandoResumen] = useState(false);

  useEffect(() => {
    listarHistorialCajas()
      .then(setCajas)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo cargar el historial de cajas')))
      .finally(() => setCargando(false));
  }, []);

  async function verResumen(id: number) {
    setResumen(null);
    setModalAbierto(true);
    setCargandoResumen(true);
    try {
      const data = await obtenerResumenCaja(id);
      setResumen(data);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo cargar el resumen del turno'));
      setModalAbierto(false);
    } finally {
      setCargandoResumen(false);
    }
  }

  const columnas: ColumnsType<CajaHistorial> = [
    {
      title: 'Apertura',
      dataIndex: 'fechaApertura',
      render: (fecha: string) => new Date(fecha).toLocaleString('es-AR'),
    },
    {
      title: 'Cierre',
      dataIndex: 'fechaCierre',
      render: (fecha: string | null) => (fecha ? new Date(fecha).toLocaleString('es-AR') : '—'),
    },
    { title: 'Usuario', dataIndex: 'usuarioNombre' },
    {
      title: 'Monto inicial',
      dataIndex: 'montoInicial',
      render: (v: number) => formatoMoneda.format(v),
    },
    {
      title: 'Monto final',
      dataIndex: 'montoFinal',
      render: (v: number | null) => (v !== null ? formatoMoneda.format(v) : '—'),
    },
    {
      title: 'Estado',
      dataIndex: 'estado',
      render: (estado: string) => (
        <Tag color={estado === 'ABIERTA' ? 'blue' : 'default'}>{estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'}</Tag>
      ),
    },
    {
      title: '',
      width: 120,
      render: (_, caja) => (
        <Typography.Link onClick={() => verResumen(caja.id)}>Ver resumen</Typography.Link>
      ),
    },
  ];

  const columnasMedioPago: ColumnsType<CajaResumen['ventasPorMedioPago'][number]> = [
    { title: 'Medio de pago', dataIndex: 'medioPago', render: etiquetaMedioPago },
    { title: 'Cantidad de ventas', dataIndex: 'cantidad' },
    { title: 'Total', dataIndex: 'total', render: (v: number) => formatoMoneda.format(v) },
  ];

  const columnasVentas: ColumnsType<CajaResumen['ventas'][number]> = [
    { title: 'Hora', dataIndex: 'fecha', render: (f: string) => new Date(f).toLocaleString('es-AR') },
    { title: 'Vendedor', dataIndex: 'usuarioNombre' },
    { title: 'Medio de pago', dataIndex: 'medioPago', render: etiquetaMedioPago },
    { title: 'Cliente', dataIndex: 'clienteNombre', render: (v: string | null) => v ?? '—' },
    { title: 'Total', dataIndex: 'total', render: (v: number) => formatoMoneda.format(v) },
  ];

  const columnasCobros: ColumnsType<CajaResumen['cobros'][number]> = [
    { title: 'Hora', dataIndex: 'fecha', render: (f: string) => new Date(f).toLocaleString('es-AR') },
    { title: 'Cobrado por', dataIndex: 'usuarioNombre' },
    { title: 'Cliente', dataIndex: 'clienteNombre' },
    { title: 'Medio de pago', dataIndex: 'medioPago', render: etiquetaMedioPago },
    { title: 'Monto', dataIndex: 'monto', render: (v: number) => formatoMoneda.format(v) },
  ];

  return (
    <AppLayout>
      <Card title="Historial de cajas">
        <Table
          columns={columnas}
          dataSource={cajas}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: 'Todavía no hay turnos registrados' }}
        />
      </Card>

      <Modal
        title={resumen ? `Resumen del turno #${resumen.id}` : 'Resumen del turno'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        footer={null}
        width={760}
      >
        {cargandoResumen && (
          <div style={{ textAlign: 'center', padding: 32 }}>
            <Spin />
          </div>
        )}

        {!cargandoResumen && resumen && (
          <>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
              {resumen.usuarioNombre} — abierto {new Date(resumen.fechaApertura).toLocaleString('es-AR')}
              {resumen.fechaCierre && ` — cerrado ${new Date(resumen.fechaCierre).toLocaleString('es-AR')}`}
            </Typography.Paragraph>

            <Typography.Title level={5}>Ventas del turno</Typography.Title>
            <Table
              columns={columnasVentas}
              dataSource={resumen.ventas}
              rowKey="id"
              pagination={false}
              size="small"
              scroll={{ y: 240 }}
              locale={{ emptyText: 'Sin ventas en este turno' }}
              style={{ marginBottom: 16 }}
            />

            {resumen.cobros.length > 0 && (
              <>
                <Typography.Title level={5}>Cobros de cuenta corriente</Typography.Title>
                <Table
                  columns={columnasCobros}
                  dataSource={resumen.cobros}
                  rowKey="id"
                  pagination={false}
                  size="small"
                  scroll={{ y: 240 }}
                  style={{ marginBottom: 16 }}
                />
              </>
            )}

            <Typography.Title level={5}>Resumen por medio de pago</Typography.Title>
            <Table
              columns={columnasMedioPago}
              dataSource={resumen.ventasPorMedioPago}
              rowKey="medioPago"
              pagination={false}
              size="small"
              locale={{ emptyText: 'Sin ventas en este turno' }}
              style={{ marginBottom: 16 }}
            />

            <Typography.Paragraph>
              <strong>Total vendido:</strong> {formatoMoneda.format(resumen.totalVentas)}
              <br />
              <strong>Total cobrado (cuenta corriente):</strong> {formatoMoneda.format(resumen.totalCobros)}
              <br />
              <strong>Ingresos manuales:</strong> {formatoMoneda.format(resumen.totalIngresos)}
              <br />
              <strong>Egresos manuales:</strong> {formatoMoneda.format(resumen.totalEgresos)}
              <br />
              <strong>Monto inicial:</strong> {formatoMoneda.format(resumen.montoInicial)}
            </Typography.Paragraph>

            <Typography.Paragraph>
              <strong>Efectivo esperado en caja:</strong> {formatoMoneda.format(resumen.efectivoEsperado)}
              <br />
              <strong>Monto final contado:</strong>{' '}
              {resumen.montoFinal !== null ? formatoMoneda.format(resumen.montoFinal) : '— (turno todavía abierto)'}
            </Typography.Paragraph>

            {resumen.diferencia !== null && (
              <Typography.Paragraph>
                <strong>Diferencia:</strong>{' '}
                <Tag color={Math.abs(resumen.diferencia) < 0.01 ? 'green' : 'red'}>
                  {formatoMoneda.format(resumen.diferencia)}
                </Tag>
              </Typography.Paragraph>
            )}
          </>
        )}
      </Modal>
    </AppLayout>
  );
}
