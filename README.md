# YT-DLP Descargador (Android)

Versión Android de `yt-dlp-menu.bat`. Mismos comandos de yt-dlp, mismas opciones,
pensada para el celular.

## Cómo conseguir el APK (sin instalar nada en tu PC)

1. Crea una cuenta gratis en https://github.com y un repositorio nuevo
   (público, para poder descargar el APK directo desde el celular).
2. Sube **todo el contenido de esta carpeta** al repositorio
   (botón *Add file → Upload files*; incluye la carpeta oculta `.github`).
3. Entra a la pestaña **Actions**. La compilación arranca sola
   (tarda unos 5–10 minutos la primera vez).
4. Al terminar, tienes el APK en dos lugares:
   - **Releases → "Última versión"**: descárgalo desde el celular.
   - **Actions → la ejecución → Artifacts**: un .zip con los APK.
5. Instala el APK. Android pedirá permitir "instalar apps desconocidas" para tu navegador.

Qué APK elegir:
- `app-arm64-v8a-release.apk` → casi todos los celulares de los últimos años.
- `app-armeabi-v7a-release.apk` → celulares antiguos de 32 bits.

Ya no se genera el APK universal (pesaba ~4 veces más). El tamaño de cada APK aparece
en el resumen de la ejecución en Actions.

Si prefieres compilar localmente: abre la carpeta con Android Studio y ejecuta
`./gradlew assembleRelease`. Para el emulador de Android Studio (x86_64):
`./gradlew assembleRelease -Pabis=arm64-v8a,armeabi-v7a,x86_64`.

## Equivalencias con el .bat

| Menú del .bat                         | En la app                                              |
|---------------------------------------|--------------------------------------------------------|
| [1] Video (mejor, 1080p…360p)         | Descargar → Video                                      |
| [2] Audio (MP3, AAC, FLAC, OGG, WAV)  | Descargar → Audio                                      |
| [3] Formato personalizado             | Descargar → Formato                                    |
| [4] Ver formatos disponibles          | Descargar → Formato → "Ver formatos disponibles" (tocas los formatos y arma el código, ej. 137+140) |
| [5] Playlist completa                 | Descargar → Playlist (video MP4 o audio MP3, en carpeta con el nombre de la lista) |
| [6] Cambiar carpeta de descarga       | Ajustes → Carpeta de descarga                          |
| [7] Actualizar yt-dlp                 | Ajustes → yt-dlp → Buscar actualización                |

Los argumentos de yt-dlp (`-f`, `--merge-output-format mp4`, `-x --audio-format …`,
`--audio-quality 0`, `--no-playlist`, la plantilla de nombres de la playlist) están
en `app/src/main/java/app/ytdlp/descargador/engine/Models.kt`.

## Extras propios del celular

- Cola de descargas con progreso, tiempo restante y cancelar/reintentar.
- Las descargas siguen con la pantalla apagada (servicio en primer plano + notificación).
- "Compartir" un enlace desde YouTube u otra app → elige *YT-DLP Descargador*.
- Por defecto guarda en `Descargas/YT-DLP`; puedes elegir cualquier otra carpeta.
- Modo claro y oscuro automático.

## Notas

- Requiere Android 10 o superior.
- El motor es la librería `youtubedl-android` (yt-dlp + Python + ffmpeg empaquetados).
  Su versión se cambia en una sola línea (`ytdlpLib`) en `app/build.gradle.kts`.
- Tipografía: Bricolage Grotesque (SIL Open Font License).
- Firmado con la llave de depuración: sirve para instalar y usar, no para Play Store.

## Versión 2.0: novedades y verificación

- **Ajustes** (icono de engranaje arriba a la derecha): tema, color de acento, tamaño de fuente,
  contenedor, carpeta, reintentos, timeout, IPv4, cookies, argumentos extra, motor y diagnóstico.
- **Exportar log de errores**: Ajustes → Mantenimiento y diagnóstico. Genera un .txt compartible con
  versiones (app, yt-dlp, Python, ffmpeg), dispositivo, Android, ABI, ajustes y los últimos errores.
- **Motor actualizado**: youtubedl-android 0.18.1 (agrega QuickJS, que yt-dlp necesita para YouTube) y
  actualización de yt-dlp al primer arranque y una vez al día.
- **Prueba de no solapamiento del aviso de cola**:
  `./gradlew connectedDebugAndroidTest` (necesita emulador o dispositivo).
- Quita `backup-ytdlp-codigo.zip` del repositorio: es un respaldo viejo y no se usa.
