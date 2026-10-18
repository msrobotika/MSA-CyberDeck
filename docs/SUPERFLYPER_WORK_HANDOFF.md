# SUPERFLYPER — MS ROBOTIKA
Fecha: 2026-10-18
Rama de trabajo: feature/superflyper-usb-v0-1
Proyecto base: msrobotika/MSA-CyberDeck (original main conservado)

## Objetivo
Android Samsung Galaxy A15 como núcleo de una herramienta modular de diagnóstico y electrónica.
Complemento ESP32 por USB-C OTG: inicialmente protocolo de solo consulta SF1, 115200 8N1.

## Estado de la implementación v0.1
- Android Jetpack Compose sobre la base existente CyberDeck (no migración a Flutter todavía).
- Enumeración de adaptadores USB-UART CH340, CP210x, FTDI y CDC-ACM con usb-serial-for-android 3.11.0.
- Permisos USB solicitados cuando el usuario elige un dispositivo.
- Comunicación en serie con tres comandos de solo lectura: SF1 HELLO, SF1 STATUS y SF1 CAPABILITIES.
- Tramas reales visualizadas en la app, últimas 25 líneas en memoria.
- Botón de prueba explícita para consultar el estado de firmwares MSA-BLE-Scout y MSA-WiFi-Sentinel existentes mediante JSON {\"cmd\":\"status\" sin actualizar su firmware.
- Conservación manual de evidencias JSONL con UTC y SHA-256, en directorio privado del teléfono.
- Firmware demostrador genérico para ESP32 clásico con PlatformIO, sin Wi-Fi ni funciones de transmisión RF.

## Alcance pendiente
BLE Scout, WiFi Sentinel, RogueWatch, NFC nativo, RFID 125 kHz, IR, CC1101, GPIO,
MQTT, Modbus TCP, informes/exportación de evidencias, IA, pruebas físicas y APK de entrega.
No existe integración funcional de esos módulos todavía. Nunca mostrar contadores simulados
ni afirmar que se ha detectado un ataque. La detección USB no prueba que el firmware SF1 esté instalado.

## Hardware: NO FLASH SIN AUDITORÍA
Antes de actualizar una ESP32 con firmware de usuario:
1. Identificar físicamente placa, modelo, chip, tamaño flash, pinout y conversor USB.
2. Leer copia de seguridad COMPLETA de flash y microSD cuando corresponda.
3. Calcular SHA-256 y comparar con segunda lectura; conservar copia fuera del equipo.
4. Confirmar el perfil de placa/firmware y qué binarios serán reemplazados.
5. Sólo entonces ofrecer grabación. La rama no escribe nada en un ESP32 automáticamente.

## Validaciones necesarias
- GitHub Actions: :app:assembleDebug; corregir cualquier error.
- Galaxy A15: reconocer el USB OTG de datos y permiso, sin root.
- ESP32 clásico con CH340/CP210x: HELLO/STATUS y desconexión segura.
- Prueba de reconexión, fallo de cable, consentimiento y persistencia de evidencia.
- Verificar que no se presentan estados ficticios.
- Este desarrollo no dispone de sesión de Work ni de equipo físico conectado en este chat.

## Repositorios relacionados (NO modificar hasta auditar licencias/protocolos)
- msrobotika/MSA-BLE-Scout
- msrobotika/MSA-WiFi-Sentinel
- msrobotika/MSA-RogueWatch
- msrobotika/MSA-ChipSafe

## Siguiente iteración
Contratos de interoperabilidad entre firmwares en modo read-only y app, registro real
de periféricos; luego NFC/Android, BLE integrado, Wi-Fi y módulos ESP32 verificados.
