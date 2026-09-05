import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { AppLayout } from '../layout/AppLayout';
import {
  actualizarCliente,
  crearCliente,
  listarClientes,
  listarPagosCliente,
  listarVentasCliente,
  obtenerSaldoCliente,
  registrarPagoCliente,
  type ClientePayload,
} from '../api/clientes';
import { mensajeDeError } from '../api/client';
import { MEDIOS_PAGO } from '../types';
import type { Cliente, MedioPago, PagoCliente, SaldoCliente, VentaCliente } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const MEDIOS_PAGO_CUENTA = MEDIOS_PAGO.filter((m) => m.value !== 'CUENTA_CORRIENTE');
const FORMATO_FECHA = 'YYYY-MM-DD';

export function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [saldos, setSaldos] = useState<Record<number, number>>({});
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [clienteEditando, setClienteEditando] = useState<Cliente | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<ClientePayload>();

  const [cuentaAbierta, setCuentaAbierta] = useState(false);
  const [clienteCuenta, setClienteCuenta] = useState<Cliente | null>(null);
  const [saldoCuenta, setSaldoCuenta] = useState<SaldoCliente | null>(null);
  const [pagos, setPagos] = useState<PagoCliente[]>([]);
  const [cargandoCuenta, setCargandoCuenta] = useState(false);
  const [montoPago, setMontoPago] = useState<number | null>(null);
  const [medioPagoPago, setMedioPagoPago] = useState<MedioPago>('EFECTIVO');
  const [registrandoPago, setRegistrandoPago] = useState(false);

  const [rangoCompras, setRangoCompras] = useState<[Dayjs, Dayjs]>([dayjs().subtract(30, 'day'), dayjs()]);
  const [compras, setCompras] = useState<VentaCliente[]>([]);
  const [cargandoCompras, setCargandoCompras] = useState(false);

  function cargarClientes() {
    setCargando(true);
    listarClientes()
      .then((lista) => {
        setClientes(lista);
        const conCuenta = lista.filter((c) => c.tieneCuentaCorriente);
        Promise.all(conCuenta.map((c) => obtenerSaldoCliente(c.id)))
          .then((resultados) => {
            const mapa: Record<number, number> = {};
            resultados.forEach((r) => {
              mapa[r.clienteId] = r.saldo;
            });
            setSaldos(mapa);
          })
          .catch(() => {
            // Si falla traer los saldos no rompemos la lista de clientes, solo se ven sin saldo.
          });
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los clientes')))
      .finally(() => setCargando(false));
  }

  useEffect(cargarClientes, []);

  const clientesFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto ? clientes.filter((c) => c.nombre.toLowerCase().includes(texto)) : clientes;
  }, [clientes, busqueda]);

  function abrirNuevo() {
    setClienteEditando(null);
    form.resetFields();
    form.setFieldsValue({ tieneCuentaCorriente: false });
    setModalAbierto(true);
  }

  function abrirEdicion(cliente: Cliente) {
    setClienteEditando(cliente);
    form.setFieldsValue({
      nombre: cliente.nombre,
      telefono: cliente.telefono ?? undefined,
      tieneCuentaCorriente: cliente.tieneCuentaCorriente,
    });
    setModalAbierto(true);
  }

  async function handleGuardar() {
    const values = await form.validateFields();
    setGuardando(true);
    try {
      if (clienteEditando) {
        await actualizarCliente(clienteEditando.id, values);
        message.success('Cliente actualizado');
      } else {
        await crearCliente(values);
        message.success('Cliente creado');
      }
      setModalAbierto(false);
      cargarClientes();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar el cliente'));
    } finally {
      setGuardando(false);
    }
  }

  function abrirCuenta(cliente: Cliente) {
    setClienteCuenta(cliente);
    setCuentaAbierta(true);
    setMontoPago(null);
    setMedioPagoPago('EFECTIVO');
    setRangoCompras([dayjs().subtract(30, 'day'), dayjs()]);
    cargarCuenta(cliente.id);
  }

  function cargarCuenta(clienteId: number) {
    setCargandoCuenta(true);
    Promise.all([obtenerSaldoCliente(clienteId), listarPagosCliente(clienteId)])
      .then(([saldo, listaPagos]) => {
        setSaldoCuenta(saldo);
        setPagos(listaPagos);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo cargar la cuenta corriente')))
      .finally(() => setCargandoCuenta(false));
  }

  useEffect(() => {
    if (!cuentaAbierta || !clienteCuenta) return;
    const [desde, hasta] = rangoCompras;
    setCargandoCompras(true);
    listarVentasCliente(clienteCuenta.id, desde.format(FORMATO_FECHA), hasta.format(FORMATO_FECHA))
      .then(setCompras)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar las compras')))
      .finally(() => setCargandoCompras(false));
  }, [cuentaAbierta, clienteCuenta, rangoCompras]);

  async function handleRegistrarPago() {
    if (!clienteCuenta || !montoPago || montoPago <= 0) {
      message.warning('Ingresá un monto válido');
      return;
    }

    setRegistrandoPago(true);
    try {
      const pago = await registrarPagoCliente(clienteCuenta.id, montoPago, medioPagoPago);
      if (pago.cajaId === null) {
        message.warning('Pago registrado, pero no hay ninguna caja abierta: no se va a contar en las comisiones de ningún turno.');
      } else {
        message.success('Pago registrado');
      }
      setMontoPago(null);
      cargarCuenta(clienteCuenta.id);
      cargarClientes();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo registrar el pago'));
    } finally {
      setRegistrandoPago(false);
    }
  }

  const columnas: ColumnsType<Cliente> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Teléfono', dataIndex: 'telefono' },
    {
      title: 'Cuenta corriente',
      dataIndex: 'tieneCuentaCorriente',
      render: (tiene: boolean) => <Tag color={tiene ? 'blue' : 'default'}>{tiene ? 'Habilitada' : 'No'}</Tag>,
    },
    {
      title: 'Saldo',
      render: (_, cliente) => {
        if (!cliente.tieneCuentaCorriente) return '—';
        const saldo = saldos[cliente.id];
        if (saldo === undefined) return '—';
        return (
          <Typography.Text type={saldo > 0 ? 'danger' : 'secondary'} strong={saldo > 0}>
            {formatoMoneda.format(saldo)}
          </Typography.Text>
        );
      },
    },
    {
      title: '',
      width: 200,
      render: (_, cliente) => (
        <Flex gap={8}>
          {cliente.tieneCuentaCorriente && (
            <Typography.Link onClick={() => abrirCuenta(cliente)}>Cuenta corriente</Typography.Link>
          )}
          <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(cliente)} />
        </Flex>
      ),
    },
  ];

  const columnasCompras: ColumnsType<VentaCliente> = [
    { title: 'Fecha', dataIndex: 'fecha', render: (f: string) => new Date(f).toLocaleString('es-AR') },
    { title: 'Medio de pago', dataIndex: 'medioPago', render: (m: string) => MEDIOS_PAGO.find((mp) => mp.value === m)?.label ?? m },
    { title: 'Total', dataIndex: 'total', render: (v: number) => formatoMoneda.format(v) },
  ];

  const columnasPagos: ColumnsType<PagoCliente> = [
    { title: 'Fecha', dataIndex: 'fecha', render: (f: string) => new Date(f).toLocaleString('es-AR') },
    { title: 'Medio de pago', dataIndex: 'medioPago', render: (m: string) => MEDIOS_PAGO.find((mp) => mp.value === m)?.label ?? m },
    { title: 'Monto', dataIndex: 'monto', render: (v: number) => formatoMoneda.format(v) },
  ];

  return (
    <AppLayout>
      <Card
        title="Clientes"
        extra={
          <Flex gap={8}>
            <Input
              placeholder="Buscar por nombre"
              prefix={<SearchOutlined />}
              allowClear
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              style={{ width: 240 }}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={abrirNuevo}>
              Nuevo cliente
            </Button>
          </Flex>
        }
      >
        <Table
          columns={columnas}
          dataSource={clientesFiltrados}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: busqueda ? 'Ningún cliente coincide con la búsqueda' : 'Sin clientes' }}
        />
      </Card>

      <Modal
        title={clienteEditando ? 'Editar cliente' : 'Nuevo cliente'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        onOk={handleGuardar}
        confirmLoading={guardando}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="nombre" label="Nombre" rules={[{ required: true, message: 'Ingresá el nombre' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="telefono" label="Teléfono">
            <Input />
          </Form.Item>
          <Form.Item name="tieneCuentaCorriente" label="Cuenta corriente habilitada" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={clienteCuenta ? `Cuenta corriente — ${clienteCuenta.nombre}` : 'Cuenta corriente'}
        open={cuentaAbierta}
        onCancel={() => setCuentaAbierta(false)}
        footer={null}
        width={700}
      >
        {saldoCuenta && (
          <Typography.Paragraph>
            <strong>Total vendido a cuenta corriente:</strong>{' '}
            {formatoMoneda.format(saldoCuenta.totalVentasCuentaCorriente)}
            <br />
            <strong>Total pagado:</strong> {formatoMoneda.format(saldoCuenta.totalPagos)}
            <br />
            <strong>Saldo actual:</strong>{' '}
            <Typography.Text type={saldoCuenta.saldo > 0 ? 'danger' : 'success'} strong>
              {formatoMoneda.format(saldoCuenta.saldo)}
            </Typography.Text>
          </Typography.Paragraph>
        )}

        <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
          <Typography.Title level={5} style={{ margin: 0 }}>
            Compras
          </Typography.Title>
          <DatePicker.RangePicker
            value={rangoCompras}
            format="DD/MM/YYYY"
            allowClear={false}
            onChange={(valores) => {
              if (valores && valores[0] && valores[1]) {
                setRangoCompras([valores[0], valores[1]]);
              }
            }}
          />
        </Flex>
        <Table
          columns={columnasCompras}
          dataSource={compras}
          rowKey="id"
          pagination={false}
          size="small"
          loading={cargandoCompras}
          scroll={{ y: 240 }}
          locale={{ emptyText: 'Sin compras en el período elegido' }}
          style={{ marginBottom: 16 }}
        />

        <Typography.Title level={5}>Historial de pagos</Typography.Title>
        <Table
          columns={columnasPagos}
          dataSource={pagos}
          rowKey="id"
          pagination={false}
          size="small"
          loading={cargandoCuenta}
          locale={{ emptyText: 'Todavía no registró ningún pago' }}
          style={{ marginBottom: 16 }}
        />

        <Typography.Title level={5}>Registrar pago</Typography.Title>
        <Flex gap={8}>
          <InputNumber
            min={0.01}
            step={100}
            placeholder="Monto"
            style={{ flex: 1 }}
            value={montoPago}
            onChange={(v) => setMontoPago(v)}
          />
          <Select
            value={medioPagoPago}
            onChange={setMedioPagoPago}
            options={MEDIOS_PAGO_CUENTA}
            style={{ width: 200 }}
          />
          <Button type="primary" loading={registrandoPago} onClick={handleRegistrarPago}>
            Registrar
          </Button>
        </Flex>
      </Modal>
    </AppLayout>
  );
}
