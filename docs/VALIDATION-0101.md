# CiberDeck 0.10.1 · modelo abierto y herramientas de campo

## Estado comprobado

- APK de aplicación y de pruebas compiladas con la misma herramienta fijada en 0.10. Lint sin errores; permanecen 20 advertencias y una sugerencia.
- **109 pruebas JVM correctas**, con límites de modelos, limpieza de etiquetas de razonamiento, aritmética magnética y casos IPv4 /0, /31, /32 y entradas ambiguas.
- **6 pruebas físicas sin interfaz correctas en A15**: biblioteca nativa, TCP real sin datos de aplicación, preservación de importación, transacción de almacenamiento del modelo, inspección de firma/servicios de la APK y muestras reales del magnetómetro.
- Prueba optativa de inferencia con el Qwen3 descargado: correcta. Motor local y contexto obtenido del propio teléfono. Carga 2393 ms y respuesta 29812 ms en esa ejecución; calidad experimental, no una evaluación de precisión. [Modelo y límites](OPEN-LOCAL-MODEL.md).
- La actualización conserva el paquete y datos anteriores. El OPPO sigue desconectado y no se declara actualizado.

## Herramientas nuevas

**Campo magnético** usa `TYPE_MAGNETIC_FIELD`, presente como MXG4300S en el A15. Módulo del vector X/Y/Z en µT, precisión declarada, gráfica de hasta 60 muestras, máximo dos por segundo, referencia temporal e informe. Se pausa al salir, y el listener se desregistra directamente al detener la actividad. No hay grabación continua ni muestras precargadas. El retículo es una ilustración de sensor, no un mapa de emisores.

Las muestras físicas recibidas acreditan acceso al sensor. La pantalla, el gesto de referencia, exportación desde sus botones y el ciclo completo de pausa visual aún requieren la prueba con el móvil desbloqueado; no se dan por validados por haber compilado. La prueba `MagneticDeviceTest` está preparada para ese paso. El propietario decidió desbloquearlo más adelante.

No es un radiómetro RF ni un detector de ataques. El magnetómetro responde a campo terrestre, orientación, imanes, metales y calibración. No determina la procedencia ni seguridad sanitaria del campo. La placa ESP32 conectada anteriormente no incorpora magnetómetro: para medir autónomamente necesita hardware externo, y no se ha inventado ese sensor por software.

**Plan de red** calcula máscara IPv4, red, bloque, rango y cantidad de direcciones según prefijo. Permite leer dirección y prefijo publicados por Android para la red activa. No recorre subredes, no descubre dispositivos y el informe se identifica como cálculo, no como evidencia de equipos conectados. /31 se interpreta para enlaces punto a punto; /32 como host. No garantiza asignabilidad ni refleja concesiones DHCP.

## ESP 0.3.1: preparado, sin instalar

El radar, cabecera y navegación inferior se han comparado con la versión anterior y su código permanece idéntico. Se rediseñaron Redes, Ficha, Observaciones y Datos. La gráfica de recepción toma diferencias reales del contador de tramas y reloj del ESP; queda vacía hasta tener muestras. Un reinicio del contador borra esa serie.

Compilación ESP-IDF/PlatformIO correcta. Aplicación candidata SHA-256 `5a1e869ce173b7f9a5fea2a5f07f3bc6b70f6d9431e6e3a79a6fefdf23c4b930`. No se ha escrito flash: en esta revisión solo aparece el puerto del Samsung, no el CH340 del ESP. La recuperación original permanece intacta. Antes de instalar, comprobar placa y puerto reales y conservar un respaldo válido; nunca utilizar COM5 del Samsung como si fuese el ESP.

La imagen `docs/screenshots/esp-ui-031-preview.png` está marcada como **vista previa**. Se renderizó en escritorio ejecutando las mismas funciones C, con datos de una lectura anterior y etiquetas anonimizadas. No es una captura nueva de hardware ni acredita la instalación. La comprobación física del diseño queda pendiente de reconexión.

## Archivos principales

- `MagneticPanel.kt`, `NetworkPlan.kt`, `NetworkPlanPanel.kt`: herramientas y presentación.
- `AiModelStore.kt`, `AiPolicy.kt`, `LocalAiService.kt`, `LocalAiPanel.kt`: importación, presentación y ejecución del modelo público.
- `DeckHome.kt`, `DeckSessionModel.kt`, `MainActivity.kt`: catálogo, estado y navegación.
- `firmware/src/display.c`, `firmware/src/main.c`: candidata de pantallas 0.3.1.
- Pruebas de políticas, sensor e inferencia en `app/src/test` y `app/src/androidTest`.

Modelos, resultados con postura del teléfono, transferencias de prueba y compiladores temporales permanecen bajo rutas excluidas de Git. No se incluyen pesos en la APK ni el repositorio. La candidata se conserva en la rama de desarrollo; no se fusiona en main ni se publica una release.

APK local: `dist/CiberDeck-debug.apk`, versión `0.10.1-ciberdeck-dev`, código 15. SHA-256 `b250b0399667798286f4ef96c5660b3cf8943e62e01da8a734fe7a0d490fee7d`. El firmware candidato está en `dist/sensor/CiberDeck-ESP-0.3.1-candidate.bin`; disponer de ese archivo no significa que se haya instalado en la placa.
