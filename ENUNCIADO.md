# Enunciado original — Ejercicio 3 AsyncTask

Fuente: `EJERCICIO 3 AsyncTask.pdf` de Programación Multimedia y Dispositivos Móviles.

Partir del ejercicio **«Descarga de fichero mostrando un ProgressDialog con barra»** y realizar estos cambios:

1. La tarea ya no debe devolver el total de bytes descargados. En caso de éxito debe devolver la **ruta de destino del fichero descargado** y, en caso de error, una cadena vacía.
2. Adaptar la lógica posterior a la ejecución para trabajar con esa ruta en lugar del número de bytes.
3. En `MainActivity`, utilizar la ruta devuelta para decodificar el fichero descargado como `Bitmap` y mostrar la imagen en un `ImageView`.

La versión actual conserva también el progreso de descarga y actualiza la solución con concurrencia moderna, almacenamiento específico de la app y gestión de errores/cancelación.

No existe una base de datos asociada.
