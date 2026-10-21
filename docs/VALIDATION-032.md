# CiberDeck 0.32 · Revisión guiada del móvil

## Recorrido

Inicio → Revisar este móvil mantiene la lectura real de Android. El resultado usa el modo guiado por defecto, sin presentar las cinco secciones técnicas de golpe. Primero muestra el recuento de ajustes para revisar o datos no disponibles, la fecha y hasta tres observaciones prioritarias. REVIEW se ordena antes de UNKNOWN; INFO y OK no se convierten en alarmas. Sin prioridades se ofrecen observaciones informativas, sin declarar el equipo seguro.

Cada observación abre «Qué hacer ahora» en la misma herramienta. Usa la recomendación local de PhoneReviewPolicy y el ajuste Android existente, con nombre de acción según su destino. Volver de los ajustes no certifica nada: «Comprobar de nuevo» obtiene una nueva lectura. La interfaz compara el valor con el de entrada y presenta cambio o ausencia de cambio con fecha. El usuario puede volver al resumen. No se alteran ajustes ni se marcan comprobaciones manuales automáticamente.

«Todos los datos y herramientas» conserva Seguridad, Conexión, Recursos, Hardware y Plan de revisión. Al volver al modo guiado se detiene el muestreo en vivo, igual que al abandonar Recursos. Se conserva el guardado de chequeos y anotaciones anterior.

## Archivos

PhoneBriefPolicy.kt, GuidedPhonePanel.kt, PhoneUiModel.kt y PhoneAuditPanel.kt. El modelo conserva modo, sección, observación y valor de referencia con SavedStateHandle, sin retener una Activity. Ajuste menor en CiberDeckHome.kt para quitar una condición redundante del texto del botón. Código de versión 39, 0.32.0-ciberdeck-dev. Se adaptan los ensayos antiguos para abrir las herramientas técnicas explícitamente y se añade un recorrido guiado.

## Validación

Primera candidata compilada correctamente en 2 min 56 s, sin errores/fatales de lint, con 24 avisos existentes. 237 pruebas JVM correctas. Instalada por actualización en OPPO CPH2743, Android 16/API 36.

La primera ronda Android ejecutó siete casos: seis correctos (recorrido guiado con nueva lectura, catálogo y foco del agente) y uno fallido al recrear la actividad en modo técnico. El diagnóstico privado confirmó que se había vuelto al modo guiado. No se atribuye ese fallo a un test antiguo: se corrigió la conservación de estado de producción mediante PhoneUiModel. El ensayo aislado de diagnóstico fallido usó todavía la candidata anterior. La candidata final compiló en 2 min 22 s con cero errores/fatales de lint y 24 avisos existentes. Se repitieron las 237 pruebas JVM, todas correctas. Se actualizaron otra vez app y test APK en el OPPO, ambas Success.

La ronda final pasó los siete casos Android en 39,542 s: recorrido guiado con selección conservada tras recrear, nueva lectura y comparación de valor, regreso al resumen, acceso a Recursos y detención del muestreo al volver al modo guiado; diagnóstico anterior conservado tras recrear y gráfica RAM real; tres casos de catálogo/navegación y dos de foco del agente. Los fallos de la candidata anterior no se cuentan como validaciones. La prueba guiada verificó que no cambiasen el índice de evidencias ni el estado de trabajos.

En este OPPO no había estados REVIEW o UNKNOWN según las reglas actuales: se presentó «Sin ajustes destacados» y observaciones informativas. Esto no equivale a ausencia de amenazas. El recorrido se hizo con el bloqueo configurado; la lectura posterior mostró que su valor no había cambiado. No se alteraron ajustes para forzar una demostración.

No se considera probado un ajuste externo por haber presentado su botón. Capturas finales auténticas y revisadas: docs/screenshots/ciberdeck-guided-summary-032.png y ciberdeck-guided-step-032.png. El panel de límites está plegado en las capturas; sigue disponible. No se pulsó el ajuste de seguridad externo ni Guardar chequeo en la ronda guiada. El test del diagnóstico antiguo creó y retiró únicamente su evidencia privada de ensayo sin asociarla a un trabajo.

APK final: 65.232.350 bytes, SHA-256 ab89d53e5ee6dc660dc25029694cc91d9bfdbe35e1b7430d312be4782c9037c3. Registros privados: oppo-032-guided-ui.txt (candidata), oppo-032-resources-followup.txt (diagnóstico anterior) y oppo-032-guided-final.txt (siete casos correctos). Firmware, modelos y permisos conservados; A15 no conectado en esta ronda.

## Límites

Primer recorrido guiado aplicado al diagnóstico del móvil; el agente y otras herramientas mantienen su funcionamiento previo. Es una mejora del recorrido y de la presentación, no detección nueva de amenazas. Las prioridades proceden de reglas locales existentes y los datos de APIs públicas Android. No hay escaneo de malware ni garantía de seguridad. El botón de ajustes usa acciones Android públicas que pueden no estar disponibles en todos los fabricantes.
