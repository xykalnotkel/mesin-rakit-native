package id.mesinrakit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import id.mesinrakit.core.T

/**
 * Satu-satunya layar utama.
 *
 * PENTING: jangan pernah menutup aktivitas ini cuma karena jejak startup
 * sebelumnya belum sampai tanda aman. Itu yang bikin aplikasi "terhenti
 * terus" tiap dibuka: sekali gagal, seterusnya langsung finish() tanpa
 * sempat masuk game.
 */
class BukaActivity : android.app.Activity() {

    private lateinit var view: GameView
    private var appRef: App? = null

    private fun bukaLaporan() {
        try {
            /* Jangan CLEAR_TASK: itu bisa membunuh seluruh task, termasuk
               laporan yang baru mau muncul, apalagi laporan jalan di proses
               terpisah. Cukup NEW_TASK supaya tetap kelihatan. */
            startActivity(Intent(this, LaporActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val ctx = applicationContext
        val bawaan = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { Jejak.tandai(ctx, "! error: " + (e.javaClass.simpleName ?: "?")) } catch (x: Exception) { }
            try { appRef?.catat(e) } catch (x: Exception) { }
            try { Lapor.tulis(ctx, Lapor.jejak(e) + "\n\n" + Jejak.baca(ctx)) } catch (x: Exception) { }
            try {
                startActivity(Intent(ctx, LaporActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (x: Exception) { }
            /* Jangan panggil handler bawaan: itu yang memunculkan dialog
               sistem "telah terhenti". Laporan kita sudah cukup. */
            if (bawaan != null && bawaan.javaClass.name.startsWith("id.mesinrakit")) {
                try { bawaan.uncaughtException(t, e) } catch (x: Exception) { }
            }
            try { Thread.sleep(250) } catch (x: Exception) { }
            Process.killProcess(Process.myPid())
        }

        /* Jejak lama cuma buat catatan, BUKAN alasan menutup aplikasi.
           Kalau session sebelumnya putus, tetap masuk game. Menu sudah
           punya panel error kalau ada log tersimpan. */
        val putus = Jejak.terputus(this, Jejak.TANDA_AMAN)
        Jejak.bersih(this)
        Jejak.tandai(this, "01 mulai")
        if (putus) Jejak.tandai(this, "01b session sebelumnya putus, tetap lanjut")

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Jejak.tandai(this, "02 flag jendela")
            aturLanskap()
            aturLatar()
            sembunyikanBar()
            T.init(this, resources.displayMetrics.density)
            Jejak.tandai(this, "06 font dimuat")
            view = GameView(this)
            Jejak.tandai(this, "07 GameView dibuat")
            val a = App(this, view)
            appRef = a
            view.app = a
            Jejak.tandai(this, "08 App dibuat")
            /* boot dulu supaya scene siap sebelum SurfaceView hidup
               dan thread gambar mulai jalan. */
            a.boot()
            Jejak.tandai(this, "08b boot selesai")
            setContentView(view)
            mintaFokus()
            Jejak.tandai(this, "09 tampilan terpasang")
            Jejak.tandai(this, Jejak.TANDA_AMAN)
        } catch (e: Throwable) {
            try { appRef?.catat(e) } catch (x: Exception) { }
            Jejak.tandai(this, "! gagal: " + (e.javaClass.simpleName ?: "?"))
            try { Lapor.tulis(this, Lapor.jejak(e) + "\n\n" + Jejak.baca(this)) } catch (x: Exception) { }
            /* Jangan finish(): tampilkan pesan di layar yang sama supaya
               aplikasi tidak "terhenti" di mata sistem. */
            tampilGagal(e)
        }
    }

    /** Kalau game gagal disiapkan, tetap ada layar yang hidup. */
    private fun tampilGagal(e: Throwable) {
        try {
            val tv = TextView(this).apply {
                text = "MESIN RAKIT gagal membuka permainan.\n\n" +
                    (e.javaClass.simpleName ?: "?") + ": " + (e.message ?: "") +
                    "\n\nKetuk di mana saja untuk membuka laporan."
                setTextColor(0xFFD6E2F0.toInt())
                textSize = 16f
                setPadding(48, 48, 48, 48)
                setBackgroundColor(0xFF070C14.toInt())
                gravity = Gravity.CENTER
                setOnClickListener { bukaLaporan() }
            }
            setContentView(tv)
        } catch (x: Exception) {
            bukaLaporan()
        }
    }

    /** Lanskap dipasang lewat kode: kalau ditolak, cukup dicatat. */
    private fun aturLanskap() {
        try {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            Jejak.tandai(this, "03 lanskap sensor")
        } catch (e: Throwable) {
            Jejak.tandai(this, "03 lanskap sensor ditolak: ${e.javaClass.simpleName}")
            try {
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Jejak.tandai(this, "03 lanskap biasa")
            } catch (x: Throwable) {
                Jejak.tandai(this, "03 lanskap biasa ditolak: ${x.javaClass.simpleName}")
            }
        }
    }

    private fun aturLatar() {
        try {
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#FF070C14")))
            Jejak.tandai(this, "04 latar jendela")
        } catch (e: Throwable) {
            Jejak.tandai(this, "04 latar jendela gagal: ${e.javaClass.simpleName}")
        }
    }

    private fun sembunyikanBar() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(android.view.WindowInsets.Type.statusBars()
                        or android.view.WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
            Jejak.tandai(this, "05 bilah disembunyikan")
        } catch (e: Throwable) {
            Jejak.tandai(this, "05 sembunyikan bilah gagal: ${e.javaClass.simpleName}")
        }
    }

    override fun onWindowFocusChanged(fokus: Boolean) {
        super.onWindowFocusChanged(fokus)
        if (fokus) try { sembunyikanBar() } catch (e: Throwable) { appRef?.catat(e) }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::view.isInitialized) {
                val s = view.app.scene
                if (s is id.mesinrakit.scene.DynoScene || s is id.mesinrakit.scene.JalanScene) {
                    view.app.mulaiAudio()
                }
            }
        } catch (e: Exception) { }
    }

    override fun onPause() {
        if (::view.isInitialized) {
            try { view.app.audio.stop() } catch (e: Exception) { }
            try { view.app.simpan() } catch (e: Exception) { }
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::view.isInitialized) {
            try { view.app.audio.stop() } catch (e: Exception) { }
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::view.isInitialized && view.app.kembali()) return
        super.onBackPressed()
    }

    /* Tombol fisik ditangani langsung oleh GameView. Di sini cukup
       memastikan tampilannya pegang fokus supaya tombolnya nyampe. */
    private fun mintaFokus() {
        try {
            view.isFocusableInTouchMode = true
            view.requestFocus()
        } catch (e: Exception) { }
    }
}
