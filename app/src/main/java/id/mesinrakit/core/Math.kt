package id.mesinrakit.core

import kotlin.math.*

/* ============ helper angka ============ */
const val TAU = Math.PI * 2.0
const val GRAV = 9.81

fun clamp(v: Double, lo: Double, hi: Double): Double = when {
    v < lo -> lo
    v > hi -> hi
    v.isNaN() -> lo
    else -> v
}
fun clamp(v: Float, lo: Float, hi: Float): Float = when {
    v < lo -> lo
    v > hi -> hi
    v.isNaN() -> lo
    else -> v
}
fun clamp(v: Int, lo: Int, hi: Int): Int = when {
    v < lo -> lo
    v > hi -> hi
    else -> v
}
fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
fun lerpI(a: Int, b: Int, t: Double): Int = (a + (b - a) * t).roundToInt()
fun mapRange(v: Double, a0: Double, a1: Double, b0: Double, b1: Double): Double =
    b0 + (b1 - b0) * clamp((v - a0) / (a1 - a0), 0.0, 1.0)
fun smoothstep(t: Double): Double { val x = clamp(t, 0.0, 1.0); return x * x * (3 - 2 * x) }
fun ease(t: Double): Double = 1 - (1 - clamp(t, 0.0, 1.0)).pow(3.0)
fun deg(d: Double): Double = d * Math.PI / 180.0
fun sign0(v: Double): Double = if (v > 0) 1.0 else if (v < 0) -1.0 else 0.0
fun dbToGain(db: Double): Double = 10.0.pow(db / 20.0)
fun approx(a: Double, b: Double, eps: Double = 1e-6): Boolean = abs(a - b) < eps

/* ============ angka acak dengan seed ============ */
class Rnd(seed: Int = 12345) {
    private var s = (seed * 2654435761L) and 0x7fffffffL
    init { if (s <= 0) s = 12345L }
    fun next(): Double { s = (s * 1103515245L + 12345L) and 0x7fffffffL; return s.toDouble() / 0x7fffffffL }
    fun range(a: Double, b: Double): Double = a + (b - a) * next()
    fun int(a: Int, b: Int): Int = (a + (b - a + 1) * next()).toInt().coerceAtMost(b)
    fun pick(arr: Array<String>): String = arr[(next() * arr.size).toInt().coerceIn(0, arr.size - 1)]
    fun pick(arr: List<String>): String = arr[(next() * arr.size).toInt().coerceIn(0, arr.size - 1)]
    fun chance(p: Double): Boolean = next() < p
}
val RND = Rnd()

/* ============ noise sederhana (dipakai buat tanah dan suara) ============ */
object Noise {
    private val tbl = FloatArray(512)
    init { val r = Rnd(7); for (i in tbl.indices) tbl[i] = (r.next() * 2 - 1).toFloat() }
    fun at(x: Double): Double {
        val i = floor(x).toInt()
        val f = (x - i).toFloat()
        val a = tbl[i and 511]
        val b = tbl[(i + 1) and 511]
        return (a + (b - a) * smoothstep(f.toDouble())).toDouble()
    }
    fun fbm(x: Double, oct: Int = 3): Double {
        var v = 0.0; var amp = 1.0; var f = 1.0; var tot = 0.0
        for (i in 0 until oct) { v += at(x * f + i * 13.7) * amp; tot += amp; amp *= 0.5; f *= 2.0 }
        return v / tot
    }
}

/* ============ filter biquad (dipakai audio) ============ */
class Biquad {
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0

    fun lowpass(sr: Double, f: Double, q: Double) {
        val w = TAU * clamp(f, 10.0, sr * 0.45) / sr
        val al = sin(w) / (2 * q); val c = cos(w)
        val a0 = 1 + al
        b0 = ((1 - c) / 2) / a0; b1 = (1 - c) / a0; b2 = b0
        a1 = (-2 * c) / a0; a2 = (1 - al) / a0
    }
    fun highpass(sr: Double, f: Double, q: Double) {
        val w = TAU * clamp(f, 10.0, sr * 0.45) / sr
        val al = sin(w) / (2 * q); val c = cos(w)
        val a0 = 1 + al
        b0 = ((1 + c) / 2) / a0; b1 = (-(1 + c)) / a0; b2 = b0
        a1 = (-2 * c) / a0; a2 = (1 - al) / a0
    }
    fun bandpass(sr: Double, f: Double, q: Double) {
        val w = TAU * clamp(f, 10.0, sr * 0.45) / sr
        val al = sin(w) / (2 * q); val c = cos(w)
        val a0 = 1 + al
        b0 = al / a0; b1 = 0.0; b2 = -al / a0
        a1 = (-2 * c) / a0; a2 = (1 - al) / a0
    }
    fun peaking(sr: Double, f: Double, q: Double, gainDb: Double) {
        val A = 10.0.pow(gainDb / 40.0)
        val w = TAU * clamp(f, 10.0, sr * 0.45) / sr
        val al = sin(w) / (2 * q); val c = cos(w)
        val a0 = 1 + al / A
        b0 = (1 + al * A) / a0; b1 = (-2 * c) / a0; b2 = (1 - al * A) / a0
        a1 = (-2 * c) / a0; a2 = (1 - al / A) / a0
    }
    fun reset() { x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0 }
    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y
        return if (y.isFinite()) y else 0.0
    }
}

/* ============ kurva: titik-titik yang diinterpolasi ============ */
class Curve(private val pts: DoubleArray) {
    /** ambil nilai pada t (0..1) dengan interpolasi halus (Catmull-Rom) */
    fun at(t: Double): Double {
        if (pts.isEmpty()) return 0.0
        if (pts.size == 1) return pts[0]
        val x = clamp(t, 0.0, 1.0) * (pts.size - 1)
        val i = floor(x).toInt().coerceIn(0, pts.size - 2)
        val f = x - i
        val p0 = pts[(i - 1).coerceAtLeast(0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[(i + 2).coerceAtMost(pts.size - 1)]
        return catmull(p0, p1, p2, p3, f)
    }
    private fun catmull(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
        val t2 = t * t; val t3 = t2 * t
        return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)
    }
}

/* ============ format angka buat HUD ============ */
fun rupiah(v: Int): String {
    val s = abs(v).toString()
    val out = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) out.append('.')
        out.append(s[i])
    }
    return (if (v < 0) "-Rp " else "Rp ") + out.toString()
}
