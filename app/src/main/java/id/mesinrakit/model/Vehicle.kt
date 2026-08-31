package id.mesinrakit.model

import id.mesinrakit.core.*
import id.mesinrakit.data.DRIVER_BY_ID
import kotlin.math.*

/* ============================================================
   Fisika kendaraan 2D: suspensi per roda, gaya ban sebagai
   constraint, rigid body, tabrakan, kopling, dan gigi manual.
   ============================================================ */
class Wheel(val ox: Double, val oy: Double, val driven: Boolean) {
    var L = 0.30
    var prevComp = 0.0
    var spin = 0.0
    var spinAng = 0.0
    var contact = 0
    var slip = 0.0
    var N = 0.0
    var cx = 0.0
    var cy = 0.0
}

class Vehicle(var spec: Spec, var build: Build, var map: GameMap) {
    val isCar get() = spec.isCar
    val mu: Double
    val M: Double
    val I: Double
    val halfL: Double
    val halfH: Double
    var r: Double
    val Iw: Double
    val finalDrive: Double

    var posX = 8.0
    var posY = 0.0
    var velX = 0.0
    var velY = 0.0
    var ang = 0.0
    var angVel = 0.0

    lateinit var wheels: Array<Wheel>
    val minL = 0.16
    val maxL = 0.44
    val k: Double
    val damp: Double

    /* mesin */
    var gear = 0
    var rpm = 1200.0
    var boost = 0.0
    var cvtRatio = 2.7
    var shiftT = 0.0
    var cut = false
    var manual = false
    var throttle = 0.0
    var brakeIn = 0.0
    val st = EngineState()

    /* status jalan */
    var distance = 0.0
    var maxSpeed = 0.0
    var airTime = 0.0
    var flipT = 0.0
    var wheelieT = 0.0
    var mogok = false
    var money = 0.0
    var lastX = 8.0
    var wobble = 0.0
    var finished = false
    var finishTime = 0.0
    var time = 0.0
    var crash: CrashFx? = null
    var crashT = 0.0
    var clutchSlip = 0.0
    var engineBrake = 0.0
    var load = 0.0
    var onShift: ((dir: Int) -> Unit)? = null
    var lastShiftAt = -1.0

    data class CrashFx(val x: Double, val y: Double, val power: Double, val type: String, val part: String)

    val kmh get() = velX * 3.6
    val speed get() = hypot(velX, velY)

    init {
        val s = spec
        val extra = if (s.nWheels >= 4) 1.25 else if (s.nWheels == 3) 1.12 else 1.0
        mu = 0.95 * s.grip * extra
        M = s.mass
        val L = s.wheelbase * 1.25
        val H = max(0.5, s.frameH)
        I = M * (L * L + H * H) / 12.0
        halfL = s.wheelbase * 0.55
        halfH = H * 0.5
        r = s.wheelR
        val wheelMass = max(6.0, 10.0 * (r / 0.29))
        Iw = 0.5 * wheelMass * r * r
        finalDrive = if (isCar) 3.0 else 3.6
        k = (M * GRAV / 2) / ((maxL - minL) * 0.45)
        damp = 2 * 0.42 * sqrt(k * M / 2)
        buildWheels()
        reset(8.0)
    }

    private fun buildWheels() {
        val s = spec
        if (isCar) {
            val wb = s.wheelbase * 0.5
            wheels = arrayOf(
                Wheel(wb, -0.06, false), Wheel(wb * 0.05, -0.06, false),
                Wheel(-wb, -0.06, true), Wheel(-wb * 0.05, -0.06, true)
            )
        } else {
            wheels = arrayOf(
                Wheel(s.wheelbase * 0.5, -0.06, false),
                Wheel(-s.wheelbase * 0.5, -0.06, true)
            )
        }
    }

    fun rebuild(spec: Spec, build: Build, map: GameMap) {
        this.spec = spec; this.build = build; this.map = map
        r = spec.wheelR
        buildWheels()
        reset(posX)
    }

    fun ratio(): Double {
        val s = spec
        return if (s.cvt) cvtRatio * finalDrive
        else (s.gears.getOrNull(gear) ?: 1.0) * finalDrive
    }

    fun reset(x: Double = 8.0) {
        posX = x
        posY = map.groundY(x) + r + 0.45
        velX = 0.0; velY = 0.0; ang = 0.0; angVel = 0.0
        rpm = spec.idleRPM; boost = 0.0; gear = 0; st.boost = 0.0
        wheels.forEach { w -> w.spin = 0.0; w.L = 0.30; w.prevComp = 0.0 }
        flipT = 0.0; mogok = false; finished = false
        distance = x; lastX = x; maxSpeed = 0.0; time = 0.0
        money = 0.0; crash = null; crashT = 0.0
        map.reset()
    }

    fun recover() {
        ang = 0.0; angVel = 0.0
        velX *= 0.3; velY = 0.0
        posY = map.groundY(posX) + r + 0.55
        wheels.forEach { w -> w.L = 0.34; w.prevComp = maxL - 0.34; w.spin = abs(velX) / r }
        flipT = 0.0; rpm = max(rpm, spec.idleRPM); mogok = false
    }

    fun shift(dir: Int) {
        val s = spec
        if (s.cvt || cut && shiftT > 0.12) return
        val ng = (gear + dir).coerceIn(0, s.gears.size - 1)
        if (ng == gear) return
        gear = ng
        shiftT = 0.26
        lastShiftAt = time
        onShift?.invoke(dir)
    }

    fun pindahGear(g: Int) {
        val s = spec
        if (s.cvt) return
        val ng = g.coerceIn(0, s.gears.size - 1)
        if (ng != gear) { gear = ng; shiftT = 0.24; onShift?.invoke(if (g > gear) 1 else -1) }
    }

    /* ---------------- langkah utama ---------------- */
    fun step(dt: Double, gasIn: Double, brakeIn: Double) {
        val s = spec
        val d = s.driver
        val gas = if (mogok) 0.0 else clamp(gasIn * d.throttle, 0.0, 1.2)
        val brake = clamp(brakeIn * d.brake, 0.0, 1.2)
        throttle = gas; this.brakeIn = brake
        time += dt

        val sub = 4
        val h = dt / sub
        for (i in 0 until sub) subStep(h, gas, brake)

        /* waktu perpindahan gigi */
        if (shiftT > 0) { shiftT -= dt; cut = shiftT > 0.10 } else cut = false

        /* rasio transmisi */
        val wheelSpin = abs(wheels.last().spin)
        val rpmFromWheel = wheelSpin * ratio() * 60.0 / TAU
        if (s.cvt) {
            val target = lerp(s.peakRPM * 0.78, s.powerRPM, gas)
            val rpmPerRatio = max(6.0, wheelSpin * 9.549 * finalDrive)
            val want = clamp(target / rpmPerRatio, 1.35, 2.80)
            val rate = 1.5 * dt
            cvtRatio += clamp(want - cvtRatio, -rate, rate)
            cvtRatio = clamp(cvtRatio, 1.35, 2.80)
        } else if (!manual) {
            if (gear < s.gears.size - 1 && rpmFromWheel > s.redline * 0.955 && kmh > 4) {
                gear++; shiftT = 0.28; onShift?.invoke(1)
            } else if (gear > 0 && rpmFromWheel < s.peakRPM * 0.60) {
                gear--; shiftT = 0.22; onShift?.invoke(-1)
            }
        }

        /* putaran mesin: selip kopling di kecepatan rendah */
        val slipping = clamp(1.0 - abs(velX) / 5.5, 0.0, 1.0)
        clutchSlip = slipping * (0.35 + 0.65 * gas)
        var targetRPM = max(s.idleRPM, rpmFromWheel)
        if (mogok) targetRPM = 0.0
        else if (gas > 0.05 && slipping > 0.0)
            targetRPM = max(targetRPM, s.idleRPM + gas * (s.powerRPM - s.idleRPM) * 0.88 * slipping)

        /* limiter */
        if (!mogok && rpm > s.limiterRPM && s.limiter != "none") {
            when (s.limiter) {
                "hard" -> targetRPM = min(targetRPM, s.limiterRPM - 350)
                "rotasi" -> {
                    val w = sin(time * 34.0)
                    targetRPM = if (w > 0) min(targetRPM, s.limiterRPM - 500) else targetRPM
                }
                "soft" -> targetRPM = min(targetRPM, s.limiterRPM + 60)
            }
        }
        if (targetRPM > s.redline) targetRPM = s.redline + (targetRPM - s.redline) * 0.12
        val up = if (s.flywheel < 1) 9.0 else 6.5
        val accel = (targetRPM - rpm) * if (targetRPM > rpm) up else 7.5
        rpm = clamp(rpm + accel * dt, if (mogok) 0.0 else s.idleRPM * 0.75, s.redline * 1.08)

        st.dt = dt
        st.cut = cut
        boost = boostAt(rpm, s, if (cut) 0.0 else gas, st)

        /* beban mesin buat suara */
        load = clamp(gas * clamp(rpm / s.peakRPM, 0.15, 1.4), 0.0, 1.2)

        /* pengereman mesin saat gas ditutup */
        engineBrake = if (gas < 0.05 && !cut) {
            val over = clamp((rpm - s.idleRPM) / max(500.0, s.redline - s.idleRPM), 0.0, 1.0)
            over * (if (s.twoStroke) 0.6 else 1.0) * (if (isCar) 1.5 else 1.0)
        } else 0.0

        distance = max(distance, posX)
        maxSpeed = max(maxSpeed, kmh)
        if (abs(ang) > 1.9 && abs(velX) < 4) flipT += dt else flipT = 0.0
        if (flipT > 3.2) recover()
        airTime = if (wheels.all { it.contact < 0.02 }) airTime + dt else 0.0
        wheelieT = if (ang > 0.28 && wheels.last().contact > 0.02) wheelieT + dt else 0.0

        val dx = max(0.0, posX - lastX)
        lastX = posX
        money += dx * 1.2 + if (airTime > 0.4) dt * 25.0 else 0.0

        obstacles(dt)
        if (crashT > 0) crashT -= dt

        val rep = damageReport(build)
        if (rep.torque < 0.35 && abs(velX) < 1.5) mogok = true
        if (mogok && rep.torque > 0.5) mogok = false

        if (!finished && posX >= map.panjang.toDouble()) { finished = true; finishTime = time }
        wobble += dt * (2 + abs(velX) * 0.4)
    }

    private fun wheelTorque(): Double {
        val s = spec
        if (mogok || cut) return 0.0
        val tq = torqueAt(rpm, s, boost)
        val slipClutch = clamp(abs(velX) / 4.5, 0.0, 1.0)
        val launch = throttle * (1 - slipClutch) * (if (rpm > s.idleRPM * 1.15) 1.0 else 0.2)
        val t = tq * clamp(throttle + launch * 0.6, 0.0, 1.25)
        return t * ratio() * 0.9
    }

    private fun subStep(dt: Double, gas: Double, brake: Double) {
        val s = spec
        val ca = cos(ang); val sa = sin(ang)
        val rx = ca; val ry = sa          // sumbu kanan body
        val ux = -sa; val uy = ca         // sumbu atas body
        val dxn = -ux; val dyn = -uy      // arah ke bawah (suspensi)
        var fx = 0.0
        var fy = -M * GRAV
        var tq = 0.0

        val wheelTq = wheelTorque()
        val driven = wheels.count { it.driven }.coerceAtLeast(1)

        for (w in wheels) {
            val ax = posX + rx * w.ox + ux * w.oy
            val ay = posY + ry * w.ox + uy * w.oy
            var L = w.L
            for (it in 0 until 4) {
                val cx = ax + dxn * L
                val cy = ay + dyn * L
                val f = cy - r - map.groundY(cx)
                val df = (dyn - map.slope(cx) * dxn).let { if (abs(it) < 1e-6) 1e-6 else it }
                L -= f / df
                L = clamp(L, minL - 0.5, maxL + 0.5)
            }
            val cl = clamp(L, minL, maxL)
            val wcx = ax + dxn * cl
            val wcy = ay + dyn * cl
            val gy = map.groundY(wcx)
            val pen = gy - (wcy - r)
            w.L = cl
            val comp = maxL - cl
            val cVel = (comp - w.prevComp) / dt
            w.prevComp = comp
            var N = k * comp + damp * cVel
            if (N < 0) N = 0.0
            val nMax = k * (maxL - minL) * 2.5
            if (N > nMax) N = nMax
            var penF = 0.0
            if (pen > 0) {
                val vy = velY + angVel * (wcx - posX)
                penF = min(90000.0, 55000.0 * pen + 2600.0 * max(0.0, -vy))
            }
            val totalN = N + penF
            w.N = totalN
            w.contact = if (pen > -0.012) 1 else 0
            w.cx = wcx
            w.cy = wcy - r

            val Fs = N + penF
            fx += ux * Fs; fy += uy * Fs
            tq += (ax - posX) * (uy * Fs) - (ay - posY) * (ux * Fs)

            val slope = map.slope(wcx)
            val tl = hypot(1.0, slope)
            val tx = 1.0 / tl
            val ty = slope / tl
            val rcx = wcx - posX
            val rcy = (wcy - r) - posY
            val vpx = velX - angVel * rcy
            val vpy = velY + angVel * rcx
            val vTan = vpx * tx + vpy * ty

            var wheelT = 0.0
            if (w.driven) wheelT = wheelTq / driven
            if (brake > 0.05) {
                val sgn = when {
                    w.spin > 0.6 -> 1.0
                    w.spin < -0.6 -> -1.0
                    vTan > 0.4 -> 1.0
                    vTan < -0.4 -> -1.0
                    else -> 0.0
                }
                wheelT -= sgn * brake * 2400.0 * s.brake
            }
            /* pengereman mesin */
            if (engineBrake > 0.0 && w.driven) wheelT -= engineBrake * 26.0
            val rollT = -w.spin * 0.12
            val Ttot = wheelT + rollT

            val slipVel = w.spin * r - vTan
            val gripN = mu * totalN
            val Meff = max(25.0, M * 0.55)
            val denom = dt * (r * r / Iw + 1.0 / Meff)
            var Fx = (slipVel + Ttot * dt * r / Iw) / denom
            Fx = if (w.contact < 0.5) 0.0 else clamp(Fx, -gripN, gripN)
            w.slip = clamp(slipVel / (abs(vTan) + 1.5), -2.0, 2.0)

            fx += tx * Fx; fy += ty * Fx
            tq += rcx * (ty * Fx) - rcy * (tx * Fx)

            val wAcc = (Ttot - Fx * r) / Iw
            w.spin = clamp(w.spin + wAcc * dt, -260.0, 260.0)
            w.spinAng += w.spin * dt
        }

        /* body kena tanah */
        val corners = arrayOf(
            doubleArrayOf(halfL, -halfH), doubleArrayOf(-halfL, -halfH),
            doubleArrayOf(halfL, halfH * 0.6), doubleArrayOf(-halfL, halfH * 0.6)
        )
        for (c in corners) {
            val wx = posX + rx * c[0] + ux * c[1]
            val wy = posY + ry * c[0] + uy * c[1]
            val gy = map.groundY(wx)
            val p = gy - wy
            if (p > 0) {
                val slope = map.slope(wx)
                val tl = hypot(1.0, slope)
                val nx = -slope / tl
                val ny = 1.0 / tl
                val rcx = wx - posX
                val rcy = wy - posY
                val vpx = velX - angVel * rcy
                val vpy = velY + angVel * rcx
                val vn = vpx * nx + vpy * ny
                val Fn = min(120000.0, 42000.0 * p + 2200.0 * max(0.0, -vn))
                fx += nx * Fn; fy += ny * Fn
                tq += rcx * (ny * Fn) - rcy * (nx * Fn)
                val tx = ny
                val ty = -nx
                val vt = vpx * tx + vpy * ty
                val Ft = -clamp(vt * 1400.0, -0.5 * Fn, 0.5 * Fn)
                fx += tx * Ft; fy += ty * Ft
                tq += rcx * (ty * Ft) - rcy * (tx * Ft)
            }
        }

        /* keseimbangan pengendara */
        if (wheels.all { it.contact < 0.5 }) {
            val targetAng = atan(map.slope(posX))
            tq += (targetAng - ang) * I * 3.4 - angVel * I * 1.3
        } else {
            /* tahan supaya gak nungging terus: batasi sudut dan redam ayunan */
            tq -= clamp((ang - 0.20) * 5200.0, 0.0, 7000.0) / spec.driver.balance
            tq -= angVel * I * 1.6
        }

        /* hambatan udara & gelinding */
        val v = speed
        if (v > 0.05) {
            val drag = 0.5 * 1.2 * spec.drag * v * v
            fx -= drag * (velX / v)
            fy -= drag * (velY / v)
        }
        val roll = 0.014 * M * GRAV * spec.rolling
        if (abs(velX) > 0.1) fx -= sign0(velX) * roll

        velX += (fx / M) * dt
        velY += (fy / M) * dt
        angVel += (tq / I) * dt
        angVel *= exp(-0.55 * dt)
        angVel = clamp(angVel, -7.0, 7.0)
        posX += velX * dt
        posY += velY * dt
        ang += angVel * dt
        ang = ((ang + PI) % TAU + TAU) % TAU - PI
    }

    /* ---------------- tabrakan ---------------- */
    private fun obstacles(dt: Double) {
        val ca = cos(ang); val sa = sin(ang)
        for (o in map.obstacles) {
            if (o.hit) { o.t += dt; continue }
            val dx = o.x - posX
            if (dx > 8 || dx < -8) continue
            val t = OBSTACLE_TYPES[o.type] ?: continue
            val gy = map.groundY(o.x)
            val x0 = o.x - t.w / 2
            val x1 = o.x + t.w / 2
            val y0 = gy
            val y1 = gy + t.h
            val pts = listOf(
                doubleArrayOf(wheels[0].cx, wheels[0].cy + r * 0.5),
                doubleArrayOf(wheels.last().cx, wheels.last().cy + r * 0.5),
                doubleArrayOf(posX + ca * halfL, posY + sa * halfL),
                doubleArrayOf(posX - ca * halfL * 0.6, posY - sa * halfL * 0.6)
            )
            var hitX = 0.0
            var hitY = 0.0
            var kena = false
            for (p in pts) {
                if (p[0] > x0 && p[0] < x1 && p[1] > y0 && p[1] < y1) { hitX = p[0]; hitY = p[1]; kena = true; break }
            }
            if (!kena) continue

            val impact = abs(velX) + abs(velY) * 0.5
            o.hit = true
            o.t = 0.0
            val keras = clamp((impact - 2.2) / 16.0, 0.0, 1.0) * t.dmg
            val fromLeft = posX < o.x
            posX = if (fromLeft) min(posX, x0 - halfL * 0.85) else max(posX, x1 + halfL * 0.85)
            val bounce = t.solid * (0.25 + 0.35 * clamp(impact / 20.0, 0.0, 1.0))
            velX = -velX * bounce
            velY = max(velY, 1.0 + impact * 0.10)
            angVel += (RND.next() - 0.5) * 3.2 * t.solid - sign0(velX) * 0.8
            wheels.forEach { w -> w.spin *= 1 - 0.7 * t.solid }
            val dmg = if (keras > 0.02) applyDamage(build, keras, o.type) else null
            crash = CrashFx(hitX, hitY, clamp(impact / 18.0, 0.0, 1.0), o.type, dmg?.second ?: "")
            crashT = 0.6
            if (impact > 9 && RND.chance(0.5)) mogok = true
        }
    }
}
