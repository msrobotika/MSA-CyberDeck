# CiberDeck 0.19 · Ejecución desde el agente

## Resultado

La app pasa del catálogo de navegación asistida a un ejecutor de operaciones Android: 30 entradas con adaptador exhaustivo, de las que 29 realizan operaciones implementadas y una consulta informa de módulos futuros. No equivale a acceso root ni a ejecutar todas las acciones sin participación del usuario. [Operaciones concretas y condiciones](AGENT-ACTIONS.md).

La APK de desarrollo está instalada en el Galaxy A15 SM-A155F, Android 16/API 36. Conserva el paquete y los datos. OPPO, firmware ESP y conexión Android–ESP por OTG no se modificaron ni validaron en esta revisión.

## Archivos y comportamiento

- `AgentExecution.kt`: ejecutor suspendible que despacha todas las herramientas, espera resultados y detiene el plan ante error o cancelación.
- `AgentActionRuntime.kt`: adaptadores a los controladores BLE/GATT, NFC, mDNS, magnetómetro, APK, USB, WiFi Sentinel, auditoría TCP, llamadas, archivos, trabajos, informe, transferencia, recepción y analista local. El caso exhaustivo de herramientas obliga a actualizar el runtime al ampliar el enum.
- `AgentAndroidAccess.kt`: permisos, selector de archivos, destino de exportación, rol oficial y elecciones de operación/objeto. Una respuesta de un selector cancelado no se asigna a una solicitud posterior.
- `AgentPanel.kt`, `LocalAiPanel.kt`, `MainActivity.kt`: integración real, estado de ejecución/enlace temporal, parada, refresco del archivo y trabajos, conservación del alcance y cancelación al salir salvo durante el selector Android propio.
- `AgentPolicy.kt`: permiso explícito de red ampliado a mDNS, envío, recepción y auditoría de puertos; validación de hasta ocho puertos de auditoría. El modelo sigue produciendo identificadores tipados, nunca shell ni argumentos ejecutables.
- `AgentActionsDeviceTest.kt`, `AgentWorkflowDeviceTest.kt` y `AgentExpansionTest.kt`: nuevas pruebas y adaptación del recorrido de la versión anterior.

Las operaciones que lo necesitan piden elección del usuario: archivo, adaptador USB, IP resuelta, destino de informe o rol Android. Borrar identifica la copia exacta y exige confirmación. La recepción espera un archivo aceptado y guardado; no completa el paso solo por abrir un puerto. El enlace secreto no se copia al informe. Las interpretaciones del modelo se etiquetan como contenido generado.

Los planes admiten hasta ocho pasos distintos y permanecen en memoria. No se reinician tras cerrar el proceso ni reintentan automáticamente operaciones con efectos. Cambiar/crear un trabajo no sustituye silenciosamente el alcance fijado del plan en curso.

## Compilación y pruebas de lógica

Compiladas la APK principal y la APK de pruebas; lint correcto. Permanecen avisos de deprecación anteriores. **154 pruebas JVM correctas** en la compilación final, incluido consentimiento de red para las nuevas acciones y límites de puertos.

Durante el desarrollo se corrigieron errores de sintaxis y una dependencia de pruebas no disponible: los diálogos se prueban con las API de accesibilidad del sistema, sin añadir una biblioteca a la app. Un intento de pruebas JVM iniciado antes de terminar la compilación encontró clases ausentes; la repetición después de la compilación final pasó las 154 pruebas. Ese intento no se usa como resultado de la candidata.

## Pruebas en A15

**17 casos Android distintos terminaron correctamente**, distribuidos en los siguientes lotes; dos casos se repitieron para confirmar explícitamente que no se omitían por falta de hardware/permisos.

### Siete casos de acciones y recorrido

Lote `a15-019-actions-ui.txt`: **OK (7 tests)**, 40,516 s.

1. Despacho de las 30 entradas por el nuevo ejecutor usando un adaptador fixture claramente identificado. Comprueba orquestación, error y cancelación; no simula lecturas para presentarlas como físicas.
2. Magnetómetro real: seis muestras; consulta informativa de módulos, APK propia e inventario completados en cadena. Ningún paso se da por terminado por abrir una pantalla.
3. Selección de una copia privada creada por la prueba, verificación SHA-256 real y continuación al inventario. La fuente permanece intacta; solo se elimina el fixture propio al terminar.
4. Apertura del selector Android de APK y cancelación: ninguna lectura inventada ni ejecución del inventario posterior.
5. Activación del lector NFC y parada antes de acercar una etiqueta: no completa NFC ni el paso siguiente. No valida lectura física de una etiqueta nueva.
6. BLE: controlador real, búsqueda de diez segundos y continuación al catálogo. No se conecta a dispositivos GATT en esta prueba.
7. Enlace offline y CIDR con campos explícitos de prueba; cálculo de red comprobado sin cargar modelo.

### Nueve casos de regresión nativa

Cinco pruebas de ampliación previas, tres del ejecutor anterior y una de inferencia local terminaron correctamente en `a15-019-regression.txt`. Cubren copias verificadas y alteradas, consultas reales locales y DNS/TCP/HTTP contra un servidor de prueba del propio A15. Las pruebas de continuidad con NFC usan fixtures aislados y etiquetados, no una lectura física.

Qwen3 local propuso `{"steps":["phone","connection"]}` en **17.433 ms**; ambas herramientas devolvieron lecturas auténticas sin modificar el índice. Esto no garantiza comprender cualquier petición.

### Recorrido con IA y repetición final

El primer intento del recorrido UI en el lote de regresión no encontró jerarquías Compose al inicio; no se presenta como aprobado. Tras despertar la pantalla se repitió por separado y pasó: plan local auténtico, ninguna ejecución antes del botón, móvil y conexión reales, guardado y referencia de evidencia coincidente con el trabajo activo.

Lote `a15-019-ui-final.txt`: **OK (3 tests)**, 38,435 s. Incluye ese recorrido y la repetición BLE/NFC. El modo detallado registró código 0 para los tres, sin omisión por supuestos de hardware. No se atribuye una causa definitiva al fallo inicial de arranque de la prueba.

## Capturas auténticas

Capturas del A15, revisadas visualmente, sin lecturas simuladas ni datos personales de dispositivos cercanos desplegados:

<img src="screenshots/ciberdeck-agent-plan-019.png" width="260" alt="Plan real 0.19 antes de ejecutar"> <img src="screenshots/ciberdeck-agent-019.png" width="260" alt="Móvil y conexión reales completados en 0.19"> <img src="screenshots/ciberdeck-agent-actions.png" width="260" alt="Cuatro operaciones del agente completadas en A15">

La entrada Módulos de la captura representa una consulta informativa completada: no una operación RF/IR. El número de ajustes es una observación de las comprobaciones implementadas, no una certificación de seguridad.

## APK final

- Versión `0.19.0-ciberdeck-dev`, código 25, leída del dispositivo tras instalar.
- `app/build/outputs/apk/debug/app-debug.apk`: **66,038,389 bytes**.
- SHA-256: `10d8b10671f1d367fba80a0a17dd48d7272d61fe41fbce4c465075bd786cd332`.
- Copias locales de distribución actualizadas en `dist/`; no release pública.

## Pendientes físicos y límites

Quedan sin validar en el nuevo ejecutor: ESP32 por OTG y respuestas de radar, una etiqueta NFC física, descubrimiento GATT sobre equipo autorizado, recepción/envío con un segundo móvil, elección y análisis de APK externo, exportación a proveedor de documentos, gestión de trabajos y concesión del rol de llamadas. Sus adaptadores están implementados y compilan, pero no se declaran probados de extremo a extremo por eso. Los controladores y límites anteriores se conservan.

Las operaciones de borrado y transferencia no se probaron sobre archivos personales. Los módulos RF/IR/industriales futuros no están implementados. Android mantiene su aislamiento; la app no añade root, shell, acceso a otros procesos ni lectura libre de archivos ajenos.

Registros, evidencias, modelo y prompts permanecen en almacenamiento privado o `.private/`, fuera de Git. Avances guardados en la rama de desarrollo `feature/superflyper-usb-v0-1` y PR #1 en borrador, sin main ni release.
