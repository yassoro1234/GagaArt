package com.robotdraw.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.robotdraw.model.Document
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** Summary of a saved project, used by the home screen. */
data class ProjectInfo(val id: String, val name: String, val updated: Long, val thumb: File?)

/**
 * Saves and loads projects as a folder per project:
 *   project.json      – size, background, layer order and properties
 *   layer_<id>.png    – pixels of each layer (keeps transparency)
 *   thumb.jpg         – small preview for the home screen
 */
object Storage {

    private const val MAX_PIXELS = 9_000_000L
    private val io = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var pendingSave: Runnable? = null

    fun projectsDir(context: Context): File =
        File(context.filesDir, "projects").apply { mkdirs() }

    fun projectDir(context: Context, id: String): File =
        File(projectsDir(context), id)

    /** Pixel size actually used for a requested size, scaled down if it would exceed memory limits. */
    fun clampedSize(width: Int, height: Int): Pair<Int, Int> {
        val total = width.toLong() * height.toLong()
        if (total <= MAX_PIXELS) return Pair(width, height)
        val factor = kotlin.math.sqrt(MAX_PIXELS.toDouble() / total.toDouble())
        return Pair(
            (width * factor).roundToInt().coerceAtLeast(1),
            (height * factor).roundToInt().coerceAtLeast(1)
        )
    }

    /** Saves in the background, coalescing rapid edits into one write. */
    fun scheduleSave(context: Context, doc: Document) {
        val app = context.applicationContext
        pendingSave?.let { handler.removeCallbacks(it) }
        val task = Runnable { io.execute { write(app, doc) } }
        pendingSave = task
        handler.postDelayed(task, 1500L)
    }

    /** Saves immediately on the background thread (used when leaving the editor). */
    fun saveNow(context: Context, doc: Document) {
        pendingSave?.let { handler.removeCallbacks(it) }
        val app = context.applicationContext
        io.execute { write(app, doc) }
    }

    private fun write(context: Context, doc: Document) {
        try {
            val dir = projectDir(context, doc.id).apply { mkdirs() }
            val layers = doc.layers.toList()
            val keep = HashSet<String>()

            val layerArray = JSONArray()
            layers.forEach { layer ->
                val file = "layer_${layer.id}.png"
                keep.add(file)
                File(dir, file).outputStream().use { out ->
                    layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                layerArray.put(
                    JSONObject()
                        .put("file", file)
                        .put("name", layer.name)
                        .put("visible", layer.visible)
                        .put("locked", layer.locked)
                        .put("opacity", layer.opacity.toDouble())
                )
            }
            dir.listFiles()?.forEach { f ->
                if (f.name.startsWith("layer_") && f.name !in keep) f.delete()
            }

            val activeIndex = layers.indexOfFirst { it.id == doc.activeId }
            val json = JSONObject()
                .put("name", doc.name)
                .put("width", doc.width)
                .put("height", doc.height)
                .put("background", doc.background)
                .put("active", activeIndex)
                .put("updated", System.currentTimeMillis())
                .put("layers", layerArray)
            File(dir, "project.json").writeText(json.toString())

            writeThumbnail(dir, doc)
        } catch (e: Throwable) {
            // Autosave is best-effort; the next edit will try again.
        }
    }

    private fun writeThumbnail(dir: File, doc: Document) {
        val maxSide = 320f
        val scale = maxSide / maxOf(doc.width, doc.height).toFloat()
        val w = (doc.width * scale).roundToInt().coerceAtLeast(1)
        val h = (doc.height * scale).roundToInt().coerceAtLeast(1)
        val thumb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(thumb)
        canvas.drawColor(if (doc.background != 0) doc.background else Color.WHITE)
        canvas.scale(scale, scale)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        doc.layers.forEach { layer ->
            if (layer.visible) {
                paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
        }
        File(dir, "thumb.jpg").outputStream().use { out ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        thumb.recycle()
    }

    /** Loads a saved project into a new document. Returns null if the folder is missing or broken. */
    fun load(context: Context, id: String): Document? {
        return try {
            val dir = projectDir(context, id)
            val json = JSONObject(File(dir, "project.json").readText())
            val doc = Document(
                id = id,
                width = json.getInt("width"),
                height = json.getInt("height"),
                background = json.getInt("background")
            )
            doc.name = json.optString("name", "Untitled")

            val layers = json.getJSONArray("layers")
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            for (i in 0 until layers.length()) {
                val item = layers.getJSONObject(i)
                val bitmap = BitmapFactory.decodeFile(File(dir, item.getString("file")).path, options)
                    ?: Bitmap.createBitmap(doc.width, doc.height, Bitmap.Config.ARGB_8888)
                val layer = doc.addLayer(item.getString("name"), bitmap)
                layer.visible = item.optBoolean("visible", true)
                layer.locked = item.optBoolean("locked", false)
                layer.opacity = item.optDouble("opacity", 1.0).toFloat()
            }
            val active = json.optInt("active", -1)
            if (active in doc.layers.indices) doc.activeId = doc.layers[active].id
            doc
        } catch (e: Throwable) {
            null
        }
    }

    fun list(context: Context): List<ProjectInfo> {
        return (projectsDir(context).listFiles() ?: emptyArray())
            .filter { File(it, "project.json").exists() }
            .mapNotNull { dir ->
                runCatching {
                    val json = JSONObject(File(dir, "project.json").readText())
                    val thumb = File(dir, "thumb.jpg").takeIf { it.exists() }
                    ProjectInfo(
                        id = dir.name,
                        name = json.optString("name", "Untitled"),
                        updated = json.optLong("updated", dir.lastModified()),
                        thumb = thumb
                    )
                }.getOrNull()
            }
            .sortedByDescending { it.updated }
    }

    fun delete(context: Context, id: String) {
        projectDir(context, id).deleteRecursively()
    }

    /** Flattens visible layers over the background into one bitmap. */
    fun flatten(doc: Document, backgroundOverride: Int?): Bitmap {
        val bitmap = Bitmap.createBitmap(doc.width, doc.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = backgroundOverride ?: doc.background
        if (bg != 0) canvas.drawColor(bg, PorterDuff.Mode.SRC)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        doc.layers.forEach { layer ->
            if (layer.visible) {
                paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
        }
        return bitmap
    }

    /** Exports the document to the device's Pictures/ROBOT DRAW folder. Returns the saved file's URI. */
    fun exportToGallery(context: Context, doc: Document, jpeg: Boolean): Uri? {
        val extension = if (jpeg) "jpg" else "png"
        val safeName = doc.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "artwork" }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${safeName}_${System.currentTimeMillis()}.$extension")
            put(MediaStore.Images.Media.MIME_TYPE, if (jpeg) "image/jpeg" else "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ROBOT DRAW")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = resolver.openOutputStream(uri)?.use { out ->
            val bitmap = if (jpeg) {
                flatten(doc, Color.WHITE)
            } else {
                flatten(doc, null)
            }
            bitmap.compress(
                if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG,
                95,
                out
            )
        } != null
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return if (ok) uri else null
    }

    /** Decodes an image from a content URI and places it, scaled to fit, on a new layer. */
    fun importImage(context: Context, doc: Document, uri: Uri): Boolean {
        val source = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return false

        val layerBitmap = Bitmap.createBitmap(doc.width, doc.height, Bitmap.Config.ARGB_8888)
        val fit = minOf(doc.width.toFloat() / source.width, doc.height.toFloat() / source.height)
        val w = source.width * fit
        val h = source.height * fit
        val left = (doc.width - w) / 2f
        val top = (doc.height - h) / 2f
        val canvas = Canvas(layerBitmap)
        canvas.drawBitmap(
            source,
            null,
            android.graphics.RectF(left, top, left + w, top + h),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        source.recycle()
        doc.addLayer("Imported", layerBitmap)
        return true
    }
}
