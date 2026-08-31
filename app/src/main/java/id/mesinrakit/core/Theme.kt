package id.mesinrakit.core

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/* ============ palet warna ============ */
object C {
    val BG        = Color.parseColor("#070C14")
    val BG2       = Color.parseColor("#0B1220")
    val PANEL     = Color.parseColor("#101B2D")
    val PANEL2    = Color.parseColor("#16233A")
    val LINE      = Color.parseColor("#22344F")
    val LINE2     = Color.parseColor("#2E4568")
    val TEXT      = Color.parseColor("#DCE6F2")
    val DIM       = Color.parseColor("#7C8CA6")
    val DIM2      = Color.parseColor("#4C5B73")
    val ACC       = Color.parseColor("#22D3EE")
    val ACCD      = Color.parseColor("#0E7490")
    val AMBER     = Color.parseColor("#F59E0B")
    val AMBERD    = Color.parseColor("#B45309")
    val RED       = Color.parseColor("#EF4444")
    val GREEN     = Color.parseColor("#22C55E")
    val PURPLE    = Color.parseColor("#A855F7")
    val PINK      = Color.parseColor("#EC4899")
    val WHITE     = Color.parseColor("#FFFFFF")
    val BLACK     = Color.parseColor("#000000")

    /* warna bodi yang bisa dipakai buat ngecat */
    val PAINTS = intArrayOf(
        Color.parseColor("#E2E8F0"), Color.parseColor("#94A3B8"), Color.parseColor("#1F2937"),
        Color.parseColor("#0F172A"), Color.parseColor("#DC2626"), Color.parseColor("#EA580C"),
        Color.parseColor("#FACC15"), Color.parseColor("#16A34A"), Color.parseColor("#0891B2"),
        Color.parseColor("#2563EB"), Color.parseColor("#7C3AED"), Color.parseColor("#DB2777")
    )
    val PAINT_NAMES = arrayOf("Putih", "Abu", "Abu Gelap", "Hitam", "Merah", "Oranye",
        "Kuning", "Hijau", "Cyan", "Biru", "Ungu", "Pink")
}

/* gaya font: 0=reguler 1=medium 2=semi 3=tebal, 10..12 = angka condensed */
const val F_REG = 0
const val F_MED = 1
const val F_SEMI = 2
const val F_BOLD = 3
const val F_NUM = 10
const val F_NUM_SEMI = 11
const val F_NUM_BOLD = 12

object T {
    private lateinit var ui: Array<Typeface>
    private lateinit var num: Array<Typeface>
    private val cache = HashMap<Long, Paint>()
    private var scale = 1f

    fun init(ctx: Context, density: Float) {
        val a = ctx.assets
        fun f(n: String) = try { Typeface.createFromAsset(a, "fonts/$n.ttf") } catch (e: Exception) { Typeface.DEFAULT }
        ui = arrayOf(f("chakra_regular"), f("chakra_medium"), f("chakra_semibold"), f("chakra_bold"))
        num = arrayOf(f("barlow_regular"), f("barlow_semibold"), f("barlow_bold"))
        scale = density.coerceIn(0.75f, 3f)
        cache.clear()
    }

    /** ukuran font menyesuaikan layar: sp dikali skala terkendali */
    fun sp(v: Float): Float = v * (0.75f + scale * 0.25f)

    fun tf(style: Int): Typeface = if (style >= F_NUM) num[(style - F_NUM).coerceIn(0, 2)] else ui[style.coerceIn(0, 3)]

    /** cat teks/isi yang di-cache; jangan diubah-ubah setelah diambil */
    fun p(color: Int, size: Float, style: Int = F_REG, align: Paint.Align = Paint.Align.LEFT): Paint {
        val key = (color.toLong() and 0xffffffffL) or
                  ((size.toRawBits().toLong() and 0xffffffL) shl 32) or
                  ((style.toLong() and 0xffL) shl 56) or
                  ((align.ordinal.toLong() and 0xfL) shl 62)
        cache[key]?.let { return it }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = size
            this.typeface = tf(style)
            this.textAlign = align
        }
        cache[key] = p
        return p
    }

    private val fillCache = HashMap<Int, Paint>()
    private val strokeCache = HashMap<Long, Paint>()

    /** cat isi (tanpa teks) */
    fun fill(color: Int): Paint {
        fillCache[color]?.let { return it }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        fillCache[color] = p
        return p
    }

    /** cat garis */
    fun stroke(color: Int, w: Float): Paint {
        val key = (color.toLong() shl 32) or (w.toRawBits().toLong() and 0xffffffffL)
        strokeCache[key]?.let { return it }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = w
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokeCache[key] = p
        return p
    }
}
