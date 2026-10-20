# CiberDeck 0.25 · Elegir una tarea

## Cambios de uso

El inicio tiene una acción principal: revisar este móvil mediante `PhoneAudit.read`. La revisión anterior conserva fecha y ajustes por revisar. El agente sigue accesible con «Pedir una tarea al agente», sin ejecutar planes automáticamente al abrirlo. El robot conserva su animación y permite pausarla.

El acceso a las herramientas se organiza en seis áreas compartidas entre inicio y kit:

- Móvil y protección.
- Red e Internet.
- Bluetooth y NFC.
- ESP32 y sensores.
- Archivos y trabajos.
- Agente e IA.

Cada área contiene herramientas existentes; no añade capacidades de hardware. «Todas» muestra el catálogo completo. Buscar usa títulos, descripciones y términos de tarea como «transferir», «magnetómetro» o «puertos». Ignora mayúsculas y acentos, exige todas las palabras introducidas y busca en todo el catálogo aunque se haya elegido un área. Al borrar la consulta vuelve a esa área; «Por áreas» restaura el selector. La vuelta desde una herramienta conserva el área y la búsqueda. Los botones de navegación principales y el menú lateral siguen disponibles.

Las tarjetas pasan a una columna con escala de fuente superior a 1,3. Esto es una adaptación implementada, pendiente de inspección visual en diferentes escalas y tamaños. La clasificación no es una puntuación de seguridad ni una nueva lectura. Las limitaciones de NFC, USB OTG, RF y de la IA local se mantienen.

## Archivos

- `DeckCatalog.kt`: áreas, cobertura de rutas y búsqueda normalizada por tareas.
- `CiberDeckHome.kt`: inicio y tarjetas compartidas con el kit.
- `MainActivity.kt`, `DeckHome.kt`: selección de área, búsqueda, recuperación y navegación.
- `DeckCatalogTest.kt`: cobertura sin duplicados, búsqueda y recuperación lógica.
- `DeckCatalogDeviceTest.kt`: recorridos de inicio, áreas, vuelta, búsqueda global y acción real del móvil; sin nuevas evidencias guardadas ni tráfico externo.
- `DeckTestNavigation.kt`: acceso explícito a todas las herramientas para los ensayos existentes.

## Validación

- Compilación de aplicación, instrumentación y pruebas Kotlin correcta en 2 min 29 s. Se corrigió un import ausente de `Alignment` encontrado en el primer intento. Lint: cero errores/fatales y 22 avisos previos; se mantienen las advertencias de Gradle/API.
- **207 pruebas JVM correctas**, incluidos siete casos nuevos del catálogo: cobertura completa, áreas sin duplicados, búsqueda normalizada y por varias palabras, búsqueda global, recuperación de área y entradas sin coincidencias.
- Instalación mediante actualización en Galaxy A15 SM-A155F, Android 16: `Success`. Comprobada versión 0.25.0-ciberdeck-dev, código 32, mediante Android. Solicitud de arranque aceptada (`Status: ok`); no demuestra la presentación visual bajo bloqueo.
- **Pruebas de pantalla y capturas nuevas pendientes**: el A15 continúa bloqueado. Los tres recorridos nuevos están implementados y compilados, pero no se cuentan como pruebas superadas. Tampoco se han repetido las pruebas Android históricas. Las expectativas de inicio de esas pruebas se adaptan al identificador estable `home-phone` para no depender del texto del botón.
- OPPO no conectado; no actualizado en esta revisión. Firmware, modelo local y permisos Android sin cambios. No se ejecutan escaneos ni se crean evidencias al probar la lógica del catálogo.

APK: `app/build/outputs/apk/debug/app-debug.apk`, **65.134.046 bytes**, SHA-256 `44daaff240dbc6e4a07fdbfc1301b5145c2dca2e4e1cf08dbb190a6dc3dd7dce`. Copias locales de `dist/` actualizadas; APK, modelo y registro de instalación permanecen fuera de Git. Sin release ni fusión en main.

Próxima comprobación: desbloquear el A15 y ejecutar `DeckCatalogDeviceTest`; revisar las capturas reales de inicio, áreas y búsqueda antes de considerar validado el nuevo recorrido en pantalla.
