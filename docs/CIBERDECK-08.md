# CiberDeck 0.8 — implementación y límites

## Android y navegación

Identidad CiberDeck, mismo applicationId y firma de desarrollo. No se desinstala ni borra el almacenamiento de la aplicación al actualizar. Se conserva la transferencia entre móviles, receptor desde navegador, evidencias, NFC, BLE, diagnóstico DNS/TCP/TLS y consola USB. Menú por áreas, inicio con accesos directos, y separación entre consola USB y lectura Wi-Fi del ESP.

## Mediciones y visualización

Mi móvil consulta API públicas de Android: bloqueo configurado, parche declarado por el sistema, depuración USB, cifrado informado, red activa, transporte VPN, DNS privado, proxy, RAM, volumen de la app, temperatura de batería y sensores presentes. No identifica malware ni infiere vulnerabilidades a partir del nombre del fabricante. Revisar un parche de más de 180 días es un criterio local explícito, no una consulta de CVE ni la fecha de la última actualización del fabricante.

Las series RAM/batería se inician con una acción explícita y muestrean cada 2 s, máximo 60 observaciones, con fecha UTC y tiempo monotónico. Al salir de la pantalla o detener la actividad se detienen. Las curvas no unen huecos mayores de 5 s; se puede guardar la serie como evidencia. La escala RAM se adapta al rango observado: no representa porcentaje de peligro. No se generan puntos de demostración.

Radar Wi-Fi: ángulo = canal 1–13; radio = RSSI observado, con límites de escala −100 a −30 dBm. Ninguna coordenada representa ubicación física. El puntero del ESP refleja el canal sintonizado. El gráfico de canales cuenta redes, no mide ocupación de aire. Las barras BLE muestran RSSI de los registros recibidos, sin distancia estimada. La app representa la última lectura ESP; las gráficas BLE y la lectura Android→ESP directa requieren validación física adicional.

## Enlaces

Inspección offline de una URL HTTP(S), sin resolver DNS, conectar, seguir redirecciones o enviar a terceros. Se muestra el dominio ASCII real, presencia de credenciales/@, Unicode/punycode, IP, HTTP, puerto no predeterminado y coincidencia con una lista local pequeña de acortadores. Son señales de revisión, no una clasificación de phishing. El informe omite URL completa, ruta, parámetros, fragmento y credenciales para reducir exposición de tokens. La entrada se descarta al dejar su pantalla (salvo restauración de estado de la propia actividad).

## Llamadas: experimental y voluntario

Servicio oficial `CallScreeningService`, protegido por `BIND_SCREENING_SERVICE`. Android 10+ debe ofrecer y conceder el rol `ROLE_CALL_SCREENING`, que el usuario elige en el diálogo del sistema. Android puede reemplazar otra app que tenga ese rol. Se puede revocar en aplicaciones predeterminadas. No se cambia el marcador predeterminado ni se solicita READ_CALL_LOG, READ_CONTACTS, micrófono o accesibilidad.

Solo se conservan llamadas entrantes entregadas por Android desde la activación. La plataforma puede excluir contactos, números ocultos/restringidos y llamadas de apps. No se importan llamadas anteriores. En Android 11+ se presenta el resultado STIR/SHAKEN del operador: superado, fallido o no disponible. Superado no acredita que el llamante sea honesto; sin verificación no implica fraude. No identifica el titular del número ni analiza voz, mensajes o contenido de la conversación.

Cada llamada entrante se permite de inmediato, antes de leer/escribir almacenamiento; nunca se silencia, rechaza o elimina del historial. Aviso opcional de prioridad alta si falla la verificación o coincide exactamente con un número añadido por el usuario. No se intenta deducir prefijos nacionales; usa el formato recibido. El aviso oculta parte del número y requiere permiso de notificaciones. No usa pantalla completa ni superposiciones. Las restricciones de Android y la interfaz de llamada pueden afectar cuándo se muestra.

Registro máximo 100 eventos, lista máximo 200 números, almacenamiento privado y backup Android desactivado. Botones quitar de lista, borrar registro con confirmación y exportar solo por acción del usuario. El informe de llamadas contiene números: trátalo como información personal. No se sube el registro a GitHub. Prueba con llamada física y notificación durante llamada todavía pendiente; no se sustituyó la app de filtrado del propietario desde ADB.

Referencias oficiales: [filtrado de llamadas](https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls), [contrato y plazo del servicio](https://developer.android.com/reference/android/telecom/CallScreeningService), [USB Host](https://developer.android.com/develop/connectivity/usb/host), [KeyguardManager](https://developer.android.com/reference/android/app/KeyguardManager).

## ESP32 y recuperación

ESP32-D0WD rev 1.0, CH340, COM4, flash 4 MiB DIO/40 MHz, display ST7789 horizontal 240×135. Lectura completa original SHA-256 `7ae03309c1238258a55b4f043f99b4addc44a7349e3c0c314563e43d988904e7`, 4 194 304 bytes; respaldo privado conservado. Firmware posterior `MSA-WiFi-Sentinel-0.3.0-CiberDeck`.

Se escribió exclusivamente la aplicación en 0x10000, tras autorización para renovar su interfaz y comprobar el respaldo. Imagen verificada por esptool; bootloader y particiones sin escribir. Región sentinel_nvs 0x300000–0x305fff leída después y comparada byte por byte con el original: idéntica. Se eliminó el borrado NVS automático de recuperación: ante fallo de inicialización se detiene sin borrar.

Prueba física desde PC: respuesta de identidad, redes auténticas, contador de tramas, pausa/reanudación y cinco capturas del framebuffer 240×135 correctas. Captura final: 11 redes, 2086 tramas; son valores puntuales, no especificaciones de rendimiento. Configuración y estado de monitor restaurados tras la prueba. Capturas con SSID/BSSID o postura de seguridad del móvil permanecen privadas. Radar y avisos genéricos se incluyen tras revisión visual.

USB serie Android→ESP mediante OTG sigue pendiente de cableado físico y permiso del sistema. El ESP32 clásico no ofrece USB NCM nativo. RF externo, IR externo, GPIO y MQTT/Modbus/PLC continúan como módulos pendientes: no se anuncian como implementados.

## Validación de esta candidata

- Compilación debug y APK de instrumentación: correctas; actualización instalada sobre la app del OPPO CPH2743, Android 16/API 36, sin desinstalar ni borrar datos.
- 81 pruebas JVM correctas: protocolos existentes, evidencias, evaluación del móvil, geometría de radio, inspector de enlaces y política local de avisos de llamadas.
- 12 pruebas nativas distintas correctas en el OPPO, ejecutadas por lotes: experiencia/navegación, receptor, DNS/TCP/HEAD contra fixtures loopback, radio, consola USB sin OTG, chequeo real con recreación de actividad y muestreo RAM/batería, inspector offline y presencia/protección del servicio de llamadas. Los fixtures están identificados como pruebas; no se publican como incidentes reales.
- Se corrigió una prueba antigua que buscaba un botón aún no compuesto por la lista del nuevo inicio; al desplazar explícitamente hasta el botón, el flujo completo vuelve a pasar.
- El servicio de llamadas está declarado y protegido por el permiso del sistema. No se invocó una llamada ficticia para aparentar validación. Falta elegir el rol y hacer una llamada física desde un número autorizado no guardado en contactos; comprobar tanto avisos habilitados como denegados. No se garantiza que el operador proporcione STIR/SHAKEN.
- Lint sin errores; quedan advertencias de compatibilidad, dependencias, icono y el gestor TLS local con pinning ya existente. No se actualizaron todas las dependencias durante este cambio de funciones.
- Cinco pantallas del ESP revisadas a partir del framebuffer real; configuración NVS intacta después del cambio de interfaz.

Los resultados nativos y capturas detalladas se conservan en `.private/android-validation/` y `.private/ux-review/`, ignorados por Git. El respaldo de firmware está en `.private/backups/esp32-ciberdeck-ui-20261018/`, con metadatos, comandos y comprobación SHA-256. No incluye un compromiso de validación de radio externo, GPIO, IA, VPN o interceptación de tráfico, que siguen fuera de esta candidata.

## Ampliación 0.8.1: inspector de APK y Samsung A15

Se añade Inspector APK al área Protección personal, sin solicitar permisos de instalación ni inventario completo de aplicaciones. El usuario selecciona un APK con el selector Android, o inspecciona el APK de la propia CiberDeck. Límite 128 MiB, lectura por bloques, copia temporal privada eliminada al terminar, también ante errores. No se descomprime, instala, carga código ni sube el archivo.

Se calcula SHA-256 sobre todos los bytes; Android interpreta paquete, nombre declarado, versión, API mínima/objetivo, certificados firmantes actuales, permisos y componentes exportados del manifiesto. Se distingue un permiso declarado de uno concedido. Las banderas depurable, solo-pruebas y texto-claro son datos del manifiesto; no equivalen a malware, riesgo confirmado o tráfico observado. No se analiza XML de seguridad de red, reputación del editor, bytecode, comportamiento en ejecución ni APK divididos/APKS/XAPK. Las huellas deben compararse con una fuente independiente de confianza.

La inspección se conserva en memoria al girar el móvil, permite guardar un informe como evidencia y deja el original intacto. Permisos mostrados: máximo 500; componentes exportados: primeros 40 y recuento total. Los componentes pueden tener restricciones declaradas o controles dentro del código; exportado no implica vulnerable.

Samsung SM-A155F/A15, Android 16/API 36, detectado y actualizado sobre 0.5.2 conservando sus datos. Las pruebas de enlaces, servicio de llamadas y recursos reales pasaron en 0.8.0. El OPPO dejó de estar conectado por USB al cambiar al A15: no se asume que ambos sigan conectados.

Referencia: [PackageManager y metadatos de archivos APK](https://developer.android.com/reference/android/content/pm/PackageManager).

## Continuación 0.9

El asistente de Google local se documenta en [LOCAL-AI.md](LOCAL-AI.md). No sustituye pruebas o mediciones ni implica inferencia validada sin pesos. La versión 0.8.1 pasó 85 pruebas JVM y cuatro nativas en el A15, incluyendo inspección de su APK real, certificado, permiso del servicio de llamadas, rechazo de archivo inválido y eliminación de copia temporal. La continuación conserva esa base.
