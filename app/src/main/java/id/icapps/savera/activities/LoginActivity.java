package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import id.icapps.savera.GBApplication;
import id.icapps.savera.Http;
import id.icapps.savera.R;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.util.GB;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText textUser, textPassword;
    private CheckBox rememberMe;
    private TextView textVersion;
    private Button btnLogin;
    private String user, password;
    private boolean activityTrackerAvailable = false;
    private LocalStorage localStorage;
    private ProgressDialog progressDialog;
    private String selectedCompanyHeader = "";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        localStorage = new LocalStorage(LoginActivity.this);

        // Determine availability of device with activity tracking functionality
        activityTrackerAvailable = false;
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsActivityTracking()) {
                activityTrackerAvailable = true;
                break;
            }
        }

        textVersion = findViewById(R.id.textVersion);
        textUser = findViewById(R.id.textUser);
        textPassword = findViewById(R.id.textPassword);
        rememberMe = findViewById(R.id.rememberMe);
        btnLogin = findViewById(R.id.btnLogin);

        if (localStorage.getRememberMe()) {
            rememberMe.setChecked(true);
            textUser.setText(localStorage.getUser());
            textPassword.setText(localStorage.getPassword());
        } else if (localStorage.getUser() != null && !localStorage.getUser().isEmpty()) {
            textUser.setText(localStorage.getUser());
        }

        btnLogin.setOnClickListener(view -> checkLogin());

        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            textVersion.setText("Versi " + info.versionName);
            localStorage.setVersion("Savera X " + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkLogin() {
        user = textUser.getText().toString();
        password = textPassword.getText().toString();
        if (user.isEmpty() || password.isEmpty()) {
            toast(LoginActivity.this, "User and Password are required", Toast.LENGTH_SHORT, GB.ERROR);
        } else {
            // Developer bypass mode - offline login
            if (user.equals("developer") && password.equals("270595")) {
                Log.d(TAG, "Developer mode activated - bypassing server authentication");
                
                // Set complete dummy data for offline mode
                localStorage.setUser(user);
                persistRememberedCredentials(user, password);
                localStorage.setToken("developer_token_offline");
                
                // Complete employee JSON with all required fields
                String employeeJson = "{" +
                    "\"id\":1," +
                    "\"company_id\":1," +
                    "\"code\":\"DEV001\"," +
                    "\"fullname\":\"Developer Mode\"," +
                    "\"email\":\"developer@local\"," +
                    "\"phone\":\"+6281234567890\"," +
                    "\"department_name\":\"Engineering\"," +
                    "\"mess_name\":\"Developer Mess\"," +
                    "\"photo\":null" +
                    "}";
                localStorage.setEmployee(employeeJson);
                
                localStorage.setDevice("00:00:00:00:00:00");
                localStorage.setShift("{\"id\":1,\"name\":\"Developer Shift\"}");
                localStorage.setAdmin(true);
                localStorage.markUserContextSynced();
                
                toast(LoginActivity.this, "Developer mode: Offline login successful", Toast.LENGTH_SHORT, GB.INFO);
                
                // Redirect to HomeActivity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            localStorage.setUser(user);
            persistRememberedCredentials(user, password);
            if (!rememberMe.isChecked()) {
                textPassword.setText("");
            }
            sendLogin();
        }
    }

    private void persistRememberedCredentials(String userValue, String passwordValue) {
        if (rememberMe != null && rememberMe.isChecked()) {
            localStorage.setRememberMe(true);
            localStorage.setUser(userValue == null ? "" : userValue.trim());
            localStorage.setPassword(passwordValue == null ? "" : passwordValue);
        } else {
            localStorage.clearRememberedCredentials();
            localStorage.setUser(userValue == null ? "" : userValue.trim());
            localStorage.setPassword("");
        }
    }

    private void sendLogin() {
        sendLogin(0, false, true);
    }

    private void sendLogin(int companyIndex, boolean useCompanyPrefix, boolean allowCompanyPrefixRetry) {
        forcePublicRouteForAuth();

        List<String> companyCandidates = resolveCompanyCandidates();
        if (companyIndex < 0 || companyIndex >= companyCandidates.size()) {
            toast(LoginActivity.this, "Company not found.", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }
        String companyHeader = companyCandidates.get(companyIndex);

        JSONObject params = new JSONObject();
        try {
            if (!user.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(user).matches()) {
                params.put("email", user);
            } else {
                String loginIdentity = user;
                if (useCompanyPrefix && !companyHeader.isEmpty()) {
                    String companyPrefix = companyHeader;
                    if (!loginIdentity.startsWith(companyPrefix)) {
                        loginIdentity = companyPrefix + loginIdentity;
                    }
                }
                params.put("email", loginIdentity);
            }
            params.put("password", password);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating login params", e);
            toast(LoginActivity.this, "Error preparing login request", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }

        String data = params.toString();
        String url = getString(R.string.base_url) + "/login";
        
        Log.d(TAG, "Starting login to: " + url + " (company=" + companyHeader + ", useCompanyPrefix=" + useCompanyPrefix + ")");

        // Show progress dialog
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                Http http = new Http(LoginActivity.this, url);
                http.setMethod("post");
                http.setData(data);
                http.setUseEndpointResolver(false);
                http.setIncludeCompanyHeader(!companyHeader.isEmpty());
                if (!companyHeader.isEmpty()) {
                    http.setCompanyHeaderValue(companyHeader);
                }
                http.setConnectTimeoutMs(8000);
                http.setReadTimeoutMs(12000);
                http.setMaxAttempts(1);
                http.send();

                runOnUiThread(() -> {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Integer code = http.getStatusCode();
                        Log.d(TAG, "Login response code: " + code);
                        
                        if (code == null || code == 0) {
                            toast(LoginActivity.this, "No response from server. Check your internet connection.", Toast.LENGTH_LONG, GB.ERROR);
                            return;
                        }

                        if (code == 200) {
                            try {
                                String response = http.getResponse();
                                if (response == null || response.isEmpty()) {
                                    toast(LoginActivity.this, "Empty response from server", Toast.LENGTH_SHORT, GB.ERROR);
                                    return;
                                }
                                
                                JSONObject responseObj = new JSONObject(response);
                                String token = responseObj.getString("token");
                                localStorage.setToken(token);
                                String resolvedCompany = extractCompanyFromLoginResponse(responseObj, companyHeader);
                                selectedCompanyHeader = resolvedCompany;
                                if (!resolvedCompany.isEmpty()) {
                                    localStorage.setCompanyCode(resolvedCompany);
                                }
                                Log.d(TAG, "Login successful, getting profile...");

                                getProfile();
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing login response", e);
                                toast(LoginActivity.this, "Invalid server response", Toast.LENGTH_SHORT, GB.ERROR);
                            }
                        } else if (code == 422 || code == 401 || code == 404) {
                            try {
                                String response = http.getResponse();
                                String msg = "Login failed (Error " + code + ")";
                                if (response != null && !response.isEmpty()) {
                                    JSONObject responseObj = new JSONObject(response);
                                    msg = responseObj.optString("message", msg);
                                }

                                if (containsCompanyNotFound(msg)) {
                                    int nextCompanyIndex = companyIndex + 1;
                                    if (nextCompanyIndex < companyCandidates.size()) {
                                        Log.d(TAG, "Company not found for " + companyHeader + ", retrying with next company candidate");
                                        sendLogin(nextCompanyIndex, false, true);
                                        return;
                                    }
                                }

                                if (allowCompanyPrefixRetry && !useCompanyPrefix && !Patterns.EMAIL_ADDRESS.matcher(user).matches() && !companyHeader.isEmpty()) {
                                    Log.d(TAG, "Login rejected for raw username, retrying once with company prefix");
                                    sendLogin(companyIndex, true, false);
                                    return;
                                }

                                toast(LoginActivity.this, msg, Toast.LENGTH_SHORT, GB.ERROR);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing error response", e);
                                toast(LoginActivity.this, "Login failed (Error " + code + ")", Toast.LENGTH_SHORT, GB.ERROR);
                            }
                        } else {
                            toast(LoginActivity.this, "Error user login (HTTP " + code + ")", Toast.LENGTH_SHORT, GB.ERROR);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing login response", e);
                        toast(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT, GB.ERROR);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during login request", e);
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    toast(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR);
                });
            }
        }).start();
    }

    private void getProfile() {
        forcePublicRouteForAuth();

        String url = getString(R.string.base_url) + "/profile";
        
        Log.d(TAG, "Getting profile from: " + url);

        // Show progress dialog
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                Http http = new Http(LoginActivity.this, url);
                http.setMethod("get");
                http.setToken(true);
                http.setCacheTtlSeconds(900);
                http.setUseEndpointResolver(false);
                http.setIncludeCompanyHeader(true);
                http.setConnectTimeoutMs(8000);
                http.setReadTimeoutMs(12000);
                http.setMaxAttempts(1);
                http.send();

                runOnUiThread(() -> {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Integer code = http.getStatusCode();
                        Log.d(TAG, "Profile response code: " + code);
                        
                        if (code == null || code == 0) {
                            toast(LoginActivity.this, "No response from server. Check your internet connection.", Toast.LENGTH_LONG, GB.ERROR);
                            return;
                        }

                        if (code == 200) {
                            try {
                                String response = http.getResponse();
                                if (response == null || response.isEmpty()) {
                                    toast(LoginActivity.this, "Empty profile response", Toast.LENGTH_SHORT, GB.ERROR);
                                    return;
                                }
                                
                                JSONObject responseObj = new JSONObject(response);

                                applyProfileContext(responseObj, selectedCompanyHeader);

                                Log.d(TAG, "Profile loaded successfully, starting HomeActivity");
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing profile response", e);
                                toast(LoginActivity.this, "Invalid profile response", Toast.LENGTH_SHORT, GB.ERROR);
                            }
                        } else if (code == 404) {
                            String response = http.getResponse();
                            String msg = "Failed to get profile (Error 404)";
                            if (response != null && !response.isEmpty()) {
                                try {
                                    JSONObject responseObj = new JSONObject(response);
                                    msg = responseObj.optString("message", msg);
                                } catch (JSONException ignored) {
                                }
                            }

                            if (containsCompanyNotFound(msg)) {
                                Log.w(TAG, "Profile rejected by backend; live API did not resolve user profile from database");
                                toast(LoginActivity.this, "Login berhasil, tetapi API production belum mengembalikan profile user dari database.", Toast.LENGTH_LONG, GB.ERROR);
                                return;
                            }

                            // Keep legacy behavior only for deployments where profile endpoint is intentionally unavailable.
                            Log.w(TAG, "Profile endpoint returned 404 (non-company case), continuing to HomeActivity with token-only session");
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else if (code == 422 || code == 401) {
                            try {
                                String response = http.getResponse();
                                if (response != null && !response.isEmpty()) {
                                    JSONObject responseObj = new JSONObject(response);
                                    String msg = responseObj.getString("message");
                                    toast(LoginActivity.this, msg, Toast.LENGTH_SHORT, GB.ERROR);
                                } else {
                                    toast(LoginActivity.this, "Failed to get profile (Error " + code + ")", Toast.LENGTH_SHORT, GB.ERROR);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing error response", e);
                                toast(LoginActivity.this, "Failed to get profile (Error " + code + ")", Toast.LENGTH_SHORT, GB.ERROR);
                            }
                        } else {
                            toast(LoginActivity.this, "Error get user profile (HTTP " + code + ")", Toast.LENGTH_SHORT, GB.ERROR);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing profile response", e);
                        toast(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT, GB.ERROR);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during profile request", e);
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    toast(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR);
                });
            }
        }).start();
    }

    private void forcePublicRouteForAuth() {
        String publicBaseUrl = getString(R.string.base_url_public);
        localStorage.setApiPreferredRoute("public");
        localStorage.setApiActiveBaseUrl(publicBaseUrl);
    }

    private List<String> resolveCompanyCandidates() {
        LinkedHashSet<String> companies = new LinkedHashSet<>();
        String persisted = localStorage.getStoredCompanyCode();
        if (persisted != null) {
            companies.add(persisted.trim());
        }
        String configured = getString(R.string.base_company);
        if (configured != null) {
            companies.add(configured.trim());
        }
        companies.add("INDEXIM");
        companies.add("SAVERA");
        companies.add("UDU");
        companies.add("");

        List<String> resolved = new ArrayList<>();
        for (String value : companies) {
            if (value == null) {
                continue;
            }
            resolved.add(value.trim());
        }
        return resolved;
    }

    private String extractCompanyFromLoginResponse(JSONObject responseObj, String fallback) {
        String[] directKeys = new String[]{"company", "company_code", "tenant", "tenant_code"};
        for (String key : directKeys) {
            String value = responseObj.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value.toUpperCase(Locale.ROOT);
            }
        }

        JSONObject employeeObj = responseObj.optJSONObject("employee");
        if (employeeObj != null) {
            for (String key : directKeys) {
                String value = employeeObj.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value.toUpperCase(Locale.ROOT);
                }
            }

            JSONObject companyObj = employeeObj.optJSONObject("company");
            if (companyObj != null) {
                for (String key : new String[]{"code", "company_code", "tenant_code", "name"}) {
                    String value = companyObj.optString(key, "").trim();
                    if (!value.isEmpty()) {
                        return value.toUpperCase(Locale.ROOT);
                    }
                }
            }
        }

        return fallback == null ? "" : fallback.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsCompanyNotFound(String message) {
        if (message == null) {
            return false;
        }
        return message.toLowerCase(Locale.ROOT).contains("company not found");
    }

    private void applyProfileContext(JSONObject responseObj, String companyHeader) throws JSONException {
        if (companyHeader != null && !companyHeader.trim().isEmpty()) {
            localStorage.setCompanyCode(companyHeader);
        }
        if (responseObj.has("employee") && !responseObj.get("employee").toString().equals("null")) {
            localStorage.setEmployee(responseObj.get("employee").toString());
        }
        if (responseObj.has("device") && !responseObj.get("device").toString().equals("null")) {
            JSONObject jsonDevice = new JSONObject(responseObj.get("device").toString());
            localStorage.setDevice(jsonDevice.optString("mac_address", localStorage.getDevice()));
        }
        if (responseObj.has("shift") && !responseObj.get("shift").toString().equals("null")) {
            localStorage.setShift(responseObj.get("shift").toString());
        }
        if (responseObj.has("is_admin") && responseObj.getInt("is_admin") == 1) {
            localStorage.setAdmin(true);
        }
        localStorage.markUserContextSynced();
    }

}
