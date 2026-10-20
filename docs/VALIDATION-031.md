# CiberDeck 0.31 · Inicio sencillo y cambios sin saltos

## Cambios

Inicio presenta tres acciones principales: Revisar este móvil, Pedir ayuda al agente y Ver qué ha cambiado. Se conserva el robot, pero el encabezado ocupa menos. Las áreas, herramientas recientes, conexión y actividad se despliegan desde «Más herramientas y actividad». El menú y el kit siguen disponibles; no se eliminan herramientas ni evidencias. El estado de despliegue se conserva al recrear la actividad.

Al entrar en la investigación se revisan automáticamente las copias guardadas, sin un segundo botón de inicio. «Volver a buscar cambios» permite repetir. La vista muestra el resumen y ofrece guardarlo antes de la lista. Las parejas sin diferencias quedan ocultas por defecto con opción para verlas. «Ver qué cambió» despliega en la misma tarjeta fechas y hasta doce campos modificados; cada campo permite ver Antes/Después, abreviados a 240 caracteres. La comparación completa sigue disponible para todos los valores. Los campos no cambiados no se presentan como una garantía de seguridad. El método técnico permanece plegado.

## Validación

Compilación final de aplicación y pruebas correcta en 2 min 36 s, después de una candidata correcta en 2 min 33 s. Lint sin errores/fatales y 24 avisos existentes. 234 pruebas JVM correctas. App 0.31 código 38 y test APK instaladas por actualización en A15, ambas Success.

La primera ronda Android ejecutó ocho casos: seis correctos (catálogo, agente y cambios con despliegue en la misma tarjeta), y dos fallos del ensayo al buscar elementos de la lista que aún no estaban compuestos. Se ajustó exclusivamente el desplazamiento de las pruebas de Inicio y recientes para usar la lista antes de buscar el elemento. Compilación del test APK en 33 s. El seguimiento con el APK de pruebas recompilado pasó los dos casos en 9,794 s: tres acciones visibles, desplegar/plegar áreas y acceso a NFC reciente tras recrear la actividad. En total, ocho casos Android distintos correctos. Una ronda intermedia todavía usó el test antiguo de recientes, porque su edición coincidió con la compilación de instrumentación; no se cuenta como validación. Se recompiló el APK de pruebas una vez congeladas las fuentes (52 s) y se volvió a instalar antes de la ronda final. No hubo cambios de producción para resolver esos fallos del ensayo.

Capturas reales revisadas de Inicio y resumen: docs/screenshots/ciberdeck-home-031.png y ciberdeck-investigation-031.png. Las tres acciones aparecen completas en el A15 de la prueba. El resumen corresponde a las copias guardadas, no a nuevas mediciones ni amenazas. El nombre del trabajo visible es la revisión local del A15.

El guardado de resumen continúa usando el mecanismo existente y no se ha pulsado en esta ronda. No se ha cambiado firmware, modelo ni permisos. OPPO no conectado. No se atribuye validación en otros tamaños, orientaciones o escalas de letra a la prueba del A15.

APK: 66.368.106 bytes; SHA-256 c167adda9d85bbd5b0ebb4f7ebd433448656277da5054dffe1ce87d5175d4075. Registros privados en .private/android-validation/a15-031-simple-ui.txt y a15-031-simple-ui-followup.txt y a15-031-simple-ui-final.txt.

## Límites

Esta revisión simplifica Inicio y la investigación. Los demás formularios técnicos siguen disponibles y requieren revisiones de experiencia de uso posteriores. No hay nuevos sensores, tráfico ni funciones de IA. No se acredita facilidad de uso universal mediante pruebas automatizadas: hace falta probar con usuarios.
