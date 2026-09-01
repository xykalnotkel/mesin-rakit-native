package id.mesinrakit.scene

import android.graphics.Canvas
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.model.damageReport
import id.mesinrakit.ui.BTN_HANTU
import id.mesinrakit.ui.metal
import kotlin.math.*

/* Panel ringkasan yang dipakai di beberapa layar */
fun ringkasan(c: Canvas, app: App, x: Float, y: Float, w: Float, h: Float) {
    val s = app.spec
    c.panelJudul("Ringkasan Rakitan", x, y, w, h)
    val px = x + 16f
    var py = y + 46f
    val lw = w - 32f
    val rep = damageReport(app.build)
    c.tx(s.karakter.archetype, px, py, T.sp(17f), C.ACC, F_BOLD); py += 24f
    c.tx(s.karakter.desc, px, py, T.sp(12f), C.DIM, F_REG)
    py = c.wrap(s.karakter.desc, px, py + 16f, lw, T.sp(12f), C.DIM) + 8f
    py += 6f
    fun baris(l: String, v: String, warna: Int = C.TEXT) {
        if (py > y + h - 20f) return
        c.tx(l, px, py, T.sp(12.5f), C.DIM, F_REG)
        c.txr(v, px + lw, py, T.sp(12.5f), warna, F_SEMI)
        py += 19f
    }
    baris("Konfigurasi", if (s.rotary) "Rotary ${s.rotors} rotor" else "${s.cyl} silinder ${s.layout}" + if (s.twoStroke) " 2-tak" else "")
    baris("Isi silinder", "${s.dispCc.toInt()} cc")
    baris("Tenaga puncak", "${String.format("%.1f", s.maxPower)} hp @ ${s.powerRPM.toInt()} rpm")
    baris("Torsi puncak", "${String.format("%.1f", s.maxTorque)} Nm")
    baris("Rasio", "${String.format("%.3f", s.hpPerKg)} hp/kg")
    baris("Berat total", "${String.format("%.0f", s.mass)} kg")
    baris("Roda", "${s.nWheels} roda, r ${String.format("%.2f", s.wheelR)} m")
    baris("Transmisi", if (s.cvt) "CVT matik" else "${s.gears.size} gigi manual")
    baris("Knalpot", s.exh.name)
    baris("CDI", s.limiter.let { if (it == "none") "Tanpa limiter" else "Limiter $it @ ${s.limiterRPM.toInt()}" })
    if (py < y + h - 40f) {
        py += 6f
        c.tx("Kondisi", px, py, T.sp(12.5f), C.DIM, F_REG)
        c.bar(px + 70f, py - 10f, lw - 70f, 10f, s.kondisi,
            if (s.kondisi > 0.75) C.GREEN else if (s.kondisi > 0.4) C.AMBER else C.RED)
        py += 22f
        if (rep.parts > 0) c.tx("${rep.parts} part rusak, servis ${rupiah(id.mesinrakit.model.servisCost(app.build))}",
            px, py, T.sp(12f), C.AMBER, F_SEMI)
    }
}

fun radarKarakter(c: Canvas, s: id.mesinrakit.model.Spec, cx: Float, cy: Float, r: Float) {
    val k = s.karakter
    c.radar(cx, cy, r, doubleArrayOf(k.galak, k.halus, k.bass, k.nyaring, k.respons),
        arrayOf("galak", "halus", "bass", "nyaring", "respons"), C.ACC)
}

/** gambar kendaraan kecil dari susunan part (tampak samping) */
fun gambarKendaraan(c: Canvas, app: App, cx: Float, cy: Float, w: Float, h: Float, skala: Float = 1f) {
    val s = app.spec
    /* kalau sumbu roda belum ada (rakitan kosong), jangan digambar:
       pembagian dengan nol bikin koordinat rusak. */
    if (!s.wheelbase.isFinite() || s.wheelbase <= 0.01) return
    if (!fin(cx, cy, w, h) || w <= 0f || h <= 0f) return
    val rr = (s.wheelR * skala * (w / (s.wheelbase * 1.9f))).toFloat()
    val wb = (s.wheelbase * skala * (w / (s.wheelbase * 1.9f))).toFloat()
    val body = C.PAINTS.getOrElse(app.build.colorIdx) { C.ACC }
    val frameCol = 0xFF6B7686.toInt()
    val ban = 0xFF15191F.toInt()

    /* roda */
    val belakangX = cx - wb / 2f
    val depanX = cx + wb / 2f
    val rodaY = cy + h * 0.20f
    for (rx in floatArrayOf(belakangX, depanX)) {
        c.dot(rx, rodaY, rr, T.fill(ban))
        c.dot(rx, rodaY, rr * 0.55f, T.fill(0xFF2A3444.toInt()))
        c.dot(rx, rodaY, rr * 0.18f, T.fill(0xFF8A97A8.toInt()))
        c.dot(rx, rodaY, rr, T.stroke(0xFF3A4350.toInt(), rr * 0.14f))
    }

    /* rangka: pakai desain custom kalau ada */
    val f = app.build.frame
    val st = T.stroke(frameCol, max(3f, rr * 0.30f))
    if (f != null && f.tubes.isNotEmpty()) {
        val b = f.bounds()
        val bw = (b[2] - b[0]).toFloat().coerceAtLeast(0.01f)
        val bh = (b[3] - b[1]).toFloat().coerceAtLeast(0.01f)
        val sc = min(wb / bw, h * 0.5f / bh)
        val ox = cx - (b[0] + b[2]).toFloat() / 2f * sc
        val oy = cy - (b[1] + b[3]).toFloat() / 2f * sc
        for (t in f.tubes) {
            val a = f.nodes[t.a]; val b2 = f.nodes[t.b]
            val ax = ox + a.x.toFloat() * sc
            val ay = oy - a.y.toFloat() * sc
            val bx = ox + b2.x.toFloat() * sc
            val by = oy - b2.y.toFloat() * sc
            val mx = (ax + bx) / 2f
            val my = (ay + by) / 2f
            val bow = max(abs(a.bow), abs(b2.bow)).toFloat()
            if (bow > 0.01f) {
                val nx = -(by - ay); val ny = (bx - ax)
                val len = hypot(nx.toDouble(), ny.toDouble()).toFloat().coerceAtLeast(0.001f)
                val px2 = mx + nx / len * bow * len * 0.35f
                val py2 = my + ny / len * bow * len * 0.35f
                val path = android.graphics.Path()
                path.moveTo(ax, ay); path.quadTo(px2, py2, bx, by)
                c.drawPath(path, T.stroke(frameCol, max(3f, rr * 0.26f * (t.dia / 0.032).toFloat())))
            } else c.drawLine(ax, ay, bx, by, st)
        }
    } else {
        c.drawLine(belakangX, rodaY, cx, cy - h * 0.02f, st)
        c.drawLine(cx, cy - h * 0.02f, depanX, rodaY, st)
        c.drawLine(belakangX, rodaY, depanX, rodaY - h * 0.06f, st)
    }

    /* body: pakai warna cat */
    c.rr(cx - wb * 0.34f, cy - h * 0.16f, wb * 0.62f, h * 0.24f, rr * 0.35f, T.fill(body))
    /* jok */
    c.rr(cx - wb * 0.30f, cy - h * 0.24f, wb * 0.30f, h * 0.10f, rr * 0.15f, T.fill(0xFF101720.toInt()))
    /* stang */
    c.drawLine(cx + wb * 0.34f, cy - h * 0.14f, cx + wb * 0.46f, cy - h * 0.30f, T.stroke(0xFF8A97A8.toInt(), max(2.5f, rr * 0.16f)))
    /* mesin (blok) */
    val blokW = wb * (if (s.cyl >= 6) 0.42f else if (s.cyl >= 4) 0.34f else 0.24f)
    c.rr(cx - blokW / 2f, cy + h * 0.02f, blokW, h * 0.16f, rr * 0.12f, metal(cx - blokW / 2f, cy, cx + blokW / 2f, cy + h * 0.2f))
    /* knalpot */
    c.drawLine(cx - blokW * 0.4f, cy + h * 0.14f, cx - wb * 0.44f, cy + h * 0.10f,
        T.stroke(0xFFB9C4D4.toInt(), max(3f, rr * 0.22f)))
    c.rr(cx - wb * 0.56f, cy + h * 0.06f, wb * 0.16f, h * 0.09f, rr * 0.12f, metal(cx - wb * 0.56f, cy, cx - wb * 0.4f, cy + h * 0.2f))
}
