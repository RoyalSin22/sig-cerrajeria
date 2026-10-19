import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Los tests corren en hora de Peru, como el local: los errores de fecha que ya
// pasaron (toISOString corriendo el dia) solo se ven con un huso distinto de UTC.
process.env.TZ = 'America/Lima'

// Config aparte de vite.config.ts para no cargar el plugin de PWA en los tests.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
