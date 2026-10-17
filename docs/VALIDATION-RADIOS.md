# Validación local · 0.4.0-dev · 17 octubre 2026

Sin publicación remota. Equipos autorizados: OPPO CPH2743 y Samsung Galaxy A15 SM-A155F, Android API 36. Los identificadores y registros completos permanecen en `.private/`, fuera de Git.

- 31 pruebas JVM: hash, protocolo del sensor, diagnósticos y protocolo de sesiones. Incluyen invitaciones inválidas, caducadas o públicas, límites de tamaño, nombres y hashes alterados.
- Cuatro pruebas instrumentadas de sesiones: transferencia TLS real de prueba, rechazo de certificado no coincidente, rechazo del receptor sin guardado y dos guardados concurrentes sin pérdida del índice.
- Una prueba de interfaz confirma acciones explícitas para Bluetooth, NFC y Sesiones sin búsquedas automáticas. Capturas PNG reales de estas vistas en reposo, sin credenciales de invitación.
- Dos teléfonos físicos: OPPO receptor y A15 emisor, transferencia de archivo generado de 1.048.576 bytes por sockets TLS directos sobre Wi-Fi. Ambos extremos confirman el mismo SHA-256: `631b84027d6b9e52b539c4e8373622d23032dfadc64d60af87339c9037e4f769`. ADB controla la prueba e intercambia la invitación; no transporta el archivo ni crea túneles.
- NFC: lectura física del llavero de cafetera confirmada en el A15: Android anuncia MifareClassic, NfcA y NdefFormatable, sin NDEF disponible. El informe de 1.156 bytes se guardó mediante el botón de la app y se verificaron tamaño y SHA-256 desde su almacenamiento privado. No se leyó memoria de sectores ni se implementó emulación. La tarjeta de fichaje sigue pendiente de una lectura identificada por separado.

La clave TLS inicial no permitía firmar el resumen ya calculado por Conscrypt. Se corrigió con una clave AndroidKeyStore dedicada que autoriza DIGEST_NONE y un KeyManager que selecciona esa clave. No se elimina la validación del pin ni se cambia la confianza HTTPS global. Las pruebas positivas y negativas de TLS pasan tras la corrección.

## Repetición

Compila app y androidTest e instala ambas APK en los dos equipos. `tools/test_two_phones.py --receiver SERIAL --sender SERIAL` realiza la transferencia de prueba. `tools/test_ble_pair.py --peripheral SERIAL --scanner SERIAL` crea un periférico BLE temporal con UUID aleatorio y consulta solo ese dispositivo; su ejecución física sigue pendiente. La publicidad BLE pertenece exclusivamente a la variante debug de pruebas; no es una función automática de la app y el helper revoca ese permiso al terminar.

No se considera probado el enlace USB directo Android–ESP, una etiqueta NDEF con contenido, todas las variantes BLE ni redes con aislamiento entre clientes. Las pruebas no equivalen a una auditoría independiente de seguridad.

## Revisión NFC posterior

Instalada en ambos teléfonos la revisión que conserva los metadatos si falla NDEF, diferencia ausencia de NDEF y registros vacíos, y renombra el botón como Guardar informe NFC. Compilación, lint y 31 pruebas JVM completadas; prueba de interfaz repetida en A15: 1/1 correcta. La lectura física de una etiqueta con registros NDEF sigue pendiente. Tras inspeccionar el llavero se aclaró que terminaba la sesión de lectura y no la app; la interfaz muestra ahora el final de lectura y las tecnologías antes de los detalles.

La prueba BLE entre teléfonos se intentó, pero OPPO rechazó la concesión de permisos desde ADB antes de iniciar el escaneo. No se desactivaron protecciones del sistema y no se considera una prueba BLE aprobada. El permiso temporal ADVERTISE concedido al A15 se revocó.

APK debug local: SHA-256 `0416d14bc14c0639953f7c0b3195406ee62ad74734341fce0440741bd7b3e6a2`.

Capturas reales en reposo: [Bluetooth](screenshots/android-bluetooth-idle.png), [NFC](screenshots/android-nfc-idle.png), [Sesiones](screenshots/android-peer-idle.png). No muestran la lectura de una tarjeta ni una transferencia en curso.
