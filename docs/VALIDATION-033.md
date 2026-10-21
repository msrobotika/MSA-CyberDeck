# CiberDeck 0.33 · Centro de mando por voz y texto

## Funciones

El agente ofrece una cabecera con identidad propia, entrada escrita y «Dictar una petición». El dictado abre ACTION_RECOGNIZE_SPEECH de Android, solicita español y preferencia sin conexión y recibe el texto como borrador de hasta 800 caracteres. El usuario revisa y pulsa Enviar petición. No se añade RECORD_AUDIO, escucha en segundo plano ni palabra de activación. El proveedor del dictado puede usar Internet o no estar disponible; se mantiene la entrada escrita.

Peticiones directas nuevas: «CiberDeck, ¿tengo Internet?», «Jarvis, comprueba mi conexión por favor», «Haz un chequeo de mi móvil» y «Compara mis lecturas guardadas». La misión «Revisa esta web https://…» propone LINK + WEB, conserva la URL exacta y exige revisión y autorización de red; no ejecuta tráfico automáticamente. Se conservan los demás adaptadores, selectores, controles y motor de IA local. Los alias son reglas explícitas, no comprensión general del lenguaje ni una inferencia del modelo.

El resumen ofrece reproducción explícita mediante TextToSpeech. Se elige una voz española instalada que el motor declara no dependiente de red; si no existe, se conserva la salida textual. No se descargan voces automáticamente. La lectura usa estado de ejecución y hasta dos resúmenes de herramientas nativas de móvil/conexión/recursos. No lee campos completos, URL, QR ni respuestas generadas; distingue pasos manuales e interpretaciones de IA. El motor Android es externo y su declaración de voz local no constituye una auditoría de su comportamiento. La lectura se detiene al pasar a segundo plano y el motor se libera al salir.

Los resultados se presentan antes del equipo de especialistas, ahora plegado. AgentAnswerPolicy destaca una observación real frente a pasos auxiliares: conexión precede a una muestra posterior de RAM; WEB precede al análisis sintáctico LINK cuando ambos existen. No promueve resultados manuales, pendientes, con error ni texto de IA a «Lo observado». El resumen es el original de la herramienta, no una conclusión inventada. Inicio destaca Hablar con CiberDeck. Se mantiene la identidad CiberDeck/MSA Robotika y el robot original.

La consola incorpora un HUD oscuro con trama, aros cian y robot original MSA Robotika. El estado procede de la sesión: espera, preparación, ejecución, intervención pendiente, finalización y lectura de voz. El contador cuenta resultados nativos aceptados por AgentAnswerPolicy, no sensores ficticios. Los aros son ornamentales: giran solo durante actividad y respetan la desactivación de animaciones del sistema. No hay blancos de radar inventados, alertas de ataques ni telemetría aleatoria. El formulario mantiene su posición tras una cabecera compacta y el resultado tiene borde propio.

## Archivos

AgentHud.kt, AgentAnswerPolicy.kt, AgentVoicePolicy.kt, AgentSpeaker.kt, AgentVoiceInput.kt, AgentTasks.kt, AgentPanel.kt, CiberDeckHome.kt y consultas de visibilidad en AndroidManifest.xml. Versión 0.33 código 40. Sin nuevas bibliotecas ni permisos de micrófono.

## Validación

Candidata final compilada en 1 min 57 s. **248 pruebas JVM correctas**, incluidos filtros de fuentes, límites del resumen hablado, alias y URL/autorización de la misión web. Lint: cero errores/fatales y 24 avisos. Permanece un aviso Kotlin de callback TTS obsoleto, además de los avisos anteriores.

APK y APK de ensayo instaladas por actualización en Samsung A15 SM-A155F Android 16, conservando datos y modelo. **Nueve casos Android correctos en 50,850 s**: petición «CiberDeck, ¿tengo Internet?» con connection/resources reales sin cargar modelo, observación principal idéntica al resultado original de conexión, HUD en espera y finalizado, control de voz y ausencia de RECORD_AUDIO, síntesis o fallback, tareas directas, foco y equipo desplegable.

El motor encontró voz española declarada local y notificó «Lectura finalizada» para la frase de prueba. No se acredita audición física. Las dos capturas PNG enlazadas proceden de la Activity real del A15; se capturó en vertical restaurando después la orientación de la Activity, sin cambiar el ajuste global del usuario. El resultado visible afirma únicamente que Android ha validado Internet, no una navegación manual ni seguridad de la red.

[Entrada del agente](screenshots/ciberdeck-agent-command-033.png) · [Respuesta real de conexión](screenshots/ciberdeck-agent-answer-033.png).

Las rondas previas detectaron exceso de controles y una respuesta que destacaba la última muestra auxiliar de RAM. La candidata final adelanta el formulario, pliega el equipo y elige la observación principal con política determinista. Se verificó nuevamente tras el HUD y los últimos ajustes; no se toma el resumen anterior de RAM como respuesta válida de conexión.

APK de desarrollo 0.33.0-ciberdeck-dev, código 40, 66442801 bytes. SHA-256: `41fd373903604869477bf3f2a22c4d19b6c676b958901d1a3097abff77d0fe05`. Logs y registro de instalación permanecen en `.private/android-validation/`, fuera de Git. No se modificó firmware, no se probó OTG directo ni se revalidó inferencia del modelo en esta ronda. El OPPO no estaba conectado.

La disponibilidad del intent no acredita una transcripción hablada. Los callbacks de síntesis no prueban que el usuario oiga el altavoz.

## Fuentes técnicas

- [RecognizerIntent oficial](https://developer.android.com/reference/android/speech/RecognizerIntent): Activity de dictado, resultado y preferencia offline, dependiente del proveedor.
- [TextToSpeech oficial](https://developer.android.com/reference/android/speech/tts/TextToSpeech): inicialización, voces, síntesis y liberación.
- [Visibilidad de motores TTS](https://developer.android.com/training/package-visibility/use-cases): consulta TTS_SERVICE para Android 11 o posterior.

## Pendiente

Prueba de dictado hablado con el usuario, calidad acústica, voces no disponibles y proveedores alternativos. No es un asistente omnipotente: conserva permisos Android, alcance de las herramientas y revisión de operaciones con red/destino. No se han añadido root, intrusiones, clonación de credenciales, RF/IR ni micrófono permanente.
