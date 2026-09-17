# TUTORIAL LENGKAP — APK KEPUASAN PELANGGAN PLN ULP TANJUNG REDEB

## 1. Konsep versi sederhana

Aplikasi Android ini tidak memakai database Android terpisah. APK hanya berfungsi sebagai tampilan kiosk/fullscreen yang membuka Web App Google Apps Script. Database tetap Google Spreadsheet.

Alur:
**Pelanggan → APK → Google Apps Script Web App → Google Spreadsheet**

Keuntungannya:
- tidak ada address bar/menu browser,
- database tetap mudah dibuka oleh petugas,
- perubahan dashboard/rekap tetap dilakukan di Spreadsheet,
- APK cukup dikonfigurasi satu kali.

---

## 2. Isi paket

Folder penting:
- `app/` = source APK Android.
- `backend-google-apps-script/Code.gs` = backend Spreadsheet.
- `backend-google-apps-script/Index.html` = tampilan Web App.
- `backend-google-apps-script/Template_Database_....xlsx` = template database.
- `.github/workflows/build-apk.yml` = build APK otomatis di GitHub.
- `QUICK_START.txt` = panduan ringkas.

---

## 3. Tahap A — Siapkan Google Spreadsheet

### Cara yang paling mudah
1. Upload file Excel template yang ada di `backend-google-apps-script`.
2. Buka dengan Google Sheets.
3. Simpan sebagai Google Spreadsheet bila diperlukan.

Atau buat Spreadsheet kosong baru.

Nama yang disarankan:
**Kepuasan Pelanggan PLN ULP Tanjung Redeb**

---

## 4. Tahap B — Pasang Google Apps Script

1. Buka Spreadsheet.
2. Klik **Extensions / Ekstensi → Apps Script**.
3. Di file `Code.gs`, hapus kode bawaan.
4. Buka file lokal:
   `backend-google-apps-script/Code.gs`
5. Copy seluruh isinya ke `Code.gs`.
6. Klik tanda **+** di Apps Script.
7. Pilih **HTML**.
8. Beri nama:
   `Index`
9. Buka file lokal:
   `backend-google-apps-script/Index.html`
10. Copy seluruh isi ke file `Index`.
11. Klik **Save**.

---

## 5. Tahap C — Buat struktur database otomatis

Jika Spreadsheet baru/kosong:
1. Pada bagian pilihan fungsi Apps Script pilih:
   `setupSpreadsheet`
2. Klik **Run/Jalankan**.
3. Google akan meminta izin.
4. Pilih akun Google.
5. Izinkan akses.
6. Kembali ke Spreadsheet.

Sheet yang seharusnya tersedia:
- DASHBOARD
- DATABASE_SURVEI
- REKAP_HARIAN
- REKAP_BULANAN
- REKAP_TAHUNAN
- PENGATURAN

**Jika Spreadsheet lama sudah berisi data produksi, jangan jalankan setup ulang tanpa backup.**

---

## 6. Tahap D — Deploy Web App

1. Buka Apps Script.
2. Klik **Deploy → New deployment**.
3. Klik ikon roda gigi / Select type.
4. Pilih **Web app**.
5. Description:
   `Kepuasan Pelanggan PLN ULP Tanjung Redeb`
6. Execute as:
   **Me**
7. Who has access:
   pilih akses yang memungkinkan tablet/HP APK membuka URL.
8. Klik **Deploy**.
9. Copy URL hasil deploy.

URL yang dipakai harus URL Web App produksi, biasanya berakhir:
`/exec`

Contoh:
`https://script.google.com/macros/s/AKfycbxxxxxxxx/exec`

Simpan URL ini.

---

## 7. Tahap E — Build APK

### Pilihan 1 — Android Studio

1. Install Android Studio.
2. Extract paket ZIP.
3. Buka Android Studio.
4. Klik **Open**.
5. Pilih folder:
   `PLN_Kepuasan_Pelanggan_APK_READY`
6. Tunggu proses Gradle Sync.
7. Jika Android Studio meminta SDK Android, klik Install/Accept.
8. Setelah Sync selesai pilih:
   **Build → Build App Bundles or APKs → Build APKs**
9. Tunggu sampai muncul:
   **BUILD SUCCESSFUL**
10. Klik **locate**, atau buka:
   `app/build/outputs/apk/debug/app-debug.apk`
11. Rename bila diinginkan menjadi:
   `PLN-Kepuasan-Pelanggan-ULP-Tanjung-Redeb.apk`

### Pilihan 2 — GitHub Actions

Ini paling mudah jika tidak ingin setup Android Studio lengkap.

1. Buat repository GitHub kosong.
2. Upload **seluruh isi** folder project ke repository tersebut.
3. Pastikan folder `.github/workflows` ikut terupload.
4. Buka tab **Actions**.
5. Pilih workflow:
   **Build APK**
6. Klik:
   **Run workflow**
7. Tunggu proses build selesai.
8. Buka run yang sudah hijau/sukses.
9. Scroll ke bagian **Artifacts**.
10. Download:
    `PLN-Kepuasan-Pelanggan-APK`
11. Extract ZIP artifact.
12. Di dalamnya ada:
    `PLN-Kepuasan-Pelanggan-ULP-Tanjung-Redeb.apk`

---

## 8. Tahap F — Install APK

Pada tablet/HP Android:
1. Kirim file APK ke perangkat.
2. Tekan file APK.
3. Jika muncul blokir:
   masuk ke **Settings → Install unknown apps**.
4. Izinkan aplikasi File Manager/Chrome yang digunakan.
5. Tekan **Install**.
6. Setelah selesai tekan **Open**.

---

## 9. Pengaturan pertama APK

Saat pertama dibuka, APK menampilkan layar setup sederhana.

1. Masukkan URL Web App Google Apps Script yang berakhir `/exec`.
2. Tekan:
   **SIMPAN & MULAI**
3. APK langsung membuka survei.
4. Pengaturan disimpan di perangkat.

Mulai pembukaan berikutnya:
**APK langsung masuk ke survei fullscreen.**

Tidak perlu memasukkan URL lagi.

---

## 10. Mode fullscreen/kiosk

APK otomatis:
- menyembunyikan status bar,
- menyembunyikan navigation bar,
- tidak menampilkan URL,
- tidak menampilkan tab browser,
- menahan tombol Back,
- menjaga layar tetap menyala.

Jadi pelanggan hanya melihat:
- Logo PLN
- Judul survei
- 4 emoticon

---

## 11. Menu Admin tersembunyi

Untuk mengubah URL:

1. Pada halaman survei tekan dan tahan layar sekitar **4 detik**.
2. Masukkan PIN.

PIN default:
**1234**

3. Ubah URL.
4. Tekan **Simpan**.

Di menu yang sama Anda dapat mengganti PIN.

**Untuk penggunaan produksi, ganti PIN 1234.**

---

## 12. Jika salah URL

Tekan dan tahan layar 4 detik → PIN → ubah URL.

Jika ingin kembali ke layar setup awal:
**Pengaturan Admin → Reset Awal**

---

## 13. Jika internet terputus

APK akan menampilkan halaman:
**Koneksi Internet Terputus**

Tersedia tombol:
- **COBA LAGI**
- **PENGATURAN**

Setelah Wi-Fi/internet aktif kembali, tekan **COBA LAGI**.

---

## 14. Tes sebelum dipasang di meja pelayanan

Lakukan tes:

1. Buka APK.
2. Pilih 😄 Sangat Baik.
3. Tunggu pesan Terima Kasih.
4. Buka Spreadsheet.
5. Buka `DATABASE_SURVEI`.
6. Pastikan ada baris baru.
7. Ulangi untuk 🙂, ☹️, 😡.
8. Pastikan dashboard ikut berubah.
9. Tutup APK dan buka kembali.
10. Pastikan APK langsung membuka survei tanpa setup ulang.

---

## 15. Pengaturan tablet yang disarankan

Untuk perangkat khusus survei:
- aktifkan Wi-Fi kantor,
- matikan auto-rotate bila posisi tablet permanen,
- atur brightness secukupnya,
- sambungkan charger permanen dengan aman,
- nonaktifkan notifikasi yang tidak perlu,
- letakkan ikon APK di Home Screen,
- gunakan PIN layar perangkat untuk petugas,
- jangan berikan PIN admin APK kepada pelanggan.

---

## 16. Update Web App tanpa install ulang APK

Keuntungan versi ini: perubahan tampilan Web App dapat dilakukan tanpa membuat APK baru.

Caranya:
1. Edit `Index.html` / `Code.gs` di Apps Script.
2. **Deploy → Manage deployments**.
3. Edit deployment.
4. Pilih **New version**.
5. Deploy.

Jika URL `/exec` tetap sama, APK otomatis memakai versi terbaru.

APK baru hanya dibutuhkan bila kode native Android ingin diubah.

---

## 17. Checklist siap digunakan

Sebelum dipasang di loket, pastikan:

- [ ] Spreadsheet sudah dibuat.
- [ ] Apps Script sudah dipasang.
- [ ] Web App sudah dideploy.
- [ ] URL `/exec` sudah diuji di browser.
- [ ] APK sudah dibuild.
- [ ] APK sudah diinstall.
- [ ] URL sudah disimpan di APK.
- [ ] Penilaian masuk ke DATABASE_SURVEI.
- [ ] Dashboard berjalan.
- [ ] PIN admin sudah diganti.
- [ ] Tablet sudah fullscreen dan layar tetap aktif.

Jika seluruh checklist terpenuhi, aplikasi sudah siap digunakan sebagai mesin survei kepuasan pelanggan.
