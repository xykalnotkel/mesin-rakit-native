package id.mesinrakit.scene

import android.graphics.Canvas
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.model.Spec
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
    private var presetKe = 0
    private var bukaPreset = false
    private var bukaBantuan = false
    private var idLog = -1

    override fun draw(c: Canvas, w: Float, h: Float) {
        val s = app.spec
        /* judul */
        c.txc("MESIN RAKIT", w / 2f, h * 0.13f, T.sp(46f), C.TEXT, F_BOLD)
        c.txc("Sandbox rakit mesin 2D - suara lahir dari rakitanmu sendiri", w / 2f, h * 0.13f + 34f, T.sp(13f), C.DIM, F_REG)

        val kolKiri = w * 0.05f
        val lw = w * 0.34f
        ringkasan(c, app, kolKiri, h * 0.24f, lw, h * 0.62f)

        /* radar karakter */
        val rx = kolKiri + lw + w * 0.06f
        c.panelJudul("Karakter Mesin", rx, h * 0.24f, w * 0.26f, h * 0.62f)
        radarKarakter(c, s, rx + w * 0.13f, h * 0.50f, min(w * 0.10f, h * 0.20f))
        var ty = h * 0.24f + h * 0.62f - 54f
        val tag = s.karakter.tags
        var tx = rx + 14f
        for (t in tag) {
            val tw = T.p(C.TEXT, T.sp(11f), F_SEMI).measureText(t) + 18f
            if (tx + tw > rx + w * 0.26f - 12f) { tx = rx + 14f; ty += 24f }
            c.rr(tx, ty, tw, 20f, 10f, T.fill(C.PANEL2))
            c.txc(t, tx + tw / 2f, ty + 10f, T.sp(11f), C.ACC, F_SEMI)
            tx += tw + 6f
        }

        /* tombol aksi */
        val bx = w * 0.70f
        val bw = w * 0.25f
        var by = h * 0.24f
        val bh = h * 0.088f
        val gap = h * 0.014f
        idBengkel = tombol(c, "BENGKEL  (rakit part)", bx, by, bw, bh, BTN_UTAMA, T.sp(15f)); by += bh + gap
        idRangka = tombol(c, "RANGKA  (bentuk sendiri)", bx, by, bw, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idDyno = tombol(c, "DYNO  (dengar suaranya)", bx, by, bw, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idPit = tombol(c, "PIT  (ganti pengendara)", bx, by, bw, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idPeta = tombol(c, "MAP  (pilih lintasan)", bx, by, bw, bh, BTN_NORMAL, T.sp(15f)); by += bh + gap
        idJalan = tombol(c, "JALAN", bx, by, bw, bh, BTN_UTAMA, T.sp(16f), s.valid); by += bh + gap * 2
        idBantuan = tombol(c, "Cara main", bx, by, bw * 0.48f, bh * 0.8f, BTN_HANTU, T.sp(13f))
        idPreset = tombol(c, "Preset", bx + bw * 0.52f, by, bw * 0.48f, bh * 0.8f, BTN_HANTU, T.sp(13f))

        /* uang */
        c.rr(w * 0.70f, h * 0.10f, w * 0.25f, 34f, 17f, T.fill(C.PANEL))
        c.txc("Uang  ${rupiah(app.build.money)}", w * 0.825f, h * 0.10f + 17f, T.sp(14f), C.AMBER, F_SEMI)

        if (!s.valid) {
            c.txc("Belum bisa jalan: ${s.missing.take(3).joinToString(", ")}", w * 0.825f, h * 0.90f, T.sp(12.5f), C.RED, F_MED)
        }

        if (bukaPreset) panelPreset(c, w, h)
        if (bukaBantuan) panelBantuan(c, w, h)
        panelError(c, w, h)
    }

    private fun panelPreset(c: Canvas, w: Float, h: Float) {
        val pw = w * 0.46f
        val ph = h * 0.70f
        val x = (w - pw) / 2f
        val y = (h - ph) / 2f
        c.rr(0f, 0f, w, h, 0f, T.fill(0xCC050A12.toInt()))
        c.panelJudul("Preset Rakitan", x, y, pw, ph)
        val list = id.mesinrakit.data.PRESETS
        val rowH = (ph - 70f) / list.size
        for (i in list.indices) {
            val ry = y + 44f + i * rowH
            val ps = list[i]
            val dipilih = app.build.name == ps.name
            tombol(c, ps.name, x + 12f, ry, pw - 90f, rowH - 6f, BTN_HANTU, T.sp(13f), true, i, dipilih)
            c.tx(ps.tags.firstOrNull() ?: "", x + pw - 74f, ry + rowH / 2f, T.sp(11f), C.DIM, F_REG)
            add(x + pw - 150f, ry, 60f, rowH - 6f, 1000 + i)
        }
        tombol(c, "Tutup", x + pw - 90f, y + ph - 46f, 78f, 34f, BTN_NORMAL, T.sp(13f), true, 999)
    }

    private fun panelBantuan(c: Canvas, w: Float, h: Float) {
        val pw = w * 0.60f
        val ph = h * 0.76f
        val x = (w - pw) / 2f
        val y = (h - ph) / 2f
        c.rr(0f, 0f, w, h, 0f, T.fill(0xCC050A12.toInt()))
        c.panelJudul("Cara main", x, y, pw, ph)
        val teks = "BENGKEL: pilih part di palet, ketuk grid buat pasang. Ketuk lalu geser buat mindah. " +
            "Mode Cat buat menggambar bentuk body, mode Hapus buat buang part.\n\n" +
            "RANGKA: taruh simpul lalu tarik dari simpul ke simpul buat bikin pipa. " +
            "Pilih simpul buat mengatur lengkungan, diameter, ketebalan, dan bahan. " +
            "Rangka harus punya dudukan stang, dudukan mesin, dan dudukan as roda belakang.\n\n" +
            "DYNO: tahan GAS, atur BEBAN, dengarkan suaranya dan lihat grafik torsi serta tenaga.\n\n" +
            "PIT: ganti pengendara. Berat dan gaya berkendara ikut memengaruhi akselerasi.\n\n" +
            "MAP: pilih lintasan. Lintasan Lurus datar tanpa rintangan, khusus buat setting.\n\n" +
            "JALAN: tombol GAS dan REM di layar, atau panah atas dan bawah di keyboard. " +
            "Q dan E buat oper gigi manual, R buat bangun kalau terbalik. " +
            "Tabrakan merusak part: tenaga turun dan mesin bisa bocor. Servis di Bengkel."
        c.wrap(teks, x + 20f, y + 44f, pw - 40f, T.sp(13.5f), C.TEXT)
        tombol(c, "Tutup", x + pw - 100f, y + ph - 46f, 80f, 34f, BTN_NORMAL, T.sp(13f), true, 998)
    }

    /** kalau aplikasi sempat error, tunjukkan supaya bisa dilaporin */
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
