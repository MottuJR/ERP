import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
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
import { registrarMovimientoStock } from '../api/inventario';
import { mensajeDeError } from '../api/client';
import type { Producto } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function ProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [productoEditando, setProductoEditando] = useState<Producto | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<ProductoPayload>();
  const seVendePorPeso = Form.useWatch('seVendePorPeso', form);

  const [nuevaCategoriaAbierta, setNuevaCategoriaAbierta] = useState(false);
  const [nombreNuevaCategoria, setNombreNuevaCategoria] = useState('');
  const [creandoCategoria, setCreandoCategoria] = useState(false);

  const [stockActualNuevo, setStockActualNuevo] = useState<number | null>(null);

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

  const productosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto ? productos.filter((p) => p.nombre.toLowerCase().includes(texto)) : productos;
  }, [productos, busqueda]);

  function abrirNuevo() {
    setProductoEditando(null);
    setStockActualNuevo(null);
    form.resetFields();
    form.setFieldsValue({ seVendePorPeso: false, stockMinimo: 0, unidadMedida: 'UNIDAD', tipo: 'REVENTA' });
    setModalAbierto(true);
  }

  function abrirEdicion(producto: Producto) {
    setProductoEditando(producto);
    setStockActualNuevo(producto.stockActual);
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
    // Un producto vendido por peso siempre necesita PLU (y no puede tener código de barras fijo,
    // porque la balanza lo cambia en cada pesada). Uno que NO se vende por peso puede tener los
    // dos: código de barras fijo, y/o un PLU si la balanza está configurada para imprimirle una
    // cantidad de unidades en vez de un peso (ver EscaneoService — ej. facturas contadas así).
    const payload: ProductoPayload = {
      ...values,
      codigoBarras: values.seVendePorPeso ? null : values.codigoBarras || null,
      codigoPLU: values.codigoPLU || null,
      activo: productoEditando ? values.activo : undefined,
    };

    setGuardando(true);
    try {
      if (productoEditando) {
        await actualizarProducto(productoEditando.id, payload);
        const delta =
          stockActualNuevo !== null ? stockActualNuevo - productoEditando.stockActual : 0;
        if (delta !== 0) {
          await registrarMovimientoStock({
            itemTipo: 'PRODUCTO',
            itemId: productoEditando.id,
            tipo: 'AJUSTE',
            cantidad: delta,
            motivo: 'Corrección manual de stock desde edición de producto',
          });
        }
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
      render: (_, p) => {
        if (p.seVendePorPeso) return `PLU ${p.codigoPLU}`;
        const partes = [p.codigoBarras, p.codigoPLU ? `PLU ${p.codigoPLU}` : null].filter(Boolean);
        return partes.length > 0 ? partes.join(' / ') : '—';
      },
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
            <Popconfirm
              title={`¿Dar de baja "${producto.nombre}"?`}
              description="Deja de aparecer para vender, pero se puede reactivar después desde Editar."
              okText="Dar de baja"
              okButtonProps={{ danger: true }}
              cancelText="Cancelar"
              onConfirm={() => handleDesactivar(producto)}
            >
              <Button type="text" danger>
                Dar de baja
              </Button>
            </Popconfirm>
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
              Nuevo producto
            </Button>
          </Flex>
        }
      >
        <Table
          columns={columnas}
          dataSource={productosFiltrados}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: busqueda ? 'Ningún producto coincide con la búsqueda' : 'Sin productos' }}
        />
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
              label="Código PLU (etiqueta de balanza, en gramos)"
              rules={[{ required: true, message: 'Ingresá el código PLU' }]}
            >
              <Input />
            </Form.Item>
          ) : (
            <>
              <Form.Item name="codigoBarras" label="Código de barras (opcional)">
                <Input />
              </Form.Item>
              <Form.Item
                name="codigoPLU"
                label="Código PLU (opcional — si la balanza está configurada para contar unidades, ej. facturas)"
              >
                <Input />
              </Form.Item>
            </>
          )}

          <Form.Item name="stockMinimo" label="Stock mínimo (alerta de stock crítico)" rules={[{ required: true }]}>
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>

          {productoEditando && (
            <>
              <Form.Item label="Stock actual (corregir manualmente si hubo un error de carga o venta)">
                <InputNumber
                  min={0}
                  step={1}
                  style={{ width: '100%' }}
                  value={stockActualNuevo}
                  onChange={(v) => setStockActualNuevo(v)}
                />
              </Form.Item>

              <Form.Item name="activo" label="Activo" valuePropName="checked">
                <Switch />
              </Form.Item>
            </>
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
