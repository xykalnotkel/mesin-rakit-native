package id.mesinrakit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.WindowManager
import id.mesinrakit.core.T

class MainActivity : android.app.Activity() {
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

        /* Penangkap error dipasang sedini mungkin supaya semua kejadian,
           termasuk yang terjadi di thread gambar dan thread audio, sempat
           menyimpan jejaknya sebelum proses dimatikan. */
        val bawaan = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { Jejak.tandai(this, "! error: " + (e.javaClass.simpleName ?: "?")) } catch (x: Exception) { }
            try { appRef?.catat(e) } catch (x: Exception) { }
            bukaLaporan()
            try { bawaan?.uncaughtException(t, e) } catch (x: Exception) { }
            Process.killProcess(Process.myPid())
        }

        /* kalau percobaan terakhir berhenti sebelum mencapai tanda aman,
           tampilkan dulu layar laporan supaya pemain bisa membacanya. */
        if (Jejak.terputus(this, TANDA_AMAN)) {
            Jejak.tandai(this, "0 laporan dibuka otomatis")
            startActivity(Intent(this, LaporActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }
        Jejak.bersih(this)
        Jejak.tandai(this, "1 onCreate mulai")
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            hideBar()
            Jejak.tandai(this, "2 hideBar selesai")
            T.init(this, resources.displayMetrics.density)
            Jejak.tandai(this, "3 font dimuat")
            view = GameView(this)
            Jejak.tandai(this, "4 GameView dibuat")
            val app = App(this, view)
            appRef = app
            view.app = app
            Jejak.tandai(this, "5 App dibuat")
            setContentView(view)
            Jejak.tandai(this, "6 setContentView selesai")
            try { app.boot() } catch (e: Throwable) { app.catat(e) }
            Jejak.tandai(this, "7 boot selesai")
        } catch (e: Throwable) {
            /* gagal di tengah jalan: simpan, tampilkan laporannya, lalu tutup
               dengan rapi supaya tidak muncul dialog kosong. */
            try { appRef?.catat(e) } catch (x: Exception) { }
            Jejak.tandai(this, "! onCreate gagal: " + (e.javaClass.simpleName ?: "?"))
            bukaLaporan()
            finish()
        }
    }

    companion object { const val TANDA_AMAN = "7 boot selesai" }

    private fun hideBar() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
    }

    override fun onWindowFocusChanged(fokus: Boolean) {
        super.onWindowFocusChanged(fokus)
        if (fokus) try { hideBar() } catch (e: Throwable) { appRef?.catat(e) }
    }

    override fun onResume() {
        super.onResume()
        /* audio hanya dinyalakan lagi kalau kita memang sedang di layar
           yang butuh suara. Jadi gangguan audio gak pernah menghentikan
           aplikasi waktu pertama kali dibuka. */
        try {
            val butuh = view.app.scene is id.mesinrakit.scene.DynoScene ||
                        view.app.scene is id.mesinrakit.scene.JalanScene
            if (butuh) view.app.mulaiAudio()
        } catch (e: Exception) { }
    }

    override fun onPause() {
        /* audio dimatikan total, bukan sekadar dijeda:
           track yang dijeda bikin thread nulis gagal terus. */
        if (::view.isInitialized) {
            try { view.app.audio.stop() } catch (e: Exception) { }
            try { view.app.simpan() } catch (e: Exception) { }
        }
        super.onPause()
    }

    override fun onDestroy() {
        /* jangan mengakses view kalau onCreate sempat gagal, nanti malah
           menutupi error aslinya. */
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
}
