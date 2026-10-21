# CiberDeck 0.14 · IA local con evidencias verificadas

## Flujo implementado

Desde **Evidencias**, un diagnóstico o comparación tiene la acción **Explicar con IA local**. La app comprueba índice, tamaño, SHA-256 y formato antes de adjuntarlo al asistente. No admite como fuente otra interpretación de IA. El contexto seleccionado permanece solo en memoria del proceso y puede retirarse sin borrar el archivo.

La pantalla muestra la fuente, fecha de comprobación y referencia ID/SHA-256 desplegable. Elegir una fuente o una pregunta rápida no genera ninguna respuesta ni carga el modelo. Los accesos «Explicar», «Comprobar» y «Preparar informe» solo rellenan la pregunta editable. Hay que cargar el modelo y pulsar «Analizar en este móvil».

Antes de generar, vuelve a verificarse la fuente guardada en un hilo de trabajo. Si ya no está, su tamaño/huella cambia o el motor no está listo, no se envía la pregunta. Salir o detener la actividad cancela la preparación y descarga el motor. La solicitud fija pregunta, modelo, contexto y referencia de la fuente para que cambiar después de selección no cambie la atribución de una respuesta anterior.

«Guardar interpretación» conserva también ID y SHA-256 de la fuente y su fecha de verificación, además de los metadatos previos del modelo y la pregunta. La salida se mantiene identificada como interpretación generada, separada de las observaciones originales. El guardado utiliza el trabajo activo existente. No modifica, elimina ni exporta automáticamente el archivo fuente.

## Privacidad y límites

El contexto enviado al modelo sigue limitado a 2.200 caracteres; ID/huella y metadatos consumen parte de ese límite, por lo que puede quedar contenido fuera. El texto se trata como datos no confiables y ninguna respuesta se ejecuta. Un modelo pequeño puede equivocarse o seguir instrucciones maliciosas de una lectura: este flujo no es un detector de ataques ni una garantía frente a inyección de instrucciones.

Se conserva Qwen3 0.6B importado y el motor Google LiteRT-LM, sin descargas adicionales, registro, nube o permiso Android nuevo. No se leen archivos no elegidos. No se modifica el ESP ni se valida OTG o el OPPO en esta revisión.

## Archivos

SavedAiContext.kt valida y prepara la fuente. DeckSessionModel.kt conserva la selección en memoria; MainActivity.kt conecta Evidencias con IA; EvidenceBrowser.kt añade la acción. LocalAiPanel.kt añade selección, preguntas rápidas, reverificación, cancelación y metadatos al guardar; LocalAiClient.kt amplía los metadatos de la solicitud sin cambiar el protocolo del motor.

SavedAiContextDeviceTest cubre integridad y fuentes incompatibles. SavedContextInferenceDeviceTest es una prueba optativa con el modelo ya importado y un diagnóstico real guardado: no descarga ni sustituye pesos. SavedAiContextUiDeviceTest queda preparada para probar selección, pregunta y retirada con la pantalla desbloqueada.

## Validación

- Compilación de APK y APK de pruebas y lint correctos; quedan advertencias previas de obsolescencia.
- 121 pruebas JVM de regresión correctas.
- Dos pruebas físicas sin interfaz correctas en A15: preparación y reverificación de fuentes, y generación Qwen3 real desde un diagnóstico del propio teléfono ya guardado. El caso aislado rechaza un cambio de un byte manteniendo el tamaño, una fuente eliminada y un informe kind=ai.
- Inferencia real: 48401 ms de generación y 382 caracteres recibidos, sin error del motor. Acredita ejecución local, no precisión de la respuesta ni calidad profesional. El prompt/resultado y los identificadores se conservan bajo almacenamiento privado, excluidos de Git.
- Revisión cualitativa: la respuesta de prueba fue genérica e incluyó una afirmación sobre no detectar ataques que no se considera sustentada. No se transforma en hallazgo ni acredita seguridad; la interpretación debe revisarse. El éxito de la prueba solo acredita generación y flujo de datos.
- Se corrigió después la presentación de errores de preparación para que un intento fallido no cambie el estado atribuido a una respuesta anterior. Se volvió a compilar la APK y se comprobó lint correctamente para ese ajuste; no cambia el motor ni el protocolo probado.
- El A15 sigue bloqueado: no se declara comprobado el recorrido visual nuevo ni el guardado desde sus botones. La prueba UI está preparada, y las pantallas de comparación de 0.13 siguen pendientes de desbloqueo. No se publican capturas simuladas como si fueran físicas.
- APK 0.14.0-ciberdeck-dev, código 19 instalada mediante actualización conservando datos. SHA-256: `d9dfaed07d229e703c83541e528edac25d9fa376545b8c8944d235000877f989`.

Avances en la rama y PR existentes, sin fusionar main ni publicar una release.
