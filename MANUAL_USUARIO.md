# Manual de Usuario — SIG-Cerrajería

Esta es la guía para usar el sistema en el día a día del mostrador. No necesitas saber de computación — cada sección explica un paso concreto.

## Entrar al sistema

Abre la dirección que te dieron (en la PC del local, o desde el celular si está en la red). Escribe tu usuario y contraseña, y toca **Ingresar**.

Vas a ver cinco pestañas arriba: **Inicio, Venta, Inventario, Taller, Domicilio**. Y un botón con tres rayitas (☰) a la izquierda, para todo lo demás: Clientes, Compras, Caja, Reportes, Ajustes.

### Instalarla en el celular

No hace falta bajar nada de una tienda de aplicaciones. Desde el navegador del celular (Chrome en Android, Safari en iPhone), entra a la dirección del sistema y:

- **Android (Chrome):** toca los tres puntos → "Instalar aplicación" (o "Agregar a pantalla de inicio"). Aparece el ícono de la llave junto a tus demás apps.
- **iPhone (Safari):** toca el botón de compartir (el cuadrado con la flecha) → "Agregar a pantalla de inicio".

Se abre en pantalla completa, sin la barra del navegador, como cualquier app instalada. Los datos (ventas, stock, caja) siempre se piden en vivo al servidor — no se guardan viejos en el celular, así que hace falta conexión para trabajar, pero nunca vas a ver un número de stock o de caja desactualizado.

## Lo primero de cada día: abrir caja

Antes de vender o cobrar cualquier cosa, tienes que abrir la caja. El sistema no te deja registrar una venta sin esto — es a propósito, para que nunca haya dinero sin cuenta.

Entra a **Caja** (desde el menú ☰), pon el monto con el que arrancas el día (por ejemplo, `S/ 100.00`) y toca **Abrir caja**. Vas a ver un punto verde en la barra de arriba que dice "Caja abierta" mientras dure tu turno.

## Vender

Ve a la pestaña **Venta**. Busca el producto por nombre o código (por ejemplo "KW1" o "chapa yale") y tócalo para agregarlo. Puedes sumar o restar cantidad con los botones `+` y `−`, o quitar el producto del todo con el ícono de basura.

Abajo aparece el total. Elige cómo te pagan (Efectivo, Yape, Plin, Tarjeta, o Mixto si es una combinación) y toca **Cobrar**.

Si eliges **Mixto**, el sistema te pide cuánto pagaron en cada método — por ejemplo `S/ 20.00` en efectivo y `S/ 15.00` en Yape — y no te deja cobrar hasta que la suma cuadre exactamente con el total.

Si te equivocaste en una venta, un Administrador puede anularla desde **Historial de ventas** (el link está arriba de la pantalla de Venta). Eso devuelve el stock y descuenta el dinero de la caja, siempre que la caja de esa venta siga abierta.

## Recibir una reparación (Taller)

Ve a **Taller** y toca **Nueva orden**. Busca al cliente (o créalo ahí mismo si es nuevo), describe qué trajo y cuál es el problema, y pon el precio total del trabajo. Si te deja un adelanto, anótalo ahí mismo — el sistema calcula solo el saldo pendiente.

Las órdenes se ven organizadas en columnas según su estado: Recibido, En reparación, Esperando repuesto, Listo, Entregado. Toca una orden para ver su detalle y avanzarla de estado.

**Importante:** si la orden tiene saldo pendiente, el sistema no te deja marcarla como Entregada sin cobrar ese saldo primero. Cuando el cliente viene a recoger, toca la orden, y si debe algo, te va a pedir el monto y el método de pago antes de dejarte cerrar la entrega. Esto es a propósito: evita que un trabajo salga sin haberse cobrado del todo.

## Agendar un servicio a domicilio

En **Domicilio**, toca **Agendar servicio**. Busca al cliente, pon la dirección, fecha, hora y qué tipo de trabajo es (por ejemplo "Cambio de chapa"). La pantalla tiene vista por semana o por mes — cambia con los botones "Semana" / "Mes" arriba.

Cuando el técnico sale, marca el servicio como "En camino", y al terminar, "Realizado".

## Inventario

En **Inventario** puedes explorar todo el catálogo sin buscar nada (se muestra por página, con "Cargar más" abajo), o escribir para buscar un producto puntual. El chip "Stock bajo" te muestra solo lo que se está por acabar.

Toca un producto para ver su ficha: ahí puedes subir o cambiar su foto, editar el precio o la marca, y ajustar el stock con los botones `+`/`−` — el sistema te va a pedir un motivo (por ejemplo "conteo físico" o "producto dañado") antes de guardar el ajuste, para que quede registrado por qué cambió.

Para dar de alta un producto nuevo, toca **Nuevo producto** arriba. Los campos que te pide cambian según el tipo que elijas (una llave virgen pide "perfil", un candado pide "tamaño", etc.).

## Clientes

En **Clientes**, busca por nombre o teléfono, o explora la lista completa si no buscas nada. Toca **Nuevo cliente** para registrar uno. Dentro de la ficha de cada cliente puedes ver su historial de compras y de órdenes de trabajo, y editar sus datos.

## Comprar a un proveedor

En **Compras**, elige un proveedor de la lista (o crea uno nuevo ahí mismo), busca el producto, pon cantidad y precio unitario. Si marcas "Pagar de la caja activa", el sistema descuenta el monto de tu caja del día — para eso necesitas tener la caja abierta.

## Cerrar caja

Al final del turno, entra a **Caja**. Vas a ver el resumen: monto de apertura, ingresos, egresos, lo cobrado por Yape y por Plin, y el **efectivo esperado en caja**. Yape y Plin no están en el cajón, así que no se cuentan. Cuenta solo el efectivo real que tienes y ponlo en "Efectivo contado". El sistema te muestra la diferencia — si sobra, falta, o cuadra exacto.

## Reportes

Desde el menú ☰, entra a **Reportes**. Elige un rango de fechas y consulta: total vendido, ventas por método de pago, los productos más vendidos, y el valor total de tu inventario.

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

## Administrador vs. Empleado

El Administrador puede, además de todo lo anterior: crear y desactivar usuarios, restablecer contraseñas, anular ventas, ver el registro de auditoría (quién hizo qué y cuándo), y editar productos. Un Empleado puede vender, atender órdenes, agendar servicios y registrar clientes, pero no ve esas pantallas de administración.
