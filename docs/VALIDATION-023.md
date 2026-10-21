# CiberDeck 0.23 · Coordinador y especialistas

## Qué cambia

Primera implementación de delegación por herramientas, no una sociedad de seis modelos que razonan de forma independiente. Un coordinador asigna las herramientas de cada plan a seis trabajadores de ámbito cerrado: Móvil, Red, Sensores, Hardware, Evidencias y Analista. Solo aparecen los necesarios. Se conserva el orden del plan; un error o cancelación detiene los pasos posteriores. El plan sigue limitado a ocho operaciones y conserva destinos, permisos, selectores y controles existentes.

`AgentTeam` mantiene una asignación exhaustiva de las treinta herramientas. Cada trabajador rechaza una herramienta de otro ámbito antes de llamar al controlador. `executeAgentActions` hace la delegación real y notifica la herramienta activa antes de esperar su respuesta; no completa un paso por abrir una pantalla. El panel deriva cola, actividad, resultados, interacción, error y pasos sin ejecutar del estado de ejecución, no del texto del modelo. Los resultados siguen teniendo su fecha, método y límites originales.

El balance del coordinador cuenta resultados de herramientas, texto generado, confirmaciones manuales, omisiones y pasos sin resultado. Es código determinista: no declara ausencia de amenazas. El informe guardado registra la asignación y la configuración de generación. El analista ofrece el resumen abreviado de todos los especialistas de la misión, el último resultado o una evidencia elegida. El resumen conserva fechas, errores y confirmaciones manuales; puede quedar truncado por el presupuesto de entrada del modelo. No sustituye consultar los originales.

El motor compartido usa temperatura **0,01**, `topK=1`, `topP=1`. Esto reduce las elecciones de muestreo; **no prueba exactitud ni elimina errores del modelo**. Continúa usando Qwen3 0.6B y LiteRT-LM 0.13.1. La tarea directa del móvil no necesita cargar IA. No se añaden root, shell, accesos a otras apps ni firmware.

## Validación de esta candidata

- APK de desarrollo y APK de pruebas compiladas correctamente. Se reconstruyó la instrumentación tras corregir el retorno de un método JUnit; los resultados siguientes usan esa APK de pruebas final.
- Lint: **0 errores, 22 avisos, 1 sugerencia**. Persisten avisos de API/Gradle existentes; no se ocultan.
- **190 pruebas JVM correctas**: incluye once casos nuevos de delegación, ámbito, estados, cancelación, contexto y metadatos. Son pruebas de lógica, no lecturas de hardware.
- Instalación con actualización conservando datos en **Galaxy A15 SM-A155F, Android 16**, versión 0.23.0-ciberdeck-dev, código 30. Android aceptó el lanzamiento de MainActivity. El OPPO no está conectado y no se ha actualizado.
- **Dos pruebas Android físicas correctas en 23,868 s**, ejecutadas mediante instrumentación sin desbloquear el teléfono ni acceder a la pantalla: el modelo local real devolvió `{"steps":["phone","connection"]}` para la petición de revisar móvil/conexión con los nuevos parámetros; el coordinador obtuvo diagnósticos reales de tipo `phone`, `connection` y `resources`, comprobó fechas nuevas, modelo del terminal, asignación a Móvil/Red y metadatos del informe.
- Registro local privado: `.private/android-validation/a15-023-team.txt`; respuesta e informe recuperados del almacenamiento privado de la app en `team-023-plan-private.txt` y `team-023-private.json`. No se publican lecturas privadas.
- Tras desbloquear el A15: **seis pruebas de interfaz correctas en 81,108 s**. Cuatro recorridos conservados (tarea del móvil, petición de conexión, petición desconocida y detalles/preparar de nuevo) y dos nuevos: asignación visible Móvil 4/4 + Red 1/1 y balance calculado; selección del resumen compartido, introducción de pregunta en diálogo Android y respuesta del modelo local, marcada «Interpretación generada · revisar». Se comprobó en memoria que la entrada real contenía `team-context`, `Móvil/phone` y `Red/connection`. No se validó la exactitud semántica de esa respuesta.
- Ensayos previos conservados: el primero pasó 3/4 y falló al recorrer el menú por acceso multihilo a la semántica de Compose; se añadió espera de inactividad tras limpiar el filtro. El siguiente pasó 5/6 y agotó la espera al reconocer el botón nativo «Continuar»; la automatización pasó a admitir también su texto en mayúsculas. Ninguno de estos cambios afecta al código de producción. El ensayo final pasó 6/6; registros privados `a15-023-phone-ui.txt`, `a15-023-team-ui.txt` y `a15-023-team-ui-final.txt`.
- Cuatro PNG auténticos del A15 revisados visualmente: vista completa del equipo en vertical y detalles desplazados del equipo/balance/analista en horizontal. La captura vertical se tomó ejecutando también la tarea desde la pantalla, fuera de las pruebas instrumentadas; la rotación se fijó temporalmente y se restauró a su modo original `free`. La tarea quedó visible en el teléfono. No son dibujos ni pantallas con datos simulados.
- **Pendiente**: las treinta operaciones sobre hardware real, OPPO, conexión OTG directa y validación de calidad general del analista. Las capturas de 0.22 siguen identificadas con su versión.

La respuesta de planificación correcta y el recorrido de interfaz no validan la calidad general del analista. **En este nuevo ensayo, con temperatura 0,01, Qwen3 0.6B volvió a producir afirmaciones sin respaldo**: atribuyó a la app ausencia de permiso de Internet y sugirió dependencia del ESP para la conexión/seguridad. El flujo no convierte esas frases en decisiones ni lecturas. Los fallos de interpretación observados en 0.22 continúan; bajar la temperatura no los ha eliminado. El informe privado abrevia los campos a 1000 caracteres según su política; la prueba comprobó el contenido íntegro de la entrada en memoria antes de exportar el informe.

<img src="screenshots/ciberdeck-team-portrait-023.png" width="280" alt="Equipo del coordinador con resultados reales, captura vertical del A15">

[Detalle de equipo horizontal](screenshots/ciberdeck-team-023.png) · [Balance calculado](screenshots/ciberdeck-team-balance-023.png) · [Estado del analista tras generación real](screenshots/ciberdeck-team-analyst-023.png).

APK final: `app/build/outputs/apk/debug/app-debug.apk`, **66.160.531 bytes**. SHA-256: `dc89cdaefc29173b25234f7d77fe4c37e39e8450d1c7ad8339b4686ac7f6d61b`. Las copias locales de `dist/` y su fichero de huellas se actualizaron. Pesos, APK y trazas privadas permanecen excluidos de Git; no se ha publicado una release.

## Archivos principales

- `AgentTeam.kt`, `AgentTeamPanel.kt`: especialistas, contrato, contexto compartido, estados y panel.
- `AgentExecution.kt`, `AgentPanel.kt`: delegación en el flujo existente y herramienta activa.
- `AgentPolicy.kt`: estado y metadatos del informe.
- `AgentActionRuntime.kt`: selección del contexto compartido para el analista.
- `LocalAiService.kt`, `LocalAiPanel.kt`: muestreo y explicación visible.
- `AgentTeamTest.kt`: asignación, orden, rechazo de ámbito, error, cancelación, procedencia y estados.

## Gemma 4: evaluado, todavía no instalado

Consulta de fuentes primarias el 20-10-2026. [Google documenta Gemma 4 E2B/E4B para dispositivos móviles](https://ai.google.dev/gemma/docs/core). La [distribución E2B de LiteRT Community](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) declara Apache 2.0; el API público devuelve `gated=false` para la revisión `41c5569623a7c189d6eec3e5225260c58cedbce7`.

La variante general `gemma-4-E2B-it.litertlm` mide **2.588.147.712 bytes**, SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`: supera el límite de importación actual de 2 GiB. La variante GPU mide 2.008.432.640 bytes y necesita una ruta de aceleración compatible; el motor de esta app está configurado en CPU. No se ha descargado ni reemplazado el modelo que funciona. Antes de ofrecerlo habrá que comprobar la versión de motor requerida por el archivo concreto, preservar el modelo anterior, implementar los límites adecuados y medir carga, memoria, latencia y calidad en el terminal real. Los benchmarks de la ficha corresponden a otros dispositivos, no al A15 ni al OPPO.
