package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.model.EngineState
import id.mesinrakit.model.Spec
import id.mesinrakit.model.boostAt
import id.mesinrakit.model.torqueAt
import id.mesinrakit.ui.*
import kotlin.math.*

/* ============================================================
   DYNO: putar mesin di tempat, dengarkan suaranya, lihat
   grafik torsi dan tenaga yang lahir dari rakitan.
   ============================================================ */
class DynoScene(app: App) : Scene(app) {
    private var rpm = 1200.0
    private var beban = 0.35
    private var boost = 0.0
    private val st = EngineState()
    private var crank = 0.0
    private var cut = false
    private var idGas = -1
    private var idRem = -1
    private var idBebanM = -1
    private var idBebanP = -1
    private var idReset = -1
    private var idKembali = -1
    private var specCache: Spec? = null
    private var tqArr = FloatArray(0)
    private var pwArr = FloatArray(0)

    private data class Puff(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var r: Float)
    private val puffs = ArrayList<Puff>()

    private fun siapkanKurva() {
        val s = app.spec
        if (specCache === s && tqArr.isNotEmpty()) return
        specCache = s
        val N = 72
        tqArr = FloatArray(N + 1)
        pwArr = FloatArray(N + 1)
        for (i in 0..N) {
            val r = i.toDouble() / N * s.rMax
            val b = s.boostMax * clamp((r - s.spoolRPM) / max(600.0, s.peakRPM - s.spoolRPM), 0.0, 1.0)
            val tq = torqueAt(r, s, b)
            tqArr[i] = tq.toFloat()
            pwArr[i] = (tq * r * TAU / 60.0 / 745.7).toFloat()
        }
    }

    override fun enter() { rpm = app.spec.idleRPM; siapkanKurva() }

    override fun update(dt: Float) {
        val s = app.spec
        siapkanKurva()
        val gas = if (pressed.contains(idGas)) 1.0 else 0.0
        val rem = if (pressed.contains(idRem)) 1.0 else 0.0
        st.dt = dt.toDouble()

        /* limiter */
        cut = false
        if (rpm > s.limiterRPM && s.limiter != "none") {
            when (s.limiter) {
                "hard" -> cut = true
                "rotasi" -> cut = sin(rpm * 0.004 + crank * 0.02) > 0
                "soft" -> cut = rpm > s.limiterRPM + 250
            }
        }
        st.cut = cut
        boost = boostAt(rpm, s, if (cut) 0.0 else gas, st)
        val tq = if (cut) 0.0 else torqueAt(rpm, s, boost) * gas
        val tahanan = beban * 62.0 * (0.30 + rpm / s.rMax) + rem * 90.0 + rpm * 0.0018 + 2.0
        val inertia = 0.09 + s.dispCc / 1000.0 * 0.30
        var d = (tq - tahanan) / inertia * 9.549 * dt.toDouble()
        if (gas > 0 && rpm < s.idleRPM) d = max(d, (s.idleRPM - rpm) * 3.0 * dt)
        rpm = clamp(rpm + d, s.idleRPM * 0.65, s.rMax)

        /* putaran poros buat efek embusan knalpot */
        val dulu = crank
        crank = (crank + rpm / 60.0 * 360.0 * dt.toDouble()) % s.cycleDeg
        for (a in s.firing) {
            var lewati = false
            if (dulu > crank) lewati = a >= dulu || a <= crank
            else lewati = a in dulu..crank
            if (lewati && gas > 0.1) {
                puffs.add(Puff(0f, 0f, -(0.6 + RND.next() * 1.2).toFloat(),
                    (-(0.2 + RND.next() * 0.5)).toFloat(), 0.75f, (5 + RND.next() * 7).toFloat()))
            }
        }
        val it = puffs.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            p.x += p.vx * dt * 60f
            p.y += p.vy * dt * 60f
            p.r += dt * 22f
            if (p.life <= 0) it.remove()
        }

        app.audio.update(dt.toDouble(), rpm, gas, (tq / max(1.0, s.maxTorque)).toDouble(), boost,
            0.0, 0.0, 0.0, cut, false)
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        val s = app.spec
        c.txc("DYNO", w / 2f, h * 0.10f, T.sp(26f), C.TEXT, F_BOLD)
        c.txc(s.karakter.archetype, w / 2f, h * 0.10f + 26f, T.sp(14f), C.ACC, F_SEMI)

        /* gauge rpm */
        val gx = w * 0.16f
        val gy = h * 0.52f
        val gr = min(w * 0.11f, h * 0.20f)
        c.arcGauge(gx, gy, gr, (rpm / s.redline).coerceAtMost(1.0), if (rpm > s.redline * 0.9) C.RED else C.ACC)
        c.txc("${rpm.toInt()}", gx, gy - 6f, T.sp(34f), C.TEXT, F_NUM_BOLD)
        c.txc("rpm", gx, gy + 26f, T.sp(13f), C.DIM, F_SEMI)
        if (boost > 0.05) c.txc("BOOST ${String.format("%.1f", boost)} bar", gx, gy + gr + 26f, T.sp(13f), C.AMBER, F_BOLD)
        if (cut) c.txc("LIMITER", gx, gy - gr - 18f, T.sp(13f), C.RED, F_BOLD)

        /* angka torsi dan tenaga sekarang */
        val tqNow = torqueAt(rpm, s, boost)
        val pwNow = tqNow * rpm * TAU / 60.0 / 745.7
        c.panel(gx - gr, gy + gr + 44f, gr * 2f, 74f, 12f)
        c.txc("${String.format("%.1f", tqNow)} Nm", gx, gy + gr + 66f, T.sp(18f), C.ACC, F_NUM_SEMI)
        c.txc("${String.format("%.1f", pwNow)} hp", gx, gy + gr + 98f, T.sp(18f), C.AMBER, F_NUM_SEMI)

        /* grafik torsi & tenaga */
        val cx0 = w * 0.33f
        val cy0 = h * 0.24f
        val cw = w * 0.36f
        val ch = h * 0.50f
        c.panelJudul("Kurva Torsi dan Tenaga", cx0, cy0, cw, ch)
        val gx0 = cx0 + 46f
        val gy0 = cy0 + 34f
        val gw = cw - 70f
        val gh = ch - 76f
        c.rrs(gx0, gy0, gw, gh, 6f, T.stroke(C.LINE, 1f))
        val tqMax = max(1f, tqArr.maxOrNull() ?: 1f) * 1.12f
        val pwMax = max(1f, pwArr.maxOrNull() ?: 1f) * 1.12f
        fun path(arr: FloatArray, maks: Float): Path {
            val p = Path()
            for (i in arr.indices) {
                val x = gx0 + gw * i / (arr.size - 1)
                val y = gy0 + gh - (arr[i] / maks) * gh
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }
        c.drawPath(path(tqArr, tqMax), T.stroke(C.ACC, 2.4f))
        c.drawPath(path(pwArr, pwMax), T.stroke(C.AMBER, 2.4f))
        /* kursor rpm sekarang */
        val kx = gx0 + gw * (rpm / s.rMax).toFloat().coerceIn(0f, 1f)
        c.drawLine(kx, gy0, kx, gy0 + gh, T.stroke(C.WHITE, 1.4f))
        /* legenda */
        c.tx("Torsi (Nm)", gx0, cy0 + ch - 12f, T.sp(11f), C.ACC, F_SEMI)
        c.tx("Tenaga (hp)", gx0 + 110f, cy0 + ch - 12f, T.sp(11f), C.AMBER, F_SEMI)
        c.txr("${s.rMax.toInt()} rpm", gx0 + gw, cy0 + ch - 12f, T.sp(11f), C.DIM, F_REG)
        c.tx("0", gx0 - 14f, gy0 + gh, T.sp(10f), C.DIM, F_REG)

        /* bentuk gelombang langsung */
        val wx = w * 0.72f
        val wy = h * 0.24f
        val ww = w * 0.25f
        val wh = h * 0.16f
        c.panelJudul("Bentuk Gelombang", wx, wy, ww, wh)
        val wave = app.audio.wave()
        val p = Path()
        for (i in wave.indices) {
            val x = wx + 12f + (ww - 24f) * i / (wave.size - 1)
            val y = wy + wh / 2f + 14f - wave[i] * wh * 0.42f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        c.drawPath(p, T.stroke(C.GREEN, 1.8f))

        /* embusan knalpot */
        for (pf in puffs) {
            val a = (pf.life / 0.75f * 90f).toInt().coerceIn(0, 255)
            val pp = T.fill(0xFF9AA7B8.toInt()); pp.alpha = a / 2
            c.dot(wx + ww * 0.5f + pf.x * 22f, wy + wh + 26f + pf.y * 18f, pf.r, pp)
        }

        /* karakter */
        val kx0 = w * 0.72f
        val ky0 = h * 0.46f
        val kw = w * 0.25f
        val kh = h * 0.42f
        c.panelJudul("Karakter", kx0, ky0, kw, kh)
        radarKarakter(c, s, kx0 + kw / 2f, ky0 + kh * 0.45f, min(kw * 0.30f, kh * 0.32f))
        var tx = kx0 + 14f
        var ty = ky0 + kh - 30f
        for (t in s.karakter.tags) {
            val tw = T.p(C.TEXT, T.sp(11f), F_SEMI).measureText(t) + 18f
            if (tx + tw > kx0 + kw - 12f) { tx = kx0 + 14f; ty += 24f }
            c.rr(tx, ty, tw, 20f, 10f, T.fill(C.PANEL2))
            c.txc(t, tx + tw / 2f, ty + 10f, T.sp(11f), C.ACC, F_SEMI)
            tx += tw + 6f
        }

        /* kontrol */
        val bw = w * 0.11f
        val bh = h * 0.11f
        val by = h * 0.76f
        idGas = tombol(c, "GAS", w * 0.10f, by, bw, bh, BTN_UTAMA, T.sp(18f), true, 0, pressed.contains(idGas))
        idRem = tombol(c, "REM", w * 0.22f, by, bw, bh, BTN_BAHAYA, T.sp(18f), true, 0, pressed.contains(idRem))
        c.panel(w * 0.35f, by, w * 0.20f, bh, 12f)
        c.txc("BEBAN ${(beban * 100).toInt()} persen", w * 0.45f, by + 22f, T.sp(13f), C.DIM, F_SEMI)
        c.bar(w * 0.365f, by + 34f, w * 0.17f, 12f, beban.toDouble(), C.AMBER)
        idBebanM = tombol(c, "-", w * 0.365f, by + 54f, w * 0.08f, 30f, BTN_NORMAL, T.sp(16f))
        idBebanP = tombol(c, "+", w * 0.455f, by + 54f, w * 0.08f, 30f, BTN_NORMAL, T.sp(16f))
        idReset = tombol(c, "Reset", w * 0.60f, by + bh * 0.35f, w * 0.10f, bh * 0.55f, BTN_HANTU, T.sp(14f))
        idKembali = tombol(c, "Kembali", w - 130f, h - 62f, 110f, 42f, BTN_HANTU, T.sp(14f))
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h == null) return
        when (h.id) {
            idBebanM -> beban = clamp(beban - 0.1, 0.0, 1.0)
            idBebanP -> beban = clamp(beban + 0.1, 0.0, 1.0)
            idReset -> { rpm = app.spec.idleRPM; boost = 0.0; st.boost = 0.0 }
            idKembali -> app.pindah("menu")
        }
    }

    override fun tombolKembali(): Boolean { app.pindah("menu"); return true }
}
