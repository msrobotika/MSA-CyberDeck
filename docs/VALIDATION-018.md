# CiberDeck 0.18 · Agente ampliado

Candidata de desarrollo instalada en Galaxy A15 (SM-A155F, Android 16/API 36). Conserva paquete, identidad y módulos anteriores. Rama `feature/superflyper-usb-v0-1`, PR #1 en borrador; sin fusión en main ni release.

## Implementación

- Catálogo de 30 entradas: 14 herramientas directas, 16 asistidas. [Tabla completa](AGENT-CAPABILITIES.md).
- Planes de uno a ocho pasos distintos: IA local propone JSON estricto; selección manual disponible sin modelo. El informe distingue el origen.
- Diagnósticos directos: móvil, conexión, comparación, revisión, recursos, propia APK, inventario, integridad, estado de llamadas, DNS, TCP, HTTP/TLS, enlace sin conexión e IPv4/CIDR.
- Los pasos asistidos abren el módulo existente y pausan. BLE/NFC/otras lecturas guardadas pueden incorporarse verificando tipo, índice, tamaño y SHA-256. Conservan la fecha original: verificar la copia no demuestra que la lectura sea reciente.
- Las acciones sin resultado importable requieren confirmación manual diferenciada o pueden omitirse. Ninguna de estas opciones se presenta como medición automática. La interpretación IA se identifica como contenido generado.
- El plan se conserva en memoria al navegar; un acceso permite volver al agente. Continúa después del último paso completado sin repetirlo. No hay persistencia del plan frente a muerte del proceso.
- Destinos, puerto y autorización de red se completan en la app y quedan fijados durante la ejecución. El modelo no inventa argumentos ejecutables. Cancelación y errores detienen los pasos posteriores.
- Informes limitados a 64 KiB; estados de todos los pasos antes de detalles abreviados, procedencia de fuentes y asociación al trabajo activo.

Archivos principales: `AgentPolicy.kt`, `AgentTools.kt`, `AgentPanel.kt`, `DeckSessionModel.kt`, `LocalAiPanel.kt`, `MainActivity.kt` y versión de `app/build.gradle.kts`. Se añadieron pruebas de lógica, dispositivo y recorrido asistido. No se añadieron permisos Android ni se modificó firmware ESP.

## Verificación completada

- Compilación APK y APK de pruebas, compilación de pruebas JVM y lint: correctas.
- 153 pruebas JVM: correctas. Incluyen límites del plan, validación de destinos, pausa, reanudación, procedencia, confirmación manual y omisión.
- 9 pruebas nativas en el A15: correctas (18,361 s). Cinco de ampliación, tres del agente previo y una de inferencia local.
- Catálogo: cobertura de las pantallas existentes; las 16 entradas asistidas pausan sin ejecutar el adaptador directo.
- Lecturas locales reales: revisión, recursos, propia APK, inventario, integridad y estado del filtro de llamadas. Índice de evidencias sin cambios en estas pruebas.
- DNS/TCP/HTTP reales contra un servidor de prueba de loopback del propio A15; sin destino externo. Autorización ausente rechazada. Esta prueba no valida un certificado TLS externo.
- Continuación con copias: fixture aislado y etiquetado de prueba, rechazo de tipo incorrecto, corrupción de igual tamaño y fuente borrada. No representa una lectura física NFC.
- Enlace y CIDR: cálculo sin tráfico con entradas explícitas de prueba.
- Qwen3 local propuso `{"steps":["phone","connection"]}` en 15.100 ms; ambas herramientas produjeron respuestas auténticas. La prueba no valida la interpretación de cualquier petición posible. SHA-256 del modelo: `7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1`.

Los registros detallados se conservan en `.private/android-validation/`, excluidos de Git por privacidad.

## Interfaz y hardware pendientes

El primer lote visual falló con `No Compose hierarchies found` mientras el usuario actual estaba bloqueado (`deviceLocked=1`). Las pruebas nativas finales sí pasaron. No se declara validado el recorrido visual 0.18 ni se presentan capturas anteriores como nuevas.

Quedan preparadas dos pruebas del flujo manual y asistido: APK propia → guardar → volver al agente → incorporar resultado → continuar inventario, y enlace + CIDR. También queda preparada la repetición del recorrido de botones con IA. Requieren desbloquear el A15 normalmente.

No se han probado de nuevo físicamente BLE, NFC, magnetómetro, transferencias, llamadas ni USB OTG en esta revisión. OPPO y ESP no están validados por estas pruebas. RF/IR continúan siendo módulos futuros. La continuidad del plan no inicia transferencias, borrados ni permisos por abrir una pantalla.

## APK instalada

- Versión: `0.18.0-ciberdeck-dev`, código 24.
- Archivo generado: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 65.937.966 bytes.
- SHA-256: `3c5f58c6d41b32e26f90f6011eb1513cfb9fecb57e15d74cf4baf230d55a979a`.
- Instalación ADB correcta y versión leída del A15. Inicio solicitado; registro AndroidRuntime del proceso sin errores, con pantalla bloqueada. Esto no sustituye a una prueba visual.

## Alcance del acceso

El agente utiliza las funciones registradas de CiberDeck. No incluye shell, ejecución de código arbitrario, root ni lectura libre de datos privados de otras apps. Android mantiene su [aislamiento de aplicaciones](https://source.android.com/docs/security/app-sandbox); el acceso a documentos compartidos se realiza mediante los [selectores autorizados del sistema](https://developer.android.com/training/data-storage/shared/documents-files).

Próximo paso físico: desbloquear el A15, completar el recorrido asistido y comprobar parada y periféricos disponibles. La calidad del modelo y las acciones asistidas siguen siendo experimentales; no equivalen a un operador autónomo con acceso ilimitado al móvil.
