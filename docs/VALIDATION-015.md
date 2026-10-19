# CiberDeck 0.15 · Plan de revisión manual del móvil

## Uso y comportamiento

En **Mi móvil**, analiza o actualiza la lectura y abre **Plan de revisión**. Se reutilizan las observaciones reales de las API públicas ya presentes en CiberDeck. El plan usa reglas locales, no generación de IA, reputación ni un catálogo de CVE. Presenta primero los estados que las reglas existentes clasifican para revisar, después datos desconocidos e información restante. No suma una puntuación de seguridad.

Cada observación incluye su valor, límites, un siguiente paso y, donde existe, el acceso al ajuste relacionado. Abrir ese acceso no cambia la configuración; la actividad de Ajustes puede no existir en algún equipo y se conserva el manejo de error anterior. El texto del criterio local de parche ahora dice «al superar 180 días», consistente con su comparación existente (>180); no cambia el umbral ni se comprueba la última versión del fabricante.

«Anotar mi comprobación» permite guardar hasta 600 caracteres y marcar «He revisado esta observación». Es una declaración manual del usuario, no una corrección verificada por la app. La gráfica solo cuenta observaciones sin revisar, marcadas por el usuario o cuya lectura cambió. No implica ausencia de ataques, malware ni otros riesgos.

La clave local agrupa modelo declarado, sección y título; no identifica criptográficamente al dispositivo físico. La huella de la lectura incorpora valor, explicación, estado y acceso al ajuste. Una lectura posterior con los mismos datos conserva la marca; si cambian, se muestra como lectura cambiada y hay que revisarla antes de volver a marcar. El reloj de cada consulta no forma parte de la huella, pero sí los cambios de contexto, como la edad explicada del parche. Las notas anteriores siguen accesibles y no se reafirman automáticamente.

## Almacenamiento e informe

Archivo privado phone-review/index.json con escritura atómica, máximo 128 observaciones, 600 caracteres por nota y 512 KiB. Un archivo no interpretable no se reemplaza automáticamente y no bloquea las demás secciones. Las claves se derivan del modelo declarado, sección y título; se guardan notas, huellas y marcas, sin recopilar números de serie ni credenciales.

«Guardar plan como evidencia» usa el guardado habitual y el trabajo activo. Incluye fecha de la lectura Android, valores observados, explicación, regla, marca manual, fecha/nota y huellas actual y de la anotación. Las notas del informe se abrevian a 200 caracteres o menos si es necesario para respetar 64 KiB; las notas completas permanecen en el plan. El reporte se identifica como revisión manual y puede compararse con otra lectura del mismo tipo/destino. No modifica originales ni ajustes ni envía tráfico.

## Archivos

PhoneReview.kt: reglas, huellas, estados, formato e informe; PhoneReviewStore.kt: persistencia; PhoneReviewPanel.kt: gráfica, filtro, notas y accesos. PhoneAuditPanel.kt incorpora la sección manteniendo las anteriores. PhoneAssessment.kt aclara el texto del umbral existente. PhoneReviewPolicyTest, PhoneReviewStoreDeviceTest y PhoneReviewUiDeviceTest cubren lógica, almacenamiento/lectura física e interfaz preparada. El gesto del test previo de recursos se hace tras desplazar hasta su botón.

## Validación

- Compilación de app y APK de pruebas y lint correctos. Se corrigió un error de sintaxis en una prueba; permanecen advertencias previas de obsolescencia.
- 129 pruebas JVM correctas: ocho nuevas de prioridad, vinculación de marcas, notas sin marcar, formato/Unicode, límites, duplicados e informe.
- Dos pruebas físicas sin interfaz correctas en A15: persistencia real con AtomicFile en una carpeta de fixture aislada, e informe de observaciones reales obtenido de Android. Se comprobaron cambios de huella, desmarcado, preservación del índice tras validación fallida y archivo inválido no sobrescrito.
- La lectura física produjo 11 observaciones y ninguna marca automática. El informe real se conserva localmente bajo almacenamiento privado, excluido de Git; las marcas de prueba se hicieron solo en la carpeta de caché aislada, eliminada al terminar.
- APK 0.15.0-ciberdeck-dev, código 20 instalada conservando datos. SHA-256: `e3ffea028f5ade6a308a79dea52ca85fad9c142592893ac9cba40110031b25b3`.
- El A15 sigue bloqueado: la nueva sección, anotaciones desde botones, apertura de Ajustes y guardado/exportación desde la interfaz no se declaran validados físicamente. No hay captura nueva; la prueba UI está preparada. Los recorridos visuales pendientes de 0.13/0.14 también siguen pendientes de desbloqueo.

## Referencias y límites

Los accesos siguen las [acciones públicas de Ajustes Android](https://developer.android.com/reference/android/provider/Settings#ACTION_SECURITY_SETTINGS). El parche procede del [campo SECURITY_PATCH](https://developer.android.com/reference/android/os/Build.VERSION#SECURITY_PATCH); el plan no descubre parches o vulnerabilidades por ese dato. Consultadas el 19 de octubre de 2026.

No se modifica firmware ESP, no se añaden permisos ni servicios, no se descarga ningún modelo y no se valida el enlace OTG ni el OPPO en esta revisión. La IA de 0.14 conserva sus límites de calidad documentados. Avances en la rama/PR existentes, sin fusionar main ni release.
