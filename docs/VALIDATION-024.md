# CiberDeck 0.24 · Fuentes primero y equipo compacto

## Implementación

El equipo del agente muestra nombre y estado en una fila por especialista. Las operaciones se despliegan con «Ver tareas de especialistas». El resumen principal conserva progreso, errores y último resultado; los resultados completos siguen en «Ver detalles de resultados». Los treinta adaptadores, sus permisos y la delegación no cambian. Los ajustes de modelo del analista también quedan plegados, con importación/carga accesibles.

«Revisar fuentes · sin IA» toma opcionalmente una instantánea Android mediante las API públicas y consulta la fuente elegida. Si es una evidencia guardada vuelve a comprobar su copia antes de preparar el resumen, igual que al generar. Funciona sin cargar ni consultar un modelo. La tarjeta muestra valores Android con su fecha, procedencia de la fuente adicional, fragmento consultado y entorno/objetivo. «Guardar fuentes» crea un diagnóstico `source-brief`, preparado por código.

`AiSourcePacket` conserva la consulta como una instantánea, no como una vista del estado que cambie mientras el modelo responde. Sus filas proceden exclusivamente de la lectura Android tipada y de los campos representados en el contexto; no se extraen del objetivo ni de la respuesta. La misma lista permitida y ordenada alimenta el contexto y las filas; se prioriza la versión Android. Las fuentes adicionales conservan tipo, destino, fecha y hasta 1200 caracteres. Resúmenes, confirmaciones manuales y texto de IA no se presentan como mediciones nuevas. Una fuente puede ser incompleta o errónea: este resumen no certifica su verdad ni la seguridad del equipo.

La generación sigue siendo opcional. Su respuesta queda rotulada como interpretación para revisar y plegada por defecto. Se puede abrir, seleccionar, guardar y volver a plegar; los datos capturados permanecen separados. Al preparar fuentes sin IA se retira de la vista la respuesta anterior. Esto **no corrige la calidad del modelo** ni es un detector general de alucinaciones. Las entradas y sus huellas siguen disponibles cuando se envía una consulta real.

## Pruebas

- Compilación final de APK y pruebas correcta; lint: **0 errores, 22 avisos, 1 sugerencia**. Se mantienen los avisos previos de API/Gradle.
- **200 pruebas JVM correctas**, con diez casos nuevos sobre procedencia, exclusión de campos, prioridad de Android, fragmento de 1200 caracteres y separación de declaraciones/IA/manual/resúmenes.
- Actualización instalada conservando datos en **Galaxy A15 SM-A155F, Android 16**, versión 0.24.0-ciberdeck-dev, código 31. Solo está conectado este terminal; el OPPO que recibió 0.23 no se ha actualizado a 0.24.
- **12 pruebas Android correctas en 148,042 s** sobre la APK final: cuatro casos nuevos de fuentes y ocho regresiones de tareas, especialistas, runtime nativo y verificación de evidencia. Incluye generación local real con Qwen3, captura de entorno en la entrada, SHA-256, salida plegada por defecto, preservación de fuentes al abrirla, exclusión de Android y guardado sin cargar modelo. La primera candidata también pasó 12/12 en 131,928 s; después se repitieron por cambios de posición de controles, retirada de salida anterior y visibilidad de errores/progreso.
- La prueba con texto `UNVERIFIED_UI_FIXTURE_NOT_A_DEVICE_READING` y error `EXPLICIT_ENGINE_UI_FIXTURE_NOT_A_DEVICE_READING` verifica solo controles de pantalla. No son inferencias ni lecturas y no se guardan ni capturan. Los ensayos con modelo real se identifican por `agentAi=qwen3`; no valoran exhaustivamente la exactitud del texto generado.
- Se corrigió la limpieza del ensayo de guardado: al borrar su informe temporal debe retirar también la referencia creada por ese ensayo en el trabajo activo. Dos referencias temporales de las rondas anteriores se identificaron por nombre, metadatos exactos y las ventanas de ejecución; se guardaron copias privadas antes de retirarlas mediante `AuditWorkStore`. Tras recompilar solo la instrumentación, el caso aislado pasó en **6,11 s** y una comparación de los índices antes/después confirmó que solo desaparecieron esas dos referencias. Nombres, notas, alcances, otros archivos y demás referencias quedaron idénticos. El código de producción y la APK no cambiaron durante este ajuste del ensayo.
- Registros privados: `.private/android-validation/a15-024-sources-ui.txt`, `a15-024-sources-ui-final.txt`, `a15-024-source-cleanup.txt` y `ai-sources-024-private.txt`. Respaldos y plan exacto de limpieza permanecen en `.private/`; no se publican.
- Dos PNG auténticos del A15 revisados visualmente, con lecturas reales y sin textos fixture: fuentes sin interpretación y equipo compacto. Las capturas anteriores conservan su versión. Se abrió además el analista y se prepararon fuentes frescas desde la pantalla sin generar IA.

La prueba de generación confirma el transporte y la separación de datos/salida; **no demuestra que las conclusiones sean correctas**. Se mantienen los límites de calidad documentados en 0.23. No se ha cambiado firmware ni importado otro modelo.

APK final: `app/build/outputs/apk/debug/app-debug.apk`, **66.194.886 bytes**, SHA-256 `d2de85a5aacd797023b8b918f6e9bf727543796146d2cdc3866c79023b896473`. Copias locales y huellas de `dist/` actualizadas. APK, pesos y registros privados fuera de Git; sin release ni fusión en main.

<img src="screenshots/ciberdeck-sources-024.png" width="280" alt="Fuentes reales sin interpretación en A15"> <img src="screenshots/ciberdeck-team-024.png" width="280" alt="Especialistas del coordinador en la presentación compacta">

## Archivos

- `AiSources.kt`, `AiSourcesPanel.kt`: paquete de fuentes, procedencia, informe y tarjeta.
- `LocalAiClient.kt`, `LocalAiPanel.kt`: preparación sin generación, captura previa a IA, ajustes y respuesta plegados.
- `AiContext.kt`: lista permitida común y orden de campos.
- `AgentTeamPanel.kt`, `AgentPanel.kt`: equipo compacto y último resultado con acceso al detalle.
- `AiSourcesTest.kt`, `AiContextTest.kt`, `AiSourcesDeviceTest.kt`: procedencia, límites, controles y flujos.

## Límites

No añade sensores ni identifica ataques. El magnetómetro mantiene su alcance de observación local, sin evaluar exposición laboral ni riesgo sanitario. USB OTG directo, OPPO en esta versión y módulos externos siguen pendientes. No hay cambios en permisos, firmware, modelos importados ni funciones existentes de red, NFC o archivos. Los ensayos con texto de respuesta inyectado para probar el plegado se identifican como fixtures, no como inferencia real, y no se guardan ni capturan.
