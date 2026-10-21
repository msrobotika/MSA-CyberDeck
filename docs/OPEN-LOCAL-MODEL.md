# Modelo local sin registro

La alternativa seleccionada para el A15 es **Qwen3 0.6B mixed INT4** de LiteRT Community. Es un modelo de Qwen convertido a LiteRT-LM; el motor de ejecución es de Google, los pesos no son Gemma ni Gemini.

- Fuente: [LiteRT Community/Qwen3-0.6B](https://huggingface.co/litert-community/Qwen3-0.6B).
- Archivo: `qwen3_0_6b_mixed_int4.litertlm`.
- Revisión fijada: `e6687177e00897731e7716267eebd4eba0dde7c2`.
- Tamaño: 497 516 544 bytes, unos 498 MB decimales.
- SHA-256: `7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1`.
- Licencia declarada: [Apache 2.0 del modelo original](https://huggingface.co/Qwen/Qwen3-0.6B/blob/main/LICENSE).
- Descarga pública: no se utilizaron cuenta, token ni aceptación de condiciones de Gemma. Se comprobó el hash publicado y después la copia transferida al A15.

La variante mixed INT4 está documentada con LiteRT-LM 0.13.1, la versión fijada en CiberDeck. No debe confundirse con otros archivos del mismo repositorio que requieren motores posteriores. El enlace de descarga se fija a la revisión anterior para no cambiar silenciosamente los pesos:

[Descargar el archivo exacto](https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e6687177e00897731e7716267eebd4eba0dde7c2/qwen3_0_6b_mixed_int4.litertlm).

## Uso

Abrir Asistente IA, importar el archivo, pulsar **Cargar IA local**, elegir el contexto y escribir la pregunta. El modelo descargado se conserva fuera de Git y se importa a una copia privada de CiberDeck. Su eliminación desde la app no borra el original seleccionado. La APK no incorpora los pesos ni los descarga en segundo plano.

Se configura `enable_thinking=false` en el contexto de la plantilla del motor y se solicita también `/no_think` cuando el nombre identifica Qwen3. Algunas conversiones emiten etiquetas `<think>` dentro del texto: CiberDeck oculta esos bloques y no presenta pensamientos incompletos como respuesta. Si no hay respuesta final, se informa del fallo. Es una adaptación de presentación y de instrucciones, no una mejora demostrada de la precisión.

## Validación y utilidad

La prueba física optativa `LocalModelDeviceTest` importa el archivo real, comprueba nombre/tamaño/hash, carga el motor en el A15 y pide explicar observaciones obtenidas por `PhoneAudit`, no un incidente ficticio. Deja el resultado y tiempos en el directorio privado de validación. No descarga modelos desde CI ni ejecuta comandos propuestos por la IA.

La primera ejecución cargó en 2388 ms y generó texto en 37280 ms, pero mostró razonamiento interno y algunas conclusiones excesivas. Ese resultado motivó el ajuste de formato y modo breve. **Es un asistente pequeño y experimental**, adecuado para probar explicaciones de diagnósticos; no se acredita como experto, detector de ataques, catálogo de CVE ni sustituto de las herramientas de medición. La fluidez de la respuesta no prueba su exactitud. Revisar siempre sus conclusiones frente a las observaciones originales.

La prueba posterior con plantilla ajustada pasó: carga 2393 ms, generación 29812 ms, salida no vacía y sin etiquetas de razonamiento. La respuesta sigue siendo genérica y no respeta siempre la extensión solicitada; no se declara una evaluación de calidad superada. El intento de captura de la interfaz se detuvo porque el teléfono volvió a bloquearse. Su propietario decidió desbloquearlo después: no se sortea el bloqueo ni se fabrican capturas de esta revisión.

Los tiempos son del Galaxy A15 de esta prueba con CPU y dos hilos. No son una promesa para el OPPO, para todos los prompts ni para una ejecución con carga térmica distinta. Se conservan los límites de memoria, contexto, tiempo y salida descritos en [IA local](LOCAL-AI.md).

## Pantallas del ESP, candidata 0.3.1

El radar inicial se conserva. Las otras vistas se rediseñan con tarjetas de redes, barras de RSSI, arco de señal, contadores separados de cambios/deauth/disassoc y una gráfica de tramas recibidas por segundo. La serie usa diferencias reales de contadores y tiempo medido; no se precarga con puntos decorativos. Sin referencia configurada no se muestra un falso estado de red segura.

El firmware compila para ESP32 clásico. En esta revisión el PC solo detecta el puerto del Samsung y no el CH340 del ESP: **no se ha instalado ni validado físicamente el rediseño**. Las vistas previas de escritorio, si se muestran, se generan con el mismo código C y un snapshot anterior anonimizado; no son capturas nuevas de la placa. Al reconectarla, verificar identidad y recuperación antes de escribir únicamente la región de aplicación.
