package id.mesinrakit

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Layar yang muncul kalau aplikasi berhenti. Jalan di proses terpisah
 * (lihat android:process di manifest) supaya tetap hidup waktu proses
 * utama dimatikan. Isinya jejak error yang bisa disalin pemain.
 */
class LaporActivity : Activity() {

    private fun teks(s: String, ukuran: Float, warna: Int): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = ukuran
        t.setTextColor(warna)
        t.setPadding(0, 8, 0, 8)
        return t
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val isi = try {
            openFileInput(NAMA_BERKAS).bufferedReader().readText()
        } catch (e: Exception) {
            "(belum ada catatan error tersimpan)"
        }
        val jejak = Jejak.baca(this)
        val ringkas = "Jejak terakhir:\n$jejak\n" +
                      "Tanda aman: ${MainActivity.TANDA_AMAN}\n" +
                      if (jejak.contains(MainActivity.TANDA_AMAN)) "  SUDAH tercapai\n"
                      else "  BELUM tercapai (berhenti sebelum frame pertama)\n"

        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF070C14.toInt())
            setPadding(28, 28, 28, 28)
        }
        lay.addView(teks("MESIN RAKIT berhenti", 22f, 0xFFFF5A6E.toInt()))
        lay.addView(teks("Salin isi bawah ini terus kirim ke pembuatnya.", 13f, 0xFF8FA3BC.toInt()))
        lay.addView(teks(ringkas, 12f, 0xFFFFC266.toInt()))

        val rincian = TextView(this).apply {
            text = isi
            textSize = 11.5f
            setTextColor(0xFFD6E2F0.toInt())
            typeface = Typeface.MONOSPACE
            setPadding(0, 16, 0, 16)
        }
        val gulung = ScrollView(this).apply {
            addView(rincian)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        lay.addView(gulung)

        fun tombol(nama: String, aksi: () -> Unit): Button {
            val b = Button(this)
            b.text = nama
            b.setOnClickListener { aksi() }
            return b
        }
        lay.addView(tombol("SALIN") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("error mesin rakit", ringkas + "\n" + isi))
            Toast.makeText(this, "Tersalin ke clipboard", Toast.LENGTH_SHORT).show()
        })
        lay.addView(tombol("HAPUS LOG") {
            try { deleteFile(NAMA_BERKAS) } catch (e: Exception) { }
            finish()
        })
        lay.addView(tombol("COBA MAIN LAGI") {
            // bersihkan jejak supaya masuk ke game, bukan balik ke sini lagi
            Jejak.bersih(this)
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        })
        lay.addView(tombol("TUTUP") { finish() })

        setContentView(lay)
    }

    companion object { const val NAMA_BERKAS = "crash.txt" }
}
