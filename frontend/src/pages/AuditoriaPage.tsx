import { useEffect, useState } from 'react';
import { Card, Select, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { AppLayout } from '../layout/AppLayout';
import { listarAuditoria, type RegistroAuditoria } from '../api/auditoria';
import { mensajeDeError } from '../api/client';

// Backend normalizado para usar un único casing por entidad (antes "Producto" y "PRODUCTO"
// convivían por venir de dos lugares distintos — altas/bajas vs. ajustes de stock — y el filtro
// mostraba la misma entidad duplicada).
const ENTIDADES = ['Producto', 'Insumo', 'Caja', 'Cliente'];

export function AuditoriaPage() {
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([]);
  const [entidad, setEntidad] = useState<string | undefined>();
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    setCargando(true);
    listarAuditoria(entidad)
      .then(setRegistros)
      .catch((err) => message.error(mensajeDeError(err, 'No se pudo cargar la auditoría')))
      .finally(() => setCargando(false));
  }, [entidad]);

  const columnas: ColumnsType<RegistroAuditoria> = [
    {
      title: 'Fecha',
      dataIndex: 'fecha',
      render: (fecha: string) => new Date(fecha).toLocaleString('es-AR'),
    },
    { title: 'Usuario', dataIndex: 'usuarioNombre', render: (v) => v ?? '—' },
    { title: 'Entidad', dataIndex: 'entidad' },
    { title: 'ID', dataIndex: 'entidadId' },
    { title: 'Acción', dataIndex: 'accion' },
    { title: 'Detalle', dataIndex: 'detalle' },
  ];

  return (
    <AppLayout>
      <Card
        title="Auditoría"
        extra={
          <Select
            allowClear
            placeholder="Filtrar por entidad"
            style={{ width: 220 }}
            value={entidad}
            onChange={setEntidad}
            options={ENTIDADES.map((e) => ({ value: e, label: e }))}
          />
        }
      >
        <Table
          columns={columnas}
          dataSource={registros}
          rowKey="id"
          loading={cargando}
          locale={{ emptyText: 'Todavía no hay registros de auditoría' }}
        />
      </Card>
    </AppLayout>
  );
}
