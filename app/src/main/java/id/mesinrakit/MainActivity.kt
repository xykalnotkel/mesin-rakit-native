package id.mesinrakit

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import id.mesinrakit.core.T

class MainActivity : android.app.Activity() {
    private lateinit var view: GameView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideBar()
        T.init(this, resources.displayMetrics.density)
        view = GameView(this)
        val app = App(this, view)
        view.app = app
        setContentView(view)
        app.boot()
    }

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
        view.app.audio.start()
    }

    override fun onPause() {
        view.app.audio.pause()
        view.app.simpan()
        super.onPause()
    }

    override fun onDestroy() {
        view.app.audio.stop()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (!view.app.kembali()) super.onBackPressed()
    }
}
