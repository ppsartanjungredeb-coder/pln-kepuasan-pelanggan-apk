package com.pln.ulptanjungredeb.kepuasan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS="pln_kepuasan_v4";
    private static final String KEY_URL="web_url";
    private static final String KEY_PIN="admin_pin";
    private static final String DEFAULT_PIN="1234";
    private static final String LOCAL_URL="file:///android_asset/web/index.html";

    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private FrameLayout root;
    private WebView webView;
    private LinearLayout offlinePanel;
    private ProgressBar loading;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final Runnable fullscreenTask=this::enterImmersiveMode;
    private final Runnable adminTask=this::showAdminPin;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        String u=prefs.getString(KEY_URL,"").trim();
        if(u.isEmpty()) showSetup(); else showBrowser(u);
        handler.postDelayed(fullscreenTask,350);
        registerNetworkWatcher();
    }

    private TextView label(String s,int size,boolean bold,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private void showSetup(){
        cleanupWebView();
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(244,251,255));
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER_HORIZONTAL); box.setPadding(dp(28),dp(26),dp(28),dp(26)); scroll.addView(box);
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.logo_pln); logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams lpLogo=new LinearLayout.LayoutParams(dp(125),dp(155)); lpLogo.gravity=Gravity.CENTER_HORIZONTAL; box.addView(logo,lpLogo);
        box.addView(label("KEPUASAN PELANGGAN",23,true,Color.rgb(21,51,74)));
        TextView unit=label("PLN ULP TANJUNG REDEB",17,true,Color.rgb(0,114,188)); LinearLayout.LayoutParams ulp=new LinearLayout.LayoutParams(-1,-2); ulp.topMargin=dp(4); box.addView(unit,ulp);
        TextView note=label("Pengaturan awal hanya sekali.\nMasukkan URL Web App Google Apps Script yang berakhir /exec.",14,false,Color.DKGRAY); LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(-1,-2); nlp.topMargin=dp(18); box.addView(note,nlp);
        EditText url=new EditText(this); url.setHint("https://script.google.com/macros/s/.../exec"); url.setTextSize(15); url.setSingleLine(false); url.setMinLines(2); url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI); LinearLayout.LayoutParams urlp=new LinearLayout.LayoutParams(-1,-2); urlp.topMargin=dp(18); box.addView(url,urlp);
        Button start=new Button(this); start.setText("SIMPAN & MULAI"); start.setTextSize(16); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(58)); slp.topMargin=dp(16); box.addView(start,slp);
        Button demo=new Button(this); demo.setText("COBA DEMO LOKAL"); LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,dp(52)); dlp.topMargin=dp(8); box.addView(demo,dlp);
        TextView foot=label("Setelah disimpan, aplikasi langsung membuka survei fullscreen pada pembukaan berikutnya.\nAdmin: tahan pojok kiri atas 4 detik.",12,false,Color.GRAY); LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,-2); flp.topMargin=dp(14); box.addView(foot,flp);
        setContentView(scroll); handler.postDelayed(fullscreenTask,250);
        start.setOnClickListener(v->{String value=url.getText().toString().trim(); if(!validWebUrl(value)){url.setError("URL harus dimulai https:// atau http://"); return;} prefs.edit().putString(KEY_URL,value).apply(); showBrowser(value);});
        demo.setOnClickListener(v->{prefs.edit().putString(KEY_URL,LOCAL_URL).apply(); showBrowser(LOCAL_URL);});
    }

    private void showBrowser(String url){
        cleanupWebView(); root=new FrameLayout(this); root.setBackgroundColor(Color.WHITE);
        try{webView=new WebView(this);}catch(Throwable t){showRecovery("WebView Android tidak dapat dijalankan.","Perbarui Google Chrome / Android System WebView, restart perangkat, lalu coba lagi."); return;}
        root.addView(webView,new FrameLayout.LayoutParams(-1,-1));
        loading=new ProgressBar(this); FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(dp(52),dp(52)); plp.gravity=Gravity.CENTER; root.addView(loading,plp);
        offlinePanel=createOfflinePanel(); offlinePanel.setVisibility(View.GONE); root.addView(offlinePanel,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root); configureWebView(); configureAdminHotspot(); handler.postDelayed(fullscreenTask,300); loadUrl(url);
    }

    private void configureWebView(){
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false);
        CookieManager cm=CookieManager.getInstance(); cm.setAcceptCookie(true); if(Build.VERSION.SDK_INT>=21){cm.setAcceptThirdPartyCookies(webView,true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);}
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String sc=r.getUrl().getScheme(); if(sc==null)return true; sc=sc.toLowerCase(); return !(sc.equals("https")||sc.equals("http")||sc.equals("file"));}
            @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap f){super.onPageStarted(v,u,f); if(loading!=null)loading.setVisibility(View.VISIBLE); if(offlinePanel!=null)offlinePanel.setVisibility(View.GONE);}
            @Override public void onPageFinished(WebView v,String u){super.onPageFinished(v,u); if(loading!=null)loading.setVisibility(View.GONE); v.evaluateJavascript("(function(){['fullscreenBtn','fs'].forEach(function(id){var e=document.getElementById(id);if(e)e.style.display='none';});})();",null); handler.postDelayed(fullscreenTask,180);}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){if(loading!=null)loading.setVisibility(View.GONE); if(offlinePanel!=null)offlinePanel.setVisibility(View.VISIBLE);}}
        });
    }

    private LinearLayout createOfflinePanel(){
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setGravity(Gravity.CENTER); p.setPadding(dp(28),dp(28),dp(28),dp(28)); p.setBackgroundColor(Color.rgb(244,251,255));
        p.addView(label("KONEKSI BERMASALAH",24,true,Color.rgb(0,114,188)));
        TextView msg=label("Aplikasi tidak dapat membuka halaman survei.\nPeriksa Wi-Fi / internet lalu coba kembali.",15,false,Color.DKGRAY); LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2); mlp.topMargin=dp(14); p.addView(msg,mlp);
        Button retry=new Button(this); retry.setText("COBA LAGI"); LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(dp(300),dp(58)); rlp.topMargin=dp(20); p.addView(retry,rlp);
        Button admin=new Button(this); admin.setText("PENGATURAN ADMIN"); LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(dp(300),dp(52)); alp.topMargin=dp(8); p.addView(admin,alp);
        retry.setOnClickListener(v->loadSavedUrl()); admin.setOnClickListener(v->showAdminPin()); return p;
    }

    private void showRecovery(String title,String message){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(28),dp(28),dp(28),dp(28)); box.setBackgroundColor(Color.rgb(244,251,255));
        box.addView(label(title,22,true,Color.rgb(0,114,188))); TextView m=label(message,15,false,Color.DKGRAY); LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2); mlp.topMargin=dp(15); box.addView(m,mlp);
        Button reset=new Button(this); reset.setText("RESET PENGATURAN"); LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(dp(300),dp(56)); rlp.topMargin=dp(20); box.addView(reset,rlp); reset.setOnClickListener(v->{prefs.edit().remove(KEY_URL).apply(); showSetup();});
        setContentView(box); handler.postDelayed(fullscreenTask,250);
    }

    private void configureAdminHotspot(){
        webView.setOnTouchListener((v,e)->{float x=e.getX(),y=e.getY(); boolean hot=x<=v.getWidth()*0.15f&&y<=v.getHeight()*0.18f; switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN: if(hot)handler.postDelayed(adminTask,4000); break; case MotionEvent.ACTION_MOVE: if(!hot)handler.removeCallbacks(adminTask); break; case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: handler.removeCallbacks(adminTask); break;} return false;});
    }

    private void showAdminPin(){
        handler.removeCallbacks(adminTask); EditText pin=new EditText(this); pin.setHint("PIN Admin"); pin.setGravity(Gravity.CENTER); pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Admin").setMessage("Masukkan PIN").setView(pin).setNegativeButton("Batal",null).setPositiveButton("Lanjut",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String saved=prefs.getString(KEY_PIN,DEFAULT_PIN); if(pin.getText().toString().equals(saved)){d.dismiss(); showAdminSettings();}else pin.setError("PIN salah");}));
        d.setOnDismissListener(x->handler.postDelayed(fullscreenTask,150)); d.show();
    }

    private void showAdminSettings(){
        EditText url=new EditText(this); String cur=prefs.getString(KEY_URL,""); url.setHint("URL Web App /exec"); url.setText(cur.startsWith("http")?cur:""); url.setSingleLine(false); url.setMinLines(2); url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        EditText pin=new EditText(this); pin.setHint("PIN baru (opsional, minimal 4 digit)"); pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(22),dp(8),dp(22),0); wrap.addView(url); wrap.addView(pin);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Pengaturan APK").setView(wrap).setNegativeButton("Batal",null).setNeutralButton("Reset Awal",null).setPositiveButton("Simpan",null).create();
        d.setOnShowListener(x->{
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String u=url.getText().toString().trim(); if(!u.isEmpty()&&!validWebUrl(u)){url.setError("URL tidak valid"); return;} SharedPreferences.Editor ed=prefs.edit(); if(!u.isEmpty())ed.putString(KEY_URL,u); String np=pin.getText().toString().trim(); if(!np.isEmpty()){if(np.length()<4){pin.setError("Minimal 4 digit");return;}ed.putString(KEY_PIN,np);} ed.apply(); d.dismiss(); loadSavedUrl(); Toast.makeText(this,"Pengaturan disimpan",Toast.LENGTH_SHORT).show();});
            d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{prefs.edit().remove(KEY_URL).apply(); d.dismiss(); showSetup();});
        }); d.setOnDismissListener(x->handler.postDelayed(fullscreenTask,150)); d.show();
    }

    private boolean validWebUrl(String u){return u.startsWith("https://")||u.startsWith("http://");}
    private void loadSavedUrl(){String u=prefs.getString(KEY_URL,"").trim(); if(u.isEmpty()){showSetup(); return;} if(webView==null)showBrowser(u); else{if(offlinePanel!=null)offlinePanel.setVisibility(View.GONE); webView.loadUrl(u);}}
    private void loadUrl(String u){if(webView==null)return; if(u.startsWith("file://")||validWebUrl(u))webView.loadUrl(u); else{prefs.edit().remove(KEY_URL).apply(); showSetup();}}

    private void enterImmersiveMode(){try{if(Build.VERSION.SDK_INT>=30){WindowInsetsController c=getWindow().getInsetsController(); if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars()); c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else{getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}}catch(Throwable ignored){}}

    private void registerNetworkWatcher(){try{connectivityManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); if(connectivityManager==null||Build.VERSION.SDK_INT<24)return; networkCallback=new ConnectivityManager.NetworkCallback(){@Override public void onAvailable(Network n){handler.post(()->{if(offlinePanel!=null&&offlinePanel.getVisibility()==View.VISIBLE)loadSavedUrl();});}}; NetworkRequest req=new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(); connectivityManager.registerNetworkCallback(req,networkCallback);}catch(Throwable ignored){}}
    private void unregisterNetworkWatcher(){try{if(connectivityManager!=null&&networkCallback!=null)connectivityManager.unregisterNetworkCallback(networkCallback);}catch(Throwable ignored){}}
    private void cleanupWebView(){handler.removeCallbacks(adminTask); if(webView!=null){try{webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy();}catch(Throwable ignored){} webView=null;} root=null; offlinePanel=null; loading=null;}

    @Override public void onWindowFocusChanged(boolean h){super.onWindowFocusChanged(h); if(h)handler.postDelayed(fullscreenTask,120);}
    @Override protected void onResume(){super.onResume(); if(webView!=null)webView.onResume(); handler.postDelayed(fullscreenTask,180);}
    @Override protected void onPause(){if(webView!=null)webView.onPause(); super.onPause();}
    @Override @SuppressWarnings("deprecation") public void onBackPressed(){handler.postDelayed(fullscreenTask,80);}
    @Override protected void onDestroy(){unregisterNetworkWatcher(); handler.removeCallbacksAndMessages(null); cleanupWebView(); super.onDestroy();}
}
