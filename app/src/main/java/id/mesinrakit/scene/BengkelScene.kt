package id.mesinrakit.scene

import android.graphics.Canvas
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.data.*
import id.mesinrakit.model.*
import id.mesinrakit.ui.*
import kotlin.math.*

/* ============================================================
   BENGKEL: editor grid buat pasang part, ngecat body, dan
   merakit kendaraan dari nol.
   ============================================================ */
class BengkelScene(app: App) : Scene(app) {
    private var kat = "blok"
    private var pid: String? = null
    private var mode = 0            // 0 pasang, 1 cat, 2 hapus
    private var sel = -1 to -1      // sel yang dipilih buat info
    private var scroll = 0f
    private var geserDari: Pair<Int, Int>? = null
    private var geserPid: String? = null
    private var geserX = 0f
    private var geserY = 0f
    private var geserOk = false

    /* id tombol */
    private var idMode = IntArray(3) { -1 }
    private var idRangka = -1
    private var idDyno = -1
    private var idJalan = -1
    private var idMenu = -1
    private var idLengkapi = -1
    private var idBersih = -1
    private var idServis = -1
    private var idServisKurang = -1
    private var idPreset = -1
    private val idKat = HashMap<String, Int>()
    private val idWarna = IntArray(C.PAINTS.size) { -1 }
    private var gridRect = FloatArray(4)

    private val urutanLengkap = listOf("blk1", "pis1", "rod1", "crk1", "hd1", "cam1", "vlv1",
        "car1", "flt1", "ex1", "tk1", "fu2", "cdi1", "cvt1", "chn1", "fr1", "ty1", "ty2",
        "rm1", "rm2", "brk1", "st1", "sk1", "sa1", "fk1", "sh1", "fp1", "lp1", "bt1")

    override fun enter() { if (pid == null) pid = partsByCat(kat).firstOrNull()?.id }

    override fun draw(c: Canvas, w: Float, h: Float) {
        val atas = h * 0.085f
        val bawah = h * 0.60f
        val tList = h * 0.615f

        /* baris atas */
        c.rr(0f, 0f, w, atas, 0f, T.fill(C.BG2))
        c.tx("BENGKEL", 24f, atas * 0.62f, T.sp(20f), C.TEXT, F_BOLD)
        c.tx("Uang  ${rupiah(app.build.money)}", 160f, atas * 0.62f, T.sp(14f), C.AMBER, F_SEMI)
        val sb = 96f
        var sx = w - 30f - sb
        idMenu = tombol(c, "Menu", sx, 10f, sb, atas - 20f, BTN_HANTU, T.sp(12f)); sx -= sb + 8f
        idJalan = tombol(c, "Jalan", sx, 10f, sb, atas - 20f, BTN_UTAMA, T.sp(12f), app.spec.valid); sx -= sb + 8f
        idDyno = tombol(c, "Dyno", sx, 10f, sb, atas - 20f, BTN_NORMAL, T.sp(12f)); sx -= sb + 8f
        idRangka = tombol(c, "Rangka", sx, 10f, sb, atas - 20f, BTN_NORMAL, T.sp(12f)); sx -= sb + 8f

        /* grid */
        val gw = w * 0.44f
        val gh = bawah - atas - 24f
        val cell = min(gw / GRID_W, gh / GRID_H)
        val gx = 20f
        val gy = atas + (gh - cell * GRID_H) / 2f + 8f
        gridRect = floatArrayOf(gx, gy, cell * GRID_W, cell * GRID_H)
        c.panel(gx - 8f, gy - 8f, cell * GRID_W + 16f, cell * GRID_H + 16f, 12f)
        for (r in 0 until GRID_H) {
            for (col in 0 until GRID_W) {
                val x = gx + col * cell
                val y = gy + r * cell
                val k = cellKey(col, r)
                val p = app.build.items[k]?.let { PART[it] }
                if (p != null) {
                    val o = app.build.originOf(col, r)
                    if (o != null && o.first == col && o.second == r) {
                        val dmg = app.build.dmg[k] ?: 0.0
                        c.rr(x + 2f, y + 2f, cell * p.gw - 4f, cell * p.gh - 4f, 8f, T.fill(C.PANEL2))
                        gambarPart(c, p, x + 4f, y + 4f, cell * p.gw - 8f, cell * p.gh - 8f)
                        if (dmg > 0.02) {
                            c.rr(x + 2f, y + 2f, cell * p.gw - 4f, cell * p.gh - 4f, 8f, T.stroke(C.RED, 2f))
                            c.txc("${(dmg * 100).toInt()}%", x + cell * p.gw / 2f, y + cell * p.gh - 10f, T.sp(10f), C.RED, F_BOLD)
                        }
                        if (sel.first == col && sel.second == r) c.rrs(x + 2f, y + 2f, cell * p.gw - 4f, cell * p.gh - 4f, 8f, T.stroke(C.ACC, 2f))
                    }
                } else {
                    val cat = app.build.paint[k]
                    if (cat != null) {
                        c.rr(x + 2f, y + 2f, cell - 4f, cell - 4f, 5f, T.fill(C.PAINTS[cat]))
                    } else {
                        c.rr(x + 3f, y + 3f, cell - 6f, cell - 6f, 5f, T.fill(C.BG2))
                        c.rrs(x + 3f, y + 3f, cell - 6f, cell - 6f, 5f, T.stroke(C.LINE, 1f))
                    }
                }
            }
        }

        /* kanan: preview + ringkasan */
        val rx = gx + cell * GRID_W + 28f
        val rw = w - rx - 20f
        c.panelJudul("Tampak Rakitan", rx, atas + 8f, rw, h * 0.26f)
        gambarKendaraan(c, app, rx + rw / 2f, atas + 8f + h * 0.15f, rw * 0.8f, h * 0.20f)
        val fh = app.build.frame
        if (fh != null) {
            c.txc(if (fh.complete()) "Rangka custom: siap" else "Rangka custom: belum lengkap",
                rx + rw / 2f, atas + h * 0.24f, T.sp(11f), if (fh.complete()) C.GREEN else C.AMBER, F_SEMI)
        }
        ringkasan(c, app, rx, atas + h * 0.28f, rw, h * 0.30f)

        /* info part terpilih */
        val iy = atas + h * 0.60f - 4f
        c.panelJudul("Info Part", rx, iy, rw, bawah - iy + 4f)
        val p = pid?.let { PART[it] }
        if (p != null) {
            c.tx(p.name, rx + 14f, iy + 42f, T.sp(15f), C.TEXT, F_BOLD)
            c.tx("${p.massKg} kg", rx + rw - 14f, iy + 42f, T.sp(12f), C.DIM, F_SEMI)
            c.wrap(p.desc, rx + 14f, iy + 62f, rw - 28f, T.sp(11.5f), C.DIM)
            c.tx("Harga ${rupiah(p.price)}", rx + 14f, iy + 112f, T.sp(12f), C.AMBER, F_SEMI)
        }

        /* palet bawah */
        c.rr(0f, tList, w, h - tList, 0f, T.fill(C.BG2))
        idMode[0] = tombol(c, "Pasang", 20f, tList + 8f, 96f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 0)
        idMode[1] = tombol(c, "Cat", 124f, tList + 8f, 78f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 1)
        idMode[2] = tombol(c, "Hapus", 210f, tList + 8f, 90f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 2)
        idLengkapi = tombol(c, "Lengkapi", 312f, tList + 8f, 104f, 34f, BTN_UTAMA, T.sp(13f))
        idBersih = tombol(c, "Kosongkan", 424f, tList + 8f, 112f, 34f, BTN_BAHAYA, T.sp(13f))
        idServis = tombol(c, "Servis", 544f, tList + 8f, 92f, 34f, BTN_NORMAL, T.sp(13f))
        idServisKurang = tombol(c, "Servis 45%", 644f, tList + 8f, 112f, 34f, BTN_NORMAL, T.sp(13f))
        idPreset = tombol(c, "Preset", 764f, tList + 8f, 92f, 34f, BTN_HANTU, T.sp(13f))

        /* warna cat */
        var wx = 880f
        for (i in C.PAINTS.indices) {
            val dipilih = app.build.colorIdx == i
            c.rr(wx, tList + 10f, 30f, 30f, 6f, T.fill(C.PAINTS[i]))
            if (dipilih) c.rrs(wx - 2f, tList + 8f, 34f, 34f, 8f, T.stroke(C.WHITE, 2f))
            idWarna[i] = add(wx, tList + 10f, 30f, 30f)
            wx += 34f
            if (wx > w - 40f) break
        }

        /* tab kategori */
        var kx = 20f
        val ky = tList + 50f
        for ((id, nama) in CATEGORIES) {
            val tw = T.p(C.TEXT, T.sp(12f), F_SEMI).measureText(nama) + 22f
            idKat[id] = tombol(c, nama, kx, ky, tw, 30f, BTN_NORMAL, T.sp(12f), true, 0, kat == id)
            kx += tw + 6f
            if (kx > w - 20f) break
        }

        /* daftar part */
        val ly = ky + 38f
        val lh = h - ly - 10f
        val daftar = partsByCat(kat)
        val iw = 108f
        val total = daftar.size * iw
        scroll = clamp(scroll, min(0f, w - 40f - total), 0f)
        c.save()
        c.clipRect(20f, ly, w - 20f, ly + lh)
        for (i in daftar.indices) {
            val p2 = daftar[i]
            val x = 20f + scroll + i * iw
            if (x + iw < 10f || x > w) continue
            val dipilih = p2.id == pid
            c.rr(x, ly, iw - 8f, lh - 4f, 10f, T.fill(if (dipilih) C.PANEL2 else C.PANEL))
            if (dipilih) c.rrs(x, ly, iw - 8f, lh - 4f, 10f, T.stroke(C.ACC, 2f))
            gambarPart(c, p2, x + 14f, ly + 8f, iw - 36f, lh * 0.52f)
            c.txc(p2.short, x + (iw - 8f) / 2f, ly + lh - 30f, T.sp(11.5f), if (dipilih) C.ACC else C.TEXT, F_SEMI)
            c.txc(rupiah(p2.price), x + (iw - 8f) / 2f, ly + lh - 14f, T.sp(10f), C.DIM, F_REG)
            add(x, ly, iw - 8f, lh - 4f, 2000 + i)
        }
        c.restore()

        /* hantu part yang digeser */
        if (geserPid != null && geserDari != null) {
            val p3 = PART[geserPid]
            if (p3 != null) {
                val pp = T.fill(C.ACC); pp.alpha = 70
                c.rr(geserX, geserY, cell * p3.gw, cell * p3.gh, 8f, pp)
                c.rrs(geserX, geserY, cell * p3.gw, cell * p3.gh, 8f, T.stroke(if (geserOk) C.GREEN else C.RED, 2.5f))
                val pq = T.fill(C.PANEL); pq.alpha = 210
                c.rr(geserX, geserY, cell * p3.gw, cell * p3.gh, 8f, pq)
                gambarPart(c, p3, geserX, geserY, cell * p3.gw, cell * p3.gh)
            }
        }

        if (!app.spec.valid) {
            c.txc("Belum lengkap: ${app.spec.missing.take(4).joinToString(", ")}",
                w * 0.5f, tList - 12f, T.sp(12f), C.RED, F_MED)
        }
    }

    private fun selDiGrid(x: Float, y: Float): Pair<Int, Int>? {
        val cell = min(gridRect[2] / GRID_W, gridRect[3] / GRID_H)
        val c = ((x - gridRect[0]) / cell).toInt()
        val r = ((y - gridRect[1]) / cell).toInt()
        return if (c in 0 until GRID_W && r in 0 until GRID_H) c to r else null
    }

    private fun refresh() { app.bangunUlang() }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h == null) return
        when (h.id) {
            idMode[0] -> mode = 0
            idMode[1] -> mode = 1
            idMode[2] -> mode = 2
            idRangka -> app.pindah("rangka")
            idDyno -> app.pindah("dyno")
            idJalan -> { app.gantiMap(app.mapId); app.mulaiJalan() }
            idMenu -> { app.simpan(); app.pindah("menu") }
            idLengkapi -> lengkapi()
            idBersih -> { app.build.clear(); refresh(); app.toast.tampil("Grid dikosongkan", C.AMBER) }
            idServis -> app.servis(true)
            idServisKurang -> app.servis(false)
            idPreset -> app.pindah("menu")
            else -> {
                /* warna */
                for (i in idWarna.indices) if (h.id == idWarna[i]) { app.build.colorIdx = i; return }
                /* kategori */
                for ((k, bid) in idKat) if (h.id == bid) { kat = k; pid = partsByCat(k).firstOrNull()?.id; scroll = 0f; return }
                /* pilih part di palet */
                if (h.extra >= 2000) {
                    val daftar = partsByCat(kat)
                    val p = daftar.getOrNull(h.extra - 2000)
                    if (p != null) { pid = p.id; mode = 0; sel = -1 to -1 }
                    return
                }
            }
        }
    }

    override fun geser(x: Float, y: Float) {
        /* geser part yang sudah terpasang */
        if (geserDari == null) {
            val s = selDiGrid(x, y) ?: return
            val k = cellKey(s.first, s.second)
            val p = app.build.items[k]
            if (p != null && mode == 0) {
                val o = app.build.originOf(s.first, s.second)!!
                geserDari = o
                geserPid = p
                geserX = x - 20f
                geserY = y - 20f
                geserOk = true
            } else if (mode == 1) cat(x, y)
            else if (mode == 2) hapus(x, y)
            return
        }
        geserX = x - 20f
        geserY = y - 20f
        val t = selDiGrid(x, y)
        geserOk = if (t != null) app.build.canPlace(geserPid!!, t.first, t.second, cellKey(geserDari!!.first, geserDari!!.second)) else false
    }

    override fun lepas(x: Float, y: Float) {
        val dari = geserDari
        val dp = geserPid
        if (dari != null && dp != null) {
            val t = selDiGrid(x, y)
            val kunciLama = cellKey(dari.first, dari.second)
            if (t != null && (t.first != dari.first || t.second != dari.second) &&
                app.build.canPlace(dp, t.first, t.second, kunciLama)) {
                app.build.remove(dari.first, dari.second)
                app.build.place(dp, t.first, t.second)
                refresh()
            }
            geserDari = null; geserPid = null
            return
        }
        /* ketuk biasa */
        val s = selDiGrid(x, y) ?: return
        when (mode) {
            0 -> pasang(s.first, s.second)
            1 -> cat(x, y)
            2 -> hapus(x, y)
        }
    }

    private fun pasang(c: Int, r: Int) {
        val k = cellKey(c, r)
        if (app.build.items.containsKey(k)) { sel = c to r; pid = app.build.items[k]; return }
        val p = pid?.let { PART[it] } ?: return
        if (app.build.money < p.price) { app.toast.tampil("Uang tidak cukup buat ${p.name}", C.RED); return }
        if (!app.build.place(p.id, c, r)) { app.toast.tampil("Tempat bentrok atau keluar grid", C.AMBER); return }
        app.build.money -= p.price
        refresh()
        app.getar(12)
    }

    private fun cat(x: Float, y: Float) {
        val s = selDiGrid(x, y) ?: return
        val k = cellKey(s.first, s.second)
        if (app.build.items.containsKey(k)) return
        app.build.paint[k] = app.build.colorIdx
    }

    private fun hapus(x: Float, y: Float) {
        val s = selDiGrid(x, y) ?: return
        app.build.remove(s.first, s.second)
        refresh()
    }

    private fun lengkapi() {
        var n = 0
        for (id in urutanLengkap) {
            val p = PART[id] ?: continue
            if (app.build.items.values.contains(id)) continue
            if (app.build.money < p.price) continue
            var ok = false
            for (r in 0 until GRID_H) {
                for (c in 0 until GRID_W) {
                    if (app.build.place(id, c, r)) { ok = true; break }
                }
                if (ok) break
            }
            if (ok) { app.build.money -= p.price; n++ }
        }
        refresh()
        app.toast.tampil(if (n > 0) "$n part dipasang otomatis" else "Sudah lengkap atau uang kurang",
            if (n > 0) C.GREEN else C.DIM)
    }

    override fun tombolKembali(): Boolean { app.simpan(); app.pindah("menu"); return true }
}
