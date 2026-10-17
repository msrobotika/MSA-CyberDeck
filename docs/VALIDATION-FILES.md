# Validación de archivos y limpieza · 0.5.0-dev

Trabajo local, sin publicación. OPPO CPH2743 elegido como principal y A15 conservado como segundo equipo de prueba.

## Protocolo y navegador

45 pruebas JVM correctas, incluidas ocho nuevas de HTTP: separación de cabeceras y cuerpo, cabeceras duplicadas, Transfer-Encoding, origen ajeno, dirección de sesión distinta, tamaño máximo, cabeceras excesivas y petición incompleta.

Tres pruebas instrumentadas correctas en el OPPO:

- Página real servida por BrowserReceiver, cargada en Android WebView: selecciona programáticamente un archivo generado de laboratorio y ejecuta el fetch de la página. El receptor confirma y recibe los bytes esperados.
- Rechazo explícito sin invocar el guardado.
- Envío aprobado con contenido esperado y respuesta que contiene SHA-256 calculado en el receptor. Antes se envía una petición con origen ajeno que no guarda datos.

El cliente TCP de pruebas se corrigió para leer Content-Length y no esperar EOF después de una respuesta HTTP completa. No se cambió la validación de orígenes ni se relajó la aprobación.

Estas pruebas usan extremos de navegador y receptor en el OPPO; no sustituyen una prueba manual desde el navegador del A15 al OPPO. No se ha implementado ni probado acceso remoto a todo el almacenamiento, USB MTP ni datos privados de otras apps. El modo navegador es HTTP local sin cifrar con confirmación previa; no se presenta como equivalente a TLS.

## Borrado

Las pruebas de almacenamiento en el A15 verifican que se elimina solo la copia privada, se conserva un original generado y las demás evidencias, el índice persiste y un borrado interrumpido antes de confirmar el índice se restaura. El flujo de interfaz comprueba cancelar y después confirmar sobre un único archivo de prueba filtrado. No se utiliza una evidencia personal para probar el borrado.

Los registros completos se guardan en `.private/android-validation/`; las direcciones temporales no se publican.

Resultado final: tres pruebas de borrado correctas en el A15, incluidas cancelación y confirmación en la interfaz. Tres pruebas del receptor correctas en el OPPO. Compilación y lint de la app completados, y paquete instrumentado final compilado. APK 0.5.0-dev instalada en ambos equipos; receptor del OPPO dejado en reposo, sin servidor abierto.

SHA-256 de `dist/MSA-CyberDeck-debug.apk`: `4f44dc65af545a6e2def7fdb375298719c5cf5929376eb1e7e59cd186154d4f2`.
