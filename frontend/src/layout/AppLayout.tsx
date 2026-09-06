import type { ReactNode } from 'react';
import { Button, Flex, Layout, Menu, Tag, Typography } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { NAV_ITEMS } from './navItems';

export function AppLayout({ children }: { children: ReactNode }) {
  const { usuario, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const items = NAV_ITEMS.filter((item) => hasRole(...item.roles)).map((item) => ({
    key: item.key,
    label: item.label,
  }));

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Layout.Header style={{ display: 'flex', alignItems: 'center', gap: 24, background: '#fa8c16' }}>
        <Typography.Title level={4} style={{ color: 'white', margin: 0, whiteSpace: 'nowrap' }}>
          Todo Rico
        </Typography.Title>

        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={(e) => navigate(e.key)}
          style={{ flex: 1, minWidth: 0, background: 'transparent' }}
        />

        <Flex align="center" gap={12}>
          <Tag color="blue">{usuario?.rol}</Tag>
          <Typography.Text style={{ color: 'white' }}>{usuario?.nombre}</Typography.Text>
          <Button icon={<LogoutOutlined />} onClick={logout}>
            Salir
          </Button>
        </Flex>
      </Layout.Header>

      <Layout.Content style={{ padding: 24 }}>{children}</Layout.Content>
    </Layout>
  );
}
