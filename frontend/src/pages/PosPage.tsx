import { useEffect, useMemo, useRef, useState, type ComponentRef } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Flex,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Table,
  Typography,
  message,
} from 'antd';
import type { InputRef, RefSelectProps } from 'antd';
import { DesktopOutlined, DeleteOutlined, PlusOutlined, ScanOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import { AppLayout } from '../layout/AppLayout';
import { listarProductos } from '../api/productos';
import { crearCliente, listarClientes } from '../api/clientes';
import { obtenerCajaActual } from '../api/caja';
import { confirmarVenta, escanear, type ItemVentaPayload } from '../api/ventas';
import { mensajeDeError } from '../api/client';
import { MEDIOS_PAGO, type Caja, type Cliente, type MedioPago, type Producto } from '../types';
import { CANAL_PANTALLA_CLIENTE, type ItemPantallaCliente, type MensajePantallaCliente } from '../pos/pantallaCliente';

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
  const [montoManual, setMontoManual] = useState<number | null>(null);

  // Para que Enter vaya saltando de campo en campo: producto -> cantidad -> (monto, si es por
  // peso) -> agregar -> vuelve a producto, sin que el cajero tenga que tocar el mouse.
  const productoManualRef = useRef<RefSelectProps>(null);
  const cantidadManualRef = useRef<ComponentRef<typeof InputNumber>>(null);
  const montoManualRef = useRef<ComponentRef<typeof InputNumber>>(null);

  const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
  const [medioPago, setMedioPago] = useState<MedioPago>('EFECTIVO');
  const [confirmando, setConfirmando] = useState(false);

  const [clientesConCuenta, setClientesConCuenta] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<number | undefined>();

  const [nuevoClienteAbierto, setNuevoClienteAbierto] = useState(false);
  const [nombreNuevoCliente, setNombreNuevoCliente] = useState('');
  const [telefonoNuevoCliente, setTelefonoNuevoCliente] = useState('');
  const [creandoCliente, setCreandoCliente] = useState(false);

  const [cajaActual, setCajaActual] = useState<Caja | null>(null);
  const [cargandoCaja, setCargandoCaja] = useState(true);

  // El lector láser escribe donde esté el foco y termina con Enter, como un teclado. Si el
  // foco se queda en un botón después de agregar un ítem, el siguiente escaneo se pierde —
  // por eso hay que devolverlo acá después de cada acción (éxito, error, o al confirmar).
  const inputCodigoRef = useRef<InputRef>(null);

  function enfocarInputCodigo() {
    setTimeout(() => inputCodigoRef.current?.focus(), 0);
  }

  // Canal de comunicación con la pantalla del cliente (segunda ventana, ver PantallaClientePage).
  // No pasa por el backend, es puramente entre pestañas del mismo navegador. Se crea/cierra en
  // un useEffect (no directo en el cuerpo del componente) para llevarse bien con el StrictMode
  // de desarrollo, que monta-desmonta-remonta los efectos: si el canal se crea afuera, la
  // primera "desmontada" simulada lo cierra para siempre y las siguientes postMessage tiran
  // InvalidStateError.
  const canalPantallaClienteRef = useRef<BroadcastChannel | null>(null);

  function cargarClientesConCuenta() {
    return listarClientes()
      .then((clientes) => setClientesConCuenta(clientes.filter((c) => c.tieneCuentaCorriente)))
      .catch(() => {
        // La cuenta corriente es opcional en el flujo de venta: si no se pueden cargar los
        // clientes, simplemente no se ofrece esa opción, no hace falta romper la pantalla.
      });
  }

  useEffect(() => {
    listarProductos()
      .then(setProductos)
      .catch((err) => setErrorProductos(mensajeDeError(err, 'No se pudieron cargar los productos')))
      .finally(() => setCargandoProductos(false));

    cargarClientesConCuenta();

    obtenerCajaActual()
      .then(setCajaActual)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo consultar la caja actual')))
      .finally(() => setCargandoCaja(false));
  }, []);

  function abrirNuevoCliente() {
    setNombreNuevoCliente('');
    setTelefonoNuevoCliente('');
    setNuevoClienteAbierto(true);
  }

  async function handleCrearCliente() {
    if (!nombreNuevoCliente.trim()) {
      message.warning('Ingresá el nombre del cliente');
      return;
    }

    setCreandoCliente(true);
    try {
      const cliente = await crearCliente({
        nombre: nombreNuevoCliente.trim(),
        telefono: telefonoNuevoCliente.trim() || null,
        tieneCuentaCorriente: true,
      });
      await cargarClientesConCuenta();
      setClienteId(cliente.id);
      setNuevoClienteAbierto(false);
      message.success(`Cliente "${cliente.nombre}" creado`);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo crear el cliente'));
    } finally {
      setCreandoCliente(false);
    }
  }

  const total = useMemo(
    () => carrito.reduce((acc, item) => acc + (item.cantidad ?? 0) * item.precioUnitario, 0),
    [carrito],
  );

  const itemsParaPantallaCliente = useMemo<ItemPantallaCliente[]>(
    () =>
      carrito.map((item) => ({
        productoNombre: item.productoNombre,
        cantidad: item.cantidad ?? 0,
        seVendePorPeso: item.seVendePorPeso,
        unidadMedida: item.unidadMedida,
        precioUnitario: item.precioUnitario,
        subtotal: (item.cantidad ?? 0) * item.precioUnitario,
      })),
    [carrito],
  );

  // Crea el canal al montar y lo cierra al desmontar — único lugar que toca el ciclo de vida
  // del canal en sí (los demás efectos solo lo usan a través del ref).
  useEffect(() => {
    const canal = new BroadcastChannel(CANAL_PANTALLA_CLIENTE);
    canalPantallaClienteRef.current = canal;
    return () => {
      canal.close();
      canalPantallaClienteRef.current = null;
    };
  }, []);

  // Cada cambio en el carrito (agregar/quitar ítem, cambiar cantidad) se transmite tal cual a la
  // pantalla del cliente, si hay una abierta escuchando.
  useEffect(() => {
    canalPantallaClienteRef.current?.postMessage({
      tipo: 'actualizar',
      items: itemsParaPantallaCliente,
      total,
    } satisfies MensajePantallaCliente);
  }, [itemsParaPantallaCliente, total]);

  // Si la pantalla del cliente se abre (o se recarga) a mitad de una venta, pide el estado
  // actual en vez de quedarse vacía hasta el próximo ítem escaneado.
  useEffect(() => {
    const canal = canalPantallaClienteRef.current;
    if (!canal) return;

    const onMessage = (event: MessageEvent<MensajePantallaCliente>) => {
      if (event.data.tipo === 'solicitar-estado') {
        canalPantallaClienteRef.current?.postMessage({
          tipo: 'actualizar',
          items: itemsParaPantallaCliente,
          total,
        } satisfies MensajePantallaCliente);
      }
    };

    canal.addEventListener('message', onMessage);
    return () => canal.removeEventListener('message', onMessage);
  }, [itemsParaPantallaCliente, total]);

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

  function handleSeleccionarProductoManual(id: number) {
    const producto = productos.find((p) => p.id === id);
    setProductoManualId(id);
    setCantidadManual(1);
    setMontoManual(producto?.seVendePorPeso ? Math.round(producto.precioVenta * 100) / 100 : null);
    setTimeout(() => cantidadManualRef.current?.focus(), 0);
  }

  // Los campos de cantidad y monto se mantienen sincronizados en los dos sentidos: cambiar uno
  // recalcula el otro contra el precio por kg, así el cajero puede usar el que le resulte más
  // cómodo según lo que le pida el cliente ("medio kilo" o "$3000 de queso").
  function handleCambiarCantidadManual(v: number | null) {
    setCantidadManual(v);
    if (productoManualSeleccionado?.seVendePorPeso) {
      setMontoManual(v !== null ? Math.round(v * productoManualSeleccionado.precioVenta * 100) / 100 : null);
    }
  }

  function handleCambiarMontoManual(v: number | null) {
    setMontoManual(v);
    if (productoManualSeleccionado?.seVendePorPeso) {
      setCantidadManual(
        v !== null ? Math.round((v / productoManualSeleccionado.precioVenta) * 1000) / 1000 : null,
      );
    }
  }

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
    setMontoManual(null);
    productoManualRef.current?.focus();
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
      canalPantallaClienteRef.current?.postMessage({
        tipo: 'venta-confirmada',
        total: venta.total,
      } satisfies MensajePantallaCliente);
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
            <Flex vertical gap={8} style={{ marginTop: 8 }}>
              <Select
                ref={productoManualRef}
                showSearch
                placeholder="Producto"
                loading={cargandoProductos}
                value={productoManualId}
                onChange={handleSeleccionarProductoManual}
                optionFilterProp="label"
                options={productos.map((p) => ({
                  value: p.id,
                  label: `${p.nombre} — ${formatoMoneda.format(p.precioVenta)}`,
                }))}
              />

              <Flex gap={8}>
                <InputNumber
                  ref={cantidadManualRef}
                  min={productoManualSeleccionado?.seVendePorPeso ? 0.001 : 1}
                  step={productoManualSeleccionado?.seVendePorPeso ? 0.1 : 1}
                  style={{ flex: 1 }}
                  placeholder={productoManualSeleccionado?.seVendePorPeso ? 'Peso en kg' : 'Cantidad'}
                  value={cantidadManual}
                  onChange={handleCambiarCantidadManual}
                  onPressEnter={() =>
                    productoManualSeleccionado?.seVendePorPeso
                      ? montoManualRef.current?.focus()
                      : handleAgregarManual()
                  }
                />
                {productoManualSeleccionado?.seVendePorPeso && (
                  <InputNumber
                    ref={montoManualRef}
                    min={0.01}
                    step={100}
                    style={{ flex: 1 }}
                    placeholder="Monto en $"
                    prefix="$"
                    value={montoManual}
                    onChange={handleCambiarMontoManual}
                    onPressEnter={handleAgregarManual}
                  />
                )}
                <Button onClick={handleAgregarManual} disabled={!productoManualId}>
                  Agregar
                </Button>
              </Flex>
            </Flex>
          </Card>
        </Col>

        <Col span={14}>
          <Card
            title="Carrito"
            extra={
              <Button
                icon={<DesktopOutlined />}
                onClick={() => window.open('/pantalla-cliente', 'pantallaCliente', 'noopener')}
              >
                Pantalla del cliente
              </Button>
            }
          >
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
                  <>
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
                    <Button icon={<PlusOutlined />} onClick={abrirNuevoCliente}>
                      Nuevo cliente
                    </Button>
                  </>
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

      <Modal
        title="Nuevo cliente"
        open={nuevoClienteAbierto}
        onCancel={() => setNuevoClienteAbierto(false)}
        onOk={handleCrearCliente}
        confirmLoading={creandoCliente}
      >
        <Typography.Text>Nombre</Typography.Text>
        <Input
          value={nombreNuevoCliente}
          onChange={(e) => setNombreNuevoCliente(e.target.value)}
          style={{ marginTop: 8, marginBottom: 16 }}
          autoFocus
        />
        <Typography.Text>Teléfono (opcional)</Typography.Text>
        <Input
          value={telefonoNuevoCliente}
          onChange={(e) => setTelefonoNuevoCliente(e.target.value)}
          onPressEnter={handleCrearCliente}
          style={{ marginTop: 8 }}
        />
        <Typography.Paragraph type="secondary" style={{ marginTop: 16, marginBottom: 0 }}>
          Se crea con la cuenta corriente ya habilitada, para poder cargarle esta venta.
        </Typography.Paragraph>
      </Modal>
    </AppLayout>
  );
}
