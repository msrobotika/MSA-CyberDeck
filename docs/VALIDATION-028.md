# CiberDeck 0.28 · Identidad visual de campo

## Diseño implementado

La portada conserva el robot original de MS ROBOTIKA con más presencia, una retícula decorativa, halo de respiración y aro estático. La pausa se presenta como control accesible en la esquina de la tarjeta y respeta la desactivación de animaciones del sistema. No representa un escaneo, ataque, carga de trabajo ni conexión al ESP32. El texto de estado utiliza la última instantánea real del móvil o indica que todavía no hay lectura.

El tema utiliza fondo carbón, superficies azul oscuro, contornos finos y acentos por área: cian para móvil, menta para red, violeta para proximidad, ámbar para hardware, azul para archivos y rosa para agente/IA. Los nombres de área y herramienta siguen identificándolas sin depender del color. Los iconos se dibujan con código: escudo, nodos de red, Bluetooth, chip y estrella del asistente, además de archivos. No se añade una biblioteca ni se descarga otro recurso gráfico.

Las tarjetas de área comparten altura por fila, muestran un icono propio, descripción, flecha y el número de entradas del catálogo correspondiente. Ese número no es una medición de seguridad ni un recuento de dispositivos conectados. Las tarjetas de herramientas reutilizan el color e icono del área, distinguen su condición de uso en una etiqueta y mantienen el texto de alcance. Los controles principales del inicio son rectangulares con esquinas moderadas. El acceso al trabajo activo pasa a un tamaño secundario y la navegación inferior tiene una separación más clara.

No cambia la navegación por tareas, la ejecución de herramientas, el modelo local, las fuentes, el protocolo USB ni los datos guardados. La cuadrícula sigue pasando a una columna con escala de fuente superior a 1,3; no se fijan alturas máximas que recorten el texto.

## Archivos

- `CiberDeckHome.kt`: portada, robot, control de animación y tarjetas de áreas.
- `DeckDesign.kt`: paleta, superficies, formas, iconos vectoriales y navegación.
- `DeckHome.kt`: tarjetas de herramientas y etiquetas de alcance.
- `DeckCatalog.kt`: iconos de las áreas; mismas rutas y funciones.
- `MainActivity.kt`: jerarquía visual del acceso al trabajo activo.
- `app/build.gradle.kts`: versión 0.28, código 35.

## Validación

- Compilación de aplicación, instrumentación y pruebas Kotlin correcta en **2 min 29 s**. Lint sin errores/fatales: **24 avisos y 1 sugerencia**, como en 0.27. El compilador señala dos usos nuevos de `quadraticBezierTo` en el escudo, API disponible pero deprecada a favor de `quadraticTo`; no se oculta ese aviso. Se mantiene la advertencia anterior de API Android/Gradle.
- **216 pruebas JVM correctas**, sin pruebas añadidas que se limiten a replicar estilos. Las rutas, búsquedas, informes y controles de negocio conservan los casos existentes.
- Contraste calculado de los acentos sobre la superficie base `#121B26`: cian 11,97:1, menta 11,83:1, violeta 8,41:1, ámbar 10,79:1, azul 8,86:1 y rosa 9,30:1. Texto secundario 9,62:1 sobre esa misma superficie. Son cálculos de colores sólidos, **no una auditoría de accesibilidad de la interfaz**, degradados, estados o tamaños.
- Actualización instalada en Galaxy A15 SM-A155F Android 16: `Success`, versión **0.28.0-ciberdeck-dev**, código 35, comprobada mediante Android. Solicitud de arranque aceptada; sin `FATAL EXCEPTION` en el registro del proceso consultado. A15 todavía bloqueado al finalizar; eso no demuestra que la nueva composición se vea correctamente ni valida el layout en ejecución.
- **Revisión posterior en el A15 desbloqueado, el 20 de octubre de 2026**: diez casos Android distintos comprobados (catálogo 3, foco del agente 2, tareas reales del móvil 4 y navegación DNS/recreación 1). Primera ronda: nueve correctos y un fallo del ensayo al buscar un área antes de que su fila estuviera compuesta en la lista perezosa. Se cambió el desplazamiento del ensayo a `performScrollToNode` sobre la lista; el caso pasó después en 5,529 s. No se cambió la APK para esa corrección del ensayo. El resultado final cubre los diez casos; no se presenta como una única ronda 10/10. Registros privados `a15-028-visual.txt` y `a15-028-visual-followup.txt`.
- Cinco PNG auténticos de 1080 × 2340 revisados: inicio, áreas, búsqueda, agente con cinco resultados locales reales y DNS loopback. Las filas de áreas tienen altura compartida y no se observaron recortes de texto en las vistas capturadas. Las pantallas largas continúan mediante desplazamiento; una captura no contiene todo el catálogo. Los archivos privados de algunos ensayos conservan nombres de versiones anteriores; las copias publicadas llevan 028 porque se obtuvieron con esa APK. Las capturas históricas de 0.27 se conservan por separado. OPPO no conectado.
- Datos del paquete, evidencias, modelo local y firmware conservados. No se añaden permisos ni bibliotecas. APK, modelo y registros de instalación/arranque quedan fuera de Git. Copias locales y huellas en `dist/` actualizadas; sin release ni fusión en main.

APK final: **65.183.194 bytes**, SHA-256 `8fbe0523d5c0afedda0199e669f46949df4315b7676a1e23a0e352238f228cb1`. Registro privado `.private/android-validation/a15-028-install.json`.

Pendiente: escalas de fuente mayores, otros tamaños de pantalla y OPPO. Los diez casos de esta ronda no validan todos los controladores o hardware. DNS loopback no demuestra Internet externo; no se generó IA ni se actualizó firmware.

<img src="screenshots/ciberdeck-home-028.png" width="240" alt="Inicio real de CiberDeck 0.28"> <img src="screenshots/ciberdeck-kit-028.png" width="240" alt="Áreas con acento e icono propio"> <img src="screenshots/ciberdeck-search-028.png" width="240" alt="Tarjetas de herramientas con alcance visible">

[Resultados reales del agente](screenshots/ciberdeck-agent-028.png) · [Diagnóstico loopback real](screenshots/ciberdeck-dns-028.png).
