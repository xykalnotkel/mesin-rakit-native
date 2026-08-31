package id.mesinrakit.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import id.mesinrakit.core.*
import kotlin.math.*
import id.mesinrakit.App

/* ============================================================
   Kerangka antarmuka: semua digambar di canvas, tombol
   langsung terdaftar saat digambar (immediate mode).
   ============================================================ */
data class Hot(val x: Float, val y: Float, val w: Float, val h: Float, val id: Int, val extra: Int = 0)

const val BTN_NORMAL = 0
const val BTN_UTAMA = 1
const val BTN_BAHAYA = 2
const val BTN_HANTU = 3

abstract class Scene(val app: App) {
    val hot = ArrayList<Hot>()
    /** id tombol yang sedang ditekan (bisa lebih dari satu: multi sentuh) */
    val pressed = HashSet<Int>()
    private var nextId = 0

    fun begin() { hot.clear(); nextId = 0 }

    fun add(x: Float, y: Float, w: Float, h: Float, extra: Int = 0): Int {
        val id = nextId++
        hot.add(Hot(x, y, w, h, id, extra))
        return id
    }

    fun pick(x: Float, y: Float): Hot? {
        for (i in hot.size - 1 downTo 0) {
            val r = hot[i]
            if (x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h) return r
        }
        return null
    }

    /** gambar tombol sekaligus daftarkan area sentuhnya */
    fun tombol(c: Canvas, label: String, x: Float, y: Float, w: Float, h: Float,
               style: Int = BTN_NORMAL, size: Float = T.sp(14f), aktif: Boolean = true,
               extra: Int = 0, terpilih: Boolean = false): Int {
        val id = add(x, y, w, h, extra)
        val bg = when {
            !aktif -> C.PANEL
            style == BTN_UTAMA -> if (terpilih) C.ACC else C.ACCD
            style == BTN_BAHAYA -> if (terpilih) C.RED else 0xFF7F1D1D.toInt()
            style == BTN_HANTU -> if (terpilih) C.PANEL2 else 0x00000000
            else -> if (terpilih) C.PANEL2 else C.PANEL
        }
        val fg = when {
            !aktif -> C.DIM2
            style == BTN_UTAMA -> C.BLACK
            style == BTN_BAHAYA -> C.WHITE
            else -> if (terpilih) C.ACC else C.TEXT
        }
        if (bg != 0) c.rr(x, y, w, h, 10f, T.fill(bg))
        if (style == BTN_HANTU || terpilih) c.rrs(x, y, w, h, 10f, T.stroke(if (terpilih) C.ACC else C.LINE, 1.6f))
        else if (style != BTN_UTAMA) c.rrs(x, y, w, h, 10f, T.stroke(C.LINE, 1.2f))
        c.txc(label, x + w / 2f, y + h / 2f, size, fg, F_SEMI)
        return id
    }

    /** baris nilai: label di kiri, isi di kanan */
    fun baris(c: Canvas, label: String, nilai: String, x: Float, y: Float, w: Float, warna: Int = C.TEXT) {
        c.tx(label, x, y, T.sp(13f), C.DIM, F_REG)
        c.txr(nilai, x + w, y, T.sp(13f), warna, F_SEMI)
    }

    fun judulBesar(c: Canvas, teks: String, cx: Float, y: Float, ukuran: Float = T.sp(30f), warna: Int = C.TEXT) {
        c.txc(teks, cx, y, ukuran, warna, F_BOLD)
    }

    open fun enter() {}
    open fun leave() {}
    open fun update(dt: Float) {}
    open fun draw(c: Canvas, w: Float, h: Float) {}
    /** tombol ditekan */
    open fun press(h: Hot?, x: Float, y: Float) {}
    /** tombol dilepas */
    open fun release(h: Hot?) {}
    /** jari bergerak */
    open fun geser(x: Float, y: Float) {}
    /** jari diangkat (dipakai editor buat menaruh part) */
    open fun lepas(x: Float, y: Float) {}
    open fun tombolKembali(): Boolean = false
}

/* ============================================================
   Pesan singkat yang muncul sebentar
   ============================================================ */
class Toast {
    private var teks = ""
    private var t = 0f
    private var warna = C.ACC

    fun tampil(pesan: String, warna: Int = C.ACC, durasi: Float = 2.6f) {
        teks = pesan; t = durasi; this.warna = warna
    }
    fun update(dt: Float) { if (t > 0) t -= dt }
    fun draw(c: Canvas, w: Float, h: Float) {
        if (t <= 0) return
        val p = T.p(C.TEXT, T.sp(14f), F_SEMI)
        val tw = p.measureText(teks)
        val bw = tw + 40f
        val bh = 46f
        val x = (w - bw) / 2f
        val y = h - bh - 26f
        val alpha = min(1f, t / 0.4f)
        val f = Paint(T.fill(C.PANEL2)); f.alpha = (230 * alpha).toInt()
        c.rr(x, y, bw, bh, 23f, f)
        c.rrs(x, y, bw, bh, 23f, Paint(T.stroke(warna, 2f)).apply { this.alpha = (200 * alpha).toInt() })
        val tp = Paint(T.p(C.TEXT, T.sp(14f), F_SEMI)); tp.alpha = (255 * alpha).toInt()
        val fm = tp.fontMetrics
        c.drawText(teks, x + 20f, y + bh / 2f - (fm.ascent + fm.descent) / 2f, tp)
    }
}

/* cat logam buat gambar part */
fun metal(x0: Float, y0: Float, x1: Float, y1: Float, terang: Boolean = true): Paint {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(x0, y0, x1, y1,
        if (terang) 0xFF8A97A8.toInt() else 0xFF5A6472.toInt(),
        if (terang) 0xFF48525F.toInt() else 0xFF2A323C.toInt(), Shader.TileMode.CLAMP)
    return p
}
fun metalHor(x0: Float, x1: Float, y: Float): Paint = metal(x0, y, x1, y, true)
