# Validación local de experiencia · 0.6.0-dev

Fecha: 17 de octubre de 2026. Equipo: OPPO CPH2743, Android API 36. El A15 no está conectado durante esta iteración. Cambios locales, sin publicación.

## Resultado

- Compilación de aplicación y pruebas Android completada. `lintDebug`: cero errores y once avisos. Los avisos incluyen versiones de dependencias, icono monocromo pendiente, compatibilidad de atributos y el TrustManager de la transferencia con certificado fijado; no equivalen a una revisión de seguridad independiente.
- Cincuenta pruebas JUnit de lógica pasan mediante JUnitCore: hash, sensor, diagnósticos, protocolo TLS/invitación, archivo y comparación NFC, recepción HTTP, QR y contexto del informe.
- Once pruebas instrumentadas de regresión pasan en el OPPO: dos del servicio receptor, dos de nueva experiencia, una de conexión/DNS/TCP/HEAD/archivo, dos de historial e integridad, tres de borrado y una de controles de radio sin búsqueda automática.
- Una prueba adicional de observación NFC completada y recreación de Activity pasa. Las otras dos pruebas de experiencia vuelven a pasar en esa ejecución. Son doce pruebas Android distintas, no catorce casos distintos.

## Qué se comprobó

El flujo de conexión emplea servidores de laboratorio en loopback; no escanea destinos ajenos. Comprueba DNS/IP, conexión TCP, una petición HEAD, guardado, verificación y navegación tras resultados largos. El historial abre un diagnóstico NFC generado después de recrear la actividad. El borrado prueba cancelación, confirmación sobre un filtro de prueba y recuperación de una transacción interrumpida.

El receptor HTTP se mantiene con Activity en estado CREATED: sirve la página y sigue escuchando hasta detenerlo. Otra prueba propone bytes generados con la pantalla de la app detenida, comprueba que aún no se han guardado, vuelve para aprobar, verifica la respuesta SHA-256 y los bytes privados exactos y confirma el cierre del servicio. La prueba usa la pila HTTP real del teléfono; no sustituye la prueba física anterior entre dos móviles ni cubre todos los fabricantes y restricciones de batería.

Una copia de diagnóstico alterada se rechaza al abrir y se marca NO VERIFICADA en el informe, sin incluirla como contenido de diagnóstico verificado. El resultado NFC retenido se inyecta como fixture en el ViewModel; esta prueba cubre recreación de pantalla, no una nueva lectura física de etiqueta.

## Inspección visual

Se capturaron PNG reales en Android de Inicio, Herramientas, Evidencias e Informe. Se revisaron jerarquía, paleta oscura, botones, navegación y campos. Se mantienen en `.private/ux-review/`, excluidos de Git, porque pueden mostrar archivos y estados del propietario. Las capturas de laboratorio no son resultados de una auditoría real.

## Límites y trabajo posterior

La iteración no valida USB OTG móvil–ESP, nuevas etiquetas físicas, transferencia TLS con el segundo teléfono desconectado, TalkBack, todos los tamaños de texto ni un conjunto amplio de pantallas. El informe tiene un único borrador, sin asignación de evidencias a varios casos. HTTP local sigue sin cifrar; para contenido sensible existe el modo TLS entre dos CyberDeck, que sigue necesitando primer plano.

Faltan revisión independiente de seguridad, pruebas con usuarios profesionales, más dispositivos y medición de accesibilidad antes de presentar el producto como listo para distribución general. No se afirma liderazgo de mercado ni certificación MASVS.

Logs privados: `ux-06-final-build.log`, `ux-06-final-regression.txt`, `ux-06-retention-test.txt` y `ux-06-junit.txt`, dentro de `.private/android-validation/`.
