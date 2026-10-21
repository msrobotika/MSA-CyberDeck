# Cambios de CiberDeck

## Documentación · 21 de octubre de 2026

- La portada de `main` pasa a describir la APK pública 0.37, con descarga, instalación, alcance, permisos y límites registrados.
- Se incorporan las guías de uso y los informes pertinentes desde la rama de desarrollo, con un índice y una guía de compilación.
- Se destaca la captura auténtica de conexión 0.37 y se identifican las de 0.36 y el prototipo como anteriores.
- Se explica la separación entre `main` (código del prototipo 0.1), la etiqueta publicada y la rama de desarrollo pendiente de fusión.

Este cambio actualiza documentación e imágenes. No publica otra APK, no fusiona la implementación y no añade resultados nuevos de pruebas.

## 0.37.0-dev · 21 de octubre de 2026

[APK y código etiquetado](https://github.com/msrobotika/MSA-CyberDeck/releases/tag/v0.37.0-dev) · código Android 44.

- Reconocimiento de «analiza mi red wifi» y variantes explícitas en la entrada del agente y en el seguimiento.
- Envío compartido para texto y transcripciones de Android; aviso visible para peticiones desconocidas cuando no hay IA cargada.
- Consulta de conexión y recursos reales sin necesitar ESP32 ni modelo. Pedir Wi-Fi no transforma datos móviles en una lectura Wi-Fi.
- Validación registrada: 266 pruebas JVM y 18 casos Android en A15; actualización, versión y arranque en OPPO. Lint sin errores y con 24 avisos.

Dictado humano, enlace OTG directo Android–ESP y otras limitaciones siguen pendientes. Consulta [método, huella y alcance](docs/VALIDATION-037.md).

## 0.36 · Historial

Seguimiento contextual, repetición con sesión limpia y guardado verificado de resultados. [Informe de esa ronda](docs/VALIDATION-036.md). No debe confundirse su APK ni sus recuentos con 0.37.

El resto del historial técnico permanece en [la rama de desarrollo](https://github.com/msrobotika/MSA-CyberDeck/tree/feature/superflyper-usb-v0-1/docs) y en los commits del proyecto.
