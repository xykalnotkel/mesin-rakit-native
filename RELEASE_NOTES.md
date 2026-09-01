MESIN RAKIT - versi native Android (Kotlin)

Sandbox rakit mesin 2D. Rakit dari nol, bentuk sendiri, lalu dengarkan
suaranya yang lahir dari susunan part yang kamu pasang.

Yang ada di rilis ini
- Mesin dan transmisi: 111 part nyata di 11 kategori, dari blok 1 silinder
  sampai V8, plus mesin rotary wankel 2 dan 3 rotor.
- Gigi manual 4, 5, dan 6 percepatan dengan bunyi oper gigi, kopling selip
  waktu mulai jalan, pengereman mesin, dan transmisi matik CVT.
- 16 preset siap pakai, 5 pengendara, 5 lintasan termasuk Lintasan Lurus
  khusus buat setting dan tes top speed.
- Suara disintesis langsung dari rakitan: nada knalpot dari panjang dan
  diameter pipa, pola ledakan dari jumlah silinder, siulan turbo, isapan
  udara, ketukan mesin, dan letupan waktu gas ditutup.
- Gaya knalpot unik: Standar, Racing, Kopong, Chamber 2-Tak, Drumben,
  Helikopter, Thai Style, dan Vortex.
- CDI dengan kurva pengapian dan tiga model limiter: halus, keras, dan rotasi.
- Bengkel Rangka: taruh simpul, tarik pipa, tekuk, atur diameter, ketebalan,
  dan bahan. Rangka butuh dudukan stang, mesin, dan as roda belakang.
- Bentuk body bebas: gambar sendiri di grid pakai 12 warna cat, hasilnya
  jadi bodi kendaraan.
- Tabrakan merusak part (tingkat sedang): tenaga turun, bisa mogok, dan
  bisa diservis di bengkel, penuh atau cuma 45 persen.
- Font kustom, tanpa emoji, semua digambar langsung di kanvas Android.

Cara main
- BENGKEL: pilih part di palet, ketuk grid buat pasang, geser buat mindah.
- RANGKA: taruh simpul lalu tarik dari simpul ke simpul.
- DYNO: tahan GAS, atur BEBAN, lihat kurva torsi dan tenaga.
- JALAN: tombol GAS dan REM di layar, Q dan E buat oper gigi, R buat bangun.

Yang baru di 1.1.1
- Layar laporan error. Kalau aplikasi berhenti, layar ini muncul sendiri
  dan menampilkan pesan errornya lengkap dengan jejak startup, plus tombol
  salin supaya gampang dikirim. Layarnya jalan di proses terpisah, jadi
  tetap muncul walaupun proses utama sudah dimatikan.
- Penanda jejak: tiap langkah startup dicatat ke berkas. Jadi sekalipun
  aplikasi mati sebelum sempat menyimpan pesan error, tetap kelihatan dia
  berhenti di langkah ke berapa.
- onPause, onDestroy, dan tombol balik sudah diamankan: tidak lagi
  mengakses tampilan yang belum jadi, yang kemarin bisa menutupi error
  aslinya.
- Catatan error sekarang menyertakan merek HP, versi Android, dan versi
  aplikasi.

Yang baru di 1.1.0
- APK sekarang dibangun sebagai rilis dan ditandatangani dengan kunci tetap.
  Artinya pembaruan bisa langsung ditimpa tanpa harus hapus aplikasi lama,
  dan berkasnya siap diupload ke Play Store kalau lu mau.
- Kunci tanda tangannya disimpan di GitHub Secrets, tidak pernah masuk repo.

Yang dibenahi di 1.0.3
- Audio tidak lagi disentuh sama sekali waktu aplikasi dibuka. Mesin suara
  baru hidup waktu masuk Dyno atau Jalan, jadi kalau ada HP yang menolak
  format audionya, aplikasi tetap masuk ke menu dan bisa dimainkan.
- Layar disiapkan paling awal saat boot. Kalau ada yang gagal, menu tetap
  tampil bersama pesan error, bukan layar gelap.

Yang dibenahi di 1.0.2
- Thread audio tidak lagi memakai prioritas maksimum dan selalu memberi
  jeda kalau penulisan gagal. Ini biang utama aplikasi tiba-tiba berhenti
  atau membeku di sebagian HP.
- Kalau format audio float tidak didukung HP, otomatis turun ke 16 bit.
- Kalau tetap terjadi error, sekarang pesannya ditampilkan di layar menu
  dan disimpan di Android/data/id.mesinrakit/files/crash-mesin-rakit.txt
  supaya gampang dilaporkan.

Catatan teknis
- Dibangun dengan Kotlin 2.0 + Android Gradle Plugin, tanpa dependensi
  pihak ketiga. Audio disintesis langsung memakai AudioTrack (PCM float).
- Android 6.0 (API 23) ke atas, layar lanskap.
- Font Chakra Petch dan Barlow Condensed, lisensi SIL Open Font License.
