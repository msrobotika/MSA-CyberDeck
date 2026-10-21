# CiberDeck 0.21 · Resultados del agente primero

## Cambio

Al ejecutar una tarea, el agente muestra primero un resumen de progreso y resultados. Antes había que recorrer el plan y varias tarjetas grandes para llegar al guardado. Ahora se ven el estado, los pasos con resultado y una síntesis de cada herramienta; «Guardar ejecución como evidencia» está en el mismo resumen.

«Ver detalles de resultados» despliega los métodos y datos completos existentes. Los pasos pendientes de interacción conservan sus controles visibles. La parada permanece accesible mientras se ejecuta; «Preparar de nuevo» no repite acciones y conserva la huella del modelo que preparó el plan.

El resumen distingue errores, pausa, interrupción, pasos omitidos, confirmaciones manuales e interpretaciones de IA. Completar herramientas no equivale a certificar seguridad. Los estados se calculan sobre los resultados del ejecutor; no se inventan hallazgos, muestras ni puntuaciones.

## Archivos

`AgentSummary.kt`, `AgentPanel.kt`, versión Android, pruebas de resumen y de tareas en dispositivo. Documentación y captura real del A15.

## Validación final

- Compilación de APK y pruebas, compilación JVM y lint correctos (2 min 25 s). Se conserva un aviso anterior por API de cifrado Android deprecada.
- 169 pruebas JVM correctas. Siete casos nuevos verifican preparación sin ejecución, progreso inicial, interrupción parcial, espera de interacción, error en el último paso y distinción de resultados, omisiones, confirmaciones manuales e IA.
- 14 casos Android correctos en el A15, 74,297 s, registro privado `a15-021-final.txt`.
- Revisión de cinco pasos con datos reales, resumen visible sin modelo, conexión local, mensajes ante petición desconocida y apertura/cierre de métodos completos.
- «Preparar de nuevo» deja resultados vacíos y ejecución parada hasta una nueva pulsación.
- Regresiones comprobadas: controladores de acciones, sensor real, BLE, cancelación de selector, parada de NFC sin inventar una lectura, navegación y planificación con Qwen3 real seguida de ejecución y guardado verificable.

Los casos de orquestación con adaptadores de prueba no validan hardware físico. En particular, parar un lector NFC sin etiqueta no comprueba una lectura NFC.

## Límites

Se mantienen las herramientas, permisos y límites de 0.20. Esta revisión mejora presentación y continuidad; no añade nuevas capacidades RF, acceso root o validación OTG. El modelo local sigue siendo experimental. Una etiqueta o segundo equipo físico requiere su propia prueba; los casos de pruebas de estado se identifican como fixtures, no lecturas reales.

El OPPO se actualizó a 0.20 en el turno anterior. Al comenzar esta revisión solo está conectado el A15; no se da por instalada la 0.21 en el OPPO ausente.

Sin modificación de firmware ni borrado de datos. Rama de desarrollo y PR #1 en borrador, sin main ni release.

## Captura real

<img src="screenshots/ciberdeck-agent-summary-021.png" width="320" alt="Resumen real de cinco comprobaciones del A15, con acceso a detalles y guardado">

Capturada desde la app ejecutada en el A15 después de cinco resultados auténticos, sin cargar modelo ni conectar un periférico. No muestra identificadores de red; los datos completos están plegados.

## APK local

Versión `0.21.0-ciberdeck-dev`, código 28, leída del paquete instalado en el A15.

- Ruta: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 65084894 bytes.
- SHA-256: `9dc127017be57595b2e733c70df2f5512be9b6884243e27434a177d89c7b6e1b`.
- Copias de distribución local en `dist/`; no se publica una release.
