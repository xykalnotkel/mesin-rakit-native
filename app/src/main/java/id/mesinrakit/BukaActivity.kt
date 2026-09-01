package id.mesinrakit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.WindowManager
import id.mesinrakit.core.T

/**
 * Satu-satunya layar utama.
 *
 * Dulunya ada layar pembuka dan layar game yang terpisah. Layar pembuka
 * terbukti selalu berhasil jalan, sedangkan layar game mati bahkan sebelum
 * baris pertama kodenya dipanggil. Karena itu sekarang semua digabung di
 * sini: aktivitas yang memakai tema standar, dan setiap langkah dicatat
 * supaya kalau ada yang gagal, langsung kelihatan di langkah ke berapa.
 */
class BukaActivity : android.app.Activity() {

    private lateinit var view: GameView
    private var appRef: App? = null

    private fun bukaLaporan() {
        try {
            startActivity(Intent(this, LaporActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                          Intent.FLAG_ACTIVITY_CLEAR_TASK or
                          Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } catch (e: Exception) { }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        /* Penangkap error dipasang paling awal. */
        val bawaan = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { Jejak.tandai(this, "! error: " + (e.javaClass.simpleName ?: "?")) } catch (x: Exception) { }
            try { appRef?.catat(e) } catch (x: Exception) { }
            try { Lapor.tulis(this, Lapor.jejak(e) + "\n\n" + Jejak.baca(this)) } catch (x: Exception) { }
            bukaLaporan()
            try { bawaan?.uncaughtException(t, e) } catch (x: Exception) { }
            Process.killProcess(Process.myPid())
        }

        /* Kalau percobaan terakhir berhenti sebelum tanda aman, tampilkan
           dulu laporannya supaya pemain bisa membaca dan mengirimnya. */
        if (Jejak.terputus(this, Jejak.TANDA_AMAN)) {
            Jejak.tandai(this, "00a laporan dibuka otomatis")
            bukaLaporan()
            finish()
            return
        }

        Jejak.bersih(this)
        Jejak.tandai(this, "01 mulai")
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
            setContentView(view)
            mintaFokus()
            Jejak.tandai(this, "09 tampilan terpasang")
            a.boot()
            Jejak.tandai(this, Jejak.TANDA_AMAN)
        } catch (e: Throwable) {
            try { appRef?.catat(e) } catch (x: Exception) { }
            Jejak.tandai(this, "! gagal: " + (e.javaClass.simpleName ?: "?"))
            try { Lapor.tulis(this, Lapor.jejak(e) + "\n\n" + Jejak.baca(this)) } catch (x: Exception) { }
            bukaLaporan()
            finish()
        }
    }

    /** Lanskap dipasang lewat kode: kalau ditolak, cukup dicatat. */
    private fun aturLanskap() {
        try {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            Jejak.tandai(this, "03 lanskap sensor")
        } catch (e: Throwable) {
            Jejak.tandai(this, "! lanskap sensor ditolak: ${e.javaClass.simpleName}")
            try {
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Jejak.tandai(this, "03 lanskap biasa")
            } catch (x: Throwable) {
                Jejak.tandai(this, "! lanskap biasa ditolak: ${x.javaClass.simpleName}")
            }
        }
    }

    private fun aturLatar() {
        try {
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#FF070C14")))
            Jejak.tandai(this, "04 latar jendela")
        } catch (e: Throwable) {
            Jejak.tandai(this, "! latar jendela gagal: ${e.javaClass.simpleName}")
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
            Jejak.tandai(this, "! sembunyikan bilah gagal: ${e.javaClass.simpleName}")
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
