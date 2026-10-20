# CiberDeck 0.30 · Misión «¿Qué cambió?»

## Implementación

Acceso desde Inicio y desde Archivos y trabajos. La misión revisa hasta 100 copias recientes del filtro, verificadas por tamaño y SHA-256 mediante EvidenceStore. Agrupa por tipo y destino exactos y compara las dos últimas copias guardadas de cada grupo. No ejecuta nuevas mediciones, tráfico, un modelo de IA ni firmware.

Cada tarjeta muestra destino, tipo, fechas declaradas de observación y recuentos de campos iguales, modificados, añadidos y ausentes. La barra visual cuenta esos campos: no es un indicador de riesgo. El orden de guardado no acredita cronología ni identidad física. Se puede abrir la pareja en Comparar para verificar otra vez y consultar los valores originales.

El filtro empieza en el trabajo activo cuando existe; también permite revisar todas las copias. Informa de grupos sin pareja y copias no legibles, sin atribuirlas a ataques. No sobrescribe originales. El informe de misión guarda referencias, huellas y recuentos, sin copiar los valores originales de los campos. Se guarda mediante el mecanismo de evidencia y asociación al trabajo existente.

## Archivos

FieldInvestigation.kt, FieldInvestigationPanel.kt, MainActivity.kt, DeckHome.kt y DeckCatalog.kt. La comparación existente sigue disponible. Versión de desarrollo 0.30, código 37.

## Validación

- Compilación final de aplicación, instrumentación y tests Kotlin correcta en 2 min 12 s, después de una compilación inicial correcta en 2 min 43 s y de los ajustes de tamaño de informe y alcance de Comparar. Análisis sin errores/fatales, 24 avisos existentes.
- 234 pruebas JVM correctas, incluidos ocho casos de misión: todas las parejas, tipo/destino exactos, copia sin pareja, duplicados/derivados/corruptos, límite de 100, cancelación, omisión de valores originales e informes grandes legibles bajo 64 KiB.
- App 0.30 código 37 y APK de instrumentación instaladas mediante actualización en Galaxy A15 RF8X70M9B4A, ambas Success. El teléfono estaba desbloqueado antes de ejecutar la interfaz.
- Cinco pruebas Android correctas en 19,212 s: misión con copias existentes sin mutarlas, tres casos del catálogo y preparación/verificación de una pareja existente en Comparar. Registro privado `.private/android-validation/a15-030-mission-ui.txt`.
- La misión mostrada usó el filtro del trabajo activo: 21 copias compatibles, cuatro parejas, 106 campos diferentes y un grupo sin pareja. Son comparaciones textuales de archivos guardados, no 106 amenazas ni nuevas mediciones; los archivos pueden contener resultados históricos o de ensayos previos. El test calculó la referencia desde esos mismos archivos verificados y comprobó que el índice y el estado de trabajos permanecieran intactos.
- Captura auténtica revisada: `docs/screenshots/ciberdeck-investigation-030.png`. Está desplazada hasta la tarjeta de resultado; el título parcial superior responde al desplazamiento, no a recorte del diseño. No revela destinos ni valores originales.
- Guardado del informe de misión compilado y generación/privacidad/tamaño cubiertos por lógica. El botón de guardado y el salto desde una tarjeta concreta a Comparar no se han recorrido físicamente en esta ronda; no se atribuyen al test separado de Comparar. Reutilizan el guardado y navegación existentes.

APK: 66.361.635 bytes; SHA-256 `3b3cd0f717ee5706446df87ccabb352d135274296fe10a53c53511d82bbf65c3`. Modelos, firmware y permisos sin cambios. OPPO ausente en ADB en esta ronda.

## Pendiente

Captura de nuevas lecturas desde una misión, mapas de relaciones, correlación entre dispositivos y explicación guiada por IA. Esta primera misión compara pruebas ya guardadas. No es detección de intrusiones ni monitorización en segundo plano. USB OTG y actualización del OPPO requieren sus pruebas físicas respectivas.
