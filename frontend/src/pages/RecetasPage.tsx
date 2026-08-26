import { useEffect, useState } from 'react';
import { Button, Card, Empty, Flex, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import axios from 'axios';
import { AppLayout } from '../layout/AppLayout';
import { listarProductos } from '../api/productos';
import { listarInsumos } from '../api/inventario';
import { actualizarReceta, crearReceta, obtenerReceta } from '../api/produccion';
import { mensajeDeError } from '../api/client';
import type { Insumo, Producto } from '../types';

interface FilaReceta {
  key: string;
  insumoId?: number;
  cantidad: number;
}

export function RecetasPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [insumos, setInsumos] = useState<Insumo[]>([]);
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);

  const [productoId, setProductoId] = useState<number | undefined>();
  const [filas, setFilas] = useState<FilaReceta[]>([]);
  const [recetaExiste, setRecetaExiste] = useState(false);
  const [cargandoReceta, setCargandoReceta] = useState(false);
  const [guardando, setGuardando] = useState(false);

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
      setRecetaExiste(true);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setFilas([]);
        setRecetaExiste(false);
      } else {
        message.error(mensajeDeError(err, 'No se pudo cargar la receta'));
      }
    } finally {
      setCargandoReceta(false);
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

    setGuardando(true);
    try {
      const payload = items.map((f) => ({ insumoId: f.insumoId as number, cantidad: f.cantidad }));
      if (recetaExiste) {
        await actualizarReceta(productoId, payload);
      } else {
        await crearReceta(productoId, payload);
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
      title: 'Cantidad por unidad de producto',
      width: 260,
      render: (_, fila) => (
        <InputNumber
          min={0.001}
          step={0.1}
          style={{ width: '100%' }}
          value={fila.cantidad}
          onChange={(v) => actualizarFila(fila.key, { cantidad: v ?? 0 })}
        />
      ),
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
            </>
          )}
        </Space>
      </Card>
    </AppLayout>
  );
}
