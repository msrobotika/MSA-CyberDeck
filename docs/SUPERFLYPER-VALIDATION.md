# SuperFlyper · resultados de continuidad · 2026-10-18

## Fases, archivos y resultados

| Fase | Archivos principales | Implementación o resultado | Limitación y siguiente paso |
|---|---|---|---|
| 1 · entorno | Merge 7a46274, configuración Gradle y fuentes CyberDeck | Rama existente feature/superflyper-usb-v0-1 recuperada del PR #1. Se incorporan los doce commits locales de CyberDeck; main no se modifica | PR sigue en borrador; no fusionar sin revisión |
| 2 · hardware | Copias privadas, BACKUP.md, SHA256.txt, logs de identificación y eFuse | OPPO por ADB y CH340 COM4 comprobados; ESP32-D0WD rev1, flash 4 MB. Dos lecturas completas idénticas | Placa comercial no identificada por VID/PID; ninguna restauración ni escritura probada |
| 3 · aplicación | DeckHome, DeckDesign, DeckNavigationDrawer, UsbBridgePanel | Menú lateral por áreas plegables, módulos con estados explícitos, funciones CyberDeck conservadas | RF, IR, GPIO, electrónica, MQTT/Modbus/PLC e IA pendientes; registro de módulos interno, sin carga de plugins externos |
| 4 · USB | UsbBridge, UsbProtocol, probe_superflyper.py y contrato USB | Permiso inicial, detección, consultas heredadas/SF1/JSON, correlación JSON, límites de tramas, logs y reconexión limitada | Código y parser probados; transporte PC serie real comprobado. OTG Android, permiso físico y reconexión de cable sin validar |
| 5 · APK | build.gradle.kts, pruebas JUnit/Android, workflow | Versión 0.7.0-superflyper-dev código 10 compilada e instalada en OPPO CPH2743 Android16/API36 | No es una APK release firmada para distribución; ampliar pruebas de dispositivos y accesibilidad |
| 6 · integración | SUPERFLYPER_WORK_HANDOFF.md y adaptador heredado | Sentinel responde físicamente; BLE Scout comparte consulta status. Se revisan RogueWatch y ChipSafe sin modificarlos | RogueWatch local no usa status JSON y ChipSafe es utilidad host: no se anuncian como integraciones operativas |

## Compilación

Android: assembleDebug, assembleDebugAndroidTest, compileDebugUnitTestKotlin y lintDebug completados. Lint: cero errores, once avisos (incluye dependencias, compatibilidad e icono). Firmware genérico ESP32: PlatformIO espressif32 6.12.0 + ArduinoJson 7.4.3, compilación correcta; 21.488 bytes RAM y 282.029 bytes de programa. Se compila solamente: no se ha subido a la placa ni conserva su pantalla/Sentinel.

APK local: `dist/SuperFlyper-debug.apk`, excluida de Git.
SHA-256: `95f42e1f00f89d9d23874ef44d5fe1906c706d2411fe1a91c3908c0443c37c51`.

## Pruebas

- **61 pruebas JUnit pasan**, incluidas once de tramas USB, UTF-8 fragmentado, recuperación tras trama larga, correlación de respuesta, compatibilidad y rechazo de comandos de escritura. Las otras cincuenta cubren las funciones existentes. En este PC se usa JUnitCore por el problema previo del worker Gradle; Actions ejecuta la tarea Gradle ordinaria.
- **14 pruebas Android distintas completadas con resultado correcto en el OPPO**, a través de la batería y la repetición: menú plegable, panel USB sin lecturas ficticias, módulos, navegación, recreación NFC, recepción temporal, diagnósticos contra loopback, informes, historial y borrado de fixtures. La primera batería terminó con 13/14; el selector de texto de una prueba era ambiguo por la cabecera del menú cerrado. Tras corregirlo, las dos pruebas SuperFlyper pasan. No se cuenta una ejecución repetida como un caso distinto.
- La prueba del protocolo descubrió un reinicio de búfer que resolvía `clear()` sobre la lista de resultados; se corrigió a `this@UsbFrameDecoder.clear()` y los once casos USB pasan.
- Se consulta el firmware original mediante `status` antes y después del respaldo. El log privado conserva TX/RX reales; es una prueba **PC–ESP32**, no Android OTG.
- Dos lecturas completas de flash de 4.194.304 bytes, iguales byte por byte y con SHA-256 coincidente. Primera por ROM, segunda con stub temporal en RAM. No se escribe firmware ni eFuses.
- Datos y preferencias de CyberDeck respaldados antes de actualizar. Índice de evidencias idéntico antes y después de actualizar la APK y el APK de pruebas. No se desinstala ni se borran datos de usuario para instalar.

## Capturas y registros

`docs/screenshots/superflyper-menu.png` y `superflyper-usb.png` son capturas de la interfaz real del OPPO; se inspeccionaron visualmente antes de incorporarlas. El panel indica que no hay adaptador conectado al teléfono, sin trasladar ficticiamente COM4 al USB de Android.

Logs privados en `.private/android-validation/`: superflyper-menu-build.log, superflyper-junit-final.txt, superflyper-final-device-tests.txt, superflyper-menu-device-tests.txt, superflyper-firmware-build.log, superflyper-pc-serial.jsonl y superflyper-logcat.txt. Backups y datos de radio permanecen fuera de Git.

## Pendiente físico

Conectar ESP32 → OPPO por OTG y probar selección, permiso, identidad anunciada, consulta status, exportación de TX/RX, desconexión y reconexión. No se declara fiable o validada la reconexión sin esa prueba. Mantener firmware Sentinel para esta prueba: no hace falta flashear el candidato. La IA y los periféricos RF/IR/industriales necesitan iteraciones separadas con su hardware real.
