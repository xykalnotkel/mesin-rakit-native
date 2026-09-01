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

Yang baru di 1.4.1
- Format desain MRPACK1. Menu punya tombol Salin kode dan Tempel.
  Desain rangka dan body dari studio web (mesin.xyspace.my.id/drawing)
  langsung kepasang setelah ditempel.
- Biang "terhenti terus": kalau session sebelumnya belum sampai tanda aman,
  aplikasi langsung menutup diri dan membuka laporan. Setelah itu jejak
  tidak pernah dibersihkan, jadi tiap buka langsung ditutup lagi. Sekarang
  jejak lama cuma dicatat, permainan tetap dibuka.
- Tanda seru di jejak (peringatan lanskap, latar, dsb) tidak lagi dianggap
  crash. Dulu satu peringatan mengunci aplikasi selamanya.
- MainActivity yang sudah dihapus masih tertulis di manifest: dibuang.
- Dialog sistem "telah terhenti" tidak lagi dipanggil dari penangkap error.
- Rendering software dikembalikan (cegah crash driver GPU).
- Boot game selesai sebelum SurfaceView hidup, supaya thread gambar tidak
  merender layar yang belum siap.
- Ganti orientasi tidak lagi merestart aktivitas di tengah onCreate.

Yang baru di 1.4.0
- Layar game dan layar pembuka digabung jadi satu. Layar pembuka terbukti
  selalu berhasil jalan, jadi sekarang seluruh permainan hidup di dalamnya.
  Aktivitas kedua dihapus total.
- Setiap langkah startup sekarang punya penanda sendiri: mulai, flag
  jendela, lanskap, latar, bilah disembunyikan, font, tampilan, App,
  sampai boot selesai. Kalau ada yang gagal, laporannya langsung nunjukin
  di langkah ke berapa.
- Tombol fisik (Q, E, R, spasi) diperbaiki: tampilan sekarang minta fokus
  supaya tombolnya nyampe.
- Berkas Unduhan crash-mesin-rakit.txt sekarang juga ditulis waktu startup
  gagal, jadi bisa dibaca tanpa perlu membuka aplikasi dua kali.

Yang baru di 1.3.1
- Dari laporan pemain terbukti bahwa aplikasi mati di dalam super.onCreate,
  sebelum kode kita jalan. Dua setelan yang selama ini ditulis di manifest
  dan tema sekarang dipasang lewat kode dan dibungkus penangkap error:
  orientasi lanskap dan warna latar jendela. Kalau ada HP yang menolak
  salah satunya, sekarang cuma dicatat lalu dilewati, aplikasi tetap jalan.
- Penanda jejak ditambah sebelum dan sesudah super.onCreate, supaya kalau
  masih ada masalah, langsung kelihatan apakah itu di pembuatan jendela
  Android atau di kode kita.

Yang baru di 1.3.0
- Alat uji mandiri. Sekarang ada ikon kedua di laci aplikasi bernama
  UJI MESIN. Buka ikon itu dan dia akan menguji satu per satu: activity,
  penyimpanan berkas, font kustom, kanvas, model mesin, audio, dan
  permukaan gambar. Hasilnya ditulis di layar, jadi langsung kelihatan
  bagian mana yang bermasalah di HP tertentu.

Yang baru di 1.2.1
- Catatan error sekarang juga ditulis ke folder Unduhan atas nama
  crash-mesin-rakit.txt. Tidak butuh izin apa pun, dan bisa dibuka
  langsung dari aplikasi Berkas lalu dibagikan. Ini jalur cadangan
  seandainya layar laporan di dalam aplikasi tidak sempat muncul.
- Penangkap error menulis berkasnya sendiri, jadi tetap ada isinya
  sekalipun error terjadi sebelum komponen aplikasi selesai disiapkan.

Yang baru di 1.2.0
- Pintu masuk baru yang temanya benar-benar standar Android. Tujuannya satu:
  memastikan layar laporan error selalu bisa muncul, sekalipun layar game
  mati sebelum kode kita sempat jalan.
- Tema game dikupas sampai tersisa yang perlu saja. Setelan jendela yang
  tidak standar (status bar tembus pandang, background khusus) dibuang
  karena itu yang paling mungkin bikin Activity mati di dalam super.onCreate.
- Manifest dibersihkan: flag isGame, resizeableActivity, dan daftar
  configChanges yang kepanjangan semuanya dibuang.
- Logika penanda jejak dibenerin: pemasangan baru tidak lagi dibilang crash.

Yang baru di 1.1.2
- Semua helper gambar sekarang menolak koordinat NaN dan tak terhingga.
  Skia bisa mati secara native kalau dikasih nilai rusak, dan itu tidak
  bisa ditangkap try/catch biasa. Ini kemungkinan besar biang aplikasi
  berhenti mendadak tanpa pesan.
- Error yang ditangkap sekarang bertipe Throwable, jadi kehabisan memori
  dan sejenisnya juga ikut tertangkap, bukan cuma Exception.
- Penangkap error dipasang di baris paling awal onCreate, dan seluruh
  isi onCreate dibungkus: kalau ada yang gagal, layar laporan tetap
  muncul, bukan dialog kosong.
- Gambar memakai rendering software (hardwareAccelerated dimatikan) buat
  menghindari crash driver GPU di HP tertentu.
- Penanda jejak tidak lagi menulis berkas tiap frame.

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
