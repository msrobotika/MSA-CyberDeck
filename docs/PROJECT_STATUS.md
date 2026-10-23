# Estado técnico · MSA-CyberDeck

**Revisión:** 2026-10-23 · **Estado:** Aplicación Android de desarrollo; APK 0.37 etiquetada, main con código anterior

## Material y evidencias disponibles

- README e instrucciones de uso; licencia declarada mediante archivo LICENSE.
- Existe [workflow de CI en main](../.github/workflows/ci.yml). Su ejecución no acredita que el código de main coincida con la APK v0.37; esta correspondencia requiere verificar el commit de cada etiqueta/release.
- Capturas de versión Android en documentación: [`docs/screenshots/cyberdeck-dashboard.png`](../docs/screenshots/cyberdeck-dashboard.png), [`docs/screenshots/ciberdeck-network-037.png`](../docs/screenshots/ciberdeck-network-037.png).

## Limitaciones / trabajo pendiente

Para compilar la APK 0.37 se necesita seleccionar la etiqueta o rama correspondiente; main no acredita ese binario.

Un screenshot o una release no sustituyen un test reproducible de los datos, la compilación o el hardware. Especificar el commit de procedencia de cada entrega, mantener los comandos de prueba al día y documentar dependencias.
