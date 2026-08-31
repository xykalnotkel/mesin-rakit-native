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
  MainActivity.kt     activity, mode layar penuh, siklus hidup
  GameView.kt         SurfaceView, loop render, sentuhan multi jari
  App.kt              keadaan aplikasi, pindah layar, simpan dan muat
  core/               matematika, filter biquad, tema, font, helper gambar
  data/               katalog part, preset, pengendara, map, gaya knalpot
  model/              spesifikasi mesin, fisika kendaraan, map, rangka
  audio/              mesin suara: siklus mesin dan lapisan hidup
  scene/              menu, bengkel, bengkel rangka, dyno, pit, peta, jalan
  ui/                 kerangka scene dan widget kanvas
test/TestModel.kt     uji logika model, jalan langsung di JVM
```

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
