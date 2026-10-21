# Primer uso de CiberDeck 0.37

## Descargar e instalar

[Descargar la APK pública](https://github.com/msrobotika/MSA-CyberDeck/releases/download/v0.37.0-dev/CiberDeck-0.37.0-dev.apk). Android 8 o posterior con sistema de 64 bits. Es una versión de desarrollo; los pesos de IA y el ESP32 no son necesarios para las primeras comprobaciones.

Abre la APK con Android y revisa los avisos de instalación. No desactives Play Protect. Si ya tienes una versión compatible, elige actualizar. Ante un conflicto de firma, exporta primero tus evidencias y revisa la procedencia de la APK: desinstalar borra las copias privadas.

## Tu primera comprobación

1. Abre **Inicio → Abrir agente**.
2. Escribe **«Analiza mi red wifi»** y pulsa **Enviar petición**.
3. Consulta **Lo observado**: muestra el estado que Android conoce de la conexión y el transporte usado. Si indica datos móviles, no estás observando una conexión Wi-Fi activa.
4. Abre los detalles para consultar interfaz, direcciones locales y DNS. Esta tarea no busca intrusos, no mide velocidad ni certifica que la red sea segura.
5. Pulsa **Guardar resultado** para conservar una copia verificable, o **Repetir tarea** para obtener una lectura nueva.

También puedes pulsar **Revisar mi móvil** para consultar ajustes, conexión, recursos y APK propia sin cargar un modelo ni conectar el ESP.

## Pedir por voz o continuar

El botón de dictado abre el proveedor de reconocimiento de Android. La transcripción vuelve al campo de texto: revísala y pulsa Enviar. La disponibilidad y el funcionamiento sin conexión dependen de ese proveedor.

Después del resultado, **Continúa esta tarea** acepta «repítelo», «guárdalo», «muéstrame los detalles» o una nueva petición reconocida como «analiza mi red wifi». El guardado se confirma después de verificar la copia. Reutilizar una petición del historial solo rellena el borrador.

Las tareas directas utilizan reglas explícitas; no comprenden cualquier instrucción. Si no se reconoce una frase y no hay IA cargada, aparece un aviso. Para tareas con red, archivos, permisos o hardware, completa los pasos que la app solicite; abrir una pantalla no equivale a terminar una operación.

## Encontrar herramientas

Usa **Kit**, sus áreas y búsqueda, o el menú lateral. Las herramientas se agrupan por móvil, red, proximidad, hardware, archivos y agente. **Evidencias** reúne las copias guardadas; **Informe** permite exportar el resultado mediante el selector Android.

| Quiero… | Dónde y cómo | Qué obtengo |
| --- | --- | --- |
| Revisar mi teléfono | Agente → Revisar mi móvil | Observaciones de Android con fecha y fuente |
| Comprobar un servicio propio | Red → dominio/IP y puerto autorizado | DNS y resultado TCP, no un diagnóstico de vulnerabilidad |
| Consultar una web autorizada | Web → URL completa y revisión del alcance | Certificado y cabeceras; no se sigue la redirección automáticamente |
| Ver servicios locales | Mi red → búsqueda manual | Anuncios mDNS/DNS-SD, no todos los clientes del router |
| Observar BLE | Bluetooth → permisos → búsqueda | Anuncios y RSSI; no distancia física ni identidad verificada |
| Leer una etiqueta | NFC → Leer etiqueta NFC → acercarla durante 30 s | Tecnologías y NDEF disponible; no una credencial clonada |
| Inspeccionar un QR | Enlaces y QR → elegir imagen | Contenido clasificado sin ejecutarlo ni conectarse automáticamente |
| Medir campo magnético | Campo magnético → iniciar y llevar el móvil al lugar | Muestras del sensor en µT; no RF ni una evaluación sanitaria |
| Revisar cambios | Guardar dos lecturas compatibles → comparar | Cambios de campos con sus fuentes y huellas |

## Evidencias, archivos e informes

Guardar crea una copia privada; verificar recalcula tamaño y SHA-256. Puedes abrir/exportar una copia y borrar únicamente lo que ya no necesites mediante la confirmación correspondiente. Un hash no prueba la veracidad de una afirmación ni la identidad de un remitente.

Los informes pueden contener IP, identificadores o nombres sensibles: revísalos antes de compartir. Exporta los archivos importantes antes de desinstalar la app. [Gestión de archivos y limpieza](FILES-AND-CLEANUP.md).

## Otro móvil

**Con CiberDeck en ambos:** abre Sesiones, prepara la recepción y comparte la invitación temporal con el remitente. El remitente selecciona el archivo y el receptor lo aprueba; se usa TLS local y se comprueba SHA-256.

**Sin instalarla en el remitente:** el receptor puede preparar una recepción desde navegador y compartir su enlace temporal/QR. Ambos deben estar en una red local de confianza que permita comunicación entre clientes. Este modo usa HTTP sin cifrar, tiene límites de tiempo/tamaño y exige aprobación; no lo uses para contenido confidencial.

La app recibe archivos que el propietario selecciona: no permite explorar arbitrariamente todo el otro móvil. [Pasos y límites](RADIOS-AND-PEER.md).

## IA opcional

Abre **Asistente IA**, importa el [modelo compatible documentado](OPEN-LOCAL-MODEL.md), cárgalo y elige las fuentes. No viene incluido en la APK. El analista y el planificador son experimentales; revisa sus respuestas contra las observaciones. No se garantiza que cualquier petición se convierta en un plan correcto.

## ESP32 opcional

Conecta una placa con firmware compatible directamente al móvil mediante OTG de datos. Abre USB, actualiza la lista, elige el dispositivo y concede el permiso. La app consulta firmware/lecturas; Radio representa los datos recibidos. El radar usa canal y RSSI, no dirección ni distancia física.

Tener móvil y ESP conectados por separado al PC no crea ese enlace. **La validación física del enlace directo Android–ESP sigue pendiente**. No flashees una placa desconocida para probar esta APK; conserva primero identidad, firmware y recuperación. [Contrato USB](SUPERFLYPER-USB-PROTOCOL.md).

## Si algo no responde

| Situación | Qué revisar |
| --- | --- |
| No ejecuta una petición | Confirma que estás en Agente, revisa el texto transcrito y pulsa Enviar. Prueba la frase exacta «Analiza mi red wifi»; la planificación libre necesita modelo cargado |
| No hay redes en Radio | Necesita una respuesta auténtica del ESP compatible; no es un escáner Wi-Fi autónomo del móvil |
| NFC detecta pero no muestra contenido | Puede no haber NDEF accesible. Detectar una tarjeta no permite copiarla ni usarla para fichar |
| No abre el enlace de recepción | Prepara otro enlace si caducó; revisa misma Wi-Fi, acceso entre clientes y esquema HTTP. La dirección no se abre por Internet |
| No aparece un archivo importado | Busca la copia en Evidencias; no se incorpora automáticamente a Música/Galería. Puedes abrirla o exportarla |
| La IA no carga o tarda | Verifica el formato/modelo compatible y memoria disponible. Descarga el motor de memoria al terminar |
| La APK no instala | Revisa Android de 64 bits, almacenamiento y posible conflicto de firma; no borres datos para resolverlo sin exportarlos |

## Qué se ha probado

0.37: 266 pruebas de lógica y 18 casos Android correctos en Samsung A15. En OPPO CPH2743 se comprobó actualización, versión y arranque. La transcripción de dictado se ensayó con un resultado controlado; no se presenta como prueba humana de micrófono. [Informe completo](VALIDATION-037.md).
