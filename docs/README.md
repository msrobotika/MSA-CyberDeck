# Documentación de CiberDeck

Actualizada el **21 de octubre de 2026** para la APK pública de desarrollo **0.37.0**, código Android 44. La portada y estas guías describen esa APK. El código de `main` sigue siendo el prototipo 0.1: para trabajar sobre la aplicación actual usa [v0.37.0-dev](https://github.com/msrobotika/MSA-CyberDeck/tree/v0.37.0-dev) o [la rama de desarrollo](https://github.com/msrobotika/MSA-CyberDeck/tree/feature/superflyper-usb-v0-1).

## Empezar

| Documento | Contenido |
| --- | --- |
| [Guía de uso](QUICKSTART-ANDROID.md) | Instalación, primera petición, herramientas y solución de problemas |
| [Capturas](SCREENSHOTS.md) | Interfaz real, versión y origen de cada imagen destacada |
| [Compilación](BUILD.md) | Código correcto, JDK, SDK, Gradle y comprobaciones |
| [Cambios](../CHANGELOG.md) | Novedades de la versión publicada y actualización documental |
| [Validación de 0.37](VALIDATION-037.md) | Resultados registrados en A15 y OPPO, huella de APK y pruebas pendientes |

## Guías por función

| Documento | Contenido |
| --- | --- |
| [Archivos y limpieza](FILES-AND-CLEANUP.md) | Copias privadas, exportación, borrado y recepción desde navegador |
| [Bluetooth, NFC y transferencias](RADIOS-AND-PEER.md) | Permisos, sesiones entre móviles y límites |
| [Mi red](MI-RED.md) | Descubrimiento de servicios locales mDNS/DNS-SD |
| [Modelo local](OPEN-LOCAL-MODEL.md) | Qwen3 compatible, descarga fijada, hash y pruebas de inferencia |
| [Motor de IA](LOCAL-AI.md) | Recursos, contexto, aislamiento y limitaciones |
| [Protocolo USB del ESP32](SUPERFLYPER-USB-PROTOCOL.md) | Contrato con firmware compatible; OTG directo pendiente |

Los números de versión en guías de funciones indican cuándo se documentó su incorporación, no una APK nueva disponible. Para instalar y conocer el alcance actual, empieza por la guía de 0.37.

## Informes anteriores

[Validación de 0.36](VALIDATION-036.md) explica las dos capturas anteriores conservadas en la galería. [Validación del sensor](VALIDATION-SENSOR.md) recoge pruebas desde PC del 17 de octubre y correcciones posteriores; no acredita un enlace OTG Android–ESP. Sus APK, recuentos y dispositivos corresponden a sus respectivas rondas.

El [archivo técnico de desarrollo](https://github.com/msrobotika/MSA-CyberDeck/tree/feature/superflyper-usb-v0-1/docs) conserva el resto de informes y diseños históricos. Una vista previa de escritorio, un framebuffer o una imagen del prototipo no equivalen a una captura nueva de la APK 0.37.
