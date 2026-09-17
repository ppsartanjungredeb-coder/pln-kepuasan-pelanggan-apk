package com.pln.ulptanjungredeb.kepuasan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS = "pln_kepuasan_settings";
    private static final String KEY_WEB_URL = "web_url";
    private static final String KEY_ADMIN_PIN = "admin_pin";
    private static final String DEFAULT_PIN = "1234";
    private static final String LOCAL_URL = "file:///android_asset/web/index.html";

    private SharedPreferences prefs;
    private WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean holdTriggered = false;

    private final Runnable adminHold = () -> {
        holdTriggered = true;
        showPinDialog();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        enterImmersiveMode();

        String savedUrl = prefs.getString(KEY_WEB_URL, "").trim();
        if (savedUrl.isEmpty()) {
            showFirstSetup();
        } else {
            showWebView(savedUrl);
        }
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(21,51,74));
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void showFirstSetup() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(244,251,255));
        scroll.addView(root);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.pln.ulptanjungredeb.kepuasan.R.drawable.logo_pln);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(150), dp(180));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logo, logoLp);

        TextView title = text("KEPUASAN PELANGGAN", 24, true);
        root.addView(title);

        TextView unit = text("PLN ULP TANJUNG REDEB", 18, true);
        unit.setTextColor(Color.rgb(0,114,188));
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        unitLp.topMargin = dp(4);
        root.addView(unit, unitLp);

        TextView info = text(
                "Pengaturan awal hanya dilakukan sekali.\nMasukkan URL Web App Google Apps Script yang sudah dideploy.",
                15, false
        );
        info.setTextColor(Color.DKGRAY);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = dp(18);
        root.addView(info, infoLp);

        EditText url = new EditText(this);
        url.setHint("https://script.google.com/macros/s/.../exec");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setSingleLine(false);
        url.setMinLines(2);
        url.setTextSize(15);
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        urlLp.topMargin = dp(20);
        root.addView(url, urlLp);

        Button start = new Button(this);
        start.setText("SIMPAN & MULAI");
        start.setTextSize(16);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        startLp.topMargin = dp(18);
        root.addView(start, startLp);

        Button demo = new Button(this);
        demo.setText("COBA DEMO LOKAL");
        LinearLayout.LayoutParams demoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        demoLp.topMargin = dp(10);
        root.addView(demo, demoLp);

        TextView hint = text(
                "Setelah tersimpan, APK akan langsung membuka survei fullscreen setiap kali dijalankan.\n"
                        + "Untuk mengubah URL: tekan & tahan layar 4 detik → PIN 1234.",
                13, false
        );
        hint.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(16);
        root.addView(hint, hintLp);

        setContentView(scroll);
        enterImmersiveMode();

        start.setOnClickListener(v -> {
            String value = url.getText().toString().trim();
            if (!(value.startsWith("https://") || value.startsWith("http://"))) {
                url.setError("Masukkan URL Web App yang benar");
                return;
            }
            prefs.edit().putString(KEY_WEB_URL, value).apply();
            showWebView(value);
        });

        demo.setOnClickListener(v -> {
            prefs.edit().putString(KEY_WEB_URL, LOCAL_URL).apply();
            showWebView(LOCAL_URL);
        });
    }

    private void showWebView(String url) {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        configureWebView();
        configureAdminGesture();
        setContentView(webView);
        enterImmersiveMode();

        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
            webView.loadUrl(url);
        } else {
            showFirstSetup();
        }
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

                if ("pln".equals(scheme)) {
                    if ("retry".equals(uri.getHost())) loadSavedPage();
                    if ("settings".equals(uri.getHost())) showPinDialog();
                    return true;
                }

                return !(scheme.equals("https") || scheme.equals("http") || scheme.equals("file"));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "(function(){"
                        + "['fullscreenBtn','fs'].forEach(function(id){var x=document.getElementById(id);if(x)x.style.display='none';});"
                        + "})();";
                view.evaluateJavascript(js, null);
                enterImmersiveMode();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showOfflinePage();
            }
        });
    }

    private void showOfflinePage() {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;font-family:Arial;background:#f4fbff;color:#15334a;display:grid;place-items:center;min-height:100vh;text-align:center}"
                + ".c{padding:28px;max-width:520px}h1{color:#0072bc}button{width:100%;padding:17px;margin:8px 0;border:0;border-radius:14px;font-size:16px;font-weight:bold}"
                + ".p{background:#0072bc;color:white}.s{background:#eaf7fd;color:#005b96}</style></head>"
                + "<body><div class='c'><h1>Koneksi Internet Terputus</h1>"
                + "<p>Periksa Wi-Fi/data internet, kemudian tekan Coba Lagi.</p>"
                + "<button class='p' onclick=\"location.href='pln://retry'\">COBA LAGI</button>"
                + "<button class='s' onclick=\"location.href='pln://settings'\">PENGATURAN</button>"
                + "</div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void loadSavedPage() {
        String url = prefs.getString(KEY_WEB_URL, "").trim();
        if (url.isEmpty()) showFirstSetup();
        else webView.loadUrl(url);
    }

    private void configureAdminGesture() {
        webView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    holdTriggered = false;
                    handler.postDelayed(adminHold, 4000);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_MOVE:
                    if (!holdTriggered) handler.removeCallbacks(adminHold);
                    break;
            }
            return false;
        });
    }

    private void showPinDialog() {
        runOnUiThread(() -> {
            EditText pin = new EditText(this);
            pin.setHint("PIN Admin");
            pin.setGravity(Gravity.CENTER);
            pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Pengaturan Admin")
                    .setMessage("PIN default: 1234")
                    .setView(pin)
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Lanjut", null)
                    .create();

            dialog.setOnShowListener(d ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String saved = prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN);
                        if (pin.getText().toString().equals(saved)) {
                            dialog.dismiss();
                            showSettings();
                        } else {
                            pin.setError("PIN salah");
                        }
                    })
            );
            dialog.setOnDismissListener(d -> enterImmersiveMode());
            dialog.show();
        });
    }

    private void showSettings() {
        EditText url = new EditText(this);
        url.setHint("URL Web App Google Apps Script");
        String current = prefs.getString(KEY_WEB_URL, "");
        url.setText(current.equals(LOCAL_URL) ? "" : current);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setSingleLine(false);
        url.setMinLines(2);

        EditText newPin = new EditText(this);
        newPin.setHint("PIN baru (opsional)");
        newPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(22), dp(8), dp(22), 0);
        wrap.addView(url);
        wrap.addView(newPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Pengaturan APK")
                .setMessage("Ubah URL Web App atau PIN Admin.")
                .setView(wrap)
                .setNegativeButton("Batal", null)
                .setNeutralButton("Reset Awal", null)
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = url.getText().toString().trim();
                if (!value.isEmpty() && !(value.startsWith("https://") || value.startsWith("http://"))) {
                    url.setError("URL tidak valid");
                    return;
                }

                SharedPreferences.Editor ed = prefs.edit();
                if (!value.isEmpty()) ed.putString(KEY_WEB_URL, value);

                String pinValue = newPin.getText().toString().trim();
                if (!pinValue.isEmpty()) {
                    if (pinValue.length() < 4) {
                        newPin.setError("PIN minimal 4 digit");
                        return;
                    }
                    ed.putString(KEY_ADMIN_PIN, pinValue);
                }
                ed.apply();
                dialog.dismiss();
                loadSavedPage();
                Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit().remove(KEY_WEB_URL).apply();
                dialog.dismiss();
                webView = null;
                showFirstSetup();
            });
        });

        dialog.setOnDismissListener(d -> enterImmersiveMode());
        dialog.show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        enterImmersiveMode();
        Toast.makeText(this, "Mode kiosk aktif", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(adminHold);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
