# CiberDeck 0.37 · Peticiones de conexión Wi-Fi

Se corrigen dos rutas que impedían ejecutar «analiza mi red wifi»: faltaba la palabra «red» en el reconocimiento de conexión y el cuadro de seguimiento solo admitía instrucciones sobre el resultado anterior. Ambos cuadros usan ahora AgentInputPolicy y el mismo envío, también para las transcripciones recibidas desde Android.

Se reconocen variantes explícitas como «Wi-Fi», «Wi‑Fi», «wi fi», «red inalámbrica», espacios repetidos y «Jarvis, analiza mi red wifi por favor». Se conservan las comprobaciones de negación, tareas compuestas y operaciones destructivas. Una petición desconocida muestra un aviso visible si la IA no está disponible; con el modelo cargado pasa al planificador existente. No implica comprensión general de cualquier petición.

La tarea reutiliza CONNECTION y RESOURCES: consulta la conexión activa y recursos reales del teléfono, sin ESP32 ni modelo de IA. La respuesta principal muestra el transporte observado en ese resultado. Android puede informar Wi-Fi, datos móviles, VPN u otra conexión; pedir Wi-Fi no transforma una conexión móvil en una lectura Wi-Fi. No realiza un barrido de equipos, prueba de velocidad ni auditoría de seguridad. No cambia permisos, modelo ni firmware.

Versión 0.37.0-ciberdeck-dev, código 44. Compilación correcta y 266 pruebas JVM correctas. Las nuevas pruebas cubren variantes, límites del reconocimiento y envío compartido. La prueba de dictado utiliza un resultado controlado del contrato Android; no equivale a hablar al micrófono.

## Validación final

Instalada por actualización en Samsung A15 SM-A155F Android 16, versión y código comprobados. Pasaron los 18 casos Android en 123,493 segundos, incluidas la frase exacta escrita, una tarea nueva desde seguimiento, transcripción controlada «analiza mi red Wi-Fi» sin modelo cargado, aviso visible para una auditoría no reconocida y las regresiones de conversación, lecturas, repetición y guardado real sin duplicados. Las fuentes y el transporte mostrado coinciden con el resultado real de Android. No se comprobó dictado hablado por una persona. Compilación en 2 min 44 s; lint cero errores y 24 avisos.

APK: 65298010 bytes, SHA-256 `ff0f9921b402909a8c741c4820d7b66a31ccbcec88a1e935aaff3153530a18fc`. Copia local dist/CiberDeck-0.37.0-dev.apk. Registros privados a15-037-network-final.txt y a15-037-install.json en .private/android-validation. OPPO no conectado en esta ronda; no se declara actualizado.

[Captura auténtica del resultado de conexión en 0.37](screenshots/ciberdeck-network-037.png), tomada durante el caso existente «¿tengo Internet?», que usa la misma herramienta. La frase exacta nueva se verificó por aserciones de interfaz y estado, sin sustituir las lecturas reales por datos de ensayo. Sin cambios de código después de compilar la APK validada.
