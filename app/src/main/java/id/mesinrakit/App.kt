package id.mesinrakit

import android.content.Context
import android.graphics.Canvas
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import id.mesinrakit.audio.AudioEngine
import id.mesinrakit.core.*
import id.mesinrakit.data.*
import id.mesinrakit.data.Pack
import id.mesinrakit.model.*
import id.mesinrakit.scene.*
import id.mesinrakit.ui.Scene
import id.mesinrakit.ui.Toast
import kotlin.math.*

/* ============================================================
   Inti aplikasi: nyimpen rakitan, pindah layar, simpan/muat,
   dan nyambungkan suara dengan keadaan terakhir.
   ============================================================ */
class App(val ctx: Context, val view: GameView) {
    var build = Build()
    var spec = Spec()
    val audio = AudioEngine()
    val toast = Toast()

    var mapId = "kota"
    var map = GameMap(MAP_BY_ID[mapId] ?: MAPS[0])
    var vehicle: Vehicle? = null

    var gas = 0.0
    var rem = 0.0
    var kopling = 0.0

    lateinit var scene: Scene
    private val scenes = HashMap<String, Scene>()

    /* dipakai layar jalan: status terakhir buat suara */
    var lastRPM = 1000.0
    var lastThr = 0.0

    fun layar(nama: String): Scene = scenes.getOrPut(nama) {
        when (nama) {
            "menu" -> MenuScene(this)
            "bengkel" -> BengkelScene(this)
            "rangka" -> RangkaScene(this)
            "dyno" -> DynoScene(this)
            "pit" -> PitScene(this)
            "peta" -> PetaScene(this)
            "jalan" -> JalanScene(this)
            else -> MenuScene(this)
        }
    }

    fun boot() {
        /* layar disiapkan paling awal: kalau ada yang gagal setelah ini,
           pemain tetap lihat menu dan pesan error, bukan layar gelap. */
        scene = layar("menu")
        try {
            muat()
        } catch (e: Exception) {
            catat(e)
            try { pasangPreset(build, id.mesinrakit.data.PRESETS[0]) } catch (x: Exception) { }
        }
        try { bangunUlang() } catch (e: Exception) { catat(e) }
        pindah("menu")
    }

    fun bangunUlang() {
        spec = deriveSpec(build)
        spec.karakter()
        audio.setSpec(spec)
        vehicle?.let { v ->
            v.rebuild(spec, build, map)
        }
    }

    fun pindah(nama: String) {
        if (::scene.isInitialized && scenes.values.contains(scene)) scene.leave()
        audio.update(0.016, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, true)
        /* audio cuma hidup di dyno dan jalan. Di layar lain dimatikan supaya
           gangguan di perangkat tertentu gak bikin aplikasi berhenti. */
        if (nama != "dyno" && nama != "jalan") {
            try { audio.stop() } catch (e: Exception) { }
        }
        scene = layar(nama)
        scene.enter()
    }

    /** nyalakan audio dengan aman. Gagal pun aplikasi tetap jalan. */
    fun mulaiAudio() {
        if (audio.ready) return
        try {
            if (!audio.start()) toast.tampil("Audio gagal dibuka, game tetap jalan", C.AMBER)
        } catch (e: Exception) {
            try { toast.tampil("Audio gagal dibuka, game tetap jalan", C.AMBER) } catch (x: Exception) { }
            catat(e)
        }
    }

    fun gantiMap(id: String) {
        mapId = id
        map = GameMap(MAP_BY_ID[id] ?: MAPS[0])
        vehicle = null
    }

    fun mulaiJalan() {
        if (!spec.valid) {
            toast.tampil("Rakitan belum lengkap: ${spec.missing.take(2).joinToString(", ")}", C.RED)
            return
        }
        mulaiAudio()
        val v = Vehicle(spec, build, map)
        v.onShift = { audio.clunk() }
        vehicle = v
        pindah("jalan")
    }

    fun update(dt: Float) {
        scene.update(dt)
        toast.update(dt)
        val v = vehicle
        if (v != null && scene is JalanScene) {
            v.step(dt.toDouble(), gas, rem)
            val rep = damageReport(build)
            audio.update(dt.toDouble(), v.rpm, v.throttle, v.load, v.boost,
                v.kmh, abs(v.wheels.last().slip), v.airTime, v.cut, v.mogok)
            lastRPM = v.rpm
            lastThr = v.throttle
            if (v.crashT > 0 && v.crash != null && v.crash!!.power > 0.05) {
                audio.crash(v.crash!!.power)
                v.crash = null
            }
        }
    }

    fun draw(c: Canvas, w: Float, h: Float) {
        scene.begin()
        scene.draw(c, w, h)
        toast.draw(c, w, h)
    }

    fun sentuh(h: id.mesinrakit.ui.Hot?, x: Float, y: Float) {
        if (h != null) { scene.pressed.add(h.id); scene.press(h, x, y) } else scene.press(null, x, y)
    }
    fun lepas(h: id.mesinrakit.ui.Hot?) {
        if (h != null) { scene.pressed.remove(h.id); scene.release(h) } else scene.release(null)
    }

    fun kembali(): Boolean = scene.tombolKembali()

    /* ---------------- penanganan error ---------------- */
    var errorTeks: String? = null
    private val errorSudah = HashSet<String>()

    /** catat error supaya bisa dibaca pemain, bukan cuma bikin aplikasi mati */
    fun catat(e: Throwable) {
        try {
            val kunci = e.toString().take(120)
            if (errorSudah.contains(kunci)) return
            errorSudah.add(kunci)
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            val isi = infoPerangkat() + "\n" + sw.toString().take(2500)
            errorTeks = isi
            simpanLog(isi)
        } catch (x: Exception) { }
    }

    /** keterangan perangkat, biar gampang menebak penyebabnya */
    fun infoPerangkat(): String = try {
        "Perangkat : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
        "Android   : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n" +
        "Layar     : ${android.os.Build.PRODUCT}\n" +
        "Versi app : ${ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName}\n" +
        "Waktu     : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}"
    } catch (e: Exception) { "Perangkat : (tidak diketahui)" }

    fun simpanLog(isi: String) {
        /* penyimpanan internal paling penting: selalu bisa ditulis dan
           bisa dibaca lagi oleh layar laporan yang jalan di proses lain. */
        try {
            ctx.openFileOutput(LaporActivity.NAMA_BERKAS, Context.MODE_PRIVATE).use {
                it.write(isi.toByteArray())
            }
        } catch (e: Exception) { }
        try {
            ctx.getSharedPreferences("mesinrakit", Context.MODE_PRIVATE)
                .edit().putString("crash", isi).apply()
        } catch (e: Exception) { }
        try {
            val dir = ctx.getExternalFilesDir(null)
            if (dir != null) java.io.File(dir, "crash-mesin-rakit.txt").writeText(isi)
        } catch (e: Exception) { }
        /* salinan di folder Unduhan supaya pemain bisa buka dan kirim
           sendiri, sekalipun layar laporan di aplikasi tidak muncul. */
        Lapor.tulis(ctx, isi)
    }

    fun muatLog(): String? = try {
        ctx.getSharedPreferences("mesinrakit", Context.MODE_PRIVATE).getString("crash", null)
    } catch (e: Exception) { null }

    fun hapusLog() {
        errorTeks = null
        try { ctx.deleteFile(LaporActivity.NAMA_BERKAS) } catch (e: Exception) { }
        try {
            ctx.getSharedPreferences("mesinrakit", Context.MODE_PRIVATE).edit().remove("crash").apply()
            val dir = ctx.getExternalFilesDir(null)
            if (dir != null) {
                val f = java.io.File(dir, "crash-mesin-rakit.txt")
                if (f.exists()) f.delete()
            }
        } catch (e: Exception) { }
    }

    fun getar(ms: Long = 30) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (android.os.Build.VERSION.SDK_INT >= 26) {
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms)
            }
        } catch (e: Exception) { }
    }

    /* ---------------- bengkel ---------------- */
    fun servis(penuh: Boolean) {
        val biaya = if (penuh) servisCost(build) else servisDarurat(build)
        if (biaya == 0) { toast.tampil("Tidak ada yang rusak", C.DIM); return }
        if (build.money < biaya) { toast.tampil("Uang kurang: ${rupiah(biaya - build.money)}", C.RED); return }
        build.money -= biaya
        if (penuh) servis(build) else servisSebagian(build)
        bangunUlang()
        simpan()
        toast.tampil(if (penuh) "Servis penuh selesai" else "Servis darurat 45 persen", C.GREEN)
    }

    fun pakaiPreset(ps: Preset) {
        pasangPreset(build, ps)
        build.frame = null
        bangunUlang()
        simpan()
        toast.tampil("Preset ${ps.name} dipasang", C.ACC)
    }

    /* ---------------- simpan & muat ---------------- */
    fun simpan() {
        val p = ctx.getSharedPreferences("mesinrakit", Context.MODE_PRIVATE)
        val sb = StringBuilder()
        sb.append("v=1\n")
        sb.append("items=").append(build.items.entries.joinToString(";") { "${it.key}:${it.value}" }).append("\n")
        sb.append("paint=").append(build.paint.entries.joinToString(";") { "${it.key}:${it.value}" }).append("\n")
        sb.append("dmg=").append(build.dmg.entries.joinToString(";") { "${it.key}:${(it.value * 100).toInt()}" }).append("\n")
        sb.append("color=${build.colorIdx}\n")
        sb.append("driver=${build.driverId}\n")
        sb.append("money=${build.money}\n")
        sb.append("name=${build.name}\n")
        sb.append("map=$mapId\n")
        val f = build.frame
        if (f != null) {
            sb.append("fnodes=").append(f.nodes.joinToString(";") { "${it.x}:${it.y}:${it.bow}:${it.kind}" }).append("\n")
            sb.append("ftubes=").append(f.tubes.joinToString(";") { "${it.a}:${it.b}:${it.dia}:${it.thick}:${it.mat}" }).append("\n")
            sb.append("fmat=${f.material}\n")
        }
        p.edit().putString("save", sb.toString()).apply()
    }

    fun muat() {
        val p = ctx.getSharedPreferences("mesinrakit", Context.MODE_PRIVATE)
        val raw = p.getString("save", null) ?: run { pakaiPreset(PRESETS[0]); return }
        try {
            val m = HashMap<String, String>()
            for (line in raw.split('\n')) {
                val i = line.indexOf('=')
                if (i > 0) m[line.substring(0, i)] = line.substring(i + 1)
            }
            build = Build()
            m["items"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
                val a = it.split(':'); if (a.size == 3) build.items["${a[0]},${a[1]}"] = a[2]
            }
            m["paint"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
                val a = it.split(':'); if (a.size == 3) build.paint["${a[0]},${a[1]}"] = a[2].toInt()
            }
            m["dmg"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
                val a = it.split(':'); if (a.size == 3) build.dmg["${a[0]},${a[1]}"] = a[2].toInt() / 100.0
            }
            build.colorIdx = m["color"]?.toIntOrNull() ?: 0
            build.driverId = m["driver"] ?: "budi"
            build.money = m["money"]?.toIntOrNull() ?: 1_500_000
            build.name = m["name"] ?: "Rakitanku"
            val f = FrameDesign()
            m["fnodes"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
                val a = it.split(':')
                if (a.size >= 4) f.nodes.add(Node(a[0].toDouble(), a[1].toDouble(), a[2].toDouble(), a[3]))
            }
            m["ftubes"]?.takeIf { it.isNotEmpty() }?.split(';')?.forEach {
                val a = it.split(':')
                if (a.size >= 5) f.tubes.add(Tube(a[0].toInt(), a[1].toInt(), a[2].toDouble(), a[3].toDouble(), a[4]))
            }
            f.material = m["fmat"] ?: "besi"
            if (f.nodes.isNotEmpty()) build.frame = f
            val mid = m["map"] ?: "kota"
            if (MAP_BY_ID.containsKey(mid)) { mapId = mid; map = GameMap(MAP_BY_ID[mid]!!) }
            if (build.items.isEmpty()) pakaiPreset(PRESETS[0])
        } catch (e: Exception) {
            pakaiPreset(PRESETS[0])
        }
    }

    /** kode bagikan: ringkasan rakitan yang bisa ditempel */
    fun kodeBagikan(): String {
        val sb = StringBuilder()
        sb.append("MR1|").append(build.name.replace('|', ' ')).append('|')
        sb.append(build.items.values.joinToString(","))
        return android.util.Base64.encodeToString(sb.toString().toByteArray(), android.util.Base64.NO_WRAP)
    }

    /** salin desain lengkap (MRPACK1) ke clipboard */
    fun salinPack() {
        try {
            val teks = Pack.encode(build, "pack")
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("mesin rakit", teks))
            toast.tampil("Kode MRPACK1 tersalin", C.ACC)
        } catch (e: Exception) {
            toast.tampil("Gagal menyalin kode", C.RED)
        }
    }

    /** tempel MRPACK1 dari clipboard, langsung dipakai */
    fun tempelPack(): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val teks = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: ""
            val data = Pack.decode(teks)
            if (data == null) {
                toast.tampil("Clipboard bukan kode MRPACK1", C.RED)
                false
            } else {
                Pack.terapkan(build, data)
                bangunUlang()
                simpan()
                toast.tampil(
                    if (data.kind == "part") "Usulan part \"${data.name}\" dipasang sebagai cat body"
                    else "Desain \"${data.name}\" dipasang",
                    if (data.kind == "part") C.AMBER else C.GREEN)
                true
            }
        } catch (e: Exception) {
            toast.tampil("Gagal menempel kode", C.RED)
            false
        }
    }
}
