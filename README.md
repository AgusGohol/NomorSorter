# Nomor Sorter — Java siap dijalankan dengan JRE

## Baru di versi 2.10.0

- Tombol **Salin semua device** di samping menu **⋯** pada halaman Input device. Pilih **Sudah WA** atau **Belum WA**; aplikasi menyalin nomor dari semua device pada tanggal yang sedang aktif, urut per device (Device 1, Device 2, …). Nomor yang sama di beberapa device ikut tersalin sesuai jumlah device-nya, dan nomor yang disembunyikan dari riwayat device ikut tersalin. Fitur ini bersifat sementara.

## Baru di versi 2.9.0 — penyimpanan lebih aman

- **Hanya satu jendela.** Jika Nomor Sorter dibuka dua kali dari folder yang sama, jendela kedua menolak dibuka. Dua jendela sebelumnya bisa saling menimpa nomor.
- **Tidak menimpa data lain.** Setiap kali menyimpan, aplikasi membaca ulang database dari disk dan hanya menambahkan nomor baru, bukan mengganti seluruh isi device dengan salinan di memori.
- **Langsung tertulis ke disk.** Berkas disimpan dengan mode sinkron sehingga tidak menjadi kosong (0 byte) bila PC mati mendadak.
- **Jurnal** di `data/jurnal/<tahun-bulan>.txt`: setiap nomor yang disimpan atau dihapus dicatat, dan berkas ini hanya pernah ditambah, tidak pernah ditimpa. Di halaman **Belum WA / Sudah WA**, menu **⋯ → Pulihkan nomor yang hilang (dari jurnal)** mengembalikan nomor yang pernah disimpan tetapi tidak ada lagi di database. Nomor yang Anda hapus sendiri tidak ikut kembali.
- **Cadangan harian otomatis** di `data/cadangan/<tanggal>.zip` saat aplikasi dibuka (disimpan 30 hari terakhir). Untuk memulihkan secara manual, tutup aplikasi lalu ekstrak folder tanggal dari ZIP ke folder `data`.
- **Konfirmasi tanggal.** Jika device sedang memakai tanggal selain hari ini, aplikasi bertanya dulu: simpan ke tanggal itu, pakai hari ini, atau batal. Pesan setelah menyimpan juga menyebut device dan tanggalnya.

## Baru di versi 2.8.0 — tampilan baru

Tampilan dirapikan agar lebih minimalis: satu warna aksen, huruf Segoe UI, ikon Windows, dan tombol dikelompokkan sesuai fungsinya. Semua fitur tetap ada, sebagian pindah tempat:

| Dulu | Sekarang |
| --- | --- |
| Tombol Device 1–5 dan **+ Tambah device** | Tab **Device 1, Device 2, …** dan tombol **+** |
| **− Hapus Device N** | Klik kanan tab device terakhir → **Hapus Device N** |
| **Masukkan ke Sudah WA / Belum WA** | **Ke Sudah WA / Ke Belum WA** di bawah kotak input |
| Tab Sudah WA / Belum WA | Tombol segmen **Sudah WA · Belum WA** (dengan jumlah) |
| **Hapus nomor terpilih**, **Kosongkan / Pulihkan riwayat** | Menu **⋯** di bawah tabel |
| **Hapus terpilih / Hapus semua** (halaman database) | Menu **⋯** di bawah tabel database |
| **Pilih dari kalender** | Ikon kalender di samping pilihan tanggal |

Kolom **Cari nomor** di halaman input kini juga menerima awalan `08`, `62`, dan `+62`.

## Baru di versi 2.7.0

- **Jumlah device bisa ditambah.** Klik **+ Tambah device** di sebelah tombol device. Jumlah device disimpan di `data/jumlah-device.txt` (bawaan 5). Tombol **− Hapus Device N** hanya menghapus device terakhir, dan hanya jika device itu belum memiliki catatan di database, agar tidak ada nomor yang hilang dari tampilan.
- **Ekspor ke Notepad (.txt) atau Excel (.xlsx)** lewat tombol **Ekspor nomor** di sisi kiri, atau **Ekspor TXT/Excel** pada halaman device dan halaman database. Ekspor CSV lama digantikan fitur ini.
  - Centang **beberapa tanggal sekaligus**, atau gunakan **Centang bulan ini** / **Centang semua tanggal**. Device dan status (Sudah WA / Belum WA) juga bisa dipilih.
  - **Notepad**: satu nomor per baris dari atas ke bawah, tanpa tanggal.
  - **Excel**: tabel No., Tanggal, Device, COM, Nomor, Status. Nomor disimpan sebagai teks agar angka 0 atau tanda + di depan tidak hilang.
  - Awalan nomor dapat dipilih: `8...`, `08...`, `62...`, atau `+62...`. Opsi **Buang nomor duplikat** (aktif secara bawaan) hanya menulis nomor yang sama satu kali.
  - Ekspor mengambil data dari database utama, termasuk nomor yang disembunyikan dari riwayat device.
- Data **tidak pernah terhapus otomatis** saat pergantian hari atau bulan. Setiap tanggal disimpan di foldernya sendiri dan hanya terhapus lewat tombol hapus di halaman database.
- **Pembaruan otomatis dari GitHub.** Setiap kali dibuka, aplikasi memeriksa [rilis terbaru](https://github.com/AgusGohol/NomorSorter/releases/latest). Jika ada versi baru, muncul pilihan **Perbarui sekarang**, **Nanti**, atau **Lewati versi ini**. Saat diperbarui, aplikasi mengunduh ZIP, menutup diri, memasang pembaruan lewat `NomorSorterUpdater.jar` (folder `data` tidak diubah, cadangan disimpan di `cadangan-pembaruan`), lalu terbuka kembali. Tombol **Periksa pembaruan** di sisi kiri dapat dipakai kapan saja. Butuh koneksi internet; tanpa internet aplikasi tetap berjalan seperti biasa.

> Pengguna versi 2.6.3 atau lebih lama perlu memasang 2.7.0 **sekali secara manual** (unduh ZIP dari halaman Releases, lalu pakai `jalankan-updater.bat`). Setelah itu pembaruan berikutnya berjalan otomatis.

Versi 2.6.3 mengenali nomor yang dimulai dengan `8`, `08`, `62`, atau `+62`, kemudian **menyimpannya sebagai `8...`**. Data lama berawalan `08` atau `62` dikonversi ke `8` saat aplikasi pertama dibuka setelah pembaruan, termasuk riwayat tersembunyi; salinan berkas lama disimpan dengan akhiran `.sebelum-format-8.bak`.

Aplikasi desktop Java Swing untuk mengelompokkan nomor Anda dalam **Sudah WA** dan **Belum WA** berdasarkan tulisan pada baris data. Status juga dapat dipilih manual; aplikasi tidak memeriksa akun WhatsApp secara langsung.

## Cara menjalankan di Windows

1. Ekstrak seluruh isi ZIP.
2. Pastikan **JRE / Java Runtime versi 17 atau lebih baru** terpasang.
3. Klik dua kali `jalankan.bat`. Anda **tidak perlu JDK atau javac**.

Untuk pembaruan berikutnya, jalankan `jalankan-updater.bat` **dari folder NomorSorter yang sama**. Klik **Pilih ZIP pembaruan** dan pilih ZIP baru tanpa mengekstraknya. Updater akan menemukan aplikasi utama secara otomatis; folder `data` tetap tersimpan. ZIP paket ini menyertakan updater di folder aplikasi.

Alternatif dari Command Prompt di folder `NomorSorter`:

```bat
java -jar NomorSorter.jar
```

Tempel teks tabel (satu entri per baris) lalu klik **Sortir otomatis dari tulisan WA**. Contoh:

```text
COM4  XL Axiata  083135760716  -51  on  69
COM7  XL Axiata  083172103663  -51  on  69  1 | wa
COM9  XL Axiata  083824890138  -51  on  69  2 | tg
COM12 XL Axiata  83133392181   -55  on  60  1 | wa
```

Baris COM7 masuk **Sudah WA** karena ada kata `wa` tersendiri. Baris COM4 dan COM9 masuk **Belum WA**. Tulisan `belum wa` juga dianggap Belum WA. Aplikasi hanya membaca penanda dalam teks, bukan memeriksa akun WhatsApp secara langsung.

Nilai COM pada baris yang sama dengan nomor (misalnya `COM7`) ikut ditampilkan pada kolom **COM** dan disimpan bersama nomor. Baris yang tidak memiliki COM ditampilkan dengan tanda `—`. Ekspor Excel juga berisi COM, nomor, status, dan tanggal. Klik baris untuk menyalin nomornya saja.

Teks dapat diimpor dari TXT/CSV. Tombol masukkan manual tetap tersedia. Ada pula pencarian, hapus dari riwayat, dan ekspor TXT/Excel.

## Data per tanggal dan menyalin nomor

Pilih **Device** di atas daftar tanggal (bawaan Device 1–5, bisa ditambah). Setiap device memiliki kotak input dan hasil sortir tersendiri. Input yang belum diproses tetap ada ketika berpindah device selama aplikasi masih terbuka. Semua device menyimpan hasil dalam **basis data bersama per tanggal**, dengan asal device tetap dicatat.

Setiap nomor baru disimpan berdasarkan tanggal komputer saat diinput. Contoh: `data/2026-09-28/belum-wa.db` dan `data/2026-09-28/sudah-wa.db` berisi catatan gabungan dari semua device. Ketika hari pada komputer berganti, semua device otomatis beralih ke tanggal baru tanpa perlu menutup aplikasi; perubahan juga diperiksa saat berpindah device atau memproses nomor. Untuk menginput pada tanggal tertentu, pilih tanggal yang ada pada daftar **Tanggal** atau klik **Pilih dari kalender** untuk memilih tanggal tanpa mengetik. Nomor yang diproses disimpan pada tanggal yang aktif di device tersebut, termasuk tanggal manual. Saat hari komputer berganti, tanggal input semua device kembali ke hari baru. Pilih tanggal pada daftar **Tanggal** untuk melihat data lama.

Tombol **Belum WA** dan **Sudah WA** di sebelah kiri membuka **halaman database dalam jendela utama**. Setiap halaman memiliki pilihan tanggal lewat daftar atau tombol **Kalender**, tombol **Input pada tanggal ini** untuk beralih ke halaman Device dan menyimpan input pada tanggal pilihan, serta pencarian nomor sendiri (menerima awalan 8, 08, 62, atau +62), tabel Device/COM/Nomor, tombol **Salin nomor terpilih**, dan tombol **Salin semua nomor tanggal ini**. Klik satu baris untuk langsung menyalin nomornya. Pencarian menyaring tabel pada tanggal yang dipilih; kosongkan kotak pencarian untuk melihat semua catatan. Tombol salin semua tetap menyalin seluruh nomor pada tanggal itu. Tombol **Device** di sisi kiri kembali ke halaman input. Nomor yang sama dari beberapa device hanya disalin satu kali pada tombol salin semua. Tombol **Salin nomor terpilih** pada tampilan device menyalin hanya baris yang dipilih, sedangkan salin semua hanya menyalin nomor dari device yang sedang dipilih. Hasil salin memakai format `8...`.

Pada halaman database **Sudah WA** dan **Belum WA**, tombol **Hapus terpilih** menghapus catatan yang dipilih (termasuk asal device yang tercantum pada baris itu), sedangkan **Hapus semua pada tanggal ini** menghapus seluruh catatan status tersebut pada tanggal yang dipilih. Keduanya mengubah database utama setelah konfirmasi. Status lain dan tanggal lain tidak ikut terhapus. Tombol **Pulihkan riwayat** pada halaman device tidak memulihkan catatan yang sudah dihapus dari database utama.

Di halaman **Device**, tombol **Hapus nomor terpilih** menyembunyikan baris terpilih dari riwayat device. **Kosongkan riwayat tanggal ini** menyembunyikan seluruh baris Sudah WA dan Belum WA pada device serta tanggal yang sedang dipilih. **Pulihkan riwayat tanggal ini** menampilkan kembali baris tersebut. Penghapusan riwayat hanya memengaruhi tampilan device: nomor tetap ada di database utama pada halaman kiri. Riwayat tersembunyi disimpan terpisah di `data/riwayat-tersembunyi` dan tetap tersembunyi setelah aplikasi ditutup. Jika nomor yang sama dimasukkan lagi, ia tampil kembali pada riwayat device. Ekspor dan salin dari halaman device hanya mengambil baris yang terlihat.

Jika Anda memakai aplikasi updater, pilih ZIP ini dan folder NomorSorter yang saat ini digunakan; folder `data` lama tetap disimpan. Jika memperbarui secara manual, **salin folder `data` dari versi lama** ke folder aplikasi baru sebelum pertama kali menjalankannya. Data harian lima device lama digabung per tanggal; folder asal disimpan sebagai `.migrated`. Berkas lebih lama `data/sudah-wa.db` dan `data/belum-wa.db` dimasukkan ke tanggal saat pertama kali dibuka karena versi itu belum menyimpan tanggal asal. Jangan menghapus folder `data` saat memperbarui aplikasi. Berkas `.db` adalah format teks internal, bukan SQLite.

Kode sumber disertakan di `src/NomorSorter.java`. File `.jar` sudah dikompilasi untuk Java 17.

## Untuk pengembang: membuat rilis baru

1. Ubah kode di `src/NomorSorter.java`, lalu naikkan versi pada **dua tempat** yang harus sama: berkas `VERSION` dan konstanta `APP_VERSION`.
2. Jalankan `powershell -ExecutionPolicy Bypass -File build.ps1` (butuh JDK 17). Hasilnya `dist/NomorSorter_<versi>.zip`.
3. Buat rilis GitHub bertag `v<versi>` dan lampirkan ZIP tersebut, misalnya:
   `gh release create v2.7.1 dist/NomorSorter_2.7.1.zip --title "Nomor Sorter 2.7.1" --notes "Isi perubahan"`
   Catatan rilis (`--notes`) ditampilkan kepada pengguna pada jendela pembaruan.

Aplikasi pengguna membaca `releases/latest`, jadi rilis berstatus *draft* atau *pre-release* tidak dikirim ke pengguna dan bisa dipakai untuk uji coba. `NomorSorterUpdater.jar` disertakan dalam bentuk terkompilasi (kode sumbernya tidak tersedia di repo ini).
