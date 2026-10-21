# CiberDeck 0.34 · Envío de petición magnética dictada

## Corrección

La frase «mírame el campo magnético del cuarto de baño» no pertenecía a los alias directos de 0.33. Sin modelo cargado no preparaba ningún plan y el aviso quedaba después de los accesos rápidos/catálogo. El usuario veía un botón aparentemente inerte; no se había validado ese dictado en 0.33.

AgentTasks añade la misión magnética y variantes explícitas de la petición, con acentos, prefijo y lugar. Se conserva el texto original como petición; el lugar no se convierte en una localización observada ni permite medir a distancia. Solo se lee TYPE_MAGNETIC_FIELD del móvil actual, sin ESP32 ni modelo. Las peticiones negativas o combinadas con otra operación no se convierten silenciosamente en una sola tarea.

AgentPanel muestra la explicación de la lectura local antes del envío y el aviso de petición no reconocida junto al botón. Se conserva la revisión de la transcripción antes de ejecutar. Los informes magnéticos destacan el último módulo real en µT y el número de muestras; sin muestras no se inventa un valor. El resumen hablado acepta la observación magnética nativa. El adaptador existente toma seis muestras, libera el listener y muestra un error si falta sensor o no responde. Los límites de método permanecen en el informe; no mide RF ni acredita seguridad sanitaria.

Versión de desarrollo 0.34 código 41; conserva datos, motor y demás herramientas. Sin cambios de firmware ni bibliotecas.

## Pruebas

Se añaden regresiones JVM de la frase exacta y variantes, peticiones ambiguas/negativas y resumen derivado del vector real. La prueba Android intercepta únicamente el resultado del contrato ACTION_RECOGNIZE_SPEECH con un texto de ensayo; verifica vuelta al campo, conservación tras recreación y envío al sensor real. Esto no demuestra una transcripción por micrófono. Una prueba separada verifica la cancelación sin envío. La medición del A15 se realiza donde está conectado, no en el cuarto de baño del usuario.

Primera candidata: 252 pruebas JVM correctas, compilación en 2 min 35 s, lint sin errores y 24 avisos. La ronda Android pasó diez casos y falló la conservación del borrador tras recrear la Activity: el contrato devolvía bien el texto antes de recrearla, pero el campo aparecía vacío después. Se corrige la fuente del borrador en DeckSessionModel, conservada en memoria durante configuración/disposición de la lista; no se escribe el texto hablado a disco ni se promete recuperación tras muerte del proceso. Se repetirá la ronda completa.


## Resultado final

Compilación final correcta en 2 min 18 s; 252 pruebas JVM correctas. Lint sin errores/fatales y 24 avisos. APK y test APK instaladas mediante actualización en Samsung A15 SM-A155F Android 16, conservando datos y modelo. Once casos Android correctos en 65,579 s, incluida la regresión que había fallado, cancelación y avisos de envío.

El A15 ofreció magnetómetro y produjo seis muestras físicas; la última magnitud fue 33.8 µT. Es una lectura puntual del lugar del teléfono durante el ensayo, no una medición del baño ni una evaluación de riesgo. La captura es real y la petición visible es el texto del contrato de ensayo; el micrófono no se ha validado por una persona hablando.

[Captura del resultado real](screenshots/ciberdeck-agent-magnetic-034.png).

APK 0.34.0-ciberdeck-dev, código 41, 65265242 bytes. SHA-256: `4869536c645c13ca5ab3ee75feb822b6d853aea4896f89c8acc22c2608ab2a99`. Logs, contrato de ensayo e informe de muestras se conservan en `.private/android-validation/`. OPPO no conectado: esta corrección está instalada en el A15; en el OPPO sigue pendiente. No se modificó firmware ni se probó OTG.
