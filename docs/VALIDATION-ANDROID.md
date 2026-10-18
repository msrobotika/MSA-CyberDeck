# Primera instalación en Android físico — 17 octubre 2026

- Dispositivo de prueba: CPH2743; ADB autorizado por el propietario.
- APK local: versión 0.2.0-dev, versionCode 2, 11292494 bytes.
- SHA-256: `A0217142DC385373ACA1DF8843A17F1A69C11D7180811B521980DA283D27178F`.
- Instalación mediante `adb install -r`: Success.
- Inicio de `com.msarobotika.cyberdeck/.MainActivity`: Status ok, arranque en frío en 2301 ms.
- Captura real revisada: interfaz renderizada, robot, tarjetas y textos presentes. La vista estaba desplazada; esta captura no verifica toda la navegación.
- Consulta posterior de AndroidRuntime: sin errores de cierre en los 250 registros consultados.
- Captura original conservada en `.private/android-validation/resumen.png`. No se publica: incluye la barra de estado del propietario.
- 19 pruebas JVM superadas con JUnitCore. Incluyen pruebas DNS, TCP sin carga, HTTP HEAD sin seguir redirecciones y validación TLS con servidor local de prueba. No equivalen a ejecución de esas funciones en el teléfono.

Pendiente: navegación y diagnósticos desde la interfaz Android, importación/persistencia/exportación en el teléfono y enlace USB directo con el ESP mediante OTG. El teléfono conectado al PC no valida el enlace teléfono–ESP. No se publicaron cambios ni APK.

## Validación funcional 0.2.1-dev en el teléfono

- Compilación de app y APK de pruebas: correcta. APK de app: 11292745 bytes; SHA-256 `3C4C40A40289E98DFD02B81D6311957988D4A4E3D86EB771DCBB70E847BFD9E8`.
- Test instrumental `DeviceWorkflowTest.connectionDnsWebEvidenceAndPersistentNavigation`: **OK (1 test)** en 11,515 segundos, ejecutado en CPH2743.
- Acciones reales sobre controles Compose: comprobar conexión local, resolución IP con el resolver del sistema, conexión TCP aceptada por un servidor loopback, consulta HTTP HEAD a un servidor temporal loopback, guardar resultados, verificar hashes, cambiar de pestaña tras desplazar resultados y recrear actividad.
- La prueba comprueba que el servidor recibe HEAD, que se completa la revisión HTTP y que los diagnósticos guardados persisten y aparecen en el informe generado. No invoca el selector Android de exportación ni la importación de archivos del usuario.
- 19 pruebas JVM vuelven a pasar. TLS se valida en JVM con fixtures; no se ha probado HTTPS externo desde el teléfono en esta ejecución.
- Capturas PNG nativas generadas por el test de la app: `screenshots/android-home-actions.png`, `screenshots/android-dns-evidence.png`, `screenshots/android-web-evidence.png`. Son ejecución real, no maquetas. La portada está recortada para mostrar solo acciones y excluir la tarjeta con direcciones de la red. La captura de conexión con direcciones locales y la lista de evidencias del propietario se conservan solo en `.private/`.
- La prueba añade evidencias de laboratorio; no borra evidencias existentes. Los servidores de prueba se cierran al terminar.
- El enlace OTG teléfono–ESP y la exportación mediante selector siguen pendientes. Sin publicación en GitHub.
