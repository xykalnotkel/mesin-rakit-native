package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import id.mesinrakit.App
import id.mesinrakit.core.*
import kotlin.math.*
import id.mesinrakit.ui.*

class MenuScene(app: App) : Scene(app) {
    private var idBengkel = -1
    private var idRangka = -1
    private var idDyno = -1
    private var idPit = -1
    private var idPeta = -1
    private var idJalan = -1
    private var idPreset = -1
    private var idBantuan = -1
    private var idSalin = -1
    private var idTempel = -1
    private var bukaPreset = false
    private var bukaBantuan = false
    private var idLog = -1

    override fun draw(c: Canvas, w: Float, h: Float) {
        val s = app.spec
        /* lantai bengkel 2.5D */
        c.drawPaint(grad(0f, 0f, w, h, 0xFF071018.toInt(), 0xFF0B1220.toInt()))
        val horizon = h * 0.58f
        val lantai = Path()
        lantai.moveTo(0f, horizon); lantai.lineTo(w, horizon); lantai.lineTo(w, h); lantai.lineTo(0f, h); lantai.close()
        c.drawPath(lantai, T.fill(0xFF0A1522.toInt()))
        val grid = T.stroke(0x1422D3EE, 1.2f)
        var i = 0
        while (i <= 10) {
            val t = i / 10f
            val y = horizon + (h - horizon) * t * t
            c.drawLine(0f, y, w, y, grid)
            i++
        }
        for (k in -8..8) {
            val x0 = w / 2f + k * w * 0.08f
            c.drawLine(x0, horizon, w / 2f + k * w * 0.22f, h, grid)
        }

        /* motor besar di kiri */
        gambarKendaraan(c, app, w * 0.34f, h * 0.46f, w * 0.52f, h * 0.42f)
        c.tx(gayaLabel(s, app.build.name), w * 0.08f, h * 0.22f, T.sp(13f), C.ACC, F_SEMI)
        c.tx(s.karakter.archetype, w * 0.08f, h * 0.22f + 26f, T.sp(22f), C.TEXT, F_BOLD)

        /* judul */
        gambarLogo(c, 28f + 22f, 28f + 22f, 22f)
        c.tx("MESIN RAKIT", 64f, 42f, T.sp(22f), C.TEXT, F_BOLD)
        c.tx("sandbox rakit mesin", 64f, 64f, T.sp(12f), C.DIM, F_REG)

        c.rr(w * 0.08f, h * 0.86f, w * 0.28f, 36f, 18f, T.fill(C.PANEL))
        c.txc("Uang  ${rupiah(app.build.money)}", w * 0.22f, h * 0.86f + 18f, T.sp(14f), C.AMBER, F_SEMI)

        /* tombol kanan */
        val bx = w * 0.62f
        val bw = w * 0.32f
        var by = h * 0.14f
        val bh = max(52f, h * 0.085f)
        val gap = h * 0.012f
        idJalan = tombol(c, "MULAI BERMAIN", bx, by, bw, bh, BTN_UTAMA, T.sp(18f), s.valid); by += bh + gap
        idBengkel = tombol(c, "BENGKEL", bx, by, bw, bh, BTN_NORMAL, T.sp(16f)); by += bh + gap
        idRangka = tombol(c, "RANGKA", bx, by, bw * 0.48f, bh, BTN_NORMAL, T.sp(15f))
        idDyno = tombol(c, "DYNO", bx + bw * 0.52f, by, bw * 0.48f, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idPit = tombol(c, "PIT", bx, by, bw * 0.48f, bh, BTN_NORMAL, T.sp(15f))
        idPeta = tombol(c, "PETA", bx + bw * 0.52f, by, bw * 0.48f, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idPreset = tombol(c, "Preset Indo", bx, by, bw * 0.48f, bh * 0.85f, BTN_HANTU, T.sp(14f))
        idBantuan = tombol(c, "Cara main", bx + bw * 0.52f, by, bw * 0.48f, bh * 0.85f, BTN_HANTU, T.sp(14f)); by += bh * 0.85f + gap
        idSalin = tombol(c, "Salin desain", bx, by, bw * 0.48f, bh * 0.78f, BTN_HANTU, T.sp(13f))
        idTempel = tombol(c, "Tempel", bx + bw * 0.52f, by, bw * 0.48f, bh * 0.78f, BTN_HANTU, T.sp(13f))

        if (!s.valid) {
            c.txc("Lengkapi dulu: ${s.missing.take(2).joinToString(", ")}", bx + bw / 2f, h * 0.92f, T.sp(12f), C.RED, F_MED)
        }
        c.txc("Made XySpace", w * 0.78f, h * 0.965f, T.sp(11f), C.DIM2, F_SEMI)

        if (bukaPreset) panelPreset(c, w, h)
        if (bukaBantuan) panelBantuan(c, w, h)
        panelError(c, w, h)
    }

    private fun panelPreset(c: Canvas, w: Float, h: Float) {
        val pw = w * 0.50f
        val ph = h * 0.74f
        val x = (w - pw) / 2f
        val y = (h - ph) / 2f
        c.rr(0f, 0f, w, h, 0f, T.fill(0xCC050A12.toInt()))
        c.panelJudul("Preset rakitan", x, y, pw, ph)
        val list = id.mesinrakit.data.PRESETS
        val rowH = (ph - 78f) / list.size
        for (i in list.indices) {
            val ry = y + 48f + i * rowH
            val ps = list[i]
            val dipilih = app.build.name == ps.name
            tombol(c, ps.name, x + 14f, ry, pw - 28f, rowH - 8f, BTN_HANTU, T.sp(14f), true, i, dipilih)
            add(x + 14f, ry, pw - 28f, rowH - 8f, 1000 + i)
        }
        tombol(c, "Tutup", x + pw - 110f, y + ph - 52f, 96f, 40f, BTN_NORMAL, T.sp(14f), true, 999)
    }

    private fun panelBantuan(c: Canvas, w: Float, h: Float) {
        val pw = w * 0.62f
        val ph = h * 0.78f
        val x = (w - pw) / 2f
        val y = (h - ph) / 2f
        c.rr(0f, 0f, w, h, 0f, T.fill(0xCC050A12.toInt()))
        c.panelJudul("Cara main", x, y, pw, ph)
        val teks = "Splash lalu menu utama. MULAI membawa rakitan ke jalan. " +
            "BENGKEL pasang part. RANGKA bentuk pipa. DYNO dengar suara. " +
            "GAS/REM di layar, Q/E gigi, R bangun. Tabrakan merusak part — servis di bengkel. " +
            "Studio Drawing: mesin.xyspace.my.id/drawing — salin, lalu Tempel di menu."
        c.wrap(teks, x + 22f, y + 52f, pw - 44f, T.sp(15f), C.TEXT)
        tombol(c, "Tutup", x + pw - 120f, y + ph - 54f, 100f, 42f, BTN_NORMAL, T.sp(14f), true, 998)
    }

    private fun panelError(c: Canvas, w: Float, h: Float) {
        val log = app.errorTeks ?: app.muatLog() ?: return
        val baris = log.split("\n").filter { it.isNotBlank() }.take(5)
        val pw = min(w * 0.92f, 900f)
        val ph = 34f + baris.size * 17f
        val x = (w - pw) / 2f
        val y = h - ph - 70f
        c.rr(x, y, pw, ph, 10f, T.fill(0xCC3B1218.toInt()))
        c.rrs(x, y, pw, ph, 10f, T.stroke(C.RED, 1.6f))
        c.tx("Terjadi error (ketuk buat tutup)", x + 12f, y + 22f, T.sp(12f), C.RED, F_BOLD)
        var yy = y + 40f
        for (b in baris) {
            c.tx(b.take(110), x + 12f, yy, T.sp(11f), C.TEXT, F_REG)
            yy += 17f
        }
        idLog = add(x, y, pw, ph)
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h == null) return
        when (h.id) {
            idBengkel -> app.pindah("bengkel")
            idRangka -> app.pindah("rangka")
            idDyno -> app.pindah("dyno")
            idPit -> app.pindah("pit")
            idPeta -> app.pindah("peta")
            idJalan -> { app.gantiMap(app.mapId); app.mulaiJalan() }
            idLog -> { app.hapusLog(); idLog = -1 }
            idPreset -> bukaPreset = true
            idBantuan -> bukaBantuan = true
            idSalin -> app.salinPack()
            idTempel -> app.tempelPack()
            998 -> bukaBantuan = false
            999 -> bukaPreset = false
            else -> if (h.extra in 1000..1100) {
                val ps = id.mesinrakit.data.PRESETS.getOrNull(h.extra - 1000)
                if (ps != null) { app.pakaiPreset(ps); bukaPreset = false; app.getar(25) }
            }
        }
    }

    override fun tombolKembali(): Boolean {
        if (bukaPreset) { bukaPreset = false; return true }
        if (bukaBantuan) { bukaBantuan = false; return true }
        return false
    }
}

fun gayaLabel(s: id.mesinrakit.model.Spec, name: String): String = when (gayaKendaraan(s, name)) {
    "beat" -> "Gaya Beat / matic skuter"
    "revo" -> "Gaya Revo / underbone"
    "supra" -> "Gaya Supra"
    "fizr" -> "Gaya Fizr / sport underbone"
    "cruiser" -> "Gaya cruiser"
    "mobil" -> "Mobil global"
    else -> "Rakitan bebas"
}
