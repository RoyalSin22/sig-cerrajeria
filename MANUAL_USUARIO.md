# Manual de Usuario — SIG-Cerrajería

Esta es la guía para usar el sistema en el día a día del mostrador. No necesitas saber de computación — cada sección explica un paso concreto.

## Entrar al sistema

Abre la dirección que te dieron (en la PC del local, o desde el celular si está en la red). Escribe tu usuario y contraseña, y toca **Ingresar**. Si no estás seguro de lo que escribiste en la contraseña, toca **Ver** para mostrarla.

**Cómo moverse:**
- **En la PC**, el menú está siempre a la izquierda: Inicio, Vender, Taller, Inventario, Domicilio, Caja, Clientes, Compras, Reportes y Ajustes.
- **En el celular**, el menú está abajo, al alcance del pulgar: **Inicio, Vender, Taller, Stock** (el inventario) y **Más**, que abre el resto.
- Arriba siempre se ve si la caja está **abierta** (verde) o **cerrada** (naranja). Tocándolo vas directo a Caja.

La pantalla de **Inicio** tiene botones grandes para lo de todos los días: **Duplicado de llave**, **Vender**, **Recibir trabajo**, **Ver stock**, **Domicilio** y **Caja**. Debajo está el resumen del día.

### Letra más grande o más chica

En **Ajustes → Tamaño de la letra** elige **Normal**, **Grande** (viene así) o **Muy grande**. Cambia todo: textos, botones y campos. Cada equipo recuerda su tamaño, así que la PC y el celular pueden tener uno distinto.

### Instalarla en el celular

No hace falta bajar el sistema de una tienda de aplicaciones. Lo que sí necesita el celular es la app **Tailscale** (de Play Store), conectada con la cuenta de la cerrajería: es la que lo une con la PC del local, estés en el local o fuera. La configuración de una sola vez está en [INSTALACION.md](./INSTALACION.md) → "Usar el sistema desde el celular".

Con Tailscale conectado, entra desde el navegador del celular (Chrome en Android, Safari en iPhone) a la dirección del sistema (`https://….ts.net`, te la da quien lo instaló) y:

- **Android (Chrome):** toca los tres puntos → "Instalar aplicación" (o "Agregar a pantalla de inicio"). Aparece el ícono de la llave junto a tus demás apps.
- **iPhone (Safari):** toca el botón de compartir (el cuadrado con la flecha) → "Agregar a pantalla de inicio".

Se abre en pantalla completa, sin la barra del navegador, como cualquier app instalada. Los datos (ventas, stock, caja) siempre se piden en vivo al servidor — no se guardan viejos en el celular, así que hace falta conexión para trabajar, pero nunca vas a ver un número de stock o de caja desactualizado.

## Lo primero de cada día: abrir caja

Antes de vender o cobrar cualquier cosa, tienes que abrir la caja. El sistema no te deja registrar una venta sin esto — es a propósito, para que nunca haya dinero sin cuenta.

Entra a **Caja** (tocando "Caja cerrada" arriba, o desde el menú), pon el monto con el que arrancas el día (por ejemplo, `S/ 100.00`) y toca **Abrir caja**. Vas a ver un punto verde en la barra de arriba que dice "Caja abierta" mientras dure tu turno.

## Vender

Ve a **Vender**. Busca el producto por nombre o código (por ejemplo "KW1" o "chapa yale") y tócalo para agregarlo. Puedes sumar o restar cantidad con los botones `+` y `−` o escribirla directamente (por ejemplo `25`), o quitarlo con **Quitar**.

Abajo aparece el total. Elige cómo te pagan (**Efectivo**, **Yape**, **Plin**, **Tarjeta**, o **Mixto** si es una combinación) y toca el botón grande **Cobrar**, que muestra el monto a cobrar.

Si eliges **Mixto**, el sistema te pide cuánto pagaron en cada método — por ejemplo `S/ 20.00` en efectivo y `S/ 15.00` en Yape — y no te deja cobrar hasta que la suma cuadre exactamente con el total.

### Duplicado de llave (sin datos del cliente)

Para un duplicado no hace falta registrar a nadie. Son tres toques:

1. **Duplicado de llave** (en Inicio o en Vender).
2. La llave (puedes buscarla por nombre o código).
3. Cuántas copias: **1**, **2**, **3** o **4** (cada botón muestra el total). Para otra cantidad, escríbela en "Otra cantidad".

Después, **Cobrar**. Se cobra como cualquier venta y una llave virgen sale del stock por cada copia.

Cada llave tiene dos precios, que el Administrador pone en su ficha (Inventario → la llave → Editar):

| Precio | Ejemplo (llave de casa) | Cuándo se cobra |
|---|---|---|
| **Precio del duplicado** (material + corte) | S/ 2.50 | Con el botón **Duplicado de llave**, o con **Duplicado** al buscarla en Venta |
| **Llave sola** | lo que cobres por la llave sin duplicar | Muy rara vez: a otro cerrajero o de adorno. Botón **Sola** al buscarla |

El **precio de compra** (S/ 0.90 en el ejemplo, lo que te cuesta el material) es aparte y no se cobra. Una llave sin precio del duplicado no aparece en **Duplicado de llave**. En el carrito, los duplicados se ven como "Duplicado · Llave de casa".

Si te equivocaste en una venta, un Administrador puede anularla desde **Historial de ventas** (el link está arriba de la pantalla de Venta). Eso devuelve el stock y descuenta el dinero de la caja, siempre que la caja de esa venta siga abierta.

### Servicio sin producto (abrir un candado, afilar, etc.)

Para cobrar un trabajo que no saca nada del inventario, toca **Servicio** (al lado de Duplicado de llave). Elige uno de los comunes (Abrir candado, Abrir puerta, Cambio de combinación, Instalación de chapa, Reparación de licuadora, Afilar cuchilla) o escribe otro, pon el precio y toca **Agregar**. Se cobra junto con lo demás del carrito, entra a la caja como cualquier venta y no mueve el stock. En el carrito se ve como "Servicio · Abrir candado".

## Recibir una reparación (Taller)

Ve a **Taller** y toca **Nueva orden**. Busca al cliente (o créalo ahí mismo si es nuevo), describe qué trajo y cuál es el problema, y pon el precio total del trabajo. Si te deja un adelanto, anótalo ahí mismo — el sistema calcula solo el saldo pendiente.

Las órdenes se ven organizadas en columnas según su estado: Recibido, En reparación, Esperando repuesto, Listo, Entregado. Toca una orden para ver su detalle y avanzarla de estado.

**Importante:** si la orden tiene saldo pendiente, el sistema no te deja marcarla como Entregada sin cobrar ese saldo primero. Cuando el cliente viene a recoger, toca la orden, y si debe algo, te va a pedir el monto y el método de pago antes de dejarte cerrar la entrega. Esto es a propósito: evita que un trabajo salga sin haberse cobrado del todo.

**Cancelar una orden.** Si el cliente se arrepiente o nunca vuelve a recoger su trabajo, abre la orden y toca **Cancelar orden**. Tienes que escribir el motivo y, si dejó adelanto, elegir:
- **Devolverlo**: sale de **tu** caja (tienes que tenerla abierta) por el método que elijas. Si es en efectivo, descuéntalo del cajón: el cierre ya lo tiene en cuenta.
- **No devolverlo** (por ejemplo, si nunca la recogió): el adelanto se queda como ingreso.

Una orden cancelada ya no se puede cobrar ni cambiar de estado, y aparece en su propia columna "Cancelado".

**Material usado.** En una orden en curso, en **Material usado**, busca la pieza del inventario (un cilindro, un resorte), pon la cantidad y toca **Usar**: sale del stock y queda en el Kárdex con el número de la orden. Esto no cambia el precio de la orden: el precio lo pones tú al crearla.

**Avisar por WhatsApp.** En el detalle de una orden hay dos botones que abren WhatsApp con el mensaje ya escrito al teléfono del cliente: **Enviar comprobante** (qué dejó, número de orden y saldo pendiente) y, cuando la orden está en **Listo**, **Avisar que ya está listo**. Solo tienes que tocar Enviar en WhatsApp. Si el cliente no tiene un celular válido (9 dígitos), te avisa en vez de mostrar los botones. Al crear una orden, su detalle se abre solo para que mandes el comprobante enseguida.

**Garantía.** Si el cliente vuelve porque el trabajo falló, abre la orden original (columna **Entregado**) y toca **Garantía**. Escribe qué falló y se crea una orden nueva **sin cobro**, marcada "Garantía de OT-…", que sigue el mismo camino (en reparación, listo, entregado). No pide caja ni mueve dinero. Si usas una pieza, regístrala en **Material usado** para que salga del stock.

## Agendar un servicio a domicilio

En **Domicilio**, toca **Agendar servicio**. Busca al cliente, pon la dirección, fecha, hora y qué tipo de trabajo es (por ejemplo "Cambio de chapa"). La pantalla tiene vista por semana o por mes — cambia con los botones "Semana" / "Mes" arriba.

Cuando el técnico sale, marca el servicio como "En camino". Al terminar, toca **Realizado y cobrar** y anota cuánto cobró y cómo (efectivo, Yape o Plin). Ese cobro entra en la caja **de quien lo marca**, así que el técnico tiene que tener su caja abierta, aunque esté fuera del local (lo hace desde el celular). Si el servicio no se cobra (garantía, cortesía), pon 0: no pasa por la caja.

Un servicio realizado ya no se puede cambiar, para no descuadrar la caja de quien lo cobró. Si un servicio no se hace, tócalo en **Cancelar** (y "Volver a programar" si se reprograma).

## Inventario

En **Inventario** puedes explorar todo el catálogo sin buscar nada (se muestra por página, con "Cargar más" abajo), o escribir para buscar un producto puntual. El chip "Stock bajo" te muestra solo lo que se está por acabar.

Toca un producto para ver su ficha: ahí puedes subir o cambiar su foto, editar el precio o la marca, y ajustar el stock:

1. Elige **Entran** (llegó mercadería) o **Salen** (se dañó, se perdió).
2. Escribe **cuántas** (por ejemplo `1000` si llegó un millar de llaves) o usa los atajos **+1**, **+10**, **+100**, **+500**, **+1000**.
3. Ves cuántas **quedarán** antes de guardar.
4. Toca un motivo (**Llegó mercadería**, **Conteo físico**, **Producto dañado**…) o escríbelo, y toca **Guardar**.

Para dar de alta un producto nuevo, toca **Nuevo producto** arriba. Los campos que te pide cambian según el tipo que elijas (una llave virgen pide "perfil", un candado pide "tamaño", etc.).

### Cambiar muchas cantidades y precios con Excel

Para no cambiar producto por producto, el Administrador toca **Cantidades y precios (Excel)** en Inventario:

1. **Descarga el inventario**: baja un archivo con todos los productos (código, nombre, tipo, marca y stock actual), una columna **cantidad** vacía y los precios actuales (**precio_compra**, **precio_venta** y **precio_duplicado**).
2. Ábrelo en Excel y escribe la cantidad solo en las filas que quieras cambiar (las vacías no se tocan). Corrige los precios que quieras: por ejemplo, para poner el duplicado de todas las llaves de casa a S/ 2.50, escribe `2.50` en la primera y arrastra hacia abajo. El precio de duplicado solo va en llaves.
3. Guárdalo (Ctrl+S, como CSV) y súbelo eligiendo qué significa el número:
   - **Lo que conté (reemplaza)**: el número pasa a ser el stock. Sirve para el inventario físico.
   - **Lo que llegó (se suma)**: el número se suma al stock. Sirve cuando llega mercadería; si además quieres registrar lo que gastaste, usa **Compras** en vez de esto.
4. Antes de guardar ves cada cambio (por ejemplo `Stock 200 → 1200` o `Duplicado S/ 2.00 → S/ 2.50`). Si hay un problema, como un código que no existe, una cantidad con decimales o un precio de duplicado en algo que no es llave, no se cambia nada hasta que lo corrijas.

Los precios se cambian tal cual, sin importar si elegiste "conté" o "llegó". Cada cambio de stock queda en el Kárdex del producto con el motivo "Conteo físico (planilla)" o "Mercadería recibida (planilla)".

### Cargar muchos productos de una vez (Importar)

Si tienes tu inventario en Excel, el Administrador puede cargarlo todo junto con el botón **Importar**:

1. Toca **Importar** y descarga la **plantilla**. Ábrela en Excel y llena una fila por producto (puedes borrar los dos ejemplos).
2. Guárdala con **Archivo → Guardar como → CSV UTF-8** (no como libro de Excel `.xlsx`).
3. Elige ese archivo. El sistema lo **revisa primero y no guarda nada todavía**: te muestra los productos que leyó, o la lista de problemas con su número de fila (el mismo que ves en Excel).
4. Si hay problemas, corrígelos en Excel, vuelve a guardar y elige el archivo otra vez. Cuando no hay ninguno, toca **Importar N productos**.

| Columna | ¿Obligatoria? | Ejemplo |
|---|---|---|
| `codigo` | Sí — no puede repetirse | `KW1-001` |
| `nombre` | Sí | `Llave virgen KW1` |
| `tipo` | Sí | `Llave virgen`, `Llave de color`, `Llave de diseño`, `Chapa`, `Cilindro`, `Candado`, `Llavero`, `Argolla`, `Accesorio` |
| `precio_compra`, `precio_venta` | Sí | `3.50` o `3,50` (también acepta `S/ 3.50`) |
| `stock` | No (vacío = 0) | `50` |
| `stock_minimo` | No (vacío = 5) | `10` |
| `marca`, `perfil`, `color`, `diseno`, `modelo`, `tamano`, `material` | No | `Kwikset` |

Bueno saber:
- **Es todo o nada**: si una sola fila tiene un error, no se carga ninguna, así nunca queda una carga a medias.
- **Solo agrega productos nuevos.** Si un código ya existe, esa fila se marca como problema: para cambiar el stock o el precio de algo que ya está cargado, usa su ficha (el ajuste de stock pide motivo).
- El stock que cargues queda en el Kárdex de cada producto como "Stock inicial importado desde archivo".
- Las columnas que el sistema no reconoce (por ejemplo una de "notas") se ignoran y te avisa cuáles fueron.

### Repuestos de licuadora

Las cuchillas, vasos, empaques y demás repuestos se cargan con el tipo **Repuesto de licuadora** (en el Excel de importación vale escribir "repuesto" o "licuadora"). El chip **Licuadora** de Inventario muestra solo esos.

### Colores

Los colores de la app son los de la cerrajería: amarillo con negro. El amarillo va siempre de fondo con letra negra (amarillo sobre blanco no se lee). Además, están elegidos para leerse bien también con daltonismo (paleta Okabe-Ito en los gráficos y contraste alto en textos y avisos). Los avisos no dependen solo del color: el stock bajo, por ejemplo, lleva un ícono de alerta y la palabra "bajo".

## Clientes

En **Clientes**, busca por nombre o teléfono, o explora la lista completa si no buscas nada. Toca **Nuevo cliente** para registrar uno. Dentro de la ficha de cada cliente puedes ver su historial de compras y de órdenes de trabajo, y editar sus datos.

## Comprar a los distribuidores

### La lista de compras

En **Compras → Lista de compras** armas lo que hay que ir a comprar. El botón **Agregar** suma de una vez todo lo que está en stock bajo, con una cantidad sugerida (lo que falta para llegar al doble del mínimo), y con el buscador agregas cualquier otro producto. Cambia las cantidades a mano: por ejemplo, `1000` llaves.

Con **Enviar por WhatsApp** la mandas a tu celular o a quien vaya a comprar, y con **Copiar** la pegas donde quieras. La lista se guarda en el equipo donde la armaste (no se comparte entre la PC y el celular).

### Registrar lo que compraste

Al volver, toca **Ya compré: registrar la compra** en la lista: se abre **Registrar compra** con los productos y cantidades ya puestos. También puedes entrar directo a **Compras** y agregar los productos con el buscador.

Elige el distribuidor (o crea uno nuevo ahí mismo), revisa cantidad y precio unitario de cada producto (el precio viene del precio de compra del producto) y toca **Registrar compra**. Todo se guarda junto: si algo falla, no se guarda nada. Lo comprado sale solo de la lista de compras. Si marcas "Pagar de la caja activa", el total sale de tu caja del día — para eso necesitas tener la caja abierta.

## Cerrar caja

Al final del turno, entra a **Caja**. Vas a ver el resumen: monto de apertura, ingresos, egresos, lo cobrado por Yape y por Plin, y el **efectivo esperado en caja**. Yape y Plin no están en el cajón, así que no se cuentan. Cuenta solo el efectivo real que tienes y ponlo en "Efectivo contado". Mientras escribes, el sistema te dice si cuadra, cuánto sobra o cuánto falta.

**Si no cuadra**, tienes que escribir qué pasó (por ejemplo "se dio mal un vuelto" o "se pagó un delivery con plata del cajón") antes de poder cerrar. La diferencia y tu explicación quedan guardadas en **Historial de caja** y en la auditoría, para revisarlas después.

## Reportes

Desde el menú ☰, entra a **Reportes**. Elige un rango de fechas y consulta:
- **Ingresos totales**, que suman las ventas de mostrador, el taller y los servicios a domicilio.
- Por separado, cada uno de esos tres. El taller cuenta los adelantos y liquidaciones cobrados en esas fechas, menos las devoluciones de órdenes canceladas. El domicilio cuenta lo cobrado en los servicios realizados en esas fechas.
- Las ventas por método de pago, los productos más vendidos y el valor total de tu inventario.
- **Servicios realizados**: cuántos domicilios se hicieron y cuánto se cobró, separados por tipo de trabajo y por técnico, más las órdenes del taller recibidas, entregadas y las que siguen abiertas.
- **Clientes que más dejaron**: un ranking que suma lo que cada cliente gastó en compras, taller y domicilio. Las ventas de mostrador sin cliente no entran, porque no se sabe de quién son.

## El asistente

En la esquina inferior derecha hay un botón redondo (ícono de robot). Ábrelo y pregúntale en lenguaje normal — entiende cosas como:

- "¿Cuántas llaves KW1 quedan?"
- "Registra una venta de 2 llaves Cantol y 1 llavero"
- "Agenda un cambio de chapa mañana a las 9am para [un cliente ya registrado]"
- "Muéstrame los trabajos pendientes"

Si te pide una acción con dinero (como registrar una venta) y no tienes la caja abierta, te lo va a decir en vez de hacerlo — las mismas reglas que rigen el resto del sistema aplican acá también. Si algo que le pides es ambiguo (dos productos con nombres parecidos, un cliente que no existe), te va a preguntar en vez de adivinar.

## Cambiar tu contraseña

En **Ajustes → Contraseña**, toca **Cambiar**, escribe tu contraseña actual y la nueva dos veces. Al guardarla se cierra tu sesión (y cualquier otra que tuvieras abierta, por ejemplo en el celular): vuelve a entrar con la nueva.

Si un empleado olvida su contraseña, el Administrador entra a **Ajustes → Usuarios**, toca **Restablecer contraseña** junto a su nombre y le pone una temporal. Después el empleado la cambia por una propia.

## Datos de la cerrajería

En **Ajustes → Datos de la cerrajería** (solo el Administrador) se ponen el nombre, el teléfono/WhatsApp y la dirección. Salen en la pantalla de entrada, en el menú y en los mensajes de WhatsApp a los clientes.

## Administrador vs. Empleado

El Administrador puede, además de todo lo anterior: crear y desactivar usuarios, restablecer contraseñas, anular ventas, ver el registro de auditoría (quién hizo qué, cuándo, qué cambió —por ejemplo un precio de S/ 2.00 a S/ 2.50— y por qué), crear y editar productos, ajustar el stock, cambiar fotos, importar el inventario desde Excel y registrar compras. Un Empleado puede vender, atender órdenes, agendar y cobrar servicios a domicilio, registrar clientes, consultar el inventario y ver el historial de compras. Los botones de administración no le aparecen.
