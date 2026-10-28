import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ProductoResponse } from '../types/productos'
import type { ClienteResponse } from '../types/clientes'
import type { OrdenTrabajoResponse } from '../types/ordenes'
import type { ServicioDomicilioResponse } from '../types/domicilio'
import type { VentaResponse, DetalleVentaResponse } from '../types/ventas'
import type { CajaDiariaResponse } from '../types/caja'
import type { AuditLogResponse } from '../types/auditoria'
import type { CompraResponse, ProveedorResponse } from '../types/compras'
import type { UsuarioResponse } from '../types/usuarios'

/**
 * Modo demo (npm run build:demo): un servidor de mentira dentro del navegador,
 * con datos de ejemplo de una cerrajeria. Sirve para mostrar la app sin la PC
 * del sistema: se puede vender, recibir trabajos y ajustar stock, pero nada se
 * guarda (al recargar la pagina vuelve al estado inicial). No entra al build
 * normal: main.tsx lo importa solo si VITE_DEMO=1.
 */

// ---------- Fechas en hora local, como las manda el backend (sin zona) ----------
function iso(fecha: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${fecha.getFullYear()}-${p(fecha.getMonth() + 1)}-${p(fecha.getDate())}T${p(fecha.getHours())}:${p(fecha.getMinutes())}:00`
}
function haceMinutos(min: number) {
  return iso(new Date(Date.now() - min * 60_000))
}
function dia(offsetDias: number) {
  return haceMinutos(-offsetDias * 24 * 60).slice(0, 10)
}

// ---------- Datos de ejemplo ----------
let siguienteId = 1000
const nuevoId = () => ++siguienteId

function producto(id: number, codigo: string, nombre: string, tipo: ProductoResponse['tipoProducto'], stock: number,
  minimo: number, compra: number, venta: number, duplicado: number | null = null, extra: Partial<ProductoResponse> = {}): ProductoResponse {
  return {
    id, codigo, nombre, tipoProducto: tipo, marca: null, cantidadStock: stock, stockMinimo: minimo, precioCompra: compra,
    precioVenta: venta, precioDuplicado: duplicado, imagenUrl: null, llavePerfil: null, llaveColor: null,
    llaveDisenoTematica: null, productoModelo: null, productoTamano: null, productoMaterial: null, activo: true,
    stockBajo: stock <= minimo, ventaRapida: false, ...extra,
  }
}

const productos: ProductoResponse[] = [
  producto(1, 'CASA-YALE', 'Llave de casa Yale', 'LLAVE_VIRGEN', 480, 100, 0.9, 1.5, 2.5, { marca: 'Yale', llavePerfil: 'Yale', ventaRapida: true }),
  producto(2, 'CASA-FORTE', 'Llave de casa Forte', 'LLAVE_VIRGEN', 320, 100, 0.9, 1.5, 2.5, { marca: 'Forte', llavePerfil: 'Forte', ventaRapida: true }),
  producto(3, 'CASA-CANTOL', 'Llave de casa Cantol', 'LLAVE_VIRGEN', 210, 100, 0.9, 1.5, 2.5, { marca: 'Cantol', llavePerfil: 'Cantol' }),
  producto(4, 'KW1', 'Llave KW1 (Kwikset)', 'LLAVE_VIRGEN', 60, 100, 1, 2, 3, { marca: 'Kwikset', llavePerfil: 'KW1' }),
  producto(5, 'CANDADO-ANT', 'Llave de candado', 'LLAVE_VIRGEN', 150, 50, 0.7, 1.2, 2.5, { ventaRapida: true }),
  producto(6, 'COLOR-AZUL', 'Llave de color azul', 'LLAVE_COLOR', 90, 30, 1.2, 2.5, 4, { llaveColor: 'Azul' }),
  producto(7, 'DISENO-FUTBOL', 'Llave diseño fútbol', 'LLAVE_DISENO', 25, 20, 2.5, 5, 8, { llaveDisenoTematica: 'Fútbol' }),
  producto(8, 'CAN-40', 'Candado Forte 40 mm', 'CANDADO', 4, 5, 18, 30, null, { marca: 'Forte', productoTamano: '40 mm', ventaRapida: true }),
  producto(9, 'CAN-50', 'Candado Forte 50 mm', 'CANDADO', 8, 4, 24, 40, null, { marca: 'Forte', productoTamano: '50 mm' }),
  producto(10, 'CHAPA-YALE-2G', 'Chapa Yale 2 golpes', 'CHAPA', 6, 2, 55, 85, null, { marca: 'Yale' }),
  producto(11, 'CIL-40', 'Cilindro 40 mm', 'CILINDRO', 10, 3, 22, 38),
  producto(12, 'LLAVERO-METAL', 'Llavero metálico', 'LLAVERO', 40, 10, 1.5, 4, null, { ventaRapida: true }),
  producto(13, 'ARGOLLA', 'Argolla de acero', 'ARGOLLA', 200, 50, 0.1, 0.5, null, { ventaRapida: true }),
  producto(14, 'OSTER-CUCH', 'Cuchilla Oster original', 'REPUESTO_LICUADORA', 6, 3, 14, 25, null, { marca: 'Oster' }),
  producto(15, 'OSTER-VASO', 'Vaso de vidrio Oster', 'REPUESTO_LICUADORA', 2, 2, 28, 45, null, { marca: 'Oster' }),
  producto(16, 'OSTER-EMPAQUE', 'Empaque de jebe Oster', 'REPUESTO_LICUADORA', 15, 5, 1.5, 4, null, { marca: 'Oster' }),
]

const clientes: ClienteResponse[] = [
  { id: 1, nombre: 'Rosa Quispe', telefono: '987654321', direccion: 'Jr. Lampa 456', observaciones: null },
  { id: 2, nombre: 'Luis Ramos', telefono: '912345678', direccion: 'Av. Arequipa 1230', observaciones: 'Cliente frecuente' },
  { id: 3, nombre: 'Carmen Flores', telefono: '956111222', direccion: null, observaciones: null },
  { id: 4, nombre: 'Jorge Huamán', telefono: '945333444', direccion: 'Calle Los Pinos 78', observaciones: null },
]

function orden(id: number, cliente: string, recibido: string, problema: string, estado: OrdenTrabajoResponse['estado'],
  precio: number, pagado: number, horas: number): OrdenTrabajoResponse {
  return {
    id, numeroOrden: 'OT-' + String(id).padStart(6, '0'), cliente, productoRecibido: recibido, problema, diagnostico: null,
    precioTotal: precio, montoPagado: pagado, saldoPendiente: precio - pagado, estado, fechaIngreso: haceMinutos(horas * 60),
    fechaPrometida: null, fechaEntrega: estado === 'ENTREGADO' ? haceMinutos(horas * 30) : null, observaciones: null,
    motivoCancelacion: null, garantia: false, numeroOrdenOrigen: null,
    telefonoCliente: clientes.find((c) => c.nombre === cliente)?.telefono ?? '987654321',
  }
}

const ordenes: OrdenTrabajoResponse[] = [
  orden(7, 'Jorge Huamán', 'Licuadora Oster', 'Hace ruido y no corta', 'RECIBIDO', 35, 15, 2),
  orden(6, 'Carmen Flores', 'Chapa de puerta principal', 'La llave no gira', 'RECIBIDO', 45, 0, 3),
  orden(5, 'Luis Ramos', 'Cerradura de cajón de escritorio', 'Perdió la llave', 'EN_REPARACION', 35, 15, 20),
  orden(4, 'Jorge Huamán', 'Cilindro de puerta de calle', 'Cambio de combinación', 'ESPERANDO_REPUESTO', 60, 30, 48),
  orden(3, 'Rosa Quispe', 'Chapa Yale', 'Se traba al cerrar', 'LISTO', 50, 20, 30),
  orden(2, 'Luis Ramos', 'Candado de portón', 'Abrir sin llave y hacer copia', 'ENTREGADO', 25, 25, 72),
]

const domicilio: ServicioDomicilioResponse[] = [
  { id: 1, cliente: 'Rosa Quispe', direccion: 'Jr. Lampa 456', telefono: '987654321', fecha: dia(0), hora: '16:00',
    tipoServicio: 'Apertura de puerta', observaciones: 'Tercer piso', estado: 'PROGRAMADO', montoCobrado: null, metodoPago: null, realizadoPor: null },
  { id: 2, cliente: 'Jorge Huamán', direccion: 'Calle Los Pinos 78', telefono: '945333444', fecha: dia(1), hora: '10:30',
    tipoServicio: 'Cambio de chapa', observaciones: null, estado: 'PROGRAMADO', montoCobrado: null, metodoPago: null, realizadoPor: null },
  { id: 3, cliente: 'Luis Ramos', direccion: 'Av. Arequipa 1230', telefono: '912345678', fecha: dia(-1), hora: '11:00',
    tipoServicio: 'Instalación de cerrojo', observaciones: null, estado: 'REALIZADO', montoCobrado: 80, metodoPago: 'YAPE', realizadoPor: 'Juan Barrera' },
]

const USUARIO = 'Juan Barrera'
const negocio = { nombre: 'Cerrajería Barrera', telefono: '982616709', direccion: 'Av. Alfonso Ugarte N° 115, frente al Mercado N° 2, Surco' }

function detalle(p: ProductoResponse, cantidad: number, dup: boolean): DetalleVentaResponse {
  const precio = dup ? p.precioDuplicado ?? p.precioVenta : p.precioVenta
  return { productoId: p.id, nombreProducto: p.nombre, cantidad, precioUnitario: precio, subtotal: precio * cantidad, duplicado: dup }
}

const ventas: VentaResponse[] = [
  { id: 3, cliente: null, usuario: USUARIO, fechaHora: haceMinutos(25), montoTotal: 7.5, metodoPago: 'EFECTIVO',
    detalles: [detalle(productos[0], 3, true)], anulada: false, desglosePago: [] },
  { id: 2, cliente: null, usuario: USUARIO, fechaHora: haceMinutos(70), montoTotal: 30, metodoPago: 'YAPE',
    detalles: [detalle(productos[7], 1, false)], anulada: false, desglosePago: [] },
  { id: 1, cliente: null, usuario: USUARIO, fechaHora: haceMinutos(140), montoTotal: 5, metodoPago: 'EFECTIVO',
    detalles: [detalle(productos[1], 2, true)], anulada: false, desglosePago: [] },
]

let caja: CajaDiariaResponse | null = {
  id: 1, usuario: USUARIO, fechaApertura: haceMinutos(240), fechaCierre: null, montoApertura: 50, montoCierre: null,
  montoIngresos: 42.5, montoEgresos: 0, montoYape: 30, montoPlin: 0, montoTarjeta: 0, saldoEsperado: 92.5,
  efectivoEsperado: 62.5, estado: 'ABIERTA', diferenciaCierre: null, observacionCierre: null,
}
const historialCaja: CajaDiariaResponse[] = []

const proveedores: ProveedorResponse[] = [
  { id: 1, nombre: 'Distribuidora Lima Centro', telefono: '014281111', direccion: 'Jr. Paruro 900' },
  { id: 2, nombre: 'Llaves y Candados SAC', telefono: '987000111', direccion: null },
]
const compras: CompraResponse[] = [
  { id: 1, proveedor: 'Distribuidora Lima Centro', producto: 'Llave de casa Yale', cantidad: 500, precioUnitario: 0.9, precioTotal: 450, fecha: dia(-7) },
]
const usuarios: UsuarioResponse[] = [
  { id: 1, username: 'julio', nombre: 'Juan Barrera', role: 'ROLE_ADMIN', activo: true },
  { id: 2, username: 'pedro', nombre: 'Pedro (ayudante)', role: 'ROLE_EMPLOYEE', activo: true },
]
const kardex: Record<number, { id: number; tipoMovimiento: string; cantidad: number; saldoAnterior: number; saldoNuevo: number;
  referenciaTipo: string; referenciaId: number | null; motivo: string | null; usuario: string; fechaHora: string }[]> = {}
const materiales: Record<number, { productoId: number; codigo: string; nombre: string; cantidad: number; usuario: string; fechaHora: string }[]> = {}
const auditoria: AuditLogResponse[] = [
  { id: 2, usuario: USUARIO, accion: 'EDITAR', entidad: 'Producto', entidadId: 1, fechaHora: haceMinutos(300),
    detalle: { descripcion: 'Precios cambiados: CASA-YALE · Llave de casa Yale', motivo: 'Planilla de Excel',
      cambios: { precioDuplicado: { antes: '2.00', despues: '2.50' } } } },
  { id: 1, usuario: USUARIO, accion: 'APERTURA_CAJA', entidad: 'CajaDiaria', entidadId: 1, fechaHora: haceMinutos(240),
    detalle: { descripcion: 'Apertura de caja', cambios: { montoApertura: { antes: null, despues: '50.00' } } } },
]

// ---------- Ayudas ----------
class ErrorDemo extends Error {
  constructor(public status: number, mensaje: string) { super(mensaje) }
}
const fallar = (mensaje: string, status = 400): never => { throw new ErrorDemo(status, mensaje) }

function pagina<T>(items: T[], params: Record<string, unknown>) {
  const size = Number(params.size ?? 20)
  const number = Number(params.page ?? 0)
  return {
    content: items.slice(number * size, number * size + size),
    page: { number, size, totalElements: items.length, totalPages: Math.max(1, Math.ceil(items.length / size)) },
  }
}

function buscarProductos(q: string) {
  const t = q.toLowerCase()
  return productos.filter((p) => p.activo && (p.nombre.toLowerCase().includes(t) || p.codigo.toLowerCase().includes(t))).slice(0, 20)
}

function registrarAuditoria(accion: string, entidad: string, entidadId: number, descripcion: string, motivo?: string) {
  auditoria.unshift({ id: nuevoId(), usuario: USUARIO, accion, entidad, entidadId, fechaHora: iso(new Date()),
    detalle: { descripcion, ...(motivo ? { motivo } : {}) } })
}

function moverStock(p: ProductoResponse, delta: number, tipo: string, ref: string, refId: number | null, motivo: string | null) {
  if (p.cantidadStock + delta < 0) fallar(`Stock insuficiente para el producto: ${p.nombre}`, 409)
  const anterior = p.cantidadStock
  p.cantidadStock += delta
  p.stockBajo = p.cantidadStock <= p.stockMinimo
  ;(kardex[p.id] ??= []).unshift({ id: nuevoId(), tipoMovimiento: tipo, cantidad: delta, saldoAnterior: anterior,
    saldoNuevo: p.cantidadStock, referenciaTipo: ref, referenciaId: refId, motivo, usuario: USUARIO, fechaHora: iso(new Date()) })
}

function exigirCaja() {
  if (!caja) fallar('No tienes una caja diaria abierta. Debes abrir caja antes de continuar.', 409)
  return caja!
}

function ingreso(monto: number, metodo: string) {
  const c = exigirCaja()
  c.montoIngresos += monto
  if (metodo === 'YAPE') c.montoYape += monto
  if (metodo === 'PLIN') c.montoPlin += monto
  c.saldoEsperado = c.montoApertura + c.montoIngresos - c.montoEgresos
  c.efectivoEsperado = c.saldoEsperado - c.montoYape - c.montoPlin - c.montoTarjeta
}

const producto_ = (id: number) => productos.find((p) => p.id === id) ?? fallar('Producto no encontrado', 404)
const orden_ = (id: number) => ordenes.find((o) => o.id === id) ?? fallar('Orden no encontrada', 404)
const hoy = () => dia(0)

// ---------- Rutas ----------
// Los cuerpos llegan como JSON sin tipo; cada ruta lee lo que necesita
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Manejador = (m: RegExpMatchArray, body: Record<string, any>, params: Record<string, any>) => unknown
const rutas: [string, RegExp, Manejador][] = [
  ['POST', /^\/auth\/(refresh|login)$/, () => ({ accessToken: 'demo', username: 'julio', nombre: USUARIO, role: 'ROLE_ADMIN' })],
  ['POST', /^\/auth\/logout$/, () => ({})],
  ['GET', /^\/asistente\/estado$/, () => ({ disponible: false })],
  ['GET', /^\/negocio$/, () => negocio],
  ['PUT', /^\/negocio$/, (_m, b) => Object.assign(negocio, b, { telefono: String(b.telefono).replace(/[^0-9+]/g, '') })],

  ['GET', /^\/dashboard$/, () => {
    const bajos = productos.filter((p) => p.activo && p.stockBajo)
    return {
      ventasHoy: ventas.filter((v) => !v.anulada && v.fechaHora.startsWith(hoy())).reduce((s, v) => s + v.montoTotal, 0),
      ventasMes: 1240 + ventas.filter((v) => !v.anulada).reduce((s, v) => s + v.montoTotal, 0),
      trabajosPendientes: ordenes.filter((o) => ['RECIBIDO', 'EN_REPARACION', 'ESPERANDO_REPUESTO'].includes(o.estado)).length,
      trabajosListos: ordenes.filter((o) => o.estado === 'LISTO').length,
      serviciosDomicilioProximos: domicilio.filter((s) => s.estado === 'PROGRAMADO').length,
      productosStockBajo: bajos.length,
      alertasStockBajo: bajos.map((p) => p.nombre),
      ultimosMovimientos: auditoria.slice(0, 6).map((a) => ({ descripcion: a.detalle?.descripcion ?? a.accion, usuario: a.usuario, fechaHora: a.fechaHora })),
    }
  }],

  // Caja
  ['GET', /^\/caja\/abierta$/, () => caja ?? fallar('No tienes una caja abierta', 404)],
  ['POST', /^\/caja\/abrir$/, (_m, b) => {
    if (caja) fallar('Ya tienes una caja abierta')
    caja = { id: nuevoId(), usuario: USUARIO, fechaApertura: iso(new Date()), fechaCierre: null, montoApertura: Number(b.montoApertura),
      montoCierre: null, montoIngresos: 0, montoEgresos: 0, montoYape: 0, montoPlin: 0, montoTarjeta: 0,
      saldoEsperado: Number(b.montoApertura), efectivoEsperado: Number(b.montoApertura), estado: 'ABIERTA', diferenciaCierre: null, observacionCierre: null }
    registrarAuditoria('APERTURA_CAJA', 'CajaDiaria', caja.id, 'Apertura de caja')
    return caja
  }],
  ['POST', /^\/caja\/cerrar$/, (_m, b) => {
    const c = exigirCaja()
    const diferencia = Math.round((Number(b.montoContado) - c.efectivoEsperado) * 100) / 100
    if (diferencia !== 0 && !b.observacion?.trim()) {
      fallar(`El cajón no cuadra (${diferencia > 0 ? 'sobran' : 'faltan'} S/ ${Math.abs(diferencia).toFixed(2)}): escribe qué pasó antes de cerrar`)
    }
    Object.assign(c, { estado: 'CERRADA', fechaCierre: iso(new Date()), montoCierre: Number(b.montoContado),
      diferenciaCierre: diferencia, observacionCierre: b.observacion?.trim() || null })
    historialCaja.unshift({ ...c })
    caja = null
    registrarAuditoria('CIERRE_CAJA', 'CajaDiaria', c.id, diferencia === 0 ? 'Cierre de caja: cuadra' : `Cierre de caja con diferencia de S/ ${diferencia.toFixed(2)}`)
    return { caja: c, montoContado: Number(b.montoContado), diferencia }
  }],
  ['GET', /^\/caja\/historial$/, (_m, _b, p) => pagina([...(caja ? [caja] : []), ...historialCaja], p)],
  ['GET', /^\/caja\/\d+\/movimientos$/, () => []],

  // Productos
  ['GET', /^\/productos$/, (_m, _b, p) => pagina(productos.filter((x) => x.activo && (!p.tipoProducto || x.tipoProducto === p.tipoProducto)), p)],
  ['GET', /^\/productos\/buscar$/, (_m, _b, p) => buscarProductos(String(p.q ?? ''))],
  ['GET', /^\/productos\/duplicables$/, () => productos.filter((p) => p.activo && p.precioDuplicado != null)],
  ['GET', /^\/productos\/venta-rapida$/, () => productos.filter((p) => p.activo && p.ventaRapida)],
  ['PUT', /^\/productos\/(\d+)\/venta-rapida$/, (m, _b, p) =>
    Object.assign(producto_(Number(m[1])), { ventaRapida: String(p.activo) === 'true' })],
  ['GET', /^\/productos\/stock-bajo$/, () => productos.filter((p) => p.activo && p.stockBajo)],
  ['GET', /^\/productos\/(\d+)\/kardex$/, (m) => kardex[Number(m[1])] ?? []],
  ['GET', /^\/productos\/(\d+)$/, (m) => producto_(Number(m[1]))],
  ['POST', /^\/productos\/(\d+)\/ajustar-stock$/, (m, b) => {
    const p = producto_(Number(m[1]))
    moverStock(p, Number(b.cantidad), 'AJUSTE', 'AJUSTE', null, b.motivo)
    registrarAuditoria('AJUSTE_INVENTARIO', 'Producto', p.id, `Ajuste de stock: ${p.codigo} · ${p.nombre}`, b.motivo)
    return p
  }],
  ['POST', /^\/productos$/, (_m, b) => {
    const p = producto(nuevoId(), b.codigo, b.nombre, b.tipoProducto, Number(b.cantidadStock ?? 0), Number(b.stockMinimo ?? 5),
      Number(b.precioCompra), Number(b.precioVenta), b.precioDuplicado ?? null, { marca: b.marca || null })
    productos.unshift(p)
    return p
  }],
  ['PUT', /^\/productos\/(\d+)$/, (m, b) => Object.assign(producto_(Number(m[1])), b)],
  ['DELETE', /^\/productos\/(\d+)$/, (m) => { producto_(Number(m[1])).activo = false; return '' }],
  ['GET', /^\/productos\/planilla$/, () => fallar('En la demo no se descargan archivos', 400)],

  // Ventas
  ['POST', /^\/ventas$/, (_m, b) => {
    exigirCaja()
    const venta: VentaResponse = { id: nuevoId(), cliente: null, usuario: USUARIO, fechaHora: iso(new Date()), montoTotal: 0,
      metodoPago: b.metodoPago, detalles: [], anulada: false, desglosePago: b.desglosePago ?? [] }
    for (const item of b.items) {
      if (item.productoId == null) {
        // Servicio rapido: sin producto ni stock
        venta.detalles.push({ productoId: null, nombreProducto: item.descripcion, cantidad: item.cantidad,
          precioUnitario: Number(item.precio), subtotal: Number(item.precio) * item.cantidad, duplicado: false })
        continue
      }
      const p = producto_(item.productoId)
      moverStock(p, -item.cantidad, 'SALIDA', 'VENTA', venta.id, null)
      venta.detalles.push(detalle(p, item.cantidad, !!item.duplicado))
    }
    venta.montoTotal = venta.detalles.reduce((s, d) => s + d.subtotal, 0)
    if (b.metodoPago === 'MIXTO') for (const l of b.desglosePago) ingreso(Number(l.monto), l.metodoPago)
    else ingreso(venta.montoTotal, b.metodoPago)
    ventas.unshift(venta)
    registrarAuditoria('CREAR', 'Venta', venta.id, `Venta #${venta.id}: ` + venta.detalles.map((d) =>
      `${d.cantidad} × ${d.duplicado ? 'duplicado ' : ''}${d.nombreProducto}`).join(', '))
    return venta
  }],
  ['GET', /^\/ventas$/, (_m, _b, p) => pagina(ventas, p)],
  ['GET', /^\/ventas\/(\d+)$/, (m) => ventas.find((v) => v.id === Number(m[1])) ?? fallar('Venta no encontrada', 404)],
  ['POST', /^\/ventas\/(\d+)\/anular$/, (m) => {
    const v = ventas.find((x) => x.id === Number(m[1])) ?? fallar('Venta no encontrada', 404)
    if (v.anulada) fallar('La venta ya se encuentra anulada')
    v.detalles.forEach((d) => { if (d.productoId != null) moverStock(producto_(d.productoId), d.cantidad, 'ENTRADA', 'VENTA', v.id, null) })
    ingreso(-v.montoTotal, v.metodoPago === 'MIXTO' ? 'EFECTIVO' : v.metodoPago)
    v.anulada = true
    return v
  }],

  // Taller
  ['GET', /^\/ordenes$/, (_m, _b, p) => (p.estado ? ordenes.filter((o) => o.estado === p.estado) : ordenes)],
  ['GET', /^\/ordenes\/(\d+)\/materiales$/, (m) => materiales[Number(m[1])] ?? []],
  ['GET', /^\/ordenes\/(\d+)$/, (m) => orden_(Number(m[1]))],
  ['POST', /^\/ordenes$/, (_m, b) => {
    const cliente = clientes.find((c) => c.id === b.clienteId) ?? fallar('Cliente no encontrado', 404)
    const adelanto = Number(b.adelanto ?? 0)
    if (adelanto > 0) ingreso(adelanto, b.metodoPagoAdelanto ?? 'EFECTIVO')
    const o = orden(nuevoId(), cliente.nombre, b.productoRecibido, b.problema, 'RECIBIDO', Number(b.precioTotal), adelanto, 0)
    ordenes.unshift(o)
    registrarAuditoria('CREAR', 'OrdenTrabajo', o.id, `Orden ${o.numeroOrden} recibida: ${o.productoRecibido}`)
    return o
  }],
  ['PATCH', /^\/ordenes\/(\d+)\/estado$/, (m, b) => {
    const o = orden_(Number(m[1]))
    if (b.nuevoEstado === 'ENTREGADO' && o.saldoPendiente > 0) fallar(`No se puede entregar: saldo pendiente de ${o.saldoPendiente}`, 409)
    o.estado = b.nuevoEstado
    return o
  }],
  ['POST', /^\/ordenes\/(\d+)\/pagos$/, (m, b) => {
    const o = orden_(Number(m[1]))
    ingreso(Number(b.monto), b.metodoPago)
    o.montoPagado += Number(b.monto); o.saldoPendiente = o.precioTotal - o.montoPagado
    return o
  }],
  ['POST', /^\/ordenes\/(\d+)\/entregar$/, (m, b) => {
    const o = orden_(Number(m[1]))
    if (o.saldoPendiente > 0) {
      if (!b || Number(b.monto) < o.saldoPendiente) fallar(`Debe liquidar el saldo pendiente completo (${o.saldoPendiente}) para entregar la orden`, 409)
      ingreso(Number(b.monto), b.metodoPago)
      o.montoPagado = o.precioTotal; o.saldoPendiente = 0
    }
    o.estado = 'ENTREGADO'; o.fechaEntrega = iso(new Date())
    return o
  }],
  ['POST', /^\/ordenes\/(\d+)\/cancelar$/, (m, b) => {
    const o = orden_(Number(m[1]))
    if (b.devolverAdelanto && o.montoPagado > 0) { ingreso(-o.montoPagado, b.metodoDevolucion ?? 'EFECTIVO'); o.montoPagado = 0 }
    o.estado = 'CANCELADO'; o.motivoCancelacion = b.motivo; o.saldoPendiente = o.precioTotal - o.montoPagado
    return o
  }],
  ['POST', /^\/ordenes\/(\d+)\/garantia$/, (m, b) => {
    const original = orden_(Number(m[1]))
    if (original.estado !== 'ENTREGADO') fallar(`Solo se abre una garantía sobre una orden entregada`)
    const g = { ...orden(nuevoId(), original.cliente, original.productoRecibido, b.problema, 'RECIBIDO', 0, 0, 0),
      garantia: true, numeroOrdenOrigen: original.numeroOrden }
    ordenes.unshift(g)
    registrarAuditoria('GARANTIA', 'OrdenTrabajo', g.id, `Garantía ${g.numeroOrden} de la orden ${original.numeroOrden}`, b.problema)
    return g
  }],
  ['POST', /^\/ordenes\/(\d+)\/materiales$/, (m, b) => {
    const o = orden_(Number(m[1]))
    const p = producto_(b.productoId)
    moverStock(p, -b.cantidad, 'SALIDA', 'ORDEN_TRABAJO', o.id, null)
    ;(materiales[o.id] ??= []).push({ productoId: p.id, codigo: p.codigo, nombre: p.nombre, cantidad: b.cantidad, usuario: USUARIO, fechaHora: iso(new Date()) })
    return materiales[o.id]
  }],

  // Clientes
  ['GET', /^\/clientes\/buscar$/, (_m, _b, p) => {
    const t = String(p.q ?? '').toLowerCase()
    return clientes.filter((c) => c.nombre.toLowerCase().includes(t) || c.telefono.includes(t))
  }],
  ['GET', /^\/clientes$/, (_m, _b, p) => pagina(clientes, p)],
  ['GET', /^\/clientes\/(\d+)\/compras$/, () => []],
  ['GET', /^\/clientes\/(\d+)\/ordenes$/, (m) => {
    const c = clientes.find((x) => x.id === Number(m[1]))
    return ordenes.filter((o) => o.cliente === c?.nombre)
  }],
  ['GET', /^\/clientes\/(\d+)$/, (m) => clientes.find((c) => c.id === Number(m[1])) ?? fallar('Cliente no encontrado', 404)],
  ['POST', /^\/clientes$/, (_m, b) => { const c = { id: nuevoId(), observaciones: null, direccion: null, ...b } as ClienteResponse; clientes.unshift(c); return c }],
  ['PUT', /^\/clientes\/(\d+)$/, (m, b) => Object.assign(clientes.find((c) => c.id === Number(m[1])) ?? fallar('Cliente no encontrado', 404), b)],

  // Domicilio
  ['GET', /^\/servicios-domicilio\/agenda$/, (_m, _b, p) =>
    domicilio.filter((s) => (!p.desde || s.fecha >= p.desde) && (!p.hasta || s.fecha <= p.hasta))],
  ['POST', /^\/servicios-domicilio$/, (_m, b) => {
    const cliente = clientes.find((c) => c.id === b.clienteId)
    const s: ServicioDomicilioResponse = { id: nuevoId(), cliente: cliente?.nombre ?? '', direccion: b.direccion, telefono: b.telefono,
      fecha: b.fecha, hora: b.hora, tipoServicio: b.tipoServicio, observaciones: b.observaciones ?? null, estado: 'PROGRAMADO',
      montoCobrado: null, metodoPago: null, realizadoPor: null }
    domicilio.push(s)
    return s
  }],
  ['PATCH', /^\/servicios-domicilio\/(\d+)\/estado$/, (m, b) => {
    const s = domicilio.find((x) => x.id === Number(m[1])) ?? fallar('Servicio no encontrado', 404)
    s.estado = b.estado ?? b.nuevoEstado
    return s
  }],
  ['POST', /^\/servicios-domicilio\/(\d+)\/realizar$/, (m, b) => {
    const s = domicilio.find((x) => x.id === Number(m[1])) ?? fallar('Servicio no encontrado', 404)
    if (Number(b.monto) > 0) ingreso(Number(b.monto), b.metodoPago)
    Object.assign(s, { estado: 'REALIZADO', montoCobrado: Number(b.monto), metodoPago: b.metodoPago ?? null, realizadoPor: USUARIO })
    return s
  }],

  // Compras
  ['GET', /^\/proveedores$/, () => proveedores],
  ['POST', /^\/proveedores$/, (_m, b) => { const p = { id: nuevoId(), telefono: null, direccion: null, ...b } as ProveedorResponse; proveedores.push(p); return p }],
  ['GET', /^\/compras$/, (_m, _b, p) => pagina(compras, p)],
  ['POST', /^\/compras\/lote$/, (_m, b) => {
    const proveedor = proveedores.find((p) => p.id === b.proveedorId)?.nombre ?? ''
    return b.items.map((i: { productoId: number; cantidad: number; precioUnitario: number }) => {
      const p = producto_(i.productoId)
      moverStock(p, i.cantidad, 'ENTRADA', 'COMPRA', null, null)
      const c = { id: nuevoId(), proveedor, producto: p.nombre, cantidad: i.cantidad, precioUnitario: i.precioUnitario,
        precioTotal: i.cantidad * i.precioUnitario, fecha: hoy() }
      compras.unshift(c)
      return c
    })
  }],

  // Reportes (cifras de ejemplo, con lo vendido en la demo)
  ['GET', /^\/reportes\/ventas$/, () => {
    const total = 1240 + ventas.filter((v) => !v.anulada).reduce((s, v) => s + v.montoTotal, 0)
    return { totalVentas: total, cantidadTransacciones: 96 + ventas.length,
      porMetodoPago: [{ metodoPago: 'EFECTIVO', total: total * 0.62 }, { metodoPago: 'YAPE', total: total * 0.3 }, { metodoPago: 'PLIN', total: total * 0.08 }],
      ingresosTaller: 420, ingresosDomicilio: 310, serviciosDomicilioRealizados: 5, ingresosTotales: total + 730 }
  }],
  ['GET', /^\/reportes\/productos-mas-vendidos$/, () => [
    { productoId: 1, nombre: 'Llave de casa Yale', cantidadVendida: 210, totalVendido: 525 },
    { productoId: 2, nombre: 'Llave de casa Forte', cantidadVendida: 140, totalVendido: 350 },
    { productoId: 5, nombre: 'Llave de candado', cantidadVendida: 60, totalVendido: 150 },
    { productoId: 8, nombre: 'Candado Forte 40 mm', cantidadVendida: 7, totalVendido: 210 },
  ]],
  ['GET', /^\/reportes\/inventario$/, () => {
    const items = productos.filter((p) => p.activo).map((p) => ({ codigo: p.codigo, nombre: p.nombre, tipoProducto: p.tipoProducto,
      cantidadStock: p.cantidadStock, precioVenta: p.precioVenta, valorLinea: p.cantidadStock * p.precioCompra, stockBajo: p.stockBajo }))
    return { totalProductosActivos: items.length, totalUnidades: items.reduce((s, i) => s + i.cantidadStock, 0),
      valorTotalInventario: items.reduce((s, i) => s + i.valorLinea, 0), items }
  }],
  ['GET', /^\/reportes\/servicios$/, () => ({
    domicilio: { realizados: 5, cancelados: 1, cobrado: 310, porTipo: [{ nombre: 'Apertura de puerta', cantidad: 3, total: 150 },
      { nombre: 'Cambio de chapa', cantidad: 2, total: 160 }], porTecnico: [{ nombre: USUARIO, cantidad: 5, total: 310 }] },
    taller: { recibidas: 9, entregadas: 6, abiertas: ordenes.filter((o) => !['ENTREGADO', 'CANCELADO'].includes(o.estado)).length, ingresosNetos: 420 },
  })],
  ['GET', /^\/reportes\/clientes$/, () => ({ clientesConMovimiento: 4, ventasSinCliente: 92, ranking: [
    { clienteId: 2, nombre: 'Luis Ramos', ventas: { cantidad: 2, total: 40 }, taller: { cantidad: 2, total: 60 }, domicilio: { cantidad: 1, total: 80 }, total: 180 },
    { clienteId: 1, nombre: 'Rosa Quispe', ventas: { cantidad: 1, total: 30 }, taller: { cantidad: 1, total: 20 }, domicilio: { cantidad: 0, total: 0 }, total: 50 },
  ] })],

  // Otros
  ['GET', /^\/buscar$/, (_m, _b, p) => {
    const t = String(p.q ?? '').toLowerCase()
    return { productos: buscarProductos(t), clientes: clientes.filter((c) => c.nombre.toLowerCase().includes(t)),
      ordenes: ordenes.filter((o) => o.cliente.toLowerCase().includes(t) || o.numeroOrden.toLowerCase().includes(t)) }
  }],
  ['GET', /^\/auditoria$/, (_m, _b, p) => {
    const lista = p.entidad ? auditoria.filter((a) => a.entidad === p.entidad && a.entidadId === Number(p.entidadId)) : auditoria
    return pagina(lista, p)
  }],
  ['GET', /^\/usuarios$/, () => usuarios],
  ['POST', /^\/usuarios$/, (_m, b) => { const u = { id: nuevoId(), activo: true, username: b.username, nombre: b.nombre, role: b.role } as UsuarioResponse; usuarios.push(u); return u }],
  ['PATCH', /^\/usuarios\/(\d+)\/estado$/, (m, b) => Object.assign(usuarios.find((u) => u.id === Number(m[1]))!, { activo: b.activo })],
  ['PATCH', /^\/usuarios\/.*\/password$/, () => fallar('En la demo no se cambian contraseñas')],
  ['POST', /^\/asistente\/mensaje$/, () => ({ respuesta: 'En la demo el asistente no está conectado.' })],
]

/** Adaptador de axios: responde cada pedido desde los datos en memoria. */
export const adaptadorDemo: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
  await new Promise((r) => setTimeout(r, 120)) // se siente como una red real, pero rapida
  const metodo = (config.method ?? 'get').toUpperCase()
  const ruta = (config.url ?? '').replace(/^https?:\/\/[^/]+/, '').replace(/^\/api\/v1/, '').split('?')[0]
  const body = typeof config.data === 'string' && config.data ? JSON.parse(config.data) : (config.data ?? {})
  const responder = (status: number, data: unknown): AxiosResponse => ({ data, status, statusText: String(status), headers: {}, config })

  for (const [m, patron, manejador] of rutas) {
    const coincide = metodo === m && ruta.match(patron)
    if (!coincide) continue
    try {
      return responder(200, manejador(coincide, body, config.params ?? {}))
    } catch (e) {
      if (!(e instanceof ErrorDemo)) throw e
      const error = Object.assign(new Error(e.message), {
        isAxiosError: true, config, response: responder(e.status, { detail: e.message, status: e.status }),
      })
      throw error
    }
  }
  const error = Object.assign(new Error('No disponible en la demo'), {
    isAxiosError: true, config, response: responder(404, { detail: 'Esta función no está disponible en la demo' }),
  })
  throw error
}
