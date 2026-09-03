import { useEffect, useState } from 'react';
import { Alert, Button, Card, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import axios from 'axios';
import { AppLayout } from '../layout/AppLayout';
import { listarProductos } from '../api/productos';
import { confirmarOrdenProduccion, obtenerReceta } from '../api/produccion';
import { mensajeDeError } from '../api/client';
import type { Producto, Receta, RecetaItem } from '../types';

export function ProduccionPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [cargandoProductos, setCargandoProductos] = useState(true);

  const [productoId, setProductoId] = useState<number | undefined>();
  const [receta, setReceta] = useState<Receta | null>(null);
  const [errorReceta, setErrorReceta] = useState<string | null>(null);
  const [cargandoReceta, setCargandoReceta] = useState(false);

  const [cantidad, setCantidad] = useState<number | null>(1);
  const [confirmando, setConfirmando] = useState(false);

  useEffect(() => {
    listarProductos()
      .then((prods) => setProductos(prods.filter((p) => p.tipo === 'ELABORADO')))
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los productos')))
      .finally(() => setCargandoProductos(false));
  }, []);

  async function handleSeleccionarProducto(id: number) {
    setProductoId(id);
    setReceta(null);
    setErrorReceta(null);
    setCargandoReceta(true);
    try {
      const r = await obtenerReceta(id);
      setReceta(r);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setErrorReceta('Este producto no tiene una receta cargada — no se puede producir.');
      } else {
        setErrorReceta(mensajeDeError(err, 'No se pudo cargar la receta'));
      }
    } finally {
      setCargandoReceta(false);
    }
  }

  async function handleConfirmar() {
    if (!productoId || !cantidad || cantidad <= 0) return;

    setConfirmando(true);
    try {
      const orden = await confirmarOrdenProduccion(productoId, cantidad);
      message.success(`Orden #${orden.id} confirmada — se sumaron ${orden.cantidad} unidades de stock`);
      setCantidad(1);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo confirmar la orden de producción'));
    } finally {
      setConfirmando(false);
    }
  }

  const columnas: ColumnsType<RecetaItem> = [
    { title: 'Insumo', dataIndex: 'insumoNombre' },
    { title: 'Por unidad', dataIndex: 'cantidad', width: 120 },
    {
      title: `Total a consumir (x${cantidad || 0})`,
      width: 200,
      render: (_, item) => `${(item.cantidad * (cantidad || 0)).toFixed(3)} ${item.unidadMedida.toLowerCase()}`,
    },
  ];

  return (
    <AppLayout>
      <Card title="Registrar orden de producción">
        <Space orientation="vertical" style={{ width: '100%' }} size="large">
          <Select
            showSearch
            placeholder="Elegí el producto a producir"
            style={{ width: 400 }}
            loading={cargandoProductos}
            value={productoId}
            onChange={handleSeleccionarProducto}
            optionFilterProp="label"
            options={productos.map((p) => ({ value: p.id, label: p.nombre }))}
          />

          {errorReceta && <Alert type="warning" title={errorReceta} showIcon />}

          {receta && !cargandoReceta && (
            <>
              <Space align="center">
                <Typography.Text>Cantidad a producir:</Typography.Text>
                <InputNumber min={0.001} step={1} value={cantidad} onChange={(v) => setCantidad(v)} />
              </Space>

              <Typography.Text type="secondary">Esto es lo que se va a descontar de insumos:</Typography.Text>
              <Table columns={columnas} dataSource={receta.items} rowKey="insumoId" pagination={false} />

              <Button
                type="primary"
                size="large"
                loading={confirmando}
                disabled={!cantidad || cantidad <= 0}
                onClick={handleConfirmar}
              >
                Confirmar producción
              </Button>
            </>
          )}
        </Space>
      </Card>
    </AppLayout>
  );
}
