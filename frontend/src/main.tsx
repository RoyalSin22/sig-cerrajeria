import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, HashRouter } from 'react-router-dom'
import App from './App.tsx'
import { axiosClient } from './api/axiosClient'
import { AuthProvider } from './context/AuthContext.tsx'
import { CajaProvider } from './context/CajaContext.tsx'
import { ToastProvider } from './context/ToastContext.tsx'
import { ThemeProvider } from './context/ThemeContext.tsx'
import './index.css'

// En la demo no hay backend: las rutas van en el "#" (sirve en cualquier
// carpeta) y un servidor de mentira responde con datos de ejemplo.
const esDemo = import.meta.env.VITE_DEMO === '1'
const Router = esDemo ? HashRouter : BrowserRouter

async function arrancar() {
  if (esDemo) {
    const { adaptadorDemo } = await import('./demo/servidorDemo')
    axiosClient.defaults.adapter = adaptadorDemo
  }
  createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <Router>
        <AuthProvider>
          <ToastProvider>
            <CajaProvider>
              <App />
            </CajaProvider>
          </ToastProvider>
        </AuthProvider>
      </Router>
    </ThemeProvider>
  </StrictMode>,
  )
}

arrancar()
