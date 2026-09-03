import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Flex,
  Input,
  InputNumber,
  Row,
  Select,
  Table,
  Typography,
  message,
} from 'antd';
import type { InputRef } from 'antd';
import { DeleteOutlined, ScanOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import { AppLayout } from '../layout/AppLayout';
import { listarProductos } from '../api/productos';
import { listarClientes } from '../api/clientes';
import { obtenerCajaActual } from '../api/caja';
import { confirmarVenta, escanear, type ItemVentaPayload } from '../api/ventas';
import { mensajeDeError } from '../api/client';
import { MEDIOS_PAGO, type Caja, type Cliente, type MedioPago, type Producto } from '../types';

interface ItemCarrito {
  key: string;
  codigoEscaneado?: string;
  productoId: number;
  productoNombre: string;
  seVendePorPeso: boolean;
  unidadMedida: string;
  cantidad: number | null;
  precioUnitario: number;
}

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function PosPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [cargandoProductos, setCargandoProductos] = useState(true);
  const [errorProductos, setErrorProductos] = useState<string | null>(null);

  const [codigoInput, setCodigoInput] = useState('');
  const [escaneando, setEscaneando] = useState(false);

  const [productoManualId, setProductoManualId] = useState<number | undefined>();
  const [cantidadManual, setCantidadManual] = useState<number | null>(1);

  const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
  const [medioPago, setMedioPago] = useState<MedioPago>('EFECTIVO');
  const [confirmando, setConfirmando] = useState(false);

  const [clientesConCuenta, setClientesConCuenta] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<number | undefined>();

  const [cajaActual, setCajaActual] = useState<Caja | null>(null);
  const [cargandoCaja, setCargandoCaja] = useState(true);

  // El lector láser escribe donde esté el foco y termina con Enter, como un teclado. Si el
  // foco se queda en un botón después de agregar un ítem, el siguiente escaneo se pierde —
  // por eso hay que devolverlo acá después de cada acción (éxito, error, o al confirmar).
  const inputCodigoRef = useRef<InputRef>(null);

  function enfocarInputCodigo() {
    setTimeout(() => inputCodigoRef.current?.focus(), 0);
  }

  useEffect(() => {
    listarProductos()
      .then(setProductos)
      .catch((err) => setErrorProductos(mensajeDeError(err, 'No se pudieron cargar los productos')))
      .finally(() => setCargandoProductos(false));

    listarClientes()
      .then((clientes) => setClientesConCuenta(clientes.filter((c) => c.tieneCuentaCorriente)))
      .catch(() => {
        // La cuenta corriente es opcional en el flujo de venta: si no se pueden cargar los
        // clientes, simplemente no se ofrece esa opción, no hace falta romper la pantalla.
      });

    obtenerCajaActual()
      .then(setCajaActual)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo consultar la caja actual')))
      .finally(() => setCargandoCaja(false));
  }, []);

  const total = useMemo(
    () => carrito.reduce((acc, item) => acc + (item.cantidad ?? 0) * item.precioUnitario, 0),
    [carrito],
  );

  async function handleEscanear() {
    const codigo = codigoInput.trim();
    if (!codigo || escaneando) return;

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
      const cantidadTexto = resultado.seVendePorPeso ? `${resultado.cantidad.toFixed(3)} kg` : `x${resultado.cantidad}`;
      message.success({ content: `${resultado.productoNombre} (${cantidadTexto})`, duration: 1.2 });
      setCodigoInput('');
    } catch (err) {
      message.error(mensajeDeError(err, 'No se encontró ningún producto con ese código'));
      setCodigoInput('');
    } finally {
      setEscaneando(false);
      enfocarInputCodigo();
    }
  }

  const productoManualSeleccionado = productos.find((p) => p.id === productoManualId);

  function handleAgregarManual() {
    const producto = productos.find((p) => p.id === productoManualId);
    if (!producto || !cantidadManual || cantidadManual <= 0) return;

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

  function handleCambiarCantidad(key: string, cantidad: number | null) {
    setCarrito((prev) => prev.map((item) => (item.key === key ? { ...item, cantidad } : item)));
  }

  async function handleConfirmarVenta() {
    if (carrito.length === 0 || !cajaActual) return;

    if (medioPago === 'CUENTA_CORRIENTE' && !clienteId) {
      message.warning('Elegí a qué cliente cargarle la cuenta corriente');
      return;
    }

    if (carrito.some((item) => !item.cantidad || item.cantidad <= 0)) {
      message.warning('Hay un ítem del carrito sin una cantidad válida');
      return;
    }

    setConfirmando(true);
    try {
      const items: ItemVentaPayload[] = carrito.map((item) =>
        item.codigoEscaneado
          ? { codigoEscaneado: item.codigoEscaneado, cantidad: item.cantidad as number }
          : { productoId: item.productoId, cantidad: item.cantidad as number },
      );

      const venta = await confirmarVenta({
        medioPago,
        items,
        cajaId: cajaActual.id,
        clienteId: medioPago === 'CUENTA_CORRIENTE' ? clienteId : undefined,
      });

      message.success(`Venta #${venta.id} confirmada — total ${formatoMoneda.format(venta.total)}`);
      setCarrito([]);
      setMedioPago('EFECTIVO');
      setClienteId(undefined);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo confirmar la venta'));
    } finally {
      setConfirmando(false);
      enfocarInputCodigo();
    }
  }

  // Atajo para confirmar la venta sin soltar el mouse ni ir a buscar el botón: Ctrl+Enter
  // desde cualquier parte de la pantalla, mientras haya algo en el carrito.
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        if (carrito.length > 0 && !confirmando && cajaActual) {
          handleConfirmarVenta();
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [carrito, confirmando, cajaActual]);

  const columnas: ColumnsType<ItemCarrito> = [
    { title: 'Producto', dataIndex: 'productoNombre' },
    {
      title: 'Cantidad',
      dataIndex: 'cantidad',
      width: 160,
      render: (_, item) =>
        item.seVendePorPeso ? (
          <span>
            {(item.cantidad ?? 0).toFixed(3)} kg
          </span>
        ) : (
          <InputNumber
            min={1}
            step={1}
            value={item.cantidad}
            onChange={(valor) => handleCambiarCantidad(item.key, valor)}
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
      render: (_, item) => formatoMoneda.format((item.cantidad ?? 0) * item.precioUnitario),
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
    <AppLayout>
      {errorProductos && <Alert type="error" title={errorProductos} showIcon style={{ marginBottom: 16 }} />}

      {!cargandoCaja && !cajaActual && (
        <Alert
          type="warning"
          title="No hay ninguna caja abierta"
          description={
            <>
              Las ventas quedan sin turno asignado y no se contabilizan en las comisiones. Abrí una en{' '}
              <Link to="/caja">Caja</Link> antes de vender.
            </>
          }
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={24}>
        <Col span={10}>
          <Card title="Agregar producto" style={{ marginBottom: 24 }}>
            <Typography.Text type="secondary">Escanear código de barras o etiqueta de balanza</Typography.Text>
            <Flex gap={8} style={{ marginTop: 8, marginBottom: 24 }}>
              <Input
                ref={inputCodigoRef}
                prefix={<ScanOutlined />}
                placeholder="Código escaneado"
                value={codigoInput}
                onChange={(e) => setCodigoInput(e.target.value)}
                onPressEnter={handleEscanear}
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
                onChange={(v) => {
                  setProductoManualId(v);
                  setCantidadManual(1);
                }}
                optionFilterProp="label"
                options={productos.map((p) => ({
                  value: p.id,
                  label: `${p.nombre} — ${formatoMoneda.format(p.precioVenta)}`,
                }))}
              />
              <InputNumber
                min={productoManualSeleccionado?.seVendePorPeso ? 0.001 : 1}
                step={productoManualSeleccionado?.seVendePorPeso ? 0.1 : 1}
                value={cantidadManual}
                onChange={(v) => setCantidadManual(v)}
                onPressEnter={handleAgregarManual}
              />
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
              <Flex gap={8}>
                <Select
                  value={medioPago}
                  onChange={(valor) => {
                    setMedioPago(valor);
                    if (valor !== 'CUENTA_CORRIENTE') setClienteId(undefined);
                  }}
                  options={MEDIOS_PAGO}
                  style={{ width: 220 }}
                />
                {medioPago === 'CUENTA_CORRIENTE' && (
                  <Select
                    showSearch
                    placeholder="Cliente"
                    style={{ width: 220 }}
                    value={clienteId}
                    onChange={setClienteId}
                    optionFilterProp="label"
                    options={clientesConCuenta.map((c) => ({ value: c.id, label: c.nombre }))}
                    notFoundContent="Ningún cliente tiene cuenta corriente habilitada"
                  />
                )}
              </Flex>
              <Typography.Title level={3} style={{ margin: 0 }}>
                Total: {formatoMoneda.format(total)}
              </Typography.Title>
            </Flex>

            <Button
              type="primary"
              size="large"
              block
              style={{ marginTop: 16 }}
              disabled={
                carrito.length === 0 ||
                !cajaActual ||
                (medioPago === 'CUENTA_CORRIENTE' && !clienteId) ||
                carrito.some((item) => !item.cantidad || item.cantidad <= 0)
              }
              loading={confirmando}
              onClick={handleConfirmarVenta}
            >
              Confirmar venta
            </Button>
          </Card>
        </Col>
      </Row>
    </AppLayout>
  );
}
