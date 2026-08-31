package id.mesinrakit.model

import id.mesinrakit.core.*
import id.mesinrakit.data.*
import kotlin.math.*

/* ============================================================
   Spesifikasi: seluruh angka yang lahir dari rakitan.
   Termasuk kurva torsi, karakter mesin, pola ledakan, dan
   parameter yang dipakai mesin suara.
   ============================================================ */
class Spec {
    var valid = false
    var hasEngine = false
    var hasWheels = false
    var hasFrame = false
    var missing = listOf<String>()

    var cyl = 1
    var layout = "I1"
    var twoStroke = false
    var rotary = false
    var rotors = 1
    var dispCc = 125.0
    var bore = 0.0525
    var strokeLen = 0.0574
    var comp = 9.5
    var valves = 2

    var idleRPM = 1300.0
    var peakRPM = 6000.0
    var powerRPM = 8000.0
    var redline = 9000.0
    var limiterRPM = 9500.0
    var limiter = "soft"
    var rMax = 10500.0

    var maxTorque = 0.0
    var maxPower = 0.0
    var hpPerKg = 0.0
    var tq: Curve = Curve(doubleArrayOf(0.0))

    var mass = 120.0
    var wheelbase = 1.25
    var wheelR = 0.30
    var grip = 1.0
    var rolling = 1.0
    var drag = 0.62
    var brake = 1.0
    var nWheels = 2
    var isCar = false
    var seatH = 0.78
    var frameH = 0.55
    var strength = 1.0
    var driver: Driver = DRIVERS[0]

    var cvt = false
    var gears = doubleArrayOf(2.8, 1.9, 1.4, 1.0)
    var finalDrive = 3.0
    var eff = 0.92

    var boostMax = 0.0
    var lag = 0.0
    var spoolRPM = 2500.0
    var indDrive = ""

    var camLope = 0.1
    var camProfile = 0.3
    var flow = 1.0
    var flywheel = 1.0
    var fuelType = "bensin"
    var octane = 92.0
    var energy = 1.0
    var advance = 14.0
    var advancePeak = 30.0
    var multiSpark = 0.0

    var exh: ExhaustStyle = EXH_STYLES[0]
    var exhLen = 1.0
    var exhDia = 0.05
    var muffling = 0.6

    var cycleDeg = 720.0
    var firing = doubleArrayOf(0.0)
    var karakter = Karakter()

    var dmgTorque = 1.0
    var dmgGrip = 1.0
    var dmgBoost = 1.0
    var knock = 0.0
    var leak = 0.0
    var kondisi = 1.0
    var frameCustom: FrameDesign? = null
}

data class Karakter(
    val galak: Double = 0.5, val halus: Double = 0.5, val bass: Double = 0.5,
    val nyaring: Double = 0.5, val respons: Double = 0.5,
    val archetype: String = "Rakitan Sembarang", val desc: String = "", val tags: List<String> = emptyList()
)

class EngineState { var boost = 0.0; var dt = 0.016; var cut = false }

/* ============================================================
   Turunan utama
   ============================================================ */
fun deriveSpec(b: Build): Spec {
    val s = Spec()
    val ps = b.allParts()
    fun cat(c: String) = ps.filter { it.cat == c }
    fun first(c: String) = cat(c).firstOrNull()

    /* ---- pengendara ---- */
    s.driver = DRIVER_BY_ID[b.driverId] ?: DRIVERS[0]

    /* ---- blok & konfigurasi ---- */
    val blk = first("blok")
    val rotors = ps.count { it.cat == "rot" && it.shape == "rotor" }
    s.rotary = blk?.rotary == true
    s.rotors = max(1, rotors)
    if (blk != null) {
        s.cyl = if (s.rotary) s.rotors else blk.cyl
        s.layout = blk.layout
        s.twoStroke = blk.twoStroke
        s.dispCc = if (s.rotary) blk.ccPer * s.rotors else blk.ccPer
        s.bore = blk.bore
        s.strokeLen = blk.strokeLen
        s.hasEngine = true
    }
    val stroker = ps.firstOrNull { it.strokeLen > 0 && it.cat == "rot" }
    if (stroker != null) s.strokeLen += stroker.strokeLen

    /* ---- kepala, noken, klep ---- */
    val hd = first("head")
    val cam = first("head")?.takeIf { it.camProfile > 0 } ?: ps.firstOrNull { it.camProfile > 0 }
    s.flow = hd?.flow ?: 1.0
    s.valves = hd?.valves ?: 2
    s.comp = (ps.firstOrNull { it.cat == "rot" && it.comp > 0 }?.comp) ?: (hd?.comp ?: 9.5)
    s.camProfile = cam?.camProfile ?: 0.3
    s.camLope = cam?.camLope ?: 0.05
    if (s.rotary) { s.comp = min(s.comp, 10.4); s.valves = 0; s.camLope *= 0.5 }

    /* ---- intake & induksi paksa ---- */
    val turbo = ps.firstOrNull { it.cat == "intake" && it.boost > 0 }
    s.boostMax = turbo?.boost ?: 0.0
    s.lag = turbo?.lag ?: 0.0
    s.indDrive = turbo?.indDrive ?: ""
    s.spoolRPM = 1800.0 + s.lag * 5200.0
    val thr = ps.filter { it.cat == "intake" && it.throttle > 0 }.maxOfOrNull { it.throttle } ?: 1.0
    val plenum = ps.filter { it.cat == "intake" && it.plenum > 0 }.let { l -> if (l.isEmpty()) 1.0 else l.sumOf { it.plenum } / l.size }

    /* ---- knalpot ---- */
    val ex = first("exhaust")
    s.exhLen = ex?.exhLen ?: 1.0
    s.exhDia = ex?.exhDia ?: 0.05
    s.muffling = ex?.muffling ?: 0.6
    s.exh = exhStyle(ex?.style ?: "standar")
    val hdr = ps.firstOrNull { it.cat == "exhaust" && it.id.startsWith("hdr") }
    if (hdr != null) s.exhLen += hdr.exhLen

    /* ---- bahan bakar ---- */
    val fuel = ps.firstOrNull { it.cat == "fuel" && it.fuel.isNotEmpty() }
    s.fuelType = fuel?.fuel ?: "bensin"
    s.octane = fuel?.octane ?: 92.0
    s.energy = fuel?.energy ?: 1.0
    val diesel = s.fuelType == "solar"

    /* ---- pengapian ---- */
    val cdi = first("cdi")
    s.advance = cdi?.advance ?: 14.0
    s.advancePeak = cdi?.advancePeak ?: 30.0
    s.limiter = cdi?.limiter ?: "soft"
    s.multiSpark = cdi?.multiSpark ?: 0.0
    var lim = cdi?.limiterRPM ?: 0
    if (lim > 0 && lim < 3000) lim = 0

    /* ---- flywheel ---- */
    s.flywheel = if (ps.any { it.id == "fly2" }) 0.55 else if (ps.any { it.id == "crk3" }) 0.7 else 1.0

    /* ---- transmisi ---- */
    val gb = ps.firstOrNull { it.cat == "trans" && (it.ratios.isNotEmpty() || it.cvt) }
    s.cvt = gb?.cvt ?: false
    if (s.cvt) { s.gears = doubleArrayOf(gb?.ratioLow ?: 2.7); }
    else s.gears = gb?.ratios ?: doubleArrayOf(2.8, 1.9, 1.4, 1.0)
    s.eff = gb?.eff ?: 0.92
    s.finalDrive = ps.firstOrNull { it.cat == "trans" && it.finalDrive > 0 }?.finalDrive
        ?: if (ps.any { it.frameType == "car" }) 3.5 else 2.9

    /* ---- roda & ban ---- */
    val tyres = ps.filter { it.cat == "wheel" && it.wheelDia > 0 && it.id.startsWith("ty") }
    val rims = ps.filter { it.cat == "wheel" && it.id.startsWith("rm") }
    s.nWheels = max(1, min(tyres.size, 6))
    s.wheelR = if (tyres.isNotEmpty()) tyres.maxOf { it.wheelDia } / 2.0 else 0.30
    s.grip = if (tyres.isNotEmpty()) tyres.map { it.grip }.average() else 1.0
    s.rolling = if (tyres.isNotEmpty()) tyres.map { it.rolling }.average() else 1.0
    s.hasWheels = tyres.size >= 2 && rims.size >= 2
    s.brake = max(0.6, ps.filter { it.cat == "wheel" && it.brake > 0 }.sumOf { it.brake }.let { if (it == 0.0) 0.7 else it })

    /* ---- rangka ---- */
    val fr = ps.firstOrNull { it.cat == "frame" && it.frameType.isNotEmpty() }
    val custom = b.frame
    s.isCar = fr?.frameType == "car"
    if (custom != null && custom.complete()) {
        s.frameCustom = custom
        s.hasFrame = true
        val bd = custom.bounds()
        s.wheelbase = max(0.9, min(3.2, (bd[2] - bd[0]) * 1.05))
        s.frameH = max(0.35, min(1.4, bd[3] - bd[1]))
        s.strength = custom.strength()
    } else if (fr != null) {
        s.hasFrame = true
        s.wheelbase = fr.wheelbase
        s.seatH = fr.seatH
        s.frameH = if (s.isCar) 0.62 else max(0.45, fr.seatH * 0.72)
        s.strength = fr.strength
    }

    /* ---- aerodinamika & body ---- */
    val bodyAero = ps.filter { it.cat == "body" && it.aero != 1.0 }.map { it.aero }
    var aero = 1.0
    for (v in bodyAero) aero *= v
    if (fr != null) aero *= fr.aero
    val front = if (tyres.isNotEmpty()) tyres.maxOf { it.wheelW } * s.wheelR * 2.6 else 0.42
    s.drag = if (s.isCar) 0.72 * aero else (0.42 * aero + front * 0.55)

    /* ---- massa ---- */
    var m = ps.sumOf { it.massKg }
    m += s.driver.mass
    if (custom != null && custom.complete()) m += custom.mass()
    s.mass = max(45.0, m)

    /* ---- kerusakan ---- */
    val rep = damageReport(b)
    s.dmgTorque = rep.torque
    s.dmgGrip = rep.grip
    s.dmgBoost = rep.boost
    s.leak = rep.leak
    s.knock = clamp(s.knock + rep.knock, 0.0, 1.0)
    s.kondisi = rep.kondisi

    /* ---- kurva torsi ---- */
    val dispM3 = s.dispCc / 1e6
    val compEff = if (diesel) s.comp * 0.62 else s.comp
    val octFac = 1.0 + (s.octane - 92.0) / 260.0
    val knockRisk = clamp((compEff - s.octane / 8.2) / 6.0, 0.0, 1.0)
    s.knock = knockRisk
    val baseBmep = (7.4 + compEff * 0.42) * octFac * s.energy *
                   (1 + (s.advancePeak - 30.0) / 150.0) * (1 - knockRisk * 0.18)
    val peakX = 1.0 + s.camProfile * 0.34 + (if (s.twoStroke) 0.12 else 0.0)
    val width = 0.62 + s.camProfile * 0.30
    val camLowLoss = 0.10 + s.camProfile * 0.42
    val peakRPMRaw = (if (s.rotary) 7200.0 else 6400.0) +
        (if (s.twoStroke) 1500.0 else 0.0) +
        s.camProfile * 2600.0 - (if (diesel) 2200.0 else 0.0) +
        (if (s.cyl >= 6) 900.0 else 0.0) - s.strokeLen * 22000.0
    s.peakRPM = clamp(peakRPMRaw, 3200.0, 12000.0)
    s.powerRPM = s.peakRPM * (1.10 + s.camProfile * 0.12)
    s.idleRPM = (if (diesel) 780.0 else 1050.0) + s.camLope * 850.0 + (if (s.twoStroke) 620.0 else 0.0)
    s.redline = s.powerRPM * (if (diesel) 1.14 else 1.22) + (if (s.rotary) 700.0 else 0.0)
    s.limiterRPM = if (lim > 0) lim.toDouble() else s.redline * 1.06
    s.rMax = s.redline * 1.15

    fun veAt(x: Double): Double {
        var ve = 0.60 + 0.42 * exp(-((x - peakX) / width).pow(2.0))
        ve *= s.flow * plenum.pow(0.35)
        ve *= 1.0 - camLowLoss * exp(-((x - 0.45) / 0.30).pow(2.0))
        ve *= 1.0 + (s.exhLen - 1.0) * 0.16 * exp(-((x - 0.62) / 0.26).pow(2.0))
        ve -= s.muffling * 0.14 * smoothstep((x - 0.92) / 0.55)
        if (s.twoStroke) ve *= 1.0 + 0.42 * exp(-((x - 1.05) / 0.16).pow(2.0))
        if (s.rotary) ve *= 0.92 + 0.30 * smoothstep((x - 0.45) / 0.7)
        return clamp(ve, 0.24, 1.30)
    }

    val N = 40
    val arr = DoubleArray(N + 1)
    var best = 0.0
    for (i in 0..N) {
        val x = i.toDouble() / N
        val rpm = x * s.rMax
        val bmep = baseBmep * veAt(rpm / s.peakRPM) * if (s.rotary) 0.86 else 1.0
        var tq = bmep * 1e5 * dispM3 / (4.0 * Math.PI)
        if (s.twoStroke) tq *= 1.5
        if (s.rotary) tq *= 1.15
        tq *= s.eff.pow(0.15)
        arr[i] = max(0.0, tq * s.dmgTorque)
        best = max(best, tq * s.dmgTorque)
    }
    s.tq = Curve(arr)
    s.maxTorque = best
    var pBest = 0.0
    for (i in 0..N) {
        val rpm = i.toDouble() / N * s.rMax
        pBest = max(pBest, arr[i] * rpm * TAU / 60.0 / 745.7)
    }
    s.maxPower = pBest
    s.hpPerKg = s.maxPower / s.mass

    /* ---- pola ledakan ---- */
    s.cycleDeg = if (s.twoStroke || s.rotary) 360.0 else 720.0
    s.firing = firingFor(s)

    /* ---- syarat lengkap ---- */
    val miss = ArrayList<String>()
    if (blk == null) miss.add("Blok mesin")
    if (s.rotary) {
        if (!ps.any { it.id == "rot1" }) miss.add("Rotor rotary")
        if (!ps.any { it.id == "eccs" }) miss.add("Poros eksentrik")
    } else if (blk != null) {
        if (!ps.any { it.cat == "rot" && it.id.startsWith("pis") }) miss.add("Piston")
        if (!ps.any { it.id.startsWith("rod") }) miss.add("Setang piston")
        if (!ps.any { it.id.startsWith("crk") }) miss.add("Kruk as")
        if (!ps.any { it.cat == "head" }) miss.add("Kepala silinder")
        if (!ps.any { it.cat == "head" && it.camProfile > 0 }) miss.add("Noken as")
    }
    if (!ps.any { it.cat == "intake" && (it.throttle > 0 || it.boost > 0) }) miss.add("Intake (karbu/injeksi/turbo)")
    if (ex == null) miss.add("Knalpot")
    if (!ps.any { it.cat == "fuel" && it.fuel.isNotEmpty() }) miss.add("Bahan bakar")
    if (!ps.any { it.id.startsWith("tk") }) miss.add("Tangki")
    if (cdi == null) miss.add("CDI")
    if (gb == null) miss.add("Transmisi")
    if (!s.hasFrame) miss.add(if (b.frame != null) "Rangka belum lengkap (butuh dudukan stang, mesin, as roda belakang)" else "Rangka")
    if (tyres.size < 2) miss.add("Minimal 2 ban")
    if (rims.size < 2) miss.add("Minimal 2 velg")
    if (s.isCar && tyres.size < 4) miss.add("Mobil butuh 4 ban")
    s.missing = miss
    s.valid = miss.isEmpty()
    return s
}

/** sudut ledakan tiap silinder dalam satu siklus (derajat poros) */
fun firingFor(s: Spec): DoubleArray {
    if (s.rotary) {
        val n = 3 * max(1, s.rotors)
        return DoubleArray(n) { i -> i * 360.0 / n }
    }
    val n = max(1, s.cyl)
    if (s.twoStroke) return DoubleArray(n) { i -> i * 360.0 / n }
    return when (s.layout) {
        "I1" -> doubleArrayOf(0.0)
        "I2" -> doubleArrayOf(0.0, 270.0)
        "V2" -> doubleArrayOf(0.0, 315.0)
        "boxer" -> doubleArrayOf(0.0, 360.0)
        "I3" -> doubleArrayOf(0.0, 240.0, 480.0)
        "I4" -> doubleArrayOf(0.0, 180.0, 360.0, 540.0)
        "I6" -> DoubleArray(6) { i -> i * 120.0 }
        "V8" -> DoubleArray(8) { i -> i * 90.0 }
        else -> DoubleArray(n) { i -> i * 720.0 / n }
    }
}

fun torqueAt(rpm: Double, s: Spec, boost: Double = 0.0): Double {
    val t = clamp(rpm / s.rMax, 0.0, 1.0)
    var v = s.tq.at(t) * s.dmgTorque
    if (boost > 0) v *= 1.0 + boost * 0.44
    return max(0.0, v)
}

fun powerHp(tq: Double, rpm: Double): Double = tq * rpm * TAU / 60.0 / 745.7

fun boostAt(rpm: Double, s: Spec, gas: Double, st: EngineState): Double {
    if (s.boostMax <= 0.0) { st.boost = 0.0; return 0.0 }
    if (st.cut) { st.boost = max(0.0, st.boost - st.dt * 3.0); return st.boost }
    val spool = clamp((rpm - s.spoolRPM) / max(600.0, s.peakRPM - s.spoolRPM), 0.0, 1.0)
    val target = s.boostMax * spool.pow(1.2) * (0.16 + 0.84 * clamp(gas, 0.0, 1.0)) * s.dmgBoost
    val up = st.dt / max(0.06, s.lag * 0.55 + 0.05)
    val dn = st.dt / max(0.04, s.lag * 0.22 + 0.04)
    st.boost = if (target > st.boost) min(target, st.boost + up) else max(target, st.boost - dn)
    return st.boost
}

/* ============================================================
   Karakter mesin
   ============================================================ */
private data class Arch(val name: String, val desc: String, val tags: List<String>, val test: (Spec) -> Boolean)

private val ARCHETYPES = listOf(
    Arch("Pendengki Solar", "Klak-klak berat, getar kasar, dengung dalam yang tidak pernah halus.",
        listOf("diesel", "kasar", "bass")) { s: Spec -> s.fuelType == "solar" },
    Arch("Kotlin 2-Tak", "Meledak tiap putaran, ringan, dan melengking sampai telinga panas.",
        listOf("2-tak", "nyaring", "ringan")) { s: Spec -> s.twoStroke },
    Arch("Brap Rotary", "Brap-brap rapat tanpa klep, nada tinggi yang melengking terus naik.",
        listOf("rotary", "unik", "balap")) { s: Spec -> s.rotary },
    Arch("Gendang Jalanan", "Tiap ledakan jadi gebukan gendang, dalam dan berulang seperti drumben.",
        listOf("gendang", "dalam", "unik")) { s: Spec -> s.exh.id == "drumben" },
    Arch("Baling-Baling", "Suara terpotong beraturan, seperti baling-baling helikopter sedang menghangat.",
        listOf("helikopter", "modulasi", "unik")) { s: Spec -> s.exh.id == "helikopter" },
    Arch("Suling Thai", "Pipa kecil panjang, dengung logam tipis yang memanjang tinggi.",
        listOf("thai", "nyaring", "dengung")) { s: Spec -> s.exh.id == "thai" },
    Arch("Raja Burble", "Stasioner mengayun, gas ditutup langsung muncrat letusan kecil.",
        listOf("burble", "letus", "liar")) { s: Spec -> s.camLope > 0.7 && s.muffling < 0.5 },
    Arch("Turbin Sutra", "Banyak silinder plus turbo: halus, padat, dan naik tanpa henti.",
        listOf("halus", "turbo", "atas")) { s: Spec -> s.boostMax > 0 && s.cyl >= 6 },
    Arch("Nafas Naga", "Turbo besar: jeda dulu, lalu hembusan panjang yang tidak habis-habis.",
        listOf("turbo", "jeda", "besar")) { s: Spec -> s.boostMax >= 1.5 },
    Arch("Whine Abadi", "Kompresor mekanik melengking terus mengikuti putaran mesin.",
        listOf("supercharger", "siulan", "respons")) { s: Spec -> s.indDrive == "sc" },
    Arch("Potato Cruiser", "Gumuruh dalam dan santai, tenaga datang paling awal.",
        listOf("cruiser", "bass", "santai")) { s: Spec -> s.layout == "V2" && s.hpPerKg < 0.09 },
    Arch("Boxer Dataran", "Dua silinder berlawanan, dengung rata dan getar nyaris hilang.",
        listOf("boxer", "rata", "touring")) { s: Spec -> s.layout == "boxer" },
    Arch("Brebet Atas", "Bawah brebet mengayun, atas baru benar-benar hidup.",
        listOf("brebet", "atas", "lope")) { s: Spec -> s.camLope > 0.5 },
    Arch("Semut Gesit", "Kecil, ringan, putaran naik cepat dan suaranya renyah.",
        listOf("kecil", "gesit", "renyah")) { s: Spec -> s.dispCc <= 150 && s.hpPerKg > 0.05 },
    Arch("Tukang Sorong", "Torsi bawah besar, suara berat, enak buat nanjak dan muatan.",
        listOf("torsi", "bawah", "kuat")) { s: Spec -> s.maxTorque > 60 && s.powerRPM < 7000 },
    Arch("Sutra Jalanan", "Enam silinder segaris: halus seperti kain sutra dari bawah sampai atas.",
        listOf("halus", "I6", "mewah")) { s: Spec -> s.cyl >= 6 },
    Arch("Jagoan Atas", "Semua tenaga disimpan di putaran atas, harus digeber sampai limiter.",
        listOf("atas", "balap", "liar")) { s: Spec -> s.powerRPM > 9500 },
    Arch("Si Liar Kopong", "Pipa kosong tanpa peredam, setiap ledakan telanjang dan gampang meletus.",
        listOf("kopong", "keras", "letus")) { s: Spec -> s.exh.id == "kopong" || s.muffling < 0.15 }
    )

fun computeKarakter(s: Spec): Karakter {
    val galak = clamp(s.hpPerKg / 0.22 + s.camProfile * 0.35 + (if (s.twoStroke) 0.2 else 0.0) + s.knock * 0.2, 0.05, 1.0)
    val halus = clamp(0.15 + s.cyl * 0.13 - s.camLope * 0.5 + (if (s.rotary) 0.35 else 0.0) - s.knock * 0.25, 0.05, 1.0)
    val bass = clamp(s.dispCc / 1600.0 + (s.strokeLen - 0.05) * 6.0 + s.muffling * 0.35 - s.cyl * 0.04, 0.05, 1.0)
    val nyaring = clamp(1.0 - s.muffling * 0.55 + s.exh.f0 * 0.28 - s.dispCc / 2600.0 + (if (s.twoStroke) 0.25 else 0.0) +
        (if (s.rotary) 0.3 else 0.0), 0.05, 1.0)
    val respons = clamp(0.75 - s.lag * 0.55 + s.flywheel * 0.25 - s.mass / 900.0 + (if (s.twoStroke) 0.2 else 0.0), 0.05, 1.0)
    val ar = ARCHETYPES.firstOrNull { it.test(s) }
    val name = ar?.name ?: "Rakitan Sembarang"
    val desc = ar?.desc ?: "Belum punya kepribadian jelas. Coba ganti knalpot, noken, atau jumlah silinder."
    val tags = ar?.tags ?: listOf(netralTag(galak, halus, bass, nyaring, respons))
    return Karakter(galak, halus, bass, nyaring, respons, name, desc, tags)
}
private fun netralTag(g: Double, h: Double, b: Double, n: Double, r: Double): String {
    val m = maxOf(g, h, b, n, r)
    return when (m) { g -> "galak"; h -> "halus"; b -> "bass"; n -> "nyaring"; else -> "respons" }
}
fun Spec.karakter(): Karakter { this.karakter = computeKarakter(this); return this.karakter }

/* ============================================================
   Kerusakan
   ============================================================ */
private val DMG_FX = mapOf(
    "blok" to doubleArrayOf(0.55, 0.0, 0.0, 0.30, 0.30),  // torsi, grip, boost, knock, bocor
    "rot" to doubleArrayOf(0.45, 0.0, 0.0, 0.22, 0.18),
    "head" to doubleArrayOf(0.30, 0.0, 0.0, 0.25, 0.25),
    "intake" to doubleArrayOf(0.28, 0.0, 0.85, 0.10, 0.12),
    "exhaust" to doubleArrayOf(0.12, 0.0, 0.0, 0.05, 0.45),
    "fuel" to doubleArrayOf(0.20, 0.0, 0.0, 0.18, 0.16),
    "cdi" to doubleArrayOf(0.22, 0.0, 0.0, 0.35, 0.05),
    "trans" to doubleArrayOf(0.35, 0.10, 0.0, 0.05, 0.05),
    "wheel" to doubleArrayOf(0.05, 0.55, 0.0, 0.0, 0.0),
    "frame" to doubleArrayOf(0.08, 0.15, 0.0, 0.0, 0.02),
    "body" to doubleArrayOf(0.0, 0.05, 0.0, 0.0, 0.0)
)

data class DamageReport(
    val parts: Int, val worst: Double, val avg: Double,
    val torque: Double, val grip: Double, val boost: Double,
    val knock: Double, val leak: Double, val kondisi: Double,
    val list: List<Triple<String, String, Double>>
)

fun damageReport(b: Build): DamageReport {
    if (b.dmg.isEmpty()) return DamageReport(0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, emptyList())
    var dT = 0.0; var dG = 0.0; var dB = 0.0; var kn = 0.0; var lk = 0.0
    var worst = 0.0; var sum = 0.0; var n = 0
    val out = ArrayList<Triple<String, String, Double>>()
    for ((cell, d) in b.dmg) {
        val pid = b.items[cell] ?: continue
        val p = PART[pid] ?: continue
        val fx = DMG_FX[p.cat] ?: doubleArrayOf(0.1, 0.0, 0.0, 0.0, 0.0)
        dT += fx[0] * d; dG += fx[1] * d; dB += fx[2] * d; kn += fx[3] * d; lk += fx[4] * d
        worst = max(worst, d); sum += d; n++
        out.add(Triple(cell, p.name, d))
    }
    if (n == 0) return DamageReport(0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, emptyList())
    val avg = sum / n
    out.sortByDescending { it.third }
    return DamageReport(
        n, worst, avg,
        clamp(1 - dT, 0.25, 1.0), clamp(1 - dG, 0.25, 1.0), clamp(1 - dB, 0.0, 1.0),
        clamp(kn, 0.0, 1.0), clamp(lk, 0.0, 1.0), clamp(1 - avg, 0.0, 1.0),
        out.take(12)
    )
}

fun servisCost(b: Build): Int {
    val r = damageReport(b)
    if (r.parts == 0) return 0
    val biaya = 5000 + r.avg * 45000 + r.parts * 3500
    return ((biaya + 250) / 500).toInt() * 500
}
fun servisDarurat(b: Build): Int = (servisCost(b) * 0.45 / 500).toInt() * 500

/** servis penuh: semua kerusakan hilang */
fun servis(b: Build) { b.dmg.clear() }
/** servis darurat: kerusakan dipotong 45 persen */
fun servisSebagian(b: Build) {
    val it = b.dmg.entries.iterator()
    while (it.hasNext()) {
        val e = it.next()
        val v = e.value * 0.55
        if (v < 0.02) it.remove() else e.setValue(v)
    }
}

private val OBSTACLE_TARGETS = mapOf(
    "batu" to listOf("wheel", "frame", "exhaust", "trans"),
    "drum" to listOf("wheel", "exhaust", "frame"),
    "pagar" to listOf("frame", "wheel", "exhaust"),
    "pohon" to listOf("frame", "blok", "head", "fuel"),
    "mobil" to listOf("frame", "blok", "head", "intake", "wheel")
)

fun applyDamage(b: Build, amount: Double, obstacle: String): Pair<String, String>? {
    val pref = OBSTACLE_TARGETS[obstacle] ?: listOf("wheel", "frame", "blok")
    val cells = b.items.keys.toList()
    if (cells.isEmpty()) return null
    val want = pref[(RND.next() * pref.size).toInt().coerceIn(0, pref.size - 1)]
    val match = cells.filter { PART[b.items[it]]?.cat == want }
    val pick = if (match.isNotEmpty() && RND.chance(0.65)) match[(RND.next() * match.size).toInt().coerceIn(0, match.size - 1)]
              else cells[(RND.next() * cells.size).toInt().coerceIn(0, cells.size - 1)]
    val before = b.dmg[pick] ?: 0.0
    b.dmg[pick] = clamp(before + amount, 0.0, 1.0)
    return pick to (PART[b.items[pick]]?.name ?: "?")
}
