# Catálogo del agente · CiberDeck 0.18

30 entradas: 14 directas y 16 asistidas. Hasta ocho pasos distintos por plan. El catálogo permite preparar una selección manual sin cargar IA; el origen queda registrado. Los módulos futuros siguen siendo información, no capacidades implementadas.

| Identificador | Función | Modo | Pantalla | Resultado del paso |
|---|---|---|---|---|
| phone | Revisar este móvil | Directa local | — | Resultado de la herramienta existente |
| connection | Estado de conexión | Directa local | — | Resultado de la herramienta existente |
| compare | Comparar lecturas | Directa local | — | Resultado de la herramienta existente |
| review | Plan de revisión del móvil | Directa local | — | Resultado de la herramienta existente |
| resources | Medir RAM y batería | Directa local | — | Resultado de la herramienta existente |
| self_apk | Inspeccionar la APK de CiberDeck | Directa local | — | Resultado de la herramienta existente |
| inventory | Resumen de evidencias | Directa local | — | Resultado de la herramienta existente |
| integrity | Verificar copias del trabajo | Directa local | — | Resultado de la herramienta existente |
| call_status | Estado del filtro de llamadas | Directa local | — | Resultado de la herramienta existente |
| dns | Resolver un destino DNS | Directa con destino autorizado | — | Resultado de la herramienta existente |
| tcp | Comprobar un puerto TCP | Directa con destino autorizado | — | Resultado de la herramienta existente |
| web | Inspección HTTP y TLS | Directa con destino autorizado | — | Resultado de la herramienta existente |
| link | Analizar un enlace sin abrirlo | Directa local | — | Resultado de la herramienta existente |
| cidr | Calcular una red IPv4 | Directa local | — | Resultado de la herramienta existente |
| ble | Explorar Bluetooth LE | Asistida | Bluetooth | Diagnóstico guardado de tipo ble, verificado al continuar |
| nfc | Leer una etiqueta NFC | Asistida | NFC | Diagnóstico guardado de tipo nfc, verificado al continuar |
| lan | Descubrir servicios de mi red | Asistida | Mi red | Diagnóstico guardado de tipo lan, verificado al continuar |
| usb | Conectar y consultar el ESP32 | Asistida | USB | Confirmación manual identificada u omisión; no lectura automática |
| radio | Ver el radar Wi-Fi del ESP | Asistida | Radio | Confirmación manual identificada u omisión; no lectura automática |
| magnetic | Medir campo magnético | Asistida | Magnético | Diagnóstico guardado de tipo magnetic, verificado al continuar |
| apk | Elegir e inspeccionar una APK | Asistida | APK | Diagnóstico guardado de tipo apk, verificado al continuar |
| exposure | Auditar un equipo autorizado | Asistida | Equipo | Diagnóstico guardado de tipo exposure, verificado al continuar |
| calls | Configurar avisos de llamadas | Asistida | Llamadas | Diagnóstico guardado de tipo calls, verificado al continuar |
| transfer | Transferir entre dos CiberDeck | Asistida | Sesiones | Confirmación manual identificada u omisión; no lectura automática |
| receive | Recibir desde navegador | Asistida | Recibir | Confirmación manual identificada u omisión; no lectura automática |
| files | Importar, exportar o borrar copias | Asistida | Evidencias | Confirmación manual identificada u omisión; no lectura automática |
| works | Crear o gestionar trabajos | Asistida | Trabajos | Confirmación manual identificada u omisión; no lectura automática |
| export | Preparar y exportar informes | Asistida | Informes | Confirmación manual identificada u omisión; no lectura automática |
| analyst | Interpretar una evidencia con IA | Asistida | IA | Diagnóstico guardado de tipo ai, verificado al continuar |
| modules | Consultar módulos aún pendientes | Asistida | Módulos | Confirmación manual identificada u omisión; no lectura automática |

## Continuidad y procedencia

Una herramienta asistida pausa la secuencia antes de abrir su pantalla. Abrir no inicia ni completa la operación. El acceso Volver al agente conserva el plan. Cuando esa herramienta guarda un diagnóstico del tipo esperado en la misma sesión, el agente ofrece incorporarlo; al continuar vuelve a comprobar pertenencia al índice, tamaño, SHA-256 y tipo. Conserva ID, hash y fecha originales. La verificación acredita la copia, no la frescura, autenticidad ni calidad de la observación. Las interpretaciones IA se muestran como texto generado.

También se puede confirmar manualmente una interacción o saltarla. Ambas opciones se distinguen de resultados verificados. Las operaciones de archivos y transferencias mantienen su interfaz y permisos; una confirmación manual no demuestra por sí sola que se enviara, borrara o exportara un archivo. Los pasos automáticos restantes continúan después. Un error detiene el plan.

## Red, archivos y alcance Android

DNS/TCP/HEAD exigen destinos explícitos en formularios y confirmación de autorización. Cambiar el destino invalida esa confirmación. Las URL y CIDR para cálculos se validan antes de ejecutar. El modelo no crea direcciones, permisos ni argumentos. Los pasos de lectura local no envían tráfico de prueba.

Acceso a los módulos de CiberDeck no significa root ni control universal de Android. Se conserva el aislamiento del sistema y el selector de documentos para archivos externos. Referencias oficiales: [aislamiento de aplicaciones](https://source.android.com/docs/security/app-sandbox) y [acceso a documentos elegidos por el usuario](https://developer.android.com/training/data-storage/shared/documents-files), consultadas el 19 de octubre de 2026.

Estado de pruebas de esta candidata: véase VALIDATION-018.md. La cobertura de rutas del catálogo no acredita pruebas físicas de todos los periféricos.
