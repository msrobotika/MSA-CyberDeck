# Validación de Mi red — 17 octubre 2026

- Versión instalada: 0.3.0-dev (versionCode 4), dispositivo CPH2743.
- Compilación de app y APK instrumental: BUILD SUCCESSFUL.
- APK: 11744784 bytes; SHA-256 `93970850650D45B26AB98783C3E1ACC5A50E48F3FA4B9919A3ACC2664FA5FFF9`.
- `lintDebug`: BUILD SUCCESSFUL, 0 errores y 7 avisos sobre versiones de dependencias y recursos del icono. No se declara ausencia total de avisos.
- Se protegió el uso de TRANSPORT_USB con una comprobación de versión de Android. Se añadieron exclusiones de datos para copias automáticas y transferencias del sistema en Android 12 o superior.
- La instalación inicial requirió una respuesta del propietario a Google Play Protect. El propietario confirmó haber respondido y la instalación terminó con Success. No se desactivó ninguna protección mediante herramientas.
- Prueba instrumental en la compilación final: **OK (2 tests)** en 31,468 segundos.
- `LanInventoryDeviceTest`: registró un anuncio HTTP temporal en el propio móvil; lo descubrió por su nombre, resolvió el puerto anunciado, detuvo la sesión, guardó JSON y verificó SHA-256. Otra sesión se detuvo automáticamente tras 15 segundos. El servicio de prueba se retiró al terminar.
- `DeviceWorkflowTest`: volvió a comprobar conexión local, resolución IP, TCP loopback, HTTP HEAD loopback, guardado, verificación y persistencia. Es una prueba funcional contra fixtures locales, no una auditoría de otros equipos de la red.
- Captura nativa revisada: `screenshots/android-lan-overview.png`, recortada para excluir las fichas con direcciones. La original permanece en `.private/android-validation/android-lan-private.png`.
- La app quedó abierta en Mi red, sin búsqueda en curso. La última prueba guardó evidencias identificables por sus destinos de laboratorio; no se borraron evidencias anteriores.
- La prueba no mide cobertura de inventario en routers diferentes ni demuestra detectar todos los clientes Wi-Fi. El enlace OTG teléfono–ESP sigue pendiente.
- Esta versión no se publicó en GitHub.
