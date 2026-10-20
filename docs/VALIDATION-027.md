# CiberDeck 0.27 · Resultados antes que formularios

## Implementación

DNS/TCP y web muestran primero la última observación: tipo, resumen, destino y fecha de lectura. El guardado permanece visible; el formulario se conserva debajo con «Otra comprobación». No se ejecuta una consulta por abrir la pantalla, copiar o filtrar un resultado.

Los campos técnicos quedan plegados inicialmente. Se pueden abrir y buscar por nombre o contenido, sin distinguir mayúsculas ni acentos; todas las palabras introducidas deben coincidir. Se conservan el orden original y los campos con nombres repetidos. «Mostrar todos los campos» retira la búsqueda. El recuento es de campos, no una puntuación de seguridad.

«Guardar resultado como evidencia» recibe siempre el `DiagnosticResult` completo original, aunque haya un filtro activo. «Copiar resumen» conserva tipo, destino, fecha y resumen, y avisa que no incluye los campos técnicos. Android 13 o posterior recibe la marca de contenido sensible del portapapeles; esto no impide que otras aplicaciones lean contenido según los permisos y restricciones del sistema. La app no lee el portapapeles, abre enlaces ni comparte nada por red al copiar.

Una nueva observación reinicia el desplegable, la consulta y el mensaje de copia. No se arrastran a una lectura diferente. No cambia la resolución DNS, las consultas TCP/HEAD, validación TLS, permisos ni almacenamiento de evidencias. Las limitaciones de cada comprobación permanecen en la herramienta y en sus campos de método.

El diagnóstico pasa al `ViewModel` de sesión para conservarse cuando Android recrea la actividad, como ocurre en una rotación. No equivale a guardarlo: terminar el proceso o cerrar la sesión puede perderlo; hay que usar el guardado para conservar una evidencia. Al recibirse una observación en red/web la lista vuelve al inicio para presentar el resultado.

## Archivos

- `DiagnosticPresentation.kt`: búsqueda sobre los campos originales y texto del resumen.
- `DiagnosticResultCard.kt`: presentación, búsqueda, guardado y copia mediante callbacks.
- `DiagnosticsPanel.kt`: integración en red/web y escritura explícita del resumen al portapapeles.
- `DiagnosticPresentationTest.kt`: filtros, duplicados, orden, origen y alcance del resumen.
- `DiagnosticResultCardDeviceTest.kt`: dos pruebas aisladas de UI con fixtures explícitos, sin portapapeles real, evidencias o red.
- `DeviceWorkflowTest.kt`: expectativa final de inicio adaptada al identificador estable del botón desde 0.25.
- `DeckSessionModel.kt`, `MainActivity.kt`: retención en memoria del diagnóstico y posición de la lista al llegar una lectura.
- `DiagnosticNavigationDeviceTest.kt`: ensayo preparado con resolución real de la dirección loopback y recreación de actividad, sin evidencias guardadas ni destinos externos.

## Validación

- Compilación final de aplicación, instrumentación y pruebas Kotlin correcta en **2 min 30 s**; candidata anterior correcta en 3 min 14 s antes del cambio de retención y posición. Lint: cero errores/fatales, **24 avisos y 1 sugerencia**, como en 0.26. Se mantienen los avisos de API/Gradle.
- **216 pruebas JVM correctas**, con seis casos nuevos de búsqueda, conservación de duplicados/orden, filtro vacío o sin coincidencias y procedencia/alcance del resumen copiado.
- Aplicación 0.27.0-ciberdeck-dev, código 34, y APK de pruebas instaladas mediante actualización en Galaxy A15 SM-A155F Android 16: ambas `Success`. Versión comprobada mediante Android. Solicitud de arranque aceptada; sin `FATAL EXCEPTION` en el registro del proceso consultado. No demuestra la presentación bajo bloqueo ni la ausencia de otros defectos.
- **Tres pruebas Android nuevas pendientes**: dos casos aislados de tarjeta con fixtures explícitos y uno de navegación/recreación con resolución loopback. Están compiladas e instaladas, pero el A15 sigue bloqueado. Los casos de 0.25/0.26 y las regresiones relevantes también permanecen pendientes; no se atribuyen a esta revisión las pruebas físicas de versiones anteriores. No se generaron capturas de 0.27.
- OPPO no conectado ni actualizado. Firmware, modelo local, permisos y datos guardados no se modifican. La instalación preserva el paquete y sus datos privados.

APK final: **66.261.632 bytes**, SHA-256 `b7df00422136f1531379c65604c64428eb39ef82b8092692eb4ab5a02ea0d09e`. Copias y huellas de `dist/` actualizadas. Registros de instalación/arranque en `.private/android-validation/a15-027-install.json` y `a15-027-runtime-private.txt`, fuera de Git junto con APK y modelo. Sin release ni fusión en main.

Próximo paso: desbloquear el A15 para ejecutar `DiagnosticResultCardDeviceTest` y `DiagnosticNavigationDeviceTest`, comprobar los recorridos pendientes del kit y agente y revisar capturas reales. El ensayo loopback no guarda evidencias ni consulta destinos externos. Los fixtures de la tarjeta no se presentan como mediciones y no escriben el portapapeles ni almacén real.
