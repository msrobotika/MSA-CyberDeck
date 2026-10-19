# CiberDeck — MS ROBOTIKA

Aplicación Android local para comprobar, observar y documentar dispositivos propios o autorizados, con ESP32 opcional. Conserva el proyecto CyberDeck, paquete y funciones anteriores. El nombre visible vuelve a **CiberDeck**; la rama `feature/superflyper-usb-v0-1` conserva su nombre por continuidad. [PR de desarrollo #1](https://github.com/msrobotika/MSA-CyberDeck/pull/1), sin fusionar en main ni publicar una release.

## Desarrollo 0.11 · Trabajos de auditoría y herramientas de campo

APK candidata para Android 8 o posterior **de 64 bits** (ARM64/x86_64). Instalada y comprobada en Galaxy A15; esta revisión no se ha instalado en el OPPO desconectado.

- **Trabajos de auditoría**: separa cada revisión por nombre, alcance, objetivos y notas. Asocia los diagnósticos guardados al trabajo activo, incorpora evidencias existentes y exporta un informe con comprobación SHA-256. Archivar no borra archivos. [Uso y validación](docs/VALIDATION-011.md).
- **Auditar un equipo**: flujo guiado desde el inicio. Resuelve un destino, confirma una IP y hasta ocho puertos, acepta el alcance autorizado y comprueba conexiones TCP sin enviar datos de aplicación. Gráfico de resultados, tiempos medidos, referencia temporal, comparación e informe verificable. La red queda fijada y un cambio interrumpe la prueba. Un puerto accesible no acredita una vulnerabilidad ni una intrusión.
- **Asistente IA local**: motor Google LiteRT-LM con Qwen3 0.6B mixed INT4, descargable sin registro. Importación privada, SHA-256, contexto elegido, parada y liberación de memoria. Generación comprobada físicamente en A15; calidad experimental y conclusiones sujetas a revisión. Los pesos no están incluidos en la APK ni Git. [Modelo exacto y pruebas](docs/OPEN-LOCAL-MODEL.md).
- **Campo magnético**: sensor Android real en µT, ejes X/Y/Z, gráfica, referencia temporal e informe. Requiere magnetómetro; no mide radiofrecuencia ni detecta ataques. El ESP32 necesita un sensor externo para medir por sí mismo.
- **Plan de red**: calculadora IPv4/CIDR, máscara, red, rango y direcciones utilizables; puede leer la IPv4 activa. Cálculo local sin escanear ni enviar tráfico.
- Inicio con cuatro accesos directos y menú lateral por áreas desplegables.
- **Mi móvil**: bloqueo, fecha del parche, depuración USB, conexión/VPN/DNS privado, RAM, almacenamiento, batería y hardware según las API públicas de Android.
- **Gráficas reales**: RAM y batería cada dos segundos al iniciarlas; máximo 60 muestras, pausa al salir. Sin muestras ficticias ni carga artificial de CPU.
- **Radar Wi-Fi**: canal determina el ángulo, RSSI el radio. No indica dirección física ni distancia. Usa la última lectura auténtica del ESP32; no es un flujo continuo.
- **BLE**: barras de RSSI de anuncios recibidos, sin inventar distancia ni seguimiento de personas.
- **Inspector APK**: hash SHA-256, certificados, permisos y componentes del manifiesto de un APK elegido, sin instalarlo ni ejecutarlo. Límite 128 MiB, sin reputación ni análisis de malware.
- **Inspector de enlaces**: inspección sintáctica sin conexión; HTTP, destino tras @, Unicode/punycode, IP, puerto y lista limitada de acortadores. Sin visitar la URL ni declarar que es segura.
- **Guardia de llamadas**, experimental: API oficial Android de filtrado con elección expresa de la app por el usuario. Registro privado limitado a 100 eventos entrantes, lista personal de revisión, avisos por coincidencia exacta o verificación de operador fallida. No consulta reputación externa, no graba ni bloquea llamadas. Sin llamada física de validación todavía.
- ESP32 con cinco pantallas horizontales y la misma paleta/identidad: Radar, Redes, Ficha, Avisos y USB. BOOT cambia de vista; no es táctil.

[Validación de esta candidata](docs/VALIDATION-011.md) · [Pruebas, límites y privacidad 0.8](docs/CIBERDECK-08.md) · [Contrato USB](docs/SUPERFLYPER-USB-PROTOCOL.md) · [Continuidad anterior](docs/SUPERFLYPER_WORK_HANDOFF.md).

PC→ESP y PC→OPPO funcionan por separado. Esto **no valida** la conexión directa Android→ESP mediante OTG, que sigue pendiente. El firmware real conserva el protocolo WiFi Sentinel; no se ha sustituido por el firmware genérico candidato.

<img src="docs/screenshots/ciberdeck-home.png" width="260" alt="Inicio real de CiberDeck en Galaxy A15"> <img src="docs/screenshots/ciberdeck-equipo.png" width="260" alt="Auditoría TCP real contra un servicio de prueba en el propio A15"> <img src="docs/screenshots/ciberdeck-local-ai.png" width="260" alt="Asistente local con Qwen3 importado en el A15">

La captura de auditoría corresponde a una conexión real a un servicio local de prueba; no representa un incidente. Las pantallas proceden de la app ejecutada en el A15. El radar siguiente procede del framebuffer de la placa real.

Capturas actualizadas del A15: trabajo persistente con diagnósticos reales y gráfica de muestras del magnetómetro. La captura de IA muestra el modelo importado y descargado de memoria; la generación local se comprobó por separado.

<img src="docs/screenshots/ciberdeck-work.png" width="260" alt="Trabajo real de revisión local del A15"> <img src="docs/screenshots/ciberdeck-magnetic-graph.png" width="260" alt="Gráfica de muestras reales del magnetómetro del A15">

<img src="docs/screenshots/ciberdeck-esp-radar.png" width="480" alt="Framebuffer real del radar del ESP32, 240 por 135">

El rediseño ESP 0.3.1 conserva el radar inicial y cambia las otras cuatro vistas. **Compilado, aún sin instalar porque la placa no está detectada.** Vista previa del mismo código C con un snapshot anterior anonimizado, no una captura física nueva:

<img src="docs/screenshots/esp-ui-031-preview.png" width="700" alt="Vista previa offline claramente marcada del firmware ESP 0.3.1, pendiente de instalación">

## Base CyberDeck conservada

Kit Android local para observar, comprobar y documentar auditorías autorizadas. Las funciones de la base 0.6 se mantienen en la candidata actual.

## Experiencia 0.6

Cuatro destinos permanentes: Inicio, Herramientas, Evidencias e Informe. El catálogo agrupa nueve herramientas con búsqueda. La portada Robotika muestra actividad real, acciones rápidas y efectos pausables. Las tarjetas de archivos priorizan abrir, exportar y gestionar; los detalles técnicos se despliegan cuando hacen falta.

El informe admite un borrador privado con nombre, alcance y notas del trabajo. Comprueba las copias antes de incluir diagnósticos y marca las alteradas como no verificadas. El borrador no es un gestor de casos ni una certificación de integridad.

La recepción HTTP local puede continuar al cambiar de aplicación durante su sesión limitada de dos minutos. Se aprueba cada archivo desde la app; detenerla, cerrar desde recientes o agotar el plazo cierra el receptor. Las notificaciones son opcionales. Consulta [archivos y límites](docs/FILES-AND-CLEANUP.md), [revisión de producto](docs/PRODUCT-AND-UX-REVIEW.md) y [validación de la experiencia](docs/VALIDATION-UX.md).

## Implementado en el código local

- Bluetooth: búsqueda BLE manual, RSSI y descubrimiento de servicios GATT autorizados, sin leer ni escribir características.
- NFC: identificador, tecnologías y contenido NDEF disponible; guardado de un informe, sin emulación de credenciales.
- Sesiones: transferencia directa entre móviles por TLS local, invitación temporal, aprobación y SHA-256.
- Mi red: descubrimiento manual de servicios DNS-SD/mDNS, resolución de anuncios y guardado como evidencia.
- Distribución gráfica de servicios web, impresión, multimedia y estaciones de trabajo observados.
- Portada con acciones directas y navegación fija al desplazarse.
- Estado real de conexión de Android: transporte, interfaz, direcciones y DNS configurados, sin escaneo ni ubicación.
- Diagnóstico DNS/IP y conexión TCP a un puerto elegido, sin envío de cargas.
- Revisión HTTP(S) mediante HEAD: certificado TLS, cabeceras y redirección sin seguirla automáticamente.
- Guardado de diagnósticos JSON como evidencia con SHA-256 e inclusión en el informe.
- Archivo de evidencias con búsqueda por nombre, fecha o hash, filtros y orden de guardado.
- Reapertura de informes guardados con verificación SHA-256 y comparación de dos observaciones NFC.
- Borrado confirmado de copias privadas, individual o de los resultados del filtro, con tamaño ocupado.
- Apertura de copias verificadas con un visor del móvil y exportación mediante el selector de destino; recepción visible en Evidencias al terminar.
- Recepción temporal desde navegador sin instalar CyberDeck en el remitente: HTTP local opcional, selección de un archivo y aprobación del receptor. Consulta [uso y límites](docs/FILES-AND-CLEANUP.md).
- Selección de archivos mediante el selector de Android.
- Copia a almacenamiento privado y cálculo SHA-256 durante la lectura.
- Índice persistente con identificador, nombre, tamaño y fecha de importación.
- Verificación posterior de las copias guardadas.
- Exportación de un informe Markdown mediante el selector de destino.
- Conector USB-serie para WiFi Sentinel: permiso Android, identificación del firmware, estado y redes observadas.
- Radar RSSI por canal y distribución de redes por canal a partir de la última lectura USB.
- Mascota original MSA Robotika con respiración, luces pulsantes e icono Android adaptativo.
- Firmware ESP32 complementario con cinco pantallas horizontales de 240 × 135 y controles BOOT.

La app arranca sin registros ni dispositivos simulados. Solo muestra radio tras una respuesta compatible del sensor. La pestaña Radio representa una lectura, no un flujo continuo; vuelve a leer desde USB para actualizar. El ángulo representa el canal observado: no se estima dirección ni distancia.

Mi red utiliza anuncios de servicios, no una lista completa de clientes del router. Las pruebas físicas y límites se documentan en [VALIDATION-LAN.md](docs/VALIDATION-LAN.md).

## Pendiente

Ampliar cobertura a otros móviles y validar el enlace USB móvil–ESP mediante OTG, validación física de etiquetas NDEF y más variantes BLE, importación estructurada de resultados de otras herramientas, PDF y VPN. El sensor físico se ha probado desde el PC. La imagen antigua cyberdeck-dashboard.png es una ilustración del primer prototipo.

La vista previa web está en preview/index.html. La captura docs/screenshots/esp-radar-horizontal.png procede del framebuffer real del ESP. La mascota se guarda en app/src/main/res/drawable-nodpi/cyberdeck_robot.png; consulta docs/ROBOT-DESIGN.md.

## Sensor físico

Placa ideaspark ESP32-D0WD con CH340 y pantalla ST7789 con perfil de 135 × 240, utilizada en horizontal (240 × 135). Pines MOSI 23, SCLK 18, CS 15, DC 2, RST 4, luz 32 y BOOT 0. Firmware basado en MSA WiFi Sentinel, identidad MSA-WiFi-Sentinel-0.3.0-CiberDeck. Trabaja en recepción Wi-Fi pasiva; BLE aún no se implementa.

BOOT corto: siguiente vista. Doble pulsación en Redes/Huella: siguiente red. Mantener en Radar: pausa/reanuda. Mantener en Huella abre una confirmación; otra pulsación larga guarda la referencia elegida. No guarda contraseñas. La app no modifica NVS ni flashea.

La copia de seguridad de la placa y las capturas con SSID/BSSID están en .private/ y se excluyen de Git. No se publican identificadores observados.

Los hashes permiten comparar las copias locales; la fecha depende del reloj del teléfono. No se afirma una cadena de custodia certificada. Al desinstalar la app se borran sus copias privadas: exporta previamente lo que necesites conservar.

## Compilación y pruebas

JDK 17, Gradle 8.9 y Android SDK 35. Las pruebas de lógica de 0.6 suman cincuenta casos: hash, sensor, diagnósticos, transferencia, archivo/NFC, recepción HTTP, QR y contexto del informe. El lanzador de pruebas de Gradle tiene un fallo de sockets locales en este PC; se ejecutan directamente con JUnitCore tras compilar los tests. La validación Android y sus límites se conserva en los documentos `docs/VALIDATION-*.md`; las pruebas del sensor están en [VALIDATION-SENSOR.md](docs/VALIDATION-SENSOR.md).

Las modificaciones de esta iteración se mantienen locales hasta completar la validación y revisión del propietario.


## Radios y transferencia local · 0.4.0-dev

Consulta [RADIOS-AND-PEER.md](docs/RADIOS-AND-PEER.md) para uso y límites, y [VALIDATION-RADIOS.md](docs/VALIDATION-RADIOS.md) para las pruebas. Esta versión permanece local.

La versión local 0.5.0-dev añade recepción desde navegador y limpieza de evidencias. Consulta [validación de archivos](docs/VALIDATION-FILES.md).
