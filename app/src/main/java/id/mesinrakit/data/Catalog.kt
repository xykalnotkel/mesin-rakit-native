package id.mesinrakit.data

import id.mesinrakit.core.*
import id.mesinrakit.model.Build
import id.mesinrakit.model.GRID_W

/* ============================================================
   Gaya knalpot. Tiap gaya mengubah gelombang suara: nada dasar,
   panjang dengung, overtone, modulasi, dan kecenderungan letus.
   ============================================================ */
data class ExhaustStyle(
    val id: String, val name: String, val desc: String,
    val f0: Double = 1.0,       // pengali nada dasar
    val decay: Double = 1.0,    // panjang dengung ledakan
    val partials: DoubleArray = doubleArrayOf(1.0, 0.55, 0.32, 0.18),
    val noise: Double = 0.25,   // campuran desis
    val amHz: Double = 0.0,     // modulasi amplitude (helikopter)
    val amDepth: Double = 0.0,
    val ring: Double = 0.0,     // dengung logam panjang (thai)
    val ringHz: Double = 1800.0,
    val boom: Double = 0.0,     // gebukan gendang (drumben)
    val boomHz: Double = 78.0,
    val pop: Double = 0.25      // kecenderungan letusan saat gas ditutup
)

val EXH_STYLES = listOf(
    ExhaustStyle("standar", "Standar", "Bersekat rapat, dengung pendek, bass tebal.",
        f0 = 1.0, decay = 0.75, noise = 0.18, pop = 0.08),
    ExhaustStyle("racing", "Racing", "Aliran bebas, dengung panjang dan keras.",
        f0 = 1.18, decay = 1.45, partials = doubleArrayOf(1.0, 0.72, 0.48, 0.3, 0.16), noise = 0.34, pop = 0.45),
    ExhaustStyle("kopong", "Kopong", "Tanpa peredam: ledakan telanjang dan gampang meledak.",
        f0 = 1.32, decay = 1.9, partials = doubleArrayOf(1.0, 0.85, 0.62, 0.44, 0.28, 0.16), noise = 0.5, pop = 0.85),
    ExhaustStyle("chamber", "Chamber 2-Tak", "Kerucut resonansi, nada nyaring dan tajam.",
        f0 = 1.75, decay = 0.85, partials = doubleArrayOf(1.0, 0.9, 0.55, 0.42, 0.2), noise = 0.22, pop = 0.35),
    ExhaustStyle("drumben", "Drumben", "Ruang membran: tiap ledakan jadi gebukan gendang dalam.",
        f0 = 0.82, decay = 1.1, partials = doubleArrayOf(1.0, 0.3, 0.12), noise = 0.14,
        boom = 0.85, boomHz = 72.0, pop = 0.2),
    ExhaustStyle("helikopter", "Helikopter", "Ujung berbilah: suara terpotong beraturan seperti baling-baling.",
        f0 = 1.05, decay = 1.3, partials = doubleArrayOf(1.0, 0.45, 0.3, 0.12), noise = 0.3,
        amHz = 11.0, amDepth = 0.62, pop = 0.3),
    ExhaustStyle("thai", "Thai Style", "Pipa kecil panjang: melengking tinggi dengan dengung logam panjang.",
        f0 = 2.1, decay = 1.6, partials = doubleArrayOf(1.0, 1.05, 0.75, 0.5, 0.3, 0.18), noise = 0.16,
        ring = 0.55, ringHz = 2600.0, pop = 0.55),
    ExhaustStyle("vortex", "Vortex", "Pipa spiral: gelombang menggulung, nada melengkung ke atas.",
        f0 = 1.12, decay = 1.55, partials = doubleArrayOf(1.0, 0.6, 0.42, 0.26, 0.14), noise = 0.36, pop = 0.4)
)
val EXH_BY_ID = EXH_STYLES.associateBy { it.id }
fun exhStyle(id: String): ExhaustStyle = EXH_BY_ID[id] ?: EXH_STYLES[0]

/* ============================================================
   Pengendara
   ============================================================ */
data class Driver(
    val id: String, val name: String, val desc: String,
    val mass: Double, val throttle: Double, val brake: Double, val balance: Double,
    val skill: String, val color: Int
)
val DRIVERS = listOf(
    Driver("budi", "Budi", "Mekanik kampung. Gas halus, pengereman aman, paling seimbang.",
        72.0, 1.00, 1.00, 1.00, "Seimbang", 0xFF22D3EE.toInt()),
    Driver("rina", "Rina", "Rider harian. Ringan, tarikan awal lebih enteng, agak gugup di jalan rusak.",
        58.0, 1.05, 0.92, 1.08, "Lincah", 0xFFEC4899.toInt()),
    Driver("jono", "Jono", "Tukang angkut. Bobot berat, gas pol, tapi rem mendadak bikin ban selip.",
        98.0, 1.12, 0.85, 0.92, "Kuat", 0xFFF59E0B.toInt()),
    Driver("sastro", "Pak Sastro", "Veteran sabar. Gas pelan, mesin awet, handling kalem.",
        85.0, 0.90, 1.10, 1.15, "Kalem", 0xFF22C55E.toInt()),
    Driver("dewi", "Dewi", "Pembalap liar. Gas spontan, keseimbangan bagus, ban cepat aus.",
        62.0, 1.18, 1.05, 1.05, "Agresif", 0xFFA855F7.toInt())
)
val DRIVER_BY_ID = DRIVERS.associateBy { it.id }

/* ============================================================
   Map
   ============================================================ */
data class Pal(val sky0: Int, val sky1: Int, val hill: Int, val hill2: Int,
               val road: Int, val road2: Int, val line: Int, val dust: Int)
data class MapDef(
    val id: String, val name: String, val desc: String,
    val panjang: Int, val flat: Int, val ramp: Int,
    val waves: List<Triple<Double, Double, Double>>, // panjang gelombang (m), amplitudo (m), fase
    val bumps: DoubleArray,                          // [k1, a1, k2, a2]
    val obsTypes: List<String>, val obsRate: Double, val obsMinX: Int,
    val pal: Pal, val straight: Boolean = false
)
val MAPS = listOf(
    MapDef("kota", "Jalan Kota", "Aspal mulus, tanjakan landai, rintangan ringan.",
        1400, 34, 70,
        listOf(Triple(110.0, 0.9, 0.0), Triple(240.0, 1.6, 1.7), Triple(420.0, 2.4, 0.4), Triple(900.0, 4.0, 2.2)),
        doubleArrayOf(0.29, 0.12, 0.61, 0.06),
        listOf("drum", "mobil"), 0.0035, 120,
        Pal(0xFF0B1220.toInt(), 0xFF1E3A5F.toInt(), 0xFF16233A.toInt(), 0xFF0E1A2B.toInt(),
            0xFF2A3444.toInt(), 0xFF3B4759.toInt(), 0xFF94A3B8.toInt(), 0xFF334155.toInt())),
    MapDef("gunung", "Tanjakan Gunung", "Tanjakan panjang menanjak, udara tipis, pemandangan bukit.",
        1200, 26, 55,
        listOf(Triple(100.0, 1.2, 0.0), Triple(220.0, 1.9, 1.2), Triple(430.0, 3.0, 0.3), Triple(880.0, 4.5, 2.0)),
        doubleArrayOf(0.33, 0.16, 0.67, 0.08),
        listOf("batu", "pohon"), 0.0042, 90,
        Pal(0xFF0A1424.toInt(), 0xFF24405E.toInt(), 0xFF1B2C44.toInt(), 0xFF101C2E.toInt(),
            0xFF303B4C.toInt(), 0xFF414E61.toInt(), 0xFFCBD5E1.toInt(), 0xFF3E4A5C.toInt())),
    MapDef("gurun", "Gurun Pasir", "Gelombang pasir panjang, debu beterbangan, jarak pandang jauh.",
        1600, 40, 80,
        listOf(Triple(90.0, 1.1, 0.0), Triple(200.0, 2.4, 0.9), Triple(400.0, 4.2, 2.1), Triple(950.0, 3.2, 0.7)),
        doubleArrayOf(0.25, 0.10, 0.55, 0.05),
        listOf("batu", "drum"), 0.0030, 150,
        Pal(0xFF2A1B10.toInt(), 0xFF6B4A21.toInt(), 0xFF3A2A16.toInt(), 0xFF241A0E.toInt(),
            0xFF8A6A38.toInt(), 0xFFA07C45.toInt(), 0xFFE7D2A8.toInt(), 0xFFB08C4E.toInt())),
    MapDef("rusak", "Jalan Rusak", "Aspal berlubang, penuh rintangan. Cepat bikin part rusak.",
        1200, 22, 45,
        listOf(Triple(60.0, 0.7, 0.0), Triple(130.0, 1.3, 1.4), Triple(280.0, 2.0, 0.6), Triple(600.0, 2.6, 1.9)),
        doubleArrayOf(0.42, 0.22, 0.85, 0.12),
        listOf("batu", "drum", "mobil", "pagar"), 0.0090, 60,
        Pal(0xFF101418.toInt(), 0xFF2A2F36.toInt(), 0xFF1A1F26.toInt(), 0xFF0E1116.toInt(),
            0xFF333A42.toInt(), 0xFF414A54.toInt(), 0xFF8B95A1.toInt(), 0xFF4A525C.toInt())),
    MapDef("lurus", "Lintasan Lurus", "Jalan datar tanpa rintangan, khusus buat setting dan tes top speed.",
        3000, 3000, 1,
        listOf(Triple(100000.0, 0.0, 0.0)), doubleArrayOf(0.0, 0.0, 0.0, 0.0),
        emptyList(), 0.0, 0,
        Pal(0xFF070C14.toInt(), 0xFF16243C.toInt(), 0xFF0F1826.toInt(), 0xFF0A111C.toInt(),
            0xFF242E3D.toInt(), 0xFF2F3B4D.toInt(), 0xFFF1F5F9.toInt(), 0xFF3A4557.toInt()),
        straight = true)
)
val MAP_BY_ID = MAPS.associateBy { it.id }

/* ============================================================
   Preset: susunan part di grid bengkel.
   Tulisan per baris = isi kolom, tanda "-" berarti kosong.
   ============================================================ */
data class Preset(
    val id: String, val name: String, val desc: String,
    val rows: List<String>, val colorIdx: Int = 0, val driver: String = "budi",
    val tags: List<String> = emptyList()
)

val PRESETS = listOf(
    Preset("bebek", "Bebek Harian 125cc", "Matik harian: irit, halus, tarikan cukup.",
        listOf("blk1 hd1 pis1 rod1 crk1 cam1 vlv1 spr1 car1 flt1",
               "ex1 tk1 fu2 cdi1 cvt1 chn1",
               "fr1 ty1 ty2 rm1 rm2 brk1 sa1 fk1 sh1 st1 sk1 fp1 lp1 bt1"),
        colorIdx = 8, driver = "budi", tags = listOf("harian", "matik")),
    Preset("sport250", "Sport 250 Parallel Twin", "Dua silinder segaris, putaran atas panjang.",
        listOf("blk2 hd2 pis1 rod2 crk3 cam2 vlv2 spr1 car2 flt2 im2",
               "hdr1 ex2 tk1 fu3 cdi2 gb6 clt1 chn1 rd1",
               "fr2 ty3 ty3 rm2 rm2 brk1 sa2 fk2 sh1 st2 sk2 fr9 lp1 bt1"),
        colorIdx = 4, driver = "rina", tags = listOf("sport", "manual")),
    Preset("cruiser", "Cruiser V-Twin 750", "Gumuruh dalam, torsi besar di putaran bawah.",
        listOf("blkv2 hd1 pis2 rod1 crk2 cam1 vlv1 spr1 inj1 flt1",
               "hdr1 ex2 tk1 fu2 cdi1 gb5 clt1 chn1",
               "fr1 ty2 ty3 rm1 rm1 brk1 sa1 fk1 sh2 st3 sk1 fp1 lp1 bt1"),
        colorIdx = 3, driver = "sastro", tags = listOf("cruiser", "torsi")),
    Preset("mobil", "Mobil I4 1500 Turbo", "Empat silinder turbo, transmisi enam percepatan.",
        listOf("blk4 hd2 pis1 rod1 crk1 cam2 vlv2 spr1 inj1 tur1 ic1 im1",
               "hdr2 ex2 tk1 fu2 cdi3 gb6 clt1 fd1 rd1 bt1",
               "fr4 ty5 ty5 rm3 rm3 brk1 sa1 fk1 sh1 st1 sk1 fr9",
               "ty5 ty5 rm3 rm3 lp1 bt1"),
        colorIdx = 9, driver = "budi", tags = listOf("mobil", "turbo")),
    Preset("boxer", "Boxer 1200 Touring", "Dua silinder berlawanan, dengung halus jarak jauh.",
        listOf("blkbox hd1 pis2 rod1 crk1 cam1 vlv1 spr1 inj1 flt1",
               "hdr1 ex1 tk1 fu2 cdi1 gb6 clt1 chn1 rd1",
               "fr3 ty2 ty2 rm1 rm1 brk1 sa1 fk1 sh1 st3 sk1 fr9 lp1 bt1"),
        colorIdx = 1, driver = "sastro", tags = listOf("touring", "halus")),
    Preset("kotlin", "Kotlin 2-Tak 100cc", "Ringan, melengking, ledakan tiap putaran.",
        listOf("blk2t hd1 pis1 rod2 crk3 cam1 vlv1 spr1 car2 flt2",
               "ex4 tk1 fu3 cdi2 cvt2 chn1",
               "fr1 ty1 ty2 rm1 rm2 brk1 sa1 fk1 sh1 st1 sk1 fp1 lp1 bt1"),
        colorIdx = 5, driver = "dewi", tags = listOf("2-tak", "nyaring")),
    Preset("v8", "V8 Muscle 5000 SC", "Supercharger di atas delapan silinder. Gumuruh dalam.",
        listOf("blkv8 hd2 pis1 rod1 crk2 cam2 vlv2 spr1 sc1 flt2 im2",
               "hdr2 ex2 tk1 fu3 cdi3 gb5 clt1 fd2 rd1 bt1",
               "fr4 ty5 ty5 rm3 rm3 brk1 sa1 fk1 sh1 st1 sk1 fr9",
               "ty5 ty5 rm3 rm3 lp1 bt1"),
        colorIdx = 4, driver = "jono", tags = listOf("muscle", "supercharger")),
    Preset("diesel", "Diesel Pikul I6 2500", "Solar, kompresi tinggi, suara klak-klak berat.",
        listOf("blk6 hd4 pis2 rod1 crk2 cam1 vlv1 spr1 inj1 flt1",
               "hdr1 ex2 tk1 fu4 cdi1 gb5 clt1 fd1 rd1 bt1",
               "fr4 ty5 ty5 rm3 rm3 brk1 sa1 fk1 sh2 st1 sk1 fp1",
               "ty5 ty5 rm3 rm3 lp1 bt1"),
        colorIdx = 2, driver = "jono", tags = listOf("diesel", "pikul")),
    Preset("i3turbo", "I3 Turbo Liar", "Tiga silinder turbo kecil: ngelitik, liar, blow-off cerewet.",
        listOf("blk3 hd3 pis3 rod2 crk3 cam3 vlv2 spr1 tur1 ic1 im2",
               "hdr2 ex3 tk1 fu3 cdi3 gb6 clt1 chn1 rd1 bt1",
               "fr2 ty3 ty3 rm2 rm2 brk1 sa2 fk2 sh1 st2 sk2 fp1 lp1"),
        colorIdx = 11, driver = "dewi", tags = listOf("liar", "turbo")),
    Preset("trail", "Trail Adventure 250", "Suspensi tinggi, ban pacul, siap jalan rusak.",
        listOf("blk1b hd1 pis1 rod1 crk1 cam1 vlv1 spr1 car1 flt1",
               "ex2 tk1 fu2 cdi1 gb5 clt1 chn1",
               "fr3 ty4 ty4 rm1 rm1 brk1 sa1 fk1 sh1 st3 sk1 fp1 lp1 bt1"),
        colorIdx = 6, driver = "rina", tags = listOf("trail", "tanah")),
    Preset("rotary13b", "Rotary 13B Balap", "Dua rotor wankel: enam ledakan per putaran, brap-brap melengking.",
        listOf("blkrot rot1 rot1 eccs hd2 cam2 vlv2 spr1 car2 flt2 im2",
               "hdr2 ex7 tk1 fu3 cdi3 gb6 clt1 chn1 rd1",
               "fr2 ty3 ty3 rm2 rm2 brk1 sa2 fk2 sh1 st2 sk2 fp1 lp1 bt1"),
        colorIdx = 5, driver = "dewi", tags = listOf("rotary", "balap")),
    Preset("rotary20b", "Rotary 20B Tiga Rotor", "Sembilan ledakan per putaran. Nada tinggi khas Le Mans.",
        listOf("blkrot3 rot1 rot1 rot1 eccs hd3 cam3 vlv2 spr1 tur1 ic1 im2",
               "hdr2 ex3 tk1 fu3 cdi2 gb6 clt1 fd2 rd1 bt1",
               "fr2 ty3 ty3 rm2 rm2 brk1 sa2 fk2 sh1 st2 sk2 fr9 lp1"),
        colorIdx = 4, driver = "dewi", tags = listOf("rotary", "ekstrem")),
    Preset("drumben", "Knalpot Drumben", "Knalpot gendang: tiap ledakan jadi gebukan dung-dung.",
        listOf("blk1 hd1 pis1 rod1 crk1 cam2 vlv1 spr1 car2 flt2",
               "ex5 tk1 fu3 cdi2 cvt1 chn1",
               "fr1 ty1 ty2 rm1 rm2 brk1 sa1 fk1 sh1 st1 sk1 fp1 lp1 bt1"),
        colorIdx = 5, driver = "budi", tags = listOf("gendang", "unik")),
    Preset("heli", "Gaya Helikopter", "Ujung berbilah: suara terpotong seperti baling-baling.",
        listOf("blk1 hd1 pis1 rod1 crk1 cam1 vlv1 spr1 car2 flt2",
               "ex6 tk1 fu2 cdi2 cvt1 chn1",
               "fr1 ty1 ty2 rm1 rm2 brk1 sa1 fk1 sh1 st1 sk1 fp1 lp1 bt1"),
        colorIdx = 8, driver = "budi", tags = listOf("helikopter", "unik")),
    Preset("thai", "Gaya Thai", "Pipa kecil panjang: melengking tinggi dan berdengung.",
        listOf("blk1 hd1 pis3 rod2 crk3 cam3 vlv2 spr1 car2 flt2",
               "ex7 tk1 fu3 cdi2 cvt2 chn1",
               "fr1 ty1 ty2 rm1 rm2 brk1 sa1 fk1 sh1 st1 sk2 fp1 lp1 bt1"),
        colorIdx = 6, driver = "dewi", tags = listOf("thai", "nyaring")),
    Preset("sprint", "Rangka Ringan Sprint", "Rangka custom ringan buat akselerasi cepat.",
        listOf("blk1 hd1 pis3 rod2 crk3 cam2 vlv2 spr1 car2 flt2",
               "ex2 tk1 fu3 cdi2 gb4 clt1 fd1 chn1",
               "fr5 ty3 ty3 rm2 rm2 brk1 sa2 fk2 sh1 st2 sk2 fp1 lp1 bt1"),
        colorIdx = 0, driver = "rina", tags = listOf("sprint", "custom"))
)

/** pasang preset ke rakitan: part besar otomatis diberi ruang lebih */
fun pasangPreset(b: Build, ps: Preset) {
    b.clear()
    ps.rows.forEachIndexed { r, baris ->
        var c = 0
        for (id in baris.trim().split(Regex("\\s+"))) {
            if (id == "-") { c++; continue }
            val pt = PART[id] ?: continue
            var geser = 0
            while (c + pt.gw <= GRID_W && !b.canPlace(id, c, r) && geser < GRID_W) { c++; geser++ }
            if (c + pt.gw <= GRID_W && b.place(id, c, r)) c += pt.gw
        }
    }
    b.colorIdx = ps.colorIdx
    b.driverId = ps.driver
    b.name = ps.name
}

/** ubah baris preset jadi penempatan (kolom, baris, id part) */
fun presetPlacements(ps: Preset): List<Triple<Int, Int, String>> {
    val out = ArrayList<Triple<Int, Int, String>>()
    ps.rows.forEachIndexed { r, row ->
        val cells = row.trim().split(Regex("\\s+"))
        cells.forEachIndexed { c, id -> if (id != "-" && PART.containsKey(id)) out.add(Triple(c, r, id)) }
    }
    return out
}
