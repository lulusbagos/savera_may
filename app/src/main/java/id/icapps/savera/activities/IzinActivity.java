package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import id.icapps.savera.GBApplication;
import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.GB;

public class IzinActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(IzinActivity.class);
    private TextView textNama, textNik, textDepartemen, textShift, textType, textDetail, textDate;
    private EditText textPhone, textNote;
    private RadioButton btnJenis1, btnJenis2, btnJenis3;
    private ImageButton btnOpen;
    private LinearLayout my_izin_form, my_izin_print;
    private LocalStorage localStorage;
    private int companyId;
    private int employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_izin);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        textNama = findViewById(R.id.textNama);
        textNik = findViewById(R.id.textNik);
        textDepartemen = findViewById(R.id.textDepartemen);
        textShift = findViewById(R.id.textShift);
        textType = findViewById(R.id.textType);
        textDetail = findViewById(R.id.textDetail);
        textDate = findViewById(R.id.textDate);
        textPhone = findViewById(R.id.textPhone);
        textNote = findViewById(R.id.textNote);

        btnJenis1 = findViewById(R.id.btnJenis1);
        btnJenis2 = findViewById(R.id.btnJenis2);
        btnJenis3 = findViewById(R.id.btnJenis3);

        my_izin_form = findViewById(R.id.my_izin_form);
        my_izin_print = findViewById(R.id.my_izin_print);

        localStorage = new LocalStorage(IzinActivity.this);
        if (!localStorage.getEmployee().isEmpty() && !localStorage.getEmployee().isBlank()) {
            try {
                JSONObject jsonEmployee = new JSONObject(localStorage.getEmployee());
                if (jsonEmployee.has("fullname") && !jsonEmployee.getString("fullname").equals("null")) {
                    textNama.setText(jsonEmployee.getString("fullname"));
                }
                if (jsonEmployee.has("code") && !jsonEmployee.getString("code").equals("null")) {
                    textNik.setText(jsonEmployee.getString("code"));
                }
                if (jsonEmployee.has("department_name") && !jsonEmployee.getString("department_name").equals("null")) {
                    textDepartemen.setText(jsonEmployee.getString("department_name"));
                }
                if (jsonEmployee.has("phone") && !jsonEmployee.getString("phone").equals("null")) {
                    textPhone.setText(jsonEmployee.getString("phone").replaceAll("[^\\d.]", ""));
                }
                if (jsonEmployee.has("id") && !jsonEmployee.getString("id").equals("null")) {
                    companyId = jsonEmployee.getInt("company_id");
                    employeeId = jsonEmployee.getInt("id");
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(view -> submitIzin());

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btnOpen = findViewById(R.id.btnOpen);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(IzinActivity.this, true);
            }
        });

        ImageButton btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(IzinActivity.this, true);
            }
        });

        ImageButton btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(IzinActivity.this, false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    private void submitIzin() {
        btnOpen.setVisibility(View.GONE);
        my_izin_print.setVisibility(View.GONE);
        my_izin_print.setAlpha(0.0f);

        JSONObject params = new JSONObject();
        try {
            String type = "";
            if (btnJenis1.isChecked()) {
                type = btnJenis1.getText().toString();
            } else if (btnJenis2.isChecked()) {
                type = btnJenis2.getText().toString();
            } else if (btnJenis3.isChecked()) {
                type = btnJenis3.getText().toString();
            }
            params.put("employee_id", employeeId);
            params.put("type", type);
            params.put("phone", textPhone.getText().toString());
            params.put("note", textNote.getText().toString());

            textType.setText(type);
            textDetail.setText(textNote.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = getString(R.string.base_url) + "/leave";

        new Thread(() -> {
            Http http = new Http(IzinActivity.this, url);
            http.setMethod("post");
            http.setData(params.toString());
            http.setToken(true);
            http.send();

            runOnUiThread(() -> {
                Integer code = http.getStatusCode();
                if (code == 200) {
                    try {
                        JSONObject response = new JSONObject(http.getResponse());
                        JSONObject data = response.optJSONObject("data");
                        if (data == null) {
                            data = new JSONObject(response.get("data").toString());
                        }
                        if (data.has("shift") && !data.getString("shift").equals("null")) {
                            textShift.setText(data.getString("shift"));
                        }
                        if (data.has("date") && !data.getString("date").equals("null")) {
                            textDate.setText(data.getString("date"));
                        }
                        my_izin_print.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(500)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        btnOpen.setVisibility(View.VISIBLE);
                                        my_izin_print.setVisibility(View.VISIBLE);
                                        my_izin_form.setVisibility(View.GONE);
                                    }
                                });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (code == 422 || code == 401 || code == 404) {
                    try {
                        JSONObject response = new JSONObject(http.getResponse());
                        String msg = response.getString("message");
                        toast(IzinActivity.this, msg, Toast.LENGTH_SHORT, GB.ERROR);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    toast(IzinActivity.this, "Error get user izin", Toast.LENGTH_SHORT, GB.ERROR);
                }
            });
        }).start();
    }

    private void take_share_screenshot(Context context, boolean share) {
        final NestedScrollView layout = findViewById(R.id.my_izin);
        final LinearLayout layoutInner = findViewById(R.id.my_izin_inner);
        int width = layoutInner.getWidth();
        int height = layout.getHeight() - 370;
        Bitmap screenShot = getScreenShot(layoutInner, width, height, context);
        String fileName = FileUtils.makeValidFileName("Screenshot-" + "Izin-" + DateTimeUtils.formatIso8601(new Date(Calendar.getInstance().getTimeInMillis())) + ".png");

        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            if (share) {
                shareScreenshot(targetFile, context);
            }
            GB.toast(this, "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        context.grantUriPermission(
                context.getApplicationContext().getPackageName(),
                contentUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.setType("image/*");
        String shareBody = "Pengajuan Izin - " + textDate.getText() + " - " + textNik.getText() + " - " + textNama.getText();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.step_streaks_achievements_sharing_title));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show();
        }
    }

    private static Bitmap getScreenShot(View view, int width, int height, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(context));
        view.draw(canvas);
        return bitmap;
    }
}
