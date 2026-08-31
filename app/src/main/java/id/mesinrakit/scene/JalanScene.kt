package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import android.view.KeyEvent
import id.mesinrakit.App
import id.mesinrakit.KeyHandler
import id.mesinrakit.core.*
import id.mesinrakit.model.*
import id.mesinrakit.ui.*
import kotlin.math.*

/* ============================================================
   JALAN: bawa rakitan di lintasan. Kamera, HUD, rintangan,
   tabrakan, dan efek suaranya.
   ============================================================ */
class JalanScene(app: App) : Scene(app), KeyHandler {
    private var idGas = -1
    private var idRem = -1
    private var idGigiUp = -1
    private var idGigiDn = -1
    private var idManual = -1
    private var idUlang = -1
    private var idKeluar = -1
    private var idReset = -1
    private var goncang = 0f
    private var ppm = 60f
    private val asap = ArrayList<FloatArray>()
    private val percik = ArrayList<FloatArray>()

    override fun enter() {
        asap.clear(); percik.clear(); goncang = 0f
    }

    override fun update(dt: Float) {
        val v = app.vehicle ?: return
        val gas = if (pressed.contains(idGas)) 1.0 else 0.0
        val rem = if (pressed.contains(idRem)) 1.0 else 0.0
        app.gas = gas
        app.rem = rem
        if (goncang > 0) goncang = max(0f, goncang - dt * 2.5f)

        /* partikel knalpot */
        if (gas > 0.1 && v.rpm > v.spec.idleRPM * 1.4 && Math.random() < dt * 22) {
            asap.add(floatArrayOf(v.posX.toFloat(), v.posY.toFloat(), 0f, 0f, 1f))
        }
        val it = asap.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p[2] -= dt * (2f + abs(v.kmh).toFloat() * 0.03f)
            p[3] += dt * 0.6f
            p[4] -= dt * 0.9f
            if (p[4] <= 0) it.remove()
        }
        val ip = percik.iterator()
        while (ip.hasNext()) {
            val p = ip.next()
            p[0] += p[2] * dt
            p[1] += p[3] * dt
            p[3] -= dt * 9f
            p[4] -= dt * 1.6f
            if (p[4] <= 0) ip.remove()
        }
        if (v.crashT > 0.55f && v.crash != null) {
            goncang = 1f
            for (i in 0 until 12) {
                percik.add(floatArrayOf(v.crash!!.x.toFloat(), v.crash!!.y.toFloat(),
                    (Math.random() - 0.5).toFloat() * 8f, (Math.random() * 6).toFloat(), 1f))
            }
        }
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        val v = app.vehicle ?: run { app.pindah("menu"); return }
        val s = v.spec
        val pal = v.map.pal
        ppm = h * 0.115f
        val camX = v.posX - ppm.let { (w * 0.34f) / it }
        val camY = v.posY - h * 0.42f / ppm
        fun sx(wx: Double) = ((wx - camX) * ppm).toFloat() + w * 0.34f
        fun sy(wy: Double) = (h * 0.70f - (wy - camY) * ppm).toFloat()

        /* langit */
        c.drawPaint(grad(0f, 0f, w, h, pal.sky0, pal.sky1))

        /* bukit jauh */
        val bukitH = h * 0.42f
        val pathB = Path()
        pathB.moveTo(0f, h * 0.72f)
        var bxx = 0f
        while (bxx <= w) {
            val wx = camX + bxx / ppm * 0.35
            val yy = h * 0.70f - (sin(wx * 0.0016) * 0.5 + sin(wx * 0.0041 + 1.3) * 0.3 + 0.9) * bukitH * 0.5
            pathB.lineTo(bxx, yy.toFloat())
            bxx += 24f
        }
        pathB.lineTo(w, h); pathB.lineTo(0f, h); pathB.close()
        c.drawPath(pathB, T.fill(pal.hill))
        val pathB2 = Path()
        pathB2.moveTo(0f, h * 0.72f)
        bxx = 0f
        while (bxx <= w) {
            val wx = camX + bxx / ppm * 0.6
            val yy = h * 0.72f - (sin(wx * 0.0022 + 2.1) * 0.5 + 0.6) * bukitH * 0.34
            pathB2.lineTo(bxx, yy.toFloat())
            bxx += 24f
        }
        pathB2.lineTo(w, h); pathB2.lineTo(0f, h); pathB2.close()
        c.drawPath(pathB2, T.fill(pal.hill2))

        /* tanah */
        val gx0 = camX - 4.0
        val gx1 = camX + (w / ppm).toDouble() + 6.0
        val p = Path()
        p.moveTo(-10f, h + 10f)
        var x = gx0
        while (x < gx1) {
            p.lineTo(sx(x), sy(v.map.groundY(x)))
            x += 0.35
        }
        p.lineTo(w + 10f, h + 10f)
        p.close()
        c.drawPath(p, T.fill(pal.road))
        /* lapisan atas tanah */
        val p2 = Path()
        p2.moveTo(-10f, h + 10f)
        x = gx0
        while (x < gx1) {
            p2.lineTo(sx(x), sy(v.map.groundY(x) - 0.10))
            x += 0.35
        }
        p2.lineTo(w + 10f, h + 10f)
        p2.close()
        c.drawPath(p2, T.fill(pal.road2))
        /* garis tepi jalan */
        val p3 = Path()
        x = gx0
        var first = true
        while (x < gx1) {
            if (first) { p3.moveTo(sx(x), sy(v.map.groundY(x))); first = false }
            else p3.lineTo(sx(x), sy(v.map.groundY(x)))
            x += 0.35
        }
        c.drawPath(p3, T.stroke(pal.line, 2.5f))

        /* penanda jarak tiap 100 m */
        var m = (camX / 100).toInt() * 100
        while (m < camX + w / ppm) {
            if (m >= 0) {
                val mx = sx(m.toDouble())
                val my = sy(v.map.groundY(m.toDouble()))
                c.drawLine(mx, my, mx, my - 18f, T.stroke(pal.line, 2f))
                c.txc("${m}m", mx, my - 28f, T.sp(10f), pal.line, F_SEMI)
            }
            m += 100
        }

        /* rintangan */
        for (o in v.map.obstacles) {
            if (o.x < gx0 - 3 || o.x > gx1 + 3) continue
            val t = OBSTACLE_TYPES[o.type] ?: continue
            val ox = sx(o.x)
            val oy = sy(v.map.groundY(o.x))
            val ow = (t.w * ppm).toFloat()
            val oh = (t.h * ppm).toFloat()
            c.save()
            if (o.hit) {
                c.translate(ox, oy)
                c.rotate(min(70f, o.t.toFloat() * 130f))
                c.translate(-ox, -oy)
            }
            when (o.type) {
                "batu" -> {
                    c.rr(ox - ow / 2, oy - oh, ow, oh, ow * 0.35f, T.fill(t.color))
                    c.rr(ox - ow * 0.3f, oy - oh * 0.85f, ow * 0.35f, oh * 0.3f, ow * 0.1f, T.fill(0xFFA9A99B.toInt()))
                }
                "pohon" -> {
                    c.rr(ox - ow * 0.18f, oy - oh * 0.45f, ow * 0.36f, oh * 0.45f, 4f, T.fill(0xFF5A3F2A.toInt()))
                    c.dot(ox, oy - oh * 0.72f, oh * 0.30f, T.fill(t.color))
                    c.dot(ox - ow * 0.35f, oy - oh * 0.55f, oh * 0.18f, T.fill(0xFF35502F.toInt()))
                }
                "drum" -> {
                    c.rr(ox - ow / 2, oy - oh, ow, oh, ow * 0.12f, T.fill(t.color))
                    for (i in 1..2) c.drawLine(ox - ow / 2, oy - oh * i / 3f, ox + ow / 2, oy - oh * i / 3f, T.stroke(0xFFB45309.toInt(), 3f))
                }
                "mobil" -> {
                    c.rr(ox - ow / 2, oy - oh, ow, oh * 0.62f, ow * 0.08f, T.fill(t.color))
                    c.rr(ox - ow * 0.32f, oy - oh * 1.0f, ow * 0.55f, oh * 0.42f, ow * 0.06f, T.fill(0xFF3A5670.toInt()))
                    c.dot(ox - ow * 0.28f, oy - oh * 0.10f, oh * 0.16f, T.fill(0xFF11161D.toInt()))
                    c.dot(ox + ow * 0.28f, oy - oh * 0.10f, oh * 0.16f, T.fill(0xFF11161D.toInt()))
                }
                else -> {
                    c.rr(ox - ow / 2, oy - oh, ow, oh, 3f, T.fill(t.color))
                    c.drawLine(ox - ow / 2, oy - oh * 0.6f, ox + ow / 2, oy - oh * 0.6f, T.stroke(0xFF7C4A06.toInt(), 3f))
                }
            }
            c.restore()
        }

        /* asap knalpot */
        for (a in asap) {
            val al = (a[4] * 110).toInt().coerceIn(0, 255)
            val pp = T.fill(0xFF9AA7B8.toInt()); pp.alpha = al / 2
            c.dot(sx(a[0].toDouble() + a[2]) - 20f, sy(a[1].toDouble() + a[3]) - 10f, (6 + (1 - a[4]) * 26) * ppm / 60f, pp)
        }
        for (p4 in percik) {
            val pp = T.fill(C.AMBER)
            c.dot(sx(p4[0].toDouble()), sy(p4[1].toDouble()), 3f, pp)
        }

        /* kendaraan */
        c.save()
        if (goncang > 0.01f) c.translate((Math.random() - 0.5).toFloat() * 14f * goncang,
            (Math.random() - 0.5).toFloat() * 14f * goncang)
        gambarKendaraanJalan(c, v, sx(v.posX), sy(v.posY), ppm)
        c.restore()

        /* garis finish */
        val fx = sx(v.map.panjang.toDouble())
        if (fx > -100 && fx < w + 100) {
            val fy = sy(v.map.groundY(v.map.panjang.toDouble()))
            for (i in 0 until 6) for (j in 0 until 2) {
                c.rr(fx - 12f + j * 12f, fy - 60f + i * 12f, 12f, 12f, 0f,
                    T.fill(if ((i + j) % 2 == 0) C.WHITE else C.BLACK))
            }
        }

        /* HUD */
        val kecepatan = abs(v.kmh).toInt()
        c.txc("$kecepatan", w * 0.50f, h * 0.16f, T.sp(44f), C.TEXT, F_NUM_BOLD)
        c.txc("km/jam", w * 0.50f, h * 0.16f + 30f, T.sp(12f), C.DIM, F_SEMI)

        /* rpm */
        val rx = w * 0.50f
        val ry = h * 0.80f
        val rr = h * 0.11f
        c.arcGauge(rx, ry, rr, (v.rpm / s.redline).coerceAtMost(1.0),
            if (v.rpm > s.redline * 0.92) C.RED else if (v.rpm > s.peakRPM) C.AMBER else C.ACC, C.LINE2, 7f)
        c.txc("${v.rpm.toInt()}", rx, ry - 4f, T.sp(20f), C.TEXT, F_NUM_SEMI)
        c.txc("rpm", rx, ry + 18f, T.sp(10f), C.DIM, F_REG)
        val gigi = if (s.cvt) "CVT" else if (v.manual) "M${v.gear + 1}" else "A${v.gear + 1}"
        c.txc(gigi, rx, ry + rr + 16f, T.sp(16f), if (v.cut) C.RED else C.ACC, F_NUM_BOLD)
        if (v.boost > 0.05) c.txc("${String.format("%.1f", v.boost)} bar", rx, ry - rr - 14f, T.sp(12f), C.AMBER, F_BOLD)

        /* informasi atas */
        c.rr(w * 0.02f, h * 0.06f, w * 0.20f, 74f, 12f, T.fill(0x990B1220.toInt()))
        c.tx("Jarak", w * 0.035f, h * 0.06f + 24f, T.sp(11f), C.DIM, F_REG)
        c.txr("${v.distance.toInt()} / ${v.map.panjang} m", w * 0.205f, h * 0.06f + 24f, T.sp(12f), C.TEXT, F_SEMI)
        c.tx("Waktu", w * 0.035f, h * 0.06f + 44f, T.sp(11f), C.DIM, F_REG)
        c.txr("${String.format("%.1f", v.time)} s", w * 0.205f, h * 0.06f + 44f, T.sp(12f), C.TEXT, F_SEMI)
        c.tx("Uang", w * 0.035f, h * 0.06f + 64f, T.sp(11f), C.DIM, F_REG)
        c.txr(rupiah(v.money.toInt()), w * 0.205f, h * 0.06f + 64f, T.sp(12f), C.AMBER, F_SEMI)

        /* kondisi */
        val rep = damageReport(app.build)
        c.rr(w * 0.02f, h * 0.06f + 84f, w * 0.20f, 22f, 8f, T.fill(0x990B1220.toInt()))
        c.tx("Kondisi", w * 0.035f, h * 0.06f + 99f, T.sp(11f), C.DIM, F_REG)
        c.bar(w * 0.09f, h * 0.06f + 91f, w * 0.115f, 8f, s.kondisi,
            if (s.kondisi > 0.75) C.GREEN else if (s.kondisi > 0.4) C.AMBER else C.RED)
        if (rep.parts > 0) c.tx("${rep.parts} part rusak", w * 0.035f, h * 0.06f + 120f, T.sp(11f), C.RED, F_SEMI)

        /* tombol */
        val bw = w * 0.13f
        val bh = h * 0.20f
        val by = h * 0.70f
        idRem = tombol(c, "REM", w * 0.03f, by, bw, bh, BTN_BAHAYA, T.sp(18f), true, 0, pressed.contains(idRem))
        idGas = tombol(c, "GAS", w - bw - w * 0.03f, by, bw, bh, BTN_UTAMA, T.sp(18f), true, 0, pressed.contains(idGas))
        if (!s.cvt) {
            idGigiDn = tombol(c, "G-", w - bw - w * 0.03f - 66f, by + bh * 0.10f, 58f, bh * 0.36f, BTN_NORMAL, T.sp(15f))
            idGigiUp = tombol(c, "G+", w - bw - w * 0.03f - 66f, by + bh * 0.54f, 58f, bh * 0.36f, BTN_NORMAL, T.sp(15f))
            idManual = tombol(c, if (v.manual) "MANUAL" else "OTO", w - bw - w * 0.03f - 132f, by + bh * 0.10f, 60f, bh * 0.36f,
                BTN_NORMAL, T.sp(12f), true, 0, v.manual)
        }
        idReset = tombol(c, "Bangun", w * 0.03f, by - 46f, 92f, 38f, BTN_NORMAL, T.sp(13f), abs(v.ang) > 1.0)
        idUlang = tombol(c, "Ulang", w * 0.03f + 100f, by - 46f, 92f, 38f, BTN_HANTU, T.sp(13f))
        idKeluar = tombol(c, "Pit", w - 110f, h * 0.05f, 90f, 38f, BTN_HANTU, T.sp(13f))

        /* pesan */
        var msg = ""
        var col = C.ACC
        when {
            v.mogok -> { msg = "MOGOK - mesin rusak parah, servis di bengkel"; col = C.RED }
            v.finished -> { msg = "SELESAI"; col = C.GREEN }
            v.flipT > 0.5 -> { msg = "TERBALIK - ketuk Bangun"; col = C.AMBER }
            v.wheelieT > 0.6 -> { msg = "WHEELIE"; col = C.AMBER }
            v.airTime > 0.6 -> { msg = "MELAYANG"; col = C.ACC }
            v.crashT > 0 -> { msg = "TABRAKAN"; col = C.RED }
        }
        if (msg.isNotEmpty()) c.txc(msg, w * 0.5f, h * 0.30f, T.sp(22f), col, F_BOLD)

        /* kartu selesai */
        if (v.finished) {
            c.rr(0f, 0f, w, h, 0f, T.fill(0xCC050A12.toInt()))
            val pw = w * 0.42f
            val ph = h * 0.56f
            val px = (w - pw) / 2f
            val py = (h - ph) / 2f
            c.panelJudul("Selesai", px, py, pw, ph)
            var yy = py + 62f
            c.txc(v.map.name, px + pw / 2f, yy, T.sp(20f), C.ACC, F_BOLD); yy += 40f
            fun baris(l: String, v2: String) {
                c.tx(l, px + 26f, yy, T.sp(14f), C.DIM, F_REG)
                c.txr(v2, px + pw - 26f, yy, T.sp(14f), C.TEXT, F_SEMI)
                yy += 28f
            }
            baris("Waktu", "${String.format("%.1f", v.finishTime)} detik")
            baris("Kecepatan tertinggi", "${v.maxSpeed.toInt()} km/jam")
            baris("Jarak", "${v.distance.toInt()} m")
            baris("Uang didapat", rupiah(v.money.toInt()))
            baris("Kondisi akhir", "${(s.kondisi * 100).toInt()} persen")
            idUlang = tombol(c, "Ulang", px + 26f, py + ph - 62f, pw * 0.42f, 42f, BTN_UTAMA, T.sp(15f))
            idKeluar = tombol(c, "Pit", px + pw * 0.5f, py + ph - 62f, pw * 0.42f, 42f, BTN_NORMAL, T.sp(15f))
        }
    }

    /* gambar kendaraan tampak samping di jalan */
    private fun gambarKendaraanJalan(c: Canvas, v: Vehicle, cx: Float, cy: Float, ppm: Float) {
        val s = v.spec
        val b = app.build
        val r = (s.wheelR * ppm).toFloat()
        val ca = cos(v.ang).toFloat()
        val sa = -sin(v.ang).toFloat()   // layar: y ke bawah
        fun px(ox: Double, oy: Double) = cx + (ca * ox - sa * oy).toFloat() * ppm
        fun py(ox: Double, oy: Double) = cy + (sa * ox + ca * oy).toFloat() * ppm

        c.save()
        /* roda */
        for (i in v.wheels.indices) {
            val wh = v.wheels[i]
            val wx = px(wh.ox, wh.oy)
            val wy = py(wh.ox, wh.oy)
            c.dot(wx, wy, r, T.fill(0xFF15191F.toInt()))
            c.dot(wx, wy, r * 0.62f, T.fill(0xFF2A3444.toInt()))
            /* jari-jari */
            for (k in 0 until 6) {
                val a = wh.spinAng + k * PI / 3
                c.drawLine(wx, wy, wx + cos(a).toFloat() * r * 0.58f, wy + sin(a).toFloat() * r * 0.58f,
                    T.stroke(0xFF8A97A8.toInt(), max(1f, r * 0.07f)))
            }
            c.dot(wx, wy, r * 0.16f, T.fill(0xFF9AA7B8.toInt()))
            c.dot(wx, wy, r, T.stroke(0xFF3A4350.toInt(), max(1f, r * 0.12f)))
        }

        /* body dari cat yang digambar pemain */
        val bodyCol = C.PAINTS.getOrElse(b.colorIdx) { C.ACC }
        val selCat = b.paint.keys.toList()
        if (selCat.isNotEmpty()) {
            var c0 = 99; var r0 = 99; var c1 = -1; var r1 = -1
            for (k in selCat) {
                val a = k.split(",")
                val cc = a[0].toInt(); val rr = a[1].toInt()
                c0 = min(c0, cc); c1 = max(c1, cc); r0 = min(r0, rr); r1 = max(r1, rr)
            }
            val cw = (c1 - c0 + 1).toFloat()
            val chh = (r1 - r0 + 1).toFloat()
            val sel = 0.42f / max(cw, chh)
            for (k in selCat) {
                val a = k.split(",")
                val cc = (a[0].toInt() - c0) - (cw - 1) / 2f
                val rr = (a[1].toInt() - r0) - (chh - 1) / 2f
                val bx = px(cc * sel * 2.4 - 0.15, -rr * sel * 2.0 + s.frameH * 0.9)
                val by = py(cc * sel * 2.4 - 0.15, -rr * sel * 2.0 + s.frameH * 0.9)
                c.rr(bx - sel * ppm * 0.9f, by - sel * ppm * 0.9f, sel * ppm * 1.8f, sel * ppm * 1.8f, sel * ppm * 0.3f, T.fill(bodyCol))
            }
        }

        /* rangka */
        val f = b.frame
        val frameCol = 0xFF7C8798.toInt()
        if (f != null && f.nodes.isNotEmpty()) {
            val bd = f.bounds()
            val bw2 = (bd[2] - bd[0]).coerceAtLeast(0.01)
            val bh2 = (bd[3] - bd[1]).coerceAtLeast(0.01)
            val sc = min(s.wheelbase / bw2, (s.frameH * 1.6) / bh2)
            val ox0 = -(bd[0] + bd[2]) / 2 * sc
            val oy0 = -(bd[1] + bd[3]) / 2 * sc + s.wheelR * 0.8
            for (t in f.tubes) {
                val a = f.nodes[t.a]; val b2 = f.nodes[t.b]
                val ax = px((a.x - (bd[0] + bd[2]) / 2) * sc, (a.y - (bd[1] + bd[3]) / 2) * sc + s.wheelR * 0.8)
                val ay = py((a.x - (bd[0] + bd[2]) / 2) * sc, (a.y - (bd[1] + bd[3]) / 2) * sc + s.wheelR * 0.8)
                val bx = px((b2.x - (bd[0] + bd[2]) / 2) * sc, (b2.y - (bd[1] + bd[3]) / 2) * sc + s.wheelR * 0.8)
                val by = py((b2.x - (bd[0] + bd[2]) / 2) * sc, (b2.y - (bd[1] + bd[3]) / 2) * sc + s.wheelR * 0.8)
                val bow = ((a.bow + b2.bow) / 2).toFloat()
                if (abs(bow) > 0.01f) {
                    val path = Path()
                    val dx = bx - ax; val dy = by - ay
                    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.01f)
                    path.moveTo(ax, ay)
                    path.quadTo((ax + bx) / 2 - dy / len * bow * len * 0.4f,
                        (ay + by) / 2 + dx / len * bow * len * 0.4f, bx, by)
                    c.drawPath(path, T.stroke(frameCol, max(2.5f, (t.dia / 0.032).toFloat() * r * 0.20f)))
                } else {
                    c.drawLine(ax, ay, bx, by, T.stroke(frameCol, max(2.5f, (t.dia / 0.032).toFloat() * r * 0.20f)))
                }
            }
        } else {
            val w1 = v.wheels.first()
            val w2 = v.wheels.last()
            c.drawLine(px(w2.ox, w2.oy), py(w2.ox, w2.oy), px(0.0, s.wheelR * 0.9), py(0.0, s.wheelR * 0.9),
                T.stroke(frameCol, max(3f, r * 0.22f)))
            c.drawLine(px(w1.ox, w1.oy), py(w1.ox, w1.oy), px(0.0, s.wheelR * 0.9), py(0.0, s.wheelR * 0.9),
                T.stroke(frameCol, max(3f, r * 0.22f)))
        }

        /* blok mesin */
        val blokW = (0.16 + s.cyl * 0.035).coerceAtMost(0.55)
        c.rr(px(-blokW / 2, s.wheelR * 0.35).toFloat(), py(blokW / 2, s.wheelR * 1.2).toFloat(),
            (blokW * ppm).toFloat(), (s.wheelR * 0.95 * ppm).toFloat(), r * 0.12f,
            metal(px(-blokW / 2, s.wheelR * 0.35), py(-blokW / 2, s.wheelR * 0.35), px(blokW / 2, s.wheelR * 1.2), py(blokW / 2, s.wheelR * 1.2)))

        /* knalpot */
        c.drawLine(px(-0.1, s.wheelR * 0.8), py(-0.1, s.wheelR * 0.8),
            px(-s.wheelbase * 0.55, s.wheelR * 0.7), py(-s.wheelbase * 0.55, s.wheelR * 0.7),
            T.stroke(0xFFB9C4D4.toInt(), max(2.5f, r * 0.16f)))

        /* jok */
        c.rr(px(-s.wheelbase * 0.28, s.wheelR * 1.5), py(-s.wheelbase * 0.28, s.wheelR * 1.9),
            (s.wheelbase * 0.36 * ppm).toFloat(), (s.wheelR * 0.32 * ppm).toFloat(), r * 0.12f, T.fill(0xFF101720.toInt()))

        /* stang */
        c.drawLine(px(s.wheelbase * 0.42, s.wheelR * 1.6), py(s.wheelbase * 0.42, s.wheelR * 1.6),
            px(s.wheelbase * 0.52, s.wheelR * 2.1), py(s.wheelbase * 0.52, s.wheelR * 2.1),
            T.stroke(0xFF9AA7B8.toInt(), max(2.5f, r * 0.13f)))

        /* pengendara */
        val lean = clamp(v.ang * 0.7, -0.5, 0.5)
        val hx = px(-s.wheelbase * 0.05, s.wheelR * 1.9 + lean * 0.2)
        val hy = py(-s.wheelbase * 0.05, s.wheelR * 1.9 + lean * 0.2)
        val hcol = s.driver.color
        c.dot(hx, hy - r * 0.55f, r * 0.28f, T.fill(hcol))                       // helm
        c.rr(hx - r * 0.22f, hy - r * 0.35f, r * 0.44f, r * 0.75f, r * 0.16f, T.fill(hcol))  // badan
        c.drawLine(hx, hy - r * 0.30f, hx + r * 0.5f, hy - r * 0.05f, T.stroke(hcol, max(2f, r * 0.16f)))  // lengan
        c.drawLine(hx - r * 0.1f, hy + r * 0.35f, hx + r * 0.25f, hy + r * 0.05f, T.stroke(0xFF2A3444.toInt(), max(2f, r * 0.15f))) // kaki
        c.restore()
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        val v = app.vehicle ?: return
        if (h == null) return
        when (h.id) {
            idReset -> v.recover()
            idUlang -> { app.gantiMap(app.mapId); app.mulaiJalan() }
            idKeluar -> { app.simpan(); app.pindah("bengkel") }
            idManual -> v.manual = !v.manual
            idGigiUp -> v.shift(1)
            idGigiDn -> v.shift(-1)
        }
    }

    override fun key(code: Int, turun: Boolean): Boolean {
        val v = app.vehicle ?: return false
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_D ->
                { if (turun) pressed.add(idGas) else pressed.remove(idGas); return true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_SPACE ->
                { if (turun) pressed.add(idRem) else pressed.remove(idRem); return true }
            KeyEvent.KEYCODE_Q -> { if (turun) v.shift(-1); return true }
            KeyEvent.KEYCODE_E -> { if (turun) v.shift(1); return true }
            KeyEvent.KEYCODE_M -> { if (turun) v.manual = !v.manual; return true }
            KeyEvent.KEYCODE_R -> { if (turun) v.recover(); return true }
        }
        return false
    }

    override fun tombolKembali(): Boolean { app.simpan(); app.pindah("bengkel"); return true }
}
