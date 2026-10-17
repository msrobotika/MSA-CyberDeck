# Archivos y limpieza · 0.6.0-dev

El OPPO es el equipo principal elegido para estas pruebas. Puedes usar las mismas funciones en otro Android compatible; no se fija un identificador personal en el código.

## Encontrar y utilizar un archivo recibido

Desde 0.5.2, al terminar una recepción se abre Evidencias, se quita el filtro anterior y se busca el nombre recibido. Una importación manual también deja visibles sus resultados aunque hubiera otro filtro activo.

Las copias se guardan dentro de CyberDeck; no aparecen automáticamente en Música, Galería o Descargas. Abrir archivo comprueba tamaño y SHA-256 y entrega acceso temporal de solo lectura a la aplicación compatible que Android elija. No reproduce ni ejecuta archivos automáticamente. Guardar en el móvil abre el selector de destino Android para crear una copia con su nombre original, por ejemplo en Descargas. Conserva la evidencia privada y los originales.

Si no hay una aplicación compatible para abrirlo, utiliza Guardar en el móvil. Las exportaciones son copias independientes: borrarlas o borrar la evidencia no elimina la otra copia.

## Borrar copias

Evidencias muestra el tamaño total de las copias privadas y el de los resultados del filtro. En el menú Más de una tarjeta, Borrar esta copia afecta a una entrada. Gestionar → Borrar resultados visibles afecta a las entradas que coinciden con la búsqueda y filtro actuales; una confirmación indica cantidad y bytes antes de proceder. Conservar copias cancela.

El borrado elimina el archivo privado y su entrada del índice. No modifica el original que seleccionaste ni informes que ya hayas exportado. No hay papelera ni se promete borrado forense de memoria flash. El índice se actualiza con AtomicFile; los borrados interrumpidos se recuperan o completan al cargar el índice. La referencia NFC se limpia si se borra la evidencia correspondiente. Las lecturas aún no guardadas no forman parte del archivo privado.

## Recibir desde otro móvil sin instalar CyberDeck

1. Conecta ambos a una Wi-Fi propia de confianza que permita comunicación entre clientes.
2. En el OPPO abre Herramientas → Recibir archivos → Preparar recepción desde navegador. Lee y acepta la indicación de HTTP sin cifrar.
3. Escanea su QR con la cámara del otro móvil y abre el enlace en su navegador. Allí elige un archivo mediante el selector del sistema y pulsa Proponer archivo al receptor. Puedes cambiar de aplicación en el receptor para compartir el enlace; el servicio temporal mantiene la sesión, con el plazo visible. Activar avisos de recepción es opcional.
4. En el OPPO revisa nombre y tamaño y acepta o rechaza. Si aceptas, se guarda una copia en Evidencias y se calcula su SHA-256.

Un archivo de hasta 16 MiB por sesión. La dirección lleva un secreto aleatorio, caduca a los dos minutos y la aprobación espera hasta 45 segundos. Vuelve al receptor para aprobar. Detener la recepción, cerrar CyberDeck desde recientes o agotar el plazo cierra el servicio. No hay reinicio automático. No hay un servidor externo ni un listado web de tus evidencias. El receptor no expone descargas ni rutas del almacenamiento.

### Si la dirección no abre

Las versiones 0.5 cerraban la sesión al cambiar de aplicación. En 0.6 el receptor temporal continúa mediante un servicio foreground. Un enlace caducado o una sesión detenida no se reactivan al volver: prepara otra. El sistema también puede detener un proceso, por lo que el modo temporal no garantiza recepción permanente.

Los dos móviles deben estar en la misma red local, con acceso entre clientes. Conserva el esquema `http://`; no lo cambies por HTTPS. Los enlaces caducados, una dirección antigua, usar solo datos móviles o el aislamiento del router pueden impedir abrirla. El QR se genera dentro de la app con ZXing core 3.5.4, sin enviar el enlace a un generador externo.

Este modo usa HTTP sin cifrar: una red hostil puede observar o alterar tráfico y la dirección temporal. El SHA-256 se calcula en el receptor, no demuestra la identidad del remitente ni autentica el trayecto. Para documentos confidenciales utiliza la transferencia TLS entre dos CyberDeck. No se abre el servicio en la interfaz de datos móviles. La misma Wi-Fi puede tener aislamiento entre clientes e impedir la transferencia.

El navegador solo accede al archivo seleccionado en ese teléfono. Esta función no permite navegar desde el OPPO por todo el otro Android ni acceder a datos privados de aplicaciones. Para un explorador remoto hace falta que el otro móvil ofrezca un servicio autorizado compatible. Un cable USB por sí solo tampoco convierte Android en un servidor remoto accesible por CyberDeck; MTP/OTG no está implementado aquí.
