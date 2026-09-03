import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Divider, Empty, Flex, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import axios from 'axios';
import { AppLayout } from '../layout/AppLayout';
import { actualizarProducto, listarProductos } from '../api/productos';
import { listarInsumos } from '../api/inventario';
import { actualizarReceta, crearReceta, obtenerReceta } from '../api/produccion';
import { mensajeDeError } from '../api/client';
import { ABREVIATURA_UNIDAD_MEDIDA } from '../types';
import type { Insumo, Producto } from '../types';

interface FilaReceta {
  key: string;
  insumoId?: number;
  cantidad: number | null;
}

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function RecetasPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [insumos, setInsumos] = useState<Insumo[]>([]);
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);

  const [productoId, setProductoId] = useState<number | undefined>();
  const [filas, setFilas] = useState<FilaReceta[]>([]);
  const [rendimiento, setRendimiento] = useState<number | null>(1);
  const [recetaExiste, setRecetaExiste] = useState(false);
  const [cargandoReceta, setCargandoReceta] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const [margenDeseado, setMargenDeseado] = useState<number | null>(30);
  const [aplicandoPrecio, setAplicandoPrecio] = useState(false);

  useEffect(() => {
    Promise.all([listarProductos(), listarInsumos()])
      .then(([prods, ins]) => {
        setProductos(prods.filter((p) => p.tipo === 'ELABORADO'));
        setInsumos(ins);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar productos/insumos')))
      .finally(() => setCargandoCatalogos(false));
  }, []);

  async function handleSeleccionarProducto(id: number) {
    setProductoId(id);
    setCargandoReceta(true);
    try {
      const receta = await obtenerReceta(id);
      setFilas(receta.items.map((item) => ({ key: crypto.randomUUID(), insumoId: item.insumoId, cantidad: item.cantidad })));
      setRendimiento(receta.rendimiento);
      setRecetaExiste(true);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setFilas([]);
        setRendimiento(1);
        setRecetaExiste(false);
      } else {
        message.error(mensajeDeError(err, 'No se pudo cargar la receta'));
      }
    } finally {
      setCargandoReceta(false);
    }
  }

  const productoSeleccionado = productos.find((p) => p.id === productoId);
  const unidadProducto = productoSeleccionado ? ABREVIATURA_UNIDAD_MEDIDA[productoSeleccionado.unidadMedida] : '';

  const costoUnitarioPorInsumo = useMemo(
    () => new Map(insumos.map((i) => [i.id, i.costoUnitario])),
    [insumos],
  );

  // Costo de insumos de la tanda completa, tal como está cargada la receta (no todavía el costo
  // de una unidad vendible: para eso hay que dividirlo por el rendimiento).
  const costoTotal = useMemo(
    () =>
      filas.reduce((acc, fila) => {
        if (fila.insumoId === undefined) return acc;
        const costoUnitario = costoUnitarioPorInsumo.get(fila.insumoId) ?? 0;
        return acc + costoUnitario * (fila.cantidad ?? 0);
      }, 0),
    [filas, costoUnitarioPorInsumo],
  );

  const costoPorUnidad = rendimiento && rendimiento > 0 ? costoTotal / rendimiento : null;

  // Margen como markup sobre el costo (no como % del precio, que es lo que usa el reporte de
  // margen por producto) -- así puede superar el 100% sin problema: un producto que se vende
  // al triple de su costo tiene 200% de margen sobre costo, no "un margen imposible".
  const margenActual =
    productoSeleccionado && costoPorUnidad !== null && costoPorUnidad > 0
      ? ((productoSeleccionado.precioVenta - costoPorUnidad) / costoPorUnidad) * 100
      : null;

  const precioSugerido =
    costoPorUnidad !== null && costoPorUnidad > 0 && margenDeseado !== null
      ? costoPorUnidad * (1 + margenDeseado / 100)
      : null;

  async function handleAplicarPrecio() {
    if (!productoSeleccionado || precioSugerido === null) return;

    setAplicandoPrecio(true);
    try {
      const actualizado = await actualizarProducto(productoSeleccionado.id, {
        nombre: productoSeleccionado.nombre,
        categoriaId: productoSeleccionado.categoriaId,
        tipo: productoSeleccionado.tipo,
        seVendePorPeso: productoSeleccionado.seVendePorPeso,
        precioVenta: Math.round(precioSugerido * 100) / 100,
        unidadMedida: productoSeleccionado.unidadMedida,
        codigoBarras: productoSeleccionado.codigoBarras,
        codigoPLU: productoSeleccionado.codigoPLU,
        stockMinimo: productoSeleccionado.stockMinimo,
        activo: productoSeleccionado.activo,
      });
      setProductos((prev) => prev.map((p) => (p.id === actualizado.id ? actualizado : p)));
      message.success(`Precio de venta actualizado a ${formatoMoneda.format(actualizado.precioVenta)}`);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo actualizar el precio de venta'));
    } finally {
      setAplicandoPrecio(false);
    }
  }

  function agregarFila() {
    setFilas((prev) => [...prev, { key: crypto.randomUUID(), insumoId: undefined, cantidad: 1 }]);
  }

  function quitarFila(key: string) {
    setFilas((prev) => prev.filter((fila) => fila.key !== key));
  }

  function actualizarFila(key: string, cambios: Partial<FilaReceta>) {
    setFilas((prev) => prev.map((fila) => (fila.key === key ? { ...fila, ...cambios } : fila)));
  }

  async function handleGuardar() {
    if (!productoId) return;

    const items = filas.filter((f) => f.insumoId !== undefined);
    if (items.length === 0) {
      message.warning('Agregá al menos un insumo a la receta');
      return;
    }

    const insumoIds = items.map((f) => f.insumoId);
    if (new Set(insumoIds).size !== insumoIds.length) {
      message.error('No se puede repetir el mismo insumo en más de un ítem');
      return;
    }

    if (items.some((f) => !f.cantidad || f.cantidad <= 0)) {
      message.warning('Todos los ítems necesitan una cantidad mayor a 0');
      return;
    }

    if (!rendimiento || rendimiento <= 0) {
      message.warning('El rendimiento tiene que ser mayor a 0');
      return;
    }

    setGuardando(true);
    try {
      const payload = items.map((f) => ({ insumoId: f.insumoId as number, cantidad: f.cantidad as number }));
      if (recetaExiste) {
        await actualizarReceta(productoId, rendimiento, payload);
      } else {
        await crearReceta(productoId, rendimiento, payload);
      }
      setRecetaExiste(true);
      message.success('Receta guardada');
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar la receta'));
    } finally {
      setGuardando(false);
    }
  }

  const columnas: ColumnsType<FilaReceta> = [
    {
      title: 'Insumo',
      render: (_, fila) => (
        <Select
          showSearch
          placeholder="Insumo"
          style={{ width: '100%' }}
          value={fila.insumoId}
          onChange={(v) => actualizarFila(fila.key, { insumoId: v })}
          optionFilterProp="label"
          options={insumos.map((i) => ({ value: i.id, label: `${i.nombre} (${i.unidadMedida})` }))}
        />
      ),
    },
    {
      title: 'Cantidad (para toda la receta)',
      width: 260,
      render: (_, fila) => (
        <InputNumber
          min={0.001}
          step={0.1}
          style={{ width: '100%' }}
          value={fila.cantidad}
          onChange={(v) => actualizarFila(fila.key, { cantidad: v })}
        />
      ),
    },
    {
      title: 'Costo',
      width: 140,
      render: (_, fila) => {
        if (fila.insumoId === undefined) return '—';
        const costoUnitario = costoUnitarioPorInsumo.get(fila.insumoId) ?? 0;
        return formatoMoneda.format(costoUnitario * (fila.cantidad ?? 0));
      },
    },
    {
      title: '',
      width: 50,
      render: (_, fila) => (
        <Button danger type="text" icon={<DeleteOutlined />} onClick={() => quitarFila(fila.key)} />
      ),
    },
  ];

  return (
    <AppLayout>
      <Card title="Recetas">
        <Space orientation="vertical" style={{ width: '100%' }} size="large">
          <Select
            showSearch
            placeholder="Elegí un producto elaborado"
            style={{ width: 400 }}
            loading={cargandoCatalogos}
            value={productoId}
            onChange={handleSeleccionarProducto}
            optionFilterProp="label"
            options={productos.map((p) => ({ value: p.id, label: p.nombre }))}
          />

          {productoId && !cargandoReceta && (
            <>
              {!recetaExiste && (
                <Typography.Text type="secondary">
                  Este producto todavía no tiene receta cargada — armala agregando ítems.
                </Typography.Text>
              )}

              <Flex align="center" gap={8}>
                <Typography.Text>
                  Rendimiento — cuánto (en {unidadProducto}) da esta receta tal como está cargada abajo:
                </Typography.Text>
                <InputNumber
                  min={0.001}
                  step={1}
                  style={{ width: 120 }}
                  value={rendimiento}
                  onChange={(v) => setRendimiento(v)}
                  addonAfter={unidadProducto}
                />
              </Flex>

              <Table
                columns={columnas}
                dataSource={filas}
                rowKey="key"
                pagination={false}
                locale={{ emptyText: <Empty description="Sin ítems" /> }}
              />

              <Flex justify="space-between">
                <Button icon={<PlusOutlined />} onClick={agregarFila}>
                  Agregar ítem
                </Button>
                <Button type="primary" loading={guardando} onClick={handleGuardar}>
                  Guardar receta
                </Button>
              </Flex>

              {filas.some((f) => f.insumoId !== undefined) && (
                <>
                  <Divider style={{ margin: '8px 0' }} />

                  <Flex justify="space-between" align="flex-start" wrap="wrap" gap={24}>
                    <div>
                      <Typography.Text type="secondary">Costo de insumos de toda la tanda</Typography.Text>
                      <Typography.Title level={4} style={{ margin: '4px 0 0' }}>
                        {formatoMoneda.format(costoTotal)}
                      </Typography.Title>
                      <Typography.Text type="secondary">
                        Costo por {productoSeleccionado?.unidadMedida === 'UNIDAD' ? 'unidad' : unidadProducto}{' '}
                        vendible:{' '}
                        {costoPorUnidad !== null ? formatoMoneda.format(costoPorUnidad) : '—'}
                      </Typography.Text>
                      <br />
                      {productoSeleccionado && (
                        <Typography.Text type="secondary">
                          Precio de venta actual: {formatoMoneda.format(productoSeleccionado.precioVenta)}
                          {margenActual !== null && ` (margen sobre costo actual: ${margenActual.toFixed(1)}%)`}
                        </Typography.Text>
                      )}
                    </div>

                    <Flex vertical gap={4} align="flex-end">
                      <Flex align="center" gap={8}>
                        <Typography.Text>Margen deseado (sobre costo)</Typography.Text>
                        <InputNumber
                          min={0}
                          step={5}
                          suffix="%"
                          style={{ width: 110 }}
                          value={margenDeseado}
                          onChange={(v) => setMargenDeseado(v)}
                        />
                      </Flex>
                      <Typography.Text type="secondary">
                        Precio sugerido: {precioSugerido !== null ? formatoMoneda.format(precioSugerido) : '—'}
                      </Typography.Text>
                      <Button
                        onClick={handleAplicarPrecio}
                        loading={aplicandoPrecio}
                        disabled={precioSugerido === null}
                      >
                        Aplicar como precio de venta
                      </Button>
                    </Flex>
                  </Flex>
                </>
              )}
            </>
          )}
        </Space>
      </Card>
    </AppLayout>
  );
}
