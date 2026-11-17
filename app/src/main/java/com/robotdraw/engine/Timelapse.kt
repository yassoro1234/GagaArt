package com.robotdraw.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.robotdraw.engine.video.VideoExporter
import java.io.File
import java.util.concurrent.Executors

/**
 * Time-lapse of a drawing: after every finished stroke a small JPEG snapshot is saved in the
 * project folder. Export turns the snapshots into an MP4 (one snapshot per frame).
 */
object Timelapse {
    const val MAX_FRAMES = 600
    const val MAX_SIDE = 720
    const val FPS = 30

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private fun dir(context: Context, projectId: String): File =
        File(Storage.projectDir(context, projectId), "timelapse").apply { mkdirs() }

    fun frameCount(context: Context, projectId: String): Int =
        dir(context, projectId).list { _, name -> name.endsWith(".jpg") }?.size ?: 0

    /** Saves one snapshot. Called on the UI thread after a stroke ends; flattening is quick at this size. */
    fun capture(context: Context, projectId: String, flattened: Bitmap) {
        val folder = dir(context, projectId)
        val count = folder.list { _, name -> name.endsWith(".jpg") }?.size ?: 0
        if (count >= MAX_FRAMES) return
        val scaled = fit(flattened, MAX_SIDE)
        val file = File(folder, "%05d.jpg".format(count))
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (scaled !== flattened) scaled.recycle()
    }

    fun clear(context: Context, projectId: String) {
        dir(context, projectId).deleteRecursively()
    }

    /** Encodes all snapshots and saves the MP4 to Movies/ROBOT DRAW. [onDone] runs on the UI thread. */
    fun exportVideo(context: Context, projectId: String, projectName: String, onDone: (Uri?, Int) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            var uri: Uri? = null
            var frames = 0
            try {
                val files = dir(app, projectId).listFiles { f -> f.name.endsWith(".jpg") }
                    ?.sortedBy { it.name } ?: emptyList()
                frames = files.size
                if (files.isNotEmpty()) {
                    val first = BitmapFactory.decodeFile(files.first().absolutePath)
                    val w = first.width and 1.inv()
                    val h = first.height and 1.inv()
                    first.recycle()
                    val tmp = File(app.cacheDir, "timelapse_${System.currentTimeMillis()}.mp4")
                    val exporter = VideoExporter(w, h, FPS, bitrate(w, h), tmp.absolutePath)
                    try {
                        for (f in files) {
                            val bmp = BitmapFactory.decodeFile(f.absolutePath) ?: continue
                            exporter.pushFrame(bmp)
                            bmp.recycle()
                        }
                        exporter.finish()
                    } catch (e: Exception) {
                        exporter.abort()
                        throw e
                    }
                    uri = saveToMovies(app, tmp, projectName)
                    tmp.delete()
                }
            } catch (e: Exception) {
                uri = null
            }
            val result = uri
            val count = frames
            main.post { onDone(result, count) }
        }
    }

    private fun bitrate(w: Int, h: Int): Int = (w.toLong() * h * FPS * 0.15).toInt().coerceIn(2_000_000, 12_000_000)

    private fun fit(src: Bitmap, maxSide: Int): Bitmap {
        val scale = maxSide.toFloat() / maxOf(src.width, src.height)
        if (scale >= 1f) return src
        val w = (src.width * scale).toInt() and 1.inv()
        val h = (src.height * scale).toInt() and 1.inv()
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun saveToMovies(context: Context, file: File, projectName: String): Uri? {
        val safe = projectName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "timelapse" }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${safe}_timelapse_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ROBOT DRAW")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return if (ok) uri else null
    }
}
