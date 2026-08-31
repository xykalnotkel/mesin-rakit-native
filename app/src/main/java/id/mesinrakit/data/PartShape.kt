package id.mesinrakit.data

import android.graphics.Canvas
import android.graphics.Paint
import id.mesinrakit.core.*
import id.mesinrakit.ui.metal
import kotlin.math.*

/* ============================================================
   Gambar part menyerupai bentuk aslinya.
   Semua digambar di dalam kotak (x, y, w, h).
   ============================================================ */
fun gambarPart(c: Canvas, p: Part, x: Float, y: Float, w: Float, h: Float) {
    val s = min(w, h)
    fun px(fx: Double) = (x + w * fx).toFloat()
    fun py(fy: Double) = (y + h * fy).toFloat()
    val garis = T.stroke(C.LINE2, max(1f, s * 0.02f))
    val gelap = T.fill(0xFF1B2432.toInt())
    val logam = metal(px(0.0), py(0.15), px(0.0), py(0.9))
    val logam2 = metal(px(0.1), py(0.1), px(0.9), py(0.9), false)
    val karet = T.fill(0xFF161A1F.toInt())
    val kuningan = T.fill(0xFFC9A227.toInt())
    val tembaga = T.fill(0xFFB87333.toInt())
    val o = Paint(Paint.ANTI_ALIAS_FLAG)

    when (p.shape) {
        /* ---------- ROTATING ---------- */
        "piston" -> {
            c.rr(px(0.24), py(0.06), px(0.52) - px(0.24), py(0.94) - py(0.06), s * 0.10f, logam)
            c.rr(px(0.24), py(0.06), px(0.52) - px(0.24), py(0.24) - py(0.06), s * 0.07f, metal(px(0.2), py(0.05), px(0.5), py(0.25)))
            for (i in 0..2) { // ring
                val ry = py(0.22 + i * 0.09)
                c.drawLine(px(0.22), ry, px(0.54), ry, T.stroke(0xFF0B1017.toInt(), max(1.5f, s * 0.035f)))
            }
            c.dot(px(0.39), py(0.68), s * 0.10f, gelap)          // lubang pin
            c.dot(px(0.39), py(0.68), s * 0.055f, T.fill(0xFF0A0E14.toInt()))
            c.rrs(px(0.24), py(0.06), px(0.52) - px(0.24), py(0.94) - py(0.06), s * 0.10f, garis)
        }
        "ring" -> {
            o.style = Paint.Style.STROKE; o.strokeWidth = s * 0.09f; o.color = 0xFF7C8798.toInt()
            c.drawArc(px(0.14).toFloat(), py(0.16).toFloat(), px(0.62).toFloat(), py(0.84).toFloat(), 20f, 320f, false, o)
        }
        "rod" -> {
            c.rr(px(0.30), py(0.14), px(0.44) - px(0.30), py(0.86) - py(0.14), s * 0.05f, logam)
            c.dot(px(0.37), py(0.20), s * 0.135f, logam2); c.dot(px(0.37), py(0.20), s * 0.06f, gelap)
            c.dot(px(0.37), py(0.82), s * 0.10f, logam2); c.dot(px(0.37), py(0.82), s * 0.045f, gelap)
            c.drawLine(px(0.33), py(0.30), px(0.33), py(0.70), T.stroke(0xFF3A4350.toInt(), max(1f, s * 0.02f)))
        }
        "crank" -> {
            c.rr(px(0.06), py(0.44), px(0.94) - px(0.06), py(0.56) - py(0.44), s * 0.03f, logam)
            for (cx in doubleArrayOf(0.26, 0.74)) {
                c.dot(px(cx), py(0.50), s * 0.22f, logam2)
                c.drawArc(px(cx - 0.22), py(0.28), px(cx + 0.22), py(0.72), 200f, 140f, false, T.stroke(0xFF3E4756.toInt(), s * 0.05f))
                c.dot(px(cx), py(0.50), s * 0.06f, gelap)
            }
            c.dot(px(0.50), py(0.34), s * 0.075f, kuningan)   // crank pin
        }
        "eccShaft" -> {
            c.rr(px(0.08), py(0.46), px(0.92) - px(0.08), py(0.54) - py(0.46), s * 0.03f, logam)
            for (cx in doubleArrayOf(0.30, 0.62)) {
                c.dot(px(cx), py(0.50), s * 0.17f, logam2)
                c.dot(px(cx + 0.045), py(0.46), s * 0.07f, gelap)  // eksentrik
            }
        }
        "cam" -> {
            c.rr(px(0.10), py(0.46), px(0.90) - px(0.10), py(0.54) - py(0.46), s * 0.03f, logam)
            for (cx in doubleArrayOf(0.30, 0.52, 0.74)) {
                val l = px(cx); val t = py(0.50)
                val path = android.graphics.Path()
                path.moveTo(l, t - s * 0.22f)
                path.quadTo(l + s * 0.16f, t - s * 0.10f, l, t + s * 0.10f)
                path.quadTo(l - s * 0.13f, t - s * 0.02f, l, t - s * 0.22f)
                path.close()
                c.drawPath(path, logam2); c.drawPath(path, garis)
            }
            c.dot(px(0.90), py(0.50), s * 0.08f, kuningan)
        }
        "flywheel" -> {
            c.dot(px(0.46), py(0.50), s * 0.40f, logam2)
            c.dot(px(0.46), py(0.50), s * 0.33f, logam)
            for (i in 0 until 12) {
                val a = i * TAU / 12
                c.dot((px(0.46) + cos(a) * s * 0.37).toFloat(), (py(0.50) + sin(a) * s * 0.37).toFloat(), s * 0.035f, gelap)
            }
            c.dot(px(0.46), py(0.50), s * 0.09f, gelap)
        }
        "valve" -> {
            c.rr(px(0.44), py(0.10), px(0.56) - px(0.44), py(0.62) - py(0.10), s * 0.02f, logam)
            c.rr(px(0.30), py(0.62), px(0.70) - px(0.30), py(0.78) - py(0.62), s * 0.08f, metal(px(0.3), py(0.6), px(0.7), py(0.8)))
            c.rr(px(0.36), py(0.78), px(0.64) - px(0.36), py(0.90) - py(0.78), s * 0.03f, kuningan)
        }
        "spring" -> {
            val st = T.stroke(0xFF8A97A8.toInt(), max(1.5f, s * 0.055f))
            for (i in 0..5) {
                val yy = py(0.12 + i * 0.14)
                c.drawLine(px(0.24), yy, px(0.68), (yy + s * 0.07f), st)
            }
        }

        /* ---------- BLOK ---------- */
        "block1" -> {
            c.rr(px(0.22), py(0.10), px(0.74) - px(0.22), py(0.90) - py(0.10), s * 0.06f, logam2)
            for (i in 0..6) c.drawLine(px(0.18), py(0.14 + i * 0.12), px(0.78), py(0.14 + i * 0.12), T.stroke(0xFF5A6472.toInt(), max(1f, s * 0.028f)))
            c.dot(px(0.48), py(0.22), s * 0.17f, T.fill(0xFF0A0E14.toInt()))
            c.dot(px(0.48), py(0.22), s * 0.14f, logam)
        }
        "blockI2", "blockI3", "blockI4" -> {
            val n = if (p.shape == "blockI2") 2 else if (p.shape == "blockI3") 3 else 4
            c.rr(px(0.10), py(0.14), px(0.90) - px(0.10), py(0.86) - py(0.14), s * 0.05f, logam2)
            for (i in 0..5) c.drawLine(px(0.12), py(0.20 + i * 0.12), px(0.88), py(0.20 + i * 0.12), T.stroke(0xFF4C5665.toInt(), max(1f, s * 0.022f)))
            for (i in 0 until n) {
                val cx = 0.5 - (n - 1) * 0.13 + i * 0.26
                c.dot(px(cx), py(0.42), s * 0.115f, T.fill(0xFF0A0E14.toInt()))
                c.dot(px(cx), py(0.42), s * 0.09f, metal(px(cx - 0.09), py(0.33), px(cx + 0.09), py(0.51)))
            }
        }
        "blockV2" -> {
            c.rr(px(0.30), py(0.30), px(0.70) - px(0.30), py(0.88) - py(0.30), s * 0.06f, logam2)
            val st = T.stroke(0xFF6B7686.toInt(), max(2f, s * 0.05f))
            c.drawLine(px(0.50), py(0.55), px(0.18), py(0.16), st)
            c.drawLine(px(0.50), py(0.55), px(0.82), py(0.16), st)
            c.dot(px(0.20), py(0.16), s * 0.15f, logam); c.dot(px(0.80), py(0.16), s * 0.15f, logam)
            c.dot(px(0.50), py(0.62), s * 0.13f, T.fill(0xFF0A0E14.toInt()))
        }
        "boxer" -> {
            c.rr(px(0.34), py(0.36), px(0.66) - px(0.34), py(0.64) - py(0.36), s * 0.06f, logam2)
            c.rr(px(0.06), py(0.28), px(0.36) - px(0.06), py(0.72) - py(0.28), s * 0.08f, logam)
            c.rr(px(0.64), py(0.28), px(0.94) - px(0.64), py(0.72) - py(0.28), s * 0.08f, logam)
            for (cx in doubleArrayOf(0.10, 0.90)) c.dot(px(cx), py(0.50), s * 0.11f, T.fill(0xFF0A0E14.toInt()))
        }
        "blockV8" -> {
            c.rr(px(0.24), py(0.34), px(0.76) - px(0.24), py(0.88) - py(0.34), s * 0.05f, logam2)
            val st = T.stroke(0xFF6B7686.toInt(), max(1.5f, s * 0.035f))
            for (i in 0..3) {
                val cx = 0.32 + i * 0.12
                c.drawLine(px(0.50 + (cx - 0.5) * 0.2), py(0.60), px(cx - 0.05), py(0.30 - i * 0.03), st)
                c.drawLine(px(0.50 - (cx - 0.5) * 0.2), py(0.60), px(1.0 - cx + 0.05), py(0.30 - i * 0.03), st)
            }
            c.dot(px(0.50), py(0.66), s * 0.12f, T.fill(0xFF0A0E14.toInt()))
        }
        "rotaryHousing" -> {
            c.rr(px(0.08), py(0.16), px(0.92) - px(0.08), py(0.84) - py(0.16), s * 0.22f, logam2)
            c.rr(px(0.16), py(0.26), px(0.84) - px(0.16), py(0.74) - px(0.26), s * 0.14f, T.fill(0xFF0A0E14.toInt()))
            c.dot(px(0.34), py(0.50), s * 0.11f, T.fill(0xFF2A3444.toInt()))
            c.dot(px(0.66), py(0.50), s * 0.11f, T.fill(0xFF2A3444.toInt()))
            c.dot(px(0.50), py(0.30), s * 0.045f, kuningan)   // busi
            c.rr(px(0.12), py(0.66), px(0.30) - px(0.12), py(0.80) - py(0.66), s * 0.04f, T.fill(0xFF3A4557.toInt()))
            c.rrs(px(0.08), py(0.16), px(0.92) - px(0.08), py(0.84) - py(0.16), s * 0.22f, garis)
        }
        "rotor" -> {
            val path = android.graphics.Path()
            for (i in 0 until 3) {
                val a = -Math.PI / 2 + i * TAU / 3
                val vx = px(0.5) + cos(a) * s * 0.34
                val vy = py(0.5) + sin(a) * s * 0.34
                val a2 = a + TAU / 3
                val nx = px(0.5) + cos(a2) * s * 0.34
                val ny = py(0.5) + sin(a2) * s * 0.34
                if (i == 0) path.moveTo(vx.toFloat(), vy.toFloat())
                path.quadTo(((vx + nx) / 2 * 1.16 - px(0.5) * 0.16).toFloat(), ((vy + ny) / 2 * 1.16 - py(0.5) * 0.16).toFloat(), nx.toFloat(), ny.toFloat())
            }
            path.close()
            c.drawPath(path, logam); c.drawPath(path, garis)
            c.dot(px(0.5), py(0.5), s * 0.14f, T.fill(0xFF0A0E14.toInt()))
            for (i in 0 until 10) {
                val a = i * TAU / 10
                c.dot((px(0.5) + cos(a) * s * 0.115).toFloat(), (py(0.5) + sin(a) * s * 0.115).toFloat(), s * 0.022f, kuningan)
            }
        }

        /* ---------- KEPALA ---------- */
        "head" -> {
            c.rr(px(0.14), py(0.16), px(0.86) - px(0.14), py(0.84) - py(0.16), s * 0.06f, logam2)
            for (i in 0..4) c.drawLine(px(0.16), py(0.22 + i * 0.14), px(0.84), py(0.22 + i * 0.14), T.stroke(0xFF4C5665.toInt(), max(1f, s * 0.025f)))
            c.dot(px(0.34), py(0.50), s * 0.10f, T.fill(0xFF0A0E14.toInt()))
            c.dot(px(0.66), py(0.50), s * 0.10f, T.fill(0xFF0A0E14.toInt()))
        }
        "head2" -> {
            c.rr(px(0.10), py(0.14), px(0.90) - px(0.10), py(0.86) - py(0.14), s * 0.06f, logam2)
            for (i in 0 until 4) {
                val cx = 0.24 + i * 0.17
                c.dot(px(cx), py(0.40), s * 0.075f, T.fill(0xFF0A0E14.toInt()))
                c.dot(px(cx), py(0.62), s * 0.075f, T.fill(0xFF0A0E14.toInt()))
            }
            c.rr(px(0.12), py(0.72), px(0.88) - px(0.12), py(0.82) - py(0.72), s * 0.03f, metal(px(0.1), py(0.7), px(0.9), py(0.84)))
        }

        /* ---------- INTAKE ---------- */
        "carb" -> {
            c.rr(px(0.28), py(0.12), px(0.72) - px(0.28), py(0.58) - py(0.12), s * 0.07f, kuningan)
            c.rr(px(0.22), py(0.52), px(0.78) - px(0.22), py(0.86) - py(0.52), s * 0.10f, metal(px(0.2), py(0.5), px(0.8), py(0.9)))
            c.dot(px(0.50), py(0.24), s * 0.13f, T.fill(0xFF0A0E14.toInt()))
            c.rr(px(0.40), py(0.58), px(0.60) - px(0.40), py(0.72) - py(0.58), s * 0.02f, T.fill(0xFF2A3444.toInt()))
            c.rrs(px(0.28), py(0.12), px(0.72) - px(0.28), py(0.58) - py(0.12), s * 0.07f, garis)
        }
        "injector" -> {
            c.rr(px(0.34), py(0.16), px(0.66) - px(0.34), py(0.60) - py(0.16), s * 0.05f, T.fill(0xFF2F3B4D.toInt()))
            c.rr(px(0.42), py(0.56), px(0.58) - px(0.42), py(0.84) - py(0.56), s * 0.02f, logam)
            c.dot(px(0.50), py(0.26), s * 0.09f, T.fill(C.ACC))
            c.drawLine(px(0.50), py(0.10), px(0.50), py(0.18), T.stroke(C.RED, max(1.5f, s * 0.03f)))
        }
        "filter" -> {
            c.rr(px(0.16), py(0.24), px(0.84) - px(0.16), py(0.76) - py(0.24), s * 0.08f, T.fill(0xFF3A2E1C.toInt()))
            for (i in 0..6) c.drawLine(px(0.18 + i * 0.10), py(0.26), px(0.18 + i * 0.10), py(0.74), T.stroke(0xFF6B5636.toInt(), max(1f, s * 0.03f)))
            c.rrs(px(0.16), py(0.24), px(0.84) - px(0.16), py(0.76) - py(0.24), s * 0.08f, garis)
        }
        "turbo" -> {
            c.dot(px(0.34), py(0.50), s * 0.28f, metal(px(0.1), py(0.25), px(0.6), py(0.75)))
            c.dot(px(0.34), py(0.50), s * 0.12f, T.fill(0xFF0A0E14.toInt()))
            c.rr(px(0.58), py(0.26), px(0.90) - px(0.58), py(0.74) - py(0.26), s * 0.10f, logam)
            c.dot(px(0.74), py(0.50), s * 0.11f, T.fill(0xFF0A0E14.toInt()))
            c.drawLine(px(0.34), py(0.30), px(0.58), py(0.42), T.stroke(0xFF8A97A8.toInt(), max(2f, s * 0.06f)))
        }
        "sc" -> {
            c.rr(px(0.14), py(0.28), px(0.86) - px(0.14), py(0.72) - py(0.28), s * 0.08f, logam2)
            for (i in 0..3) c.drawLine(px(0.20 + i * 0.18), py(0.30), px(0.20 + i * 0.18), py(0.70), T.stroke(0xFF59636F.toInt(), max(2f, s * 0.05f)))
            c.dot(px(0.50), py(0.22), s * 0.13f, kuningan)
        }
        "intercooler" -> {
            c.rr(px(0.08), py(0.24), px(0.92) - px(0.08), py(0.76) - py(0.24), s * 0.05f, logam2)
            for (i in 0..7) c.drawLine(px(0.12 + i * 0.10), py(0.28), px(0.12 + i * 0.10), py(0.72), T.stroke(0xFF9AA7B8.toInt(), max(1f, s * 0.022f)))
            c.rr(px(0.04), py(0.30), px(0.12) - px(0.04), py(0.70) - py(0.30), s * 0.03f, T.fill(0xFF2A3444.toInt()))
        }
        "manifold" -> {
            c.rr(px(0.10), py(0.20), px(0.34) - px(0.10), py(0.80) - py(0.20), s * 0.08f, logam2)
            for (i in 0..3) {
                c.drawLine(px(0.34), py(0.28 + i * 0.16), px(0.74), py(0.28 + i * 0.16), T.stroke(0xFF7C8798.toInt(), max(2f, s * 0.055f)))
            }
            c.rr(px(0.72), py(0.20), px(0.90) - px(0.72), py(0.80) - py(0.20), s * 0.03f, logam)
        }

        /* ---------- KNALPOT ---------- */
        "header" -> {
            val st = T.stroke(0xFF8A97A8.toInt(), max(2f, s * 0.055f))
            for (i in 0..3) c.drawLine(px(0.10), py(0.24 + i * 0.18), px(0.60), py(0.50), st)
            c.drawLine(px(0.60), py(0.50), px(0.92), py(0.50), T.stroke(0xFF9AA7B8.toInt(), max(3f, s * 0.075f)))
        }
        "pipe" -> {
            c.drawLine(px(0.08), py(0.62), px(0.62), py(0.38), T.stroke(0xFF8A97A8.toInt(), max(4f, s * 0.12f)))
            c.rr(px(0.58), py(0.30), px(0.94) - px(0.58), py(0.46) - py(0.30), s * 0.03f, metal(px(0.5), py(0.28), px(0.95), py(0.48)))
        }
        "muffler" -> {
            c.rr(px(0.08), py(0.30), px(0.78) - px(0.08), py(0.70) - py(0.30), s * 0.18f, metal(px(0.05), py(0.26), px(0.8), py(0.74)))
            c.rr(px(0.70), py(0.40), px(0.94) - px(0.70), py(0.60) - py(0.40), s * 0.05f, metal(px(0.7), py(0.38), px(0.95), py(0.62)))
            for (i in 0..2) c.drawLine(px(0.20 + i * 0.16), py(0.32), px(0.20 + i * 0.16), py(0.68), T.stroke(0xFF39424E.toInt(), max(1f, s * 0.025f)))
        }
        "mufflerRacing" -> {
            c.rr(px(0.14), py(0.32), px(0.74) - px(0.14), py(0.68) - py(0.32), s * 0.16f, T.fill(0xFF1B2028.toInt()))
            c.rr(px(0.66), py(0.38), px(0.92) - px(0.66), py(0.62) - py(0.38), s * 0.05f, T.fill(0xFF0F131A.toInt()))
            c.rrs(px(0.14), py(0.32), px(0.74) - px(0.14), py(0.68) - py(0.32), s * 0.16f, T.stroke(C.AMBER, 2f))
            c.dot(px(0.86), py(0.50), s * 0.07f, T.fill(0xFF05070A.toInt()))
        }
        "mufflerDrum" -> {
            c.rr(px(0.12), py(0.22), px(0.76) - px(0.12), py(0.78) - py(0.22), s * 0.10f, T.fill(0xFF7A4A21.toInt()))
            c.dot(px(0.44), py(0.50), s * 0.24f, T.fill(0xFFE7D2A8.toInt()))
            c.dot(px(0.44), py(0.50), s * 0.24f, T.stroke(0xFF5A3416.toInt(), max(2f, s * 0.04f)))
            for (i in 0..3) c.drawLine(px(0.44 - 0.20 + i * 0.13), py(0.30), px(0.44 - 0.20 + i * 0.13), py(0.70), T.stroke(0xFF6B4020.toInt(), max(1f, s * 0.02f)))
            c.rr(px(0.70), py(0.42), px(0.94) - px(0.70), py(0.58) - py(0.42), s * 0.04f, metal(px(0.7), py(0.4), px(0.95), py(0.6)))
        }
        "mufflerHeli" -> {
            c.rr(px(0.10), py(0.34), px(0.64) - px(0.10), py(0.66) - py(0.34), s * 0.14f, metal(px(0.1), py(0.3), px(0.65), py(0.7)))
            val cx = px(0.80); val cy = py(0.50)
            c.drawLine(cx - s * 0.16f, cy, cx + s * 0.16f, cy, T.stroke(0xFFC9D3E0.toInt(), max(3f, s * 0.07f)))
            c.drawLine(cx, cy - s * 0.16f, cx, cy + s * 0.16f, T.stroke(0xFFC9D3E0.toInt(), max(3f, s * 0.07f)))
            c.dot(cx, cy, s * 0.05f, gelap)
        }
        "mufflerThai" -> {
            c.drawLine(px(0.06), py(0.56), px(0.70), py(0.44), T.stroke(0xFFB9C4D4.toInt(), max(2f, s * 0.055f)))
            c.rr(px(0.66), py(0.34), px(0.94) - px(0.66), py(0.56) - py(0.34), s * 0.03f, metal(px(0.65), py(0.32), px(0.95), py(0.58)))
            c.dot(px(0.92), py(0.45), s * 0.04f, T.fill(0xFF0A0E14.toInt()))
        }
        "chamber" -> {
            val st = T.stroke(0xFF9AA7B8.toInt(), max(2f, s * 0.06f))
            c.drawLine(px(0.06), py(0.60), px(0.30), py(0.52), st)
            c.drawLine(px(0.30), py(0.52), px(0.56), py(0.38), T.stroke(0xFFB9C4D4.toInt(), max(4f, s * 0.13f)))
            c.drawLine(px(0.56), py(0.38), px(0.80), py(0.52), T.stroke(0xFF8A97A8.toInt(), max(4f, s * 0.10f)))
            c.drawLine(px(0.80), py(0.52), px(0.94), py(0.56), T.stroke(0xFF6B7686.toInt(), max(2f, s * 0.05f)))
        }

        /* ---------- BAHAN BAKAR & LISTRIK ---------- */
        "tank" -> {
            c.rr(px(0.12), py(0.20), px(0.88) - px(0.12), py(0.80) - py(0.20), s * 0.18f, metal(px(0.1), py(0.16), px(0.9), py(0.84)))
            c.rr(px(0.30), py(0.16), px(0.62) - px(0.30), py(0.28) - py(0.16), s * 0.04f, T.fill(0xFF2A3444.toInt()))
            c.rrs(px(0.12), py(0.20), px(0.88) - px(0.12), py(0.80) - px(0.20), s * 0.18f, garis)
        }
        "pump" -> {
            c.dot(px(0.46), py(0.50), s * 0.26f, T.fill(0xFF2F3B4D.toInt()))
            c.rr(px(0.66), py(0.34), px(0.94) - px(0.66), py(0.66) - py(0.34), s * 0.06f, kuningan)
            c.dot(px(0.46), py(0.50), s * 0.08f, T.fill(C.ACC))
        }
        "cdi" -> {
            c.rr(px(0.10), py(0.26), px(0.90) - px(0.10), py(0.74) - py(0.26), s * 0.06f, T.fill(0xFF141A24.toInt()))
            c.rr(px(0.16), py(0.32), px(0.84) - px(0.16), py(0.68) - py(0.32), s * 0.04f, T.fill(0xFF1E2735.toInt()))
            for (i in 0..3) c.dot(px(0.28 + i * 0.16), py(0.78), s * 0.035f, kuningan)
            c.dot(px(0.80), py(0.40), s * 0.045f, T.fill(C.ACC))
            c.rrs(px(0.10), py(0.26), px(0.90) - px(0.10), py(0.74) - py(0.26), s * 0.06f, T.stroke(C.LINE2, 1.4f))
        }
        "coil" -> {
            c.rr(px(0.30), py(0.14), px(0.70) - px(0.30), py(0.74) - py(0.14), s * 0.05f, T.fill(0xFF1A2230.toInt()))
            for (i in 0..5) c.drawLine(px(0.30), py(0.20 + i * 0.09), px(0.70), py(0.20 + i * 0.09), T.stroke(0xFF3A4557.toInt(), max(1f, s * 0.03f)))
            c.rr(px(0.42), py(0.70), px(0.58) - px(0.42), py(0.92) - py(0.70), s * 0.03f, logam)
        }
        "plug" -> {
            c.rr(px(0.36), py(0.10), px(0.64) - px(0.36), py(0.34) - py(0.10), s * 0.03f, T.fill(0xFFD8DEE8.toInt()))
            c.rr(px(0.32), py(0.32), px(0.68) - px(0.32), py(0.50) - py(0.32), s * 0.03f, metal(px(0.3), py(0.3), px(0.7), py(0.52)))
            c.rr(px(0.38), py(0.48), px(0.62) - px(0.38), py(0.84) - py(0.48), s * 0.02f, T.fill(0xFF5A6472.toInt()))
            c.dot(px(0.50), py(0.86), s * 0.045f, gelap)
        }
        "battery" -> {
            c.rr(px(0.14), py(0.28), px(0.86) - px(0.14), py(0.80) - py(0.28), s * 0.05f, T.fill(0xFF101720.toInt()))
            c.dot(px(0.30), py(0.24), s * 0.06f, T.fill(C.RED))
            c.dot(px(0.70), py(0.24), s * 0.06f, T.fill(0xFF334155.toInt()))
            c.txc("12V", px(0.5), py(0.54), s * 0.18f, C.AMBER, F_BOLD)
        }
        "radiator" -> {
            c.rr(px(0.10), py(0.16), px(0.90) - px(0.10), py(0.84) - py(0.16), s * 0.04f, T.fill(0xFF1A2230.toInt()))
            for (i in 0..9) c.drawLine(px(0.14 + i * 0.08), py(0.20), px(0.14 + i * 0.08), py(0.80), T.stroke(0xFF6B7686.toInt(), max(1f, s * 0.028f)))
            c.dot(px(0.84), py(0.20), s * 0.07f, kuningan)
        }
        "nos" -> {
            c.rr(px(0.28), py(0.10), px(0.72) - px(0.28), py(0.84) - py(0.10), s * 0.16f, T.fill(0xFF1B3A5C.toInt()))
            c.rr(px(0.40), py(0.04), px(0.60) - px(0.40), py(0.14) - py(0.04), s * 0.03f, kuningan)
            c.txc("NOS", px(0.5), py(0.48), s * 0.16f, C.ACC, F_BOLD)
        }

        /* ---------- TRANSMISI ---------- */
        "gearbox" -> {
            c.rr(px(0.10), py(0.18), px(0.90) - px(0.10), py(0.82) - py(0.18), s * 0.08f, logam2)
            for (i in 0..2) {
                c.dot(px(0.30 + i * 0.20), py(0.42), s * 0.15f, kuningan)
                c.dot(px(0.30 + i * 0.20), py(0.42), s * 0.05f, gelap)
            }
            c.rr(px(0.20), py(0.62), px(0.80) - px(0.20), py(0.74) - py(0.62), s * 0.02f, T.fill(0xFF2A3444.toInt()))
            c.rrs(px(0.10), py(0.18), px(0.90) - px(0.10), py(0.82) - py(0.18), s * 0.08f, garis)
        }
        "cvt" -> {
            c.dot(px(0.30), py(0.50), s * 0.26f, metal(px(0.1), py(0.3), px(0.5), py(0.7)))
            c.dot(px(0.72), py(0.50), s * 0.20f, metal(px(0.6), py(0.35), px(0.9), py(0.65)))
            c.drawLine(px(0.30), py(0.24), px(0.72), py(0.30), T.stroke(0xFF2A3444.toInt(), max(3f, s * 0.08f)))
            c.drawLine(px(0.30), py(0.76), px(0.72), py(0.70), T.stroke(0xFF2A3444.toInt(), max(3f, s * 0.08f)))
        }
        "clutch" -> {
            c.dot(px(0.48), py(0.50), s * 0.36f, logam2)
            c.dot(px(0.48), py(0.50), s * 0.28f, T.fill(0xFF232C3A.toInt()))
            for (i in 0..7) {
                val a = i * TAU / 8
                c.dot((px(0.48) + cos(a) * s * 0.20).toFloat(), (py(0.50) + sin(a) * s * 0.20).toFloat(), s * 0.035f, kuningan)
            }
        }
        "chain" -> {
            c.dot(px(0.28), py(0.50), s * 0.20f, logam2); c.dot(px(0.28), py(0.50), s * 0.07f, gelap)
            c.dot(px(0.74), py(0.50), s * 0.16f, logam2); c.dot(px(0.74), py(0.50), s * 0.06f, gelap)
            val st = T.stroke(0xFF8A97A8.toInt(), max(2f, s * 0.05f))
            c.drawLine(px(0.28), py(0.30), px(0.74), py(0.34), st)
            c.drawLine(px(0.28), py(0.70), px(0.74), py(0.66), st)
        }
        "finaldrive" -> {
            c.dot(px(0.48), py(0.50), s * 0.38f, logam2)
            for (i in 0 until 14) {
                val a = i * TAU / 14
                c.drawLine((px(0.48) + cos(a) * s * 0.30).toFloat(), (py(0.50) + sin(a) * s * 0.30).toFloat(),
                    (px(0.48) + cos(a) * s * 0.38).toFloat(), (py(0.50) + sin(a) * s * 0.38).toFloat(), T.stroke(0xFF59636F.toInt(), max(1.5f, s * 0.035f)))
            }
            c.dot(px(0.48), py(0.50), s * 0.10f, gelap)
        }

        /* ---------- RODA ---------- */
        "tyre" -> {
            c.dot(px(0.5), py(0.5), s * 0.46f, karet)
            c.dot(px(0.5), py(0.5), s * 0.46f, T.stroke(0xFF2A3038.toInt(), max(2f, s * 0.05f)))
            for (i in 0 until 12) {
                val a = i * TAU / 12
                c.drawLine((px(0.5) + cos(a) * s * 0.34).toFloat(), (py(0.5) + sin(a) * s * 0.34).toFloat(),
                    (px(0.5) + cos(a) * s * 0.45).toFloat(), (py(0.5) + sin(a) * s * 0.45).toFloat(), T.stroke(0xFF0A0D12.toInt(), max(2f, s * 0.05f)))
            }
            c.dot(px(0.5), py(0.5), s * 0.26f, T.fill(0xFF0A0D12.toInt()))
        }
        "rim" -> {
            c.dot(px(0.5), py(0.5), s * 0.44f, T.fill(0xFF1A2230.toInt()))
            c.dot(px(0.5), py(0.5), s * 0.44f, T.stroke(0xFF9AA7B8.toInt(), max(2f, s * 0.05f)))
            if (p.rimType == "spoke") {
                for (i in 0 until 10) {
                    val a = i * TAU / 10
                    c.drawLine(px(0.5).toFloat(), py(0.5).toFloat(),
                        (px(0.5) + cos(a) * s * 0.40).toFloat(), (py(0.5) + sin(a) * s * 0.40).toFloat(), T.stroke(0xFFC3CCD8.toInt(), max(1f, s * 0.022f)))
                }
            } else {
                for (i in 0 until 5) {
                    val a = i * TAU / 5
                    c.rr((px(0.5) + cos(a) * s * 0.14 - s * 0.05).toFloat(), (py(0.5) + sin(a) * s * 0.14 - s * 0.05).toFloat(),
                        s * 0.10f, s * 0.10f, s * 0.03f, T.fill(0xFF8A97A8.toInt()))
                }
            }
            c.dot(px(0.5), py(0.5), s * 0.11f, logam)
            c.dot(px(0.5), py(0.5), s * 0.04f, gelap)
        }
        "disc" -> {
            c.dot(px(0.5), py(0.5), s * 0.42f, metal(px(0.1), py(0.1), px(0.9), py(0.9)))
            for (i in 0 until 8) {
                val a = i * TAU / 8
                c.dot((px(0.5) + cos(a) * s * 0.28).toFloat(), (py(0.5) + sin(a) * s * 0.28).toFloat(), s * 0.045f, T.fill(0xFF0A0D12.toInt()))
            }
            c.dot(px(0.5), py(0.5), s * 0.14f, logam2)
            c.rr(px(0.66), py(0.34), px(0.88) - px(0.66), py(0.66) - py(0.34), s * 0.04f, T.fill(C.RED))
        }

        /* ---------- RANGKA ---------- */
        "frame", "frameTrail" -> {
            val tinggi = if (p.shape == "frameTrail") 0.28 else 0.36
            val st = T.stroke(0xFF7C8798.toInt(), max(3f, s * 0.075f))
            c.drawLine(px(0.14), py(tinggi), px(0.86), py(tinggi), st)
            c.drawLine(px(0.14), py(tinggi), px(0.30), py(0.78), st)
            c.drawLine(px(0.30), py(0.78), px(0.70), py(0.78), st)
            c.drawLine(px(0.70), py(0.78), px(0.86), py(tinggi), st)
            c.drawLine(px(0.30), py(tinggi), px(0.46), py(0.78), T.stroke(0xFF59636F.toInt(), max(2f, s * 0.05f)))
            c.dot(px(0.14), py(tinggi), s * 0.07f, logam)
            c.dot(px(0.86), py(tinggi), s * 0.07f, logam)
        }
        "swingarm" -> {
            c.drawLine(px(0.16), py(0.50), px(0.86), py(0.50), T.stroke(0xFF7C8798.toInt(), max(4f, s * 0.10f)))
            c.drawLine(px(0.16), py(0.36), px(0.60), py(0.50), T.stroke(0xFF7C8798.toInt(), max(4f, s * 0.09f)))
            c.drawLine(px(0.16), py(0.64), px(0.60), py(0.50), T.stroke(0xFF7C8798.toInt(), max(4f, s * 0.09f)))
            c.dot(px(0.16), py(0.50), s * 0.09f, logam)
            c.dot(px(0.86), py(0.50), s * 0.08f, gelap)
        }
        "fork" -> {
            val st = T.stroke(0xFFC3CCD8.toInt(), max(4f, s * 0.10f))
            c.drawLine(px(0.34), py(0.10), px(0.34), py(0.60), st)
            c.drawLine(px(0.62), py(0.10), px(0.62), py(0.60), st)
            c.drawLine(px(0.34), py(0.62), px(0.34), py(0.90), T.stroke(0xFF8A97A8.toInt(), max(3f, s * 0.07f)))
            c.drawLine(px(0.62), py(0.62), px(0.62), py(0.90), T.stroke(0xFF8A97A8.toInt(), max(3f, s * 0.07f)))
            c.rr(px(0.24), py(0.16), px(0.74) - px(0.24), py(0.28) - py(0.16), s * 0.03f, logam)
            c.rr(px(0.24), py(0.38), px(0.74) - px(0.24), py(0.48) - py(0.38), s * 0.03f, logam)
        }
        "shock" -> {
            c.rr(px(0.38), py(0.10), px(0.62) - px(0.38), py(0.46) - py(0.10), s * 0.04f, logam)
            c.rr(px(0.42), py(0.44), px(0.58) - px(0.42), py(0.90) - py(0.44), s * 0.02f, T.fill(0xFFC3CCD8.toInt()))
            for (i in 0..4) c.drawLine(px(0.34), py(0.20 + i * 0.14), px(0.66), py(0.24 + i * 0.14), T.stroke(C.ACC, max(2f, s * 0.045f)))
            c.dot(px(0.50), py(0.08), s * 0.07f, gelap)
        }

        /* ---------- BODY ---------- */
        "seat" -> {
            c.rr(px(0.08), py(0.34), px(0.92) - px(0.08), py(0.74) - py(0.34), s * 0.14f, T.fill(0xFF151A22.toInt()))
            c.rr(px(0.12), py(0.30), px(0.88) - px(0.12), py(0.40) - py(0.30), s * 0.06f, T.fill(0xFF232C3A.toInt()))
            c.drawLine(px(0.30), py(0.42), px(0.30), py(0.68), T.stroke(0xFF0A0D12.toInt(), max(1f, s * 0.02f)))
            c.drawLine(px(0.62), py(0.42), px(0.62), py(0.68), T.stroke(0xFF0A0D12.toInt(), max(1f, s * 0.02f)))
        }
        "bar" -> {
            val st = T.stroke(0xFF8A97A8.toInt(), max(3f, s * 0.08f))
            c.drawLine(px(0.10), py(0.60), px(0.34), py(0.34), st)
            c.drawLine(px(0.34), py(0.34), px(0.66), py(0.34), st)
            c.drawLine(px(0.66), py(0.34), px(0.90), py(0.60), st)
            c.dot(px(0.50), py(0.34), s * 0.08f, logam)
            c.rr(px(0.06), py(0.58), px(0.22) - px(0.06), py(0.68) - py(0.58), s * 0.03f, T.fill(0xFF1A2230.toInt()))
            c.rr(px(0.78), py(0.58), px(0.94) - px(0.78), py(0.68) - py(0.58), s * 0.03f, T.fill(0xFF1A2230.toInt()))
        }
        "fender" -> {
            val st = T.stroke(0xFF5A6472.toInt(), max(4f, s * 0.09f))
            c.drawArc(px(0.10), py(0.16), px(0.90), py(0.84), 200f, 140f, false, st)
        }
        "light" -> {
            c.dot(px(0.5), py(0.5), s * 0.36f, T.fill(0xFF1A2230.toInt()))
            c.dot(px(0.5), py(0.5), s * 0.28f, T.fill(0xFFF5E6A8.toInt()))
            c.dot(px(0.42), py(0.42), s * 0.07f, T.fill(0xFFFFFFFF.toInt()))
            c.dot(px(0.5), py(0.5), s * 0.36f, T.stroke(0xFF8A97A8.toInt(), max(2f, s * 0.04f)))
        }
        "fairing" -> {
            c.rr(px(0.10), py(0.18), px(0.90) - px(0.10), py(0.82) - py(0.18), s * 0.24f, T.fill(0xFF2A3444.toInt()))
            c.rr(px(0.18), py(0.26), px(0.82) - px(0.18), py(0.46) - py(0.26), s * 0.12f, T.fill(0xFF3A4759.toInt()))
            c.rrs(px(0.10), py(0.18), px(0.90) - px(0.10), py(0.82) - py(0.18), s * 0.24f, garis)
        }
        "footpeg" -> {
            c.drawLine(px(0.16), py(0.34), px(0.66), py(0.62), T.stroke(0xFF8A97A8.toInt(), max(3f, s * 0.08f)))
            c.rr(px(0.60), py(0.56), px(0.94) - px(0.60), py(0.74) - py(0.56), s * 0.03f, T.fill(0xFF2A3444.toInt()))
        }
        else -> {
            c.rr(px(0.16), py(0.22), px(0.84) - px(0.16), py(0.78) - py(0.22), s * 0.08f, logam2)
            c.rrs(px(0.16), py(0.22), px(0.84) - px(0.16), py(0.78) - py(0.22), s * 0.08f, garis)
        }
    }
}
