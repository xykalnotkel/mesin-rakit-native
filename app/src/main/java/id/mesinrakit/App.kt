package id.mesinrakit

import android.content.Context
import android.graphics.Canvas
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import id.mesinrakit.audio.AudioEngine
import id.mesinrakit.core.*
import id.mesinrakit.data.*
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
        muat()
        bangunUlang()
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
        scene = layar(nama)
        scene.enter()
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
}
