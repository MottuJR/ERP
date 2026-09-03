import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Flex, Form, Input, InputNumber, Modal, Select, Switch, Table, Tag, message } from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import {
  actualizarUsuario,
  crearUsuario,
  listarUsuarios,
  type ActualizarUsuarioPayload,
  type CrearUsuarioPayload,
} from '../api/usuarios';
import { mensajeDeError } from '../api/client';
import type { Usuario } from '../types';

const ROLES = [
  { value: 'DUENO', label: 'Dueño' },
  { value: 'ENCARGADO', label: 'Encargado' },
  { value: 'VENDEDOR', label: 'Vendedor' },
];

interface FormValues {
  nombre: string;
  email: string;
  rol: 'DUENO' | 'ENCARGADO' | 'VENDEDOR';
  porcentajeComision: number | null;
  activo: boolean;
  password?: string;
}

export function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [usuarioEditando, setUsuarioEditando] = useState<Usuario | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<FormValues>();

  function cargarUsuarios() {
    setCargando(true);
    listarUsuarios()
      .then(setUsuarios)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los usuarios')))
      .finally(() => setCargando(false));
  }

  useEffect(cargarUsuarios, []);

  const usuariosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto ? usuarios.filter((u) => u.nombre.toLowerCase().includes(texto)) : usuarios;
  }, [usuarios, busqueda]);

  function abrirNuevo() {
    setUsuarioEditando(null);
    form.resetFields();
    form.setFieldsValue({ rol: 'VENDEDOR' });
    setModalAbierto(true);
  }

  function abrirEdicion(usuario: Usuario) {
    setUsuarioEditando(usuario);
    form.setFieldsValue({
      nombre: usuario.nombre,
      email: usuario.email,
      rol: usuario.rol,
      porcentajeComision: usuario.porcentajeComision,
      activo: usuario.activo,
      password: undefined,
    });
    setModalAbierto(true);
  }

  async function handleGuardar() {
    const values = await form.validateFields();

    setGuardando(true);
    try {
      if (usuarioEditando) {
        const payload: ActualizarUsuarioPayload = {
          nombre: values.nombre,
          email: values.email,
          rol: values.rol,
          activo: values.activo,
          porcentajeComision: values.porcentajeComision ?? null,
          password: values.password || undefined,
        };
        await actualizarUsuario(usuarioEditando.id, payload);
        message.success('Usuario actualizado');
      } else {
        const payload: CrearUsuarioPayload = {
          nombre: values.nombre,
          email: values.email,
          password: values.password ?? '',
          rol: values.rol,
          porcentajeComision: values.porcentajeComision ?? null,
        };
        await crearUsuario(payload);
        message.success('Usuario creado');
      }
      setModalAbierto(false);
      cargarUsuarios();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar el usuario'));
    } finally {
      setGuardando(false);
    }
  }

  const columnas: ColumnsType<Usuario> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Email', dataIndex: 'email' },
    {
      title: 'Rol',
      dataIndex: 'rol',
      render: (rol: string) => ROLES.find((r) => r.value === rol)?.label ?? rol,
    },
    {
      title: 'Comisión',
      dataIndex: 'porcentajeComision',
      render: (valor: number | null) => (valor != null ? `${valor}%` : '—'),
    },
    {
      title: 'Estado',
      dataIndex: 'activo',
      render: (activo: boolean) => <Tag color={activo ? 'green' : 'default'}>{activo ? 'Activo' : 'Inactivo'}</Tag>,
    },
    {
      title: '',
      width: 60,
      render: (_, usuario) => <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(usuario)} />,
    },
  ];

  return (
    <AppLayout>
      <Card
        title="Usuarios"
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
              Nuevo usuario
            </Button>
          </Flex>
        }
      >
        <Table
          columns={columnas}
          dataSource={usuariosFiltrados}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: busqueda ? 'Ningún usuario coincide con la búsqueda' : 'Sin usuarios' }}
        />
      </Card>

      <Modal
        title={usuarioEditando ? 'Editar usuario' : 'Nuevo usuario'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        onOk={handleGuardar}
        confirmLoading={guardando}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="nombre" label="Nombre" rules={[{ required: true, message: 'Ingresá el nombre' }]}>
            <Input />
          </Form.Item>

          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Ingresá el email' },
              { type: 'email', message: 'Email inválido' },
            ]}
          >
            <Input />
          </Form.Item>

          <Form.Item name="rol" label="Rol" rules={[{ required: true }]}>
            <Select options={ROLES} />
          </Form.Item>

          <Form.Item
            name="porcentajeComision"
            label="Porcentaje de comisión (opcional, 0-100)"
          >
            <InputNumber min={0} max={100} step={0.5} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="password"
            label={usuarioEditando ? 'Nueva contraseña (dejar vacío para no cambiarla)' : 'Contraseña'}
            rules={
              usuarioEditando
                ? [{ min: 8, message: 'Mínimo 8 caracteres' }]
                : [
                    { required: true, message: 'Ingresá una contraseña' },
                    { min: 8, message: 'Mínimo 8 caracteres' },
                  ]
            }
          >
            <Input.Password />
          </Form.Item>

          {usuarioEditando && (
            <Form.Item name="activo" label="Activo" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </AppLayout>
  );
}
