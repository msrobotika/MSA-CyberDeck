# CiberDeck 0.36 · Continuar una tarea

Se prioriza la observación principal y su fuente. El progreso solo se dibuja durante ejecución; se retira el resumen auxiliar repetido y quedan los detalles/método y la preparación avanzada plegados. La cabecera inicial es menor. Repetir y Guardar resultado aparecen junto a la respuesta; se conserva lectura de voz opcional, historial temporal y todas las herramientas.

«Continúa esta tarea» recibe texto o el resultado del dictado Android como borrador. Reconoce instrucciones exactas como repítelo, guárdalo, muéstrame los detalles y nueva petición. Son reglas explícitas con contexto de la tarea actual, no comprensión general ni nuevas inferencias. Las peticiones compuestas/negativas desconocidas no se ejecutan.

Repetir crea una sesión limpia, sin resultados, fuente pendiente ni autorización de red anteriores. Solo los planes de lectura local de teléfono/conexión/RAM/revisión/APK propia/magnetómetro se ejecutan directamente. Los demás planes se vuelven a preparar con networkApproved=false y requieren su revisión y permisos; no se repiten transferencias ni borrados automáticamente.

Guardar usa el controlador existente, vuelve a abrir la copia y verifica igualdad, tamaño y SHA-256 mediante EvidenceStore antes de confirmar la petición en el agente. La confirmación llega tras intentar la asociación al trabajo; el fallo de asociación se informa por el controlador sin afirmar que la copia no existe. Se bloquea la duplicación de esa ejecución durante la sesión. Un tiempo agotado no se anuncia como éxito. No se escribe sobre originales.

Versión 0.36 código 43. Pruebas preparadas: política de seguimiento, plan limpio/revisión de red, nueva lectura física, despliegue de fuentes y copia real verificada sin duplicación. El ensayo de guardado elimina únicamente su propia copia y referencia de trabajo identificadas por el ID creado durante esa prueba. Pendiente de resultados de compilación y ejecución.


## Validación de compilación

Candidata final compilada en 1 min 59 s. 262 pruebas JVM correctas; lint cero errores/fatales y 24 avisos. APK de desarrollo 0.36 código 43 instalada por actualización en Samsung A15, versión comprobada. El teléfono se bloqueó por inactividad antes del ensayo Android: los quince casos preparados y las capturas nuevas siguen pendientes de desbloqueo. No se presentan como validados el guardado por la interfaz ni el nuevo seguimiento hasta ejecutar esa ronda.

APK: 66518646 bytes. SHA-256 `0d3b27fa0422bb9bb338ebc20dcffa8cb5c14b94841923f099ceb8c3ea7f160c`. Copia local en dist/CiberDeck-0.36.0-dev.apk. OPPO no conectado en esta ronda; conserva 0.35. No se cambió firmware ni modelo ni se realizó dictado humano, evaluación sanitaria ni prueba OTG.
