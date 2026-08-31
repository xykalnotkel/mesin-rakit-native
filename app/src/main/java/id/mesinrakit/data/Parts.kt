package id.mesinrakit.data

/* ============================================================
   Katalog part. Satu part = satu komponen nyata yang bisa
   dipasang di grid bengkel. Bentuknya (shape) dipakai buat
   menggambar part secara realistis, bukan sekadar kotak.
   ============================================================ */

data class Part(
    val id: String, val name: String, val short: String, val cat: String, val desc: String,
    val price: Int = 0, val massKg: Double = 1.0, val shape: String = "box",
    val gw: Int = 1, val gh: Int = 1,
    // blok & rotasi
    val cyl: Int = 0, val layout: String = "", val twoStroke: Boolean = false, val rotary: Boolean = false,
    val bore: Double = 0.0, val strokeLen: Double = 0.0, val ccPer: Double = 0.0,
    // kepala & noken
    val flow: Double = 1.0, val valves: Int = 2, val comp: Double = 0.0,
    val camProfile: Double = 0.0, val camLope: Double = 0.0, val camDur: Double = 0.0,
    // intake & forced induction
    val throttle: Double = 1.0, val plenum: Double = 1.0, val boost: Double = 0.0,
    val lag: Double = 0.0, val indEff: Double = 1.0, val indDrive: String = "",
    // knalpot
    val exhLen: Double = 1.0, val exhDia: Double = 0.05, val muffling: Double = 0.5, val style: String = "standar",
    // bahan bakar
    val fuel: String = "", val octane: Double = 0.0, val energy: Double = 1.0,
    // transmisi
    val cvt: Boolean = false, val ratioLow: Double = 0.0, val ratioTop: Double = 0.0,
    val ratios: DoubleArray = doubleArrayOf(), val eff: Double = 0.92, val finalDrive: Double = 0.0,
    // roda & ban
    val wheelDia: Double = 0.0, val wheelW: Double = 0.0, val grip: Double = 1.0, val rolling: Double = 1.0,
    val rimType: String = "", val brake: Double = 0.0,
    // rangka & body
    val frameType: String = "", val strength: Double = 1.0, val wheelbase: Double = 0.0,
    val seatH: Double = 0.0, val aero: Double = 1.0,
    // pengapian
    val advance: Double = 0.0, val advancePeak: Double = 0.0, val limiter: String = "",
    val limiterRPM: Int = 0, val multiSpark: Double = 0.0,
    val tags: List<String> = emptyList()
)

val CATEGORIES = listOf(
    "blok" to "Blok Mesin", "rot" to "Rotating", "head" to "Kepala Silinder",
    "intake" to "Intake", "exhaust" to "Knalpot", "fuel" to "Bahan Bakar",
    "cdi" to "Pengapian", "trans" to "Transmisi", "wheel" to "Roda & Ban",
    "frame" to "Rangka & Suspensi", "body" to "Body & Perlengkapan"
)

private fun p(
    id: String, name: String, short: String, cat: String, desc: String,
    price: Int, mass: Double, shape: String, vararg rest: Any
): Part {
    var q = Part(id, name, short, cat, desc, price, mass, shape)
    for (r in rest) {
        when (r) {
            is Pair<*, *> -> when (r.first) {
                "gw" -> q = q.copy(gw = r.second as Int)
                "gh" -> q = q.copy(gh = r.second as Int)
                "cyl" -> q = q.copy(cyl = r.second as Int)
                "layout" -> q = q.copy(layout = r.second as String)
                "2t" -> q = q.copy(twoStroke = r.second as Boolean)
                "rotary" -> q = q.copy(rotary = r.second as Boolean)
                "bore" -> q = q.copy(bore = r.second as Double)
                "stroke" -> q = q.copy(strokeLen = r.second as Double)
                "cc" -> q = q.copy(ccPer = r.second as Double)
                "flow" -> q = q.copy(flow = r.second as Double)
                "valves" -> q = q.copy(valves = r.second as Int)
                "comp" -> q = q.copy(comp = r.second as Double)
                "cam" -> q = q.copy(camProfile = r.second as Double)
                "lope" -> q = q.copy(camLope = r.second as Double)
                "dur" -> q = q.copy(camDur = r.second as Double)
                "thr" -> q = q.copy(throttle = r.second as Double)
                "plenum" -> q = q.copy(plenum = r.second as Double)
                "boost" -> q = q.copy(boost = r.second as Double)
                "lag" -> q = q.copy(lag = r.second as Double)
                "ieff" -> q = q.copy(indEff = r.second as Double)
                "idrive" -> q = q.copy(indDrive = r.second as String)
                "elen" -> q = q.copy(exhLen = r.second as Double)
                "edia" -> q = q.copy(exhDia = r.second as Double)
                "muff" -> q = q.copy(muffling = r.second as Double)
                "style" -> q = q.copy(style = r.second as String)
                "fuel" -> q = q.copy(fuel = r.second as String)
                "oct" -> q = q.copy(octane = r.second as Double)
                "energy" -> q = q.copy(energy = r.second as Double)
                "cvt" -> q = q.copy(cvt = r.second as Boolean)
                "rlow" -> q = q.copy(ratioLow = r.second as Double)
                "rtop" -> q = q.copy(ratioTop = r.second as Double)
                "ratios" -> q = q.copy(ratios = r.second as DoubleArray)
                "eff" -> q = q.copy(eff = r.second as Double)
                "final" -> q = q.copy(finalDrive = r.second as Double)
                "wdia" -> q = q.copy(wheelDia = r.second as Double)
                "ww" -> q = q.copy(wheelW = r.second as Double)
                "grip" -> q = q.copy(grip = r.second as Double)
                "roll" -> q = q.copy(rolling = r.second as Double)
                "rim" -> q = q.copy(rimType = r.second as String)
                "brake" -> q = q.copy(brake = r.second as Double)
                "ftype" -> q = q.copy(frameType = r.second as String)
                "str" -> q = q.copy(strength = r.second as Double)
                "wb" -> q = q.copy(wheelbase = r.second as Double)
                "seath" -> q = q.copy(seatH = r.second as Double)
                "aero" -> q = q.copy(aero = r.second as Double)
                "adv" -> q = q.copy(advance = r.second as Double)
                "advp" -> q = q.copy(advancePeak = r.second as Double)
                "lim" -> q = q.copy(limiter = r.second as String)
                "lrpm" -> q = q.copy(limiterRPM = r.second as Int)
                "mspark" -> q = q.copy(multiSpark = r.second as Double)
                "tags" -> q = q.copy(tags = r.second as List<String>)
            }
        }
    }
    return q
}

val PART_LIST: List<Part> = listOf(
    /* ---------------- BLOK MESIN ---------------- */
    p("blk1", "Blok 1 Silinder 125cc", "Blok 125", "blok",
        "Blok silinder tunggal tegak, pendingin udara. Nada dasar dalam dan ngentak.",
        900_000, 9.0, "block1", "cyl" to 1, "layout" to "I1", "bore" to 0.0525, "stroke" to 0.0574,
        "cc" to 125.0, "tags" to listOf("harian", "irit")),
    p("blk1b", "Blok 1 Silinder 200cc", "Blok 200", "blok",
        "Satu silinder lebih besar. Torsi bawah naik, suara makin menggelegar.",
        1_450_000, 12.0, "block1", "cyl" to 1, "layout" to "I1", "bore" to 0.067, "stroke" to 0.0566,
        "cc" to 200.0, "tags" to listOf("torsi")),
    p("blk2t", "Blok 2-Tak 100cc", "Blok 2T", "blok",
        "Dua tak: sekali meledak tiap putaran. Suara tajam, ringan, dan melengking.",
        700_000, 7.0, "block1", "cyl" to 1, "layout" to "I1", "2t" to true, "bore" to 0.052, "stroke" to 0.047,
        "cc" to 100.0, "tags" to listOf("2-tak", "ringan")),
    p("blk2", "Blok Paralel 2 250cc", "Blok I2", "blok",
        "Dua silinder sejajar. Ledakan timpang 269/451 derajat, khas moge entry.",
        2_600_000, 18.0, "blockI2", "cyl" to 2, "layout" to "I2", "bore" to 0.060, "stroke" to 0.0441,
        "cc" to 250.0, "tags" to listOf("sport")),
    p("blk3", "Blok I3 900cc", "Blok I3", "blok",
        "Tiga silinder segaris. Nada unik antara I4 dan V-Twin, ngelitik di atas.",
        5_200_000, 32.0, "blockI3", "cyl" to 3, "layout" to "I3", "bore" to 0.078, "stroke" to 0.0628,
        "cc" to 900.0, "tags" to listOf("karakter")),
    p("blk4", "Blok I4 1500cc", "Blok I4", "blok",
        "Empat silinder segaris. Empat ledakan rata tiap 180 derajat, mulus sampai atas.",
        7_800_000, 45.0, "blockI4", "cyl" to 4, "layout" to "I4", "bore" to 0.075, "stroke" to 0.0848,
        "cc" to 1500.0, "tags" to listOf("halus", "mobil")),
    p("blk6", "Blok I6 2500cc", "Blok I6", "blok",
        "Enam silinder segaris. Suara halus seperti turbin, nyaris tanpa getar.",
        14_500_000, 78.0, "blockI4", "gw" to 3, "cyl" to 6, "layout" to "I6", "bore" to 0.082, "stroke" to 0.0788,
        "cc" to 2500.0, "tags" to listOf("mewah", "halus")),
    p("blkv2", "Blok V-Twin 750cc 45deg", "Blok V2", "blok",
        "Dua silinder membentuk V 45 derajat. Ledakan 313/407 derajat, khas cruiser.",
        8_400_000, 42.0, "blockV2", "cyl" to 2, "layout" to "V2", "bore" to 0.085, "stroke" to 0.066,
        "cc" to 750.0, "tags" to listOf("cruiser", "bass")),
    p("blkbox", "Blok Boxer 1200cc", "Blok Boxer", "blok",
        "Dua silinder berlawanan. Ledakan rata 360/360, getar minim, dengung khas.",
        16_500_000, 62.0, "boxer", "cyl" to 2, "layout" to "boxer", "bore" to 0.101, "stroke" to 0.073,
        "cc" to 1200.0, "tags" to listOf("touring", "dengung")),
    p("blkv8", "Blok V8 5000cc", "Blok V8", "blok",
        "Delapan silinder, crossplane. Delapan ledakan tiap 90 derajat, gumuruh dalam.",
        28_000_000, 145.0, "blockV8", "gw" to 3, "cyl" to 8, "layout" to "V8", "bore" to 0.092, "stroke" to 0.092,
        "cc" to 5000.0, "tags" to listOf("muscle", "gumuruh")),
    p("blkrot", "Rumah Rotary 13B", "Rumah Rotary", "blok",
        "Rumah wankel dua rotor. Tiga ledakan tiap rotor tiap putaran poros, suara brap-brap khas rotary.",
        22_000_000, 68.0, "rotaryHousing", "gw" to 2, "cyl" to 2, "layout" to "rotary", "rotary" to true,
        "cc" to 654.0, "tags" to listOf("rotary", "langka")),
    p("blkrot3", "Rumah Rotary 20B 3 Rotor", "Rotary 3", "blok",
        "Tiga rotor: sembilan ledakan tiap putaran. Melengking tinggi seperti balapan Le Mans.",
        48_000_000, 105.0, "rotaryHousing", "gw" to 3, "cyl" to 3, "layout" to "rotary", "rotary" to true,
        "cc" to 654.0, "tags" to listOf("rotary", "balap")),

    /* ---------------- ROTATING ---------------- */
    p("pis1", "Piston Forged Ringan", "Piston Forged", "rot",
        "Piston tempa, ring tipis, skirt pendek. Respons gas cepat, suara mekanik lebih renyah.",
        350_000, 0.45, "piston", "comp" to 11.5, "tags" to listOf("respons")),
    p("pis2", "Piston Cast Standar", "Piston Cast", "rot",
        "Piston cor pabrikan. Awet, suara lebih kalem, bobot sedikit lebih berat.",
        180_000, 0.62, "piston", "comp" to 9.5, "tags" to listOf("awet")),
    p("pis3", "Piston High Comp Balap", "Piston HC", "rot",
        "Kompresi tinggi, dominan tajam. Mesin jadi galak dan gampang ngelitik.",
        780_000, 0.40, "piston", "comp" to 13.8, "tags" to listOf("galak", "balap")),
    p("ring1", "Set Ring Piston", "Ring Piston", "rot",
        "Ring kompresi dan oli. Kalau aus, muncul suara semburan napas bocor.",
        120_000, 0.05, "ring", "tags" to listOf("seal")),
    p("rod1", "Setang Piston Baja", "Setang Piston", "rot",
        "Batang penghubung piston ke kruk as. Makin panjang, karakter makin kalem.",
        420_000, 0.55, "rod", "tags" to listOf("kuat")),
    p("rod2", "Setang Piston Titanium", "Setang Ti", "rot",
        "Ringan ekstrem, mesin naik rpm lebih cepat, dengung mekanik lebih tajam.",
        1_900_000, 0.28, "rod", "tags" to listOf("ringan", "balap")),
    p("crk1", "Kruk As Standar", "Kruk As", "rot",
        "Poros engkol dengan bobot imbang standar. Putaran halus di rpm rendah.",
        680_000, 3.2, "crank", "tags" to listOf("halus")),
    p("crk2", "Kruk As Stroker", "Kruk Stroker", "rot",
        "Langkah lebih panjang, torsi bawah naik, nada dasar mesin makin berat.",
        1_250_000, 3.9, "crank", "stroke" to 0.008, "tags" to listOf("torsi", "bass")),
    p("crk3", "Kruk As Light Balance", "Kruk Ringan", "rot",
        "Bobot imbang dipangkas. Mesin naik dan turun rpm lebih cepat.",
        1_450_000, 2.4, "crank", "tags" to listOf("respons")),
    p("rot1", "Rotor Rotary", "Rotor", "rot",
        "Rotor segitiga berputar eksentrik di dalam rumah wankel.",
        2_800_000, 4.2, "rotor", "rotary" to true, "tags" to listOf("rotary")),
    p("eccs", "Poros Eksentrik Rotary", "Poros Eksentrik", "rot",
        "Poros output rotary. Satu putaran rotor sama dengan tiga putaran poros ini.",
        2_100_000, 3.6, "eccShaft", "rotary" to true, "tags" to listOf("rotary")),
    p("fly1", "Flywheel Standar", "Flywheel", "rot",
        "Roda gila pabrikan. Putaran stabil, mesin gak gampang mati.",
        380_000, 2.6, "flywheel", "tags" to listOf("stabil")),
    p("fly2", "Flywheel Ringan", "Flywheel Ringan", "rot",
        "Bobot dipangkas. Gas langsung naik, tapi mesin lebih getar di stasioner.",
        950_000, 1.1, "flywheel", "tags" to listOf("respons", "lope")),

    /* ---------------- KEPALA SILINDER ---------------- */
    p("hd1", "Kepala 2 Klep Harian", "Head 2 Klep", "head",
        "Dua klep per silinder. Aliran standar, suara halus dan bersahabat.",
        850_000, 4.5, "head", "flow" to 1.0, "valves" to 2, "comp" to 9.8, "tags" to listOf("harian")),
    p("hd2", "Kepala 4 Klep Racing", "Head 4 Klep", "head",
        "Empat klep, port dipoles. Nafas panjang, suara menghisap lebih jelas.",
        2_300_000, 5.2, "head2", "flow" to 1.35, "valves" to 4, "comp" to 11.0, "tags" to listOf("aliran")),
    p("hd3", "Kepala Porting Balap", "Head Porting", "head",
        "Port besar ekstrem. Atasnya liar, bawahnya kosong, suara serak.",
        4_100_000, 5.0, "head2", "flow" to 1.6, "valves" to 4, "comp" to 12.6, "tags" to listOf("atas", "liar")),
    p("hd4", "Kepala Diesel Kubah", "Head Diesel", "head",
        "Kepala solar dengan ruang bakar kubah. Tekanan tinggi, suara klak-klak tajam.",
        2_900_000, 7.5, "head", "flow" to 0.95, "valves" to 2, "comp" to 18.0, "tags" to listOf("diesel")),
    p("cam1", "Noken As Standar", "Noken Standar", "head",
        "Profil klep pabrikan. Stasioner rata, tanpa kelopakan.",
        620_000, 1.4, "cam", "cam" to 0.30, "lope" to 0.05, "dur" to 240.0, "tags" to listOf("halus")),
    p("cam2", "Noken As Racing", "Noken Racing", "head",
        "Durasi panjang, bukaan lebih dalam. Stasioner jadi nggak rata dan berdenyut.",
        1_500_000, 1.5, "cam", "cam" to 0.62, "lope" to 0.45, "dur" to 288.0, "tags" to listOf("lope", "atas")),
    p("cam3", "Noken As Balap Ekstrem", "Noken Balap", "head",
        "Kelopakan parah. Stasioner brebet mengayun, tenaga baru muncul di putaran atas.",
        2_700_000, 1.6, "cam", "cam" to 0.90, "lope" to 0.85, "dur" to 320.0, "tags" to listOf("lope", "ekstrem")),
    p("vlv1", "Set Klep Baja", "Klep Baja", "head",
        "Klep baja standar, rapat dan awet.",
        240_000, 0.35, "valve", "tags" to listOf("awet")),
    p("vlv2", "Set Klep Titanium", "Klep Ti", "head",
        "Klep titanium ringan, bisa digeber lebih tinggi tanpa melayang.",
        1_600_000, 0.20, "valve", "tags" to listOf("balap")),
    p("spr1", "Per Klep Keras", "Per Klep", "head",
        "Per lebih keras menahan rpm tinggi, dengung mekanik sedikit lebih nyaring.",
        180_000, 0.25, "spring", "tags" to listOf("rpm")),

    /* ---------------- INTAKE ---------------- */
    p("car1", "Karburator VM 22mm", "Karbu 22", "intake",
        "Karburator venturi 22 mm. Isapan pendek dan berdesis halus.",
        420_000, 0.9, "carb", "thr" to 0.85, "tags" to listOf("karbu")),
    p("car2", "Karburator Racing 33mm", "Karbu 33", "intake",
        "Venturi besar, isapan nyaring dan respons instan.",
        1_100_000, 1.1, "carb", "thr" to 1.15, "tags" to listOf("karbu", "balap")),
    p("inj1", "Injektor + ECU Standar", "Injeksi", "intake",
        "Injeksi bahan bakar terkendali. Stasioner rapi, gas halus.",
        1_850_000, 1.6, "injector", "thr" to 1.0, "tags" to listOf("injeksi")),
    p("flt1", "Filter Udara Kotak", "Filter Kotak", "intake",
        "Saringan kotak kedap. Suara isapan tertahan, mesin terdengar lebih kalem.",
        150_000, 0.5, "filter", "plenum" to 0.85, "tags" to listOf("senyap")),
    p("flt2", "Filter Udara Terbuka", "Filter Bebas", "intake",
        "Filter terbuka. Isapan terdengar jelas, siulan masuk angin makin renyah.",
        480_000, 0.4, "filter", "plenum" to 1.2, "tags" to listOf("isapan", "nyaring")),
    p("tur1", "Turbo Kecil IHI", "Turbo Kecil", "intake",
        "Turbo kecil, spool cepat. Siulan tinggi dan desisan blow-off pendek.",
        4_200_000, 6.5, "turbo", "boost" to 0.9, "lag" to 0.22, "ieff" to 1.0, "idrive" to "turbo",
        "tags" to listOf("turbo", "respons")),
    p("tur2", "Turbo Besar T04Z", "Turbo Besar", "intake",
        "Turbo besar, jeda terasa jelas. Atasnya mengamuk, siulannya tebal.",
        8_600_000, 11.0, "turbo", "boost" to 1.8, "lag" to 0.55, "ieff" to 1.05, "idrive" to "turbo",
        "tags" to listOf("turbo", "atas")),
    p("sc1", "Supercharger Screw", "Supercharger", "intake",
        "Kompresor mekanik. Tanpa jeda, siulan melengking naik mengikuti rpm.",
        12_500_000, 18.0, "sc", "boost" to 1.1, "lag" to 0.02, "ieff" to 0.92, "idrive" to "sc",
        "tags" to listOf("supercharger")),
    p("ic1", "Intercooler", "Intercooler", "intake",
        "Pendingin udara masuk. Udara lebih padat, tenaga naik, siulan sedikit teredam.",
        2_100_000, 5.5, "intercooler", "ieff" to 1.08, "tags" to listOf("pendingin")),
    p("im1", "Intake Manifold Panjang", "Manifold Panjang", "intake",
        "Pipa masuk panjang menambah torsi bawah dan memperdalam dengung isapan.",
        750_000, 1.8, "manifold", "plenum" to 1.1, "tags" to listOf("torsi")),
    p("im2", "Intake Manifold Pendek", "Manifold Pendek", "intake",
        "Pipa pendek: atas lebih liar, isapan lebih pendek dan tajam.",
        750_000, 1.4, "manifold", "plenum" to 0.95, "tags" to listOf("atas")),

    /* ---------------- KNALPOT ---------------- */
    p("ex1", "Knalpot Standar Kedap", "Knalpot Standar", "exhaust",
        "Knalpot pabrikan bersekat banyak. Suara halus, bass tebal, tanpa dengung logam.",
        650_000, 4.2, "muffler", "elen" to 1.0, "edia" to 0.045, "muff" to 0.92, "style" to "standar",
        "tags" to listOf("senyap")),
    p("ex2", "Knalpot Racing Free Flow", "Knalpot Racing", "exhaust",
        "Aliran bebas, redaman minim. Suara keras, dengung panjang, atas bebas.",
        1_650_000, 3.1, "mufflerRacing", "elen" to 1.35, "edia" to 0.058, "muff" to 0.42, "style" to "racing",
        "tags" to listOf("keras")),
    p("ex3", "Pipa Kopong Lurus", "Kopong", "exhaust",
        "Tanpa muffler sama sekali. Ledakan telanjang, nyaring, dan gampang letus saat turun gas.",
        480_000, 1.6, "pipe", "elen" to 1.5, "edia" to 0.062, "muff" to 0.06, "style" to "kopong",
        "tags" to listOf("kopong", "letus")),
    p("ex4", "Knalpot Chamber 2-Tak", "Chamber", "exhaust",
        "Ruang resonansi kerucut. Memantulkan gel balik, khas nada nyaring dua tak.",
        1_200_000, 3.4, "chamber", "elen" to 1.8, "edia" to 0.052, "muff" to 0.35, "style" to "chamber",
        "tags" to listOf("2-tak", "nyaring")),
    p("ex5", "Knalpot Drumben", "Drumben", "exhaust",
        "Ruang besi berlapis membran. Tiap ledakan jadi gebukan gendang: dung-dung, dalam, berulang.",
        2_400_000, 7.8, "mufflerDrum", "elen" to 1.15, "edia" to 0.075, "muff" to 0.55, "style" to "drumben",
        "tags" to listOf("gendang", "unik")),
    p("ex6", "Knalpot Helikopter", "Helikopter", "exhaust",
        "Ujung berbilah dua. Suara dipotong-potong beraturan seperti baling-baling heli: nge-ngung cepat.",
        3_100_000, 6.2, "mufflerHeli", "elen" to 1.25, "edia" to 0.070, "muff" to 0.40, "style" to "helikopter",
        "tags" to listOf("helikopter", "unik")),
    p("ex7", "Knalpot Gaya Thai", "Thai Style", "exhaust",
        "Pipa panjang kecil dengan kerucut ujung. Nada tinggi melengking, dengung logam tipis panjang.",
        1_900_000, 3.0, "mufflerThai", "elen" to 2.1, "edia" to 0.040, "muff" to 0.30, "style" to "thai",
        "tags" to listOf("thai", "nyaring")),
    p("ex8", "Knalpot Vortex", "Vortex", "exhaust",
        "Pipa dipilin spiral. Gelombang diputar, suara jadi menggulung dan agak sengau.",
        2_800_000, 5.0, "pipe", "elen" to 1.45, "edia" to 0.055, "muff" to 0.38, "style" to "vortex",
        "tags" to listOf("spiral", "unik")),
    p("hdr1", "Header 4-2-1", "Header 4-2-1", "exhaust",
        "Pipa kepala menyatu bertahap. Torsi bawah naik, suara lebih padat.",
        1_100_000, 3.6, "header", "elen" to 0.35, "tags" to listOf("torsi")),
    p("hdr2", "Header 4-1 Balap", "Header 4-1", "exhaust",
        "Empat pipa langsung jadi satu. Atas bebas, ledakan terdengar terpisah dan tajam.",
        1_450_000, 3.2, "header", "elen" to 0.25, "tags" to listOf("atas")),

    /* ---------------- BAHAN BAKAR ---------------- */
    p("fu1", "Bensin Pertalite 90", "Pertalite", "fuel",
        "Bensin oktan 90. Aman kompresi rendah, gampang ngelitik kalau dipaksa.",
        0, 2.0, "tank", "fuel" to "bensin", "oct" to 90.0, "energy" to 0.98, "tags" to listOf("harian")),
    p("fu2", "Bensin Pertamax 92", "Pertamax", "fuel",
        "Oktan 92, pembakaran lebih rapi. Suara ledakan sedikit lebih bersih.",
        0, 2.0, "tank", "fuel" to "bensin", "oct" to 92.0, "energy" to 1.0, "tags" to listOf("harian")),
    p("fu3", "Bensin Balap 108", "Bensin 108", "fuel",
        "Oktan tinggi. Kompresi ekstrem aman, ledakan lebih tajam dan kering.",
        0, 2.0, "tank", "fuel" to "bensin", "oct" to 108.0, "energy" to 1.05, "tags" to listOf("balap")),
    p("fu4", "Solar Dexlite", "Solar", "fuel",
        "Bahan bakar diesel. Kompresi tinggi, klep tidak dipakai, suara jadi klak-klak berat.",
        0, 2.2, "tank", "fuel" to "solar", "oct" to 45.0, "energy" to 1.06, "tags" to listOf("diesel")),
    p("fu5", "Etanol E85", "E85", "fuel",
        "Etanol tinggi oktan, pembakaran lebih dingin. Nada ledakan lebih rapat dan kering.",
        0, 2.1, "tank", "fuel" to "etanol", "oct" to 105.0, "energy" to 0.92, "tags" to listOf("etanol")),
    p("tk1", "Tangki 5 Liter", "Tangki 5L", "fuel",
        "Tangki bensin 5 liter, cukup buat harian dan jalan jauh.",
        450_000, 3.5, "tank", "tags" to listOf("isi")),
    p("pmp1", "Pompa Bensin Racing", "Pompa Bensin", "fuel",
        "Pompa bertekanan tinggi. Aliran stabil di putaran atas.",
        780_000, 0.8, "pump", "tags" to listOf("aliran")),

    /* ---------------- PENGAPIAN / CDI ---------------- */
    p("cdi1", "CDI Standar Pabrik", "CDI Standar", "cdi",
        "Kurva pengapian aman, limiter halus. Suara rapi tanpa gagap di putaran atas.",
        350_000, 0.25, "cdi", "adv" to 12.0, "advp" to 28.0, "lim" to "soft", "lrpm" to 1000, "mspark" to 0.0,
        "tags" to listOf("aman")),
    p("cdi2", "CDI Racing Unlimiter", "CDI Racing", "cdi",
        "Tanpa limiter, pengapian lebih maju. Mesin bebas berteriak sampai batas mekanis.",
        780_000, 0.25, "cdi", "adv" to 18.0, "advp" to 34.0, "lim" to "none", "lrpm" to 0, "mspark" to 0.3,
        "tags" to listOf("balap", "bebas")),
    p("cdi3", "CDI Programmable Multi", "CDI Program", "cdi",
        "Kurva bisa diatur dan limiter keras. Di batas rpm suara memotong-motong seperti gagap.",
        1_450_000, 0.35, "cdi", "adv" to 22.0, "advp" to 38.0, "lim" to "hard", "lrpm" to 11000, "mspark" to 0.7,
        "tags" to listOf("setting", "limiter")),
    p("cdi4", "CDI Limiter Rotasi", "CDI Rotasi", "cdi",
        "Limiter model rotasi: nyala-mati bergilir, suara berputar turun naik khas balap liar.",
        1_100_000, 0.3, "cdi", "adv" to 20.0, "advp" to 36.0, "lim" to "rotasi", "lrpm" to 9500, "mspark" to 0.5,
        "tags" to listOf("rotasi", "liar")),
    p("coil1", "Koil Pengapian Kuat", "Koil", "cdi",
        "Koil output besar. Percikan kuat, pembakaran lebih sempurna.",
        320_000, 0.4, "coil", "tags" to listOf("percikan")),
    p("plg1", "Busi Dingin", "Busi Dingin", "cdi",
        "Busi berulir pendek, cocok mesin panas dan kompresi tinggi.",
        90_000, 0.06, "plug", "tags" to listOf("busi")),
    p("plg2", "Busi Panas Iridium", "Busi Iridium", "cdi",
        "Elektroda iridium, percikan stabil di putaran tinggi.",
        220_000, 0.06, "plug", "tags" to listOf("busi", "balap")),

    /* ---------------- TRANSMISI ---------------- */
    p("gb4", "Girboks Manual 4 Speed", "Gigi 4", "trans",
        "Empat percepatan rapat. Oper gigi terasa jelas dengan jeda kopling pendek.",
        1_600_000, 12.0, "gearbox", "ratios" to doubleArrayOf(2.85, 1.85, 1.30, 1.00), "eff" to 0.92,
        "tags" to listOf("manual")),
    p("gb5", "Girboks Manual 5 Speed", "Gigi 5", "trans",
        "Lima percepatan. Gigi atas lebih panjang buat ngebut di jalan lurus.",
        2_300_000, 14.0, "gearbox", "ratios" to doubleArrayOf(2.92, 1.95, 1.45, 1.15, 0.92), "eff" to 0.93,
        "tags" to listOf("manual")),
    p("gb6", "Girboks Manual 6 Speed", "Gigi 6", "trans",
        "Enam percepatan rapat, cocok balap. Tiap oper gigi rpm nyaris tidak turun.",
        3_400_000, 16.0, "gearbox", "ratios" to doubleArrayOf(2.85, 2.05, 1.60, 1.30, 1.10, 0.94), "eff" to 0.94,
        "tags" to listOf("manual", "balap")),
    p("cvt1", "CVT Matik Standar", "CVT", "trans",
        "Transmisi otomatis sabuk. Putaran mesin dijaga di tenaga puncak, tanpa jeda oper gigi.",
        1_900_000, 13.0, "cvt", "cvt" to true, "rlow" to 2.70, "rtop" to 0.86, "eff" to 0.86,
        "tags" to listOf("matik")),
    p("cvt2", "CVT Racing Berat", "CVT Racing", "trans",
        "Roller berat, rasio lebih pendek. Tarikan awal menggigit.",
        2_600_000, 13.5, "cvt", "cvt" to true, "rlow" to 3.10, "rtop" to 0.95, "eff" to 0.85,
        "tags" to listOf("matik", "tarikan")),
    p("clt1", "Kopling Manual Basah", "Kopling", "trans",
        "Kopling basah multiplat. Bisa selip halus waktu mulai jalan.",
        1_100_000, 3.2, "clutch", "tags" to listOf("manual")),
    p("fd1", "Final Drive Pendek", "Gear Pendek", "trans",
        "Rasio akhir pendek: akselerasi kuat, kecepatan puncak turun.",
        850_000, 4.5, "finaldrive", "final" to 3.2, "tags" to listOf("akselerasi")),
    p("fd2", "Final Drive Panjang", "Gear Panjang", "trans",
        "Rasio akhir panjang: atas panjang, tapi tarikan awal lebih berat.",
        850_000, 4.5, "finaldrive", "final" to 2.4, "tags" to listOf("top speed")),
    p("chn1", "Rantai & Gir Standar", "Rantai", "trans",
        "Set rantai dan gir pabrikan.",
        450_000, 2.6, "chain", "tags" to listOf("penggerak")),

    /* ---------------- RODA, VELG, BAN ---------------- */
    p("ty1", "Ban Depan 80/90-14", "Ban Depan", "wheel",
        "Ban depan kecil, tapak halus, rolling ringan.",
        350_000, 3.2, "tyre", "wdia" to 0.46, "ww" to 0.080, "grip" to 0.95, "roll" to 1.0,
        "tags" to listOf("depan")),
    p("ty2", "Ban Belakang 100/80-14", "Ban Belakang", "wheel",
        "Ban belakang standar, grip cukup buat harian.",
        480_000, 4.4, "tyre", "wdia" to 0.48, "ww" to 0.100, "grip" to 1.0, "roll" to 1.0,
        "tags" to listOf("belakang")),
    p("ty3", "Ban Balap Slick", "Slick", "wheel",
        "Ban licin tanpa alur. Grip luar biasa, dengung selip ban berubah jadi desis pendek.",
        1_350_000, 5.0, "tyre", "wdia" to 0.50, "ww" to 0.160, "grip" to 1.45, "roll" to 0.94,
        "tags" to listOf("balap", "grip")),
    p("ty4", "Ban Trail Pacul", "Ban Trail", "wheel",
        "Kembang pacul dalam. Gigit di tanah dan kerikil, berdengung di aspal.",
        890_000, 5.8, "tyre", "wdia" to 0.53, "ww" to 0.120, "grip" to 1.15, "roll" to 0.88,
        "tags" to listOf("trail", "tanah")),
    p("ty5", "Ban Mobil 195/55", "Ban Mobil", "wheel",
        "Ban mobil berdiameter besar, cocok buat rakitan beroda empat.",
        1_100_000, 9.5, "tyre", "wdia" to 0.62, "ww" to 0.195, "grip" to 1.05, "roll" to 1.05,
        "tags" to listOf("mobil")),
    p("rm1", "Velg Jari-Jari 14", "Velg Jari", "wheel",
        "Velg jari-jari klasik. Ringan, sedikit fleksibel, tampang klasik.",
        750_000, 2.4, "rim", "wdia" to 0.356, "ww" to 0.046, "rim" to "spoke", "tags" to listOf("klasik")),
    p("rm2", "Velg Racing Palang 5", "Velg Racing", "wheel",
        "Velg palang lima ringan. Putaran ban lebih stabil di kecepatan tinggi.",
        1_450_000, 2.0, "rim", "wdia" to 0.356, "ww" to 0.056, "rim" to "racing", "tags" to listOf("sport")),
    p("rm3", "Velg Mobil 15 Inci", "Velg Mobil", "wheel",
        "Velg mobil 15 inci, dipakai empat biji buat rakitan mobil.",
        1_800_000, 7.5, "rim", "wdia" to 0.381, "ww" to 0.165, "rim" to "car", "tags" to listOf("mobil")),
    p("brk1", "Cakram Depan 260mm", "Cakram", "wheel",
        "Rem cakram depan. Pengereman pakem, bunyi gesekan halus saat berhenti.",
        890_000, 1.6, "disc", "brake" to 1.0, "tags" to listOf("rem")),

    /* ---------------- RANGKA & SUSPENSI ---------------- */
    p("fr1", "Rangka Underbone", "Underbone", "frame",
        "Rangka tulang punggung bebek. Ringan, sumbu roda pendek, lincah di kota.",
        2_100_000, 11.0, "frame", "ftype" to "moto", "str" to 1.0, "wb" to 1.24, "seath" to 0.76, "aero" to 1.0,
        "tags" to listOf("harian")),
    p("fr2", "Rangka Deltabox Sport", "Deltabox", "frame",
        "Rangka alumunium kaku. Sumbu roda panjang, stabil di kecepatan tinggi.",
        6_800_000, 14.0, "frame", "ftype" to "moto", "str" to 1.45, "wb" to 1.40, "seath" to 0.80, "aero" to 0.88,
        "tags" to listOf("sport", "kaku")),
    p("fr3", "Rangka Trail Tinggi", "Rangka Trail", "frame",
        "Ground clearance tinggi, sumbu roda panjang. Enak buat jalan rusak.",
        4_200_000, 15.5, "frameTrail", "ftype" to "moto", "str" to 1.25, "wb" to 1.44, "seath" to 0.88, "aero" to 1.06,
        "tags" to listOf("trail")),
    p("fr4", "Rangka Mobil Ladder", "Rangka Mobil", "frame",
        "Sasis tangga buat mobil. Butuh empat roda, lebih berat, sangat kaku.",
        12_500_000, 85.0, "frame", "gw" to 3, "ftype" to "car", "str" to 1.8, "wb" to 2.45, "seath" to 0.55, "aero" to 1.15,
        "tags" to listOf("mobil")),
    p("fr5", "Rangka Custom Las", "Rangka Custom", "frame",
        "Rangka hasil las sendiri di Bengkel Rangka. Bentuknya ikut desainmu.",
        3_500_000, 12.0, "frame", "ftype" to "moto", "str" to 1.1, "wb" to 1.32, "seath" to 0.78, "aero" to 0.95,
        "tags" to listOf("custom")),
    p("sa1", "Lengan Ayun Standar", "Swing Arm", "frame",
        "Lengan ayun kotak baja standar.",
        950_000, 4.2, "swingarm", "tags" to listOf("belakang")),
    p("sa2", "Lengan Ayun Alumunium", "Swing Arm Alu", "frame",
        "Lengan ayun alumunium: lebih ringan dan lebih kaku.",
        2_400_000, 2.8, "swingarm", "tags" to listOf("ringan")),
    p("fk1", "Sok Depan Teleskopik", "Sok Depan", "frame",
        "Garpu teleskopik standar, redaman cukup buat jalan kota.",
        1_250_000, 5.5, "fork", "tags" to listOf("depan")),
    p("fk2", "Sok Depan Upside Down", "Sok USD", "frame",
        "Garpu terbalik, lebih kaku. Handling tajam dan ban lebih gigit.",
        4_100_000, 5.0, "fork", "tags" to listOf("sport")),
    p("sh1", "Sok Belakang Tunggal", "Sok Belakang", "frame",
        "Sokbreker belakang tunggal, redaman bisa disetel.",
        780_000, 2.2, "shock", "tags" to listOf("belakang")),
    p("sh2", "Sok Belakang Ganda Tabung", "Sok Tabung", "frame",
        "Dua sokbreker dengan tabung, tampang klasik dan menahan beban.",
        1_150_000, 3.4, "shock", "tags" to listOf("klasik")),

    /* ---------------- BODY & PERLENGKAPAN ---------------- */
    p("st1", "Stang Standar", "Stang", "body",
        "Stang baja model pabrikan, posisi duduk tegak.",
        320_000, 1.2, "bar", "tags" to listOf("kemudi")),
    p("st2", "Stang Jepit Balap", "Stang Jepit", "body",
        "Stang jepit rendah. Posisi membungkuk, aerodinamika membaik.",
        780_000, 0.9, "bar", "aero" to 0.9, "tags" to listOf("balap")),
    p("st3", "Stang Trail Lebar", "Stang Trail", "body",
        "Stang lebar buat kendali di jalan rusak.",
        420_000, 1.3, "bar", "tags" to listOf("trail")),
    p("sk1", "Jok Standar", "Jok", "body",
        "Jok busa standar buat satu atau dua penumpang.",
        450_000, 2.4, "seat", "tags" to listOf("duduk")),
    p("sk2", "Jok Balap Tipis", "Jok Balap", "body",
        "Jok tipis ringan, posisi duduk lebih rendah dan aerodinamis.",
        680_000, 1.1, "seat", "aero" to 0.94, "tags" to listOf("balap")),
    p("fp1", "Spakbor Depan", "Spakbor", "body",
        "Spakbor depan menahan cipratan air dan kerikil.",
        180_000, 0.8, "fender", "tags" to listOf("body")),
    p("lp1", "Lampu Depan", "Lampu", "body",
        "Lampu utama, wajib kalau main di malam hari.",
        260_000, 0.7, "light", "tags" to listOf("lampu")),
    p("fr9", "Fairing Setengah", "Fairing", "body",
        "Fairing setengah menutup mesin. Angin lebih rapi, hambatan udara turun.",
        1_650_000, 3.6, "fairing", "aero" to 0.82, "tags" to listOf("aero")),
    p("fp2", "Pijakan Kaki Lipat", "Footstep", "body",
        "Pijakan kaki lipat ringan buat gaya balap.",
        220_000, 0.5, "footpeg", "tags" to listOf("body")),
    p("bt1", "Aki 12V", "Aki", "body",
        "Aki 12 volt buat starter dan kelistrikan.",
        420_000, 3.2, "battery", "tags" to listOf("listrik")),
    p("rd1", "Radiator Pendingin", "Radiator", "body",
        "Radiator air. Mesin lebih adem, suara kipas muncul saat panas.",
        1_250_000, 4.5, "radiator", "tags" to listOf("pendingin")),
    p("nos1", "Sistem Nitrous", "Nitrous", "body",
        "Semprotan nitrous oksida. Tambahan tenaga seketika, suara mendesis saat disemprot.",
        3_800_000, 6.0, "nos", "boost" to 1.4, "idrive" to "nitrous", "tags" to listOf("nitrous", "liar"))
)

val PART: Map<String, Part> = PART_LIST.associateBy { it.id }

fun partsByCat(cat: String): List<Part> = PART_LIST.filter { it.cat == cat }
fun part(id: String): Part? = PART[id]
