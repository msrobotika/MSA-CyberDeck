# MSA CyberDeck — en desarrollo

Cuaderno Android local para recopilar evidencias de auditorías autorizadas.

## Implementado en el código local

- Selección de archivos mediante el selector de Android.
- Copia a almacenamiento privado y cálculo SHA-256 durante la lectura.
- Índice persistente con identificador, nombre, tamaño y fecha de importación.
- Verificación posterior de las copias guardadas.
- Exportación de un informe Markdown mediante el selector de destino.
- Enumeración USB con VID, PID, clase e interfaces.

La app arranca sin registros ni dispositivos simulados. La enumeración USB no establece una conexión de telemetría con los ESP32.

## Pendiente

Pruebas en Android, conectores serie/BLE, importación estructurada de resultados de otras herramientas, PDF y VPN. La imagen antigua de docs/screenshots es una ilustración del prototipo y no representa una ejecución verificada de esta versión.

Los hashes permiten comparar las copias locales; la fecha depende del reloj del teléfono. No se afirma una cadena de custodia certificada. Al desinstalar la app se borran sus copias privadas: exporta previamente lo que necesites conservar.

## Compilación y pruebas

JDK 17, Gradle 8.9 y Android SDK 35. Ejecutar las tareas :app:assembleDebug y :app:testDebugUnitTest. La prueba del hash cubre el vector SHA-256 de abc, copia exacta, varios bloques y detección de cambios.

Las modificaciones de esta iteración se mantienen locales hasta completar la validación y revisión del propietario.
