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

        /* HUD cluster */
        val gigi = if (s.cvt) "CVT" else if (v.manual) "M${v.gear + 1}" else "A${v.gear + 1}"
        c.cluster(w * 0.50f, h * 0.78f, h * 0.11f, abs(v.kmh), (v.rpm / s.redline).coerceAtMost(1.2), s.kondisi, "${v.rpm.toInt()}")
        c.txc(gigi, w * 0.50f, h * 0.78f + h * 0.11f * 0.95f, T.sp(16f), if (v.cut) C.RED else C.ACC, F_NUM_BOLD)
        if (v.boost > 0.05) c.txc("${String.format("%.1f", v.boost)} bar", w * 0.50f, h * 0.62f, T.sp(13f), C.AMBER, F_BOLD)

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
        val bw = max(w * 0.14f, 110f)
        val bh = max(h * 0.20f, 96f)
        val by = h * 0.68f
        idRem = tombol(c, "REM", w * 0.03f, by, bw, bh, BTN_BAHAYA, T.sp(20f), true, 0, pressed.contains(idRem))
        idGas = tombol(c, "GAS", w - bw - w * 0.03f, by, bw, bh, BTN_UTAMA, T.sp(20f), true, 0, pressed.contains(idGas))
        if (!s.cvt) {
            idGigiDn = tombol(c, "G-", w - bw - w * 0.03f - 78f, by + bh * 0.08f, 70f, bh * 0.38f, BTN_NORMAL, T.sp(16f))
            idGigiUp = tombol(c, "G+", w - bw - w * 0.03f - 78f, by + bh * 0.54f, 70f, bh * 0.38f, BTN_NORMAL, T.sp(16f))
            idManual = tombol(c, if (v.manual) "MANUAL" else "OTO", w - bw - w * 0.03f - 156f, by + bh * 0.08f, 72f, bh * 0.38f,
                BTN_NORMAL, T.sp(13f), true, 0, v.manual)
        }
        idReset = tombol(c, "Bangun", w * 0.03f, by - 52f, 110f, 44f, BTN_NORMAL, T.sp(14f), abs(v.ang) > 1.0)
        idUlang = tombol(c, "Ulang", w * 0.03f + 118f, by - 52f, 110f, 44f, BTN_HANTU, T.sp(14f))
        idKeluar = tombol(c, "Pit", w - 128f, h * 0.05f, 108f, 44f, BTN_HANTU, T.sp(14f))

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

    /* gambar kendaraan tampak samping di jalan. Y body ke atas, layar Y ke bawah. */
    private fun gambarKendaraanJalan(c: Canvas, v: Vehicle, cx: Float, cy: Float, ppm: Float) {
        val ca = cos(v.ang)
        val sa = sin(v.ang)
        fun px(ox: Double, oy: Double) = cx + ((ca * ox - sa * oy) * ppm).toFloat()
        fun py(ox: Double, oy: Double) = cy - ((sa * ox + ca * oy) * ppm).toFloat()
        val rear = v.wheels.last()
        val front = v.wheels.first()
        val rearSx = cx + ((rear.cx - v.posX) * ppm).toFloat()
        val rearSy = cy - ((rear.cy + v.r - v.posY) * ppm).toFloat()
        val frontSx = cx + ((front.cx - v.posX) * ppm).toFloat()
        val frontSy = cy - ((front.cy + v.r - v.posY) * ppm).toFloat()
        gambarRakitSamping(
            c, app, ::px, ::py, ppm, v.rpm,
            rearSx, rearSy, frontSx, frontSy,
            rear.spinAng, front.spinAng, true
        )
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
