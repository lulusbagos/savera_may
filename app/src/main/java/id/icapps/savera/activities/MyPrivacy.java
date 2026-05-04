package id.icapps.savera.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.icapps.savera.R;

public class MyPrivacy extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyPrivacy.class);
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_privacy, container, false);
        View headerCard = view.findViewById(R.id.headerCard);
        final int baseTopMargin = ((ViewGroup.MarginLayoutParams) headerCard.getLayoutParams()).topMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) headerCard.getLayoutParams();
            layoutParams.topMargin = baseTopMargin + topInset.top;
            headerCard.setLayoutParams(layoutParams);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(4);
            }
        });

        String html = "<html><head>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>"
            + "<style>"
            + "body{margin:0;background:#eef3f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#0f172a;}"
            + ".page{padding:16px 14px 110px 14px;}"
            + ".card{background:#ffffff;border-radius:16px;padding:16px;box-shadow:0 4px 18px rgba(15,23,42,.08);margin-bottom:12px;}"
            + ".badge{display:inline-block;background:#dcfce7;color:#166534;font-size:12px;font-weight:700;padding:5px 10px;border-radius:999px;margin-bottom:10px;}"
            + "h2{font-size:18px;line-height:1.35;margin:0 0 10px 0;color:#0b3a75;}"
            + "h3{font-size:15px;line-height:1.4;margin:0 0 8px 0;color:#102a43;}"
            + "p{font-size:14px;line-height:1.7;margin:0 0 10px 0;color:#1e293b;}"
            + "ul{margin:0 0 10px 18px;padding:0;}"
            + "li{font-size:14px;line-height:1.7;margin-bottom:6px;}"
            + "</style></head><body><div class='page'>"
            + "<div class='card'>"
            + "<span class='badge'>Privacy and Data Governance</span>"
            + "<h2>Kebijakan Privasi Savera</h2>"
            + "<p>Kebijakan Privasi ini berlaku untuk penggunaan Savera, aplikasi resmi yang dibuat dan dikembangkan oleh Department System Integration PT INDEXIM COALINDO. Dengan menggunakan aplikasi, pengguna menyatakan telah membaca, memahami, dan menyetujui ketentuan privasi ini.</p>"
            + "</div>"
            + "<div class='card'><h3>1. Data yang Diproses</h3><p>Savera memproses data operasional yang diperlukan untuk pelaksanaan fungsi aplikasi, termasuk data identitas pengguna, data perangkat, log aktivitas aplikasi, serta data pendukung penilaian kesiapan kerja sesuai kebutuhan operasional perusahaan.</p></div>"
            + "<div class='card'><h3>2. Tujuan Pemrosesan</h3><ul><li>Mendukung proses operasional, keselamatan kerja, dan kepatuhan internal.</li><li>Menjalankan analitik operasional yang sah sesuai mandat manajemen.</li><li>Menjaga integritas sistem, keamanan layanan, dan jejak audit.</li></ul></div>"
            + "<div class='card'><h3>3. Dasar Akses dan Kerahasiaan</h3><p>Akses terhadap data dibatasi secara ketat berdasarkan otorisasi jabatan dan prinsip kebutuhan untuk mengetahui. Setiap akses dan pemrosesan dapat direkam serta ditinjau sebagai bagian dari kontrol internal.</p></div>"
            + "<div class='card'><h3>4. Penyimpanan dan Keamanan</h3><p>PT INDEXIM COALINDO menerapkan kontrol keamanan administratif, teknis, dan operasional untuk melindungi data dari akses tidak sah, perubahan, kebocoran, atau kehilangan data.</p><p>Masa simpan data ditetapkan berdasarkan kebutuhan operasional, kebijakan perusahaan, dan ketentuan hukum yang berlaku.</p></div>"
            + "<div class='card'><h3>5. Larangan Penggunaan Tidak Sah</h3><p>Dilarang melakukan ekstraksi data, duplikasi, redistribusi, manipulasi, atau penggunaan data Savera untuk tujuan di luar mandat operasional tanpa persetujuan tertulis PT INDEXIM COALINDO.</p></div>"
            + "<div class='card'><h3>6. Penegakan dan Sanksi</h3><p>Pelanggaran terhadap kebijakan ini dapat dikenakan tindakan disipliner internal dan/atau tindakan hukum yang mengikat sesuai peraturan perundang-undangan yang berlaku di Republik Indonesia.</p></div>"
            + "<div class='card'><h3>7. Perubahan Kebijakan</h3><p>PT INDEXIM COALINDO berhak memperbarui Kebijakan Privasi ini sewaktu-waktu. Penggunaan berkelanjutan atas Savera setelah pembaruan berarti persetujuan atas kebijakan terbaru.</p></div>"
            + "</div></body></html>";
        webView = view.findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) return;
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}