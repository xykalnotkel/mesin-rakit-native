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

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideBar()
        Jejak.tandai(this, "2 hideBar selesai")
        T.init(this, resources.displayMetrics.density)
        Jejak.tandai(this, "3 font dimuat")
        view = GameView(this)
        Jejak.tandai(this, "4 GameView dibuat")
        val app = App(this, view)
        view.app = app
        Jejak.tandai(this, "5 App dibuat")
        setContentView(view)
        Jejak.tandai(this, "6 setContentView selesai")
        /* kalau ada error yang lolos, simpan dulu jejaknya, tampilkan layar
           laporan di proses lain, baru matikan proses ini. Jadi pemain
           selalu bisa lihat dan mengirim isi errornya. */
        val bawaan = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { app.catat(e) } catch (x: Exception) { }
            try {
                val i = Intent(this, LaporActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                           Intent.FLAG_ACTIVITY_CLEAR_TASK or
                           Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
            } catch (x: Exception) { }
            try { bawaan?.uncaughtException(t, e) } catch (x: Exception) { }
            Process.killProcess(Process.myPid())
        }
        try { app.boot() } catch (e: Exception) { app.catat(e) }
        Jejak.tandai(this, "7 boot selesai")
    }

    companion object { const val TANDA_AMAN = "8 frame pertama selesai" }

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
        if (fokus) hideBar()
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
