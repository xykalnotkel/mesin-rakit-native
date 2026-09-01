package id.mesinrakit

import android.content.Context

/**
 * Penanda jejak startup. Tiap langkah penting dicatat ke berkas internal,
 * jadi kalau aplikasi mati mendadak (bahkan sebelum sempat menyimpan pesan
 * error), kita tetap tahu dia berhenti di langkah ke berapa.
 */
object Jejak {
    private const val NAMA = "jejak.txt"

    /** penanda bahwa aplikasi sudah berhasil sampai titik aman */
    const val TANDA_AMAN = "10 boot selesai"

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

    /** kosong kalau belum pernah ada jejak */
    fun baca(ctx: Context): String = try {
        ctx.openFileInput(NAMA).bufferedReader().readText()
    } catch (e: Exception) { "" }

    /**
     * true kalau percobaan terakhir berhenti sebelum waktunya.
     * Berkas yang tidak ada dianggap aman: itu pemasangan baru, bukan crash.
     *
     * Catatan: tanda "!" di jejak TIDAK dianggap crash. Dulu itu bikin
     * aplikasi terkunci selamanya karena peringatan (lanskap ditolak, dsb)
     * diperlakukan sebagai kegagalan fatal.
     */
    fun terputus(ctx: Context, tanda: String): Boolean {
        val isi = try { baca(ctx) } catch (e: Exception) { "" }
        if (isi.isBlank()) return false
        return !isi.contains(tanda)
    }
}
