# CiberDeck 0.17 · Agente local y selección guiada

19 de octubre de 2026. Desarrollo sobre la rama y PR existentes. Paquete, datos y funciones anteriores conservados. No se crean permisos, servicios nuevos, descargas de modelos ni cambios de firmware.

## Agente: contrato de esta primera versión

En IA, el selector Analista / Agente conserva el analista previo y añade una vista de planificación. Requiere importar y cargar el modelo local existente. El usuario escribe una petición; un modo separado del motor solicita un plan JSON, sin adjuntar evidencias ni ejecutar herramientas durante la generación. La respuesta se valida antes de ofrecer Ejecutar este plan.

Herramientas disponibles, sin argumentos:

- phone: instantánea de las API públicas Android mediante PhoneAudit.
- connection: estado local de red mediante ConnectionDiagnostic. No realiza DNS, HTTP, escaneo ni otra prueba de tráfico.
- compare: dos diagnósticos locales compatibles, seleccionados dentro del trabajo activo capturado al ejecutar o de las evidencias locales si no hay trabajo. Lee hasta 100 candidatos, verifica copias y vuelve a comprobar la pareja antes de calcular.

La gramática admite un único objeto steps con uno a tres identificadores conocidos y distintos; rechaza campos adicionales, texto externo, identificadores desconocidos, duplicados y planes vacíos. Solo se permite opcionalmente una cerca de código json completa. No se interpreta shell, código, rutas, direcciones ni instrucciones de otros módulos. El motor conserva tools vacío y automaticToolCalling=false. La app despacha únicamente enums validados mediante código propio.

Un plan válido puede no corresponder a la intención del usuario. Se muestra completo y no se ejecuta automáticamente. El usuario pulsa una vez para ejecutar sus pasos de lectura local; un error detiene los posteriores. Se muestra cada lectura real o error y el recuento de pasos completados. Detener o salir cancela la operación; una lectura síncrona ya iniciada puede terminar antes de atender la cancelación. Sin reintentos ni ejecución autónoma en segundo plano.

Guardar ejecución usa el guardado de diagnósticos existente y su asociación al trabajo activo al pulsar Guardar. Incluye petición, hash del modelo planificador, alcance de copias, plan, fechas, estados y datos de herramientas. El texto de la IA no se convierte en un resultado observado. El informe se limita a 80 campos y 1000 caracteres por valor, reduciéndolos si hace falta para quedar bajo 64 KiB. La sesión es temporal hasta guardarla; no se promete recuperación tras muerte de proceso. No hay exportación automática ni envío a nube.

## Selección guiada para Comparar

Preparar pareja compatible revisa hasta 100 entradas recientes del filtro por orden del índice local. Omite IDs duplicados, copias no verificables, tipos/destinos vacíos, comparaciones derivadas y diagnósticos con más de 256 campos. Agrupa por coincidencia exacta de tipo y destino. Selecciona la lectura guardada más recientemente que tenga otra compatible; A es su siguiente copia anterior y B la más reciente. El guardado no acredita el orden cronológico de las observaciones ni la identidad física del destino.

La preparación no modifica las selecciones si no encuentra pareja. Permite cambiar el alcance o elegir manualmente otras lecturas. Los resúmenes y método se despliegan; los selectores manuales se ocultan al preparar una pareja. Verificar y comparar vuelve a comprobar las copias para detectar cambios posteriores a la selección.

## Archivos principales

AgentPolicy.kt: gramática, plan tipado, ejecutor secuencial e informe. AgentTools.kt: adaptadores de las tres herramientas existentes. AgentPanel.kt y DeckSessionModel.kt: petición, plan, ejecución y estados. LocalAiService/Client/Panel: mensaje PLAN con instrucciones separadas y selector de modo. ComparisonSuggestion.kt y ComparisonPanel.kt: selección limitada y vista guiada. Pruebas JVM y Android específicas, README y PNG de la comparación. Versión 0.17.0-ciberdeck-dev, código 23.

## Pruebas y resultados

- Compilación de app y APK de tests, compilación de pruebas y lintDebug correctos; permanecen advertencias de obsolescencia.
- 145 pruebas JVM correctas mediante JUnitCore local. Dieciséis nuevas en esta iteración: ocho de selección compatible/orden/duplicados/límites/cancelación y ocho de gramática, límites, ejecución, fallo, cancelación e informe. El límite de sockets del ejecutor Gradle local sigue documentado.
- AgentDeviceTest: tres pruebas físicas correctas en A15, Android 16/API 36. El validador Android acepta el plan conocido y rechaza órdenes desconocidas/duplicadas; el ejecutor lee móvil y conexión reales sin guardar evidencias; compare lee referencias reales del trabajo y devuelve una comparación verificada sin escribir archivos.
- AgentInferenceDeviceTest: prueba física correcta con el Qwen3 0.6B mixed INT4 ya importado, SHA-256 7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1. Ante «Revisa mi móvil y el estado de su conexión», devolvió steps phone, connection en 10.989 ms. El validador aceptó ambos y el ejecutor produjo diagnósticos reales phone y connection. El índice de evidencias permaneció intacto. Se probó sin depender de una pantalla desbloqueada; no se simula una aprobación desde la interfaz.
- Durante la misma iteración pasaron los dos recorridos UI de ComparisonDeviceTest (pareja guiada real y selección manual con recreación, gráfica y guardado vinculado al trabajo) y el recorrido de SavedAiContextUiDeviceTest. Esos paneles no cambiaron después de dicha prueba. Los PNG de comparación proceden de Compose en el A15 y fueron revisados visualmente.

Se detectaron y corrigieron tres problemas: el regex inicialmente aceptado por JVM necesitaba escapar los cierres para el motor Android; Qwen omitió phone en su primera propuesta, por lo que se aclararon instrucciones y ejemplos y se comprobó de nuevo con inferencia real; una prueba coroutine devolvía un objeto en vez de Unit y se corrigió su firma JUnit. Una petición probada no demuestra precisión general del planificador.

La prueba UI completa del agente falló inicialmente en el validador, antes de ejecutar herramientas. Tras corregirlo se validaron el parser y el flujo IA → herramientas con las pruebas físicas sin interfaz anteriores. El A15 volvió a bloquearse: el recorrido final de botones, guardado de la ejecución, botón Detener y captura del agente permanecen pendientes de repetir. No se presenta una captura de diseño como prueba física. Los registros, respuestas e informes reales se mantienen privados, excluidos de Git.

APK instalada conservando datos: 65.870.964 bytes. SHA-256: 593a753b0ae5794261f0cb026236ed4c3a7a68085615935b6c2f1044475c50b5. Copia local en dist/CiberDeck-debug.apk, sin release.

## Límites y siguiente paso

El agente aún no controla USB, NFC, BLE, RF, llamadas, transferencia, ajustes, archivos arbitrarios ni herramientas de red activa. No implementa un bucle abierto para hacer cualquier petición. Mantiene las herramientas anteriores accesibles manualmente. La calidad del modelo pequeño es experimental, sin garantía de detectar todos los pasos o entender cualquier petición. Ninguna lectura o informe prueba ausencia de ataques o malware.

El OPPO y el ESP no se validan en esta iteración. La conexión Android–ESP por OTG sigue pendiente, igual que las pruebas de anotaciones, Ajustes y exportación descritas anteriormente. Próximo paso: repetir el recorrido UI del agente con el A15 desbloqueado, comprobar guardado/asociación e incorporar nuevas herramientas de una en una con contrato y pruebas. Avances únicamente en la rama/PR existentes, sin fusionar main ni crear release.
