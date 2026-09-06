import { useEffect, useMemo, useState } from 'react';
import { Card, DatePicker, Tabs, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { AppLayout } from '../layout/AppLayout';
import { obtenerComisionesProduccion, obtenerComisionesVendedores } from '../api/comisiones';
import { mensajeDeError } from '../api/client';
import type { ComisionProduccion, ComisionVendedor } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const FORMATO_FECHA = 'YYYY-MM-DD';

export function ComisionesPage() {
  const [rango, setRango] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()]);
  const [comisionesVendedores, setComisionesVendedores] = useState<ComisionVendedor[]>([]);
  const [comisionesProduccion, setComisionesProduccion] = useState<ComisionProduccion[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const [desde, hasta] = rango;
    setCargando(true);

    Promise.all([
      obtenerComisionesVendedores(desde.format(FORMATO_FECHA), hasta.format(FORMATO_FECHA)),
      obtenerComisionesProduccion(desde.format(FORMATO_FECHA), hasta.format(FORMATO_FECHA)),
    ])
      .then(([vendedores, produccion]) => {
        setComisionesVendedores(vendedores);
        setComisionesProduccion(produccion);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar las comisiones')))
      .finally(() => setCargando(false));
  }, [rango]);

  const comisionPorVendedor = useMemo(() => {
    const totales = new Map<string, number>();
    for (const fila of comisionesVendedores) {
      totales.set(fila.usuarioNombre, (totales.get(fila.usuarioNombre) ?? 0) + fila.comision);
    }
    return Array.from(totales, ([usuarioNombre, comision]) => ({ usuarioNombre, comision }));
  }, [comisionesVendedores]);

  const columnasVendedores: ColumnsType<ComisionVendedor> = [
    { title: 'Turno (caja)', dataIndex: 'cajaId' },
    { title: 'Vendedor', dataIndex: 'usuarioNombre' },
    {
      title: 'Total vendido en el turno',
      dataIndex: 'totalVendido',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Cobros de cuenta corriente',
      dataIndex: 'totalCobrado',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Porcentaje',
      dataIndex: 'porcentaje',
      render: (valor: number | null) => (valor === null ? 'Sin asignar' : `${valor}%`),
    },
    {
      title: 'Comisión',
      dataIndex: 'comision',
      render: (valor: number) => formatoMoneda.format(valor),
    },
  ];

  const columnasProduccion: ColumnsType<ComisionProduccion> = [
    { title: 'Orden', dataIndex: 'ordenId' },
    { title: 'Empleado', dataIndex: 'usuarioNombre' },
    { title: 'Producto', dataIndex: 'productoNombre' },
    { title: 'Cantidad producida', dataIndex: 'cantidadProducida' },
    {
      title: 'Precio del producto',
      dataIndex: 'precioProducto',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Porcentaje',
      dataIndex: 'porcentaje',
      render: (valor: number | null) => (valor === null ? 'Sin asignar' : `${valor}%`),
    },
    {
      title: 'Comisión',
      dataIndex: 'comision',
      render: (valor: number) => formatoMoneda.format(valor),
    },
  ];

  return (
    <AppLayout>
      <Card
        title="Comisiones y liquidaciones"
        extra={
          <DatePicker.RangePicker
            value={rango}
            format="DD/MM/YYYY"
            allowClear={false}
            onChange={(valores) => {
              if (valores && valores[0] && valores[1]) {
                setRango([valores[0], valores[1]]);
              }
            }}
          />
        }
      >
        <Tabs
          items={[
            {
              key: 'vendedores',
              label: 'Vendedores (por turno)',
              children: (
                <>
                  <Typography.Title level={5}>Comisión total por vendedor</Typography.Title>
                  {comisionPorVendedor.length === 0 ? (
                    <Typography.Text type="secondary">Sin comisiones en este período.</Typography.Text>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <BarChart data={comisionPorVendedor}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="usuarioNombre" />
                        <YAxis tickFormatter={(v: number) => formatoMoneda.format(v)} width={90} />
                        <Tooltip formatter={(v: unknown) => formatoMoneda.format(Number(v))} />
                        <Bar dataKey="comision" name="Comisión" fill="#1677ff" />
                      </BarChart>
                    </ResponsiveContainer>
                  )}

                  <Table
                    columns={columnasVendedores}
                    dataSource={comisionesVendedores}
                    rowKey={(row) => `${row.cajaId}-${row.usuarioId}`}
                    loading={cargando}
                    pagination={false}
                    locale={{ emptyText: 'Sin ventas ni cobros con caja asignada en este período' }}
                    style={{ marginTop: 16 }}
                  />
                </>
              ),
            },
            {
              key: 'produccion',
              label: 'Producción',
              children: (
                <Table
                  columns={columnasProduccion}
                  dataSource={comisionesProduccion}
                  rowKey="ordenId"
                  loading={cargando}
                  pagination={false}
                  locale={{ emptyText: 'Sin órdenes de producción en este período' }}
                />
              ),
            },
          ]}
        />
      </Card>
    </AppLayout>
  );
}
