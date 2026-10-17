# Mi red — inventario de servicios anunciados

Este módulo descubre anuncios DNS-SD/mDNS mediante Android NSD. No realiza barridos de direcciones ni conecta a los puertos descubiertos.

## Uso

1. Conecta el móvil a tu red o a una red cuyo inventario tengas autorizado.
2. Abre **Mi red** y pulsa **Descubrir servicios · 15 s**.
3. Observa las fichas y el gráfico. Puedes detener antes; la sesión termina a los 15 segundos. Pasar la app a segundo plano también detiene la sesión.
4. Pulsa **Guardar inventario como evidencia**. El JSON incorpora alcance, fechas, tipos consultados, servicios e incidencias. Puede verificarse con SHA-256 e incluirse en el informe.

## Qué significan los datos

- Nombre y tipo: publicados por el servicio; no autentican al propietario o fabricante.
- Dirección y puerto: proceden de resolver el anuncio. No prueban que el puerto acepte conexiones.
- Un equipo puede publicar varios servicios. El contador representa servicios, no equipos.
- Las barras muestran cuántos anuncios se observaron por tipo; no representan dirección, distancia ni nivel de amenaza.
- Se consultan `_http._tcp.`, `_https._tcp.`, `_ipp._tcp.`, `_ipps._tcp.`, `_googlecast._tcp.` y `_workstation._tcp.`.
- Hasta 64 servicios por sesión. Las resoluciones se encolan para evitar peticiones concurrentes incompatibles. Una resolución incompleta se conserva como tal.
- Sin resultados no significa red vacía o segura. Aislamiento entre clientes, filtrado multicast, servicios silenciosos y restricciones del sistema pueden limitar la búsqueda.
- No se muestran MAC, versiones de sistema o fabricantes deducidos sin una fuente fiable. No se buscan conversaciones ni archivos privados.

Los anuncios recibidos son datos no confiables y se muestran como texto. No se abren enlaces ni se ejecutan comandos procedentes de ellos. Las capturas y evidencias reales pueden contener nombres o direcciones de equipos: se mantienen privadas salvo revisión del propietario.

## Referencia técnica

[Network Service Discovery de Android](https://developer.android.com/develop/connectivity/wifi/use-nsd).

La implementación de BLE, sesiones entre móviles y obtención de información interna con consentimiento son módulos futuros; no están habilitados por este inventario.
