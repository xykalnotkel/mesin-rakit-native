package id.mesinrakit.scene

import android.graphics.Canvas
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.data.DRIVERS
import id.mesinrakit.data.MAPS
import id.mesinrakit.model.GameMap
import id.mesinrakit.ui.*
import kotlin.math.*

/* ============================================================
   PIT: pilih pengendara.
   ============================================================ */
class PitScene(app: App) : Scene(app) {
    private var idKembali = -1
    private val idPilih = HashMap<String, Int>()

    override fun draw(c: Canvas, w: Float, h: Float) {
        c.txc("PIT", w / 2f, h * 0.12f, T.sp(34f), C.TEXT, F_BOLD)
        c.txc("Berat dan gaya berkendara ikut mengubah akselerasi serta handling",
            w / 2f, h * 0.12f + 28f, T.sp(13f), C.DIM, F_REG)

        val n = DRIVERS.size
        val m = 24f
        val cw = (w - m * (n + 1)) / n
        val cy = h * 0.24f
        val ch = h * 0.58f
        for (i in 0 until n) {
            val d = DRIVERS[i]
            val x = m + i * (cw + m)
            val dipilih = app.build.driverId == d.id
            c.panel(x, cy, cw, ch, 16f, if (dipilih) C.PANEL2 else C.PANEL,
                if (dipilih) d.color else C.LINE, if (dipilih) 2.5f else 1.4f)
            c.txc(d.name, x + cw / 2f, cy + 34f, T.sp(20f), if (dipilih) d.color else C.TEXT, F_BOLD)
            c.txc(d.skill, x + cw / 2f, cy + 58f, T.sp(12f), C.DIM, F_MED)
            c.wrap(d.desc, x + 16f, cy + 84f, cw - 32f, T.sp(12f), C.DIM)

            var by = cy + ch * 0.46f
            fun bar(l: String, v: Double, maks: Double, warna: Int) {
                c.tx(l, x + 16f, by, T.sp(12f), C.DIM, F_REG)
                c.bar(x + 84f, by - 9f, cw - 100f, 10f, v / maks, warna)
                by += 26f
            }
            bar("Berat", d.mass, 110.0, C.AMBER)
            bar("Gas", d.throttle, 1.3, C.GREEN)
            bar("Rem", d.brake, 1.3, C.RED)
            bar("Seimbang", d.balance, 1.3, C.ACC)
            c.tx("Total kendaraan", x + 16f, by + 6f, T.sp(12f), C.DIM, F_REG)
            c.txr("${String.format("%.0f", app.spec.mass - app.spec.driver.mass + d.mass)} kg",
                x + cw - 16f, by + 6f, T.sp(12f), C.TEXT, F_SEMI)
            idPilih[d.id] = tombol(c, if (dipilih) "DIPAKAI" else "PILIH",
                x + 16f, cy + ch - 54f, cw - 32f, 38f,
                if (dipilih) BTN_UTAMA else BTN_NORMAL, T.sp(14f), !dipilih)
        }
        idKembali = tombol(c, "Kembali", w / 2f - 70f, h - 62f, 140f, 42f, BTN_HANTU, T.sp(14f))
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h == null) return
        if (h.id == idKembali) { app.pindah("menu"); return }
        for ((id, bid) in idPilih) {
            if (h.id == bid) {
                app.build.driverId = id
                app.bangunUlang()
                app.simpan()
                app.getar(25)
                return
            }
        }
    }

    override fun tombolKembali(): Boolean { app.pindah("menu"); return true }
}

/* ============================================================
   PETA: pilih lintasan.
   ============================================================ */
class PetaScene(app: App) : Scene(app) {
    private var idKembali = -1
    private var idMulai = -1
    private val idMap = HashMap<String, Int>()
    private val preview = HashMap<String, GameMap>()

    override fun draw(c: Canvas, w: Float, h: Float) {
        c.txc("PILIH LINTASAN", w / 2f, h * 0.12f, T.sp(30f), C.TEXT, F_BOLD)

        val n = MAPS.size
        val m = 20f
        val cw = (w - m * (n + 1)) / n
        val cy = h * 0.22f
        val ch = h * 0.56f
        for (i in 0 until n) {
            val md = MAPS[i]
            val x = m + i * (cw + m)
            val dipilih = app.mapId == md.id
            c.panel(x, cy, cw, ch, 16f, if (dipilih) C.PANEL2 else C.PANEL,
                if (dipilih) C.ACC else C.LINE, if (dipilih) 2.5f else 1.4f)
            c.txc(md.name, x + cw / 2f, cy + 30f, T.sp(17f), if (dipilih) C.ACC else C.TEXT, F_BOLD)

            /* gambar potongan tanah */
            val gx = x + 14f
            val gy = cy + 48f
            val gw = cw - 28f
            val gh = ch * 0.36f
            c.rr(gx, gy, gw, gh, 8f, T.fill(md.pal.sky0))
            val mp = preview.getOrPut(md.id) { GameMap(md) }
            var minY = Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            val N = 60
            for (k in 0..N) {
                val yv = mp.groundY(md.panjang * k / N.toDouble())
                minY = min(minY, yv); maxY = max(maxY, yv)
            }
            val span = (maxY - minY).coerceAtLeast(1.0)
            val path = android.graphics.Path()
            path.moveTo(gx, gy + gh)
            for (k in 0..N) {
                val px = gx + gw * k / N
                val yv = mp.groundY(md.panjang * k / N.toDouble())
                val py = gy + gh - ((yv - minY) / span * gh * 0.6f + gh * 0.18f)
                path.lineTo(px.toFloat(), py.toFloat())
            }
            path.lineTo(gx + gw, gy + gh)
            path.close()
            c.drawPath(path, T.fill(md.pal.road))
            c.drawPath(path, T.stroke(md.pal.line, 1.6f))

            c.wrap(md.desc, x + 14f, gy + gh + 24f, cw - 28f, T.sp(12f), C.DIM)
            c.txc("${md.panjang} m", x + cw / 2f, cy + ch - 58f, T.sp(13f), C.DIM, F_SEMI)
            idMap[md.id] = tombol(c, if (dipilih) "DIPILIH" else "PILIH",
                x + 14f, cy + ch - 48f, cw - 28f, 38f,
                if (dipilih) BTN_UTAMA else BTN_NORMAL, T.sp(14f), !dipilih)
        }
        idMulai = tombol(c, "MULAI JALAN", w / 2f - 110f, h - 70f, 220f, 48f, BTN_UTAMA, T.sp(16f), app.spec.valid)
        idKembali = tombol(c, "Kembali", 24f, h - 62f, 120f, 40f, BTN_HANTU, T.sp(13f))
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h == null) return
        if (h.id == idKembali) { app.pindah("menu"); return }
        if (h.id == idMulai) { app.gantiMap(app.mapId); app.mulaiJalan(); return }
        for ((id, bid) in idMap) {
            if (h.id == bid) {
                app.gantiMap(id)
                app.getar(20)
                app.toast.tampil("Lintasan ${MAPS.first { it.id == id }.name} dipilih", C.ACC)
                return
            }
        }
    }

    override fun tombolKembali(): Boolean { app.pindah("menu"); return true }
}
