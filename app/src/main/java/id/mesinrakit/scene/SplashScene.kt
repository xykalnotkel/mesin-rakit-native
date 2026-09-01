package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.ui.Scene
import kotlin.math.*

/** Logo game dulu, lalu watermark Made XySpace, baru masuk menu. */
class SplashScene(app: App) : Scene(app) {
    private var t = 0f

    override fun enter() { t = 0f }

    override fun update(dt: Float) {
        t += dt
        if (t >= 3.6f) app.pindah("menu")
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        c.drawPaint(grad(0f, 0f, w, h, 0xFF05080E.toInt(), 0xFF0B1220.toInt()))
        val cx = w / 2f
        val cy = h / 2f
        if (t < 1.85f) {
            val a = ((t / 0.45f).coerceIn(0f, 1f))
            val fade = if (t > 1.45f) (1.85f - t) / 0.40f else 1f
            val g = (a * fade).coerceIn(0f, 1f)
            gambarLogo(c, cx, cy - 18f, min(w, h) * 0.16f, g)
            val tp = android.graphics.Paint(T.p(C.TEXT, T.sp(42f), F_BOLD)).apply {
                alpha = (255 * g).toInt()
            }
            val fm = tp.fontMetrics
            c.drawText("MESIN RAKIT", cx, cy + min(w, h) * 0.18f - (fm.ascent + fm.descent) / 2f, tp.apply { textAlign = android.graphics.Paint.Align.CENTER })
        } else {
            val u = ((t - 1.85f) / 0.4f).coerceIn(0f, 1f)
            val fade = if (t > 3.2f) ((3.6f - t) / 0.4f).coerceIn(0f, 1f) else 1f
            val g = u * fade
            c.txc("MADE", cx, cy - 28f, T.sp(14f), C.DIM, F_SEMI)
            val p = android.graphics.Paint(T.p(C.ACC, T.sp(36f), F_BOLD)).apply {
                alpha = (255 * g).toInt()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            c.drawText("XySpace", cx, cy + 18f, p)
            val p2 = android.graphics.Paint(T.p(C.DIM2, T.sp(12f), F_REG)).apply {
                alpha = (200 * g).toInt()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            c.drawText("mesin.xyspace.my.id", cx, cy + 52f, p2)
        }
        if (t > 0.6f) c.txc("ketuk untuk lewati", cx, h * 0.90f, T.sp(12f), C.DIM2, F_REG)
    }

    override fun press(h: id.mesinrakit.ui.Hot?, x: Float, y: Float) { app.pindah("menu") }
    override fun tombolKembali(): Boolean { app.pindah("menu"); return true }
}

fun gambarLogo(c: Canvas, cx: Float, cy: Float, s: Float, alpha: Float = 1f) {
    val a = (255 * alpha).toInt().coerceIn(0, 255)
    val fill = android.graphics.Paint(T.fill(0xFF0B1220.toInt())).apply { this.alpha = a }
    val acc = android.graphics.Paint(T.stroke(C.ACC, s * 0.07f)).apply { this.alpha = a }
    val body = android.graphics.Paint(T.stroke(C.TEXT, s * 0.06f)).apply { this.alpha = a }
    val amber = android.graphics.Paint(T.fill(C.AMBER)).apply { this.alpha = a }
    c.rr(cx - s, cy - s, s * 2f, s * 2f, s * 0.28f, fill)
    c.rrs(cx - s, cy - s, s * 2f, s * 2f, s * 0.28f, acc)
    c.drawCircle(cx - s * 0.42f, cy + s * 0.38f, s * 0.28f, acc)
    c.drawCircle(cx + s * 0.42f, cy + s * 0.38f, s * 0.24f, acc)
    val p = Path()
    p.moveTo(cx - s * 0.42f, cy + s * 0.38f)
    p.lineTo(cx - s * 0.12f, cy - s * 0.12f)
    p.lineTo(cx + s * 0.22f, cy - s * 0.18f)
    p.lineTo(cx + s * 0.42f, cy + s * 0.38f)
    c.drawPath(p, body)
    c.rr(cx - s * 0.04f, cy - s * 0.02f, s * 0.38f, s * 0.28f, s * 0.06f, amber)
}
