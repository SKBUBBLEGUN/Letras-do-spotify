package com.skbubblegun.letrasspotify;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://skbubblegun.github.io/Letras-do-spotify/";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setUserAgentString(settings.getUserAgentString() + " LetrasAndroidApp");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(url);
            }
        });

        handleIntent(getIntent());
        if (getIntent() == null || getIntent().getData() == null) {
            webView.loadUrl(HOME);
        }
    }

    private boolean route(String value) {
        Uri uri = Uri.parse(value);
        String host = uri.getHost() == null ? "" : uri.getHost();
        if ("accounts.spotify.com".equalsIgnoreCase(host)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível abrir o login do Spotify.", Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if ("letrasspotify".equalsIgnoreCase(uri.getScheme())) {
            handleCallback(uri);
            return true;
        }
        return false;
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null
                && "letrasspotify".equalsIgnoreCase(intent.getData().getScheme())) {
            handleCallback(intent.getData());
        }
    }

    private void handleCallback(Uri uri) {
        String query = uri.getEncodedQuery();
        String target = HOME + (query == null ? "" : "?" + query);
        if (webView != null) webView.post(() -> webView.loadUrl(target));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
