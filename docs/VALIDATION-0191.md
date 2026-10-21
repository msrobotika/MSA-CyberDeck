# CiberDeck 0.19.1 · Acceso visible al agente

## Problema y cambio

El agente estaba dentro de «Asistente IA» y era fácil confundirlo con el analista. Ahora tiene ruta y título propios:

- Inicio: botón «Abrir agente».
- Menú lateral: entrada «Agente» visible sin desplegar áreas.
- Kit: tarjeta «Agente» en Asistente.
- En la pantalla se muestra «Agente / Tu agente de campo» y se activa directamente el modo correcto.
- Los ajustes largos del modelo se pliegan; carga e importación siguen accesibles. El analista conserva su pantalla «Asistente IA».
- Cambiar entre analista y agente conserva el plan y el modelo ya cargado mediante una composición compartida. Al entrar en cada vista se vuelve al inicio del contenido.
- La apertura explícita con `open_agent` solo navega: no prepara ni ejecuta acciones automáticamente.

Las 30 entradas del ejecutor de 0.19 se conservan. La nueva entrada de navegación no añade una herramienta recursiva al catálogo. No se modifican firmware, permisos de manifiesto, red, archivos importados ni funciones de los controladores.

## Archivos

`MainActivity.kt`, `DeckHome.kt`, `DeckNavigationDrawer.kt`, `CiberDeckHome.kt`, `DeckDesign.kt`, `LocalAiPanel.kt`, versión Android y pruebas de navegación/IA/catálogo. README y guía del agente actualizados.

## Validación final

- APK principal y APK de pruebas compiladas; lint correcto. Aviso de deprecación anterior conservado.
- **154 pruebas JVM correctas** después de compilar las fuentes finales.
- **6 pruebas en el A15 correctas**, 35,153 s, registro privado `a15-0191-final.txt`.
- Inicio abre el agente real sin tener que seleccionar el modo.
- El menú muestra el acceso directo sin expandir categorías.
- El kit abre la ruta correcta y el cambio al analista conserva el plan.
- Modelo local real: carga, cambio analista → agente conservando el modelo en memoria, propuesta de móvil/conexión, ejecución de ambas herramientas y guardado verificable de la sesión en su trabajo.
- Enlace y CIDR con entradas explícitas sin cargar modelo.
- Cobertura del catálogo de pantallas: la entrada Agente se reconoce como navegación del propio ejecutor, no como nueva acción.

El primer lote encontró selectores de pruebas ambiguos: el botón Agente y la entrada del menú compartían texto. Se corrigió la selección de nodos para distinguir controles. Los lotes finales pasaron; no se oculta el intento fallido como si fuera una validación correcta.

## Instalación y estado

Galaxy A15 SM-A155F (`RF8X70M9B4A`), Android 16, comprobado por ADB. Instalación de actualización conservando el paquete y sus datos. Versión leída del dispositivo: `0.19.1-ciberdeck-dev`, código 26.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

- Tamaño: 66061985 bytes.
- SHA-256: `50ca33b7298ad8dbd03120dad561f61f71b04a8f83edafe2b6db02034fdcd711`.
- Copias de distribución local actualizadas en `dist/`; sin release pública.

Al finalizar se abrió directamente Agente y se comprobó en la interfaz real «Tu agente de campo» y «MODELO EN MEMORIA». No se ejecutó un plan del usuario al abrirla.

## Captura real

<img src="screenshots/ciberdeck-agent-entry-0191.png" width="300" alt="Entrada real del agente en el A15 con título y controles propios">

La captura se tomó antes de cargar el modelo durante la prueba de entrada, por eso indica «sin cargar». El estado final del teléfono se comprobó después por separado. No contiene lecturas simuladas.

## Límites

Esta revisión resuelve acceso, navegación y continuidad entre vistas. No añade nuevas funciones de hardware ni valida OTG, GATT o un segundo móvil. Continúan los límites y pendientes físicos de [0.19](VALIDATION-019.md). Las pruebas de exportación y gestión de trabajos previstas al retomar se pospusieron para priorizar el problema comunicado de visibilidad del agente.

Rama de desarrollo `feature/superflyper-usb-v0-1`, PR #1 en borrador, sin main ni release. Registros, informes y modelo quedan fuera de Git.
