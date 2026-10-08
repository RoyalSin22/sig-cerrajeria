import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Minus, Plus } from 'lucide-react'
import { productosApi } from '../api/productos'
import { extraerMensajeError } from '../api/axiosClient'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { Chip } from '../components/ui/Chip'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { BottomSheet } from '../components/ui/BottomSheet'
import { Button } from '../components/ui/Button'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { useToast } from '../context/useToast'
import { useAuth } from '../context/useAuth'
import { formatearSoles, formatearFechaHora, urlImagen } from '../utils/format'
import {
  ETIQUETA_TIPO_PRODUCTO, type ProductoResponse, type MovimientoInventarioResponse,
  type ProductoCreateRequest, type TipoProducto, type ImportacionResultado,
} from '../types/productos'

type Filtro = 'TODAS' | 'STOCK_BAJO'

export function Inventario() {
  const { notificar } = useToast()
  const { usuario } = useAuth()
  const [mostrarImportar, setMostrarImportar] = useState(false)
  const [productos, setProductos] = useState<ProductoResponse[]>([])
  const [termino, setTermino] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [cargando, setCargando] = useState(false)
  const [seleccionado, setSeleccionado] = useState<ProductoResponse | null>(null)
  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)

  const explorando = filtro === 'TODAS' && termino.trim().length < 2

  async function cargar(paginaSolicitada = 0, acumular = false) {
    setCargando(true)
    try {
      if (filtro === 'STOCK_BAJO') {
        setProductos(await productosApi.stockBajo())
        setHayMas(false)
      } else if (termino.trim().length >= 2) {
        setProductos(await productosApi.buscar(termino.trim()))
        setHayMas(false)
      } else {
        // Explorar el catalogo completo, paginado (RF: paginacion + Fase 7 grid de categorias)
        const res = await productosApi.listar(paginaSolicitada)
        setProductos((prev) => (acumular ? [...prev, ...res.content] : res.content))
        setPagina(res.page.number)
        setHayMas(res.page.number + 1 < res.page.totalPages)
      }
    } finally {
      setCargando(false)
    }
  }

  const { refrescando, distancia, umbral } = usePullToRefresh(() => cargar(0, false))

  useEffect(() => {
    cargar(0, false)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtro, termino])

  return (
    <div className="flex flex-col gap-4">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-semibold">Inventario</h1>
        <div className="flex gap-2">
          {usuario?.role === 'ROLE_ADMIN' && (
            <Button variant="secondary" onClick={() => setMostrarImportar(true)}>Importar</Button>
          )}
          <Button onClick={() => setMostrarAlta(true)}>Nuevo producto</Button>
        </div>
      </div>

      <SearchInput placeholder="Buscar por nombre o código…" onBuscar={setTermino} />

      <div className="flex gap-2 overflow-x-auto">
        <Chip activo={filtro === 'TODAS'} onClick={() => setFiltro('TODAS')}>Todas</Chip>
        <Chip activo={filtro === 'STOCK_BAJO'} onClick={() => setFiltro('STOCK_BAJO')}>Stock bajo</Chip>
      </div>

      {cargando && productos.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : productos.length === 0 ? (
        <EmptyState
          mensaje={
            filtro === 'STOCK_BAJO'
              ? 'No hay productos con stock bajo.'
              : 'No hay productos que coincidan con la búsqueda.'
          }
        />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
            {productos.map((p) => (
              <button key={p.id} onClick={() => setSeleccionado(p)} className="text-left">
                <Card className="flex h-full flex-col gap-1">
                  <span className="text-xs text-sub">{ETIQUETA_TIPO_PRODUCTO[p.tipoProducto]}</span>
                  <span className="text-sm font-medium leading-tight">{p.nombre}</span>
                  <span className="text-xs text-sub">{p.codigo}</span>
                  <div className="mt-auto flex items-center justify-between pt-2">
                    <span className="font-heading font-semibold text-steel-dark">{formatearSoles(p.precioVenta)}</span>
                    {p.stockBajo ? (
                      <Badge tono="warning">{p.cantidadStock} und</Badge>
                    ) : (
                      <Badge tono="success">{p.cantidadStock} und</Badge>
                    )}
                  </div>
                </Card>
              </button>
            ))}
          </div>
          {explorando && hayMas && (
            <Button variant="secondary" disabled={cargando} onClick={() => cargar(pagina + 1, true)} className="self-center">
              {cargando ? 'Cargando…' : 'Cargar más'}
            </Button>
          )}
        </>
      )}

      <FormularioProducto
        abierto={mostrarAlta}
        onCerrar={() => setMostrarAlta(false)}
        onCreado={(p) => {
          notificar('Producto creado: ' + p.nombre)
          setProductos((prev) => [p, ...prev])
          setMostrarAlta(false)
        }}
      />

      <ImportarInventario
        abierto={mostrarImportar}
        onCerrar={() => setMostrarImportar(false)}
        onImportado={(creados) => {
          notificar(`${creados} productos importados`)
          setMostrarImportar(false)
          cargar(0, false)
        }}
      />

      <FichaProducto
        producto={seleccionado}
        onCerrar={() => setSeleccionado(null)}
        onActualizado={(p) => {
          setProductos((prev) => prev.map((x) => (x.id === p.id ? p : x)))
          setSeleccionado(p)
        }}
        notificar={notificar}
      />
    </div>
  )
}

function FichaProducto({
  producto,
  onCerrar,
  onActualizado,
  notificar,
}: {
  producto: ProductoResponse | null
  onCerrar: () => void
  onActualizado: (p: ProductoResponse) => void
  notificar: (msg: string) => void
}) {
  const [historial, setHistorial] = useState<MovimientoInventarioResponse[]>([])
  const [cantidadAjuste, setCantidadAjuste] = useState(0)
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [editando, setEditando] = useState(false)
  const [edicion, setEdicion] = useState<{ nombre: string; marca: string; stockMinimo: number; precioCompra: number; precioVenta: number } | null>(null)
  const [subiendoImagen, setSubiendoImagen] = useState(false)

  useEffect(() => {
    if (producto) {
      setCantidadAjuste(0)
      setMotivo('')
      setError(null)
      setEditando(false)
      setEdicion({
        nombre: producto.nombre, marca: producto.marca ?? '', stockMinimo: producto.stockMinimo,
        precioCompra: producto.precioCompra, precioVenta: producto.precioVenta,
      })
      productosApi.kardex(producto.id).then(setHistorial).catch(() => setHistorial([]))
    }
  }, [producto])

  if (!producto) return null

  async function guardarEdicion(e: FormEvent) {
    e.preventDefault()
    if (!edicion) return
    setError(null)
    setGuardando(true)
    try {
      const actualizado = await productosApi.actualizar(producto!.id, {
        nombre: edicion.nombre,
        tipoProducto: producto!.tipoProducto,
        marca: edicion.marca || undefined,
        stockMinimo: edicion.stockMinimo,
        precioCompra: edicion.precioCompra,
        precioVenta: edicion.precioVenta,
        imagenUrl: producto!.imagenUrl ?? undefined,
        llavePerfil: producto!.llavePerfil ?? undefined,
        llaveColor: producto!.llaveColor ?? undefined,
        llaveDisenoTematica: producto!.llaveDisenoTematica ?? undefined,
        productoModelo: producto!.productoModelo ?? undefined,
        productoTamano: producto!.productoTamano ?? undefined,
        productoMaterial: producto!.productoMaterial ?? undefined,
      })
      notificar('Producto actualizado')
      onActualizado(actualizado)
      setEditando(false)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  async function confirmarAjuste() {
    if (cantidadAjuste === 0) return
    if (!motivo.trim()) {
      setError('El motivo del ajuste es obligatorio')
      return
    }
    setGuardando(true)
    setError(null)
    try {
      const actualizado = await productosApi.ajustarStock(producto!.id, { cantidad: cantidadAjuste, motivo })
      notificar('Stock actualizado')
      setCantidadAjuste(0)
      setMotivo('')
      onActualizado(actualizado)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  async function handleSubirImagen(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo si hace falta
    if (!archivo) return

    setSubiendoImagen(true)
    setError(null)
    try {
      const actualizado = await productosApi.subirImagen(producto!.id, archivo)
      notificar('Foto actualizada')
      onActualizado(actualizado)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setSubiendoImagen(false)
    }
  }

  return (
    <BottomSheet abierto={!!producto} onCerrar={onCerrar} titulo={producto.nombre}>
      <div className="flex flex-col gap-4">
        <div className="flex flex-col items-center gap-2">
          {urlImagen(producto.imagenUrl) ? (
            <img
              src={urlImagen(producto.imagenUrl)!}
              alt={producto.nombre}
              className="h-40 w-40 rounded-lg border border-hair object-cover"
            />
          ) : (
            <div className="flex h-40 w-40 items-center justify-center rounded-lg border border-dashed border-line text-xs text-sub">
              Sin foto
            </div>
          )}
          <label className="cursor-pointer text-xs text-steel underline">
            {subiendoImagen ? 'Subiendo…' : producto.imagenUrl ? 'Cambiar foto' : 'Subir foto'}
            <input type="file" accept="image/*" className="hidden" disabled={subiendoImagen} onChange={handleSubirImagen} />
          </label>
        </div>
        {editando ? (
          <form onSubmit={guardarEdicion} className="flex flex-col gap-3 text-sm">
            <input required value={edicion?.nombre ?? ''} onChange={(e) => setEdicion((s) => s && { ...s, nombre: e.target.value })}
              placeholder="Nombre" className="rounded-md border border-line bg-paper px-3 py-2 outline-none focus:border-steel" />
            <input value={edicion?.marca ?? ''} onChange={(e) => setEdicion((s) => s && { ...s, marca: e.target.value })}
              placeholder="Marca" className="rounded-md border border-line bg-paper px-3 py-2 outline-none focus:border-steel" />
            <div className="flex gap-2">
              <input type="number" min="0" value={edicion?.stockMinimo ?? 0}
                onChange={(e) => setEdicion((s) => s && { ...s, stockMinimo: Number(e.target.value) })}
                placeholder="Stock mínimo" className="flex-1 rounded-md border border-line bg-paper px-3 py-2 outline-none focus:border-steel" />
            </div>
            <div className="flex gap-2">
              <input required type="number" step="0.01" min="0" value={edicion?.precioCompra ?? 0}
                onChange={(e) => setEdicion((s) => s && { ...s, precioCompra: Number(e.target.value) })}
                placeholder="Precio compra" className="flex-1 rounded-md border border-line bg-paper px-3 py-2 outline-none focus:border-steel" />
              <input required type="number" step="0.01" min="0" value={edicion?.precioVenta ?? 0}
                onChange={(e) => setEdicion((s) => s && { ...s, precioVenta: Number(e.target.value) })}
                placeholder="Precio venta" className="flex-1 rounded-md border border-line bg-paper px-3 py-2 outline-none focus:border-steel" />
            </div>
            {error && <p className="text-danger">{error}</p>}
            <div className="flex gap-2">
              <Button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Button>
              <Button type="button" variant="secondary" onClick={() => setEditando(false)}>Cancelar</Button>
            </div>
          </form>
        ) : (
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div><span className="text-sub">Código</span><p>{producto.codigo}</p></div>
            <div><span className="text-sub">Tipo</span><p>{ETIQUETA_TIPO_PRODUCTO[producto.tipoProducto]}</p></div>
            {producto.marca && <div><span className="text-sub">Marca</span><p>{producto.marca}</p></div>}
            {producto.llavePerfil && <div><span className="text-sub">Perfil</span><p>{producto.llavePerfil}</p></div>}
            <div><span className="text-sub">Precio venta</span><p>{formatearSoles(producto.precioVenta)}</p></div>
            <div><span className="text-sub">Stock mínimo</span><p>{producto.stockMinimo}</p></div>
            <Button variant="secondary" onClick={() => setEditando(true)} className="col-span-2">Editar</Button>
          </div>
        )}

        <div className="panel-cepillado flex flex-col gap-3 rounded-lg p-4">
          <span className="text-sm font-medium">Ajustar stock actual: {producto.cantidadStock} unidades</span>
          <div className="flex items-center justify-center gap-4">
            <button
              onClick={() => setCantidadAjuste((c) => c - 1)}
              className="flex h-11 w-11 items-center justify-center rounded-md border border-line hover:bg-hair"
            >
              <Minus size={18} />
            </button>
            <span className="w-16 text-center font-heading text-2xl font-bold">
              {cantidadAjuste > 0 ? `+${cantidadAjuste}` : cantidadAjuste}
            </span>
            <button
              onClick={() => setCantidadAjuste((c) => c + 1)}
              className="flex h-11 w-11 items-center justify-center rounded-md border border-line hover:bg-hair"
            >
              <Plus size={18} />
            </button>
          </div>
          {cantidadAjuste !== 0 && (
            <>
              <input
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="Motivo del ajuste (obligatorio)"
                className="w-full rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel"
              />
              {error && <p className="text-sm text-danger">{error}</p>}
              <Button onClick={confirmarAjuste} disabled={guardando} className="w-full">
                {guardando ? 'Guardando…' : 'Confirmar ajuste'}
              </Button>
            </>
          )}
        </div>

        <div>
          <span className="font-heading mb-2 block text-sm font-semibold">Historial (Kárdex)</span>
          {historial.length === 0 ? (
            <p className="text-sm text-sub">Sin movimientos registrados.</p>
          ) : (
            <ul className="flex flex-col divide-y divide-hair text-sm">
              {historial.slice(0, 10).map((m) => (
                <li key={m.id} className="flex items-center justify-between py-2">
                  <span>
                    {m.tipoMovimiento === 'ENTRADA' ? '+' : m.tipoMovimiento === 'SALIDA' ? '' : ''}
                    {m.cantidad} · {m.tipoMovimiento.toLowerCase()}
                  </span>
                  <span className="text-xs text-sub">{formatearFechaHora(m.fechaHora)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </BottomSheet>
  )
}

const TIPOS_PRODUCTO: TipoProducto[] = [
  'LLAVE_VIRGEN', 'LLAVE_COLOR', 'LLAVE_DISENO', 'CHAPA', 'CILINDRO', 'CANDADO', 'LLAVERO', 'ARGOLLA', 'ACCESORIO',
]

const CAMPO_INICIAL: ProductoCreateRequest = {
  codigo: '', nombre: '', tipoProducto: 'LLAVE_VIRGEN', marca: '',
  cantidadStock: 0, stockMinimo: 5, precioCompra: 0, precioVenta: 0,
  llavePerfil: '', llaveColor: '', llaveDisenoTematica: '', productoModelo: '', productoTamano: '', productoMaterial: '',
}

function FormularioProducto({
  abierto, onCerrar, onCreado,
}: { abierto: boolean; onCerrar: () => void; onCreado: (p: ProductoResponse) => void }) {
  const [form, setForm] = useState<ProductoCreateRequest>(CAMPO_INICIAL)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  function set<K extends keyof ProductoCreateRequest>(campo: K, valor: ProductoCreateRequest[K]) {
    setForm((f) => ({ ...f, [campo]: valor }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      const creado = await productosApi.crear(form)
      setForm(CAMPO_INICIAL)
      onCreado(creado)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  const inputClase = 'rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel'

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Nuevo producto">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <div className="flex gap-2">
          <input required value={form.codigo} onChange={(e) => set('codigo', e.target.value)}
            placeholder="Código" className={`flex-1 ${inputClase}`} />
          <select value={form.tipoProducto} onChange={(e) => set('tipoProducto', e.target.value as TipoProducto)}
            className={`flex-1 ${inputClase}`}>
            {TIPOS_PRODUCTO.map((t) => <option key={t} value={t}>{ETIQUETA_TIPO_PRODUCTO[t]}</option>)}
          </select>
        </div>

        <input required value={form.nombre} onChange={(e) => set('nombre', e.target.value)}
          placeholder="Nombre" className={inputClase} />
        <input value={form.marca} onChange={(e) => set('marca', e.target.value)}
          placeholder="Marca (opcional)" className={inputClase} />

        {/* Atributos especificos segun el tipo elegido (Fase 6 §3.5) */}
        {form.tipoProducto === 'LLAVE_VIRGEN' && (
          <input value={form.llavePerfil} onChange={(e) => set('llavePerfil', e.target.value)}
            placeholder="Perfil (ej. KW1, KW10, Yale, Cantol…)" className={inputClase} />
        )}
        {form.tipoProducto === 'LLAVE_COLOR' && (
          <input value={form.llaveColor} onChange={(e) => set('llaveColor', e.target.value)}
            placeholder="Color" className={inputClase} />
        )}
        {form.tipoProducto === 'LLAVE_DISENO' && (
          <input value={form.llaveDisenoTematica} onChange={(e) => set('llaveDisenoTematica', e.target.value)}
            placeholder="Diseño / temática" className={inputClase} />
        )}
        {['CHAPA', 'CILINDRO', 'CANDADO'].includes(form.tipoProducto) && (
          <input value={form.productoModelo} onChange={(e) => set('productoModelo', e.target.value)}
            placeholder="Modelo" className={inputClase} />
        )}
        {['CANDADO', 'ARGOLLA'].includes(form.tipoProducto) && (
          <input value={form.productoTamano} onChange={(e) => set('productoTamano', e.target.value)}
            placeholder="Tamaño" className={inputClase} />
        )}
        {['LLAVERO', 'ARGOLLA'].includes(form.tipoProducto) && (
          <input value={form.productoMaterial} onChange={(e) => set('productoMaterial', e.target.value)}
            placeholder="Material" className={inputClase} />
        )}

        <div className="flex gap-2">
          <input required type="number" min="0" value={form.cantidadStock}
            onChange={(e) => set('cantidadStock', Number(e.target.value))}
            placeholder="Stock inicial" className={`flex-1 ${inputClase}`} />
          <input type="number" min="0" value={form.stockMinimo}
            onChange={(e) => set('stockMinimo', Number(e.target.value))}
            placeholder="Stock mínimo" className={`flex-1 ${inputClase}`} />
        </div>

        <div className="flex gap-2">
          <input required type="number" step="0.01" min="0" value={form.precioCompra}
            onChange={(e) => set('precioCompra', Number(e.target.value))}
            placeholder="Precio compra (S/)" className={`flex-1 ${inputClase}`} />
          <input required type="number" step="0.01" min="0" value={form.precioVenta}
            onChange={(e) => set('precioVenta', Number(e.target.value))}
            placeholder="Precio venta (S/)" className={`flex-1 ${inputClase}`} />
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Crear producto'}</Button>
      </form>
    </BottomSheet>
  )
}

/**
 * Plantilla para Excel: "sep=;" hace que Excel la abra en columnas sin
 * importar la configuracion regional de la PC. Sin tildes a proposito: sin BOM
 * Excel la lee como ANSI, y con BOM ignoraria la linea "sep=". Precios enteros
 * en el ejemplo: "1.20" en una PC con coma decimal Excel lo toma como fecha.
 */
const PLANTILLA_CSV = [
  'sep=;',
  'codigo;nombre;tipo;marca;stock;stock_minimo;precio_compra;precio_venta',
  'KW1-001;Llave virgen KW1;Llave virgen;Kwikset;50;10;2;5',
  'CAN-40;Candado 40 mm;Candado;Forte;6;2;18;30',
].join('\r\n') + '\r\n'

function descargarPlantilla() {
  const url = URL.createObjectURL(new Blob([PLANTILLA_CSV], { type: 'text/csv' }))
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = 'plantilla-inventario.csv'
  enlace.click()
  URL.revokeObjectURL(url)
}

function ImportarInventario({
  abierto, onCerrar, onImportado,
}: { abierto: boolean; onCerrar: () => void; onImportado: (creados: number) => void }) {
  const [archivo, setArchivo] = useState<File | null>(null)
  const [revision, setRevision] = useState<ImportacionResultado | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [trabajando, setTrabajando] = useState(false)

  function cerrar() {
    setArchivo(null)
    setRevision(null)
    setError(null)
    onCerrar()
  }

  async function elegirArchivo(e: ChangeEvent<HTMLInputElement>) {
    const elegido = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo tras corregirlo
    if (!elegido) return
    setArchivo(elegido)
    setRevision(null)
    setError(null)
    setTrabajando(true)
    try {
      setRevision(await productosApi.importar(elegido, false))
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setTrabajando(false)
    }
  }

  async function confirmar() {
    if (!archivo) return
    setTrabajando(true)
    setError(null)
    try {
      const resultado = await productosApi.importar(archivo, true)
      if (resultado.errores.length > 0) {
        setRevision(resultado) // algo cambio desde la revision (ej. otro alta con el mismo codigo)
      } else {
        setArchivo(null)
        setRevision(null)
        onImportado(resultado.creados)
      }
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setTrabajando(false)
    }
  }

  const listo = revision !== null && revision.errores.length === 0 && revision.productos.length > 0

  return (
    <BottomSheet abierto={abierto} onCerrar={cerrar} titulo="Importar inventario">
      <div className="flex flex-col gap-4 text-sm">
        <ol className="list-decimal space-y-1 pl-5 text-sub">
          <li>
            Descarga la{' '}
            <button type="button" onClick={descargarPlantilla} className="text-steel underline">plantilla</button>{' '}
            y llénala en Excel (una fila por producto).
          </li>
          <li>Guárdala con <b>Archivo → Guardar como → CSV UTF-8</b>.</li>
          <li>Elige el archivo: primero se revisa, y nada se guarda hasta que confirmes.</li>
        </ol>
        <p className="text-xs text-sub">
          Obligatorias: codigo, nombre, tipo, precio_compra, precio_venta. Opcionales: marca, stock, stock_minimo,
          perfil, color, diseno, modelo, tamano, material. Solo agrega productos nuevos: si un código ya existe,
          cambia su stock o precio desde su ficha.
        </p>

        <label className="cursor-pointer self-start">
          <span className="inline-flex min-h-11 items-center rounded-md border border-line px-4 py-2.5 font-medium hover:bg-hair">
            {trabajando && !revision ? 'Revisando…' : archivo ? 'Elegir otro archivo' : 'Elegir archivo CSV'}
          </span>
          <input type="file" accept=".csv,text/csv" className="hidden" disabled={trabajando} onChange={elegirArchivo} />
        </label>
        {archivo && <p className="text-xs text-sub">{archivo.name}</p>}

        {revision && (
          <div className="flex flex-col gap-3">
            {revision.columnasIgnoradas.length > 0 && (
              <p className="text-xs text-sub">Columnas ignoradas: {revision.columnasIgnoradas.join(', ')}</p>
            )}
            {revision.errores.length > 0 ? (
              <div className="flex flex-col gap-2">
                <p className="font-medium text-danger">
                  {revision.errores.length === 1 ? 'Hay 1 problema' : `Hay ${revision.errores.length} problemas`}.
                  Corrígelos en Excel y vuelve a elegir el archivo (no se importó nada):
                </p>
                <ul className="max-h-60 overflow-y-auto rounded-md border border-hair">
                  {revision.errores.slice(0, 100).map((e, i) => (
                    <li key={i} className="border-b border-hair px-3 py-1.5 last:border-0">
                      {e.fila > 0 && <span className="font-medium">Fila {e.fila}: </span>}
                      {e.mensaje}
                    </li>
                  ))}
                </ul>
                {revision.errores.length > 100 && (
                  <p className="text-xs text-sub">…y {revision.errores.length - 100} más.</p>
                )}
              </div>
            ) : (
              revision.productos.length > 0 && (
                <div className="flex flex-col gap-2">
                  <p className="font-medium">{revision.productos.length} productos listos para importar:</p>
                  <ul className="max-h-60 overflow-y-auto rounded-md border border-hair">
                    {revision.productos.slice(0, 50).map((p) => (
                      <li key={p.fila} className="flex items-center justify-between gap-2 border-b border-hair px-3 py-1.5 last:border-0">
                        <span className="min-w-0 truncate">
                          <span className="text-sub">{p.codigo}</span> · {p.nombre}
                        </span>
                        <span className="shrink-0 text-xs text-sub">
                          {ETIQUETA_TIPO_PRODUCTO[p.tipoProducto]} · {p.cantidadStock} und · {formatearSoles(p.precioVenta)}
                        </span>
                      </li>
                    ))}
                  </ul>
                  {revision.productos.length > 50 && (
                    <p className="text-xs text-sub">…y {revision.productos.length - 50} más.</p>
                  )}
                </div>
              )
            )}
          </div>
        )}

        {error && <p className="text-danger">{error}</p>}
        {listo && (
          <Button onClick={confirmar} disabled={trabajando}>
            {trabajando ? 'Importando…' : `Importar ${revision.productos.length} productos`}
          </Button>
        )}
      </div>
    </BottomSheet>
  )
}
