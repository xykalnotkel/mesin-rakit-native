package id.mesinrakit

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Penyimpan laporan ke folder Unduhan. Tidak butuh izin apa pun di
 * Android 10 ke atas, dan isinya bisa dibuka pemain lewat aplikasi
 * Berkas, lalu dibagikan ke kita.
 */
object Lapor {

    const val NAMA = "crash-mesin-rakit.txt"

    /** ubah error jadi teks jejaknya */
    fun jejak(e: Throwable): String = try {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        sw.toString()
    } catch (x: Exception) { e.toString() }

    fun tulis(ctx: Context, isi: String, nama: String = NAMA) {
        if (Build.VERSION.SDK_INT < 29) return
        try {
            val nilai = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nama)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, nilai)
                ?: return
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(isi.toByteArray()) }
            val selesai = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            ctx.contentResolver.update(uri, selesai, null, null)
        } catch (e: Exception) { }
    }
}
