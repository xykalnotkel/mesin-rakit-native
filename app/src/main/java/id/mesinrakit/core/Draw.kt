package id.mesinrakit.core

import android.graphics.*
import kotlin.math.*

/* ============ helper gambar di atas Canvas ============ */

fun Canvas.rr(x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
    val rad = min(r, min(w, h) / 2f)
    if (rad <= 0.5f) drawRect(x, y, x + w, y + h, p)
    else drawRoundRect(RectF(x, y, x + w, y + h), rad, rad, p)
}
fun Canvas.rrs(x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
    val old = p.style
    p.style = Paint.Style.STROKE
    val rad = min(r, min(w, h) / 2f)
    if (rad <= 0.5f) drawRect(x, y, x + w, y + h, p)
    else drawRoundRect(RectF(x, y, x + w, y + h), rad, rad, p)
    p.style = old
}

fun Canvas.dot(x: Float, y: Float, r: Float, p: Paint) = drawCircle(x, y, r, p)
fun Canvas.seg(x0: Float, y0: Float, x1: Float, y1: Float, p: Paint) = drawLine(x0, y0, x1, y1, p)

/** teks dengan baseline di y */
fun Canvas.tx(s: String, x: Float, y: Float, size: Float, color: Int, style: Int = F_REG, align: Paint.Align = Paint.Align.LEFT) =
    drawText(s, x, y, T.p(color, size, style, align))

/** teks yang titik tengahnya pas di (cx, cy) */
fun Canvas.txc(s: String, cx: Float, cy: Float, size: Float, color: Int, style: Int = F_REG) {
    val p = T.p(color, size, style, Paint.Align.CENTER)
    val fm = p.fontMetrics
    drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, p)
}
fun Canvas.txr(s: String, rx: Float, cy: Float, size: Float, color: Int, style: Int = F_REG) {
    val p = T.p(color, size, style, Paint.Align.RIGHT)
    val fm = p.fontMetrics
    drawText(s, rx, cy - (fm.ascent + fm.descent) / 2f, p)
}

fun Canvas.panelJudul(judul: String, x: Float, y: Float, w: Float, h: Float) {
    panel(x, y, w, h, 14f)
    tx(judul.uppercase(), x + 14f, y + 20f, T.sp(11f), C.DIM, F_BOLD)
}

fun Canvas.panel(x: Float, y: Float, w: Float, h: Float, r: Float = 14f,
                 fill: Int = C.PANEL, stroke: Int = C.LINE, sw: Float = 1.5f, alpha: Int = 255) {
    val f = T.fill(fill); f.alpha = alpha
    rr(x, y, w, h, r, f); f.alpha = 255
    if (sw > 0f) rrs(x, y, w, h, r, T.stroke(stroke, sw))
}

/** teks yang dipotong rapi sesuai lebar (tanpa emoji, tanpa elipsis aneh) */
fun Canvas.wrap(s: String, x: Float, y: Float, w: Float, size: Float, color: Int, style: Int = F_REG, lh: Float = size * 1.25f): Float {
    val p = T.p(color, size, style)
    var yy = y
    val words = s.split(' ')
    var line = ""
    for (wd in words) {
        val test = if (line.isEmpty()) wd else "$line $wd"
        if (p.measureText(test) > w && line.isNotEmpty()) {
            drawText(line, x, yy, p); yy += lh; line = wd
        } else line = test
    }
    if (line.isNotEmpty()) { drawText(line, x, yy, p); yy += lh }
    return yy - y
}

fun Canvas.bar(x: Float, y: Float, w: Float, h: Float, frac: Double, color: Int, back: Int = C.LINE) {
    T.fill(back).let { rr(x, y, w, h, h / 2f, it) }
    val fw = (w * clamp(frac, 0.0, 1.0)).toFloat()
    if (fw > 1f) T.fill(color).let { rr(x, y, fw, h, h / 2f, it) }
}

/** radar karakter: nilai 0..1 tiap sumbu */
fun Canvas.radar(cx: Float, cy: Float, r: Float, vals: DoubleArray, labels: Array<String>, color: Int) {
    val n = vals.size
    val grid = T.stroke(C.LINE, 1.2f)
    for (g in 1..4) {
        val rr = r * g / 4f
        val path = Path()
        for (i in 0 until n) {
            val a = -Math.PI / 2 + i * TAU / n
            val px = (cx + cos(a) * rr).toFloat()
            val py = (cy + sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close(); drawPath(path, grid)
    }
    val line = T.stroke(C.LINE2, 1f)
    for (i in 0 until n) {
        val a = -Math.PI / 2 + i * TAU / n
        seg(cx, cy, (cx + cos(a) * r).toFloat(), (cy + sin(a) * r).toFloat(), line)
    }
    val path = Path()
    for (i in 0 until n) {
        val a = -Math.PI / 2 + i * TAU / n
        val rr = r * clamp(vals[i], 0.0, 1.0).toFloat()
        val px = (cx + cos(a) * rr).toFloat()
        val py = (cy + sin(a) * rr).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    val fp = T.fill(color); fp.alpha = 70; drawPath(path, fp); fp.alpha = 255
    drawPath(path, T.stroke(color, 2f))
    for (i in 0 until n) {
        val a = -Math.PI / 2 + i * TAU / n
        val px = (cx + cos(a) * (r + 16f)).toFloat()
        val py = (cy + sin(a) * (r + 16f)).toFloat()
        txc(labels[i], px, py, T.sp(11f), C.DIM, F_MED)
    }
}

/** gradasi vertikal */
fun grad(x: Float, y: Float, w: Float, h: Float, c0: Int, c1: Int): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(0f, y, 0f, y + h, c0, c1, Shader.TileMode.CLAMP)
    return p
}

/** segitiga penunjuk */
fun Canvas.tri(cx: Float, cy: Float, w: Float, h: Float, rot: Float, p: Paint) {
    save(); translate(cx, cy); rotate(rot)
    val path = Path()
    path.moveTo(0f, -h / 2f); path.lineTo(w / 2f, h / 2f); path.lineTo(-w / 2f, h / 2f); path.close()
    drawPath(path, p); restore()
}

/** lingkaran progress buat rpm */
fun Canvas.arcGauge(cx: Float, cy: Float, r: Float, frac: Double, color: Int, back: Int = C.LINE2, w: Float = 8f) {
    val start = 135f
    val sweep = 270f
    drawArc(RectF(cx - r, cy - r, cx + r, cy + r), start, sweep, false, T.stroke(back, w))
    drawArc(RectF(cx - r, cy - r, cx + r, cy + r), start, (sweep * clamp(frac, 0.0, 1.0)).toFloat(), false, T.stroke(color, w))
}

/** potong teks biar muat */
fun fit(s: String, w: Float, size: Float, style: Int = F_REG): String {
    val p = T.p(C.TEXT, size, style)
    if (p.measureText(s) <= w) return s
    var out = s
    while (out.length > 1 && p.measureText(out + "...") > w) out = out.dropLast(1)
    return out + "..."
}
