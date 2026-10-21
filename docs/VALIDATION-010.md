# CiberDeck 0.10 · candidata de desarrollo

## Cambios funcionales

Continúa el paquete `com.msarobotika.cyberdeck`, sobre la misma rama y sin eliminar datos de la instalación. Se conserva la identidad CiberDeck y el robot propio. El inicio ofrece **Auditar un equipo** y **Analizar mi móvil**; el menú desplegable agrupa las herramientas por áreas.

La auditoría guiada resuelve un destino, fija una IP y la red activa de Android, solicita confirmar el alcance y establece como máximo ocho conexiones TCP, secuenciales, con espera de un segundo por puerto y sin datos de aplicación. Los sockets se vinculan a esa red. Si cambia antes de iniciar exige resolver de nuevo; si cambia durante la ejecución, interrumpe los intentos restantes. Hay resumen gráfico de resultados, tiempos medidos, referencia temporal, comparación y exportación mediante el almacén de evidencias existente.

La comparación exige igual IP, conjunto de puertos, red registrada y pruebas no interrumpidas. Es una comparación de estados TCP: no identifica software ni vulnerabilidades. Una IP reutilizada por DHCP puede corresponder a otro equipo. No hay descubrimiento automático de subredes ni pruebas sobre equipos ajenos por defecto.

El asistente experimental integra Google LiteRT-LM en un proceso privado separado. Importación de un archivo elegido, SHA-256, copia privada, controles de carga/generación/cancelación y contexto seleccionado por el usuario. Sin pesos incluidos, descarga automática, nube ni acciones ejecutadas por la IA. **La inferencia y su calidad siguen pendientes**, de acuerdo con la decisión de preparar solamente la importación. Ver [IA local](LOCAL-AI.md).

## Comprobaciones locales

- Compilación de APK de desarrollo y APK de pruebas: correcta con Java 17, Gradle 8.13, AGP 8.10.1 y Kotlin 2.2.21.
- Lint: **0 errores, 20 advertencias y 1 sugerencia**. No es una auditoría de seguridad completa; quedan advertencias de dependencias, API y mantenimiento.
- Pruebas JVM: **99 correctas**, incluida política de puertos, interrupción por cambio de red, comparación, límites de modelos, protocolos y funciones anteriores.
- Galaxy A15 SM-A155F, Android 16/API 36, ARM64: actualización conservando datos. El OPPO no está conectado en esta revisión y no se da por actualizado.
- Pruebas instrumentadas en el A15: **13 correctas** en el lote final (IA, auditoría TCP, chequeo del teléfono, enlaces, llamadas, APK, navegación y núcleos nativos). No hay excepciones fatales de CiberDeck en el registro de fallos consultado después de las pruebas.
- Motor nativo de Google cargado realmente en `:local_ai`, servicio no exportado; ausencia de modelo tratada como estado controlado, sin texto generado ficticio.
- Conexión TCP auténtica a un servidor de prueba dentro del propio A15. El servidor comprueba cierre sin recibir datos de aplicación. La interfaz verifica IP elegida, consentimiento del alcance, resultado, referencia y conservación tras recrear la actividad.
- Importación inválida conserva el índice y el archivo original. La prueba transaccional usa un contenedor de cabecera en una carpeta aislada exclusivamente para almacenamiento: **no es un modelo de inferencia y nunca se carga en el motor**.

Las capturas PNG se obtienen de la interfaz ejecutada en el teléfono. La captura de auditoría representa la conexión local de prueba, no un incidente real. Las capturas de redes y postura del teléfono con identificadores permanecen privadas.

## ESP y recuperación

Placa conectada identificada como ESP32-D0WD rev. 1.0, flash de 4 MiB, puente CH340; no es ESP32-S3 ni dispone de USB nativo. La pantalla ST7789 horizontal de 240 × 135 conserva cinco vistas y manejo mediante BOOT. Firmware `MSA-WiFi-Sentinel-0.3.0-CiberDeck`, con radar basado en canal y RSSI auténticos.

Antes de esa actualización se conservó un respaldo completo de 4 194 304 bytes y SHA-256 `7ae03309c1238258a55b4f043f99b4addc44a7349e3c0c314563e43d988904e7`, coincidente con la lectura original anterior. Solo se escribió la aplicación desde `0x10000`; verificación posterior correcta y configuración Sentinel de NVS idéntica byte a byte. La prueba física capturó cinco framebuffers, observó tráfico y comprobó pausa/reanudación. Detalles en [validación 0.8](CIBERDECK-08.md).

## Límites pendientes

- Android → ESP por USB OTG directo requiere cambiar físicamente el cableado y probar permisos, serie y recuperación. PC → teléfono y PC → ESP por separado **no validan ese enlace**.
- Gemma: faltan modelo oficial aceptado/importado, inferencia, consumo, latencia y evaluación de respuestas. No se promete rendimiento ni equivalencia con Gemini Nano.
- Guardia de llamadas: falta selección del rol por el usuario y una llamada física de prueba. No graba, bloquea ni consulta reputación externa.
- GPIO, RF, infrarrojos y PLC dependen del hardware y de módulos concretos; no se presentan como operativos por aparecer en el catálogo.
- Esta APK con IA requiere ARM64 o x86_64. No hay variante de 32 bits preparada.
- Las pruebas automatizadas no sustituyen una revisión independiente ni garantizan protección total.

Los registros y respaldos están bajo `.private/`, excluido de Git. La APK local está en `dist/CiberDeck-debug.apk`, con SHA-256 en `dist/SHA256SUMS.txt`. Se conserva como candidata en la rama de desarrollo y PR #1; no se fusiona en `main` ni se crea una release.

APK verificada: versión `0.10.0-ciberdeck-dev`, código 14; SHA-256 `48d2a018b26fc9d5e75c03c196db844b006a4d2b51ee9ff96e0cf20573eff252`. Los nombres locales anteriores de la APK se mantienen como copias de la misma candidata para los accesos existentes; los documentos históricos conservan sus hashes de versiones anteriores.
