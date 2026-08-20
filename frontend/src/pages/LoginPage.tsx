import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { mensajeDeError } from '../api/client';

interface LoginFormValues {
  email: string;
  password: string;
}

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  if (isAuthenticated) {
    const destino = (location.state as { from?: string } | null)?.from ?? '/pos';
    return <Navigate to={destino} replace />;
  }

  async function onFinish(values: LoginFormValues) {
    setError(null);
    setCargando(true);
    try {
      await login(values.email, values.password);
      navigate('/pos', { replace: true });
    } catch (err) {
      setError(mensajeDeError(err, 'No se pudo iniciar sesión'));
    } finally {
      setCargando(false);
    }
  }

  return (
    <div
      style={{
        minHeight: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f5f5f5',
      }}
    >
      <Card style={{ width: 360 }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginTop: 0 }}>
          ERP Panadería
        </Typography.Title>

        {error && <Alert type="error" title={error} showIcon style={{ marginBottom: 16 }} />}

        <Form<LoginFormValues> layout="vertical" onFinish={onFinish} disabled={cargando}>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, message: 'Ingresá tu email' }, { type: 'email', message: 'Email inválido' }]}
          >
            <Input prefix={<UserOutlined />} autoFocus autoComplete="username" />
          </Form.Item>

          <Form.Item
            name="password"
            label="Contraseña"
            rules={[{ required: true, message: 'Ingresá tu contraseña' }]}
          >
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block loading={cargando}>
              Ingresar
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
