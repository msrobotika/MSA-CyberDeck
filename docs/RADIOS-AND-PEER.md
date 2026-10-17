# Bluetooth, NFC y sesiones locales

Estas funciones son nativas Android y no necesitan el ESP conectado.

## Bluetooth

En Bluetooth, concede los permisos e inicia una búsqueda manual de 10 segundos. La app muestra anuncios, RSSI y UUID disponibles. Una dirección puede ser aleatoria y el nombre puede ser falso; ni identidad ni distancia se deducen de esos datos.

Consulta GATT solo en un equipo propio o autorizado y confirma la conexión. Se descubren servicios y propiedades de características. No se leen ni escriben características. Las propiedades READ/WRITE no demuestran que se pueda acceder sin autenticación. Puedes guardar las observaciones como evidencia. Se detiene al pasar a segundo plano.

## NFC

Pulsa Leer etiqueta NFC y acerca tu etiqueta durante la ventana de 30 segundos. Se muestran identificador y tecnologías; si Android ofrece NDEF, se muestran sus registros de texto, URI o metadatos. Los enlaces no se abren automáticamente. Límite de 64 KiB y 32 registros. No hay escritura, autenticaciones ni comandos APDU personalizados.

Guardar informe NFC conserva el diagnóstico en Evidencias. No permite usar el teléfono como la tarjeta inspeccionada. Una tarjeta de fichaje puede ser detectada sin ofrecer contenido NDEF; eso no demuestra que esté vacía ni que sea defectuosa. Si una lectura NDEF falla se conserva la identificación observada y el error, sin afirmar que se ha leído el contenido.

Para fichar con el móvil hace falta que el proveedor del sistema permita emitir una credencial compatible. Android HCE no es una copia universal de tarjetas: trabaja con ISO-DEP/APDU y su UID debe considerarse aleatorio en cada aproximación. No permite prometer la reproducción del UID de una tarjeta física. Consulta la [documentación oficial de HCE](https://developer.android.com/develop/connectivity/nfc/hce).

Los informes pueden contener identificadores o texto sensible. Revisa lo que exportas. No se suben automáticamente.

## Sesiones entre dos móviles

1. Conecta ambos a una red local de confianza que permita comunicación entre clientes y abre CyberDeck en ambos.
2. En el receptor, abre Sesiones e inicia la recepción. Copia la invitación temporal y compártela únicamente con el remitente autorizado.
3. En el remitente, pega la invitación y elige un archivo mediante el selector de Android o envía un informe.
4. El receptor revisa nombre, tamaño y SHA-256 y acepta o rechaza. La app solo confirma la entrega tras recibir la confirmación del guardado con el mismo hash.

TLS 1.2/1.3, certificado fijado mediante SHA-256 en la invitación y token aleatorio de un solo uso. Invitación de 2 minutos y aprobación de 45 segundos; archivo máximo de 16 MiB. Las sesiones se cancelan al pasar a segundo plano. Durante la sesión la pantalla se mantiene encendida; no bloquees el móvil durante la transferencia.

No hay servidor en la nube, sincronización de cuentas ni acceso a archivos ajenos al seleccionado. Esta versión utiliza IPv4 local y pegar la invitación; no incluye emparejamiento QR ni Wi-Fi Direct. La invitación contiene un secreto temporal: no la publiques. El pin asegura que se contacta al receptor indicado por quien te entrega la invitación; no verifica por sí mismo la identidad de esa persona.
