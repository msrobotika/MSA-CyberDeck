# CiberDeck 0.11 · Trabajos de auditoría

## Función implementada

En Kit o el menú desplegable, abre **Trabajos**. Crea un trabajo con nombre, alcance y objetivos; puedes añadir notas, cambiar de trabajo, desactivarlo, archivarlo y recuperarlo. El indicador de trabajo activo permite volver a él desde las herramientas. El borrador de informe anterior permanece disponible y no se migran ni eliminan sus datos.

Los diagnósticos guardados mediante la acción común se asocian al trabajo activo en el momento de pulsar Guardar. Los archivos importados, recibidos y registros USB se incorporan explícitamente desde las evidencias existentes. Si la asociación falla, la copia se conserva y se avisa para asociarla después. Cambiar de trabajo mientras termina una operación no cambia el destino elegido al guardar.

Cada referencia fija identificador, SHA-256 y tamaño originales. El informe distingue coincidencia con el índice de comprobación del archivo: vuelve a verificar las copias, marca las alteradas como NO_VERIFICADA y omite contenidos ausentes o incompatibles. Incluye un resumen de hasta veinte diagnósticos verificados recientes, alcance, objetivos y notas. No emite un dictamen automático de seguridad. Desasociar o archivar no borra las evidencias.

Almacenamiento privado con escritura atómica: máximo 50 trabajos, 500 referencias por trabajo, 2.000 referencias totales e índice de 2 MiB. Los diagnósticos incrustables del informe tienen un límite conjunto de 1 MiB. Un índice inválido no se sobrescribe automáticamente. El reloj y las declaraciones pertenecen al teléfono/usuario: no constituye una cadena de custodia certificada.

## Validación realizada en Galaxy A15

- Compilación de APK y APK de pruebas, compilación de pruebas JVM y lint: correctos. Persisten advertencias previas de obsolescencia; no se afirma ausencia de advertencias.
- 115 pruebas JVM correctas; seis nuevas de codificación, referencias fijadas, duplicados, conflictos, formato y estado activo.
- Primera tanda física: cuatro pruebas correctas de almacenamiento de trabajos, flujo real de interfaz, magnetómetro y pantalla de IA con modelo importado.
- Tras los últimos ajustes: tres pruebas físicas correctas de almacenamiento, flujo de trabajos y magnetómetro. Se comprobó persistencia al recrear la actividad, asociación del diagnóstico real del A15 y generación del informe; con datos aislados se verificaron archivos alterados/ausentes, archivo y recuperación e índice inválido preservado.
- Capturas PNG obtenidas de Compose ejecutándose en el A15: ciberdeck-work.png, ciberdeck-magnetic.png, ciberdeck-magnetic-graph.png y ciberdeck-local-ai.png. No son diseños simulados. La gráfica muestra las muestras recogidas durante la prueba, sin inventar un historial más largo.
- Informe real del A15 conservado únicamente en almacenamiento privado/local; no se incluye en Git porque contiene postura del dispositivo.
- Instalación mediante actualización conservando paquete y datos. Versión 0.11.0-ciberdeck-dev, código 16. APK SHA-256: `2812ebc1abf78387a68b933622d7e9238dff10e3be775fb2626d2dfba598b410`.

## Archivos principales

AuditWork.kt, AuditWorkStore.kt y AuditWorkPanel.kt contienen políticas, persistencia, informes e interfaz. MainActivity.kt integra guardado, navegación y exportación; DeckHome.kt incorpora la herramienta en el catálogo. AuditWorkTest, AuditWorkStoreDeviceTest y AuditWorkFlowDeviceTest comprueban lógica y flujo físico. MagneticDeviceTest incorpora la captura completa de la gráfica.

## Límites y continuidad

El modelo Qwen3 importado sigue disponible: su inferencia local real se validó en 0.10.1; la prueba de esta revisión comprueba presentación y preservación del modelo, no repite una evaluación de calidad. Es experimental. El magnetómetro mide campo magnético, no RF ni ataques.

OPPO desconectado: no actualizado en esta revisión. ESP no detectado: firmware 0.3.1 compilado, sin instalar. El puerto Samsung no se utiliza como ESP. La conexión directa Android–ESP por OTG y una llamada física de validación siguen pendientes. No se modificó firmware ni datos personales. Avances en la rama de desarrollo y PR existente, sin fusión en main ni release.
