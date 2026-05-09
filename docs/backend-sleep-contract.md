# Kontrak Data Tidur Mobile dan Backend

Dokumen ini menjadi acuan agar perhitungan di aplikasi mobile dan web/backend konsisten.

## Definisi Durasi

- `sleep` adalah total tidur efektif dalam menit.
- Tidur efektif = `deep_sleep + light_sleep + rem_sleep`.
- `awake` tidak ikut masuk ke `sleep`.
- `sleep_wearable` adalah total tidur wearable yang ditampilkan sebagai pembanding dari jam.
- `sleep_wearable` boleh mencakup awake jika data wearable mengirim awake.
- `rem_sleep` tetap ikut dikirim terpisah agar grafik tidur web tetap lengkap.
- `awake` tetap ikut dikirim terpisah agar grafik tidur web tetap lengkap.

Contoh:

- Deep: 131 menit
- Light: 181 menit
- REM: 83 menit
- Awake: 0 menit
- `sleep` / tidur efektif = 395 menit = 6 jam 35 menit
- `sleep_wearable` = `sleep + awake`

## Field Payload Mobile

Mobile mengirim field berikut pada upload summary dan sleep snapshot:

- `sleep`: total tidur efektif, tidak termasuk awake.
- `sleep_effective`: sama dengan `sleep`.
- `sleep_effective_minutes`: sama dengan `sleep`.
- `sleep_wearable`: total tidur wearable, termasuk awake jika tersedia.
- `sleep_wearable_minutes`: sama dengan `sleep_wearable`.
- `sleep_decision`: keputusan kerja berdasarkan tidur efektif.
- `light_sleep`: light sleep dalam menit.
- `deep_sleep`: deep sleep dalam menit.
- `rem_sleep`: REM dalam menit.
- `awake`: awake dalam menit.
- `sleep_start`: timestamp awal tidur.
- `sleep_end`: timestamp akhir tidur.
- `sleep_type`: `night` atau `day`.
- `user_sleep`: raw detail sesi/stage tidur untuk grafik.

Backend sebaiknya memakai `sleep` atau `sleep_effective_minutes` untuk semua aturan keputusan kerja, Debt Sleep, dan eTicket. Jangan memakai `sleep_wearable` untuk aturan kerja karena nilai itu bisa mencakup awake.

## Aturan Keputusan Kerja

Gunakan `sleep` / `sleep_effective_minutes` dalam menit.

```sql
case
  when cast(s.sleep as float) / 60 < 4.5 then 'Langsung dipulangkan'
  when cast(s.sleep as float) / 60 >= 4.5 and cast(s.sleep as float) / 60 < 5 then 'Istirahat minimal 2 jam (jam savera)'
  when cast(s.sleep as float) / 60 >= 5 and cast(s.sleep as float) / 60 < 5.5 then 'Istirahat minimal 1 jam (jam savera)'
  when cast(s.sleep as float) / 60 >= 5.5 and cast(s.sleep as float) / 60 < 6 then 'Dapat bekerja'
  when cast(s.sleep as float) / 60 >= 6 then 'Langsung bekerja'
end
```

Mobile memakai aturan yang sama:

- `< 270 menit`: Langsung dipulangkan
- `270 - 299 menit`: Istirahat minimal 2 jam (jam savera)
- `300 - 329 menit`: Istirahat minimal 1 jam (jam savera)
- `330 - 359 menit`: Dapat bekerja
- `>= 360 menit`: Langsung bekerja

## Dashboard Mobile

- `Tidur Efektif Hari Ini` memakai tidur efektif hari ini, tidak termasuk awake.
- `Tidur Efektif Kemarin` memakai tidur efektif support/kemarin yang masuk aturan, maksimal 1 jam.
- `Total Tidur Efektif` memakai `Tidur Efektif Hari Ini + Tidur Efektif Kemarin`, tidak termasuk awake.
- `Total Sleep Wearable` menampilkan nilai wearable sebagai pembanding jam, termasuk awake jika ada.

## Debt Sleep

Debt Sleep wajib memakai tidur efektif, bukan wearable total.

Rumus:

- `target = 7 jam = 420 menit`
- `hutang = max(0, target - sleep_effective_minutes)`
- jika `sleep_effective_minutes > target`, hutang tetap `0 menit` dan dapat ditulis sebagai kelebihan.

## eTicket

eTicket wajib menampilkan `Durasi Tidur Efektif`, yaitu nilai `sleep` / `sleep_effective_minutes`.

Jika backend memiliki field lama seperti `sleep`, `sleep_minutes`, atau `total_sleep_minutes`, pastikan nilainya adalah tidur efektif. Jika ingin menampilkan total dari jam, gunakan field terpisah `sleep_wearable`.

## Grafik Web

Untuk grafik web, jangan buang `rem_sleep` dan `awake`.

Backend/web tetap perlu menyimpan dan menampilkan:

- deep sleep
- light sleep
- REM sleep
- awake
- raw `user_sleep`

Tetapi untuk keputusan kerja, Debt Sleep, eTicket, dan status operator, gunakan hanya tidur efektif.
