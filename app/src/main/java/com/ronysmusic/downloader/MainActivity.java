package com.ronysmusic.downloader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address;
    private TextView status;
    private DownloadManager downloads;
    private final Set<String> seen = new LinkedHashSet<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        downloads = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(12,12,12,8);
        page.setBackgroundColor(Color.rgb(14,19,29));
        TextView title = new TextView(this);
        title.setText("🎵 Rony's Music Downloader");
        title.setTextColor(Color.WHITE); title.setTextSize(20); title.setPadding(8,10,8,12);
        page.addView(title);
        LinearLayout bar = new LinearLayout(this);
        address = new EditText(this); address.setSingleLine(true);
        address.setText("https://suno.com"); address.setHint("Paste an authorized MP3/WAV URL");
        address.setTextColor(Color.WHITE); address.setHintTextColor(Color.LTGRAY);
        bar.addView(address,new LinearLayout.LayoutParams(0,52,1));
        Button go = new Button(this); go.setText("Go");
        bar.addView(go); page.addView(bar);
        LinearLayout actions = new LinearLayout(this);
        Button browser = new Button(this); browser.setText("Suno Browser");
        Button manual = new Button(this); manual.setText("Download URL");
        Button share = new Button(this); share.setText("Share link");
        actions.addView(browser,new LinearLayout.LayoutParams(0,52,1));
        actions.addView(manual,new LinearLayout.LayoutParams(0,52,1));
        actions.addView(share,new LinearLayout.LayoutParams(0,52,1));
        page.addView(actions);
        status = new TextView(this); status.setTextColor(Color.rgb(90,210,190));
        status.setPadding(8,8,8,8); status.setText("Only authorized MP3/WAV audio. Video downloads are not supported.");
        page.addView(status);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){ address.setText(url); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req){
                String u=req.getUrl().toString(); String l=u.toLowerCase(Locale.ROOT);
                if ((l.contains(".mp3") || l.contains(".wav")) && !l.contains(".mp4")) {
                    runOnUiThread(()->{ if(seen.add(u)) status.setText("Audio link detected. Use Download URL after copying the audio link."); });
                }
                return super.shouldInterceptRequest(view,req);
            }
        });
        page.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
        go.setOnClickListener(v->navigate());
        browser.setOnClickListener(v->{address.setText("https://suno.com");navigate();});
        manual.setOnClickListener(v->download(address.getText().toString().trim()));
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,address.getText().toString());startActivity(Intent.createChooser(i,"Share audio link"));});
        web.loadUrl("https://suno.com");
    }
    private void navigate(){
        String u=address.getText().toString().trim();
        if(!u.startsWith("https://")&&!u.startsWith("http://"))u="https://"+u;
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);
        web.loadUrl(u);
    }
    private void download(String u){
        String l=u.toLowerCase(Locale.ROOT);
        if(!(l.startsWith("https://") && (l.matches(".*\\\\.mp3([?].*)?$")||l.matches(".*\\\\.wav([?].*)?$")))){
            status.setText("Please enter a direct HTTPS .mp3 or .wav link. MP4/video is blocked.");
            return;
        }
        try{
            String name=URLUtil.guessFileName(u,null,l.endsWith(".wav")?"audio/wav":"audio/mpeg");
            DownloadManager.Request r=new DownloadManager.Request(Uri.parse(u));
            r.setTitle(name); r.setDescription("Rony's Music Downloader");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC,"RonysMusicDownloader/"+name);
            downloads.enqueue(r);
            status.setText("Download queued: "+name);
        }catch(Exception e){status.setText("Download error: "+e.getMessage());}
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}