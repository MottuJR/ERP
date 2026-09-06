import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { primeraRutaPermitida } from '../layout/navItems';
import { mensajeDeError } from '../api/client';
import type { Rol } from '../types';

interface LoginFormValues {
  email: string;
  password: string;
}

export function LoginPage() {
  const { login, isAuthenticated, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  if (isAuthenticated) {
    const destino = (location.state as { from?: string } | null)?.from ?? primeraRutaPermitida(hasRole);
    return <Navigate to={destino} replace />;
  }

  async function onFinish(values: LoginFormValues) {
    setError(null);
    setCargando(true);
    try {
      const usuario = await login(values.email, values.password);
      const destino =
        (location.state as { from?: string } | null)?.from ??
        primeraRutaPermitida((...roles: Rol[]) => roles.includes(usuario.rol));
      navigate(destino, { replace: true });
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
        // Varios degradés superpuestos (no uno solo lineal) para que el naranja de marca tenga
        // algo de profundidad en vez de un color plano — más luminoso arriba a la izquierda y
        // abajo a la derecha, oscureciendo un poco hacia el resto.
        background: `
          radial-gradient(circle at 12% 15%, rgba(255, 213, 128, 0.6), transparent 45%),
          radial-gradient(circle at 88% 20%, rgba(255, 154, 60, 0.5), transparent 50%),
          radial-gradient(circle at 80% 90%, rgba(173, 74, 0, 0.45), transparent 55%),
          linear-gradient(135deg, #ffb84d 0%, #fa8c16 45%, #d9600a 100%)
        `,
      }}
    >
      <Card style={{ width: 360 }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginTop: 0 }}>
          Todo Rico
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
