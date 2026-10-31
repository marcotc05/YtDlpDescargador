package app.ytdlp.descargador.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * yt-dlp descarga primero a una carpeta temporal privada de la app y, al terminar,
 * los archivos se copian a la carpeta final:
 *  - por defecto: Descargas/YT-DLP (MediaStore, sin permisos especiales)
 *  - o una carpeta elegida por la persona (Storage Access Framework)
 */
object Storage {
    const val DEFAULT_LABEL = "Descargas/YT-DLP"

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences

    private val _treeUri = MutableStateFlow<Uri?>(null)
    val treeUri: StateFlow<Uri?> = _treeUri.asStateFlow()

    private val _label = MutableStateFlow(DEFAULT_LABEL)
    val label: StateFlow<String> = _label.asStateFlow()

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_TREE, null)?.let { Uri.parse(it) }
        if (saved != null && hasPermission(saved)) {
            _treeUri.value = saved
            _label.value = folderName(saved)
        } else if (saved != null) {
            prefs.edit().remove(KEY_TREE).apply()
        }
    }

    fun setCustomFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            app.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (ignored: Exception) {
        }
        prefs.edit().putString(KEY_TREE, uri.toString()).apply()
        _treeUri.value = uri
        _label.value = folderName(uri)
    }

    fun resetFolder() {
        prefs.edit().remove(KEY_TREE).apply()
        _treeUri.value = null
        _label.value = DEFAULT_LABEL
    }

    private fun hasPermission(uri: Uri): Boolean =
        app.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }

    private fun folderName(uri: Uri): String =
        DocumentFile.fromTreeUri(app, uri)?.name ?: "Carpeta elegida"

    // ---------- Temporales ----------

    private fun tempRoot(): File = File(app.getExternalFilesDir(null) ?: app.filesDir, "tmp")

    fun newTempDir(jobId: Long): File =
        File(tempRoot(), "job$jobId-${System.currentTimeMillis()}").also { it.mkdirs() }

    fun cleanTemp() {
        tempRoot().deleteRecursively()
    }

    // ---------- Guardado final ----------

    /** Copia todo lo descargado en [root] a la carpeta final y devuelve los destinos. */
    fun saveAll(root: File): List<Uri> {
        val files = root.walkTopDown()
            .filter { it.isFile && !isTemporary(it) }
            .toList()
        val tree = _treeUri.value
        return files.map { file ->
            val relative = file.parentFile?.relativeTo(root)?.path.orEmpty()
            if (tree != null) saveToTree(file, relative, tree) else saveToDownloads(file, relative)
        }
    }

    private fun isTemporary(f: File): Boolean {
        val n = f.name
        return n.endsWith(".part") || n.endsWith(".ytdl") || n.endsWith(".temp")
    }

    private fun mimeOf(f: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase())
            ?: "application/octet-stream"

    private fun saveToDownloads(file: File, relative: String): Uri {
        val resolver = app.contentResolver
        val path = buildString {
            append(Environment.DIRECTORY_DOWNLOADS).append("/YT-DLP")
            if (relative.isNotEmpty()) append("/").append(relative.replace('\\', '/'))
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeOf(file))
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas")
        try {
            val out = resolver.openOutputStream(uri) ?: error("No se pudo escribir en Descargas")
            out.use { o -> file.inputStream().use { it.copyTo(o, BUFFER) } }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    private fun saveToTree(file: File, relative: String, tree: Uri): Uri {
        var dir = DocumentFile.fromTreeUri(app, tree) ?: error("La carpeta elegida ya no está disponible")
        relative.split('/', '\\').filter { it.isNotEmpty() }.forEach { part ->
            dir = dir.findFile(part)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(part)
                ?: error("No se pudo crear la carpeta $part")
        }
        val target = dir.createFile(mimeOf(file), file.name) ?: error("No se pudo crear el archivo")
        val out = app.contentResolver.openOutputStream(target.uri) ?: error("No se pudo escribir el archivo")
        out.use { o -> file.inputStream().use { it.copyTo(o, BUFFER) } }
        return target.uri
    }

    private const val KEY_TREE = "tree_uri"
    private const val BUFFER = 256 * 1024
}
