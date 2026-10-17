# Primer uso de CyberDeck en Android

La app tiene funciones propias aunque no haya un ESP conectado.

1. En **Resumen**, pulsa **Comprobar mi conexión**. Muestra el estado que Android conoce de la red activa, sus direcciones y DNS. No es una prueba de velocidad ni de seguridad.
2. **Abrir herramientas DNS / TCP** lleva a Red. Escribe un dominio o IP de tu laboratorio y pulsa **Resolver DNS / IP**. Para comprobar un único puerto, indica también su número y pulsa **Comprobar puerto**.
3. **Revisar certificado y cabeceras web** lleva a Web. Introduce una URL completa con https:// y pulsa **Revisar URL**. Muestra el resultado HTTP, el certificado y las cabeceras recibidas. No sigue redirecciones ni descarga el cuerpo del sitio.
4. Al terminar, pulsa **Guardar resultado como evidencia**. En **Evidencias**, **Verificar copias** vuelve a calcular SHA-256. En **Informes**, **Guardar informe** abre el selector de destino Android.
5. Las pestañas superiores se desplazan horizontalmente y permanecen visibles al bajar por los resultados.

## Sensor opcional

**USB** necesita el ESP enchufado directamente al móvil mediante un cable/adaptador OTG de datos. Tener ambos conectados al PC no crea ese enlace. Pulsa **Actualizar USB**, **Leer WiFi Sentinel** y concede el permiso. **Radio** utiliza esa lectura, no inventa redes ni hace un escaneo autónomo desde el teléfono.

El enlace físico teléfono–ESP sigue pendiente de validación. BLE, VPN y detección avanzada de intrusiones no están implementados en esta app.

## Pruebas locales

Las evidencias de validación apuntan a 127.0.0.1 y contienen consultas contra un servidor temporal de prueba. Ese servidor se cierra al terminar: volver a consultar su puerto después puede dar conexión rechazada. No indica un fallo de Internet.

## Inventario local

En **Mi red**, inicia una búsqueda manual de 15 segundos para descubrir servicios anunciados. Consulta [MI-RED.md](MI-RED.md) para interpretar resultados y límites.
