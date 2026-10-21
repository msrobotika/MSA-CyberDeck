# Compilar CiberDeck 0.37

Esta guía documenta el entorno registrado de la APK publicada. La actualización documental del 21 de octubre de 2026 no recompila ni cambia la APK.

## Elegir el código

`main` conserva el prototipo Android 0.1, acompañado ahora por la documentación de la versión pública. Compilar `main` no produce CiberDeck 0.37. La etiqueta [v0.37.0-dev](https://github.com/msrobotika/MSA-CyberDeck/tree/v0.37.0-dev) identifica el código de la APK publicada. El desarrollo continúa en `feature/superflyper-usb-v0-1`, pendiente de fusión en el [PR #1](https://github.com/msrobotika/MSA-CyberDeck/pull/1).

```text
git clone https://github.com/msrobotika/MSA-CyberDeck.git
cd MSA-CyberDeck
git checkout v0.37.0-dev
```

## Entorno registrado

| Componente | Versión |
| --- | --- |
| JDK | 17 |
| Android SDK de compilación y destino | 35 |
| Android mínimo | 26 / Android 8, sistema de 64 bits para la APK completa |
| Android Gradle Plugin | 8.10.1 |
| Kotlin | 2.2.21 |
| Gradle usado para la candidata | 8.13 |

Instala Gradle 8.13 y comprueba `gradle --version` y `java -version`. El wrapper de esa etiqueta apunta todavía a Gradle 8.9: no se ha actualizado ni debe usarse como si fuera el entorno validado de 0.37. Usa la instalación explícita de Gradle 8.13.

Configura `JAVA_HOME` para JDK 17. Instala SDK 35 desde Android Studio y configura su ruta mediante `ANDROID_HOME` o un archivo `local.properties` en la raíz. Ejemplo Windows, sustituyendo la ruta por la real:

```properties
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
```

No subas `local.properties`, claves de firma, modelos ni evidencias privadas al repositorio.

## Compilar y comprobar

Desde la raíz del proyecto, usando Gradle 8.13:

```text
gradle assembleDebug assembleDebugAndroidTest compileDebugUnitTestKotlin lintDebug
```

APK de aplicación: `app/build/outputs/apk/debug/app-debug.apk`. La APK de pruebas se genera aparte bajo `app/build/outputs/apk/androidTest/` y no es la aplicación que debe distribuirse.

Para ejecutar pruebas en tu entorno:

```text
gradle testDebugUnitTest
gradle connectedDebugAndroidTest
```

La segunda orden requiere un Android conectado y autorizado, o un emulador compatible. Los casos con sensores y periféricos requieren el hardware correspondiente. El resultado de una compilación no demuestra que las pruebas se hayan ejecutado.

En el PC original falló el lanzador de pruebas JVM por un problema de sockets: los 266 casos registrados se ejecutaron con JUnitCore tras compilar. No se afirma que `testDebugUnitTest` se haya validado en una instalación limpia. La ronda registrada de 18 casos Android corresponde al Samsung A15; en OPPO se comprobó instalación, versión y arranque. [Informe y límites](VALIDATION-037.md).

## Instalación y firma

Una compilación local puede tener otra clave de desarrollo y no actualizar la APK pública. Exporta las evidencias antes de una eventual desinstalación; no borres datos para resolver un conflicto de firma sin conservarlos primero.

La APK pública de 0.37 tiene **65 298 010 bytes** y SHA-256:

```text
ff0f9921b402909a8c741c4820d7b66a31ccbcec88a1e935aaff3153530a18fc
```

La huella corresponde al archivo de [la release](https://github.com/msrobotika/MSA-CyberDeck/releases/tag/v0.37.0-dev), no a cualquier compilación local. No se garantiza una reproducción idéntica byte a byte.
