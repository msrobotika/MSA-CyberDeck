# CiberDeck 0.35 · Asistente de campo

## Experiencia

La cabecera conserva el robot original y los aros de estado. Una vez existe una tarea se compacta para dar prioridad a la observación. Mientras se ejecuta, el detalle procede del controlador real (herramienta o muestras), sin telemetría inventada. El contador distingue un resultado de varios.

«Nueva petición» abre el formulario sin el bloque del resultado anterior y permite volver a la tarea actual. No sustituye ni ejecuta esa tarea hasta enviar otra. «Conversación reciente» conserva hasta cinco tareas terminadas de esta sesión, con su fecha, estado y observación nativa principal. Si solo hay texto generado/manual, no se presenta como observación. Reutilizar una petición solo rellena el borrador; no repite tráfico ni autorizaciones. Vaciar la conversación no borra evidencias. Esta memoria es local en RAM y no se adjunta al modelo ni se escribe a disco; no es memoria semántica de la IA ni resiste la muerte del proceso.

«Responder en voz alta» es opcional y está desactivado inicialmente. Al activarlo, una finalización elegible aún no leída puede leer (incluida la tarea actual al activar el control) el resumen acotado existente. Se exige primer plano, motor listo, finalización sin error/espera/cancelación y un identificador aún no leído. El identificador y la preferencia sobreviven a la recreación de la Activity en el ViewModel, evitando repetir automáticamente la lectura. Pasar a segundo plano detiene la voz. Continúa disponible Escuchar resumen para reproducción manual. El motor debe ofrecer una voz española instalada declarada local; no se añaden permisos, escucha permanente ni palabra de activación.

Se conservan funciones, modelo, datos y correcciones de dictado/medición de 0.34. Versión de desarrollo 0.35 código 42. No se cambia firmware.

## Validación

Los nuevos ensayos preparados comprueban límite/deduplicación del historial, ausencia de hechos generados, condiciones de voz y uso de una petición anterior como borrador sin ejecutar. Las pruebas físicas se distinguen del contrato de dictado con texto de ensayo.


## Resultados y pendientes

Compilación final correcta en 2 min 12 s; **257 pruebas JVM correctas**. Lint: cero errores/fatales y 24 avisos. APK de desarrollo código 42 instalada mediante actualización en A15 SM-A155F; versión comprobada, datos/modelo conservados. APK de instrumentación compilada. El A15 estaba bloqueado durante las comprobaciones: **no se ejecutaron los trece casos Android previstos, no se comprobó el arranque visible ni se obtuvieron capturas de 0.35**. La voz automática y la interacción de conversación quedan experimentales hasta realizar esa prueba física. Los trece ensayos de esta ronda incluyen los once anteriores más reutilización/vaciado de conversación y estado de voz después de recrear; sus resultados no se heredan de 0.34.

OPPO no conectado; conserva 0.33 según la última instalación registrada. El Samsung tenía 0.34 y ahora 0.35. Sin pruebas nuevas de dictado por micrófono, audición física, inferencia del modelo ni USB OTG.

APK: 66491728 bytes, SHA-256 `92a855aec33017d63df9ad6822b992d4199374f231ddb60175eeb29815992c3a`. Registros privados en `.private/android-validation/`; copia local en `dist/CiberDeck-0.35.0-dev.apk`. No se crea una release ni se fusiona main.

Archivos principales: AgentConversation.kt, AgentHud.kt, AgentPanel.kt, DeckSessionModel.kt; pruebas AgentConversationPolicyTest.kt y AgentConversationDeviceTest.kt, y ampliación de AgentFocusDeviceTest.kt.
