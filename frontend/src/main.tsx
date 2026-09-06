import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ConfigProvider } from 'antd';
import esES from 'antd/locale/es_ES';
import './index.css';
import App from './App.tsx';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={esES}
      theme={{
        token: { colorPrimary: '#fa8c16' },
        // El header y el menú son naranja (ver AppLayout): el resaltado del ítem activo no puede
        // ser el mismo naranja del token primario porque se perdería contra el fondo. Se usa el
        // navy oscuro que tenía el header antes como acento, en vez del naranja para el estado
        // seleccionado/hover del menú.
        components: {
          Menu: {
            darkItemBg: 'transparent',
            darkItemSelectedBg: '#001529',
            darkItemHoverBg: 'rgba(0, 0, 0, 0.2)',
          },
        },
      }}
    >
      <App />
    </ConfigProvider>
  </StrictMode>,
);
