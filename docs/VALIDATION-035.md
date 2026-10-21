# CiberDeck 0.35 · Asistente de campo

## Experiencia

La cabecera conserva el robot original y los aros de estado. Una vez existe una tarea se compacta para dar prioridad a la observación. Mientras se ejecuta, el detalle procede del controlador real (herramienta o muestras), sin telemetría inventada. El contador distingue un resultado de varios.

«Nueva petición» abre el formulario sin el bloque del resultado anterior y permite volver a la tarea actual. No sustituye ni ejecuta esa tarea hasta enviar otra. «Conversación reciente» conserva hasta cinco tareas terminadas de esta sesión, con su fecha, estado y observación nativa principal. Si solo hay texto generado/manual, no se presenta como observación. Reutilizar una petición solo rellena el borrador; no repite tráfico ni autorizaciones. Vaciar la conversación no borra evidencias. Esta memoria es local en RAM y no se adjunta al modelo ni se escribe a disco; no es memoria semántica de la IA ni resiste la muerte del proceso.

«Responder en voz alta» es opcional y está desactivado inicialmente. Al activarlo, una finalización elegible aún no leída puede leer (incluida la tarea actual al activar el control) el resumen acotado existente. Se exige primer plano, motor listo, finalización sin error/espera/cancelación y un identificador aún no leído. El identificador y la preferencia sobreviven a la recreación de la Activity en el ViewModel, evitando repetir automáticamente la lectura. Pasar a segundo plano detiene la voz. Continúa disponible Escuchar resumen para reproducción manual. El motor debe ofrecer una voz española instalada declarada local; no se añaden permisos, escucha permanente ni palabra de activación.

Se conservan funciones, modelo, datos y correcciones de dictado/medición de 0.34. Versión de desarrollo 0.35 código 42. No se cambia firmware.

## Validación

Los nuevos ensayos preparados comprueban límite/deduplicación del historial, ausencia de hechos generados, condiciones de voz y uso de una petición anterior como borrador sin ejecutar. Las pruebas físicas se distinguen del contrato de dictado con texto de ensayo.


## Resultados y pendientes

Compilación de producción correcta en 2 min 12 s; **257 pruebas JVM correctas**. Lint: cero errores/fatales y 24 avisos. APK código 42 instalada por actualización en A15 SM-A155F, versión comprobada, datos/modelo conservados.

La primera comprobación quedó pendiente porque el A15 estaba bloqueado. Tras desbloquearlo, **los trece casos Android pasaron en 78,411 s** sobre esa misma APK: dictado mediante resultado de contrato de ensayo y cancelación, petición magnética con sensor físico, envío y respuesta nativa de conexión, síntesis/fallback, tareas del móvil, conservación del plan y foco, equipo, conversación temporal, reutilización como borrador y vaciado sin cambiar la tarea, preferencia de voz e identificador leído conservados después de recrear. El borrador nuevo oculta los campos de la tarea anterior.

El test de voz automática comprueba que una finalización real se marca una sola vez y que la marca/preferencia sobrevive a recreación; la prueba del motor por separado recibió «Lectura finalizada». No se ha verificado acústicamente el altavoz ni dictado una persona al micrófono. Los trece casos son la ronda completa de 0.35, sin heredar resultados de 0.34.

Se revisaron capturas PNG reales de entrada, cabecera compacta y respuesta de conexión. El estado de Internet es el validado por Android, no una navegación manual ni una valoración de seguridad.


OPPO no conectado; conserva 0.33 según la última instalación registrada. El Samsung tenía 0.34 y ahora 0.35. Sin pruebas nuevas de dictado por micrófono, audición física, inferencia del modelo ni USB OTG.

APK: 66491728 bytes, SHA-256 `92a855aec33017d63df9ad6822b992d4199374f231ddb60175eeb29815992c3a`. Registros privados en `.private/android-validation/`; copia local en `dist/CiberDeck-0.35.0-dev.apk`. No se crea una release ni se fusiona main.

Archivos principales: AgentConversation.kt, AgentHud.kt, AgentPanel.kt, DeckSessionModel.kt; pruebas AgentConversationPolicyTest.kt y AgentConversationDeviceTest.kt, y ampliación de AgentFocusDeviceTest.kt.


## Capturas adicionales

Se amplió únicamente la APK de ensayo para capturar conversación, borrador recuperado y control de voz, usando la orientación de la Activity y restaurándola después. La primera ronda adicional pasó el caso de voz y falló el de conversación: el dibujo de captura Compose coincidió con la precomposición de la lista y provocó SnapshotStateObserver multihilo. Se sustituyó ese método del ensayo por UiAutomation.takeScreenshot del dispositivo, sin cambiar la APK de producción. Los dos casos adicionales pasaron en 17,250 s con el nuevo capturador; se revisaron las imágenes. Esta incidencia de captura no se oculta ni se cuenta como validación correcta.

[Entrada](screenshots/ciberdeck-agent-command-035.png) · [Resultado real](screenshots/ciberdeck-agent-answer-035.png) · [Conversación](screenshots/ciberdeck-conversation-035.png) · [Borrador recuperado](screenshots/ciberdeck-followup-035.png).


Se reforzó después el ensayo automático de voz para esperar «Lectura finalizada» del motor antes de recrear la Activity. La ronda final de esos dos casos pasó en 35,040 s, con capturas de pantalla del dispositivo. Así se confirma también la finalización por callback de la respuesta automática, manteniendo pendiente su audición por una persona. La APK de producción conserva la huella indicada; solo cambió el capturador y las aserciones de la APK de ensayo.

[Control de voz y finalización](screenshots/ciberdeck-voice-035.png).
