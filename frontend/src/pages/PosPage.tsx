import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Flex,
  Input,
  InputNumber,
  Layout,
  Row,
  Select,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { DeleteOutlined, LogoutOutlined, ScanOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useAuth } from '../auth/AuthContext';
import { listarProductos } from '../api/productos';
import { confirmarVenta, escanear, type ItemVentaPayload } from '../api/ventas';
import { mensajeDeError } from '../api/client';
import { MEDIOS_PAGO, type MedioPago, type Producto } from '../types';

interface ItemCarrito {
  key: string;
  codigoEscaneado?: string;
  productoId: number;
  productoNombre: string;
  seVendePorPeso: boolean;
  unidadMedida: string;
  cantidad: number;
  precioUnitario: number;
}

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function PosPage() {
  const { usuario, logout } = useAuth();

  const [productos, setProductos] = useState<Producto[]>([]);
  const [cargandoProductos, setCargandoProductos] = useState(true);
  const [errorProductos, setErrorProductos] = useState<string | null>(null);

  const [codigoInput, setCodigoInput] = useState('');
  const [escaneando, setEscaneando] = useState(false);

  const [productoManualId, setProductoManualId] = useState<number | undefined>();
  const [cantidadManual, setCantidadManual] = useState<number>(1);

  const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
  const [medioPago, setMedioPago] = useState<MedioPago>('EFECTIVO');
  const [confirmando, setConfirmando] = useState(false);

  useEffect(() => {
    listarProductos()
      .then(setProductos)
      .catch((err) => setErrorProductos(mensajeDeError(err, 'No se pudieron cargar los productos')))
      .finally(() => setCargandoProductos(false));
  }, []);

  const total = useMemo(
    () => carrito.reduce((acc, item) => acc + item.cantidad * item.precioUnitario, 0),
    [carrito],
  );

  async function handleEscanear() {
    const codigo = codigoInput.trim();
    if (!codigo) return;

    setEscaneando(true);
    try {
      const resultado = await escanear(codigo);
      setCarrito((prev) => [
        ...prev,
        {
          key: crypto.randomUUID(),
          codigoEscaneado: codigo,
          productoId: resultado.productoId,
          productoNombre: resultado.productoNombre,
          seVendePorPeso: resultado.seVendePorPeso,
          unidadMedida: resultado.seVendePorPeso ? 'kg' : 'un.',
          cantidad: resultado.cantidad,
          precioUnitario: resultado.precioUnitario,
        },
      ]);
      setCodigoInput('');
    } catch (err) {
      message.error(mensajeDeError(err, 'No se encontró ningún producto con ese código'));
    } finally {
      setEscaneando(false);
    }
  }

  function handleAgregarManual() {
    const producto = productos.find((p) => p.id === productoManualId);
    if (!producto || cantidadManual <= 0) return;

    setCarrito((prev) => [
      ...prev,
      {
        key: crypto.randomUUID(),
        productoId: producto.id,
        productoNombre: producto.nombre,
        seVendePorPeso: producto.seVendePorPeso,
        unidadMedida: producto.seVendePorPeso ? 'kg' : 'un.',
        cantidad: cantidadManual,
        precioUnitario: producto.precioVenta,
      },
    ]);
    setProductoManualId(undefined);
    setCantidadManual(1);
  }

  function handleQuitar(key: string) {
    setCarrito((prev) => prev.filter((item) => item.key !== key));
  }

  function handleCambiarCantidad(key: string, cantidad: number) {
    setCarrito((prev) => prev.map((item) => (item.key === key ? { ...item, cantidad } : item)));
  }

  async function handleConfirmarVenta() {
    if (carrito.length === 0) return;

    setConfirmando(true);
    try {
      const items: ItemVentaPayload[] = carrito.map((item) =>
        item.codigoEscaneado
          ? { codigoEscaneado: item.codigoEscaneado, cantidad: item.cantidad }
          : { productoId: item.productoId, cantidad: item.cantidad },
      );

      const venta = await confirmarVenta({ medioPago, items });

      message.success(`Venta #${venta.id} confirmada — total ${formatoMoneda.format(venta.total)}`);
      setCarrito([]);
      setMedioPago('EFECTIVO');
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo confirmar la venta'));
    } finally {
      setConfirmando(false);
    }
  }

  const columnas: ColumnsType<ItemCarrito> = [
    { title: 'Producto', dataIndex: 'productoNombre' },
    {
      title: 'Cantidad',
      dataIndex: 'cantidad',
      width: 160,
      render: (_, item) =>
        item.seVendePorPeso ? (
          <span>
            {item.cantidad.toFixed(3)} kg
          </span>
        ) : (
          <InputNumber
            min={1}
            step={1}
            value={item.cantidad}
            onChange={(valor) => handleCambiarCantidad(item.key, valor ?? 1)}
          />
        ),
    },
    {
      title: 'Precio unitario',
      dataIndex: 'precioUnitario',
      width: 140,
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Subtotal',
      width: 140,
      render: (_, item) => formatoMoneda.format(item.cantidad * item.precioUnitario),
    },
    {
      title: '',
      width: 50,
      render: (_, item) => (
        <Button danger type="text" icon={<DeleteOutlined />} onClick={() => handleQuitar(item.key)} />
      ),
    },
  ];

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Layout.Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ color: 'white', margin: 0 }}>
          ERP Panadería — Punto de venta
        </Typography.Title>
        <Flex align="center" gap={12}>
          <Tag color="blue">{usuario?.rol}</Tag>
          <Typography.Text style={{ color: 'white' }}>{usuario?.nombre}</Typography.Text>
          <Button icon={<LogoutOutlined />} onClick={logout}>
            Salir
          </Button>
        </Flex>
      </Layout.Header>

      <Layout.Content style={{ padding: 24 }}>
        {errorProductos && <Alert type="error" title={errorProductos} showIcon style={{ marginBottom: 16 }} />}

        <Row gutter={24}>
          <Col span={10}>
            <Card title="Agregar producto" style={{ marginBottom: 24 }}>
              <Typography.Text type="secondary">Escanear código de barras o etiqueta de balanza</Typography.Text>
              <Flex gap={8} style={{ marginTop: 8, marginBottom: 24 }}>
                <Input
                  prefix={<ScanOutlined />}
                  placeholder="Código escaneado"
                  value={codigoInput}
                  onChange={(e) => setCodigoInput(e.target.value)}
                  onPressEnter={handleEscanear}
                  disabled={escaneando}
                  autoFocus
                />
                <Button type="primary" onClick={handleEscanear} loading={escaneando}>
                  Agregar
                </Button>
              </Flex>

              <Typography.Text type="secondary">Buscar producto manualmente</Typography.Text>
              <Flex gap={8} style={{ marginTop: 8 }}>
                <Select
                  showSearch
                  placeholder="Producto"
                  style={{ flex: 1 }}
                  loading={cargandoProductos}
                  value={productoManualId}
                  onChange={setProductoManualId}
                  optionFilterProp="label"
                  options={productos.map((p) => ({
                    value: p.id,
                    label: `${p.nombre} — ${formatoMoneda.format(p.precioVenta)}`,
                  }))}
                />
                <InputNumber min={1} value={cantidadManual} onChange={(v) => setCantidadManual(v ?? 1)} />
                <Button onClick={handleAgregarManual} disabled={!productoManualId}>
                  Agregar
                </Button>
              </Flex>
            </Card>
          </Col>

          <Col span={14}>
            <Card title="Carrito">
              <Table
                columns={columnas}
                dataSource={carrito}
                rowKey="key"
                pagination={false}
                locale={{ emptyText: 'Todavía no agregaste ningún producto' }}
              />

              <Flex justify="space-between" align="center" style={{ marginTop: 24 }}>
                <Select
                  value={medioPago}
                  onChange={setMedioPago}
                  options={MEDIOS_PAGO}
                  style={{ width: 220 }}
                />
                <Typography.Title level={3} style={{ margin: 0 }}>
                  Total: {formatoMoneda.format(total)}
                </Typography.Title>
              </Flex>

              <Button
                type="primary"
                size="large"
                block
                style={{ marginTop: 16 }}
                disabled={carrito.length === 0}
                loading={confirmando}
                onClick={handleConfirmarVenta}
              >
                Confirmar venta
              </Button>
            </Card>
          </Col>
        </Row>
      </Layout.Content>
    </Layout>
  );
}
