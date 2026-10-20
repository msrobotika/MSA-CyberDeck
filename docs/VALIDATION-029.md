# CiberDeck 0.29 · Inspección QR desde imágenes

## Función

«Enlaces y QR» conserva el inspector de direcciones y añade lectura de un QR desde una captura o imagen elegida en el selector Android. La decodificación ocurre en el móvil con ZXing, biblioteca ya utilizada por el proyecto. No se añade cámara, permiso de cámara, búsqueda de archivos ni servicio de reputación. Un proveedor externo del selector puede necesitar su propia conexión para entregar el archivo elegido; CiberDeck no visita el contenido decodificado.

Se distingue enlace HTTP(S), configuración Wi-Fi, contacto, texto y otros esquemas, incluidos intent/javascript/data/file/content. La clasificación no ejecuta nada: no abre páginas, sigue redirecciones, conecta Wi-Fi, modifica contactos ni lanza aplicaciones. Los enlaces compatibles pasan por el inspector sintáctico existente, con destino real y señales para revisar; no se certifican legitimidad o ausencia de fraude. Un formato web inválido no se presenta como un enlace verificado.

El contenido completo queda oculto inicialmente y al cambiar la observación. Se muestra solo al pedirlo y se puede volver a ocultar o descartar. Se conserva en memoria de sesión durante recreaciones; no es un almacén de credenciales. El informe guarda tipo, fecha, longitud, huella SHA-256 de los bytes de imagen elegidos y, si corresponde, el análisis del enlace sin su dirección completa. No guarda el payload, claves Wi-Fi, datos completos de contacto o texto, ni copia la imagen al almacén. El hash de la imagen no autentica al emisor ni certifica la veracidad del contenido.

Límites: hasta 20 MiB de imagen, dimensiones de origen hasta 50 000 por eje, reducción previa a unos 2048 píxeles por eje y máximo 4 194 304 píxeles decodificados, contenido de hasta 16 384 caracteres. Se intenta QR normal e invertido; se informa cuando no hay un código legible. Solo se devuelve un QR, no un inventario de todos los códigos de una imagen. Imágenes borrosas, pequeñas, sin margen o muy reducidas pueden fallar.

## Agente

La acción existente `LINK` incorpora una modalidad elegida de imagen QR; el catálogo mantiene treinta adaptadores. «Lee un QR de una imagen» es una tarea directa que abre el selector Android, espera una elección y utiliza el mismo lector real. En un plan manual con LINK se puede marcar la opción de imagen en vez de escribir una URL. El modo web continúa necesitando URL y autorización de red por separado. Sin runtime Android, el modo QR no inventa un resultado y falla indicando que necesita el selector.

El texto del QR se trata como dato, no como instrucciones para el agente. Su informe no incorpora el payload completo a futuras interpretaciones. Las acciones posteriores del plan siguen sujetas a sus controles y destinos; el QR no los rellena automáticamente.

## Archivos

- `QrInspection.kt`: decodificación de píxeles, clasificación e informe limitado.
- `QrImageReader.kt`: lectura acotada de URI, Bitmap Android, reducción y huella de imagen.
- `QrInspectionPanel.kt`, `MainActivity.kt`, `DeckSessionModel.kt`: selector, privacidad de presentación y sesión.
- `AgentPolicy.kt`, `AgentActionRuntime.kt`, `AgentTools.kt`, `AgentTasks.kt`, `AgentPanel.kt`: modalidad QR de LINK, tarea directa y selección Android.
- `DeckHome.kt`, `DeckCatalog.kt`: acceso «Enlaces y QR» y búsqueda por QR/imagen/captura.
- `QrInspectionTest.kt`, `QrImageDeviceTest.kt`: casos de decodificación, límites, privacidad y UI.

## Validación

- Compilación final de aplicación, instrumentación y pruebas Kotlin correcta en **2 min 16 s**, después de una candidata correcta en 2 min 37 s y del ajuste para reiniciar el estado de revelado al cambiar la observación. Lint sin errores/fatales: **24 avisos y una sugerencia**. Permanecen las advertencias de API/Gradle y del icono de escudo de 0.28.
- **226 pruebas JVM correctas**, incluidos diez casos nuevos: decodificación QR de píxeles reales generados para el ensayo, imagen invertida, privacidad de Wi-Fi/URL, clasificación de esquemas, entradas inválidas, límites y tarea QR del agente. Una invocación anticipada del runner coincidió con la regeneración del directorio de clases y falló al cargar `AgentPlan`; no fue una prueba válida. Se ejecutó de nuevo una vez terminada la compilación, con 226/226 correctos.
- Aplicación 0.29.0-ciberdeck-dev código 36 y APK de pruebas instaladas en Galaxy A15 SM-A155F Android 16 mediante actualización, ambas `Success`. Versión comprobada; solicitud de arranque aceptada y sin `FATAL EXCEPTION` en el registro del proceso consultado. Datos, modelo local, firmware y permisos conservados.
- **Dos pruebas nativas Android correctas en 3,715 s**: rechazo de bytes que no son una imagen y lectura de un PNG QR a través de una URI local Android. El QR de ensayo contiene una configuración Wi-Fi ficticia; se comprueba la huella exacta de la imagen y que el informe omita su contraseña. Solo se crean y eliminan archivos temporales propios en la caché de la app, sin evidencias ni referencias en trabajos.
- La ronda combinada de interfaz arrancó con el A15 de nuevo bloqueado y se detuvo. Los fallos de jerarquía Compose bajo bloqueo **no se cuentan como validaciones de interfaz ni como defectos demostrados de la app**. La salida `Process crashed` de esa ronda coincide con el cierre deliberado del proceso para parar el runner. Registro privado `a15-029-qr-ui.txt`; ronda aislada nativa correcta `a15-029-qr-native.txt`.
- Con el A15 desbloqueado, **14 pruebas Android correctas en 44,3 s**: tres QR (lectura nativa, bytes inválidos y mostrar/ocultar/descartar), dos de herramientas de protección, dos de foco del agente, cuatro de tareas directas y tres de catálogo/navegación. Registro privado `a15-029-qr-ui-unlocked.txt`. Esto sustituye el estado pendiente de los controles QR bajo bloqueo; no convierte el ensayo anterior interrumpido en correcto.
- **Selector Android y guardado: una prueba adicional correcta en 6,085 s**. El test crea un PNG QR Wi-Fi ficticio propio en Descargas mediante MediaStore, abre el selector real, selecciona únicamente su nombre único, comprueba la lectura y su SHA-256, verifica que el contenido esté oculto, pulsa Guardar y vuelve a leer el informe persistido. Comprueba que ni SSID ni contraseña ficticios estén en el informe. Retira exclusivamente las referencias y la evidencia de ese ensayo y elimina únicamente su URI de imagen. Registro `a15-029-qr-picker-download.txt`.
- El primer intento del selector falló porque Android recordaba Descargas y el PNG propio estaba en Imágenes. Se acortó el nombre y se cambió el archivo de ensayo a Descargas; una ejecución intermedia aún utilizó el APK de pruebas anterior mientras terminaba su empaquetado. Esas rondas fallidas no cuentan como validación del selector. La ronda final usa el APK ya compilado, con resultado correcto. No fue necesario cambiar la aplicación para completar el selector.
- Pendiente: tarea QR completa del agente con selección real; su integración sigue compilada y cubierta por reglas unitarias, sin atribuirle la prueba del botón de Enlaces. OPPO no conectado y OTG directo no probado en este recorrido.
- Captura auténtica del lector QR sin contenido: `docs/screenshots/ciberdeck-qr-entry-029.png`, revisada visualmente. Las cinco capturas del rediseño conservan su versión **0.28**. Los QR de ensayo son fixtures, no observaciones del usuario, y no se publican imágenes con sus credenciales.


APK final: **65.199.582 bytes**, SHA-256 `1efc273186f84ae4e5112b1fa9cd7107d28c31a9ae69d67c61f994f9c1a36e40`. Copias y huellas locales en `dist/` actualizadas. Registros de instalación y arranque en `.private/android-validation/a15-029-install.json` y `a15-029-runtime-private.txt`. APK, modelo y registros privados fuera de Git; código en rama de desarrollo y PR borrador, sin release ni fusión en main.
