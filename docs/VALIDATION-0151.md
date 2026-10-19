# CiberDeck 0.15.1 · Pruebas físicas de los recorridos pendientes

19 de octubre de 2026. Galaxy A15 SM-A155F, Android 16/API 36, ADB autorizado y pantalla desbloqueada por el propietario. Actualización instalada conservando los datos. No se ha instalado en el OPPO ni modificado firmware ESP.

## Corrección

Al elegir dos diagnósticos, recrear la actividad y volver a Comparar desde el catálogo, ambos selectores perdían sus IDs y el botón quedaba deshabilitado. La selección dependía del estado local de una composición dentro de la lista. Ahora ComparisonSelectionModel, creado con el propietario de actividad, mantiene únicamente los IDs mediante SavedStateHandle y StateFlow. Los contenidos no se guardan ahí: cada comparación sigue verificando las dos copias en EvidenceStore. Se añaden claves explícitas para los paneles Comparar, IA y Mi móvil en la lista dinámica.

La prueba de regresión falló antes de la corrección y pasa después. Se ha probado recreación de actividad y navegación; no se ha provocado una muerte completa del proceso ni un reinicio del teléfono. El resultado calculado no se restaura como si siguiera verificado: se vuelve a calcular con el botón.

## Archivos modificados

- ComparisonPanel.kt y MainActivity.kt: selección de actividad y claves de panel.
- app/build.gradle.kts: versión 0.15.1-ciberdeck-dev, código 21.
- ComparisonDeviceTest: prueba de recreación existente y volcado privado de semántica si falla.
- SavedAiContextUiDeviceTest: limpiar búsqueda anterior, elegir fuente real y capturar pantalla.
- PhoneReviewUiDeviceTest: guardar el plan desde el botón y comprobar su asociación al trabajo activo.
- README y tres PNG reales en docs/screenshots: comparación, plan e IA con fuente.

## Resultados

Compilación de la app y APK de pruebas, compilación de tests y lintDebug correctos. Permanecen advertencias de obsolescencia; no se afirma ausencia de advertencias. 129 pruebas JVM correctas mediante el lanzador JUnit local, porque el ejecutor Gradle de este PC tiene la limitación de sockets documentada anteriormente.

Tres pruebas instrumentadas de interfaz correctas en 16,683 segundos:

1. ComparisonDeviceTest: dos diagnósticos Android reales del trabajo activo, selección A/B, recreación de actividad, navegación de vuelta, comparación SHA-256, recuentos contra el resultado calculado, captura y guardado vinculado al trabajo. Resultado observado: 19 campos sin cambios, 2 modificados, 0 añadidos y 0 ausentes. No se interpreta como incidente ni puntuación de seguridad.
2. SavedAiContextUiDeviceTest: buscar una evidencia real por su hash, enviarla como fuente, comprobar su ID y que no hay modelo cargado ni generación, editar con la pregunta rápida «Preparar informe», retirar contexto y verificar que el original sigue íntegro. No se ha probado generar y guardar una respuesta desde los botones en este recorrido; la inferencia física previa y sus límites de calidad están en VALIDATION-014.md.
3. PhoneReviewUiDeviceTest: analizar el móvil, abrir el plan, mostrar la gráfica, guardar como evidencia de tipo phone-review y comprobar su referencia en el trabajo activo. Las 11 observaciones permanecen sin marcar; no se ha afirmado una comprobación manual del usuario.

Las capturas PNG proceden de la raíz Compose de la app instalada, no de un diseño ni datos simulados. Se revisaron visualmente antes de añadirlas. Los informes reales, volcados y registros completos quedan privados y excluidos de Git.

APK de desarrollo: 65.796.012 bytes. SHA-256: `67f455d7841cc24674f7f17ff19c6324010f791eab11e419471b5e610f92f087`. Copia local en dist/CiberDeck-debug.apk; no se publica una release.

## Límites y siguiente paso

Quedan pendientes las anotaciones manuales desde la interfaz, apertura de Ajustes, exportación con el selector Android y generación/guardado de IA desde botones. No se cambió la seguridad de bloqueo, no se añadieron permisos ni se realizaron llamadas o pruebas RF. El enlace directo Android–ESP por OTG sigue pendiente; los dispositivos conectados por separado al PC no lo validan. Sin cambios en firmware ni respaldo original. Próximo paso: validar esos recorridos y el enlace OTG con el cableado físico adecuado, conservando el alcance autorizado.

Código y capturas se guardan en la rama de desarrollo y el PR existentes; sin fusionar main ni crear release. Los documentos anteriores conservan el estado histórico de sus pruebas.
