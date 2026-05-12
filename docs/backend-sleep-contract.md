# Kontrak Data Tidur Mobile dan Backend

Dokumen ini menjadi acuan agar perhitungan di aplikasi mobile dan web/backend konsisten.

## Definisi Durasi

- `sleep` adalah total tidur efektif dalam menit.
- Tidur efektif = `deep_sleep + light_sleep + rem_sleep`.
- `awake` tidak ikut masuk ke `sleep`.
- `sleep_wearable` adalah total tidur wearable yang ditampilkan sebagai pembanding dari jam.
- `sleep_wearable` mengikuti total durasi tidur dari wearable/jam dan tidak ditambah manual dengan `awake`.
- `rem_sleep` tetap ikut dikirim terpisah agar grafik tidur web tetap lengkap.
- `awake` tetap ikut dikirim terpisah agar grafik tidur web tetap lengkap.

Contoh:

- Deep: 131 menit
- Light: 181 menit
- REM: 83 menit
- Awake: 0 menit
- `sleep` / tidur efektif = 395 menit = 6 jam 35 menit
- `sleep_wearable` = total durasi dari wearable/jam

## Field Payload Mobile

Mobile mengirim field berikut pada upload summary dan sleep snapshot:

- `sleep`: total tidur efektif, tidak termasuk awake.
- `sleep_effective`: sama dengan `sleep`.
- `sleep_effective_minutes`: sama dengan `sleep`.
- `sleep_wearable`: total tidur wearable sesuai angka jam, tidak ditambah manual dengan `awake`.
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

Backend sebaiknya memakai `sleep` atau `sleep_effective_minutes` untuk semua aturan keputusan kerja, Debt Sleep, dan eTicket. Jangan memakai `sleep_wearable` untuk aturan kerja karena nilai itu hanya pembanding angka jam.

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
- `Tidur Efektif Hari Ini` dihitung dari rangkaian tidur utama dikurangi `awake`.
- `Tidur Efektif Kemarin` memakai tidur efektif di luar rangkaian tidur utama, tidak termasuk awake.
- Jika sesi tidur menyambung dari rangkaian tidur utama melewati batas range luar, bagian carryover itu tidak dihitung sebagai `Tidur Efektif Kemarin`.
- Tidur di range luar baru dihitung setelah ada jeda bangun/non-sleep minimal 10 menit.
- Nilai `Tidur Efektif Kemarin` yang tampil adalah total aktual di range luar rangkaian.
- Nilai `Tidur Efektif Kemarin` yang masuk kalkulasi `Total Tidur Efektif` maksimal 1 jam.
- `Total Tidur Efektif` memakai `Tidur Efektif Hari Ini + min(Tidur Efektif Kemarin, 1 jam)`, tidak termasuk awake.
- `Total Sleep Wearable` menampilkan nilai wearable sebagai pembanding jam.

## Range Shift

Mode `night`:

- Rangkaian tidur utama: `18:00 -> 12:00`.
- Range luar rangkaian untuk `Tidur Efektif Kemarin`: `06:00 -> 18:00`.
- Contoh: tidur `18:00 -> 06:00` masuk rangkaian `Tidur Efektif Hari Ini`, bukan `Tidur Efektif Kemarin`.
- Jika tidur berlanjut melewati `06:00`, sisa setelah `06:00` tetap dianggap carryover tidur utama sampai operator bangun minimal 10 menit.

Mode `day` / shift 2:

- Rangkaian tidur utama: `06:00 -> 18:00`.
- Range luar rangkaian untuk `Tidur Efektif Kemarin`: `18:00 -> 06:00`.
- Contoh: tidur normal shift 2 `06:00 -> 18:00` masuk `Tidur Efektif Hari Ini`; tidur tambahan `18:00 -> 06:00` muncul sebagai `Tidur Efektif Kemarin`, tetapi kontribusi kalkulasi tetap maksimal 1 jam.
- Jika tidur berlanjut melewati `18:00`, sisa setelah `18:00` tetap dianggap carryover tidur utama sampai operator bangun minimal 10 menit.

Chart Sleep mobile memakai siklus `18:00 -> 18:00` agar segmen tidur `18:00-23:59` dan `00:00-11:59` mudah dibaca sebagai satu rangkaian.

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
