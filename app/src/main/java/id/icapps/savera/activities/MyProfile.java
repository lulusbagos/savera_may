package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import id.icapps.savera.GBApplication;
import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.util.ApiUrl;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.ImageUtils;
import id.icapps.savera.util.PendingUploadQueue;
import id.icapps.savera.util.GB;

public class MyProfile extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyProfile.class);
    private static final String TAG = "MyProfile";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_NETWORK_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_BASE_MS = 750L;
    private static final long PROFILE_AUTO_REFRESH_MAX_AGE_MS = 15 * 60 * 1000L;
    private static final int PROFILE_UPLOAD_MAX_DIMENSION = 640;
    private static final int PROFILE_UPLOAD_TARGET_MAX_BYTES = 250 * 1024;
    private static final int PROFILE_UPLOAD_MIN_QUALITY = 55;
    private static final int PROFILE_UPLOAD_INITIAL_QUALITY = 82;
    private TextView textNama, textNik, textDepartemen, textMess, textLocalSyncStatus, textLocalSyncDetail, textUploadQueueStatus, textUploadQueueDetail;
    private ImageView imageProfile;
    private LocalStorage localStorage;
    private int companyId;
    private int employeeId;
    private String employeePhoto;

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_profile, container, false);

        textNama = view.findViewById(R.id.textNama);
        textNik = view.findViewById(R.id.textNik);
        textDepartemen = view.findViewById(R.id.textDepartemen);
        textMess = view.findViewById(R.id.textMess);
        textLocalSyncStatus = view.findViewById(R.id.textLocalSyncStatus);
        textLocalSyncDetail = view.findViewById(R.id.textLocalSyncDetail);
        textUploadQueueStatus = view.findViewById(R.id.textUploadQueueStatus);
        textUploadQueueDetail = view.findViewById(R.id.textUploadQueueDetail);

        if (textLocalSyncStatus != null) {
            textLocalSyncStatus.setVisibility(View.GONE);
        }

        if (textLocalSyncDetail != null) {
            textLocalSyncDetail.setVisibility(View.GONE);
        }
        if (textUploadQueueStatus != null) {
            textUploadQueueStatus.setVisibility(View.GONE);
        }
        if (textUploadQueueDetail != null) {
            textUploadQueueDetail.setVisibility(View.GONE);
        }

        localStorage = new LocalStorage(view.getContext());
        populateEmployeeFromStorage();
        refreshNetworkSyncStatus();
        refreshUploadQueueStatus();

        imageProfile = view.findViewById(R.id.imageProfile);
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage(getActivity());
            }
        });

        showImage();

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        Button btnAbout = view.findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAboutTap();
            }
        });

        Button btnTerms = view.findViewById(R.id.btnTerms);
        btnTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(6);
            }
        });

        Button btnPrivacy = view.findViewById(R.id.btnPrivacy);
        btnPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(7);
            }
        });

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appLogout();
            }
        });

        if (!localStorage.getToken().isEmpty() && shouldRefreshProfileOnOpen()) {
            getProfile();
        }

        return view;
    }

    private void handleAboutTap() {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            ((HomeActivity) activity).changeViewPagerPostition(5);
        }
    }

    private void appLogout() {
        String url = getString(R.string.base_url) + "/logout";

        new Thread(new Runnable() {
            @Override
            public void run() {
                Http http = new Http(requireActivity(), url);
                http.setMethod("post");
                http.setToken(true);
                http.send();

                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            localStorage.setToken("");
                            localStorage.setEmployee("");
                            localStorage.setDevice("");
                            localStorage.setLocalProfilePhotoPath("");
                            localStorage.setAdmin(false);
                            GBApplication.quit();
                        }
                    });
                }
            }
        }).start();
    }

    private void chooseImage(Context context) {
        final CharSequence[] optionsMenu = {"Take Photo", "Choose from Gallery", "Exit"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setItems(optionsMenu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (optionsMenu[i].equals("Take Photo")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, 0);
                } else if (optionsMenu[i].equals("Choose from Gallery")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, 1);
                } else if (optionsMenu[i].equals("Exit")) {
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    private static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();

            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;

            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }

            return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
        } else {
            return image;
        }
    }

    private void saveImage(Bitmap photo) {
        try {
            String fileName = FileUtils.makeValidFileName("Profile-" + textNik.getText() + "-" + (System.currentTimeMillis() / 1000) + ".jpg");
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            Bitmap resizedPhoto = resizeImage(photo, PROFILE_UPLOAD_MAX_DIMENSION, PROFILE_UPLOAD_MAX_DIMENSION);
            byte[] compressed = compressBitmapForUpload(resizedPhoto);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            fOut.write(compressed);
            fOut.flush();
            fOut.close();

            if (targetFile.isFile()) {
                LOG.info("Profile photo saved locally: {} bytes", targetFile.length());
                localStorage.setLocalProfilePhotoPath(targetFile.getAbsolutePath());
                showImage();
                notifyProfileContextUpdated();
                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GB.toast(getActivity(), "Profile picture updated locally", Toast.LENGTH_LONG, GB.INFO);
                        }
                    });
                }
            }

            // GB.toast(getActivity(), "Profile picture saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting profile picture", e);
        }
    }

    private byte[] compressBitmapForUpload(Bitmap photo) {
        int quality = PROFILE_UPLOAD_INITIAL_QUALITY;
        byte[] result = new byte[0];

        while (quality >= PROFILE_UPLOAD_MIN_QUALITY) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            result = outputStream.toByteArray();

            if (result.length <= PROFILE_UPLOAD_TARGET_MAX_BYTES || quality == PROFILE_UPLOAD_MIN_QUALITY) {
                break;
            }

            quality -= 7;
        }

        return result;
    }

    private Bitmap decodeScaledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        if (uri == null || getContext() == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (InputStream boundsStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(boundsStream, null, options);
        } catch (Exception e) {
            LOG.error("Failed reading image bounds from uri", e);
            return null;
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        try (InputStream decodeStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (decodeStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(decodeStream, null, options);
        } catch (Exception e) {
            LOG.error("Failed decoding image from uri", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return Math.max(inSampleSize, 1);
    }

    private void uploadImage(File targetFile, String fileName) {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (URL sUrl : ApiUrl.candidateUrls(getString(R.string.base_url) + "/avatar")) {
                        if (sUrl.getHost() != null && sUrl.getHost().contains("_")) {
                            LOG.warn("Skip invalid upload host: {}", sUrl);
                            continue;
                        }

                        for (int attempt = 1; attempt <= MAX_NETWORK_ATTEMPTS; attempt++) {
                            HttpURLConnection conn = null;
                            FileInputStream fileInputStream = null;
                            DataOutputStream dos = null;
                            try {
                                fileInputStream = new FileInputStream(targetFile);
                                conn = (HttpURLConnection) sUrl.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                                conn.setReadTimeout(READ_TIMEOUT_MS);
                                conn.setUseCaches(false);
                                conn.setChunkedStreamingMode(0);
                                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setRequestProperty("Connection", "Keep-Alive");
                                conn.setRequestProperty("company", localStorage.getCompanyCode());
                                conn.setRequestProperty("Authorization", "Bearer " + localStorage.getToken());
                                conn.setDoOutput(true);

                                dos = new DataOutputStream(conn.getOutputStream());
                                dos.writeBytes(twoHyphens + boundary + lineEnd);
                                dos.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"" + fileName + "\"" + lineEnd);
                                dos.writeBytes(lineEnd);

                                byte[] buffer = new byte[64 * 1024];
                                int bytesRead;
                                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                    dos.write(buffer, 0, bytesRead);
                                }

                                dos.writeBytes(lineEnd);
                                dos.writeBytes(twoHyphens + boundary + lineEnd);
                                dos.writeBytes("Content-Disposition: form-data; name=\"company_id\"" + lineEnd);
                                dos.writeBytes(lineEnd);
                                dos.writeBytes(String.valueOf(companyId));
                                dos.writeBytes(lineEnd);
                                dos.writeBytes(twoHyphens + boundary + lineEnd);
                                dos.writeBytes("Content-Disposition: form-data; name=\"employee_id\"" + lineEnd);
                                dos.writeBytes(lineEnd);
                                dos.writeBytes(String.valueOf(employeeId));
                                dos.writeBytes(lineEnd);
                                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                                dos.flush();

                                int statusCode = conn.getResponseCode();
                                String responseBody = readResponseBody(conn, statusCode);

                                Log.d("HttpURL", "Url: " + sUrl);
                                Log.d("HttpURL", "status: " + statusCode);
                                for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                                    Log.d("HttpURL", header.getKey() + ": " + header.getValue());
                                }
                                if (responseBody != null) {
                                    Log.d("HttpURL", "response: " + responseBody);
                                }

                                if (statusCode >= 200 && statusCode <= 299) {
                                    applyUploadedPhotoToLocalStorage(responseBody, targetFile, fileName);
                                    Activity activity = getActivity();
                                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                GB.toast(getActivity(), "Profile picture uploaded", Toast.LENGTH_LONG, GB.INFO);
                                            }
                                        });
                                    }
                                    runOnUiThreadSafe(MyProfile.this::refreshUploadNotificationState);
                                    getProfile(true);
                                    return;
                                }

                                if (!shouldRetryNetwork(statusCode) || attempt == MAX_NETWORK_ATTEMPTS) {
                                    Activity activity = getActivity();
                                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                GB.toast(getActivity(), "Profile picture upload failed", Toast.LENGTH_LONG, GB.ERROR);
                                            }
                                        });
                                    }
                                    runOnUiThreadSafe(MyProfile.this::showUploadPendingNotification);
                                    return;
                                }
                            } catch (Exception e) {
                                LOG.error("Upload file to server error: ", e);
                                if (attempt == MAX_NETWORK_ATTEMPTS) {
                                    Activity activity = getActivity();
                                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                GB.toast(getActivity(), "Profile picture upload failed", Toast.LENGTH_LONG, GB.ERROR);
                                            }
                                        });
                                    }
                                    runOnUiThreadSafe(MyProfile.this::showUploadPendingNotification);
                                    break;
                                }
                            } finally {
                                if (dos != null) {
                                    try {
                                        dos.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                                if (conn != null) {
                                    conn.disconnect();
                                }
                            }

                            sleepBeforeRetry(attempt);
                        }
                    }
                } catch (MalformedURLException e) {
                    LOG.error("URL file error: ", e);
                }
            }
        }).start();
    }

    private void runOnUiThreadSafe(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(runnable);
        }
    }

    private void showImage() {
        String localPhotoPath = localStorage.getLocalProfilePhotoPath();
        if (!localPhotoPath.isEmpty()) {
            Bitmap localPhoto = ImageUtils.decodeSampledBitmapFromFile(localPhotoPath, 320, 320);
            if (localPhoto != null) {
                imageProfile.setImageBitmap(localPhoto);
                return;
            }
        }

        if (employeePhoto == null || employeePhoto.equals("null")) return;
        try {
            String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            if (targetFile.exists()) {
                Bitmap photo = ImageUtils.decodeSampledBitmapFromFile(targetFile.getPath(), 320, 320);
                imageProfile.setImageBitmap(photo);
            } else {
                downloadImage();
            }
        } catch (IOException e) {
            LOG.error("show image error: ", e);
        }
    }

    private void downloadImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (URL imageUrl : ApiUrl.candidateUrls(getString(R.string.base_url).replace("/api", "/image/") + employeePhoto)) {
                        HttpURLConnection conn = null;
                        for (int attempt = 1; attempt <= MAX_NETWORK_ATTEMPTS; attempt++) {
                            try {
                                conn = (HttpURLConnection) imageUrl.openConnection();
                                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                                conn.setReadTimeout(READ_TIMEOUT_MS);
                                conn.setUseCaches(false);

                                int statusCode = conn.getResponseCode();
                                if (statusCode < 200 || statusCode > 299) {
                                    if (!shouldRetryNetwork(statusCode) || attempt == MAX_NETWORK_ATTEMPTS) {
                                        LOG.error("download image error: unexpected status {}", statusCode);
                                        break;
                                    }
                                    sleepBeforeRetry(attempt);
                                    continue;
                                }

                                Bitmap photo = ImageUtils.decodeSampledBitmapFromStream(conn.getInputStream(), 320, 320);
                                if (photo == null) {
                                    break;
                                }

                                String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
                                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
                                FileOutputStream fOut = new FileOutputStream(targetFile);
                                photo.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                                fOut.flush();
                                fOut.close();
                                break;
                            } catch (Exception e) {
                                LOG.error("download image error: ", e);
                                if (attempt == MAX_NETWORK_ATTEMPTS) {
                                    break;
                                }
                                sleepBeforeRetry(attempt);
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                    conn = null;
                                }
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    LOG.error("download image URL error: ", e);
                }

                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            try {
                                String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
                                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
                                Bitmap photo = ImageUtils.decodeSampledBitmapFromFile(targetFile.getPath(), 320, 320);
                                imageProfile.setImageBitmap(photo);
                            } catch (IOException e) {
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private boolean shouldRetryNetwork(int statusCode) {
        return statusCode == 408 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void sleepBeforeRetry(int attempt) {
        long delay = RETRY_BACKOFF_BASE_MS * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String readResponseBody(HttpURLConnection conn, int statusCode) throws IOException {
        InputStreamReader isr;
        if (statusCode >= 200 && statusCode <= 299) {
            isr = new InputStreamReader(conn.getInputStream());
        } else if (conn.getErrorStream() != null) {
            isr = new InputStreamReader(conn.getErrorStream());
        } else {
            return null;
        }

        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private void applyUploadedPhotoToLocalStorage(String responseBody, File sourceFile, String uploadedFileName) {
        String uploadedPhotoPath = extractUploadedPhotoPath(responseBody, uploadedFileName);
        if (uploadedPhotoPath == null || uploadedPhotoPath.isBlank()) {
            return;
        }

        try {
            String employee = localStorage.getEmployee();
            if (employee == null || employee.isBlank()) {
                return;
            }

            JSONObject employeeJson = new JSONObject(employee);
            employeeJson.put("photo", uploadedPhotoPath);
            localStorage.setEmployee(employeeJson.toString());

            cacheUploadedPhotoLocally(sourceFile, uploadedPhotoPath);
            employeePhoto = uploadedPhotoPath;
            runOnUiThreadSafe(() -> {
                populateEmployeeFromStorage();
                showImage();
            });
            notifyProfileContextUpdated();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update local employee photo after upload", e);
        }
    }

    private String extractUploadedPhotoPath(String responseBody, String uploadedFileName) {
        String fallbackPath = normalizePhotoPath("avatar/" + uploadedFileName);
        if (responseBody == null || responseBody.isBlank()) {
            return fallbackPath;
        }

        try {
            JSONObject response = new JSONObject(responseBody);

            String direct = normalizePhotoPath(response.optString("photo", ""));
            if (!direct.isEmpty()) {
                return direct;
            }

            JSONObject employee = response.optJSONObject("employee");
            if (employee != null) {
                String employeePhotoPath = normalizePhotoPath(employee.optString("photo", ""));
                if (!employeePhotoPath.isEmpty()) {
                    return employeePhotoPath;
                }
            }

            JSONObject data = response.optJSONObject("data");
            if (data != null) {
                String dataPhoto = normalizePhotoPath(data.optString("photo", ""));
                if (!dataPhoto.isEmpty()) {
                    return dataPhoto;
                }

                JSONObject dataEmployee = data.optJSONObject("employee");
                if (dataEmployee != null) {
                    String dataEmployeePhoto = normalizePhotoPath(dataEmployee.optString("photo", ""));
                    if (!dataEmployeePhoto.isEmpty()) {
                        return dataEmployeePhoto;
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Could not parse upload response for photo path", e);
        }

        return fallbackPath;
    }

    private String normalizePhotoPath(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return "";
        }

        return normalized;
    }

    private void cacheUploadedPhotoLocally(File sourceFile, String uploadedPhotoPath) {
        try {
            String fileName = FileUtils.makeValidFileName(uploadedPhotoPath.replace("avatar/", ""));
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);

            if (sourceFile.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                return;
            }

            FileInputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.close();
            in.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed caching uploaded profile photo locally", e);
        }
    }

    private void notifyProfileContextUpdated() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Intent intent = new Intent(MyDashboard.ACTION_PROFILE_CONTEXT_UPDATED);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    private void getProfile() {
        List<String> companyCandidates = resolveCompanyCandidates();
        getProfile(false, companyCandidates, 0, true);
    }

    private void getProfile(boolean bypassCache) {
        List<String> companyCandidates = resolveCompanyCandidates();
        getProfile(bypassCache, companyCandidates, 0, true);
    }

    private void getProfile(boolean bypassCache, List<String> companyCandidates, int companyIndex, boolean includeCompanyHeader) {
        String url = getString(R.string.base_url) + "/profile";

        new Thread(new Runnable() {
            @Override
            public void run() {
                Http http = new Http(requireActivity(), url);
                http.setMethod("get");
                http.setToken(true);
                http.setCacheTtlSeconds(900);
                http.setBypassCache(bypassCache);
                http.setIncludeCompanyHeader(includeCompanyHeader);
                String companyHeader = companyCandidates.get(companyIndex);
                Log.d(TAG, "Profile request attempt: includeCompanyHeader=" + includeCompanyHeader + ", company=" + companyHeader + ", bypassCache=" + bypassCache + ", idx=" + companyIndex + "/" + (companyCandidates.size() - 1));
                if (includeCompanyHeader && !companyHeader.isEmpty()) {
                    http.setCompanyHeaderValue(companyHeader);
                }
                http.send();

                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            Integer code = http.getStatusCode();
                            Log.d(TAG, "Profile response code=" + code + ", company=" + companyHeader + ", includeCompanyHeader=" + includeCompanyHeader + ", body=" + http.getResponse());
                            if (code == 200) {
                                try {
                                    JSONObject response = new JSONObject(http.getResponse());
                                    applyProfileContext(response, companyHeader);
                                    populateEmployeeFromStorage();
                                    showImage();
                                    refreshNetworkSyncStatus();
                                    refreshUploadQueueStatus();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else if (code == 422 || code == 401 || code == 404) {
                                try {
                                    JSONObject response = new JSONObject(http.getResponse());
                                    String msg = response.getString("message");

                                    if (containsCompanyNotFound(msg) && companyIndex + 1 < companyCandidates.size()) {
                                        getProfile(bypassCache, companyCandidates, companyIndex + 1, true);
                                        return;
                                    }

                                    if (containsCompanyNotFound(msg) && includeCompanyHeader) {
                                        // Final fallback for deployments that resolve tenant from token.
                                        getProfile(bypassCache, companyCandidates, companyIndex, false);
                                        return;
                                    }

                                    if (containsCompanyNotFound(msg)) {
                                        getProfileFromDeviceFallback(companyCandidates, 0);
                                        return;
                                    }

                                    toast(requireActivity(), msg, Toast.LENGTH_SHORT, GB.ERROR);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                refreshNetworkSyncStatus();
                                refreshUploadQueueStatus();
                            } else {
                                toast(requireActivity(), "Error get user profile", Toast.LENGTH_SHORT, GB.ERROR);
                                refreshNetworkSyncStatus();
                                refreshUploadQueueStatus();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private List<String> resolveCompanyCandidates() {
        LinkedHashSet<String> companies = new LinkedHashSet<>();

        String stored = localStorage.getStoredCompanyCode();
        if (stored != null) {
            companies.add(stored.trim());
        }

        String configured = localStorage.getConfiguredCompanyCode();
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
            resolved.add(value.trim().toUpperCase(Locale.ROOT));
        }

        if (resolved.isEmpty()) {
            resolved.add("");
        }

        return resolved;
    }

    private boolean containsCompanyNotFound(String message) {
        if (message == null) {
            return false;
        }

        return message.toLowerCase(Locale.ROOT).contains("company not found");
    }

    private void applyProfileContext(JSONObject response, String companyHeader) throws JSONException {
        if (companyHeader != null && !companyHeader.trim().isEmpty()) {
            localStorage.setCompanyCode(companyHeader);
        }
        if (response.has("employee") && !response.get("employee").toString().equals("null")) {
            localStorage.setEmployee(response.get("employee").toString());
        }
        if (response.has("device") && !response.get("device").toString().equals("null")) {
            JSONObject jsonDevice = new JSONObject(response.get("device").toString());
            localStorage.setDevice(jsonDevice.optString("mac_address", localStorage.getDevice()));
        }
        if (response.has("shift") && !response.get("shift").toString().equals("null")) {
            localStorage.setShift(response.get("shift").toString());
        }
        if (response.has("is_admin") && response.getInt("is_admin") == 1) {
            localStorage.setAdmin(true);
        }
        localStorage.markUserContextSynced();
    }

    private void populateEmployeeFromStorage() {
        companyId = 0;
        employeeId = 0;
        employeePhoto = null;

        String employee = localStorage.getEmployee();
        if (employee == null || employee.isBlank()) {
            return;
        }

        try {
            JSONObject jsonEmployee = new JSONObject(employee);
            textNama.setText(getEmployeeField(jsonEmployee, "fullname", "-"));
            textNik.setText(getEmployeeField(jsonEmployee, "code", "-"));
            textDepartemen.setText(getEmployeeField(jsonEmployee, "department_name", "-"));
            textMess.setText(getEmployeeField(jsonEmployee, "mess_name", "-"));
            if (jsonEmployee.has("company_id") && !jsonEmployee.getString("company_id").equals("null")) {
                companyId = jsonEmployee.getInt("company_id");
            }
            if (jsonEmployee.has("id") && !jsonEmployee.getString("id").equals("null")) {
                employeeId = jsonEmployee.getInt("id");
            }
            String profilePhoto = getEmployeeField(jsonEmployee, "photo", "");
            if (!profilePhoto.isEmpty()) {
                employeePhoto = profilePhoto;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse stored employee context", e);
        }
    }

    private String getEmployeeField(JSONObject employee, String field, String fallback) {
        if (employee == null) {
            return fallback;
        }

        String value = employee.optString(field, fallback);
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return fallback;
        }

        return normalized;
    }

    private void getProfileFromDeviceFallback(List<String> companyCandidates, int companyIndex) {
        String macAddress = resolveBoundDeviceMac();
        if (macAddress.isEmpty()) {
            toast(requireActivity(), "Profile backend belum cocok dan MAC device belum tersedia.", Toast.LENGTH_LONG, GB.ERROR);
            refreshNetworkSyncStatus();
            refreshUploadQueueStatus();
            return;
        }

        String companyHeader = companyCandidates.get(companyIndex);
        String normalizedMacAddress = macAddress.trim().toUpperCase(Locale.ROOT);
        String url = getString(R.string.base_url) + "/device/" + normalizedMacAddress;

        new Thread(() -> {
            Http http = new Http(requireActivity(), url);
            http.setMethod("get");
            http.setToken(true);
            http.setIncludeCompanyHeader(!companyHeader.isEmpty());
            http.setBypassCache(true);
            if (!companyHeader.isEmpty()) {
                http.setCompanyHeaderValue(companyHeader);
            }
            http.send();

            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    Integer code = http.getStatusCode();
                    Log.d(TAG, "Device profile fallback code=" + code + ", company=" + companyHeader + ", mac=" + normalizedMacAddress + ", body=" + http.getResponse());

                    if (code != null && code == 200) {
                        try {
                            JSONObject response = new JSONObject(http.getResponse());
                            JSONObject profileLikeResponse = new JSONObject();
                            profileLikeResponse.put("employee", response.optJSONObject("employee"));
                            profileLikeResponse.put("device", response);
                            applyProfileContext(profileLikeResponse, companyHeader);
                            populateEmployeeFromStorage();
                            showImage();
                            refreshNetworkSyncStatus();
                            refreshUploadQueueStatus();
                            return;
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse device fallback profile", e);
                        }
                    }

                    String message = null;
                    try {
                        String response = http.getResponse();
                        if (response != null && !response.isEmpty()) {
                            message = new JSONObject(response).optString("message", null);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse device fallback error body", e);
                    }

                    if (containsCompanyNotFound(message) && companyIndex + 1 < companyCandidates.size()) {
                        getProfileFromDeviceFallback(companyCandidates, companyIndex + 1);
                        return;
                    }

                    toast(requireActivity(), message == null || message.isEmpty() ? "Gagal memuat profile device." : message, Toast.LENGTH_SHORT, GB.ERROR);
                    refreshNetworkSyncStatus();
                    refreshUploadQueueStatus();
                });
            }
        }).start();
    }

    private String resolveBoundDeviceMac() {
        String storedDevice = localStorage.getDevice();
        if (storedDevice != null) {
            String sanitizedStoredDevice = storedDevice.trim();
            if (!sanitizedStoredDevice.isEmpty() && !"00:00:00:00:00:00".equals(sanitizedStoredDevice)) {
                return sanitizedStoredDevice;
            }
        }

        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (device == null) {
                continue;
            }

            String address = device.getAddress();
            if (address != null) {
                String sanitizedAddress = address.trim();
                if (!sanitizedAddress.isEmpty()) {
                    return sanitizedAddress;
                }
            }
        }

        return "";
    }

    private void refreshNetworkSyncStatus() {
        if (textLocalSyncStatus == null || textLocalSyncDetail == null || getContext() == null) {
            return;
        }

        textLocalSyncStatus.setVisibility(View.GONE);
        textLocalSyncDetail.setVisibility(View.GONE);
        return;
    }

    private boolean shouldRefreshProfileOnOpen() {
        String employee = localStorage.getEmployee();
        return employee == null || employee.isBlank() || !localStorage.hasFreshUserContext(PROFILE_AUTO_REFRESH_MAX_AGE_MS);
    }

    private void showUploadPendingNotification() {
        Context context = getContext();
        if (context != null) {
            GB.updateUploadFailedNotification(buildUploadPendingNotificationText(context), context);
        }
    }

    private void refreshUploadNotificationState() {
        Context context = getContext();
        if (context != null) {
            if (PendingUploadQueue.isEmpty(context)) {
                GB.removeUploadFailedNotification(context);
            } else {
                showUploadPendingNotification();
            }
            refreshUploadQueueStatus();
        }
    }

    private void refreshUploadQueueStatus() {
        if (textUploadQueueStatus == null || textUploadQueueDetail == null || getContext() == null) {
            return;
        }

        textUploadQueueStatus.setVisibility(View.GONE);
        textUploadQueueDetail.setVisibility(View.GONE);
        return;
    }

    private String buildUploadPendingNotificationText(Context context) {
        JSONObject summary = PendingUploadQueue.summary(context);
        return "Sinkronisasi tertunda. Pending "
                + summary.optInt("pending", 0)
                + ", sending "
                + summary.optInt("sending", 0)
                + ", gagal "
                + summary.optInt("failed", 0)
                + ".";
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNetworkSyncStatus();
        refreshUploadQueueStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS:
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    GB.toast(getActivity(), "FlagUp Requires Access to Camara.", Toast.LENGTH_LONG, GB.INFO);
                } else if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    GB.toast(getActivity(), "FlagUp Requires Access to Your Storage.", Toast.LENGTH_LONG, GB.INFO);
                } else {
                    chooseImage(requireContext());
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        imageProfile.setImageBitmap(selectedImage);

                        if (selectedImage != null) {
                            saveImage(selectedImage);
                        }
                    }
                    break;
                case 1:
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        Uri uriImage = data.getData();
                        if (uriImage != null) {
                            Bitmap selectedImage = decodeScaledBitmapFromUri(uriImage, PROFILE_UPLOAD_MAX_DIMENSION, PROFILE_UPLOAD_MAX_DIMENSION);
                            if (selectedImage != null) {
                                imageProfile.setImageBitmap(selectedImage);
                                saveImage(selectedImage);
                            }
                        }
                    }
                    break;
            }
        }
    }
}
