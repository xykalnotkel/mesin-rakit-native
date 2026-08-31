package id.mesinrakit.audio

import id.mesinrakit.core.*
import id.mesinrakit.model.Spec
import kotlin.math.*

/* ============================================================
   Mesin suara native. Tidak ada berkas audio: semuanya
   disintesis dari spesifikasi rakitan.
   - Satu siklus mesin dirender di rpm acuan, lalu diputar
     dengan kecepatan yang mengikuti rpm (resampling).
   - Lapisan hidup (turbo, isapan, angin, selip, ketukan)
     dihitung langsung per sampel di thread audio.
   ============================================================ */
class AudioEngine {
    private var track: android.media.AudioTrack? = null
    private var thread: Thread? = null
    @Volatile var running = false
    @Volatile var ready = false
    var sr = 48000
    private var srD = 48000.0
    private val chunk = 512

    /* siklus yang dirender */
    private var cycLoad = FloatArray(1)
    private var cycIdle = FloatArray(1)
    private var cycN = 1
    private val refRPM = 900.0
    private var pos = 0.0

    /* parameter rakitan */
    private var spec: Spec? = null
    private var fRes = 180.0
    private var partials = doubleArrayOf(1.0, 0.55, 0.32, 0.18)
    private var decayBase = 0.05
    private var noiseMix = 0.25
    private var amHz = 0.0
    private var amDepth = 0.0
    private var ringHz = 1800.0
    private var ringAmt = 0.0
    private var boomHz = 78.0
    private var boomAmt = 0.0
    private var popBias = 0.25
    private var muffling = 0.6
    private var lope = 0.1
    private var knock = 0.0
    private var leak = 0.0
    private var camProfile = 0.3
    private var cyl = 1
    private var rotary = false
    private var twoStroke = false
    private var diesel = false
    private var boostMax = 0.0
    private var idleRPM = 1200.0
    private var peakRPM = 6000.0
    private var dispCc = 125.0

    /* status dari game (diisi dari thread UI) */
    @Volatile var gRPM = 1000.0
    @Volatile var gThrottle = 0.0
    @Volatile var gLoad = 0.0
    @Volatile var gBoost = 0.0
    @Volatile var gSpeed = 0.0
    @Volatile var gSlip = 0.0
    @Volatile var gAir = 0.0
    @Volatile var gCut = false
    @Volatile var gMogok = false
    @Volatile var master = 0.85f

    /* kejadian suara */
    private var popTimer = 0.0
    private var popLeft = 0
    private var popPow = 0.0
    private var clunkT = 0.0
    private var crashT = 0.0
    private var crashPow = 0.0
    private var lastThrottle = 0.0
    private var blowT = 0.0
    private var knockTimer = 0.0

    /* filter & osilator */
    private val fIntake = Biquad()
    private val fIntake2 = Biquad()
    private val fWind = Biquad()
    private val fWind2 = Biquad()
    private val fMech = Biquad()
    private val fLeak = Biquad()
    private val fSlip = Biquad()
    private val fPop = Biquad()
    private val fPop2 = Biquad()
    private val fTurbo = Biquad()

    private val waveBuf = FloatArray(256)
    private val waveLock = Any()

    /* ---------------- hidupkan ---------------- */
    fun start(): Boolean {
        if (running) return true
        val native = android.media.AudioTrack.getNativeOutputSampleRate(android.media.AudioFormat.CHANNEL_OUT_STEREO)
        sr = if (native > 0) native else 48000
        srD = sr.toDouble()
        val buf = android.media.AudioTrack.getMinBufferSize(
            sr, android.media.AudioFormat.CHANNEL_OUT_STEREO, android.media.AudioFormat.ENCODING_PCM_FLOAT)
        val t = android.media.AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setSampleRate(sr)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_FLOAT)
                    .build())
            .setBufferSizeInBytes(max(buf, sr / 5 * 8))
            .setTransferMode(android.media.AudioTrack.MODE_STREAM)
            .build()
        track = t
        running = true
        ready = true
        t.play()
        thread = Thread { loop() }.apply { name = "mesin-audio"; priority = Thread.MAX_PRIORITY; start() }
        return true
    }

    fun stop() {
        running = false
        try { thread?.join(400) } catch (e: Exception) {}
        thread = null
        try { track?.stop(); track?.release() } catch (e: Exception) {}
        track = null
        ready = false
    }

    fun pause() { try { track?.pause() } catch (e: Exception) {} }
    fun resume() { try { track?.play() } catch (e: Exception) {} }

    /* ---------------- pasang spesifikasi baru ---------------- */
    fun setSpec(s: Spec) {
        spec = s
        cyl = s.cyl
        rotary = s.rotary
        twoStroke = s.twoStroke
        diesel = s.fuelType == "solar"
        lope = s.camLope
        knock = s.knock
        leak = s.leak
        camProfile = s.camProfile
        boostMax = s.boostMax
        idleRPM = s.idleRPM
        peakRPM = s.peakRPM
        dispCc = s.dispCc
        muffling = s.muffling
        val e = s.exh
        partials = e.partials
        noiseMix = e.noise
        amHz = e.amHz
        amDepth = e.amDepth
        ringHz = e.ringHz
        ringAmt = e.ring
        boomHz = e.boomHz
        boomAmt = e.boom
        popBias = e.pop
        /* nada dasar dari panjang pipa, diameter, dan isi silinder */
        val diaFac = sqrt(0.05 / max(0.02, s.exhDia))
        fRes = (190.0 / max(0.35, s.exhLen * diaFac)) * e.f0 * (125.0 / max(40.0, dispCc / max(1, cyl))).pow(0.16)
        decayBase = 0.030 * e.decay * (1.0 + muffling * 0.4)
        renderCycles()
    }

    /* ---------------- render dua siklus: beban dan stasioner ---------------- */
    private fun renderCycles() {
        val s = spec ?: return
        val cycleDeg = s.cycleDeg
        val cycleTime = (cycleDeg / 360.0) * (60.0 / refRPM)
        val n = max(256, (cycleTime * sr).roundToInt())
        cycN = n
        cycLoad = renderCycle(s.firing, n, false)
        cycIdle = renderCycle(s.firing, n, true)
    }

    private fun renderCycle(firing: DoubleArray, n: Int, idle: Boolean): FloatArray {
        val out = FloatArray(n)
        val amp = if (idle) 0.42 else 1.0
        val rnd = Rnd(if (idle) 991 else 137)
        for (a in firing) {
            val idx = ((a / (spec?.cycleDeg ?: 720.0)) * n).roundToInt()
            val jitter = if (idle) (rnd.next() - 0.5) * lope * 0.045 * n else 0.0
            val start = ((idx + jitter).roundToInt() % n + n) % n
            val gain = amp * (if (idle) 0.72 + lope * 0.55 * rnd.next() else 1.0)
            bang(out, start, n, gain, rnd, idle)
        }
        /* rapikan tepi supaya loop tidak berbunyi klik */
        val fade = (n * 0.02).toInt().coerceAtLeast(8)
        for (i in 0 until fade) {
            val g = i.toFloat() / fade
            out[n - 1 - i] *= g
        }
        return out
    }

    /** satu ledakan: deretan partial, desis, plus gendang atau dengung logam */
    private fun bang(out: FloatArray, start: Int, n: Int, gain: Double, rnd: Rnd, idle: Boolean) {
        val len = (n * if (idle) 0.55 else 0.85).toInt()
        val noiseAmp = noiseMix * (if (idle) 0.7 else 1.0) * (1.0 - muffling * 0.45)
        for (i in 0 until len) {
            val k = (start + i) % n
            val t = i.toDouble() / sr
            var v = 0.0
            for (pi in partials.indices) {
                val f = fRes * (pi + 1) * (if (pi > 0) (1.0 + pi * 0.03) else 1.0)
                if (f > sr * 0.45) break
                val tau = decayBase / (1 + pi * 0.85)
                v += sin(TAU * f * t) * partials[pi] * exp(-t / max(0.004, tau))
            }
            /* desis ledakan */
            val nt = exp(-t / max(0.003, decayBase * 0.22))
            v += (rnd.next() * 2 - 1) * noiseAmp * nt * 0.55
            /* gendang (drumben) */
            if (boomAmt > 0.01) {
                val bt = exp(-t / max(0.02, decayBase * 2.6))
                v += sin(TAU * boomHz * t) * boomAmt * bt * 0.9
                v += sin(TAU * boomHz * 1.62 * t) * boomAmt * bt * 0.35
            }
            /* dengung logam (thai) */
            if (ringAmt > 0.01) {
                val rt = exp(-t / max(0.05, decayBase * 6.0))
                v += sin(TAU * ringHz * t) * ringAmt * rt * 0.30
                v += sin(TAU * ringHz * 1.51 * t) * ringAmt * rt * 0.16
            }
            /* modulasi bilah (helikopter) */
            if (amHz > 0.01) {
                val m = 1.0 - amDepth * 0.5 * (1.0 - cos(TAU * amHz * (t * (refRPM / 900.0)))) / 2.0 * 2.0
                v *= clamp(m, 0.05, 1.0)
            }
            /* karakter mesin */
            if (diesel) {
                val ct = exp(-t / 0.012)
                v += (rnd.next() * 2 - 1) * 0.55 * ct
                v += sin(TAU * fRes * 4.2 * t) * 0.30 * ct
            }
            if (rotary) {
                val pt = exp(-t / 0.010)
                v += (rnd.next() * 2 - 1) * 0.30 * pt          // desis port
                v += sin(TAU * fRes * 6.1 * t) * 0.22 * pt      // siulan port
                v *= 0.82
            }
            if (twoStroke) v *= 1.12
            out[k] += (v * gain * 0.34).toFloat()
        }
    }

    /* ---------------- kejadian ---------------- */
    fun pop(power: Double) { popPow = clamp(power, 0.2, 1.0); popLeft = (1 + RND.next() * 3).toInt(); popTimer = 0.02 }
    fun clunk() { clunkT = 0.09 }
    fun blowoff(power: Double) { blowT = 0.22 * clamp(power, 0.3, 1.0) }
    fun crash(power: Double) { crashPow = clamp(power, 0.2, 1.0); crashT = 0.35 }

    /* dipanggil tiap frame dari thread UI */
    fun update(dt: Double, rpm: Double, throttle: Double, load: Double, boost: Double,
               speed: Double, slip: Double, air: Double, cut: Boolean, mogok: Boolean) {
        gRPM = rpm; gThrottle = throttle; gLoad = load; gBoost = boost
        gSpeed = speed; gSlip = slip; gAir = air; gCut = cut; gMogok = mogok
        /* gas ditutup di putaran tinggi: jadwalkan letupan */
        if (lastThrottle > 0.30 && throttle < 0.06 && rpm > idleRPM * 1.8) {
            pop(0.45 + popBias * 0.55)
            if (boostMax > 0) blowoff(0.6)
        }
        lastThrottle = throttle
    }

    fun wave(): FloatArray = synchronized(waveLock) { waveBuf.copyOf() }

    /* ---------------- thread audio ---------------- */
    private fun loop() {
        val out = FloatArray(chunk * 2)
        val mono = FloatArray(chunk)
        var phaseT = 0.0
        var phaseM = 0.0
        var t = 0.0
        while (running) {
            val dt = chunk.toDouble() / sr
            val rpm = gRPM
            val thr = gThrottle
            val load = gLoad
            val boost = gBoost
            val spd = abs(gSpeed)
            val idleMix = clamp(1.0 - thr * 2.2, 0.0, 1.0)
            val rate = if (gMogok) 0.0 else (rpm / refRPM).coerceAtMost(40.0)
            val cut = gCut

            /* filter hidup mengikuti rpm */
            val rpmN = clamp(rpm / max(1000.0, peakRPM), 0.0, 1.6)
            fIntake.bandpass(srD, 320.0 + 900.0 * rpmN, 1.1)
            fIntake2.bandpass(srD, 1250.0 + 1800.0 * rpmN, 2.2)
            fWind.lowpass(srD, 700.0 + spd * 12.0, 0.7)
            fWind2.highpass(srD, 260.0, 0.7)
            fMech.peaking(srD, 220.0 + 340.0 * rpmN, 3.0, 6.0 + camProfile * 10.0)
            fLeak.highpass(srD, 2600.0, 0.8)
            fSlip.bandpass(srD, 1500.0 + spd * 8.0, 1.6)
            fPop.bandpass(srD, fRes * 1.6, 1.4)
            fPop2.lowpass(srD, 420.0, 0.9)
            fTurbo.bandpass(srD, 1800.0 + 4200.0 * rpmN, 6.0)

            for (i in 0 until chunk) {
                /* --- putar siklus dengan resampling --- */
                var s = 0.0
                if (rate > 0.01 && !cut && !gMogok) {
                    val p = pos
                    val i0 = floor(p).toInt()
                    val f = p - i0
                    val a = cycLoad[i0 % cycN]
                    val b = cycLoad[(i0 + 1) % cycN]
                    val c = cycIdle[i0 % cycN]
                    val d = cycIdle[(i0 + 1) % cycN]
                    val lo = a + (b - a) * f
                    val id = c + (d - c) * f
                    s = lo * (1 - idleMix) + id * idleMix
                    pos += rate
                    while (pos >= cycN) pos -= cycN
                } else if (!gMogok) {
                    /* limiter / mogok: sisa dengung pendek */
                    pos += rate
                    while (pos >= cycN) pos -= cycN
                }

                /* --- isapan udara --- */
                val inAmp = if (gMogok) 0.0 else (0.012 + 0.075 * thr * (0.3 + rpmN)) * (1 + boost * 0.5)
                val nz = RND.next() * 2 - 1
                s += fIntake.process(nz) * inAmp
                s += fIntake2.process(nz) * inAmp * 0.42

                /* --- siulan turbo --- */
                if (boost > 0.01 && !gMogok) {
                    val fq = (1800.0 + 4200.0 * rpmN) * (0.8 + 0.4 * boost)
                    phaseT += TAU * fq / sr
                    s += sin(phaseT) * 0.030 * boost
                    s += fTurbo.process(nz) * 0.020 * boost
                }
                /* --- blow-off --- */
                if (blowT > 0) {
                    val b = blowT / 0.22
                    s += fTurbo.process(nz) * 0.16 * b * b
                    blowT -= 1.0 / sr
                }

                /* --- mekanik mesin --- */
                if (!gMogok) {
                    val mAmp = 0.010 + 0.020 * rpmN + knock * 0.03
                    s += fMech.process(nz) * mAmp
                    /* ketukan saat kompresi terlalu tinggi */
                    knockTimer -= 1.0 / sr
                    if (knock > 0.25 && knockTimer <= 0.0) {
                        knockTimer = 0.10 + RND.next() * 0.30 / max(0.2, knock)
                        s += sin(TAU * 3400.0 * (i.toDouble() / sr)) * 0.05 * knock
                    }
                }

                /* --- angin & ban --- */
                if (spd > 1.0) s += fWind2.process(fWind.process(nz)) * min(0.05, spd / 2600.0)
                if (gSlip > 0.18 && gAir < 0.05) s += fSlip.process(nz) * 0.06 * min(1.0, gSlip)

                /* --- letupan saat gas ditutup --- */
                if (popLeft > 0) {
                    popTimer -= 1.0 / sr
                    if (popTimer <= 0) {
                        popLeft--
                        popTimer = 0.05 + RND.next() * 0.20
                        popPhase = 0.0
                        popEnv = 1.0
                        popF = fRes * (1.1 + RND.next() * 1.4)
                    }
                }
                if (popEnv > 0.001) {
                    popPhase += TAU * popF / sr
                    popEnv *= exp(-1.0 / (sr * 0.045))
                    s += (sin(popPhase) * 0.28 + fPop2.process(nz) * 0.20) * popEnv * popPow
                }

                /* --- bunyi oper gigi --- */
                if (clunkT > 0) {
                    val b = clunkT / 0.09
                    s += sin(TAU * 150.0 * (i.toDouble() / sr)) * 0.16 * b
                    s += nz * 0.06 * b
                    clunkT -= 1.0 / sr
                }

                /* --- tabrakan --- */
                if (crashT > 0) {
                    val b = crashT / 0.35
                    s += fPop2.process(nz) * 0.30 * b * crashPow
                    s += sin(TAU * 90.0 * (i.toDouble() / sr)) * 0.22 * b * crashPow
                    crashT -= 1.0 / sr
                }

                /* --- kebocoran & kerusakan --- */
                if (leak > 0.02) s += fLeak.process(nz) * 0.02 * leak

                /* --- lembutkan dengan limiter --- */
                s *= master
                val v = tanh(s * 1.25).toFloat()
                mono[i] = v
                out[i * 2] = v
                out[i * 2 + 1] = v
                t += 1.0 / sr
            }
            try { track?.write(out, 0, out.size, android.media.AudioTrack.WRITE_BLOCKING) } catch (e: Exception) { }
            synchronized(waveLock) {
                val stepSize = chunk / waveBuf.size
                for (i in waveBuf.indices) waveBuf[i] = mono[i * stepSize]
            }
        }
    }

    private var popPhase = 0.0
    private var popEnv = 0.0
    private var popF = 300.0
}
