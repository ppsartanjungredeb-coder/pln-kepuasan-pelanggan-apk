# TUTORIAL V4 STABIL

## Perubahan utama
- Startup lebih aman: fullscreen baru aktif setelah window siap.
- Kegagalan fullscreen tidak menyebabkan crash.
- WebView dibuat dengan fallback pemulihan.
- URL cukup diisi satu kali.
- Menu admin hanya dari pojok kiri atas, tahan 4 detik.
- PIN default 1234.
- Halaman offline native + auto-retry saat internet kembali.
- Tombol Back tidak menutup aplikasi.
- Layar tetap aktif.
- Database tetap Google Spreadsheet melalui Google Apps Script.

## Build lewat GitHub
1. Upload seluruh isi folder V4 ke repository.
2. Pastikan `.github/workflows/build-apk.yml` ada.
3. Actions > **Build APK V4 Stabil**.
4. Run workflow.
5. Pastikan Checkout, Setup Java, Setup Gradle, Build APK, Copy APK, Verify APK, Upload APK semuanya centang.
6. Kembali ke Summary run.
7. Download artifact **PLN-Kepuasan-Pelanggan-V4-STABIL**.
8. Extract dan install `PLN-Kepuasan-Pelanggan-V4-STABIL.apk`.

## Pengaturan pertama
1. Buka APK.
2. Masukkan URL Google Apps Script Web App yang berakhir `/exec`.
3. Tekan **SIMPAN & MULAI**.
4. Pembukaan berikutnya langsung masuk survei fullscreen.

## Menu Admin
1. Tahan pojok kiri atas layar ±4 detik.
2. PIN default `1234`.
3. Ubah URL/PIN bila perlu.
4. Ganti PIN default saat produksi.

## Jika masih crash
1. Update Google Chrome.
2. Update Android System WebView bila tersedia.
3. Restart tablet.
4. Uninstall APK lama.
5. Install V4.
6. Uji **Demo Lokal**.
7. Jika Demo Lokal berjalan tetapi URL produksi gagal, masalah ada pada akses/deployment Apps Script, bukan APK native.

## Tes produksi
1. Tekan 😄.
2. Cek `DATABASE_SURVEI`.
3. Uji semua emoticon.
4. Tutup-buka APK dan pastikan langsung ke survei.
