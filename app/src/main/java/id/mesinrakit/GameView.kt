package id.mesinrakit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.SparseIntArray
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import id.mesinrakit.core.C
import id.mesinrakit.ui.Hot
import kotlin.math.max

/* ============================================================
   Kanvas utama: loop render, sentuhan multi-jari, dan tombol
   yang bisa ditahan (gas, rem, kopling).
   ============================================================ */
class GameView(ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback {
    lateinit var app: App
    private var thread: Thread? = null
    @Volatile private var jalan = false
    private var vw = 0f
    private var vh = 0f
    private val ptr = SparseIntArray()      // id pointer -> id tombol
    private val ptrHot = HashMap<Int, Hot>()
    private var last = 0L
    var fps = 60f
    private var acc = 0f
    private var frames = 0
    private var sudahTandai = false

    init {
        /* software: hindari crash driver GPU di HP tertentu.
           dulu ada di manifest, terhapus waktu tema dikupas. */
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        holder.addCallback(this)
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        vw = width.toFloat(); vh = height.toFloat()
        if (!::app.isInitialized) return
        jalan = true
        last = System.nanoTime()
        thread = Thread { loop() }.apply { name = "mesin-render"; start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        vw = w.toFloat(); vh = hh.toFloat()
        /* jaga-jaga: kalau surfaceCreated keburu jalan sebelum App siap,
           thread gambar belum hidup. Hidupkan di sini. */
        if (!jalan && ::app.isInitialized) {
            jalan = true
            last = System.nanoTime()
            thread = Thread { loop() }.apply { name = "mesin-render"; start() }
        }
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        jalan = false
        try { thread?.join(1500) } catch (e: Exception) {}
        thread = null
    }

    private fun loop() {
        while (jalan) {
            if (!::app.isInitialized) {
                try { Thread.sleep(16) } catch (e: Exception) {}
                continue
            }
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.25f) dt = 0.25f
            if (dt < 0.004f) dt = 0.004f
            acc += dt; frames++
            if (acc > 0.5f) { fps = frames / acc; acc = 0f; frames = 0 }
            /* Throwable, bukan Exception: OutOfMemoryError dan sejenisnya
               juga harus tertangkap supaya aplikasi gak mati. */
            try { app.update(dt) } catch (e: Throwable) { app.catat(e) }
            var c: Canvas? = null
            try {
                c = holder.lockCanvas()
                if (c != null) {
                    c.drawColor(C.BG)
                    app.draw(c, vw, vh)
                }
            } catch (e: Throwable) {
                app.catat(e)
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c)
                        /* cukup sekali: jangan menulis berkas tiap frame */
                        if (!sudahTandai) {
                            sudahTandai = true
                            Jejak.tandai(context, "11 frame pertama selesai")
                        }
                    } catch (e: Exception) {}
                }
            }
            /* jaga supaya maksimal 120 frame per detik */
            val spent = (System.nanoTime() - now) / 1_000_000L
            val sisa = 8 - spent
            if (sisa > 0) try { Thread.sleep(sisa) } catch (e: Exception) {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!::app.isInitialized) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                val h = app.scene.pick(e.getX(i), e.getY(i))
                ptr.put(pid, h?.id ?: -1)
                if (h != null) ptrHot[pid] = h
                app.sentuh(h, e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val pid = e.getPointerId(i)
                    val h = app.scene.pick(e.getX(i), e.getY(i))
                    val old = ptr.get(pid, -1)
                    val baru = h?.id ?: -1
                    if (old != baru) {
                        val oh = ptrHot[pid]
                        if (oh != null) { app.lepas(oh); ptrHot.remove(pid) }
                        ptr.put(pid, baru)
                        if (h != null) ptrHot[pid] = h
                        app.sentuh(h, e.getX(i), e.getY(i))
                    } else {
                        app.scene.geser(e.getX(i), e.getY(i))
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                val oh = ptrHot[pid]
                try { app.scene.lepas(e.getX(i), e.getY(i)) } catch (ex: Exception) {}
                if (oh != null) app.lepas(oh) else app.lepas(null)
                ptrHot.remove(pid)
                ptr.delete(pid)
            }
        }
        return true
    }

    fun tekan(code: Int, turun: Boolean): Boolean {
        if (!::app.isInitialized) return false
        return (app.scene as? KeyHandler)?.key(code, turun) ?: false
    }

    override fun onKeyDown(code: Int, ev: KeyEvent): Boolean = tekan(code, true) || super.onKeyDown(code, ev)
    override fun onKeyUp(code: Int, ev: KeyEvent): Boolean = tekan(code, false) || super.onKeyUp(code, ev)
}

/** layar yang mau nerima tombol fisik */
interface KeyHandler { fun key(code: Int, turun: Boolean): Boolean }
