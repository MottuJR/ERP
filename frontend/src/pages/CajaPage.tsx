import { useEffect, useState } from 'react';
import { Alert, Button, Card, Empty, Flex, Form, InputNumber, Input, Modal, Select, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import {
  abrirCaja,
  cerrarCaja,
  listarMovimientos,
  obtenerCajaActual,
  registrarMovimiento,
} from '../api/caja';
import { mensajeDeError } from '../api/client';
import type { Caja, MovimientoCaja } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function CajaPage() {
  const [caja, setCaja] = useState<Caja | null>(null);
  const [movimientos, setMovimientos] = useState<MovimientoCaja[]>([]);
  const [cargando, setCargando] = useState(true);

  const [abrirAbierto, setAbrirAbierto] = useState(false);
  const [montoInicial, setMontoInicial] = useState<number | null>(0);
  const [abriendo, setAbriendo] = useState(false);

  const [cerrarAbierto, setCerrarAbierto] = useState(false);
  const [montoFinal, setMontoFinal] = useState<number | null>(0);
  const [cerrando, setCerrando] = useState(false);

  const [movimientoAbierto, setMovimientoAbierto] = useState(false);
  const [form] = Form.useForm<{ tipo: 'INGRESO' | 'EGRESO'; monto: number; concepto: string }>();
  const [guardandoMovimiento, setGuardandoMovimiento] = useState(false);

  function cargar() {
    setCargando(true);
    obtenerCajaActual()
      .then((actual) => {
        setCaja(actual);
        if (actual) {
          return listarMovimientos(actual.id).then(setMovimientos);
        }
        setMovimientos([]);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo cargar la caja')))
      .finally(() => setCargando(false));
  }

  useEffect(cargar, []);

  async function handleAbrir() {
    setAbriendo(true);
    try {
      await abrirCaja(montoInicial ?? 0);
      message.success('Caja abierta');
      setAbrirAbierto(false);
      setMontoInicial(0);
      cargar();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo abrir la caja'));
    } finally {
      setAbriendo(false);
    }
  }

  async function handleCerrar() {
    if (!caja) return;
    setCerrando(true);
    try {
      await cerrarCaja(caja.id, montoFinal ?? 0);
      message.success('Caja cerrada');
      setCerrarAbierto(false);
      setMontoFinal(0);
      cargar();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo cerrar la caja'));
    } finally {
      setCerrando(false);
    }
  }

  async function handleRegistrarMovimiento() {
    if (!caja) return;
    const values = await form.validateFields();
    setGuardandoMovimiento(true);
    try {
      await registrarMovimiento(caja.id, values);
      message.success('Movimiento registrado');
      setMovimientoAbierto(false);
      form.resetFields();
      listarMovimientos(caja.id).then(setMovimientos);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo registrar el movimiento'));
    } finally {
      setGuardandoMovimiento(false);
    }
  }

  const columnas: ColumnsType<MovimientoCaja> = [
    {
      title: 'Fecha',
      dataIndex: 'fecha',
      render: (fecha: string) => new Date(fecha).toLocaleString('es-AR'),
    },
    {
      title: 'Tipo',
      dataIndex: 'tipo',
      render: (tipo: string) => <Tag color={tipo === 'INGRESO' ? 'green' : 'red'}>{tipo}</Tag>,
    },
    {
      title: 'Monto',
      dataIndex: 'monto',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    { title: 'Concepto', dataIndex: 'concepto' },
  ];

  return (
    <AppLayout>
      <Card title="Caja" loading={cargando}>
        {!caja ? (
          <>
            <Alert
              type="warning"
              title="No hay ninguna caja abierta"
              description="Hay que abrir una caja antes de poder vender."
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Button type="primary" onClick={() => setAbrirAbierto(true)}>
              Abrir caja
            </Button>
          </>
        ) : (
          <>
            <Flex justify="space-between" align="flex-start" style={{ marginBottom: 24 }}>
              <div>
                <Typography.Text type="secondary">
                  Turno #{caja.id} — abierta desde {new Date(caja.fechaApertura).toLocaleString('es-AR')}
                </Typography.Text>
                <Typography.Title level={4} style={{ margin: '4px 0 0' }}>
                  Monto inicial: {formatoMoneda.format(caja.montoInicial)}
                </Typography.Title>
              </div>
              <Flex gap={8}>
                <Button icon={<PlusOutlined />} onClick={() => setMovimientoAbierto(true)}>
                  Ingreso/egreso
                </Button>
                <Button danger onClick={() => setCerrarAbierto(true)}>
                  Cerrar caja
                </Button>
              </Flex>
            </Flex>

            <Table
              columns={columnas}
              dataSource={movimientos}
              rowKey="id"
              pagination={false}
              locale={{ emptyText: <Empty description="Todavía no hay movimientos manuales en este turno" /> }}
            />
          </>
        )}
      </Card>

      <Modal
        title="Abrir caja"
        open={abrirAbierto}
        onCancel={() => setAbrirAbierto(false)}
        onOk={handleAbrir}
        confirmLoading={abriendo}
      >
        <Typography.Text>Monto inicial (efectivo con el que arranca el turno)</Typography.Text>
        <InputNumber
          min={0}
          step={100}
          style={{ width: '100%', marginTop: 8 }}
          value={montoInicial}
          onChange={(v) => setMontoInicial(v)}
          onPressEnter={handleAbrir}
          autoFocus
        />
      </Modal>

      <Modal
        title="Cerrar caja"
        open={cerrarAbierto}
        onCancel={() => setCerrarAbierto(false)}
        onOk={handleCerrar}
        confirmLoading={cerrando}
      >
        <Typography.Text>Monto final (efectivo contado al cierre)</Typography.Text>
        <InputNumber
          min={0}
          step={100}
          style={{ width: '100%', marginTop: 8 }}
          value={montoFinal}
          onChange={(v) => setMontoFinal(v)}
          onPressEnter={handleCerrar}
          autoFocus
        />
      </Modal>

      <Modal
        title="Registrar ingreso/egreso"
        open={movimientoAbierto}
        onCancel={() => setMovimientoAbierto(false)}
        onOk={handleRegistrarMovimiento}
        confirmLoading={guardandoMovimiento}
      >
        <Form form={form} layout="vertical" initialValues={{ tipo: 'INGRESO' }}>
          <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'INGRESO', label: 'Ingreso' },
                { value: 'EGRESO', label: 'Egreso' },
              ]}
            />
          </Form.Item>
          <Form.Item name="monto" label="Monto" rules={[{ required: true, message: 'Ingresá el monto' }]}>
            <InputNumber min={0.01} step={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="concepto" label="Concepto" rules={[{ required: true, message: 'Ingresá un concepto' }]}>
            <Input placeholder="Ej: pago a repartidor, retiro de efectivo, etc." />
          </Form.Item>
        </Form>
      </Modal>
    </AppLayout>
  );
}
