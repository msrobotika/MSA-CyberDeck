# Validación local: sensor complementario

Fecha: 17 octubre 2026. Sin publicación remota.

- Backup completo: 4194304 bytes. SHA-256 7F091C24A174D72F1AC960EE364017783504AE16AEDAFCA77C8D39ED6B50D2D7. Verificado con verify_flash antes de grabar. Archivo local .private/backups/esp32-20261017/before-cyberdeck.bin.
- Se confirmó el ESP32-D0WD rev1, MAC 0c:b8:15:55:ac:9c, COM4, CH340. Solo se escribió esa placa. No se modificaron eFuses ni microSD.
- Firmware compilado: MSA-WiFi-Sentinel-0.2-CyberDeck. Flash: 924576 bytes de imagen; hashes verificados al escribir.
- Identidad, modo pasivo e injection_enabled=false comprobados por USB desde PC.
- Se exportaron 10 redes. Pause/resume pasó: contador de tramas estable en pausa y creciente tras reanudar (319 al tomar el estado de validación).
- Se cambiaron por protocolo las cinco vistas y se guardaron cinco PNG de framebuffer, 320 × 170. Radar público local sin identificadores; otras capturas en .private/.
- BOOT físico y orientación del panel: requieren confirmación visual del propietario. El framebuffer está en horizontal; no demuestra por sí solo la orientación física.
- APK y clases de pruebas compiladas. Nueve pruebas ejecutadas con JUnitCore: todas pasan. Adaptador Android CH340/OTG pendiente de prueba en móvil.
- Los gráficos Android y la mascota se han compilado; no se presentan como probados en ejecución Android. La vista web está identificada como previa.
- APK actual con mascota: 11554724 bytes. SHA-256 344FCBAED1F036C83BE96C80A30790AC52405304DC0CEB2B8A64C00936A5D7F7. Copia local en dist/MSA-CyberDeck-debug.apk.

## Corrección posterior de pantalla: 0.2.1

La foto del propietario mostró recorte con el perfil 320 × 170. Se instaló el perfil 240 × 135, con el contenido redistribuido y desplazamientos (40, 53) para MV+MX. Referencia del controlador: https://raw.githubusercontent.com/Bodmer/TFT_eSPI/master/TFT_Drivers/ST7789_Rotation.h . El perfil se guardó en NVS.

El propietario confirmó: «Sí, ahora se ve completo». Se capturaron de nuevo las cinco vistas, todas 240 × 135. La prueba física desde PC volvió a pasar pausa/reanudación y exportó 10 redes, con modo pasivo e inyección desactivada. Evidencias privadas: .private/sensor-validation-compact/. La captura compartible de Radar se actualizó; se mantiene pendiente la prueba de Android/OTG y la pulsación física de BOOT.
