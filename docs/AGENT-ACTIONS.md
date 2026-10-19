# Acciones del agente · CiberDeck 0.22

El agente de la aplicación ya no se limita a navegar hacia los módulos interactivos. `AgentActionRuntime` conecta el ejecutor suspendible a los controladores existentes; `AgentAndroidAccess` espera permisos, selectores y decisiones del usuario. Las 30 entradas tienen un adaptador explícito: 29 realizan una operación implementada y `modules` informa del hardware futuro. No significa acceso root ni que todas puedan ejecutarse sin intervención.

## Tareas directas del móvil

«Revisar mi móvil» ejecuta cinco lecturas/comprobaciones con un toque. Conexión, Bluetooth, NFC e inspección de una APK tienen accesos propios. Funcionan sin cargar el modelo ni conectar un ESP32; NFC y APK esperan etiqueta o selector Android. BLE inicia directamente diez segundos de anuncios, sin menú previo. GATT sigue disponible como opción explícita en los planes del catálogo.

Las frases comunes se resuelven mediante una gramática local pequeña y comprobable. Los enlaces y los servicios de red preparan el plan y esperan entradas/alcance antes de ejecutar. Una petición no reconocida ofrece acciones o carga de IA. El planificador local no puede añadir USB/radar cuando la petición no solicita hardware. Se conservan las treinta funciones del catálogo y los selectores/permisos Android.

La cabecera del agente se ha reducido y la tarea principal aparece antes del cuadro de texto. Los ajustes del modelo quedan después de las tareas. «Preparar de nuevo» reinicia la preparación sin repetir operaciones automáticamente.

## Resultados primero

Durante y después de ejecutar, un resumen muestra los pasos con resultado, su síntesis, errores y progreso. El guardado de la sesión está en ese resumen. «Ver detalles de resultados» despliega los métodos y datos completos; las interacciones pendientes mantienen sus controles visibles. Los pasos omitidos, las confirmaciones manuales y la IA se distinguen de resultados de herramientas. «Preparar de nuevo» no ejecuta nada y conserva la huella del modelo del plan.

## Contexto del plan y del analista

El planificador recibe una síntesis fechada del móvil ya observado en la sesión, si existe, además del catálogo de herramientas. Ese dato no acredita conexión actual al ESP ni concede permisos. El contexto se conserva al preparar de nuevo y al guardar el informe.

El analista puede usar situación/objetivo declarados, una instantánea Android opcional tomada al generar y una lectura elegida o el resumen de la última sesión del agente. La entrada exacta y su huella quedan consultables. La calidad del modelo pequeño sigue siendo experimental y puede añadir inferencias sin respaldo: las fuentes se distinguen de la interpretación. [Método y validación 0.22](VALIDATION-022.md).

## Operaciones por entrada

| Entrada | Operación y condiciones |
|---|---|
| phone | Lectura real del móvil mediante API públicas |
| connection | Transporte, direcciones y DNS del estado de conexión |
| compare | Pareja de diagnósticos compatibles y copias verificadas dentro del alcance |
| review | Plan de revisión a partir de lectura y anotaciones locales |
| resources | Una muestra de RAM y batería, no una serie ficticia |
| self_apk | Inspección estática de la APK instalada de CiberDeck |
| inventory | Recuento del índice privado; no verificación de archivos |
| integrity | Verificación de referencias y hashes del trabajo fijado |
| call_status | Rol y número de eventos del filtro, sin solicitar el rol |
| dns | Resolución del dominio/IP introducido y autorizado |
| tcp | Conexión al puerto elegido, sin datos de aplicación |
| web | HEAD HTTP(S), información TLS cuando corresponda |
| link | Inspección sintáctica offline de la URL introducida |
| cidr | Cálculo IPv4/CIDR local |
| ble | Búsqueda real de diez segundos; opcionalmente selección y descubrimiento GATT de un dispositivo autorizado. Sin lectura/escritura de características |
| nfc | Activa el lector hasta treinta segundos y espera una etiqueta física. Sin etiqueta no produce lectura. Sin clonación ni emulación |
| lan | Controlador mDNS existente: quince segundos, hasta 64 servicios, sin barrido de IP |
| magnetic | Seis muestras reales del magnetómetro, con espera máxima de ocho segundos; error si no hay sensor/eventos |
| apk | Selector Android de una APK y análisis estático, sin instalarla |
| usb | Selección de adaptador, permiso USB de Android, consulta de identidad/estado y capacidades anunciadas. No flashea |
| radio | Snapshot WiFi Sentinel real por OTG. Cierra la consola para no abrir el puerto dos veces; requiere firmware compatible |
| exposure | Resuelve el destino, deja elegir la IP, fija la red y comprueba hasta ocho puertos elegidos. Un cambio de red cancela |
| calls | Solicita el rol oficial si falta y lee el registro propio. Sin aceptación del sistema no cambia el rol |
| files | Elegir importar, verificar, exportar o eliminar una copia privada. Selectores Android; borrado con confirmación del archivo exacto |
| works | Consultar, crear, activar o archivar trabajos. No cambia el alcance fijado del plan en curso; preparar otro plan para usar el nuevo trabajo |
| export | Genera el informe del alcance fijado, verifica copias y escribe en el destino elegido mediante Android |
| transfer | Pide una invitación temporal y archivo; envío TLS con certificado fijado y confirmación SHA-256 del receptor |
| receive | Recepción HTTP local temporal con enlace visible durante el paso, aceptación explícita del archivo y espera del guardado. Solo red de confianza; HTTP no cifra el contenido |
| analyst | Interpreta el último resultado del plan o un diagnóstico elegido y verificado. Pregunta explícita, modelo local, fuentes y hash. Texto generado, no medición |
| modules | Consulta informativa de RF/IR/GPIO/PLC y otras capacidades futuras. No ejecuta módulos pendientes |

## Ejecución y registro

1. Pulsar una tarea directa o abrir «Agente» desde el botón de Inicio, su entrada visible en el menú o el catálogo del kit. Preparar con IA o elegir hasta ocho funciones distintas. En 0.19.1 el agente tiene pantalla propia; «Asistente IA» conserva el analista.
2. Completar destinos/puertos y autorización de las herramientas de red. Son campos de la app, no argumentos inventados por el modelo.
3. Revisar y pulsar Ejecutar. Los permisos y selecciones necesarios aparecen en Android; no hay que abrir manualmente la pantalla de cada herramienta.
4. El agente espera la respuesta del controlador. Los errores detienen el resto del plan. Cancelar un selector no completa su paso ni ejecuta el siguiente.
5. Guardar la ejecución como evidencia para conservar estados, fechas, resultados, origen y alcance.

Las acciones de red pueden enviar tráfico o abrir un receptor temporal. La aprobación del plan no sustituye el permiso Android ni la elección de un archivo, adaptador o rol. La solicitud de borrado identifica la copia exacta y mantiene el original del usuario.

La ejecución se detiene al abandonar la pantalla o pasar a segundo plano, salvo durante un selector/permiso Android propio que debe poder devolver su resultado. Se liberan lectores, escáneres y receptores al cancelar. El receptor del agente está ligado al paso activo y no permanece escuchando tras terminar. El enlace secreto temporal no se conserva en el informe.

Los planes permanecen en memoria; no hay ejecución persistente tras cerrar el proceso. No se reintentan automáticamente transferencias, importaciones ni borrados. Los ajustes del modelo se despliegan con «Modelo y ajustes de IA»; si hay un modelo importado, «Cargar IA local» permanece visible sin desplegarlos. La precisión del planificador local es experimental; un catálogo completo no garantiza que entienda cualquier petición.

## Límites

- Acceso a funciones de CiberDeck con los permisos disponibles, no root, shell ni datos privados de otras aplicaciones.
- PC→móvil y PC→ESP no equivalen a Android→ESP por OTG.
- Una respuesta USB identifica lo que anuncia el firmware; no autentica criptográficamente el hardware.
- El magnetómetro mide campo magnético, no radiofrecuencia ni ataques.
- GATT no lee ni escribe características y NFC no copia credenciales.
- Las funciones RF/IR y módulos industriales futuros siguen pendientes.
- El ejecutor anterior de pausa y continuidad se conserva como contrato de compatibilidad; la app 0.19 utiliza el ejecutor de acciones cuando dispone del runtime Android.

Pruebas y limitaciones de la candidata: [VALIDATION-022.md](VALIDATION-022.md). El catálogo 0.18 queda conservado como registro histórico.
