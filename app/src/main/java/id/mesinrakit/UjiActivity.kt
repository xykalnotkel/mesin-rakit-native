package id.mesinrakit

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import id.mesinrakit.core.C
import id.mesinrakit.core.T
import id.mesinrakit.data.PRESETS
import id.mesinrakit.data.pasangPreset
import id.mesinrakit.model.Build as Rakitan
import id.mesinrakit.model.deriveSpec

/**
 * Alat uji mandiri. Muncul sebagai ikon kedua di laci aplikasi.
 * Setiap langkah diuji satu per satu dan hasilnya ditulis di layar,
 * supaya kita tahu persis bagian mana yang bikin masalah di HP tertentu.
 */
class UjiActivity : Activity() {

    private val hasil = StringBuilder()
    private val tangan = Handler(Looper.getMainLooper())
    private var teks: TextView? = null
    private var permukaanSelesai = false

    private fun tulis(baris: String) {
        hasil.append(baris).append('\n')
        teks?.text =hasil.toString()
    }

    private fun langkah(nama: String, blok: () -> String) {
        try {
            tulis("[BAIK]  $nama : ${blok()}")
        } catch (e: Throwable) {
            tulis("[GAGAL] $nama : ${e.javaClass.simpleName}: ${e.message}")
            try { Lapor.tulis(this, hasil.toString(), "uji-mesin-rakit.txt") } catch (x: Exception) { }
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF070C14.toInt())
            setPadding(24, 24, 24, 24)
        }
        val judul = TextView(this).apply {
            text = "UJI MESIN RAKIT"
            textSize = 20f
            setTextColor(0xFFFFC266.toInt())
        }
        teks = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFD6E2F0.toInt())
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        lay.addView(judul)
        lay.addView(ScrollView(this).apply {
            addView(teks)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(lay)

        jalankanUji()
        // hasil uji permukaan datang belakangan
        tangan.postDelayed({
            if (!permukaanSelesai) tulis("[GAGAL] 7 permukaan : tidak pernah dibuat sampai batas waktu")
        }, 6000)
    }

    private fun jalankanUji() {
        langkah("1 activity hidup") {
            "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        }
        langkah("2 berkas internal") {
            openFileOutput("uji.txt", MODE_PRIVATE).use { it.write("tes".toByteArray()) }
            openFileInput("uji.txt").bufferedReader().readText()
        }
        langkah("3 font kustom") {
            val n = listOf("chakra_regular", "chakra_medium", "chakra_semibold", "chakra_bold",
                "barlow_regular", "barlow_semibold", "barlow_bold")
            val tf = n.map { Typeface.createFromAsset(assets, "fonts/$it.ttf") }
            if (tf.any { it == null }) error("ada font yang null")
            "${tf.size} font dimuat"
        }
        langkah("4 tema dan canvas") {
            T.init(this, resources.displayMetrics.density)
            val bm = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
            val c = Canvas(bm)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = LinearGradient(0f, 0f, 0f, 200f, C.BG, C.ACC, Shader.TileMode.CLAMP)
            c.drawPaint(p)
            c.drawRoundRect(android.graphics.RectF(10f, 10f, 200f, 100f), 12f, 12f, T.fill(C.PANEL))
            c.drawText("uji", 20f, 150f, T.p(C.TEXT, 20f, 0))
            val pa = Path(); pa.moveTo(0f, 0f); pa.quadTo(50f, 50f, 100f, 0f)
            c.drawPath(pa, T.stroke(C.ACC, 3f))
            bm.recycle()
            "kanvas ok"
        }
        langkah("5 model mesin") {
            val b = Rakitan()
            pasangPreset(b, PRESETS[0])
            val s = deriveSpec(b)
            "valid=${s.valid} ${"%.1f".format(s.maxPower)} hp ${"%.1f".format(s.maxTorque)} Nm"
        }
        langkah("6 audio track") {
            val sr = 48000
            val t = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .build())
                .setBufferSizeInBytes(sr / 10 * 8)
                .build()
            val ok = t.state == AudioTrack.STATE_INITIALIZED
            val bufer = FloatArray(2048)
            t.play()
            val n = t.write(bufer, 0, bufer.size, AudioTrack.WRITE_BLOCKING)
            t.stop(); t.release()
            if (!ok) error("track tidak siap")
            "state ok, tertulis $n"
        }
        ujiPermukaan()
    }

    private fun ujiPermukaan() {
        try {
            val sv = SurfaceView(this)
            val lay = findViewById<ViewGroup>(android.R.id.content)
            lay.addView(sv, 1, 1)
            sv.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(h: SurfaceHolder) {
                    permukaanSelesai = true
                    tangan.post {
                        langkah("7 permukaan dan gambar") {
                            val c = h.lockCanvas()
                                ?: error("lockCanvas mengembalikan null")
                            c.drawColor(C.BG)
                            c.drawCircle(2f, 2f, 1f, T.fill(C.ACC))
                            h.unlockCanvasAndPost(c)
                            "permukaan siap, gambar berhasil"
                        }
                    }
                }
                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) { }
                override fun surfaceDestroyed(h: SurfaceHolder) { }
            })
        } catch (e: Throwable) {
            tulis("[GAGAL] 7 permukaan : ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
