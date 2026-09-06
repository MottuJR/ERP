import { useEffect, useRef, useState } from 'react';
import { Button, Typography } from 'antd';
import { CheckCircleFilled, ExpandOutlined } from '@ant-design/icons';
import {
  CANAL_PANTALLA_CLIENTE,
  type ItemPantallaCliente,
  type MensajePantallaCliente,
} from '../pos/pantallaCliente';

const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

// Cuánto tiempo se queda el cartel de "Gracias por tu compra" antes de volver al estado de
// espera. La venta ya vació el carrito del POS apenas se confirma, así que esto es lo único
// que evita que la pantalla del cliente pase de golpe de "total: $X" a vacía.
const DURACION_GRACIAS_MS = 6000;

/**
 * Pantalla pensada para un segundo monitor conectado a la PC del mostrador: el vendedor la abre
 * en una ventana aparte (botón "Pantalla del cliente" en el POS) y la pone en pantalla completa
 * de cara al cliente. No requiere login ni toca el backend — solo escucha, vía BroadcastChannel,
 * lo que va pasando en la pantalla de venta (PosPage) de la misma PC.
 */
export function PantallaClientePage() {
  const [items, setItems] = useState<ItemPantallaCliente[]>([]);
  const [total, setTotal] = useState(0);
  const [mostrandoGracias, setMostrandoGracias] = useState(false);
  // Total de la venta que se acaba de confirmar, aparte de `total`: el carrito se vacía en el
  // POS apenas se confirma, así que el mensaje "actualizar" con total $0 llega casi al mismo
  // tiempo que "venta-confirmada" — si mostráramos `total` en el cartel de agradecimiento,
  // alcanzaríamos a ver "Total: $0,00" en vez del total real de lo que se acaba de vender.
  const [totalVentaConfirmada, setTotalVentaConfirmada] = useState(0);
  const timeoutGraciasRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const canal = new BroadcastChannel(CANAL_PANTALLA_CLIENTE);

    canal.onmessage = (event: MessageEvent<MensajePantallaCliente>) => {
      const mensaje = event.data;

      if (mensaje.tipo === 'actualizar') {
        setItems(mensaje.items);
        setTotal(mensaje.total);
      } else if (mensaje.tipo === 'venta-confirmada') {
        setTotalVentaConfirmada(mensaje.total);
        setMostrandoGracias(true);
        window.clearTimeout(timeoutGraciasRef.current);
        timeoutGraciasRef.current = window.setTimeout(() => setMostrandoGracias(false), DURACION_GRACIAS_MS);
      }
    };

    // Por si esta pantalla se abre (o se recarga) a mitad de una venta ya empezada.
    canal.postMessage({ tipo: 'solicitar-estado' } satisfies MensajePantallaCliente);

    return () => {
      window.clearTimeout(timeoutGraciasRef.current);
      canal.close();
    };
  }, []);

  function handlePantallaCompleta() {
    document.documentElement.requestFullscreen?.().catch(() => {
      // Si el navegador rechaza el pedido (ej. sin gesto del usuario), no hay nada que hacer
      // salvo que el vendedor use F11 manualmente — no vale la pena romper la pantalla por esto.
    });
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        flexDirection: 'column',
        background: '#001529',
        color: 'white',
        fontFamily: 'inherit',
      }}
    >
      <Button
        icon={<ExpandOutlined />}
        onClick={handlePantallaCompleta}
        style={{ position: 'absolute', top: 16, right: 16, zIndex: 1 }}
      >
        Pantalla completa
      </Button>

      {mostrandoGracias ? (
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 24,
          }}
        >
          <CheckCircleFilled style={{ fontSize: '8vw', color: '#52c41a' }} />
          <Typography.Title style={{ color: 'white', fontSize: '5vw', margin: 0, textAlign: 'center' }}>
            ¡Gracias por tu compra!
          </Typography.Title>
          <Typography.Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: '1.8vw' }}>
            Total: {formatoMoneda.format(totalVentaConfirmada)}
          </Typography.Text>
        </div>
      ) : (
        <>
          <div style={{ padding: '32px 48px 0' }}>
            <Typography.Title style={{ color: 'white', fontSize: '2.5vw', margin: 0 }}>
              ERP Panadería
            </Typography.Title>
          </div>

          {items.length === 0 ? (
            <div
              style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Typography.Title style={{ color: 'rgba(255,255,255,0.45)', fontSize: '3vw', textAlign: 'center' }}>
                Bienvenido — esperando el próximo pedido
              </Typography.Title>
            </div>
          ) : (
            <>
              <div style={{ flex: 1, overflowY: 'auto', padding: '24px 48px' }}>
                {items.map((item, indice) => (
                  <div
                    key={indice}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'baseline',
                      gap: 24,
                      padding: '16px 0',
                      borderBottom: '1px solid rgba(255,255,255,0.15)',
                      fontSize: '2vw',
                    }}
                  >
                    <span style={{ flex: 1 }}>
                      {item.productoNombre}
                      <span style={{ color: 'rgba(255,255,255,0.55)', fontSize: '1.3vw', marginLeft: 12 }}>
                        {item.seVendePorPeso ? `${item.cantidad.toFixed(3)} kg` : `x${item.cantidad}`}
                        {' · '}
                        {formatoMoneda.format(item.precioUnitario)}
                        {item.seVendePorPeso ? '/kg' : ' c/u'}
                      </span>
                    </span>
                    <span style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>{formatoMoneda.format(item.subtotal)}</span>
                  </div>
                ))}
              </div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '24px 48px',
                  borderTop: '2px solid rgba(255,255,255,0.25)',
                  background: 'rgba(255,255,255,0.04)',
                }}
              >
                <Typography.Title style={{ color: 'white', fontSize: '2.2vw', margin: 0 }}>Total</Typography.Title>
                <Typography.Title style={{ color: '#95de64', fontSize: '3vw', margin: 0 }}>
                  {formatoMoneda.format(total)}
                </Typography.Title>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
