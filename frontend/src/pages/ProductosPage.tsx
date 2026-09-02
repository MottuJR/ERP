import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { crearCategoria, listarCategorias, type Categoria } from '../api/categorias';
import {
  actualizarProducto,
  crearProducto,
  desactivarProducto,
  listarProductos,
  type ProductoPayload,
} from '../api/productos';
import { mensajeDeError } from '../api/client';
import type { Producto } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function ProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [cargando, setCargando] = useState(true);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [productoEditando, setProductoEditando] = useState<Producto | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<ProductoPayload>();
  const seVendePorPeso = Form.useWatch('seVendePorPeso', form);

  const [nuevaCategoriaAbierta, setNuevaCategoriaAbierta] = useState(false);
  const [nombreNuevaCategoria, setNombreNuevaCategoria] = useState('');
  const [creandoCategoria, setCreandoCategoria] = useState(false);

  function cargarTodo() {
    setCargando(true);
    Promise.all([listarProductos(), listarCategorias()])
      .then(([prods, cats]) => {
        setProductos(prods);
        setCategorias(cats);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los productos')))
      .finally(() => setCargando(false));
  }

  useEffect(cargarTodo, []);

  function abrirNuevo() {
    setProductoEditando(null);
    form.resetFields();
    form.setFieldsValue({ seVendePorPeso: false, stockMinimo: 0, unidadMedida: 'UNIDAD', tipo: 'REVENTA' });
    setModalAbierto(true);
  }

  function abrirEdicion(producto: Producto) {
    setProductoEditando(producto);
    form.setFieldsValue({
      nombre: producto.nombre,
      categoriaId: producto.categoriaId,
      tipo: producto.tipo,
      seVendePorPeso: producto.seVendePorPeso,
      precioVenta: producto.precioVenta,
      unidadMedida: producto.unidadMedida,
      codigoBarras: producto.codigoBarras ?? undefined,
      codigoPLU: producto.codigoPLU ?? undefined,
      stockMinimo: producto.stockMinimo,
      activo: producto.activo,
    });
    setModalAbierto(true);
  }

  async function handleGuardar() {
    const values = await form.validateFields();
    // Cada producto usa solo uno de los dos códigos, según cómo se vende (sección 5 del diseño).
    const payload: ProductoPayload = {
      ...values,
      codigoBarras: values.seVendePorPeso ? null : values.codigoBarras || null,
      codigoPLU: values.seVendePorPeso ? values.codigoPLU || null : null,
      activo: productoEditando ? values.activo : undefined,
    };

    setGuardando(true);
    try {
      if (productoEditando) {
        await actualizarProducto(productoEditando.id, payload);
        message.success('Producto actualizado');
      } else {
        await crearProducto(payload);
        message.success('Producto creado');
      }
      setModalAbierto(false);
      cargarTodo();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar el producto'));
    } finally {
      setGuardando(false);
    }
  }

  async function handleDesactivar(producto: Producto) {
    try {
      await desactivarProducto(producto.id);
      message.success(`"${producto.nombre}" dado de baja`);
      cargarTodo();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo dar de baja el producto'));
    }
  }

  async function handleCrearCategoria() {
    if (!nombreNuevaCategoria.trim()) return;
    setCreandoCategoria(true);
    try {
      const categoria = await crearCategoria(nombreNuevaCategoria.trim());
      setCategorias((prev) => [...prev, categoria]);
      form.setFieldValue('categoriaId', categoria.id);
      setNuevaCategoriaAbierta(false);
      setNombreNuevaCategoria('');
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo crear la categoría'));
    } finally {
      setCreandoCategoria(false);
    }
  }

  const columnas: ColumnsType<Producto> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Categoría', dataIndex: 'categoriaNombre' },
    { title: 'Tipo', dataIndex: 'tipo' },
    {
      title: 'Precio',
      dataIndex: 'precioVenta',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Código',
      render: (_, p) => (p.seVendePorPeso ? `PLU ${p.codigoPLU}` : p.codigoBarras || '—'),
    },
    { title: 'Stock actual', dataIndex: 'stockActual' },
    {
      title: 'Estado',
      dataIndex: 'activo',
      render: (activo: boolean) => <Tag color={activo ? 'green' : 'default'}>{activo ? 'Activo' : 'Inactivo'}</Tag>,
    },
    {
      title: '',
      width: 90,
      render: (_, producto) => (
        <>
          <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(producto)} />
          {producto.activo && (
            <Button type="text" danger onClick={() => handleDesactivar(producto)}>
              Dar de baja
            </Button>
          )}
        </>
      ),
    },
  ];

  return (
    <AppLayout>
      <Card
        title="Productos"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={abrirNuevo}>
            Nuevo producto
          </Button>
        }
      >
        <Table columns={columnas} dataSource={productos} rowKey="id" loading={cargando} />
      </Card>

      <Modal
        title={productoEditando ? 'Editar producto' : 'Nuevo producto'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        onOk={handleGuardar}
        confirmLoading={guardando}
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="nombre" label="Nombre" rules={[{ required: true, message: 'Ingresá el nombre' }]}>
            <Input />
          </Form.Item>

          <Form.Item label="Categoría" required>
            <div style={{ display: 'flex', gap: 8 }}>
              <Form.Item
                name="categoriaId"
                noStyle
                rules={[{ required: true, message: 'Elegí una categoría' }]}
              >
                <Select
                  showSearch
                  optionFilterProp="label"
                  style={{ flex: 1 }}
                  options={categorias.map((c) => ({ value: c.id, label: c.nombre }))}
                />
              </Form.Item>
              <Button onClick={() => setNuevaCategoriaAbierta(true)}>+ Nueva</Button>
            </div>
          </Form.Item>

          <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ELABORADO', label: 'Elaborado (tiene receta)' },
                { value: 'REVENTA', label: 'Reventa (se compra ya terminado)' },
              ]}
            />
          </Form.Item>

          <Form.Item name="unidadMedida" label="Unidad de medida" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'UNIDAD', label: 'Unidad' },
                { value: 'KG', label: 'Kilogramo' },
                { value: 'GRAMO', label: 'Gramo' },
                { value: 'LITRO', label: 'Litro' },
              ]}
            />
          </Form.Item>

          <Form.Item name="precioVenta" label="Precio de venta" rules={[{ required: true }]}>
            <InputNumber min={0.01} step={10} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="seVendePorPeso" label="¿Se vende por peso?" valuePropName="checked">
            <Switch />
          </Form.Item>

          {seVendePorPeso ? (
            <Form.Item
              name="codigoPLU"
              label="Código PLU (etiqueta de balanza)"
              rules={[{ required: true, message: 'Ingresá el código PLU' }]}
            >
              <Input />
            </Form.Item>
          ) : (
            <Form.Item name="codigoBarras" label="Código de barras (opcional)">
              <Input />
            </Form.Item>
          )}

          <Form.Item name="stockMinimo" label="Stock mínimo (alerta de stock crítico)" rules={[{ required: true }]}>
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>

          {productoEditando && (
            <Form.Item name="activo" label="Activo" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="Nueva categoría"
        open={nuevaCategoriaAbierta}
        onCancel={() => setNuevaCategoriaAbierta(false)}
        onOk={handleCrearCategoria}
        confirmLoading={creandoCategoria}
      >
        <Input
          placeholder="Nombre de la categoría"
          value={nombreNuevaCategoria}
          onChange={(e) => setNombreNuevaCategoria(e.target.value)}
          onPressEnter={handleCrearCategoria}
        />
      </Modal>
    </AppLayout>
  );
}
