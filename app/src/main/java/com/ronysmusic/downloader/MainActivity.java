package com.ronysmusic.downloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int STORAGE_PERMISSION_REQUEST = 4102;
    private WebView web;
    private EditText address;
    private TextView status;
    private DownloadManager downloads;
    private String pendingDownloadUrl = "";
    private String detectedAudioUrl = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        downloads = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        buildUi();
        configureWebView();
        if (state == null) web.loadUrl("https://suno.com");
        else web.restoreState(state);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(10, 10, 10, 6);
        page.setBackgroundColor(Color.rgb(14, 19, 29));

        TextView title = new TextView(this);
        title.setText("🎵 Rony's Music Downloader");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(8, 8, 8, 12);
        page.addView(title);

        LinearLayout row = new LinearLayout(this);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setText("https://suno.com");
        address.setHint("Paste a direct HTTPS MP3/WAV URL");
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.LTGRAY);
        row.addView(address, new LinearLayout.LayoutParams(0, 52, 1));

        Button paste = new Button(this);
        paste.setText("Paste");
        row.addView(paste);
        Button go = new Button(this);
        go.setText("Go");
        row.addView(go);
        page.addView(row);

        LinearLayout actions = new LinearLayout(this);
        Button home = new Button(this);
        home.setText("Suno");
        Button download = new Button(this);
        download.setText("Download URL");
        Button detected = new Button(this);
        detected.setText("Use detected");
        actions.addView(home, new LinearLayout.LayoutParams(0, 52, 1));
        actions.addView(download, new LinearLayout.LayoutParams(0, 52, 1));
        actions.addView(detected, new LinearLayout.LayoutParams(0, 52, 1));
        page.addView(actions);

        status = new TextView(this);
        status.setTextColor(Color.rgb(90, 210, 190));
        status.setPadding(8, 8, 8, 8);
        status.setText("Paste a direct HTTPS MP3/WAV file URL to download.");
        page.addView(status);

        web = new WebView(this);
        page.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);

        paste.setOnClickListener(v -> pasteFromClipboard());
        go.setOnClickListener(v -> navigate());
        home.setOnClickListener(v -> {
            address.setText("https://suno.com");
            web.loadUrl("https://suno.com");
        });
        download.setOnClickListener(v -> startDownload(address.getText().toString().trim()));
        detected.setOnClickListener(v -> {
            if (detectedAudioUrl.isEmpty()) {
                status.setText("No direct MP3/WAV URL detected. Paste a direct audio URL.");
            } else {
                address.setText(detectedAudioUrl);
                status.setText("Detected link filled in. Review it, then tap Download URL.");
            }
        });
    }

    private void configureWebView() {
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                return scheme == null || !(scheme.equalsIgnoreCase("http") ||
                        scheme.equalsIgnoreCase("https"));
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isDirectAudioUrl(url)) {
                    detectedAudioUrl = url;
                    runOnUiThread(() -> status.setText(
                            "Direct MP3/WAV link detected. Tap Use detected, then Download URL."));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> {
            if (isDirectAudioUrl(url)) {
                detectedAudioUrl = url;
                address.setText(url);
                status.setText("Direct audio URL received. Review it and tap Download URL.");
            } else {
                status.setText("Unsupported link. A direct HTTPS .mp3 or .wav URL is required.");
            }
        });
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence value = clip.getItemAt(0).coerceToText(this);
                if (value != null) {
                    address.setText(value.toString().trim());
                    status.setText("Link pasted. Tap Download URL.");
                    return;
                }
            }
        }
        status.setText("Clipboard is empty.");
    }

    private void navigate() {
        String value = address.getText().toString().trim();
        String url = value.contains("://") ? value : "https://" + value;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (uri.getHost() == null || scheme == null ||
                !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            status.setText("Enter a valid HTTP or HTTPS website address.");
            return;
        }
        Object service = getSystemService(INPUT_METHOD_SERVICE);
        if (service instanceof InputMethodManager) {
            ((InputMethodManager) service).hideSoftInputFromWindow(address.getWindowToken(), 0);
        }
        web.loadUrl(url);
    }

    private boolean isDirectAudioUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String path = uri.getPath();
            if (scheme == null || !scheme.equalsIgnoreCase("https") ||
                    uri.getHost() == null || path == null) return false;
            String lower = path.toLowerCase(Locale.ROOT);
            return lower.endsWith(".mp3") || lower.endsWith(".wav");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void startDownload(String value) {
        if (!isDirectAudioUrl(value)) {
            status.setText("Enter a direct HTTPS URL ending in .mp3 or .wav. Webpages and streams are not supported.");
            return;
        }
        if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingDownloadUrl = value;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
            status.setText("Storage permission is needed on Android 9 and older.");
            return;
        }
        enqueueDownload(value);
    }

    private void enqueueDownload(String value) {
        try {
            Uri uri = Uri.parse(value);
            String path = uri.getPath().toLowerCase(Locale.ROOT);
            String mime = path.endsWith(".wav") ? "audio/wav" : "audio/mpeg";
            String name = URLUtil.guessFileName(value, null, mime);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(name);
            request.setDescription("Rony's Music Downloader");
            request.setMimeType(mime);
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MUSIC, "RonysMusicDownloader/" + name);
            long id = downloads.enqueue(request);
            status.setText("Download started. ID: " + id +
                    "\\nSaved in Music/RonysMusicDownloader/");
        } catch (Exception e) {
            status.setText("Download could not start: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED &&
                    !pendingDownloadUrl.isEmpty()) {
                String url = pendingDownloadUrl;
                pendingDownloadUrl = "";
                enqueueDownload(url);
            } else {
                pendingDownloadUrl = "";
                status.setText("Storage permission denied; download not started.");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (web != null) web.saveState(state);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
