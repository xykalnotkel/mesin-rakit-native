package id.mesinrakit.model

import id.mesinrakit.core.Rnd
import id.mesinrakit.core.TAU
import id.mesinrakit.core.clamp
import id.mesinrakit.data.MapDef
import kotlin.math.*

data class Obstacle(val type: String, val x: Double, var hit: Boolean = false, var t: Double = 0.0)

data class ObstacleDef(val w: Double, val h: Double, val dmg: Double, val solid: Double,
                       val color: Int, val label: String)

val OBSTACLE_TYPES = mapOf(
    "batu"  to ObstacleDef(0.55, 0.42, 0.55, 1.00, 0xFF8B8B7D.toInt(), "Batu"),
    "pohon" to ObstacleDef(0.42, 2.20, 0.85, 1.00, 0xFF3F5F3A.toInt(), "Pohon"),
    "drum"  to ObstacleDef(0.60, 0.85, 0.30, 0.35, 0xFFF59E0B.toInt(), "Drum"),
    "mobil" to ObstacleDef(2.30, 1.10, 1.00, 1.00, 0xFF4A6A8A.toInt(), "Mobil rusak"),
    "pagar" to ObstacleDef(0.30, 0.75, 0.22, 0.25, 0xFFA16207.toInt(), "Pagar")
)

class GameMap(val def: MapDef) {
    val id = def.id
    val name = def.name
    val panjang = def.panjang
    val pal = def.pal
    val straight = def.straight
    val obstacles: MutableList<Obstacle>

    init {
        val r = Rnd(def.id.fold(7) { a, c -> a + c.code })
        val list = ArrayList<Obstacle>()
        if (def.obsTypes.isNotEmpty()) {
            val spacing = 22
            var x = def.obsMinX
            while (x < def.panjang - 40) {
                if (r.next() < def.obsRate * spacing) {
                    val t = def.obsTypes[(r.next() * def.obsTypes.size).toInt().coerceIn(0, def.obsTypes.size - 1)]
                    list.add(Obstacle(t, x + r.next() * (spacing - 4)))
                }
                x += spacing
            }
        }
        obstacles = list
    }

    /** tinggi tanah di posisi x (meter, makin ke atas makin besar) */
    fun groundY(x: Double): Double {
        if (x < def.flat) return 0.0
        val t = x - def.flat
        val ramp = min(1.0, t / def.ramp)
        var y = 0.0
        for ((len, amp, ph) in def.waves) {
            val k = TAU / max(1.0, len)
            y += (sin(t * k + ph) - sin(ph)) * amp * ramp
        }
        val bumpRamp = kotlin.math.min(1.0, t / 30.0)
        y += sin(t * def.bumps[0] + 0.9) * def.bumps[1] * bumpRamp
        y += sin(t * def.bumps[2] + 2.1) * def.bumps[3] * bumpRamp
        return y
    }

    fun slope(x: Double): Double {
        val h = 0.15
        return (groundY(x + h) - groundY(x - h)) / (2 * h)
    }

    fun reset() { obstacles.forEach { it.hit = false; it.t = 0.0 } }
}
