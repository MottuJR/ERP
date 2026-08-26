import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Flex, InputNumber, Select, Space, Table, Typography, message } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { confirmarCompra, listarProveedores } from '../api/compras';
import { listarInsumos } from '../api/inventario';
import { mensajeDeError } from '../api/client';
import type { Insumo, Proveedor } from '../types';

interface FilaCompra {
  key: string;
  insumoId?: number;
  cantidad: number;
  costoUnitario: number;
}

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function ComprasPage() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [insumos, setInsumos] = useState<Insumo[]>([]);
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);

  const [proveedorId, setProveedorId] = useState<number | undefined>();
  const [filas, setFilas] = useState<FilaCompra[]>([]);
  const [confirmando, setConfirmando] = useState(false);

  useEffect(() => {
    Promise.all([listarProveedores(), listarInsumos()])
      .then(([provs, ins]) => {
        setProveedores(provs);
        setInsumos(ins);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar proveedores/insumos')))
      .finally(() => setCargandoCatalogos(false));
  }, []);

  const total = useMemo(
    () => filas.reduce((acc, f) => acc + f.cantidad * f.costoUnitario, 0),
    [filas],
  );

  function agregarFila() {
    setFilas((prev) => [...prev, { key: crypto.randomUUID(), insumoId: undefined, cantidad: 1, costoUnitario: 0 }]);
  }

  function quitarFila(key: string) {
    setFilas((prev) => prev.filter((f) => f.key !== key));
  }

  function actualizarFila(key: string, cambios: Partial<FilaCompra>) {
    setFilas((prev) => prev.map((f) => (f.key === key ? { ...f, ...cambios } : f)));
  }

  async function handleConfirmar() {
    if (!proveedorId) {
      message.warning('Elegí un proveedor');
      return;
    }

    const items = filas.filter((f) => f.insumoId !== undefined && f.cantidad > 0 && f.costoUnitario > 0);
    if (items.length === 0) {
      message.warning('Agregá al menos un ítem con insumo, cantidad y costo');
      return;
    }

    setConfirmando(true);
    try {
      const compra = await confirmarCompra(
        proveedorId,
        items.map((f) => ({ insumoId: f.insumoId as number, cantidad: f.cantidad, costoUnitario: f.costoUnitario })),
      );
      message.success(`Compra #${compra.id} confirmada — total ${formatoMoneda.format(compra.total)}`);
      setFilas([]);
      setProveedorId(undefined);
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo confirmar la compra'));
    } finally {
      setConfirmando(false);
    }
  }

  const columnas: ColumnsType<FilaCompra> = [
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
      title: 'Cantidad',
      width: 140,
      render: (_, fila) => (
        <InputNumber
          min={0.001}
          step={1}
          style={{ width: '100%' }}
          value={fila.cantidad}
          onChange={(v) => actualizarFila(fila.key, { cantidad: v ?? 0 })}
        />
      ),
    },
    {
      title: 'Costo unitario',
      width: 160,
      render: (_, fila) => (
        <InputNumber
          min={0.01}
          step={10}
          style={{ width: '100%' }}
          value={fila.costoUnitario}
          onChange={(v) => actualizarFila(fila.key, { costoUnitario: v ?? 0 })}
        />
      ),
    },
    {
      title: 'Subtotal',
      width: 140,
      render: (_, fila) => formatoMoneda.format(fila.cantidad * fila.costoUnitario),
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
      <Card title="Cargar compra">
        <Space orientation="vertical" style={{ width: '100%' }} size="large">
          <Select
            showSearch
            placeholder="Proveedor"
            style={{ width: 400 }}
            loading={cargandoCatalogos}
            value={proveedorId}
            onChange={setProveedorId}
            optionFilterProp="label"
            options={proveedores.map((p) => ({ value: p.id, label: p.nombre }))}
          />

          <Table
            columns={columnas}
            dataSource={filas}
            rowKey="key"
            pagination={false}
            locale={{ emptyText: 'Todavía no agregaste ningún ítem' }}
          />

          <Flex justify="space-between" align="center">
            <Button icon={<PlusOutlined />} onClick={agregarFila}>
              Agregar ítem
            </Button>
            <Typography.Title level={4} style={{ margin: 0 }}>
              Total: {formatoMoneda.format(total)}
            </Typography.Title>
          </Flex>

          <Button type="primary" size="large" loading={confirmando} onClick={handleConfirmar}>
            Confirmar compra
          </Button>
        </Space>
      </Card>
    </AppLayout>
  );
}
