# CiberDeck — MS ROBOTIKA

**Un kit Android para observar tu móvil, revisar redes autorizadas y documentar lo que encuentras, con IA local y ESP32 opcionales.**

[Descargar APK 0.37](https://github.com/msrobotika/MSA-CyberDeck/releases/download/v0.37.0-dev/CiberDeck-0.37.0-dev.apk) · [Guía de uso](docs/QUICKSTART-ANDROID.md) · [Pruebas y límites](docs/VALIDATION-037.md) · [Licencia MIT](LICENSE)

**Versión pública de desarrollo**, no una versión estable ni una herramienta certificada de auditoría. La APK incluye las herramientas Android; los pesos de IA se descargan por separado. No necesitas un ESP32 para empezar.

<img src="docs/screenshots/ciberdeck-network-037.png" width="250" alt="Resultado real de conexión en CiberDeck 0.37, Samsung A15">

Captura auténtica del dispositivo con la versión actual. Las animaciones decorativas no son lecturas de sensores ni detecciones de ataques.

## Instalar y empezar

1. Descarga **CiberDeck-0.37.0-dev.apk** desde [Releases](https://github.com/msrobotika/MSA-CyberDeck/releases/tag/v0.37.0-dev). Requiere Android 8 o posterior **de 64 bits**; el hardware disponible determina las herramientas utilizables.
2. Abre la APK en Android y, si el sistema lo solicita, permite a ese navegador o gestor instalarla. Mantén Play Protect activo y revisa los avisos del sistema. Es una APK de desarrollo, distribuida fuera de Google Play.
3. Si ya tienes una instalación compatible, elige actualizar. Si Android indica conflicto de firma, **no desinstales sin exportar primero tus evidencias**: la desinstalación elimina las copias privadas y puede ser necesario revisar la firma de tu compilación.
4. Abre **Inicio → Abrir agente** y pulsa **Revisar mi móvil**, o escribe **«Analiza mi red wifi»** y pulsa **Enviar petición**.
5. Consulta la fecha y la fuente del resultado. Puedes repetir la lectura, abrir los detalles o guardar el resultado para revisarlo después.

El dictado de Android rellena un borrador: revisa el texto y pulsa Enviar. Las tareas locales reconocidas no necesitan cargar IA. No se interpreta cualquier frase automáticamente; las peticiones complejas dependen del modelo local y pueden requerir datos, permisos y revisión del plan.

## Qué puedes hacer

| Área | Funciones implementadas | Condiciones y límites |
| --- | --- | --- |
| Móvil | Estado de ajustes, parche, batería, almacenamiento, RAM y conexión; gráficas y revisión guiada | Datos expuestos por Android, sin acceso root ni diagnóstico universal de malware |
| Conexión | Transporte, interfaz, direcciones locales, DNS y estado de Internet según Android | «Analiza mi red wifi» ejecuta esta comprobación local; no certifica seguridad ni mide velocidad |
| Red y web | Resolución DNS, comprobaciones TCP de alcance elegido, certificado y cabeceras HTTP(S), calculadora IPv4/CIDR | Destinos propios/autorizados; un puerto abierto no prueba una vulnerabilidad |
| Mi red | Descubrimiento de servicios anunciados por DNS-SD/mDNS | No enumera todos los clientes del router ni accede a sus archivos |
| Bluetooth BLE | Anuncios, RSSI, UUID y descubrimiento GATT autorizado | Sin estimar distancia ni leer/escribir características GATT |
| NFC | Identificador, tecnologías y NDEF disponible; informes y comparación | Requiere NFC; no clona tarjetas ni emula credenciales de acceso/pago |
| QR, enlaces y APK | QR desde imagen, inspección local de URL, hash/certificados/permisos/componentes de una APK elegida | No abre enlaces automáticamente ni acredita que un archivo sea seguro |
| Sensores | Magnetómetro real en µT, ejes y gráficas | Requiere sensor; no mide RF ni evalúa riesgo sanitario o exposición laboral |
| Evidencias | Copias privadas, SHA-256, búsqueda, exportación, borrado confirmado, trabajos y comparación | El hash verifica la copia; no certifica autoría ni cadena de custodia |
| Dos móviles | Archivos seleccionados por sesión TLS con aprobación; recepción temporal desde navegador | El modo navegador usa HTTP local sin cifrar: solo redes de confianza y contenido no confidencial |
| Agente e IA | Tareas directas, planes con herramientas, fuentes, seguimiento y análisis local opcional | Modelo experimental: puede equivocarse; no tiene acceso ilimitado al teléfono |
| ESP32 USB | Conector serie compatible con WiFi Sentinel y representación de lecturas reales | Cable OTG de datos, permiso y firmware compatibles; enlace directo móvil–ESP pendiente de validación física |

**Experimental:** calidad de planificación/análisis de IA y guardia de llamadas. Esta última requiere elegir expresamente la app en Android; no graba ni bloquea llamadas y su validación con llamada física está pendiente.

**Pendiente o no incluido:** VPN integrada, radio sub-GHz/IR sin módulos externos compatibles, clonación de credenciales, acceso remoto arbitrario a otro móvil, protección total y detección avanzada de intrusiones. Un ESP32 clásico con USB-UART no se convierte en adaptador Wi-Fi USB nativo por instalar esta app.

## IA local opcional

La herramienta funciona sin modelo para sus tareas directas. Para probar el analista, consulta [modelo exacto, descarga e importación](docs/OPEN-LOCAL-MODEL.md): Qwen3 0.6B mixed INT4 para LiteRT-LM, unos 498 MB adicionales. El motor es de Google; este modelo **no es Gemma ni Gemini**. Los pesos y su licencia son independientes y no están incluidos en la APK.

En **Asistente IA**, importa el archivo compatible, carga el modelo y elige las fuentes antes de preguntar. Comprueba la interpretación contra los datos originales; una respuesta convincente no demuestra exactitud. No promete reconocer cualquier petición ni sustituir a un especialista.

## ESP32 como complemento

La placa probada desde PC es una ideaspark con ESP32 clásico y pantalla ST7789 de 240 × 135 en horizontal. BOOT cambia de pantalla; no es táctil. La app espera respuestas de firmware compatible, no cualquier sketch de ESP32.

Conectar teléfono y ESP al PC por separado **no establece OTG entre ellos**. La prueba física directa Android–ESP sigue pendiente. El rediseño ESP 0.3.1 está compilado, pero no se declara instalado. Consulta [protocolo USB](docs/SUPERFLYPER-USB-PROTOCOL.md) y [validación del sensor](docs/VALIDATION-SENSOR.md). Identifica tu placa y conserva un respaldo antes de cambiar firmware; esta APK no flashea la placa.

## Privacidad y permisos

Las evidencias y modelos importados se conservan en almacenamiento privado. Exporta lo que quieras conservar antes de desinstalar. La app no incorpora una cuenta de servicio ni sube automáticamente las evidencias a la nube. Las herramientas de red, transferencias y el proveedor de dictado pueden comunicarse con sus destinos; no debe confundirse «IA local» con «todas las funciones sin red».

Bluetooth, NFC, USB, archivos, notificaciones y el rol de llamadas se utilizan según la función elegida y las autorizaciones de Android. No concede acceso a los datos privados de otras aplicaciones. Revisa identificadores, IP, nombres de redes, archivos y secretos antes de compartir informes o capturas.

Utiliza las pruebas de seguridad únicamente con dispositivos propios o expresamente autorizados. El proyecto conserva sus [condiciones de archivos y limpieza](docs/FILES-AND-CLEANUP.md) y [límites de Bluetooth, NFC y transferencias](docs/RADIOS-AND-PEER.md).

## Estado comprobado de 0.37

- APK instalada en Samsung A15 SM-A155F y OPPO CPH2743, ambos Android 16.
- **266 pruebas JVM y 18 casos instrumentados correctos en A15**. Instalación, versión y arranque comprobados en OPPO; no se atribuyen esas 18 pruebas al OPPO.
- Petición exacta «analiza mi red wifi» comprobada por texto y transcripción controlada del contrato de dictado; prueba humana de micrófono pendiente.
- Compilación correcta; lint sin errores y con 24 avisos. No es una auditoría independiente de seguridad.
- APK de 65 298 010 bytes. SHA-256: `ff0f9921b402909a8c741c4820d7b66a31ccbcec88a1e935aaff3153530a18fc`.

[Método completo y límites de esta versión](docs/VALIDATION-037.md). El historial técnico anterior permanece en los commits de Git.

## Código, compilación y contribuciones

El código de esta APK está en la etiqueta [v0.37.0-dev](https://github.com/msrobotika/MSA-CyberDeck/tree/v0.37.0-dev). La continuidad se mantiene en `feature/superflyper-usb-v0-1` y en el [PR #1](https://github.com/msrobotika/MSA-CyberDeck/pull/1), sin fusionar directamente en main.

Usa JDK 17 y Android SDK 35. El plugin Android 8.10.1 de esta rama necesita una versión de Gradle compatible: la candidata se compiló con **Gradle 8.13**; el wrapper antiguo aún apunta a 8.9 y no debe presentarse como una compilación reproducible ya verificada. Con Gradle 8.13 instalado, configura el SDK local y ejecuta:

```text
git clone https://github.com/msrobotika/MSA-CyberDeck.git
cd MSA-CyberDeck
git checkout v0.37.0-dev
gradle assembleDebug assembleDebugAndroidTest compileDebugUnitTestKotlin lintDebug
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Las claves de firma de desarrollo propias pueden diferir de la APK distribuida. El lanzador JVM de Gradle tuvo un problema de sockets en el PC de desarrollo: las pruebas de lógica se ejecutaron con JUnitCore tras compilar, como documentan los informes. No se promete todavía un proceso limpio validado en otro ordenador.

Para informar de un error, indica versión, móvil, Android, pantalla, petición exacta y resultado esperado; elimina secretos de los registros. Para contribuir, abre un issue o PR con un cambio acotado y evidencia de pruebas. No adjuntes credenciales, datos de terceros ni evidencias privadas. Código bajo [MIT](LICENSE); modelos y dependencias mantienen sus propias licencias.

## English

CiberDeck is an open-source Android toolkit for inspecting your own devices and authorized networks, collecting local evidence and optionally using an on-device AI model or a compatible ESP32. This is a **public development APK**, not a stable security product. [Download 0.37](https://github.com/msrobotika/MSA-CyberDeck/releases/download/v0.37.0-dev/CiberDeck-0.37.0-dev.apk). Android 8+ with a 64-bit OS is required. Direct Android–ESP OTG and human microphone testing remain pending. It does not clone NFC credentials, provide a built-in VPN or guarantee intrusion detection. The Spanish guide and validation reports describe the actual tested scope.
