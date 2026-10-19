# CiberDeck 0.13 · Comparar diagnósticos guardados

## Uso

Abre **Comparar lecturas** desde Auditoría en el menú/Kit, desde Evidencias o desde el trabajo activo. Elige A (referencia) y B (otra lectura), y pulsa **Verificar y comparar**. Se leen dos copias distintas guardadas en el móvil, de hasta 64 KiB cada una; antes se comprueban pertenencia al índice, tamaño y SHA-256. Tipo y destino declarados deben coincidir exactamente. Si existe un trabajo activo, el selector se limita inicialmente a sus referencias fijadas; puede ampliarse a todos los diagnósticos compatibles. Las comparaciones generadas no se incluyen como fuente del selector.

La barra representa cantidades reales de etiquetas: modificadas, añadidas, ausentes y sin cambios. Se agrupan todos los valores de una etiqueta, conservando sus duplicados e ignorando el orden dentro del grupo. No se normalizan direcciones, cantidades ni unidades. Las fechas y resúmenes se muestran aparte. No se supone que B sea cronológicamente posterior; A es la referencia elegida.

El filtro «Solo diferencias», la búsqueda por etiqueta y los detalles desplegables evitan mostrar todos los valores de golpe. Se muestran hasta 30 etiquetas; la búsqueda permite acceder a las restantes. Los selectores conservan las elecciones al recrear la actividad, pero el resultado es temporal y hay que volver a verificarlo.

**Guardar comparación** crea un diagnóstico nuevo mediante el guardado habitual, asociado al trabajo activo en ese momento. Conserva ID y SHA-256 de ambas fuentes, sus fechas y resúmenes, y hasta 50 diferencias con valores abreviados; el número puede reducirse para respetar 64 KiB. El informe declara cuántas diferencias se omiten. El contenido completo permanece en los originales. No se borran ni modifican fuentes ni se inicia tráfico de red.

## Límites

Son diferencias textuales entre observaciones. Una IP o nombre coincidente no acredita identidad física; cambios de RAM, batería, latencia o formato no prueban un incidente. «Ausente» significa etiqueta no presente en B. La fecha de verificación es puntual y procede del reloj del móvil. No es análisis de malware ni certificación. No se genera ninguna diferencia ni dato de ejemplo al arrancar la app.

## Archivos

DiagnosticComparison.kt: agrupación, comparación, referencias y reporte acotado. ComparisonPanel.kt: selección, verificación, gráfica, filtros y guardado. MainActivity.kt, DeckHome.kt y AuditWorkPanel.kt: entradas de navegación y asociación existente. ComparisonPolicyTest, ComparisonStoreDeviceTest y ComparisonDeviceTest: lógica, integridad e interfaz.

## Validación

- APK y APK de pruebas compiladas; lint sin errores (persisten avisos anteriores de obsolescencia).
- 121 pruebas JVM correctas, incluidas seis nuevas: clasificación de campos, etiquetas repetidas, destinos incompatibles, fechas/resúmenes separados, campos vacíos y reporte acotado con caracteres Unicode/control.
- APK 0.13.0-ciberdeck-dev, código 18 instalada en Galaxy A15 mediante actualización conservando datos. SHA-256: `2ca53f46146ac047d4a1a246087d890c763792ad6daa40bd3b4a2e824c4667fc`.
- La prueba física de almacenamiento con archivos aislados pasó: rechaza la misma copia dos veces, alteración de un byte conservando tamaño y eliminación de una fuente.
- Dos pruebas físicas sin interfaz correctas en A15: archivos aislados y comparación de dos diagnósticos reales del propio teléfono ya guardados. Se generó un informe privado con referencias SHA-256 y fecha de verificación; las fuentes permanecieron verificables. Resultado observado: 2 campos diferentes de 21; comparación textual, no detección de ataques. El JSON con postura del móvil se conserva localmente y no se incluye en Git.
- La prueba física de interfaz no pudo completarse: Android informó deviceLocked=1 y Compose no presentó su jerarquía al test. Quedan pendientes selección en los desplegables, restauración de selección, gráfica, guardado desde su botón y captura nueva. No se presenta una imagen de diseño como si fuese una captura real. La prueba ComparisonDeviceTest queda preparada para repetirla con el A15 desbloqueado.

OPPO y enlace OTG móvil–ESP no se validan en esta revisión. No se modifica firmware ni se amplían permisos Android. Los avances se guardan en la rama y PR existentes; sin fusión en main ni release.
