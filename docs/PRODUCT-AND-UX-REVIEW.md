# Producto y experiencia · revisión local 0.6

## Qué debe resolver

CyberDeck se orienta a trabajo de campo autorizado: observar conectividad y proximidad, comprobar un destino, conservar una copia verificable y producir un informe explicable. La diferenciación propuesta es reunir el teléfono y un sensor abierto en un flujo claro, con resultados reales y una identidad Robotika reconocible.

No se usa una afirmación de «mejor del mercado» como resultado de esta revisión. Requeriría comparación independiente, pruebas reproducibles, cobertura de dispositivos y validación con profesionales. Tampoco se afirma conformidad certificada con OWASP MASVS.

## Fuentes y aplicación

- [NIST SP 800-115](https://csrc.nist.gov/pubs/sp/800/115/final): distingue planificación, ejecución y análisis con informe. En esta iteración se incorpora alcance declarado y notas del autor al informe, selección de evidencias y comprobación de integridad al prepararlo. No se convierte el borrador en un sistema de gestión de proyectos.
- [OWASP MASVS](https://mas.owasp.org/MASVS/) y [MASTG](https://mas.owasp.org/MASTG/): referencias para revisar la seguridad de la propia app, sus datos, comunicaciones e interacción con la plataforma. Son una base de verificación; no una lista de capacidades que un escáner pueda afirmar por tener más botones.
- [Navegación Android](https://developer.android.com/develop/ui/compose/components/navigation-bar): los destinos principales deben estar accesibles de forma persistente. Se sustituyen doce pestañas horizontales por Inicio, Herramientas, Evidencias e Informe. En pantallas anchas se usa un rail lateral.
- [Accesibilidad de Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): controles de al menos 48 dp, semántica, descripción de imágenes y compatibilidad con escalado de texto. Se mantienen componentes Material, texto para estados y controles, efectos pausables y respeto al ajuste de animaciones del sistema.

## Problemas encontrados y cambios

| Problema observado | Cambio implementado |
|---|---|
| Herramientas escondidas fuera de las pestañas visibles | Catálogo agrupado, búsqueda y retorno explícito a herramientas |
| Inicio dominado por explicaciones y botones iguales | Hero Robotika, métricas reales, dos acciones principales y actividad reciente |
| Todo parecía un log de depuración | Jerarquía tipográfica, iconos coherentes, tarjetas, datos técnicos bajo detalles |
| El hash ocupaba más pantalla que el archivo | Nombre, tipo, tamaño y acciones primero; SHA-256 desplegable |
| Dificultad para encontrar archivos recibidos | Se conserva el salto a Evidencias y el filtro por nombre recibido |
| Confirmación de recepción debajo del QR | La oferta de archivo se coloca delante y oculta el QR mientras se decide |
| Explicaciones extensas antes de poder actuar | Instrucción breve y detalles de alcance desplegables |
| Informe sin contexto y con contenido potencialmente alterado | Alcance/notas locales y marcado explícito de copias no verificadas; no se incluyen como diagnóstico verificado |

## Criterios de aceptación de esta iteración

1. Alcanzar cualquier herramienta desde la navegación principal sin desplazar una tira de pestañas.
2. Saber qué se está observando y cuándo corresponde la lectura; no hay puntuaciones de seguridad inventadas.
3. Reabrir, exportar y borrar una copia sin ver primero toda la información técnica.
4. Recuperar el contexto guardado del informe y conservar un borrador ante recreación de pantalla.
5. Probar navegación, búsqueda, almacenamiento y un diagnóstico contra un servidor de laboratorio en el OPPO.
6. Inspeccionar PNG reales del inicio, catálogo, evidencias e informe. Mantener las capturas con datos del propietario en `.private/`.

## Límites que siguen siendo relevantes

La recepción por navegador es HTTP local opcional. En 0.6, un servicio foreground de tipo dataSync mantiene la sesión temporal al cambiar de aplicación; se detiene al cerrar desde recientes, detenerla o vencer su plazo. No reinicia automáticamente. Los avisos de notificación son opcionales; Android puede limitar servicios en segundo plano. La transferencia entre dos CyberDeck usa TLS fijado a la invitación y todavía requiere mantener la app visible. La app no implementa recuperación arbitraria de datos privados, clonación de credenciales, VPN ni un IDS. El USB Android–ESP y más variantes BLE necesitan validación física. El contexto del informe es un borrador único y no asigna automáticamente el archivo global a casos diferentes. El almacenamiento es privado de Android; no se afirma cifrado adicional de cada evidencia ni una cadena de custodia certificada.

El servicio sigue los [tipos foreground de Android](https://developer.android.com/develop/background-work/services/fgs/service-types). Solo arranca mediante una acción del usuario, con aviso genérico sin nombre de archivo ni secreto del enlace. El permiso de notificaciones se solicita mediante un botón opcional, sin bloquear la recepción si se deniega.

Antes de una publicación real faltan una revisión independiente de seguridad, pruebas con otros usuarios y dispositivos, guía de amenazas y un proceso de mantenimiento de dependencias. Esta revisión no autoriza publicar.
