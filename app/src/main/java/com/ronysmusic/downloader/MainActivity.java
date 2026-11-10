package com.ronysmusic.downloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button actionButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setMinHeight(dp(54));
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setPadding(dp(6), dp(6), dp(6), dp(6));
        return button;
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(8), dp(8), dp(8), dp(4));
        page.setBackgroundColor(Color.rgb(14, 19, 29));

        TextView title = new TextView(this);
        title.setText("🎵 Rony's Music Downloader");
        title.setTextColor(Color.WHITE);
        title.setTextSize(19);
        title.setPadding(dp(8), dp(8), dp(8), dp(10));
        page.addView(title);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setText("https://suno.com");
        address.setHint("Paste MP3/WAV URL or website");
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.LTGRAY);
        row.addView(address, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button paste = actionButton("PASTE", Color.rgb(65, 76, 96));
        row.addView(paste, new LinearLayout.LayoutParams(dp(88), dp(52)));
        Button go = actionButton("GO", Color.rgb(65, 76, 96));
        row.addView(go, new LinearLayout.LayoutParams(dp(72), dp(52)));
        page.addView(row);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setMinimumHeight(dp(60));
        Button detected = actionButton("USE DETECTED", Color.rgb(0, 126, 116));
        Button download = actionButton("DOWNLOAD AUDIO", Color.rgb(26, 120, 190));
        Button home = actionButton("SUNO HOME", Color.rgb(65, 76, 96));
        LinearLayout.LayoutParams actionParams =
                new LinearLayout.LayoutParams(0, dp(58), 1);
        actionParams.setMargins(dp(2), dp(3), dp(2), dp(3));
        actions.addView(detected, new LinearLayout.LayoutParams(actionParams));
        actions.addView(download, new LinearLayout.LayoutParams(actionParams));
        actions.addView(home, new LinearLayout.LayoutParams(actionParams));
        page.addView(actions, new LinearLayout.LayoutParams(-1, dp(64)));

        status = new TextView(this);
        status.setTextColor(Color.rgb(90, 220, 195));
        status.setTextSize(13);
        status.setPadding(dp(8), dp(5), dp(8), dp(5));
        status.setText("Open a song in Suno. If its direct audio file is detected, tap USE DETECTED, then DOWNLOAD AUDIO.");
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
                status.setText("No direct audio file detected yet. Open/play the song or paste its direct MP3/WAV URL.");
            } else {
                address.setText(detectedAudioUrl);
                status.setText("Audio link selected. Now tap DOWNLOAD AUDIO.");
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
                            "Direct MP3/WAV file detected. Tap USE DETECTED, then DOWNLOAD AUDIO."));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> {
            if (isDirectAudioUrl(url)) {
                detectedAudioUrl = url;
                address.setText(url);
                status.setText("Direct audio file received. Tap DOWNLOAD AUDIO.");
            } else {
                status.setText("This is not a direct MP3/WAV file link. Paste a direct audio URL instead.");
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
                    status.setText("Link pasted. Tap DOWNLOAD AUDIO.");
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
            status.setText("No direct audio URL in the box. Use USE DETECTED after a file is detected, or paste a direct HTTPS .mp3/.wav URL.");
            return;
        }
        if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingDownloadUrl = value;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
            status.setText("Allow storage permission to continue.");
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
            status.setText("Download started. Check notifications. Saved in Music/RonysMusicDownloader/ (ID " + id + ").");
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
