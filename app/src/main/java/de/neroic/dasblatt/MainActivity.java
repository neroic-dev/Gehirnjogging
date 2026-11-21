package de.neroic.dasblatt;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        styleSystemBars();

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        webView.setBackgroundColor(isNight() ? Color.rgb(20, 20, 20) : Color.rgb(243, 241, 236));
        webView.addJavascriptInterface(new Bridge(), "Native");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternal(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternal(url);
            }
        });
        setContentView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    /** Artikel-Links im normalen Browser öffnen, die App selbst bleibt in der WebView. */
    private boolean openExternal(String url) {
        if (url.startsWith("file:")) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
        return true;
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressWarnings("deprecation")
    private void styleSystemBars() {
        if (Build.VERSION.SDK_INT < 21) return;
        boolean night = isNight();
        int paper = night ? Color.rgb(20, 20, 20) : Color.rgb(243, 241, 236);
        Window w = getWindow();
        w.setStatusBarColor(paper);
        w.setNavigationBarColor(paper);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = 0;
            if (!night) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            w.getDecorView().setSystemUiVisibility(flags);
        } else {
            w.setStatusBarColor(Color.BLACK);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        styleSystemBars();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("window.back ? back() : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }

    /** Feeds nativ abrufen – so gibt es keine CORS-Sperre und keinen Proxy. */
    private class Bridge {
        @JavascriptInterface
        public void fetch(final String id, final String url) {
            pool.execute(() -> {
                String js;
                try {
                    String body = download(url, 0);
                    js = "window.__nativeDone(" + JSONObject.quote(id) + ",true," + JSONObject.quote(body) + ")";
                } catch (Exception e) {
                    String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                    js = "window.__nativeDone(" + JSONObject.quote(id) + ",false," + JSONObject.quote(msg) + ")";
                }
                final String script = js;
                webView.post(() -> webView.evaluateJavascript(script, null));
            });
        }
    }

    private static final Pattern XML_ENC = Pattern.compile("encoding=[\"']([A-Za-z0-9._-]+)[\"']");
    private static final Pattern CT_ENC = Pattern.compile("charset=([A-Za-z0-9._-]+)", Pattern.CASE_INSENSITIVE);

    private static String download(String address, int hops) throws Exception {
        if (hops > 5) throw new Exception("Zu viele Weiterleitungen");
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) DasBlatt/1.0 RSS-Reader");
        c.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*");
        c.setRequestProperty("Accept-Encoding", "gzip");
        try {
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                if (loc == null) throw new Exception("Weiterleitung ohne Ziel");
                return download(new URL(new URL(address), loc).toString(), hops + 1);
            }
            if (code >= 400) throw new Exception("HTTP " + code);

            InputStream in = c.getInputStream();
            if ("gzip".equalsIgnoreCase(c.getContentEncoding())) in = new GZIPInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 8 * 1024 * 1024) break;
            }
            in.close();
            byte[] bytes = out.toByteArray();

            String charset = "UTF-8";
            String head = new String(bytes, 0, Math.min(bytes.length, 200), "ISO-8859-1");
            Matcher m = XML_ENC.matcher(head);
            String ct = c.getContentType();
            if (m.find()) {
                charset = m.group(1);
            } else if (ct != null) {
                Matcher m2 = CT_ENC.matcher(ct);
                if (m2.find()) charset = m2.group(1);
            }
            if (!Charset.isSupported(charset)) charset = "UTF-8";
            return new String(bytes, charset);
        } finally {
            c.disconnect();
        }
    }
}
