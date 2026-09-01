package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.model.Spec
import id.mesinrakit.model.damageReport
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

fun radarKarakter(c: Canvas, s: Spec, cx: Float, cy: Float, r: Float) {
    val k = s.karakter
    c.radar(cx, cy, r, doubleArrayOf(k.galak, k.halus, k.bass, k.nyaring, k.respons),
        arrayOf("galak", "halus", "bass", "nyaring", "respons"), C.ACC)
}

/** gambar kendaraan kecil dari susunan part (tampak samping, Y body ke atas) */
fun gambarKendaraan(c: Canvas, app: App, cx: Float, cy: Float, w: Float, h: Float, skala: Float = 1f) {
    val s = app.spec
    if (!s.wheelbase.isFinite() || s.wheelbase <= 0.01) return
    if (!fin(cx, cy, w, h) || w <= 0f || h <= 0f) return
    val wb = s.wheelbase
    val scale = (w / (wb * 1.85)).toFloat() * skala
    val rodaY = cy + h * 0.22f
    val rpm = (app.vehicle?.rpm ?: max(app.lastRPM, s.idleRPM)).coerceAtLeast(400.0)
    val spin = app.t * rpm / 60.0 * 0.35
    fun px(ox: Double, oy: Double) = cx + (ox * scale).toFloat()
    fun py(ox: Double, oy: Double) = rodaY - (oy * scale).toFloat()
    gambarRakitSamping(
        c, app, ::px, ::py, scale, rpm,
        px(-wb / 2, 0.0), py(-wb / 2, 0.0),
        px(wb / 2, 0.0), py(wb / 2, 0.0),
        spin, spin, false
    )
}

/**
 * Motor/mobil terakit di posisi aslinya. Koordinat body: X maju, Y atas, meter.
 * px/py mengubah ke piksel layar.
 */
fun gambarRakitSamping(
    c: Canvas, app: App,
    px: (Double, Double) -> Float, py: (Double, Double) -> Float,
    ppm: Float, rpm: Double,
    rearSx: Float, rearSy: Float, frontSx: Float, frontSy: Float,
    spinR: Double, spinF: Double, rider: Boolean
) {
    val s = app.spec
    val b = app.build
    val body = C.PAINTS.getOrElse(b.colorIdx) { C.ACC }
    val frameCol = 0xFF7C8798.toInt()
    val wb = s.wheelbase
    val r = s.wheelR
    val rPx = max(6f, (r * ppm).toFloat())
    val crank = app.t * rpm / 60.0 * TAU
    fun sw(m: Double) = max(1.4f, (m * ppm).toFloat())
    fun kotak(x0: Double, y0: Double, x1: Double, y1: Double, p: android.graphics.Paint) {
        val path = Path()
        path.moveTo(px(x0, y0), py(x0, y0))
        path.lineTo(px(x1, y0), py(x1, y0))
        path.lineTo(px(x1, y1), py(x1, y1))
        path.lineTo(px(x0, y1), py(x0, y1))
        path.close()
        c.drawPath(path, p)
    }

    fun roda(sx: Float, sy: Float, spin: Double, cakram: Boolean) {
        if (!fin(sx, sy, rPx)) return
        c.dot(sx, sy, rPx, T.fill(0xFF12161C.toInt()))
        c.dot(sx, sy, rPx * 0.62f, T.fill(0xFF2A3444.toInt()))
        val nJari = 8
        for (k in 0 until nJari) {
            val a = spin + k * TAU / nJari
            c.drawLine(sx, sy,
                sx + (cos(a) * rPx * 0.56).toFloat(),
                sy + (sin(a) * rPx * 0.56).toFloat(),
                T.stroke(0xFF9AA7B8.toInt(), max(1f, rPx * 0.06f)))
        }
        c.dot(sx, sy, rPx * 0.16f, T.fill(0xFFB9C4D4.toInt()))
        c.dot(sx, sy, rPx, T.stroke(0xFF3A4350.toInt(), max(1.4f, rPx * 0.12f)))
        if (cakram) c.dot(sx, sy, rPx * 0.38f, T.stroke(0xFF8A97A8.toInt(), max(1.2f, rPx * 0.05f)))
    }
    roda(rearSx, rearSy, spinR, false)
    roda(frontSx, frontSy, spinF, true)
    if (s.isCar || s.nWheels >= 4) {
        roda(px(-wb * 0.12, 0.0), py(-wb * 0.12, 0.0), spinR, false)
        roda(px(wb * 0.12, 0.0), py(wb * 0.12, 0.0), spinF, true)
    }

    val f = b.frame
    if (f != null && f.tubes.isNotEmpty() && f.nodes.size >= 2) {
        val bd = f.bounds()
        val bw2 = (bd[2] - bd[0]).coerceAtLeast(0.01)
        val bh2 = (bd[3] - bd[1]).coerceAtLeast(0.01)
        val sc = min(wb / bw2, (r * 2.4) / bh2)
        val ox0 = -(bd[0] + bd[2]) / 2
        val oy0 = -(bd[1] + bd[3]) / 2 + r * 0.9
        for (t in f.tubes) {
            if (t.a !in f.nodes.indices || t.b !in f.nodes.indices) continue
            val a = f.nodes[t.a]; val b2 = f.nodes[t.b]
            c.drawLine(
                px((a.x + ox0) * sc, (a.y + oy0) * sc),
                py((a.x + ox0) * sc, (a.y + oy0) * sc),
                px((b2.x + ox0) * sc, (b2.y + oy0) * sc),
                py((b2.x + ox0) * sc, (b2.y + oy0) * sc),
                T.stroke(frameCol, sw(t.dia * 1.4)))
        }
    } else if (!s.isCar) {
        val st = T.stroke(frameCol, sw(0.028))
        c.drawLine(px(-wb * 0.18, r * 1.55), py(-wb * 0.18, r * 1.55),
            px(wb * 0.28, r * 1.70), py(wb * 0.28, r * 1.70), st)
        c.drawLine(px(wb * 0.28, r * 1.70), py(wb * 0.28, r * 1.70),
            px(wb * 0.38, r * 1.15), py(wb * 0.38, r * 1.15), st)
        c.drawLine(px(wb * 0.38, r * 1.15), py(wb * 0.38, r * 1.15),
            px(0.02, r * 0.55), py(0.02, r * 0.55), st)
        c.drawLine(px(0.02, r * 0.55), py(0.02, r * 0.55),
            px(-wb * 0.18, r * 1.55), py(-wb * 0.18, r * 1.55), st)
        c.drawLine(px(-wb * 0.18, r * 1.55), py(-wb * 0.18, r * 1.55),
            px(-wb * 0.08, r * 0.55), py(-wb * 0.08, r * 0.55), T.stroke(frameCol, sw(0.022)))
    }

    if (s.isCar) {
        kotak(-wb * 0.52, r * 0.35, wb * 0.52, r * 1.15, T.fill(body))
        kotak(-wb * 0.18, r * 1.10, wb * 0.28, r * 1.85, T.fill(body))
        kotak(-wb * 0.12, r * 1.35, wb * 0.22, r * 1.72, T.fill(0xFF1A2433.toInt()))
        gambarMesinHidup(c, px, py, ppm, s, crank, 0.22, r * 0.70)
        return
    }

    c.drawLine(px(0.04, r * 0.55), py(0.04, r * 0.55), rearSx, rearSy, T.stroke(frameCol, sw(0.032)))
    c.drawLine(px(-wb * 0.12, r * 1.35), py(-wb * 0.12, r * 1.35),
        px(-wb * 0.22, r * 0.70), py(-wb * 0.22, r * 0.70), T.stroke(C.ACC, sw(0.016)))
    c.drawLine(px(wb * 0.32, r * 1.85), py(wb * 0.32, r * 1.85), frontSx, frontSy, T.stroke(0xFFC3CCD8.toInt(), sw(0.024)))
    c.drawLine(px(wb * 0.38, r * 1.80), py(wb * 0.38, r * 1.80),
        px(wb * 0.50, 0.0), py(wb * 0.50, 0.0), T.stroke(0xFFA8B4C4.toInt(), sw(0.018)))

    gambarMesinHidup(c, px, py, ppm, s, crank, 0.02, r * 0.72)

    val knal = T.stroke(0xFFC5D0DE.toInt(), sw(0.022))
    c.drawLine(px(-0.06, r * 0.42), py(-0.06, r * 0.42),
        px(-wb * 0.42, r * 0.38), py(-wb * 0.42, r * 0.38), knal)
    kotak(-wb * 0.58, r * 0.28, -wb * 0.40, r * 0.52,
        metal(px(-wb * 0.58, r * 0.28), py(-wb * 0.58, r * 0.52),
            px(-wb * 0.40, r * 0.28), py(-wb * 0.40, r * 0.28)))

    c.drawLine(px(0.06, r * 0.48), py(0.06, r * 0.48), rearSx, rearSy - rPx * 0.35f, T.stroke(0xFF8A97A8.toInt(), sw(0.010)))
    c.drawLine(px(0.06, r * 0.32), py(0.06, r * 0.32), rearSx, rearSy + rPx * 0.20f, T.stroke(0xFF8A97A8.toInt(), sw(0.010)))

    val tank = Path()
    tank.moveTo(px(-0.04, r * 1.48), py(-0.04, r * 1.48))
    tank.quadTo(px(0.16, r * 1.92), py(0.16, r * 1.92), px(0.32, r * 1.62), py(0.32, r * 1.62))
    tank.lineTo(px(0.28, r * 1.28), py(0.28, r * 1.28))
    tank.quadTo(px(0.10, r * 1.18), py(0.10, r * 1.18), px(-0.02, r * 1.32), py(-0.02, r * 1.32))
    tank.close()
    c.drawPath(tank, T.fill(body))
    c.drawPath(tank, T.stroke(0x33000000, 1.4f))

    val jok = Path()
    jok.moveTo(px(-wb * 0.32, r * 1.52), py(-wb * 0.32, r * 1.52))
    jok.quadTo(px(-wb * 0.18, r * 1.72), py(-wb * 0.18, r * 1.72), px(-0.02, r * 1.58), py(-0.02, r * 1.58))
    jok.lineTo(px(-0.02, r * 1.40), py(-0.02, r * 1.40))
    jok.lineTo(px(-wb * 0.30, r * 1.38), py(-wb * 0.30, r * 1.38))
    jok.close()
    c.drawPath(jok, T.fill(0xFF101720.toInt()))

    c.drawLine(px(wb * 0.30, r * 1.82), py(wb * 0.30, r * 1.82),
        px(wb * 0.42, r * 2.05), py(wb * 0.42, r * 2.05), T.stroke(0xFF9AA7B8.toInt(), sw(0.016)))
    c.dot(px(wb * 0.46, r * 1.55), py(wb * 0.46, r * 1.55), rPx * 0.22f, T.fill(0xFFF5E6A8.toInt()))
    c.dot(px(wb * 0.46, r * 1.55), py(wb * 0.46, r * 1.55), rPx * 0.22f, T.stroke(0xFF8A97A8.toInt(), 1.4f))

    if (fin(frontSx, frontSy, rPx)) {
        c.drawArc(frontSx - rPx * 1.05f, frontSy - rPx * 1.15f, frontSx + rPx * 1.05f, frontSy + rPx * 0.55f,
            200f, 140f, false, T.stroke(body, sw(0.018)))
    }
    if (fin(rearSx, rearSy, rPx)) {
        c.drawArc(rearSx - rPx * 1.05f, rearSy - rPx * 1.05f, rearSx + rPx * 1.05f, rearSy + rPx * 0.45f,
            210f, 130f, false, T.stroke(body, sw(0.016)))
    }

    if (rider) {
        val hx = px(-wb * 0.04, r * 2.15)
        val hy = py(-wb * 0.04, r * 2.15)
        val hcol = s.driver.color
        c.dot(hx, hy, rPx * 0.28f, T.fill(hcol))
        c.drawLine(hx, hy + rPx * 0.22f, px(-wb * 0.08, r * 1.55), py(-wb * 0.08, r * 1.55), T.stroke(hcol, sw(0.028)))
        c.drawLine(hx, hy + rPx * 0.10f, px(wb * 0.38, r * 2.00), py(wb * 0.38, r * 2.00), T.stroke(hcol, sw(0.016)))
        c.drawLine(px(-wb * 0.08, r * 1.55), py(-wb * 0.08, r * 1.55),
            px(0.10, r * 0.55), py(0.10, r * 0.55), T.stroke(0xFF2A3444.toInt(), sw(0.016)))
    }
}

/** blok mesin cutaway: piston, stang piston, kruk berputar sesuai rpm */
fun gambarMesinHidup(
    c: Canvas,
    px: (Double, Double) -> Float, py: (Double, Double) -> Float,
    ppm: Float, s: Spec, crank: Double,
    ox: Double, oy: Double
) {
    val n = s.cyl.coerceIn(1, 6)
    val stroke = 0.055 + s.dispCc / 8000.0
    val bore = 0.032 + s.dispCc / 14000.0
    val crankR = stroke * 0.42
    fun sw(m: Double) = max(1.2f, (m * ppm).toFloat())
    val case = metal(
        px(ox - 0.10, oy - 0.04), py(ox - 0.10, oy + 0.10),
        px(ox + 0.12, oy - 0.04), py(ox + 0.12, oy - 0.04)
    )

    val karter = Path()
    karter.moveTo(px(ox - 0.11, oy - 0.02), py(ox - 0.11, oy - 0.02))
    karter.lineTo(px(ox + 0.13, oy - 0.02), py(ox + 0.13, oy - 0.02))
    karter.lineTo(px(ox + 0.11, oy + 0.12), py(ox + 0.11, oy + 0.12))
    karter.lineTo(px(ox - 0.09, oy + 0.12), py(ox - 0.09, oy + 0.12))
    karter.close()
    c.drawPath(karter, case)
    c.drawPath(karter, T.stroke(0xFF3A4350.toInt(), 1.2f))

    if (s.rotary) {
        val cx = px(ox, oy + 0.04); val cy = py(ox, oy + 0.04)
        val rr = (0.07 * ppm).toFloat()
        c.dot(cx, cy, rr, T.fill(0xFF2A323C.toInt()))
        val path = Path()
        for (i in 0 until 3) {
            val a = crank + i * TAU / 3 - PI / 2
            val vx = cx + (cos(a) * rr * 0.78).toFloat()
            val vy = cy + (sin(a) * rr * 0.78).toFloat()
            if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
        }
        path.close()
        c.drawPath(path, metal(cx - rr, cy - rr, cx + rr, cy + rr))
        c.drawPath(path, T.stroke(0xFFC9A227.toInt(), 1.4f))
        return
    }

    val vTwin = s.layout.startsWith("V")
    val boxer = s.layout == "boxer"
    val visible = min(n, if (vTwin || boxer) 2 else 4)
    val pitch = if (visible <= 1) 0.0 else 0.048

    for (i in 0 until visible) {
        val fire = crank + i * (if (s.twoStroke) TAU / max(1, n) else TAU * 2.0 / max(1, n))
        val lift = (1.0 - cos(fire)) * 0.5
        val bank = when {
            boxer -> if (i == 0) -1.0 else 1.0
            vTwin -> if (i == 0) -0.55 else 0.55
            else -> 0.0
        }
        val ca = cos(bank); val sa = sin(bank)
        val along = stroke * lift
        val slot = (i - (visible - 1) / 2.0) * pitch
        val cyl0x = ox + slot + sa * 0.02
        val cyl0y = oy + 0.10
        val cyl1x = cyl0x + sa * (stroke + 0.04)
        val cyl1y = cyl0y + ca * (stroke + 0.04)
        c.drawLine(px(cyl0x, cyl0y), py(cyl0x, cyl0y), px(cyl1x, cyl1y), py(cyl1x, cyl1y),
            T.stroke(0xFF6B7686.toInt(), sw(bore * 1.15)))
        val pinX = cyl0x + sa * (0.02 + along)
        val pinY = cyl0y + ca * (0.02 + along)
        c.dot(
            px(pinX, pinY), py(pinX, pinY), sw(bore * 0.55),
            metal(px(pinX, pinY), py(pinX, pinY), px(pinX + 0.02, pinY + 0.02), py(pinX, pinY))
        )
        val crankX = ox + slot * 0.3 + crankR * sin(fire)
        val crankY = oy + 0.04 - crankR * cos(fire)
        c.drawLine(px(pinX, pinY), py(pinX, pinY), px(crankX, crankY), py(crankX, crankY),
            T.stroke(0xFFC9A227.toInt(), sw(0.010)))
        val liftV = max(0.0, sin(fire)) * 0.012
        c.drawLine(
            px(cyl1x, cyl1y), py(cyl1x, cyl1y),
            px(cyl1x + sa * (0.02 + liftV), cyl1y + ca * (0.02 + liftV)),
            py(cyl1x + sa * (0.02 + liftV), cyl1y + ca * (0.02 + liftV)),
            T.stroke(0xFFB9C4D4.toInt(), sw(0.008))
        )
    }
    val kx = px(ox, oy + 0.04); val ky = py(ox, oy + 0.04)
    c.dot(kx, ky, sw(0.028), T.fill(0xFF8A97A8.toInt()))
    val throwX = px(ox + crankR * sin(crank), oy + 0.04 - crankR * cos(crank))
    val throwY = py(ox + crankR * sin(crank), oy + 0.04 - crankR * cos(crank))
    c.drawLine(kx, ky, throwX, throwY, T.stroke(0xFFC9A227.toInt(), sw(0.012)))
    c.dot(throwX, throwY, sw(0.010), T.fill(0xFFC9A227.toInt()))
}
