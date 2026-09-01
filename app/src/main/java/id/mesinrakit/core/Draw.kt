package id.mesinrakit.core

import android.graphics.*
import kotlin.math.*

/* ============ helper gambar di atas Canvas ============ */

/**
 * Skia (mesin gambar Android) bisa mati secara native kalau dikasih
 * koordinat NaN atau tak terhingga, dan itu tidak bisa ditangkap
 * try/catch biasa. Semua helper di bawah dicek dulu.
 */
fun fin(vararg v: Float): Boolean {
    for (x in v) if (x.isNaN() || x.isInfinite()) return false
    return true
}

fun Canvas.rr(x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
    if (!fin(x, y, w, h, r)) return
    val rad = min(r, min(w, h) / 2f)
    if (rad <= 0.5f) drawRect(x, y, x + w, y + h, p)
    else drawRoundRect(RectF(x, y, x + w, y + h), rad, rad, p)
}
fun Canvas.rrs(x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
    if (!fin(x, y, w, h, r)) return
    val old = p.style
    p.style = Paint.Style.STROKE
    val rad = min(r, min(w, h) / 2f)
    if (rad <= 0.5f) drawRect(x, y, x + w, y + h, p)
    else drawRoundRect(RectF(x, y, x + w, y + h), rad, rad, p)
    p.style = old
}

fun Canvas.dot(x: Float, y: Float, r: Float, p: Paint) { if (fin(x, y, r)) drawCircle(x, y, r, p) }
fun Canvas.seg(x0: Float, y0: Float, x1: Float, y1: Float, p: Paint) {
    if (fin(x0, y0, x1, y1)) drawLine(x0, y0, x1, y1, p)
}

/** teks dengan baseline di y */
fun Canvas.tx(s: String, x: Float, y: Float, size: Float, color: Int, style: Int = F_REG, align: Paint.Align = Paint.Align.LEFT) {
    if (!fin(x, y, size) || size <= 0f) return
    drawText(s, x, y, T.p(color, size, style, align))
}

/** teks yang titik tengahnya pas di (cx, cy) */
fun Canvas.txc(s: String, cx: Float, cy: Float, size: Float, color: Int, style: Int = F_REG) {
    if (!fin(cx, cy, size) || size <= 0f) return
    val p = T.p(color, size, style, Paint.Align.CENTER)
    val fm = p.fontMetrics
    drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, p)
}
fun Canvas.txr(s: String, rx: Float, cy: Float, size: Float, color: Int, style: Int = F_REG) {
    if (!fin(rx, cy, size) || size <= 0f) return
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
    if (!fin(x, y, w, h, r)) return
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
    if (!fin(x, y, w, h) || frac.isNaN()) return
    T.fill(back).let { rr(x, y, w, h, h / 2f, it) }
    val fw = (w * clamp(frac, 0.0, 1.0)).toFloat()
    if (fw > 1f) T.fill(color).let { rr(x, y, fw, h, h / 2f, it) }
}

/** radar karakter: nilai 0..1 tiap sumbu */
fun Canvas.radar(cx: Float, cy: Float, r: Float, vals: DoubleArray, labels: Array<String>, color: Int) {
    val n = vals.size
    /* butuh minimal 3 sumbu, kalau tidak sudutnya jadi NaN */
    if (n < 3 || !fin(cx, cy, r) || r <= 0f) return
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
    if (fin(y, h)) p.shader = LinearGradient(0f, y, 0f, y + h, c0, c1, Shader.TileMode.CLAMP)
    return p
}

/** segitiga penunjuk */
fun Canvas.tri(cx: Float, cy: Float, w: Float, h: Float, rot: Float, p: Paint) {
    if (!fin(cx, cy, w, h, rot)) return
    save(); translate(cx, cy); rotate(rot)
    val path = Path()
    path.moveTo(0f, -h / 2f); path.lineTo(w / 2f, h / 2f); path.lineTo(-w / 2f, h / 2f); path.close()
    drawPath(path, p); restore()
}

/** cluster speedo + rpm + kondisi mesin */
fun Canvas.cluster(cx: Float, cy: Float, r: Float, kmh: Double, rpmN: Double, kondisi: Double, rpmTeks: String) {
    if (!fin(cx, cy, r) || r < 20f) return
    rr(cx - r * 1.35f, cy - r * 0.95f, r * 2.7f, r * 1.9f, r * 0.18f, T.fill(0xEE0B1220.toInt()))
    rrs(cx - r * 1.35f, cy - r * 0.95f, r * 2.7f, r * 1.9f, r * 0.18f, T.stroke(C.LINE2, 1.6f))
    /* speedo kiri */
    val sx = cx - r * 0.55f
    drawCircle(sx, cy, r * 0.72f, T.fill(0xFF101820.toInt()))
    drawCircle(sx, cy, r * 0.72f, T.stroke(C.LINE2, 3f))
    val vmax = 180.0
    val frac = clamp(kmh / vmax, 0.0, 1.0)
    for (i in 0..9) {
        val a = Math.toRadians(135.0 + i * 27.0)
        val inner = r * 0.55f
        val outer = r * 0.68f
        seg((sx + cos(a) * inner).toFloat(), (cy + sin(a) * inner).toFloat(),
            (sx + cos(a) * outer).toFloat(), (cy + sin(a) * outer).toFloat(),
            T.stroke(if (i >= 8) C.RED else C.DIM, 2f))
    }
    val na = Math.toRadians(135.0 + frac * 243.0)
    seg(sx, cy, (sx + cos(na) * r * 0.50).toFloat(), (cy + sin(na) * r * 0.50).toFloat(), T.stroke(C.ACC, 3.2f))
    txc("${kmh.toInt()}", sx, cy + 4f, T.sp(22f), C.TEXT, F_NUM_BOLD)
    txc("km/jam", sx, cy + r * 0.38f, T.sp(10f), C.DIM, F_REG)
    /* rpm kanan */
    val rx = cx + r * 0.62f
    arcGauge(rx, cy - 4f, r * 0.42f, rpmN, if (rpmN > 0.92) C.RED else if (rpmN > 0.75) C.AMBER else C.ACC, C.LINE2, 6f)
    txc(rpmTeks, rx, cy - 6f, T.sp(13f), C.TEXT, F_NUM_SEMI)
    txc("rpm", rx, cy + 14f, T.sp(10f), C.DIM, F_REG)
    /* kondisi */
    tx("MESIN", cx - r * 1.18f, cy + r * 0.78f, T.sp(10f), C.DIM, F_BOLD)
    bar(cx - r * 0.72f, cy + r * 0.68f, r * 1.85f, 10f, kondisi,
        if (kondisi > 0.75) C.GREEN else if (kondisi > 0.4) C.AMBER else C.RED)
}

/** lingkaran progress buat rpm */
fun Canvas.arcGauge(cx: Float, cy: Float, r: Float, frac: Double, color: Int, back: Int = C.LINE2, w: Float = 8f) {
    if (!fin(cx, cy, r, w) || r <= 0f || frac.isNaN()) return
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
