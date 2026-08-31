package id.mesinrakit.model

import id.mesinrakit.data.PART
import id.mesinrakit.data.Part

/* ============================================================
   Rakitan: part yang terpasang di grid, cat body, kerusakan,
   dan desain rangka buatan sendiri.
   ============================================================ */
const val GRID_W = 14
const val GRID_H = 6
fun cellKey(c: Int, r: Int) = "$c,$r"

class Build {
    val items = LinkedHashMap<String, String>()  // "kolom,baris" -> id part
    val paint = LinkedHashMap<String, Int>()     // "kolom,baris" -> indeks warna
    val dmg = HashMap<String, Double>()          // "kolom,baris" -> 0..1
    var colorIdx: Int = 0
    var driverId: String = "budi"
    var money: Int = 1_500_000
    var frame: FrameDesign? = null
    var name: String = "Rakitanku"

    fun partAt(c: Int, r: Int): Part? = PART[items[cellKey(c, r)]]
    fun allParts(): List<Part> = items.values.mapNotNull { PART[it] }
    fun cellsOf(pid: String): List<String> = items.filter { it.value == pid }.keys.toList()

    /** pasang part di kiri-atas (c,r); kembalikan false kalau menabrak part lain */
    fun canPlace(pid: String, c: Int, r: Int, ignore: String? = null): Boolean {
        val p = PART[pid] ?: return false
        if (c < 0 || r < 0 || c + p.gw > GRID_W || r + p.gh > GRID_H) return false
        for (i in 0 until p.gw) for (j in 0 until p.gh) {
            val k = cellKey(c + i, r + j)
            if (k == ignore) continue
            if (items.containsKey(k)) return false
        }
        return true
    }

    fun place(pid: String, c: Int, r: Int): Boolean {
        val p = PART[pid] ?: return false
        if (!canPlace(pid, c, r)) return false
        for (i in 0 until p.gw) for (j in 0 until p.gh) items[cellKey(c + i, r + j)] = pid
        return true
    }

    /** cari sel asal dari sebuah sel yang ditempati part */
    fun originOf(c: Int, r: Int): Pair<Int, Int>? {
        val pid = items[cellKey(c, r)] ?: return null
        var cc = c; var rr = r
        while (cc > 0 && items[cellKey(cc - 1, rr)] == pid) cc--
        while (rr > 0 && items[cellKey(cc, rr - 1)] == pid) rr--
        return cc to rr
    }

    fun remove(c: Int, r: Int): String? {
        val o = originOf(c, r) ?: run {
            val k = cellKey(c, r)
            if (paint.containsKey(k)) { paint.remove(k); return null }
            return null
        }
        val pid = items[cellKey(o.first, o.second)] ?: return null
        val p = PART[pid] ?: return null
        for (i in 0 until p.gw) for (j in 0 until p.gh) {
            val k = cellKey(o.first + i, o.second + j)
            items.remove(k); paint.remove(k); dmg.remove(k)
        }
        return pid
    }

    fun clear() { items.clear(); paint.clear(); dmg.clear() }

    fun copy(): Build {
        val b = Build()
        b.items.putAll(items); b.paint.putAll(paint); b.dmg.putAll(dmg)
        b.colorIdx = colorIdx; b.driverId = driverId; b.money = money
        b.name = name; b.frame = frame?.copy()
        return b
    }
}

/* ============================================================
   Desain rangka buatan sendiri: simpul (node) dan pipa (segmen).
   Dipakai editor "lipat besi" di bengkel rangka.
   ============================================================ */
data class Node(var x: Double, var y: Double, var bow: Double = 0.0, var kind: String = "free") {
    companion object {
        const val HEADSTOCK = "headstock"   // dudukan stang
        const val ENGINE = "engine"         // dudukan mesin
        const val AXLE_R = "axleR"          // dudukan as roda belakang
        const val SEAT = "seat"             // dudukan jok
        const val FREE = "free"
    }
}
data class Tube(val a: Int, val b: Int, var dia: Double = 0.032, var thick: Double = 0.002, var mat: String = "besi")

class FrameDesign {
    val nodes = ArrayList<Node>()
    val tubes = ArrayList<Tube>()
    var material: String = "besi"

    val materials = linkedMapOf(
        "besi" to (7850.0 to 1.0),         // kg/m3 , faktor kekuatan
        "alumunium" to (2700.0 to 0.78),
        "chrome" to (7900.0 to 1.25),
        "titanium" to (4500.0 to 1.7)
    )

    fun addNode(x: Double, y: Double, kind: String = Node.FREE): Int { nodes.add(Node(x, y, 0.0, kind)); return nodes.size - 1 }
    fun addTube(a: Int, b: Int, dia: Double = 0.032, thick: Double = 0.002, mat: String = material): Boolean {
        if (a == b || a !in nodes.indices || b !in nodes.indices) return false
        tubes.add(Tube(a, b, dia, thick, mat)); return true
    }

    /** panjang total pipa (meter) */
    fun length(): Double {
        var s = 0.0
        for (t in tubes) {
            val a = nodes[t.a]; val b = nodes[t.b]
            val bow = kotlin.math.max(kotlin.math.abs(a.bow), kotlin.math.abs(b.bow))
            s += kotlin.math.hypot(b.x - a.x, b.y - a.y) * (1 + bow * 0.22 + 0.05)
        }
        return s
    }

    /** massa total (kg) dari panjang pipa, diameter, dan bahan */
    fun mass(): Double {
        var m = 0.0
        for (t in tubes) {
            val a = nodes[t.a]; val b = nodes[t.b]
            val bow = kotlin.math.max(kotlin.math.abs(a.bow), kotlin.math.abs(b.bow))
            val len = kotlin.math.hypot(b.x - a.x, b.y - a.y) * (1 + bow * 0.22 + 0.05)
            val rho = materials[t.mat]?.first ?: 7850.0
            val area = Math.PI * (t.dia * t.dia - (t.dia - 2 * t.thick) * (t.dia - 2 * t.thick)) / 4.0
            m += len * area * rho
        }
        return m + nodes.size * 0.18
    }

    /** kekuatan relatif: pipa tebal dan bahan kuat menang, bentang panjang melemah */
    fun strength(): Double {
        if (tubes.isEmpty()) return 0.0
        var s = 0.0
        for (t in tubes) {
            val a = nodes[t.a]; val b = nodes[t.b]
            val len = kotlin.math.max(0.05, kotlin.math.hypot(b.x - a.x, b.y - a.y))
            val f = materials[t.mat]?.second ?: 1.0
            s += (t.dia * 30.0) * (t.thick * 500.0) * f / len
        }
        return kotlin.math.min(2.2, s / tubes.size)
    }

    fun hasKind(k: String) = nodes.any { it.kind == k }
    fun complete() = hasKind(Node.HEADSTOCK) && hasKind(Node.ENGINE) && hasKind(Node.AXLE_R)

    fun bounds(): DoubleArray {
        if (nodes.isEmpty()) return doubleArrayOf(0.0, 0.0, 1.0, 1.0)
        var x0 = Double.MAX_VALUE; var y0 = Double.MAX_VALUE
        var x1 = -Double.MAX_VALUE; var y1 = -Double.MAX_VALUE
        for (n in nodes) {
            x0 = kotlin.math.min(x0, n.x); x1 = kotlin.math.max(x1, n.x)
            y0 = kotlin.math.min(y0, n.y); y1 = kotlin.math.max(y1, n.y)
        }
        if (x1 - x0 < 0.01) x1 = x0 + 0.01
        if (y1 - y0 < 0.01) y1 = y0 + 0.01
        return doubleArrayOf(x0, y0, x1, y1)
    }

    fun copy(): FrameDesign {
        val f = FrameDesign()
        f.nodes.addAll(nodes.map { it.copy() })
        f.tubes.addAll(tubes.map { it.copy() })
        f.material = material
        return f
    }

    companion object {
        /** rangka dasar pabrikan yang bisa dibongkar pasang di editor */
        fun preset(kind: String): FrameDesign {
            val f = FrameDesign()
            when (kind) {
                "underbone" -> {
                    val hs = f.addNode(0.62, 0.78, Node.HEADSTOCK)
                    val e1 = f.addNode(0.30, 0.50, Node.ENGINE)
                    val e2 = f.addNode(0.05, 0.46, Node.ENGINE)
                    val ax = f.addNode(-0.60, 0.44, Node.AXLE_R)
                    val st = f.addNode(-0.18, 0.72, Node.SEAT)
                    val top = f.addNode(0.20, 0.66, Node.FREE)
                    f.addTube(hs, top, 0.042, 0.0025); f.addTube(top, st, 0.038, 0.002)
                    f.addTube(hs, e1, 0.038, 0.0025); f.addTube(e1, e2, 0.036, 0.002)
                    f.addTube(e2, ax, 0.032, 0.002); f.addTube(e1, ax, 0.030, 0.0018)
                    f.addTube(st, ax, 0.028, 0.0018); f.addTube(top, e2, 0.026, 0.0016)
                }
                "deltabox" -> {
                    val hs = f.addNode(0.72, 0.80, Node.HEADSTOCK)
                    val e1 = f.addNode(0.34, 0.54, Node.ENGINE)
                    val e2 = f.addNode(0.02, 0.50, Node.ENGINE)
                    val ax = f.addNode(-0.68, 0.46, Node.AXLE_R)
                    val st = f.addNode(-0.24, 0.80, Node.SEAT)
                    val tl = f.addNode(0.28, 0.74, Node.FREE)
                    val bl = f.addNode(0.10, 0.40, Node.FREE)
                    f.addTube(hs, tl, 0.050, 0.003); f.addTube(tl, st, 0.044, 0.0025)
                    f.addTube(hs, e1, 0.046, 0.003); f.addTube(e1, bl, 0.040, 0.0025)
                    f.addTube(bl, e2, 0.040, 0.0025); f.addTube(e2, ax, 0.038, 0.0022)
                    f.addTube(e1, ax, 0.034, 0.002); f.addTube(st, ax, 0.032, 0.002)
                    f.addTube(tl, bl, 0.030, 0.002); f.addTube(bl, ax, 0.028, 0.0018)
                }
                "trail" -> {
                    val hs = f.addNode(0.70, 0.98, Node.HEADSTOCK)
                    val e1 = f.addNode(0.32, 0.62, Node.ENGINE)
                    val e2 = f.addNode(0.00, 0.56, Node.ENGINE)
                    val ax = f.addNode(-0.70, 0.52, Node.AXLE_R)
                    val st = f.addNode(-0.30, 0.92, Node.SEAT)
                    val mid = f.addNode(0.16, 0.82, Node.FREE)
                    f.addTube(hs, mid, 0.046, 0.0028); f.addTube(mid, st, 0.040, 0.0022)
                    f.addTube(hs, e1, 0.042, 0.0028); f.addTube(e1, e2, 0.038, 0.0024)
                    f.addTube(e2, ax, 0.036, 0.0022); f.addTube(e1, ax, 0.030, 0.0018)
                    f.addTube(st, ax, 0.030, 0.0018); f.addTube(mid, e2, 0.028, 0.0018)
                }
                else -> {
                    val hs = f.addNode(0.60, 0.80, Node.HEADSTOCK)
                    val e = f.addNode(0.10, 0.50, Node.ENGINE)
                    val ax = f.addNode(-0.60, 0.46, Node.AXLE_R)
                    val st = f.addNode(-0.20, 0.76, Node.SEAT)
                    f.addTube(hs, e); f.addTube(e, ax); f.addTube(hs, st); f.addTube(st, ax)
                }
            }
            return f
        }
    }
}
