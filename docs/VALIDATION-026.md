# CiberDeck 0.26 · Una tarea cada vez

## Implementación

El agente centra la vista en la tarea actual. Al preparar un plan se ocultan el formulario de petición, las tareas rápidas y el selector manual; quedan las entradas requeridas y la ejecución. Tras obtener resultados permanecen el resumen, los métodos desplegables, el guardado y «Preparar de nuevo». «Elegir otra tarea» abre el selector sin borrar ni ejecutar la sesión actual. Volver a cerrarlo conserva el plan y los resultados. Preparar otra tarea sustituye la sesión en memoria, como antes; se explica que hay que guardar los resultados que se quieran conservar.

Los controles de parada, errores, permisos e interacción manual permanecen disponibles. Los treinta adaptadores y sus límites no cambian. Esta presentación no añade comprensión general al modelo ni verifica sus conclusiones.

Inicio incorpora hasta tres accesos a herramientas abiertas recientemente, sin duplicados y con la más reciente primero. Son rutas de navegación, no resultados ni operaciones que se reanuden automáticamente. Se conservan en preferencias privadas de la aplicación y se pueden limpiar sin borrar evidencias. Las rutas raíz o desconocidas se descartan al restaurarlas.

USB, Bluetooth, NFC, recepción de archivos, DNS/TCP y web tienen una guía plegada «Cómo empezar»: requisitos, acción inicial y resultado esperado con su alcance. Abrirla no consulta hardware, inicia tráfico ni solicita permisos. Los estados y datos reales siguen en cada herramienta.

## Archivos

- `AgentPanel.kt`: selector de nueva tarea separado del plan y los resultados.
- `DeckCatalog.kt`, `CiberDeckHome.kt`, `MainActivity.kt`: recientes locales y accesos de inicio.
- `DeckToolGuide.kt`: instrucciones plegables por herramienta.
- `DeckCatalogTest.kt`: validación de orden, duplicados, límite y restauración de recientes.
- `AgentFocusDeviceTest.kt`: plan conservado al abrir/cerrar el selector y resultado real de conexión.
- `DeckRecentToolsDeviceTest.kt`: apertura desde recientes tras recrear la actividad y guía NFC; restaura las preferencias de navegación anteriores al ensayo.

## Validación

- Compilación de aplicación, instrumentación y pruebas Kotlin correcta en **2 min 32 s**. Lint sin errores/fatales, **24 avisos y 1 sugerencia**. Dos avisos nuevos recomiendan la extensión KTX para las escrituras de preferencias de navegación; no se ocultan. Las advertencias previas de API/Gradle y otras partes del proyecto permanecen.
- **210 pruebas JVM correctas**. Los tres casos nuevos verifican orden de recientes, exclusión de rutas desconocidas/raíz, límite y eliminación de duplicados al restaurar.
- Actualización de aplicación y APK de pruebas instalada mediante ADB en Galaxy A15 SM-A155F Android 16, ambas con `Success`. Versión 0.26.0-ciberdeck-dev, código 33, comprobada desde Android. Solicitud de arranque aceptada; sin `FATAL EXCEPTION` en el registro del proceso actual al consultarlo. Esto no sustituye una prueba de interacción.
- **Pruebas táctiles y capturas nuevas pendientes**: el A15 sigue bloqueado. Los dos ensayos de foco del agente y el ensayo de recientes/guía están compilados, pero no ejecutados. También siguen pendientes los tres recorridos de catálogo añadidos en 0.25. No se atribuyen a estas versiones los ensayos Android de 0.24.
- OPPO no conectado ni actualizado en esta revisión. No se cambia firmware, modelo local ni permisos. El registro privado de instalación está en `.private/android-validation/a15-026-install.json`; las APK y el modelo quedan fuera de Git.

APK final: **65.150.430 bytes**, SHA-256 `d24b18aa202263a26dec012d80821a6c1190e40a5642e5ec937cd735d1720297`. Copias de `dist/` actualizadas. Código en la rama de desarrollo y PR borrador; sin release ni fusión en main.

Próximo paso necesario: desbloquear el A15 para ejecutar `DeckCatalogDeviceTest`, `AgentFocusDeviceTest` y `DeckRecentToolsDeviceTest`, revisar las capturas reales y comprobar después las regresiones del agente que resulten afectadas.
