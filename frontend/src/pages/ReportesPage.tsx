import { useEffect, useState } from 'react';
import { Card, DatePicker, Space, Statistic, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { AppLayout } from '../layout/AppLayout';
import {
  obtenerMargenProductos,
  obtenerProductosMasVendidos,
  obtenerReporteVentas,
  obtenerStockCritico,
} from '../api/reportes';
import { mensajeDeError } from '../api/client';
import type {
  MargenProducto,
  ProductoMasVendido,
  ReporteVentas,
  StockCritico,
  StockCriticoItem,
  VentaDia,
} from '../types';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const FORMATO_FECHA = 'YYYY-MM-DD';

export function ReportesPage() {
  const [rango, setRango] = useState<[Dayjs, Dayjs]>([dayjs().subtract(30, 'day'), dayjs()]);

  const [reporteVentas, setReporteVentas] = useState<ReporteVentas | null>(null);
  const [productosMasVendidos, setProductosMasVendidos] = useState<ProductoMasVendido[]>([]);
  const [margenProductos, setMargenProductos] = useState<MargenProducto[]>([]);
  const [stockCritico, setStockCritico] = useState<StockCritico | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const [desde, hasta] = rango;
    setCargando(true);

    Promise.all([
      obtenerReporteVentas(desde.format(FORMATO_FECHA), hasta.format(FORMATO_FECHA)),
      obtenerProductosMasVendidos(desde.format(FORMATO_FECHA), hasta.format(FORMATO_FECHA)),
      obtenerMargenProductos(),
      obtenerStockCritico(),
    ])
      .then(([ventas, masVendidos, margenes, critico]) => {
        setReporteVentas(ventas);
        setProductosMasVendidos(masVendidos);
        setMargenProductos(margenes);
        setStockCritico(critico);
      })
      .catch((err) => message.error(mensajeDeError(err, 'No se pudieron cargar los reportes')))
      .finally(() => setCargando(false));
  }, [rango]);

  const columnasPorDia: ColumnsType<VentaDia> = [
    { title: 'Fecha', dataIndex: 'fecha' },
    { title: 'Cantidad de ventas', dataIndex: 'cantidadVentas' },
    {
      title: 'Total vendido',
      dataIndex: 'totalVendido',
      render: (valor: number) => formatoMoneda.format(valor),
    },
  ];

  const columnasMasVendidos: ColumnsType<ProductoMasVendido> = [
    { title: 'Producto', dataIndex: 'productoNombre' },
    { title: 'Cantidad vendida', dataIndex: 'cantidadVendida' },
    {
      title: 'Monto total',
      dataIndex: 'montoTotal',
      render: (valor: number) => formatoMoneda.format(valor),
    },
  ];

  const columnasMargen: ColumnsType<MargenProducto> = [
    { title: 'Producto', dataIndex: 'productoNombre' },
    {
      title: 'Precio de venta',
      dataIndex: 'precioVenta',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Costo de insumos',
      dataIndex: 'costoInsumos',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Margen',
      dataIndex: 'margen',
      render: (valor: number) => formatoMoneda.format(valor),
    },
    {
      title: 'Margen %',
      dataIndex: 'margenPorcentual',
      render: (valor: number) => `${valor.toFixed(1)}%`,
    },
  ];

  const columnasStockCritico: ColumnsType<StockCriticoItem> = [
    { title: 'Nombre', dataIndex: 'nombre' },
    { title: 'Stock actual', dataIndex: 'stockActual' },
    { title: 'Stock mínimo', dataIndex: 'stockMinimo' },
    {
      title: '',
      render: () => <Tag color="red">Crítico</Tag>,
    },
  ];

  return (
    <AppLayout>
      <Card
        title="Reportes"
        extra={
          <DatePicker.RangePicker
            value={rango}
            format="DD/MM/YYYY"
            allowClear={false}
            onChange={(valores) => {
              if (valores && valores[0] && valores[1]) {
                setRango([valores[0], valores[1]]);
              }
            }}
          />
        }
      >
        <Tabs
          items={[
            {
              key: 'ventas',
              label: 'Ventas por período',
              children: (
                <Space orientation="vertical" style={{ width: '100%' }} size="large">
                  <Space size="large">
                    <Statistic title="Cantidad de ventas" value={reporteVentas?.cantidadVentas ?? 0} />
                    <Statistic
                      title="Total vendido"
                      value={reporteVentas ? formatoMoneda.format(reporteVentas.totalVendido) : '-'}
                    />
                    <Statistic
                      title="Promedio por venta"
                      value={reporteVentas ? formatoMoneda.format(reporteVentas.promedioPorVenta) : '-'}
                    />
                  </Space>
                  <Table
                    columns={columnasPorDia}
                    dataSource={reporteVentas?.porDia ?? []}
                    rowKey="fecha"
                    loading={cargando}
                    pagination={false}
                  />
                </Space>
              ),
            },
            {
              key: 'masVendidos',
              label: 'Productos más vendidos',
              children: (
                <Table
                  columns={columnasMasVendidos}
                  dataSource={productosMasVendidos}
                  rowKey="productoId"
                  loading={cargando}
                  pagination={false}
                />
              ),
            },
            {
              key: 'margen',
              label: 'Margen por producto',
              children: (
                <>
                  <Typography.Text type="secondary">
                    Solo incluye productos elaborados con receta cargada.
                  </Typography.Text>
                  <Table
                    columns={columnasMargen}
                    dataSource={margenProductos}
                    rowKey="productoId"
                    loading={cargando}
                    pagination={false}
                    style={{ marginTop: 12 }}
                  />
                </>
              ),
            },
            {
              key: 'stockCritico',
              label: 'Stock crítico',
              children: (
                <Space orientation="vertical" style={{ width: '100%' }} size="large">
                  <div>
                    <Typography.Title level={5}>Productos</Typography.Title>
                    <Table
                      columns={columnasStockCritico}
                      dataSource={stockCritico?.productos ?? []}
                      rowKey="id"
                      loading={cargando}
                      pagination={false}
                      locale={{ emptyText: 'Sin productos en stock crítico' }}
                    />
                  </div>
                  <div>
                    <Typography.Title level={5}>Insumos</Typography.Title>
                    <Table
                      columns={columnasStockCritico}
                      dataSource={stockCritico?.insumos ?? []}
                      rowKey="id"
                      loading={cargando}
                      pagination={false}
                      locale={{ emptyText: 'Sin insumos en stock crítico' }}
                    />
                  </div>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </AppLayout>
  );
}
