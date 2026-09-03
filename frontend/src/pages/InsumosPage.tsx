import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Flex, Form, InputNumber, Input, Modal, Select, Table, message } from 'antd';
import { EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { useAuth } from '../auth/AuthContext';
import {
  actualizarInsumo,
  crearInsumo,
  listarInsumos,
  registrarMovimientoStock,
  type InsumoPayload,
} from '../api/inventario';
import { mensajeDeError } from '../api/client';
import type { Insumo } from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

export function InsumosPage() {
  const { hasRole } = useAuth();
  const puedeEditar = hasRole('DUENO', 'ENCARGADO');

  const [insumos, setInsumos] = useState<Insumo[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [insumoEditando, setInsumoEditando] = useState<Insumo | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [form] = Form.useForm<InsumoPayload>();

  const [stockActualNuevo, setStockActualNuevo] = useState<number | null>(null);

  function cargarInsumos() {
    setCargando(true);
    listarInsumos()
      .then(setInsumos)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los insumos')))
      .finally(() => setCargando(false));
  }

  useEffect(cargarInsumos, []);

  const insumosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto ? insumos.filter((i) => i.nombre.toLowerCase().includes(texto)) : insumos;
  }, [insumos, busqueda]);

  function abrirNuevo() {
    setInsumoEditando(null);
    setStockActualNuevo(null);
    form.resetFields();
    form.setFieldsValue({ unidadMedida: 'KG', stockMinimo: 0, costoUnitario: 0 });
    setModalAbierto(true);
  }

  function abrirEdicion(insumo: Insumo) {
    setInsumoEditando(insumo);
    setStockActualNuevo(insumo.stockActual);
    form.setFieldsValue({
      nombre: insumo.nombre,
      unidadMedida: insumo.unidadMedida,
      stockMinimo: insumo.stockMinimo,
      costoUnitario: insumo.costoUnitario,
    });
    setModalAbierto(true);
  }

  async function handleGuardar() {
    const values = await form.validateFields();
    setGuardando(true);
    try {
      if (insumoEditando) {
        await actualizarInsumo(insumoEditando.id, values);
        const delta = stockActualNuevo !== null ? stockActualNuevo - insumoEditando.stockActual : 0;
        if (delta !== 0) {
          await registrarMovimientoStock({
            itemTipo: 'INSUMO',
            itemId: insumoEditando.id,
            tipo: 'AJUSTE',
            cantidad: delta,
            motivo: 'Corrección manual de stock desde edición de insumo',
          });
        }
        message.success('Insumo actualizado');
      } else {
        await crearInsumo(values);
        message.success('Insumo creado');
      }
      setModalAbierto(false);
      cargarInsumos();
    } catch (err) {
      message.error(mensajeDeError(err, 'No se pudo guardar el insumo'));
    } finally {
      setGuardando(false);
    }
  }

  const columnas: ColumnsType<Insumo> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Unidad', dataIndex: 'unidadMedida' },
    { title: 'Stock actual', dataIndex: 'stockActual' },
    { title: 'Stock mínimo', dataIndex: 'stockMinimo' },
    {
      title: 'Costo unitario',
      dataIndex: 'costoUnitario',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    ...(puedeEditar
      ? [
          {
            title: '',
            width: 60,
            render: (_: unknown, insumo: Insumo) => (
              <Button type="text" icon={<EditOutlined />} onClick={() => abrirEdicion(insumo)} />
            ),
          },
        ]
      : []),
  ];

  return (
    <AppLayout>
      <Card
        title="Insumos"
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
            {puedeEditar && (
              <Button type="primary" icon={<PlusOutlined />} onClick={abrirNuevo}>
                Nuevo insumo
              </Button>
            )}
          </Flex>
        }
      >
        <Table
          columns={columnas}
          dataSource={insumosFiltrados}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: busqueda ? 'Ningún insumo coincide con la búsqueda' : 'Sin insumos' }}
        />
      </Card>

      <Modal
        title={insumoEditando ? 'Editar insumo' : 'Nuevo insumo'}
        open={modalAbierto}
        onCancel={() => setModalAbierto(false)}
        onOk={handleGuardar}
        confirmLoading={guardando}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="nombre" label="Nombre" rules={[{ required: true, message: 'Ingresá el nombre' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="unidadMedida" label="Unidad de medida" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'KG', label: 'Kilogramo' },
                { value: 'GRAMO', label: 'Gramo' },
                { value: 'LITRO', label: 'Litro' },
                { value: 'UNIDAD', label: 'Unidad' },
              ]}
            />
          </Form.Item>
          <Form.Item name="stockMinimo" label="Stock mínimo (alerta de stock crítico)" rules={[{ required: true }]}>
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="costoUnitario" label="Costo unitario" rules={[{ required: true }]}>
            <InputNumber min={0} step={10} style={{ width: '100%' }} />
          </Form.Item>

          {insumoEditando && (
            <Form.Item label="Stock actual (corregir manualmente si hubo un error de carga o venta)">
              <InputNumber
                min={0}
                step={1}
                style={{ width: '100%' }}
                value={stockActualNuevo}
                onChange={(v) => setStockActualNuevo(v)}
              />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </AppLayout>
  );
}
