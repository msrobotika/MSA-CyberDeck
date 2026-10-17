# MSA CyberDeck — en desarrollo

Cuaderno Android local para recopilar evidencias de auditorías autorizadas.

## Implementado en el código local

- Selección de archivos mediante el selector de Android.
- Copia a almacenamiento privado y cálculo SHA-256 durante la lectura.
- Índice persistente con identificador, nombre, tamaño y fecha de importación.
- Verificación posterior de las copias guardadas.
- Exportación de un informe Markdown mediante el selector de destino.
- Conector USB-serie para WiFi Sentinel: permiso Android, identificación del firmware, estado y redes observadas.
- Radar RSSI animado y distribución de redes por canal a partir de la última lectura USB.
- Mascota original MSA Robotika con respiración, luces pulsantes e icono Android adaptativo.
- Firmware ESP32 complementario con cinco pantallas horizontales de 240 × 135 y controles BOOT.

La app arranca sin registros ni dispositivos simulados. Solo muestra radio tras una respuesta compatible del sensor. La pestaña Radio representa una lectura, no un flujo continuo; vuelve a leer desde USB para actualizar. El ángulo del radar es decorativo: no se estima dirección ni distancia.

## Pendiente

Pruebas de instalación/USB en Android, conector BLE, importación estructurada de resultados de otras herramientas, PDF y VPN. El sensor físico se ha probado desde el PC. La imagen antigua cyberdeck-dashboard.png es una ilustración del primer prototipo.

La vista previa web está en preview/index.html. La captura docs/screenshots/esp-radar-horizontal.png procede del framebuffer real del ESP. La mascota se guarda en app/src/main/res/drawable-nodpi/cyberdeck_robot.png; consulta docs/ROBOT-DESIGN.md.

## Sensor físico

Placa ideaspark ESP32-D0WD con CH340 y pantalla ST7789 con perfil de 135 × 240, utilizada en horizontal (240 × 135). Pines MOSI 23, SCLK 18, CS 15, DC 2, RST 4, luz 32 y BOOT 0. Firmware basado en MSA WiFi Sentinel, identidad MSA-WiFi-Sentinel-0.2.1-CyberDeck. Trabaja en recepción Wi-Fi pasiva; BLE aún no se implementa.

BOOT corto: siguiente vista. Doble pulsación en Redes/Huella: siguiente red. Mantener en Radar: pausa/reanuda. Mantener en Huella abre una confirmación; otra pulsación larga guarda la referencia elegida. No guarda contraseñas. La app no modifica NVS ni flashea.

La copia de seguridad de la placa y las capturas con SSID/BSSID están en .private/ y se excluyen de Git. No se publican identificadores observados.

Los hashes permiten comparar las copias locales; la fecha depende del reloj del teléfono. No se afirma una cadena de custodia certificada. Al desinstalar la app se borran sus copias privadas: exporta previamente lo que necesites conservar.

## Compilación y pruebas

JDK 17, Gradle 8.9 y Android SDK 35. Tareas :app:assembleDebug y :app:compileDebugUnitTestKotlin completadas. Nueve pruebas JUnit pasan: dos de hash y siete de protocolo. El lanzador de pruebas de Gradle tiene un fallo de sockets locales en este PC; se ejecutaron directamente con JUnitCore. Consulta docs/VALIDATION-SENSOR.md.

Las modificaciones de esta iteración se mantienen locales hasta completar la validación y revisión del propietario.

