# CiberDeck 0.12 · Integridad visual y continuación del trabajo

## Cambios

En **Trabajos**, cada ficha incorpora «Comprobar copias del trabajo». Vuelve a leer las copias privadas y compara tamaño y SHA-256 con sus referencias fijadas. La gráfica representa cantidades reales de archivos: verificados, no verificables y referencias sin coincidencia en el índice. No representa un nivel de riesgo del móvil. No verificable puede significar copia ausente, cambiada, inaccesible o índice cambiado durante la operación; no se etiqueta automáticamente como manipulación maliciosa.

Se muestra cuándo terminó la comprobación. El resultado solo vive en memoria, se reinicia al cambiar la ficha y puede quedar desactualizado si después cambia un archivo. El informe exportado conserva su propia verificación al generarse. Detener, salir o enviar la actividad a segundo plano solicita cancelación y descarta el resultado completo: se termina después del archivo que se esté leyendo. No se escriben ni borran evidencias ni se envía tráfico.

«Continuar la revisión» ofrece accesos a Mi móvil, auditoría TCP de un equipo, inspector APK y USB. Solo están habilitados para el trabajo activo y abierto. Abrir la herramienta no inicia pruebas, escaneos ni conexiones. Los diagnósticos que el usuario guarda mantienen su asociación al trabajo activo.

## Archivos

WorkIntegrity.kt clasifica las comprobaciones contra el índice y las copias verificadas; WorkIntegrityPanel.kt presenta la acción, fecha, gráfica y cancelación. AuditWorkPanel.kt añade panel y accesos; MainActivity.kt conecta la navegación. AuditWorkStoreDeviceTest comprueba los casos de integridad con archivos de prueba aislados y cancelación. WorkIntegrityDeviceTest revisa las copias reales ya guardadas en el A15 y la navegación sin alterar el trabajo.

## Validación

- Compilación de APK y APK de pruebas, compilación de tests y lint correctos; permanecen avisos previos de obsolescencia.
- 115 pruebas JVM correctas.
- Dos pruebas físicas correctas en Galaxy A15 Android 16: integridad con archivos aislados (copia válida, alterada, borrada y cancelación sin resultado parcial) e interfaz sobre las referencias reales del trabajo activo. La interfaz mostró los recuentos comprobados y abrió Mi móvil desde el acceso directo manteniendo el trabajo activo.
- Tras compactar las explicaciones en un desplegable, se repitió la prueba física de interfaz: correcta en horizontal.
- Captura real de la app ejecutada en A15: docs/screenshots/ciberdeck-work-integrity.png. Los archivos alterados de prueba se crearon solo en una carpeta de caché aislada, eliminada al acabar; no se modificaron las copias del usuario.
- APK 0.12.0-ciberdeck-dev, código 17 instalada mediante actualización conservando datos. SHA-256: `8a9854aa6b3f8582acf4ad156b377ed41057815d3f178e3a54ffb2504ba308e3`.
- No se ha probado una interrupción durante un archivo de gran tamaño ni el bloqueo físico en mitad de la verificación: la cancelación automática por ciclo de vida está implementada, pero esos escenarios no se declaran validados.

## Continuidad y límites

Mismo paquete, actualización conservando datos; no se flashea el ESP. El OPPO no está conectado ni actualizado. USB directo Android–ESP por OTG sigue pendiente; abrir su pantalla no acredita conexión. El modelo Qwen3 importado permanece en almacenamiento privado; este cambio no modifica el motor ni amplía las capacidades de la IA. Las copias verificadas no acreditan procedencia, ausencia de malware ni una cadena de custodia certificada.
