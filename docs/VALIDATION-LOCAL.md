# Validación local — 17 octubre 2026

- Gradle 8.9, JDK 17, Android SDK 35.
- :app:assembleDebug completó y produjo una APK de desarrollo de 9477980 bytes.
- SHA-256: 5D933ED0B08317D3A3C4D63A3C09F70E4A24C008D8A8A4D44B27EBA6C9841B12.
- Las clases de EvidenceHashTest compilaron. La ejecución directa mediante JUnitCore completó OK (2 tests): vector conocido abc, copia exacta de varios bloques y cambio de hash tras alterar un byte.
- La tarea testDebugUnitTest de Gradle falló por Unable to establish loopback connection en el proceso de pruebas Java. No se informa como superada.
- No hay teléfono conectado ni se ha realizado una prueba en emulador. Importación, persistencia, exportación y enumeración USB requieren validación en Android.
- preview/index.html y docs/screenshots/preview-local.png son una vista previa web del diseño, no capturas de ejecución Android.
- No se ha publicado esta iteración ni su APK.

## Actualización posterior

La primera instalación física y los resultados actuales de 0.2.0-dev se documentan en [VALIDATION-ANDROID.md](VALIDATION-ANDROID.md). La ausencia de teléfono y el hash anteriores corresponden a la compilación inicial.
