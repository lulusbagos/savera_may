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

public class MyAbout extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyAbout.class);
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_about, container, false);
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
            + ".badge{display:inline-block;background:#dbeafe;color:#1d4ed8;font-size:12px;font-weight:700;padding:5px 10px;border-radius:999px;margin-bottom:10px;}"
            + "h2{font-size:18px;line-height:1.35;margin:0 0 10px 0;color:#0b3a75;}"
            + "h3{font-size:15px;line-height:1.4;margin:0 0 8px 0;color:#102a43;}"
            + "p{font-size:14px;line-height:1.7;margin:0 0 10px 0;color:#1e293b;}"
            + "ul{margin:0 0 10px 18px;padding:0;}"
            + "li{font-size:14px;line-height:1.7;margin-bottom:6px;}"
            + "</style></head><body><div class='page'>"
            + "<div class='card'>"
            + "<span class='badge'>Official Corporate Product</span>"
            + "<h2>Tentang Savera</h2>"
            + "<p>Savera adalah aplikasi operasional resmi yang dirancang untuk mendukung pemantauan kesiapan kerja berbasis data kesehatan operasional. Produk ini dibuat dan dikembangkan oleh Department System Integration, PT INDEXIM COALINDO.</p>"
            + "<p>Savera telah mendapatkan persetujuan manajemen PT INDEXIM COALINDO mencakup mesin aplikasi (engine), penamaan produk, rancangan antarmuka (UI/UX), arsitektur sistem, serta seluruh komponen digital di dalam aplikasi.</p>"
            + "</div>"
            + "<div class='card'>"
            + "<h3>Ruang Lingkup Penggunaan</h3>"
            + "<ul>"
            + "<li>Digunakan untuk kebutuhan operasional internal dan tata kelola keselamatan kerja.</li>"
            + "<li>Dipakai sesuai prosedur perusahaan, kontrol akses, dan kebijakan teknologi informasi yang berlaku.</li>"
            + "<li>Setiap aktivitas pemakaian dapat dicatat untuk kepentingan audit, keamanan, dan kepatuhan.</li>"
            + "</ul>"
            + "</div>"
            + "<div class='card'>"
            + "<h3>Pernyataan Hak dan Kepemilikan</h3>"
            + "<p>Seluruh hak atas Savera, termasuk engine, source logic, desain UI/UX, struktur data, nama, identitas visual, dokumentasi, dan komponen pendukung lainnya adalah milik PT INDEXIM COALINDO.</p>"
            + "<p>Dilarang keras mengubah, memodifikasi, menyalin, melakukan reverse engineering, menerbitkan ulang, mendistribusikan ulang, atau memanfaatkan komponen Savera untuk kepentingan lain tanpa persetujuan tertulis dari PT INDEXIM COALINDO.</p>"
            + "<p>Pelanggaran terhadap ketentuan ini dapat dikenakan tindakan disipliner internal serta langkah hukum perdata dan/atau pidana sesuai ketentuan peraturan perundang-undangan yang berlaku.</p>"
            + "</div>"
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