import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// Modo demo (npm run build:demo): datos de ejemplo en el navegador, sin backend.
// Rutas relativas para poder publicarlo en cualquier carpeta, y sin service
// worker (no es una app para instalar).
export default defineConfig(({ mode }) => ({
  base: mode === 'demo' ? './' : '/',
  plugins: [
    react(),
    tailwindcss(),
    mode !== 'demo' && VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
      manifest: {
        name: 'Cerrajería Barrera',
        short_name: 'Barrera',
        description: 'Sistema Integral de Gestión para una cerrajería — inventario, ventas, órdenes de trabajo y caja.',
        theme_color: '#2c455d',
        background_color: '#f6f7f8',
        display: 'standalone',
        start_url: '/',
        orientation: 'portrait',
        icons: [
          { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // No se cachea NADA de /api/**: en un sistema de caja/inventario, mostrar
        // datos viejos como si fueran actuales (stock, saldo de caja) es peligroso.
        // Solo se cachea el "cascaron" de la app (JS/CSS/iconos) para que instale
        // y cargue rapido; los datos siempre vienen frescos de la red.
        runtimeCaching: [
          {
            urlPattern: /\/api\//,
            handler: 'NetworkOnly',
          },
          {
            urlPattern: /\/uploads\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'imagenes-productos',
              expiration: { maxEntries: 200, maxAgeSeconds: 60 * 60 * 24 * 30 },
            },
          },
        ],
      },
      devOptions: {
        enabled: false, // el service worker solo se activa en el build de produccion
      },
    }),
  ],
  server: {
    port: 5173,
  },
}))
