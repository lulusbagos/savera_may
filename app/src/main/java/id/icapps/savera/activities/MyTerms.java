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

public class MyTerms extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyTerms.class);
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_terms, container, false);
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
            + ".badge{display:inline-block;background:#fee2e2;color:#991b1b;font-size:12px;font-weight:700;padding:5px 10px;border-radius:999px;margin-bottom:10px;}"
            + "h2{font-size:18px;line-height:1.35;margin:0 0 10px 0;color:#0b3a75;}"
            + "h3{font-size:15px;line-height:1.4;margin:0 0 8px 0;color:#102a43;}"
            + "p{font-size:14px;line-height:1.7;margin:0 0 10px 0;color:#1e293b;}"
            + "ul{margin:0 0 10px 18px;padding:0;}"
            + "li{font-size:14px;line-height:1.7;margin-bottom:6px;}"
            + "</style></head><body><div class='page'>"
            + "<div class='card'>"
            + "<span class='badge'>Legally Binding Terms</span>"
            + "<h2>Syarat dan Ketentuan Savera</h2>"
            + "<p>Savera adalah aplikasi resmi perusahaan yang dibuat dan dikembangkan oleh Department System Integration PT INDEXIM COALINDO dan telah disetujui manajemen PT INDEXIM COALINDO, termasuk engine, nama, UI/UX, serta seluruh komponen aplikasi.</p>"
            + "<p>Dengan mengakses, memasang, atau menggunakan Savera, pengguna menyatakan tunduk pada Syarat dan Ketentuan ini secara penuh dan mengikat.</p>"
            + "</div>"
            + "<div class='card'><h3>1. Kepemilikan Hak</h3><p>Seluruh hak kekayaan intelektual atas Savera adalah milik PT INDEXIM COALINDO. Tidak ada bagian dari Savera yang dapat dialihkan haknya kepada pihak mana pun tanpa persetujuan tertulis perusahaan.</p></div>"
            + "<div class='card'><h3>2. Larangan Tegas</h3><ul><li>Dilarang mengubah, memodifikasi, men-decompile, merekayasa balik, atau mengganggu engine Savera.</li><li>Dilarang menyalin, menerbitkan ulang, menyebarkan ulang, atau menggunakan komponen Savera untuk kebutuhan lain di luar mandat perusahaan.</li><li>Dilarang memindahkan atau mengekspos data, aset visual, dokumen, maupun alur sistem kepada pihak yang tidak berwenang.</li></ul></div>"
            + "<div class='card'><h3>3. Kepatuhan Penggunaan</h3><p>Pengguna wajib menggunakan Savera sesuai kebijakan perusahaan, standar keamanan informasi, dan instruksi operasional yang berlaku. Aktivitas pemakaian dapat dipantau untuk audit dan penegakan kepatuhan.</p></div>"
            + "<div class='card'><h3>4. Penegakan Hukum dan Sanksi</h3><p>Setiap pelanggaran atas ketentuan ini dapat dikenakan sanksi administratif/disipliner internal serta tindakan hukum perdata dan/atau pidana sesuai peraturan perundang-undangan yang berlaku di Republik Indonesia.</p></div>"
            + "<div class='card'><h3>5. Perubahan Ketentuan</h3><p>PT INDEXIM COALINDO berhak memperbarui Syarat dan Ketentuan ini sewaktu-waktu. Penggunaan berkelanjutan atas Savera setelah pembaruan dianggap sebagai penerimaan atas ketentuan terbaru.</p></div>"
            + "<div class='card'><h3>6. Hukum yang Berlaku</h3><p>Syarat dan Ketentuan ini ditafsirkan berdasarkan hukum Republik Indonesia. Setiap sengketa tunduk pada yurisdiksi hukum yang berlaku sesuai ketentuan perusahaan dan peraturan perundang-undangan.</p></div>"
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