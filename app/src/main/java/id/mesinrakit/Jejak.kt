package id.mesinrakit

import android.content.Context

/**
 * Penanda jejak startup. Tiap langkah penting dicatat ke berkas internal,
 * jadi kalau aplikasi mati mendadak (bahkan sebelum sempat menyimpan pesan
 * error), kita tetap tahu dia berhenti di langkah ke berapa.
 */
object Jejak {
    private const val NAMA = "jejak.txt"

    fun bersih(ctx: Context) {
        try { ctx.deleteFile(NAMA) } catch (e: Exception) { }
    }

    fun tandai(ctx: Context, langkah: String) {
        try {
            val baris = "$langkah\n"
            if (sudah(ctx, langkah)) return
            ctx.openFileOutput(NAMA, Context.MODE_APPEND).use { it.write(baris.toByteArray()) }
        } catch (e: Exception) { }
    }

    private fun sudah(ctx: Context, langkah: String): Boolean = try {
        baca(ctx).contains(langkah)
    } catch (e: Exception) { false }

    fun baca(ctx: Context): String = try {
        ctx.openFileInput(NAMA).bufferedReader().readText()
    } catch (e: Exception) { "(belum ada jejak)" }

    /** true kalau startup terakhir tidak sampai tanda ini */
    fun terputus(ctx: Context, tanda: String): Boolean = try {
        val isi = baca(ctx)
        isi.isNotBlank() && !isi.contains(tanda)
    } catch (e: Exception) { false }
}
