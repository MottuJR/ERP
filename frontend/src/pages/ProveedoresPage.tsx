import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Flex, Form, Input, Modal, Table, message } from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { crearProveedor, listarProveedores, actualizarProveedor, type ProveedorPayload } from '../api/compras';
import { mensajeDeError } from '../api/client';
import type { Proveedor } from '../types';

export function ProveedoresPage() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [proveedorEditando, setProveedorEditando] = useState<Proveedor | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<ProveedorPayload>();

  function cargarProveedores() {
    setCargando(true);
    listarProveedores()
      .then(setProveedores)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los proveedores')))
      .finally(() => setCargando(false));
  }

  useEffect(cargarProveedores, []);

  const proveedoresFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto ? proveedores.filter((p) => p.nombre.toLowerCase().includes(texto)) : proveedores;
  }, [proveedores, busqueda]);

  function abrirNuevo() {
    setProveedorEditando(null);
    form.resetFields();
    setModalAbierto(true);
  }

  function abrirEdicion(proveedor: Proveedor) {
    setProveedorEditando(proveedor);
    form.setFieldsValue({
      nombre: proveedor.nombre,
      contacto: proveedor.contacto ?? undefined,
      telefono: proveedor.telefono ?? undefined,
      email: proveedor.email ?? undefined,
    });
    setModalAbierto(true);
  }

  async function handleGuardar() {
    const values = await form.validateFields();
    setGuardando(true);
    try {
      if (proveedorEditando) {
        await actualizarProveedor(proveedorEditando.id, values);
        message.success('Proveedor actualizado');
      } else {
        await crearProveedor(values);
        message.success('Proveedor creado');
      }
      setModalAbierto(false);
      cargarProveedores();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar el proveedor'));
    } finally {
      setGuardando(false);
    }
  }

  const columnas: ColumnsType<Proveedor> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Contacto', dataIndex: 'contacto' },
    { title: 'Teléfono', dataIndex: 'telefono' },
    { title: 'Email', dataIndex: 'email' },
    {
      title: '',
      width: 60,
      render: (_, proveedor) => (
        <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(proveedor)} />
      ),
    },
  ];

  return (
    <AppLayout>
      <Card
        title="Proveedores"
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
              Nuevo proveedor
            </Button>
          </Flex>
        }
      >
        <Table
          columns={columnas}
          dataSource={proveedoresFiltrados}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: busqueda ? 'Ningún proveedor coincide con la búsqueda' : 'Sin proveedores' }}
        />
      </Card>

      <Modal
        title={proveedorEditando ? 'Editar proveedor' : 'Nuevo proveedor'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        onOk={handleGuardar}
        confirmLoading={guardando}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="nombre" label="Nombre" rules={[{ required: true, message: 'Ingresá el nombre' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="contacto" label="Contacto">
            <Input />
          </Form.Item>
          <Form.Item name="telefono" label="Teléfono">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email inválido' }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </AppLayout>
  );
}
