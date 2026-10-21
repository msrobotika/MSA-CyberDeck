# SUPERFLYPER — MS ROBOTIKA · continuidad de Work

Fecha: 2026-10-18. Rama: `feature/superflyper-usb-v0-1`. PR: [#1](https://github.com/msrobotika/MSA-CyberDeck/pull/1). No se fusiona ni se escribe en main.

## Base recuperada

El PR anterior partía de main 8fb346f y no contenía los doce commits de CyberDeck conservados localmente, hasta cc283e6. Se integraron mediante el merge 7a46274 en la rama SuperFlyper. Se conservan NFC, BLE, diagnósticos DNS/TCP/web, mDNS, evidencias, informes, transferencia local HTTP y TLS, radar, diseño Robotika, firmware Sentinel y pruebas. La identidad de paquete sigue siendo com.msarobotika.cyberdeck, para actualizar sin desinstalar.

## Hardware comprobado

- Principal: OPPO CPH2743, Android 16/API 36. ADB estaba inicialmente vacío y después lo enumeró. APK anterior: 0.6.0-dev, código 9. Los archivos y preferencias se respaldaron antes de actualizar. El índice de evidencias fue idéntico antes y después de instalar la actualización y el APK de tests.
- COM4: adaptador CH340 1A86:7523. Esptool identificó ESP32-D0WD revisión 1.0, cristal de 40 MHz, flash de 4 MB. No es ESP32-S3 ni USB nativo. El modelo comercial exacto de PCB no se infiere del adaptador.
- Firmware instalado: MSA-WiFi-Sentinel-0.2.1-CyberDeck. Consulta status real antes y después del respaldo. No se ha flasheado ni borrado.
- Dos lecturas completas de 4.194.304 bytes coinciden byte por byte y en SHA-256. Copias, hash, comandos y eFuses están en `.private/backups/esp32-20261018/`. No publicar ese contenido.
- Flash sin cifrado y Secure Boot desactivado según eFuses leídos. get-security-info no está soportado por el modo de este chip; se utilizó espefuse summary. Cabecera del bootloader: DIO/40 MHz/4 MB.

## Implementación

Versión candidata: 0.7.0-superflyper-dev, código 10. Conserva el tema y las funciones de CyberDeck, con portada SuperFlyper y menú lateral por áreas desplegables. La barra de accesos principales permanece.

UsbBridge mantiene una sesión serie con detección de altas/bajas, permiso Android, consulta de firmware, protocolos heredado/SF1/JSON, errores, timeout y reconexión limitada al adaptador autorizado. Un único comando pendiente, registros de 300 eventos y exportación JSONL con SHA-256. Las operaciones se limitan a consultas; no hay consola libre ni flasheo.

El registro de módulos distingue implementado, experimental y pendiente. No es un cargador de código externo. RF sub-GHz, IR, GPIO, electrónica, MQTT/Modbus/PLC e IA siguen pendientes de hardware, contrato e implementación propios. No hay proveedor IA configurado ni envío de evidencias a terceros.

Consultar [contrato USB](SUPERFLYPER-USB-PROTOCOL.md). El firmware candidato genérico compila pero no conserva pantalla ni Wi-Fi Sentinel; no instalarlo como reemplazo de la placa actual. El firmware original se usa mediante su consulta heredada.

## Pruebas y límites

El protocolo de la placa se ha comprobado por serie desde el PC. OPPO y ESP32 están conectados por separado al PC: eso NO valida USB OTG directo. Reconexión, permiso y tramas físicas Android–ESP32 necesitan el cableado OTG real. No sustituir esos resultados por pruebas de parser o capturas de interfaz.

La compilación Android, JUnit, instrumentación y revisión de logcat de la versión final se registran en SUPERFLYPER-VALIDATION.md. Los logs y capturas del teléfono van en .private/android-validation y .private/ux-review.

## Proyectos revisados sin modificarlos

| Proyecto | Reutilización o límite comprobado |
|---|---|
| MSA-CyberDeck | Funciones y diseño completos conservados e integrados en la rama |
| MSA-WiFi-Sentinel | Consulta JSON status y lector de redes existente; placa real responde |
| MSA-BLE-Scout | Código fuente ofrece status JSON; adaptador heredado reconocido, sin prueba de esa placa en esta sesión |
| MSA-RogueWatch | El firmware local usa consola de caracteres, no status JSON; no se anuncia como adaptador compatible ni detector avanzado ya integrado |
| MSA-ChipSafe | Utilidad host de identificación/lectura; no se incorpora restauración ni escrituras destructivas |

Las licencias locales revisadas de esos cuatro proyectos auxiliares son MIT. No se copian ni modifican sus fuentes en esta iteración; se reutilizan contratos y módulos ya presentes en CyberDeck.

## Siguiente prueba necesaria

Detener el lector COM del PC, conectar el ESP32 al OPPO por OTG de datos, abrir Menú → Hardware y módulos → USB · SuperFlyper, seleccionar el CH340 y permitir acceso. Comprobar firmware anunciado, tramas TX/RX, Guardar registro y recuperación al retirar y reponer el cable. No flashear para hacer esta prueba con Sentinel. La depuración ADB por cable al PC deja de estar disponible al ocupar el mismo puerto USB-C; planificar una depuración alternativa autorizada o probar en pantalla.
