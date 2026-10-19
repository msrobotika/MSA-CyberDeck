# CiberDeck 0.20 · El móvil primero

## Cambio funcional

El agente exigía cargar un modelo y preparar un plan incluso para una revisión básica. Ahora ofrece tareas directas del propio Android: revisión de cinco pasos, conexión, anuncios BLE, lectura NFC e inspección de una APK elegida. No necesitan ESP32 ni modelo. La revisión principal aparece antes del cuadro de texto; se elimina la tarjeta grande redundante del agente y los ajustes de IA quedan después de las tareas.

`AgentTasks` define siete tareas con operaciones tipadas y una gramática limitada para frases comunes. Las peticiones reconocidas se resuelven localmente; las desconocidas ofrecen alternativas o planificación con IA cuando está cargada. No se afirma comprensión general. Los enlaces y servicios de red esperan sus entradas y alcance antes de ejecutar. Abrir el agente no ejecuta una tarea.

Se conservan las treinta entradas del catálogo. BLE inicia su búsqueda sin el menú anterior; GATT queda como opción expresa. El planificador no puede añadir USB/radar sin que la petición solicite hardware. La cancelación ofrece un estado neutral y «Preparar de nuevo» permite reiniciar la preparación sin repetir operaciones automáticamente.

## Archivos modificados

`AgentTasks.kt`, `AgentPanel.kt`, `AgentPolicy.kt`, `AgentActionRuntime.kt`, `LocalAiPanel.kt`, `AiPolicy.kt`, versión Android, pruebas de tareas, acciones y planificación, README y guía del agente. Sin cambios en firmware, manifiesto o permisos.

## Validación

Compilación de APK y pruebas instrumentadas, compilación de pruebas JVM y lint correctos. 162 pruebas JVM correctas: ocho nuevas sobre tareas, frases, entradas y solicitudes de hardware; las anteriores se conservan.

Trece casos Android distintos correctos en el lote final sobre Galaxy A15 SM-A155F, Android 16: 71,546 s, registro privado `a15-020-final-layout.txt`. APK y APK de pruebas reinstaladas tras compactar la cabecera. Versión leída del paquete: `0.20.0-ciberdeck-dev`, código 27. Sin errores AndroidRuntime en la lectura final del registro.

- Tres casos nuevos: cinco resultados auténticos con un toque y sin modelo/periférico; petición de conexión resuelta localmente; petición desconocida sin error ni exigencia de ESP32.
- Seis casos de acciones existentes: operaciones y selectores, parada, BLE directo y consultas del catálogo. Los casos físicos pendientes no se convierten en lecturas por abrir una pantalla.
- Tres casos de navegación: acceso desde Inicio, menú y kit; continuidad del plan al cambiar entre analista y agente.
- Un caso con modelo Qwen3 local: carga real, conservación entre modos, propuesta de móvil/conexión antes de ejecutar, resultados auténticos y guardado verificable en el trabajo activo.

Un primer intento del caso con IA terminó con «Plan interrumpido al salir»; el dispositivo aparecía bloqueado y en reposo al comprobarlo. Tras desbloquearlo el usuario, el caso pasó. No se contabiliza el intento fallido como prueba correcta. Después se compactó la cabecera y se repitió la validación sobre la APK final.

## Capturas

PNG reales capturados desde la app de pruebas en el A15. La entrada muestra la tarea directa y la segunda captura los cinco pasos completados; los detalles de red permanecen plegados. No son diseños ni lecturas simuladas.

<img src="screenshots/ciberdeck-agent-entry-020.png" width="600" alt="Entrada real del agente en el A15">

<img src="screenshots/ciberdeck-phone-task-020.png" width="600" alt="Cinco pasos reales del móvil completados">

## Límites y próximo paso

El modelo pequeño sigue siendo experimental: estas mejoras no garantizan entender cualquier instrucción. La gramática local reconoce solo las frases documentadas por sus pruebas. Una revisión de ajustes no certifica que el móvil esté libre de malware.

No se ha actualizado el OPPO ausente, modificado el ESP32 ni validado OTG directo. NFC con etiqueta física, GATT con equipo autorizado y transferencia con segundo móvil necesitan sus recorridos físicos correspondientes; se mantienen los pendientes anteriores. Las operaciones conservan permisos y selectores Android. Sin root ni acceso privado a otras aplicaciones.

Rama `feature/superflyper-usb-v0-1`, PR #1 en borrador. Sin fusión en main ni release. Modelo, registros privados y APK quedan fuera de Git.

## APK local

Versión `0.20.0-ciberdeck-dev`, código 27.

- Ruta: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 66094803 bytes.
- SHA-256: `b7c18a968d5f318602e297c13e45cab29057e1e3c416b5beb11f581b4c9d3f14`.
- Copias locales en `dist/`; actualización sin borrar datos del paquete.
