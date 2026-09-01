package id.mesinrakit.data

import id.mesinrakit.model.Build
import id.mesinrakit.model.FrameDesign
import id.mesinrakit.model.Node
import id.mesinrakit.model.Tube
import id.mesinrakit.model.cellKey

/**
 * Format tukar desain MESIN RAKIT. Satu berkas teks yang sama dipakai
 * di game (tempel clipboard) dan di studio web.
 *
 * Awal berkas wajib: MRPACK1
 * Tiap baris: kunci=nilai
 *
 * kind     pack | body | frame | part
 * name     nama desain
 * author   pembuat
 * color    indeks cat 0..11
 * driver   id pengendara
 * items    kolom,baris:id;...
 * paint    kolom,baris:indeks;...
 * fnodes   x:y:bow:kind;...
 * ftubes   a:b:dia:thick:mat;...
 * fmat     besi | alumunium | chrome | titanium
 * paths    path SVG dinormalisasi 0..1 (opsional, studio web)
 * partcat  kategori part usulan
 */
object Pack {
    const val MAGIC = "MRPACK1"

    data class Data(
        var kind: String = "pack",
        var name: String = "Desain",
        var author: String = "",
        var color: Int = 0,
        var driver: String = "budi",
        var items: LinkedHashMap<String, String> = LinkedHashMap(),
        var paint: LinkedHashMap<String, Int> = LinkedHashMap(),
        var nodes: ArrayList<Node> = ArrayList(),
        var tubes: ArrayList<Tube> = ArrayList(),
        var fmat: String = "besi",
        var paths: String = "",
        var partcat: String = ""
    )

    fun encode(b: Build, kind: String = "pack", author: String = ""): String {
        val sb = StringBuilder()
        sb.append(MAGIC).append('\n')
        sb.append("v=1\n")
        sb.append("kind=").append(kind).append('\n')
        sb.append("name=").append(bersih(b.name)).append('\n')
        if (author.isNotBlank()) sb.append("author=").append(bersih(author)).append('\n')
        sb.append("color=").append(b.colorIdx).append('\n')
        sb.append("driver=").append(b.driverId).append('\n')
        if (b.items.isNotEmpty()) {
            sb.append("items=").append(b.items.entries.joinToString(";") { "${it.key}:${it.value}" }).append('\n')
        }
        if (b.paint.isNotEmpty()) {
            sb.append("paint=").append(b.paint.entries.joinToString(";") { "${it.key}:${it.value}" }).append('\n')
        }
        val f = b.frame
        if (f != null && f.nodes.isNotEmpty()) {
            sb.append("fnodes=").append(f.nodes.joinToString(";") {
                "${fmt(it.x)}:${fmt(it.y)}:${fmt(it.bow)}:${it.kind}"
            }).append('\n')
            sb.append("ftubes=").append(f.tubes.joinToString(";") {
                "${it.a}:${it.b}:${fmt(it.dia)}:${fmt(it.thick)}:${it.mat}"
            }).append('\n')
            sb.append("fmat=").append(f.material).append('\n')
        }
        return sb.toString()
    }

    fun decode(mentah: String): Data? {
        val teks = mentah.trim().replace("\r\n", "\n")
        if (teks.isBlank()) return null
        /* dukung juga kode lama MR1|nama|id,id */
        if (teks.startsWith("MR1|") || (!teks.startsWith(MAGIC) && looksBase64(teks))) {
            return decodeLama(teks)
        }
        val isi = if (teks.startsWith(MAGIC)) teks.substring(MAGIC.length).trimStart('\n', ' ') else teks
        val m = HashMap<String, String>()
        for (line in isi.split('\n')) {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val i = t.indexOf('=')
            if (i > 0) m[t.substring(0, i).trim()] = t.substring(i + 1)
        }
        if (m.isEmpty()) return null
        val d = Data()
        d.kind = m["kind"] ?: "pack"
        d.name = (m["name"] ?: "Desain").take(48)
        d.author = (m["author"] ?: "").take(48)
        d.color = m["color"]?.toIntOrNull()?.coerceIn(0, 11) ?: 0
        d.driver = m["driver"] ?: "budi"
        d.fmat = m["fmat"] ?: "besi"
        d.paths = m["paths"] ?: ""
        d.partcat = m["partcat"] ?: ""
        m["items"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
            val a = it.split(':')
            if (a.size == 3) d.items["${a[0]},${a[1]}"] = a[2]
            else if (a.size == 2) d.items[a[0]] = a[1]
        }
        m["paint"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
            val a = it.split(':')
            if (a.size == 3) d.paint["${a[0]},${a[1]}"] = a[2].toIntOrNull() ?: 0
            else if (a.size == 2) d.paint[a[0]] = a[1].toIntOrNull() ?: 0
        }
        m["fnodes"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
            val a = it.split(':')
            if (a.size >= 4) d.nodes.add(Node(
                a[0].toDoubleOrNull() ?: 0.0,
                a[1].toDoubleOrNull() ?: 0.0,
                a[2].toDoubleOrNull() ?: 0.0,
                a[3].ifBlank { Node.FREE }
            ))
        }
        m["ftubes"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
            val a = it.split(':')
            if (a.size >= 5) d.tubes.add(Tube(
                a[0].toIntOrNull() ?: 0,
                a[1].toIntOrNull() ?: 0,
                a[2].toDoubleOrNull() ?: 0.032,
                a[3].toDoubleOrNull() ?: 0.002,
                a[4].ifBlank { "besi" }
            ))
        }
        return d
    }

    fun terapkan(b: Build, d: Data) {
        when (d.kind) {
            "body" -> {
                b.paint.clear()
                b.paint.putAll(d.paint)
                b.colorIdx = d.color
                if (d.name.isNotBlank()) b.name = d.name
            }
            "frame" -> {
                pasangRangka(b, d)
                if (d.name.isNotBlank()) b.name = d.name
            }
            "part" -> {
                if (d.paint.isNotEmpty()) {
                    b.paint.clear()
                    b.paint.putAll(d.paint)
                }
                b.colorIdx = d.color
            }
            else -> {
                if (d.items.isNotEmpty()) {
                    b.items.clear(); b.dmg.clear()
                    b.items.putAll(d.items)
                }
                if (d.paint.isNotEmpty()) {
                    b.paint.clear()
                    b.paint.putAll(d.paint)
                }
                b.colorIdx = d.color
                if (DRIVER_BY_ID.containsKey(d.driver)) b.driverId = d.driver
                if (d.name.isNotBlank()) b.name = d.name
                if (d.nodes.isNotEmpty()) pasangRangka(b, d)
            }
        }
    }

    private fun pasangRangka(b: Build, d: Data) {
        val f = FrameDesign()
        f.nodes.addAll(d.nodes.map { it.copy() })
        f.tubes.addAll(d.tubes.map { it.copy() })
        f.material = d.fmat
        b.frame = f
    }

    private fun decodeLama(mentah: String): Data? = try {
        val raw = if (mentah.startsWith("MR1|")) mentah
                  else String(android.util.Base64.decode(mentah.trim(), android.util.Base64.DEFAULT))
        val a = raw.split('|')
        if (a.size < 3 || a[0] != "MR1") null
        else Data(kind = "pack", name = a[1], items = LinkedHashMap())
    } catch (e: Exception) { null }

    private fun looksBase64(s: String): Boolean {
        val t = s.trim()
        if (t.length < 8 || t.length % 4 != 0) return false
        return t.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
    }

    private fun bersih(s: String) = s.replace('\n', ' ').replace('=', ' ').take(48)
    private fun fmt(v: Double) = String.format(java.util.Locale.US, "%.4f", v)
}

fun cellOf(c: Int, r: Int) = cellKey(c, r)
