import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Flex, Form, Input, Modal, Switch, Table, Tag, message } from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { actualizarCliente, crearCliente, listarClientes, type ClientePayload } from '../api/clientes';
import { mensajeDeError } from '../api/client';
import type { Cliente } from '../types';

export function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [clienteEditando, setClienteEditando] = useState<Cliente | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<ClientePayload>();

  function cargarClientes() {
    setCargando(true);
    listarClientes()
      .then(setClientes)
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

  const columnas: ColumnsType<Cliente> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Teléfono', dataIndex: 'telefono' },
    {
      title: 'Cuenta corriente',
      dataIndex: 'tieneCuentaCorriente',
      render: (tiene: boolean) => <Tag color={tiene ? 'blue' : 'default'}>{tiene ? 'Habilitada' : 'No'}</Tag>,
    },
    {
      title: '',
      width: 60,
      render: (_, cliente) => <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(cliente)} />,
    },
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
    </AppLayout>
  );
}
