import id.mesinrakit.core.*
import id.mesinrakit.data.*
import id.mesinrakit.model.*
import kotlin.math.*

/* Uji logika model di JVM: spesifikasi, map, fisika, kerusakan, rangka. */
var gagal = 0
fun cek(nama: String, ok: Boolean, info: String = "") {
    if (!ok) gagal++
    println((if (ok) "OK   " else "GAGAL") + "  " + nama + if (info.isNotEmpty()) "   " + info else "")
}

fun rakitan(ps: Preset): Build {
    val b = Build()
    pasangPreset(b, ps)
    return b
}

fun main() {
    println("=== UJI MODEL MESIN RAKIT (JVM) ===")

    /* 1. katalog & data */
    cek("katalog part", PART_LIST.size >= 80, "${PART_LIST.size} part, ${CATEGORIES.size} kategori")
    cek("preset", PRESETS.size >= 16, "${PRESETS.size} preset")
    cek("map", MAPS.size >= 5, "${MAPS.size} map: " + MAPS.joinToString(", ") { it.name })
    cek("gaya knalpot", EXH_STYLES.size >= 8, EXH_STYLES.joinToString(", ") { it.name })
    cek("pengendara", DRIVERS.size >= 5, DRIVERS.joinToString(", ") { it.name })

    /* 2. tiap preset harus valid dan masuk akal */
    println("")
    for (ps in PRESETS) {
        val b = rakitan(ps)
        val s = deriveSpec(b)
        s.karakter()
        val f = s.firing
        val harap = if (s.rotary) 3 * s.rotors else s.cyl
        val ok = s.valid && s.maxTorque > 0 && s.maxPower > 1 &&
                 f.size == harap && s.mass > 40 && s.karakter.archetype.isNotEmpty()
        cek("preset ${ps.name}", ok,
            "${s.dispCc.toInt()}cc ${if (s.rotary) "rotary ${s.rotors}r" else "${s.cyl} silinder"} " +
            "| ${String.format("%.1f", s.maxPower)}hp ${String.format("%.1f", s.maxTorque)}Nm " +
            "| ${s.mass.toInt()}kg | ledakan ${f.size} tiap ${s.cycleDeg.toInt()}derajat " +
            "| ${s.karakter.archetype}")
        if (!ok && !s.valid) println("        kurang: " + s.missing.joinToString(", "))
    }

    /* 3. map: tanah mulus, tanjakan wajar */
    println("")
    for (md in MAPS) {
        val m = GameMap(md)
        var loncat = 0.0
        var tanjakan = 0.0
        var prev = m.groundY(0.0)
        var x = 0.5
        while (x < md.panjang) {
            val y = m.groundY(x)
            loncat = max(loncat, abs(y - prev))
            tanjakan = max(tanjakan, abs(m.slope(x)))
            prev = y
            x += 0.5
        }
        cek("map ${md.name}", loncat < 0.5 && tanjakan < 0.42,
            "tanjakan maks ${(atan(tanjakan) * 180 / PI).toInt()} derajat, ${m.obstacles.size} rintangan")
    }

    /* 4. fisika jalan di tiap map */
    println("")
    for (md in MAPS) {
        val b = rakitan(PRESETS.first { it.id == "bebek" })
        val s = deriveSpec(b)
        s.karakter()
        val map = GameMap(md)
        val v = Vehicle(s, b, map)
        var nan = false
        for (i in 0 until 2400) {
            v.step(1.0 / 60.0, 1.0, 0.0)
            if (v.posX.isNaN() || v.posY.isNaN() || v.rpm.isNaN()) { nan = true; break }
        }
        cek("jalan di ${md.name}", !nan && v.distance > 60,
            "${v.distance.toInt()} m, puncak ${v.maxSpeed.toInt()} km/jam, rpm ${v.rpm.toInt()}")
    }

    /* 5. gigi manual: oper gigi mengubah rasio dan rpm turun */
    println("")
    run {
        val b = rakitan(PRESETS.first { it.id == "sport250" })
        val s = deriveSpec(b)
        s.karakter()
        val v = Vehicle(s, b, GameMap(MAP_BY_ID["lurus"]!!))
        v.manual = true
        v.pindahGear(0)
        for (i in 0 until 360) v.step(1.0 / 60.0, 1.0, 0.0)
        val rpm1 = v.rpm
        val kecepatan1 = v.kmh
        v.shift(1)
        for (i in 0 until 60) v.step(1.0 / 60.0, 1.0, 0.0)
        cek("oper gigi menurunkan rpm", v.rpm < rpm1 - 200,
            "gigi ${v.gear + 1}: ${rpm1.toInt()} -> ${v.rpm.toInt()} rpm")
        cek("kecepatan tetap naik", v.kmh > kecepatan1, "${kecepatan1.toInt()} -> ${v.kmh.toInt()} km/jam")
    }

    /* 6. tabrakan & servis */
    println("")
    run {
        val b = rakitan(PRESETS.first { it.id == "bebek" })
        val s = deriveSpec(b)
        s.karakter()
        val v = Vehicle(s, b, GameMap(MAP_BY_ID["rusak"]!!))
        val torsiAwal = deriveSpec(b).maxTorque
        for (i in 0 until 3600) v.step(1.0 / 60.0, 1.0, 0.0)
        val rep = damageReport(b)
        val torsiAkhir = deriveSpec(b).maxTorque
        val ok = rep.parts > 0 && torsiAkhir < torsiAwal
        cek("tabrakan merusak part", ok,
            "${rep.parts} part rusak, torsi ${String.format("%.1f", torsiAwal)} -> ${String.format("%.1f", torsiAkhir)} Nm")
        val biaya = servisCost(b)
        servis(b)
        cek("servis membersihkan kerusakan", damageReport(b).parts == 0 && biaya > 0,
            "biaya $biaya, torsi pulih ${String.format("%.1f", deriveSpec(b).maxTorque)} Nm")
    }

    /* 7. mesin rotary punya karakter sendiri */
    println("")
    run {
        val b = rakitan(PRESETS.first { it.id == "rotary13b" })
        val s = deriveSpec(b)
        s.karakter()
        val b2 = rakitan(PRESETS.first { it.id == "rotary20b" })
        val s2 = deriveSpec(b2)
        s2.karakter()
        cek("rotary 13B: 6 ledakan per putaran", s.rotary && s.firing.size == 6,
            "${s.firing.size} ledakan, siklus ${s.cycleDeg.toInt()} derajat, ${s.dispCc.toInt()} cc")
        cek("rotary 20B: 9 ledakan per putaran", s2.firing.size == 9, "${s2.firing.size} ledakan")
        cek("karakter rotary", s.karakter.archetype == "Brap Rotary", s.karakter.archetype)
    }

    /* 8. gaya knalpot kelihatan di karakter */
    println("")
    for (idp in listOf("drumben", "heli", "thai")) {
        val b = rakitan(PRESETS.first { it.id == idp })
        val s = deriveSpec(b)
        s.karakter()
        cek("preset $idp", s.valid && s.exh.id != "standar",
            "gaya ${s.exh.name}, nada dasar ${String.format("%.0f", s.exh.f0 * 100)} persen, karakter ${s.karakter.archetype}")
    }

    /* 9. rangka custom */
    println("")
    run {
        val f = FrameDesign.preset("underbone")
        cek("rangka preset underbone", f.complete() && f.mass() > 3 && f.strength() > 0.4,
            "${String.format("%.1f", f.mass())} kg, kekuatan ${String.format("%.2f", f.strength())}, ${f.tubes.size} pipa")
        val b = rakitan(PRESETS.first { it.id == "bebek" })
        b.frame = f
        val s = deriveSpec(b)
        cek("rangka custom masuk spesifikasi", s.frameCustom != null && s.hasFrame,
            "sumbu roda ${String.format("%.2f", s.wheelbase)} m, massa total ${String.format("%.0f", s.mass)} kg")
        val kosong = FrameDesign()
        cek("rangka kosong ditolak", !kosong.complete())
    }

    /* 10. fisika tidak meledak di lintasan lurus panjang */
    println("")
    run {
        val b = rakitan(PRESETS.first { it.id == "mobil" })
        val s = deriveSpec(b)
        s.karakter()
        val v = Vehicle(s, b, GameMap(MAP_BY_ID["lurus"]!!))
        var top = 0.0
        for (i in 0 until 5400) { v.step(1.0 / 60.0, 1.0, 0.0); top = max(top, v.kmh) }
        cek("top speed mobil di lintasan lurus", v.distance > 400 && top > 60,
            "${v.distance.toInt()} m, puncak ${top.toInt()} km/jam, gigi ${v.gear + 1}")
    }

    println("")
    println(if (gagal == 0) "SEMUA UJI LULUS" else "GAGAL: $gagal uji")
    if (gagal > 0) System.exit(1)
}
