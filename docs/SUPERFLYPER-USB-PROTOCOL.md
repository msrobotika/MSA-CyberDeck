# SuperFlyper USB · contrato de consultas

Android conserva el identificador de aplicación de CyberDeck. La sesión usa USB Host y `usb-serial-for-android`, 115200 8N1, sin DTR/RTS activos después de abrir. Conectar un adaptador requiere selección inicial y permiso Android. No se identifica un SoC mediante VID/PID: un CH340 identifica un conversor, no su microcontrolador.

## Negociación y compatibilidad

1. Tras abrir el puerto se envía `{"cmd":"status"}`. Es la consulta de solo lectura que ya ofrecen WiFi Sentinel y BLE Scout. Se reconocen únicamente familias MSA conocidas; la información recibida sigue siendo una declaración del firmware.
2. Si no hay respuesta compatible tras cinco segundos, se prueba `SF1 HELLO`. Esto conserva el firmware demostrador del PR #1.
3. El candidato nuevo anuncia `protocol: "json-v1"` y acepta comandos JSON con identificador. El código Android pasa a ese dialecto cuando se anuncia. Un puerto abierto o un timeout no se muestran como hardware identificado.

Ejemplo de estructura, **no una lectura física**:

```json
{"sf":1,"id":42,"cmd":"STATUS"}
```

La respuesta incluye `sf:1`, `ok:true`, el mismo `id` y el mismo `cmd`. Un `id` distinto no completa la transacción JSON. Solo se permite una petición pendiente y se registra su timeout. El descubrimiento heredado y SF1 no ofrecen correlación fuerte ni autenticación; no se presentan como equivalentes a JSON correlacionado.

Consultas permitidas: HELLO, STATUS, CAPABILITIES y LEGACY_STATUS (adaptación a `status`). No hay consola de comandos arbitrarios, borrado, flasheo, escrituras GPIO, APDU ni emisión RF. Una ampliación con efectos deberá introducir permisos, validación y confirmación propios.

## Tramas y recuperación

JSONL UTF-8, máximo 4096 bytes por línea en Android. Las líneas mayores se descartan completas hasta el salto de línea, evitando interpretar un sufijo como otra respuesta. La trama parcial se vacía al cambiar de sesión. Los callbacks de lectores anteriores se descartan mediante una generación de sesión.

Se conserva una ventana de 300 eventos TX/RX/EVENT/ERROR/TIMEOUT con UTC del teléfono; la pantalla enseña los últimos 25. Guardar registro copia esa ventana a Evidencias como JSONL con SHA-256; no es un historial ilimitado ni sellado de tiempo certificado. Los datos pueden incluir identificadores locales: revisar antes de compartir.

La reconexión solo se intenta para VID/PID previamente elegidos y, cuando está disponible, el serial USB. Se exige coincidencia única: varios adaptadores iguales requieren intervención. Sin serial USB, VID/PID no son una identidad física fuerte. Hasta seis intentos con espera creciente; Detener enlace cancela pendientes, y salir de la app cierra el puerto. Se vuelve a solicitar permiso si Android lo necesita. No se reproducen operaciones de escritura. La sesión se mantiene al cambiar entre pantallas de la app, no como servicio permanente.

## Separación de cableado

PC → OPPO con ADB y PC → ESP32 con COM son dos enlaces independientes. Para probar USB Host real: detener lectores serie del PC, desconectar el ESP32 del PC, conectarlo al OPPO con un adaptador OTG de datos, abrir Herramientas → USB · SuperFlyper y autorizar el dispositivo. ADB por el mismo puerto USB deja de estar disponible salvo otro transporte de depuración previamente autorizado. Esta preparación no demuestra que OTG funcione.

## Firmware candidato

`firmware/superflyper-bridge` acepta JSON correlacionado y SF1, consulta chip/revisión, flash y memoria real. No conserva la pantalla ni el monitor Wi-Fi de Sentinel: **no es un reemplazo listo para esta placa con pantalla**. Solo se compila como candidato genérico; no se sube automáticamente. El firmware actual puede usarse con la consulta heredada sin reemplazarlo.

Referencias: [biblioteca USB serie](https://github.com/mik3y/usb-serial-for-android), [Android USB Host](https://developer.android.com/develop/connectivity/usb/host), [ArduinoJson](https://arduinojson.org/v7/how-to/install-arduinojson/).
