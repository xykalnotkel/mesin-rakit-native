package id.mesinrakit.scene

import android.graphics.Canvas
import android.graphics.Path
import id.mesinrakit.App
import id.mesinrakit.core.*
import id.mesinrakit.model.FrameDesign
import id.mesinrakit.model.Node
import id.mesinrakit.model.Tube
import id.mesinrakit.ui.*
import kotlin.math.*

/* ============================================================
   BENGKEL RANGKA: editor simpul dan pipa. Taruh titik, tarik
   jadi pipa, tekuk, atur diameter dan bahan. Hasilnya jadi
   rangka yang dipakai kendaraanmu.
   ============================================================ */
class RangkaScene(app: App) : Scene(app) {
    private var f = app.build.frame?.copy() ?: FrameDesign.preset("underbone")
    private var mode = 0          // 0 pilih, 1 simpul, 2 pipa, 3 hapus
    private var pilih = -1        // indeks simpul yang dipilih
    private var tarikDari = -1
    private var tarikX = 0f
    private var tarikY = 0f
    private var area = floatArrayOf(0f, 0f, 0f, 0f)
    private var idMode = IntArray(4) { -1 }
    private var idSimpan = -1
    private var idReset = -1
    private var idKembali = -1
    private val idPreset = HashMap<String, Int>()
    private val idJenis = HashMap<String, Int>()
    private var idDiaM = -1
    private var idDiaP = -1
    private var idTebalM = -1
    private var idTebalP = -1
    private var idBahan = -1
    private var idTekukM = -1
    private var idTekukP = -1
    private var idHapus = -1

    private val W_MIN = -0.9
    private val W_MAX = 0.9
    private val H_MIN = -0.1
    private val H_MAX = 1.15

    private fun keLayar(wx: Double, wy: Double): FloatArray {
        val sx = area[0] + ((wx - W_MIN) / (W_MAX - W_MIN)).toFloat() * area[2]
        val sy = area[1] + (1f - ((wy - H_MIN) / (H_MAX - H_MIN)).toFloat()) * area[3]
        return floatArrayOf(sx, sy)
    }
    private fun keDunia(sx: Float, sy: Float): DoubleArray {
        val wx = W_MIN + ((sx - area[0]) / area[2]).toDouble() * (W_MAX - W_MIN)
        val wy = H_MIN + (1.0 - ((sy - area[1]) / area[3]).toDouble()) * (H_MAX - H_MIN)
        return doubleArrayOf(wx, wy)
    }

    override fun draw(c: Canvas, w: Float, h: Float) {
        val atas = h * 0.10f
        c.rr(0f, 0f, w, atas, 0f, T.fill(C.BG2))
        c.tx("BENGKEL RANGKA", 24f, atas * 0.62f, T.sp(19f), C.TEXT, F_BOLD)
        c.tx("Taruh simpul, tarik jadi pipa, tekuk, lalu simpan ke rakitan", 250f, atas * 0.62f, T.sp(12f), C.DIM, F_REG)

        val panelW = w * 0.26f
        area = floatArrayOf(20f, atas + 8f, w - panelW - 40f, h - atas - 20f)
        c.panel(area[0], area[1], area[2], area[3], 12f)

        /* kisi meter */
        var g = 0.0
        while (g <= W_MAX) {
            val p = keLayar(g, 0.0)
            c.drawLine(p[0], area[1], p[0], area[1] + area[3], T.stroke(C.LINE, 1f))
            g += 0.1
        }
        var gy = 0.0
        while (gy <= H_MAX) {
            val p = keLayar(0.0, gy)
            c.drawLine(area[0], p[1], area[0] + area[2], p[1], T.stroke(C.LINE, 1f))
            gy += 0.1
        }
        /* garis tanah */
        val gt = keLayar(0.0, 0.0)
        c.drawLine(area[0], gt[1], area[0] + area[2], gt[1], T.stroke(C.LINE2, 2f))

        /* pipa */
        for (t in f.tubes) {
            val a = f.nodes.getOrNull(t.a) ?: continue
            val b = f.nodes.getOrNull(t.b) ?: continue
            val pa = keLayar(a.x, a.y)
            val pb = keLayar(b.x, b.y)
            val bow = (a.bow + b.bow) / 2.0
            val wid = max(3f, (t.dia / 0.032).toFloat() * max(4f, area[3] * 0.016f))
            val path = Path()
            path.moveTo(pa[0], pa[1])
            if (abs(bow) > 0.005) {
                val mx = (pa[0] + pb[0]) / 2f
                val my = (pa[1] + pb[1]) / 2f
                val dx = pb[0] - pa[0]
                val dy = pb[1] - pa[1]
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
                val nx = -dy / len
                val ny = dx / len
                path.quadTo(mx + nx * bow.toFloat() * len * 0.55f, my + ny * bow.toFloat() * len * 0.55f, pb[0], pb[1])
            } else path.lineTo(pb[0], pb[1])
            c.drawPath(path, T.stroke(warnaBahan(t.mat), wid))
        }

        /* tarik pipa baru */
        if (tarikDari >= 0) {
            val a = f.nodes[tarikDari]
            val pa = keLayar(a.x, a.y)
            c.drawLine(pa[0], pa[1], tarikX, tarikY, T.stroke(C.ACC, 3f))
        }

        /* simpul */
        for (i in f.nodes.indices) {
            val n = f.nodes[i]
            val p = keLayar(n.x, n.y)
            val r = 9f
            val col = when (n.kind) {
                Node.HEADSTOCK -> C.ACC
                Node.ENGINE -> C.AMBER
                Node.AXLE_R -> C.GREEN
                Node.SEAT -> C.PURPLE
                else -> C.TEXT
            }
            c.dot(p[0], p[1], r + 3f, T.fill(C.BG))
            c.dot(p[0], p[1], r, T.fill(col))
            if (i == pilih) c.dot(p[0], p[1], r + 6f, T.stroke(C.WHITE, 2f))
            c.txc(labelJenis(n.kind), p[0], p[1] - 20f, T.sp(10f), col, F_BOLD)
        }

        /* panel kanan */
        val px = w - panelW - 12f
        var py = atas + 8f
        idMode[0] = tombol(c, "Pilih", px, py, panelW / 2f - 4f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 0)
        idMode[1] = tombol(c, "Simpul", px + panelW / 2f + 4f, py, panelW / 2f - 4f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 1)
        py += 40f
        idMode[2] = tombol(c, "Pipa", px, py, panelW / 2f - 4f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 2)
        idMode[3] = tombol(c, "Hapus", px + panelW / 2f + 4f, py, panelW / 2f - 4f, 34f, BTN_NORMAL, T.sp(13f), true, 0, mode == 3)

        py += 48f
        c.panelJudul("Simpul terpilih", px, py, panelW, h * 0.34f)
        val n = f.nodes.getOrNull(pilih)
        if (n == null) {
            c.tx("Ketuk simpul buat mengatur", px + 14f, py + 62f, T.sp(12f), C.DIM)
        } else {
            c.tx("Jenis dudukan", px + 14f, py + 62f, T.sp(12f), C.DIM, F_REG)
            var jx = px + 14f
            var jy = py + 74f
            for ((k, lbl) in listOf(Node.FREE to "Bebas", Node.HEADSTOCK to "Stang",
                    Node.ENGINE to "Mesin", Node.AXLE_R to "As Roda", Node.SEAT to "Jok")) {
                val tw = T.p(C.TEXT, T.sp(11f), F_SEMI).measureText(lbl) + 18f
                if (jx + tw > px + panelW - 12f) { jx = px + 14f; jy += 28f }
                idJenis[k] = tombol(c, lbl, jx, jy, tw, 24f, BTN_NORMAL, T.sp(11f), true, 0, n.kind == k)
                jx += tw + 6f
            }
            var yy = jy + 38f
            c.tx("Lengkung pipa", px + 14f, yy, T.sp(12f), C.DIM, F_REG)
            idTekukM = tombol(c, "-", px + 130f, yy - 12f, 34f, 26f, BTN_NORMAL, T.sp(14f))
            c.txc(String.format("%.2f", n.bow), px + 168f, yy, T.sp(12f), C.TEXT, F_NUM_SEMI)
            idTekukP = tombol(c, "+", px + 186f, yy - 12f, 34f, 26f, BTN_NORMAL, T.sp(14f))
            yy += 34f
            idHapus = tombol(c, "Hapus simpul", px + 130f, yy - 12f, 90f, 26f, BTN_BAHAYA, T.sp(11f))
        }

        /* pengaturan pipa */
        py += h * 0.34f + 12f
        c.panelJudul("Pipa baru & bahan", px, py, panelW, h * 0.20f)
        c.tx("Diameter", px + 14f, py + 62f, T.sp(12f), C.DIM, F_REG)
        idDiaM = tombol(c, "-", px + 120f, py + 50f, 34f, 26f, BTN_NORMAL, T.sp(14f))
        c.txc(String.format("%.0f mm", Tube(0, 0).let { f.tubes.lastOrNull()?.dia ?: 0.032 } * 1000),
            px + 168f, py + 62f, T.sp(12f), C.TEXT, F_NUM_SEMI)
        idDiaP = tombol(c, "+", px + 200f, py + 50f, 34f, 26f, BTN_NORMAL, T.sp(14f))
        c.tx("Ketebalan", px + 14f, py + 92f, T.sp(12f), C.DIM, F_REG)
        idTebalM = tombol(c, "-", px + 120f, py + 80f, 34f, 26f, BTN_NORMAL, T.sp(14f))
        c.txc(String.format("%.1f mm", Tube(0, 0).let { f.tubes.lastOrNull()?.thick ?: 0.002 } * 1000),
            px + 168f, py + 92f, T.sp(12f), C.TEXT, F_NUM_SEMI)
        idTebalP = tombol(c, "+", px + 200f, py + 80f, 34f, 26f, BTN_NORMAL, T.sp(14f))
        idBahan = tombol(c, "Bahan: ${f.material}", px + 14f, py + 112f, panelW - 28f, 30f, BTN_NORMAL, T.sp(12f))

        /* preset & hasil */
        py += h * 0.20f + 12f
        c.panelJudul("Preset & hasil", px, py, panelW, h - py - 60f)
        var q = px + 14f
        for (p in listOf("underbone" to "Underbone", "deltabox" to "Deltabox", "trail" to "Trail")) {
            val tw = T.p(C.TEXT, T.sp(11f), F_SEMI).measureText(p.second) + 18f
            idPreset[p.first] = tombol(c, p.second, q, py + 34f, tw, 26f, BTN_NORMAL, T.sp(11f))
            q += tw + 6f
        }
        var ry = py + 74f
        fun baris(l: String, v: String, warna: Int = C.TEXT) {
            c.tx(l, px + 14f, ry, T.sp(12f), C.DIM, F_REG)
            c.txr(v, px + panelW - 14f, ry, T.sp(12f), warna, F_SEMI)
            ry += 20f
        }
        baris("Panjang pipa", "${String.format("%.2f", f.length())} m")
        baris("Massa rangka", "${String.format("%.1f", f.mass())} kg")
        baris("Kekuatan", "${String.format("%.2f", f.strength())}",
            if (f.strength() > 1.1) C.GREEN else if (f.strength() > 0.6) C.AMBER else C.RED)
        baris("Simpul / pipa", "${f.nodes.size} / ${f.tubes.size}")
        ry += 6f
        c.tx(if (f.complete()) "Syarat dudukan terpenuhi" else "Butuh dudukan stang, mesin, as roda belakang",
            px + 14f, ry, T.sp(11.5f), if (f.complete()) C.GREEN else C.RED, F_SEMI)

        idSimpan = tombol(c, "Simpan ke rakitan", px, h - 54f, panelW * 0.55f, 40f, BTN_UTAMA, T.sp(14f), f.complete())
        idReset = tombol(c, "Reset", px + panelW * 0.58f, h - 54f, panelW * 0.40f, 40f, BTN_BAHAYA, T.sp(13f))
        idKembali = tombol(c, "Kembali", 20f, h - 54f, 110f, 40f, BTN_HANTU, T.sp(13f))
    }

    private fun warnaBahan(m: String) = when (m) {
        "alumunium" -> 0xFFC9D3E0.toInt()
        "chrome" -> 0xFFE2E8F0.toInt()
        "titanium" -> 0xFFB8A9C9.toInt()
        else -> 0xFF8A97A8.toInt()
    }
    private fun labelJenis(k: String) = when (k) {
        Node.HEADSTOCK -> "stang"; Node.ENGINE -> "mesin"; Node.AXLE_R -> "as roda"
        Node.SEAT -> "jok"; else -> ""
    }

    private fun simpulDi(sx: Float, sy: Float): Int {
        var best = -1
        var bd = 26.0
        for (i in f.nodes.indices) {
            val p = keLayar(f.nodes[i].x, f.nodes[i].y)
            val d = hypot((p[0] - sx).toDouble(), (p[1] - sy).toDouble())
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    override fun press(h: Hot?, x: Float, y: Float) {
        if (h != null) {
            when (h.id) {
                idMode[0] -> mode = 0
                idMode[1] -> mode = 1
                idMode[2] -> mode = 2
                idMode[3] -> mode = 3
                idDiaM -> f.tubes.lastOrNull()?.let { it.dia = clamp(it.dia - 0.004, 0.016, 0.070) }
                idDiaP -> f.tubes.lastOrNull()?.let { it.dia = clamp(it.dia + 0.004, 0.016, 0.070) }
                idTebalM -> f.tubes.lastOrNull()?.let { it.thick = clamp(it.thick - 0.0005, 0.0008, 0.006) }
                idTebalP -> f.tubes.lastOrNull()?.let { it.thick = clamp(it.thick + 0.0005, 0.0008, 0.006) }
                idBahan -> {
                    val daftar = f.materials.keys.toList()
                    val i = (daftar.indexOf(f.material) + 1) % daftar.size
                    f.material = daftar[i]
                    for (t in f.tubes) t.mat = f.material
                }
                idTekukM -> f.nodes.getOrNull(pilih)?.let { it.bow = clamp(it.bow - 0.08, -0.8, 0.8) }
                idTekukP -> f.nodes.getOrNull(pilih)?.let { it.bow = clamp(it.bow + 0.08, -0.8, 0.8) }
                idHapus -> hapusSimpul(pilih)
                idReset -> { f = FrameDesign(); pilih = -1 }
                idSimpan -> simpan()
                idKembali -> app.pindah("bengkel")
                else -> {
                    for ((k, bid) in idJenis) if (h.id == bid) f.nodes.getOrNull(pilih)?.let { it.kind = k }
                    for ((k, bid) in idPreset) if (h.id == bid) { f = FrameDesign.preset(k); pilih = -1 }
                }
            }
            return
        }
        /* sentuh di area kerja */
        if (x < area[0] || x > area[0] + area[2] || y < area[1] || y > area[1] + area[3]) return
        val idx = simpulDi(x, y)
        when (mode) {
            1 -> {   /* taruh simpul */
                if (idx >= 0) { pilih = idx; return }
                val d = keDunia(x, y)
                pilih = f.addNode(d[0].coerceIn(W_MIN, W_MAX), d[1].coerceIn(H_MIN, H_MAX))
                app.getar(12)
            }
            2 -> {   /* tarik pipa */
                if (idx >= 0) { tarikDari = idx; tarikX = x; tarikY = y }
            }
            3 -> {   /* hapus */
                if (idx >= 0) hapusSimpul(idx)
            }
            else -> pilih = idx
        }
    }

    override fun geser(x: Float, y: Float) {
        if (tarikDari >= 0) { tarikX = x; tarikY = y; return }
        if (mode == 0 && pilih >= 0 && pressed.isEmpty()) {
            val d = keDunia(x, y)
            f.nodes.getOrNull(pilih)?.let { n ->
                n.x = d[0].coerceIn(W_MIN, W_MAX)
                n.y = d[1].coerceIn(H_MIN, H_MAX)
            }
        }
    }

    override fun lepas(x: Float, y: Float) {
        if (tarikDari >= 0) {
            val ke = simpulDi(x, y)
            val dia = f.tubes.lastOrNull()?.dia ?: 0.032
            val tebal = f.tubes.lastOrNull()?.thick ?: 0.002
            if (ke >= 0 && ke != tarikDari) f.addTube(tarikDari, ke, dia, tebal, f.material)
            tarikDari = -1
        }
    }

    private fun hapusSimpul(i: Int) {
        if (i !in f.nodes.indices) return
        f.nodes.removeAt(i)
        val sisa = ArrayList<Tube>()
        for (t in f.tubes) {
            val a = if (t.a > i) t.a - 1 else t.a
            val b = if (t.b > i) t.b - 1 else t.b
            if (t.a == i || t.b == i) continue
            sisa.add(Tube(a, b, t.dia, t.thick, t.mat))
        }
        f.tubes.clear(); f.tubes.addAll(sisa)
        pilih = -1
        app.getar(20)
    }

    private fun simpan() {
        if (!f.complete()) { app.toast.tampil("Rangka belum punya dudukan stang, mesin, dan as roda", C.RED); return }
        app.build.frame = f.copy()
        app.bangunUlang()
        app.simpan()
        app.getar(30)
        app.toast.tampil("Rangka custom disimpan (${String.format("%.1f", f.mass())} kg)", C.GREEN)
    }

    override fun tombolKembali(): Boolean { app.pindah("bengkel"); return true }
}
