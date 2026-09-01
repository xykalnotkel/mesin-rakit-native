package id.mesinrakit

import android.content.Intent
import android.os.Bundle

/**
 * Pintu masuk paling polos. Temanya standar Android, isinya hampir gak ada
 * apa-apa, tujuannya cuma satu: memastikan kita selalu bisa melihat laporan
 * error sekalipun layar utama mati sebelum kode kita jalan.
 */
class BukaActivity : android.app.Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Jejak.tandai(this, "00 pembuka jalan")

        val lapor = Jejak.terputus(this, MainActivity.TANDA_AMAN)
        val tujuan = if (lapor) LaporActivity::class.java else MainActivity::class.java
        try {
            startActivity(Intent(this, tujuan)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                          Intent.FLAG_ACTIVITY_CLEAR_TASK or
                          Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } catch (e: Exception) {
            Jejak.tandai(this, "! pembuka gagal: ${e.javaClass.simpleName}")
        }
        finish()
    }
}
