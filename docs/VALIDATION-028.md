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
- **Revisión visual, capturas auténticas y regresiones Android de 0.28 pendientes**. Los 16 casos Android comprobados en 0.27 no se atribuyen al diseño nuevo. Las imágenes de 0.27 permanecen identificadas por versión. OPPO no conectado.
- Datos del paquete, evidencias, modelo local y firmware conservados. No se añaden permisos ni bibliotecas. APK, modelo y registros de instalación/arranque quedan fuera de Git. Copias locales y huellas en `dist/` actualizadas; sin release ni fusión en main.

APK final: **65.183.194 bytes**, SHA-256 `8fbe0523d5c0afedda0199e669f46949df4315b7676a1e23a0e352238f228cb1`. Registro privado `.private/android-validation/a15-028-install.json`.

Próximo paso: desbloquear el A15 para ejecutar los recorridos de catálogo, agente y resultados y revisar capturas de inicio, áreas, búsqueda y resultado con la APK nueva. Comprobar especialmente la cuadrícula de altura compartida y el texto de la portada, también con escalas de fuente mayores.
