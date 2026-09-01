# MESIN RAKIT

Sandbox rakit mesin 2D buat Android. Rakit kendaraan dari nol, bentuk
rangkanya sendiri, lalu dengarkan suara mesin yang lahir dari susunan part
yang kamu pasang.

Versi ini ditulis penuh dengan **Kotlin native** (tanpa WebView, tanpa
game engine pihak ketiga). Semua antarmuka digambar langsung di Canvas
Android, dan suara disintesis sendiri lewat AudioTrack.

## Tumpukan teknologi

| Bagian | Pilihan |
| --- | --- |
| Bahasa | Kotlin 2.0.21 |
| Build | Gradle 8.9 + Android Gradle Plugin 8.6.1 |
| Antarmuka | Canvas 2D Android (SurfaceView + thread render sendiri) |
| Audio | AudioTrack PCM float, sintesis langsung per sampel |
| Dependensi | Tidak ada. Murni framework Android |
| Minimum | Android 6.0 (API 23), layar lanskap |

## Struktur

```
app/src/main/java/id/mesinrakit/
  BukaActivity.kt     pintu masuk + layar game
  GameView.kt         SurfaceView, loop render, sentuhan multi jari
  App.kt              keadaan aplikasi, pindah layar, simpan dan muat
  data/Pack.kt        format tukar desain MRPACK1
  core/ data/ model/ audio/ scene/ ui/
web/                  situs unduhan + studio Drawing (Vercel)
test/TestModel.kt     uji logika model, jalan langsung di JVM
```

Situs resmi: [mesin.xyspace.my.id](https://mesin.xyspace.my.id)
Studio Drawing, riwayat rilis, dan lisensi lengkap ada di situs.
Lisensi bahan pihak ketiga: lihat berkas `LICENSE`.

## Cara membangun

Butuh JDK 17 dan Android SDK.

```bash
gradle assembleDebug          # APK debug di app/build/outputs/apk/debug/
java -cp ... TestModelKt      # lihat bagian pengujian
```

APK rilis juga dikerjakan otomatis di GitHub Actions setiap push ke `main`
dan setiap tag `v*`. Tag menghasilkan Release berisi APK.

## Pengujian

Model murni Kotlin (tidak menyentuh Android), jadi bisa diuji langsung di
JVM memakai compiler Kotlin:

```bash
SRC=$(find app/src/main/java/id/mesinrakit/core \
           app/src/main/java/id/mesinrakit/data \
           app/src/main/java/id/mesinrakit/model -name '*.kt' | grep -v PartShape)
kotlinc -cp "$ANDROID_HOME/platforms/android-34/android.jar" \
        -d /tmp/outtest $SRC test/TestModel.kt
java -cp /tmp/outtest:$KOTLIN_HOME/lib/kotlin-stdlib.jar TestModelKt
```

Uji mencakup: keabsahan 16 preset, ledakan per silinder (termasuk rotary),
kelulusan tanah tiap map, simulasi jalan 40 detik di tiap lintasan, oper
gigi manual, tabrakan dan servis, serta perhitungan rangka custom.

## Lisensi font

Chakra Petch dan Barlow Condensed, SIL Open Font License 1.1.
