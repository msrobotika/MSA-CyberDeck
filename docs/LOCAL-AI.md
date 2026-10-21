# Asistente local de CiberDeck

Integración experimental de modelos locales mediante Google LiteRT-LM para Android. La alternativa actual es Qwen3 0.6B mixed INT4, descargada e importada en el A15 sin registro. La generación se ha probado con observaciones reales del teléfono; su calidad sigue siendo limitada y requiere revisión. No se simulan respuestas. Ver [modelo, hash y validación](OPEN-LOCAL-MODEL.md).

## Flujo

1. Abrir Asistente IA en el menú.
2. Obtener el archivo Qwen3 mixed INT4 enlazado en la guía, sin registro. Gemma es una alternativa opcional con sus propias condiciones y no se ha descargado ni aceptado su licencia en nombre del usuario. No se pide introducir tokens, claves ni contraseñas en CiberDeck.
3. Importar el archivo `.litertlm` con el selector de Android. Se comprueba la cabecera, se calcula SHA-256 por bloques y se guarda una copia privada de hasta 2 GiB. La extensión y el hash no autentican el editor. La compatibilidad definitiva se conoce al cargarlo.
4. Cargar manualmente el modelo. El motor usa CPU con dos hilos, contexto de 2048 tokens y una comprobación previa de al menos 1,5 GiB de RAM disponible. Es una precaución, no una garantía de memoria suficiente para cualquier modelo. No se promete velocidad en el A15 ni en el OPPO sin una prueba del modelo concreto.
5. Elegir contexto explícito: ninguno, chequeo del móvil, auditoría de un equipo, APK inspeccionada o último diagnóstico. Vista previa de los datos adjuntos, hasta 2200 caracteres, y pregunta de hasta 800. No se adjuntan automáticamente conversaciones, llamadas, fotos, credenciales o archivos del teléfono.
6. Generar, detener y guardar opcionalmente la interpretación. La salida se etiqueta como IA y no modifica la evidencia original.

## Recursos y aislamiento

`LocalAiService` no está exportado y se ejecuta en `:local_ai`, separado del proceso principal. Solo se carga la biblioteca nativa al comprobar el motor; los pesos se cargan con una acción explícita. Si el proceso nativo termina, el cliente informa del fallo y mantiene disponibles las demás herramientas. Al salir de la vista o detener la actividad se desconecta el servicio y libera su proceso y RAM. Las respuestas conservadas en memoria no acreditan que el motor siga cargado.

Límite de salida 5000 caracteres, contexto total del motor 2048 tokens y vigilancia de 90 segundos por operación. El botón Detener solicita cancelación al motor; si este no responde, el límite temporal termina su proceso. Los fragmentos recibidos son mensajes de streaming, no tokens ni una medida de rendimiento equivalente a tokens/segundo.

No hay herramientas registradas en el modelo ni llamadas automáticas a red, shell, USB, NFC o BLE. La IA solo devuelve texto; ninguna salida se ejecuta. El contexto se trata como datos no fiables. Las instrucciones reducen algunos errores pero no garantizan resistencia a inyección ni exactitud: revisar todas las conclusiones. No clasificar como ataque algo que solo sea una observación.

El proceso separado comparte UID y permisos de CiberDeck: no es un sandbox de seguridad independiente. Esta integración no envía prompts ni informes a un servidor, ni usa Gemini API como respaldo. Abrir la página del modelo o importar desde un proveedor de archivos en la nube puede necesitar red fuera de la inferencia local.

## Estado

Motor, cliente IPC, importación, controles y generación Qwen3 comprobados en el A15. Una respuesta breve a un diagnóstico real tardó unos 30 segundos con CPU y dos hilos; no equivale a un benchmark ni una evaluación de precisión. Las primeras respuestas mostraron límites de calidad y se corrigió la presentación del razonamiento interno; no se acredita al modelo como experto ni detector de ataques. Gemma y el OPPO siguen sin una prueba de inferencia. Gemini Nano/AICore es una tecnología distinta: AICore no apareció instalado en el A15 durante la revisión; no se da por disponible en ese móvil ni en el OPPO.

Versiones fijadas: LiteRT-LM 0.13.1, Kotlin 2.2.21, AGP 8.10.1, Gradle 8.13 y Java 17. Se eligió una versión del motor compatible con la base de compilación actualizable, sin depender de `latest.release`. Android mínimo 26. Esta candidata incluye bibliotecas nativas arm64-v8a y x86_64: la APK completa requiere un dispositivo de 64 bits. No se ofrece todavía una variante para teléfonos de 32 bits sin IA. A15 y OPPO identificados son ARM64.

## Fuentes

- [LiteRT-LM Android](https://developers.google.com/edge/litert-lm/android)
- [API de la versión fijada](https://github.com/google-ai-edge/LiteRT-LM/tree/v0.13.1/kotlin/java/com/google/ai/edge/litertlm)
- [Modelo usado: Qwen3 0.6B, archivo y revisión fijados](OPEN-LOCAL-MODEL.md)
- [Alternativa Gemma, con sus propias condiciones y sin validar aquí](https://huggingface.co/litert-community/Gemma3-1B-IT)
- [Gemini Nano](https://developer.android.com/ai/gemini-nano)

Las licencias del motor y los modelos son independientes de la licencia del código de CiberDeck. Qwen3 declara Apache 2.0; Gemma tiene sus propias condiciones. No se incluyen pesos en el repositorio, APK ni artefactos CI.
