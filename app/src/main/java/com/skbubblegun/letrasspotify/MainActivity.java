package com.skbubblegun.letrasspotify;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String REDIRECT = "letrasspotify://callback";
    private static final String API = "https://api.spotify.com/v1";
    private static final String PREF = "spotify_native";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Long> lyricTimes = new ArrayList<>();
    private final List<String> lyricTexts = new ArrayList<>();
    private final List<TextView> lyricViews = new ArrayList<>();

    private android.content.SharedPreferences prefs;
    private LinearLayout root, lyricColumn;
    private EditText clientInput;
    private TextView status, trackTitle, artist, message, elapsed, duration;
    private ScrollView lyricScroll;
    private SeekBar seekBar;
    private Button playButton;
    private String currentTrack = "";
    private long progressMs, durationMs, progressAt;
    private boolean playing, seeking, lyricsSynced;
    private int activeLine = -1;

    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            if (prefs != null && prefs.contains("access")) pollPlayback();
            main.postDelayed(this, 4000);
        }
    };
    private final Runnable clockTask = new Runnable() {
        @Override public void run() {
            if (playing && !seeking && durationMs > 0) {
                long p = Math.min(durationMs, progressMs + (System.currentTimeMillis() - progressAt));
                seekBar.setProgress((int) (p * 1000 / durationMs));
                elapsed.setText(formatTime(p));
                updateLyricHighlight(p);
            }
            main.postDelayed(this, 400);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12,12,14));
        getWindow().setNavigationBarColor(Color.rgb(12,12,14));
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        buildScreen();
        if (getIntent() != null && getIntent().getData() != null) handleCallback(getIntent().getData());
        else if (prefs.contains("access")) showPlayer();
        else showHome();
        main.post(pollTask);
        main.post(clockTask);
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String value) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false);
        b.setTextColor(Color.BLACK); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30,215,96)));
        return b;
    }
    private void buildScreen() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(16)); root.setBackgroundColor(Color.rgb(12,12,14));
        setContentView(root);
    }
    private void showHome() {
        root.removeAllViews();
        TextView heading = text("Letras do Spotify", 26, true);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        status = text("Conecte sua conta para mostrar as letras da música em reprodução.", 14, false);
        status.setTextColor(Color.LTGRAY); status.setPadding(0, dp(10), 0, dp(18));
        root.addView(status);
        clientInput = new EditText(this); clientInput.setSingleLine(true);
        clientInput.setHint("Spotify Client ID"); clientInput.setTextColor(Color.WHITE);
        clientInput.setHintTextColor(Color.GRAY); clientInput.setText(prefs.getString("client_id", ""));
        root.addView(clientInput, new LinearLayout.LayoutParams(-1, -2));
        TextView help = text("Crie um app em developer.spotify.com/dashboard e adicione este Redirect URI nas configurações:\n" + REDIRECT + "\n\nUse o Client ID. Não informe Client Secret.", 13, false);
        help.setTextColor(Color.LTGRAY); help.setPadding(0, dp(12), 0, dp(16));
        root.addView(help);
        Button login = button("Conectar ao Spotify");
        root.addView(login, new LinearLayout.LayoutParams(-1, -2));
        login.setOnClickListener(v -> beginLogin());
    }
    private void showPlayer() {
        root.removeAllViews();
        TextView heading = text("LETRAS", 13, true); heading.setTextColor(Color.rgb(30,215,96));
        root.addView(heading);
        trackTitle = text("Nada tocando", 22, true);
        trackTitle.setPadding(0, dp(12), 0, dp(2)); root.addView(trackTitle);
        artist = text("", 14, false); artist.setTextColor(Color.LTGRAY); root.addView(artist);
        message = text("Abra o Spotify e reproduza uma música.", 15, false);
        message.setGravity(Gravity.CENTER); message.setTextColor(Color.LTGRAY);
        root.addView(message, new LinearLayout.LayoutParams(-1, dp(72)));
        lyricScroll = new ScrollView(this); lyricScroll.setFillViewport(false);
        lyricColumn = new LinearLayout(this); lyricColumn.setOrientation(LinearLayout.VERTICAL);
        lyricColumn.setPadding(0, dp(12), 0, dp(12)); lyricScroll.addView(lyricColumn);
        root.addView(lyricScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout times = new LinearLayout(this); times.setGravity(Gravity.CENTER_VERTICAL);
        elapsed = text("0:00", 12, false); duration = text("0:00", 12, false);
        times.addView(elapsed); times.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1)); times.addView(duration);
        root.addView(times);
        seekBar = new SeekBar(this); seekBar.setMax(1000); root.addView(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int p, boolean user) { if (user) elapsed.setText(formatTime(durationMs * p / 1000)); }
            @Override public void onStartTrackingTouch(SeekBar b) { seeking = true; }
            @Override public void onStopTrackingTouch(SeekBar b) {
                seeking = false; long target = durationMs * b.getProgress() / 1000;
                progressMs = target; progressAt = System.currentTimeMillis(); control("/me/player/seek?position_ms=" + target, "PUT");
            }
        });
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setPadding(0, dp(8), 0, 0);
        Button previous = button("⏮"); playButton = button("▶"); Button next = button("⏭");
        controls.addView(previous); controls.addView(playButton); controls.addView(next); root.addView(controls);
        previous.setOnClickListener(v -> control("/me/player/previous", "POST"));
        next.setOnClickListener(v -> control("/me/player/next", "POST"));
        playButton.setOnClickListener(v -> control(playing ? "/me/player/pause" : "/me/player/play", "PUT"));
        Button disconnect = new Button(this); disconnect.setText("Desconectar"); disconnect.setAllCaps(false);
        disconnect.setTextColor(Color.LTGRAY); disconnect.setBackgroundColor(Color.TRANSPARENT);
        root.addView(disconnect); disconnect.setOnClickListener(v -> {
            prefs.edit().remove("access").remove("refresh").remove("expires").apply();
            showHome();
        });
    }

    private void beginLogin() {
        String client = clientInput.getText().toString().trim();
        if (client.isEmpty()) { status.setText("Cole o Client ID do Spotify."); return; }
        try {
            String verifier = randomString(64), state = randomString(24);
            prefs.edit().putString("client_id", client).putString("verifier", verifier).putString("oauth_state", state).apply();
            String challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes("UTF-8")), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            Uri auth = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
                    .appendQueryParameter("client_id", client).appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", REDIRECT).appendQueryParameter("scope", SCOPES)
                    .appendQueryParameter("state", state).appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("code_challenge", challenge).build();
            startActivity(new Intent(Intent.ACTION_VIEW, auth));
        } catch (Exception e) { status.setText("Não foi possível iniciar o login: " + e.getMessage()); }
    }
    private String randomString(int bytes) {
        byte[] data = new byte[bytes]; new SecureRandom().nextBytes(data);
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intent.getData() != null) handleCallback(intent.getData());
    }
    private void handleCallback(Uri uri) {
        if (!"letrasspotify".equals(uri.getScheme()) || !"callback".equals(uri.getHost())) return;
        String error = uri.getQueryParameter("error");
        if (error != null) { Toast.makeText(this, "Login cancelado: " + error, Toast.LENGTH_LONG).show(); showHome(); return; }
        String code = uri.getQueryParameter("code"), state = uri.getQueryParameter("state");
        if (code == null || state == null || !state.equals(prefs.getString("oauth_state", ""))) {
            Toast.makeText(this, "Resposta de login inválida. Tente novamente.", Toast.LENGTH_LONG).show(); showHome(); return;
        }
        runWorker(() -> {
            try {
                JSONObject token = tokenRequest(form("grant_type", "authorization_code", "code", code,
                        "redirect_uri", REDIRECT, "client_id", prefs.getString("client_id", ""),
                        "code_verifier", prefs.getString("verifier", "")));
                saveToken(token);
                main.post(() -> { showPlayer(); pollPlayback(); });
            } catch (Exception e) { main.post(() -> { showHome(); status.setText("Falha no login: " + e.getMessage()); }); }
        });
    }

    private void runWorker(Runnable task) { worker.execute(task); }
    private String form(String... pairs) throws Exception {
        StringBuilder b = new StringBuilder();
        for (int i=0;i<pairs.length;i+=2) {
            if (b.length()>0) b.append('&');
            b.append(URLEncoder.encode(pairs[i], "UTF-8")).append('=').append(URLEncoder.encode(pairs[i+1], "UTF-8"));
        }
        return b.toString();
    }
    private JSONObject tokenRequest(String body) throws Exception {
        String response = request("https://accounts.spotify.com/api/token", "POST", body,
                "application/x-www-form-urlencoded", null);
        return new JSONObject(response);
    }
    private void saveToken(JSONObject j) throws Exception {
        long expires = System.currentTimeMillis() + j.optLong("expires_in", 3600) * 1000L - 30000;
        android.content.SharedPreferences.Editor e = prefs.edit()
                .putString("access", j.getString("access_token")).putLong("expires", expires);
        if (j.has("refresh_token")) e.putString("refresh", j.getString("refresh_token"));
        e.apply();
    }
    private String accessToken() throws Exception {
        if (System.currentTimeMillis() < prefs.getLong("expires", 0)) return prefs.getString("access", null);
        String refresh = prefs.getString("refresh", null);
        if (refresh == null) throw new Exception("Faça login novamente.");
        JSONObject j = tokenRequest(form("grant_type", "refresh_token", "refresh_token", refresh,
                "client_id", prefs.getString("client_id", "")));
        saveToken(j); return prefs.getString("access", null);
    }
    private String spotify(String path, String method, String body) throws Exception {
        return request(API + path, method, body, "application/json", accessToken());
    }
    private String request(String address, String method, String body, String contentType, String bearer) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(15000);
        if (contentType != null) c.setRequestProperty("Content-Type", contentType);
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) { c.setDoOutput(true); try (OutputStream out = c.getOutputStream()) { out.write(body.getBytes("UTF-8")); } }
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
        StringBuilder result = new StringBuilder();
        if (in != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line; while ((line = r.readLine()) != null) result.append(line);
        }
        c.disconnect();
        if (code < 200 || code >= 400) throw new Exception("HTTP " + code + (result.length() > 0 ? ": " + result : ""));
        return result.toString();
    }
    private void pollPlayback() {
        if (!prefs.contains("access")) return;
        runWorker(() -> {
            try {
                String raw = spotify("/me/player?additional_types=episode", "GET", null);
                if (raw == null || raw.isEmpty()) { main.post(() -> showNoTrack()); return; }
                JSONObject data = new JSONObject(raw);
                JSONObject item = data.optJSONObject("item");
                if (item == null) { main.post(() -> showNoTrack()); return; }
                String id = item.optString("id", "");
                long p = data.optLong("progress_ms", 0), d = item.optLong("duration_ms", 0);
                boolean isPlaying = data.optBoolean("is_playing", false);
                String name = item.optString("name", "Música");
                JSONArray artists = item.optJSONArray("artists");
                StringBuilder by = new StringBuilder();
                if (artists != null) for (int i=0;i<artists.length();i++) {
                    if (i>0) by.append(", "); by.append(artists.optJSONObject(i).optString("name"));
                }
                String artistName = by.toString();
                main.post(() -> {
                    progressMs=p; progressAt=System.currentTimeMillis(); durationMs=d; playing=isPlaying;
                    playButton.setText(playing ? "Ⅱ" : "▶");
                    duration.setText(formatTime(d)); seekBar.setProgress(d > 0 ? (int)(p*1000/d) : 0);
                    elapsed.setText(formatTime(p));
                    if (!id.equals(currentTrack)) {
                        currentTrack=id; trackTitle.setText(name); artist.setText(artistName);
                        loadLyrics(name, artistName, item.optString("album_name", ""), d);
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("HTTP 401")) {
                        prefs.edit().remove("access").remove("refresh").apply(); showHome();
                        status.setText("Sessão expirada. Conecte novamente.");
                    } else if (e.getMessage() != null && e.getMessage().contains("HTTP 403")) {
                        String detail = e.getMessage();
                        if (detail.length() > 260) detail = detail.substring(0, 260);
                        if (message != null) {
                            message.setText("Erro 403 do Spotify:\n" + detail);
                            message.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(this, "Erro 403: " + detail, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    private void showNoTrack() {
        playing=false; progressMs=0; durationMs=0; currentTrack="";
        trackTitle.setText("Nada tocando"); artist.setText("");
        playButton.setText("▶"); elapsed.setText("0:00"); duration.setText("0:00"); seekBar.setProgress(0);
        setLyricsMessage("Abra o Spotify e reproduza uma música.");
    }
    private void loadLyrics(String title, String by, String album, long durationMs) {
        setLyricsMessage("Buscando letra…");
        runWorker(() -> {
            try {
                String url = "https://lrclib.net/api/get?track_name=" + enc(title) + "&artist_name=" + enc(by)
                        + "&album_name=" + enc(album) + "&duration=" + (durationMs/1000);
                String raw = request(url, "GET", null, null, null);
                JSONObject j = new JSONObject(raw);
                String synced = j.optString("syncedLyrics", ""), plain = j.optString("plainLyrics", "");
                if (!synced.isEmpty()) parseLyrics(synced, true);
                else if (!plain.isEmpty()) parseLyrics(plain, false);
                else setLyricsMessage("Letra não encontrada para esta música.");
            } catch (Exception first) {
                try {
                    String raw = request("https://lrclib.net/api/search?q=" + enc(title + " " + by), "GET", null, null, null);
                    JSONArray arr = new JSONArray(raw); JSONObject pick = null;
                    for (int i=0;i<arr.length();i++) {
                        JSONObject x=arr.getJSONObject(i);
                        if (Math.abs(x.optLong("duration") - durationMs/1000) <= 15 && !x.optString("syncedLyrics","").isEmpty()) { pick=x; break; }
                    }
                    if (pick == null && arr.length()>0) pick=arr.getJSONObject(0);
                    if (pick == null) setLyricsMessage("Letra não encontrada para esta música.");
                    else if (!pick.optString("syncedLyrics","").isEmpty()) parseLyrics(pick.optString("syncedLyrics"), true);
                    else if (!pick.optString("plainLyrics","").isEmpty()) parseLyrics(pick.optString("plainLyrics"), false);
                    else setLyricsMessage("Letra não encontrada para esta música.");
                } catch (Exception e) { setLyricsMessage("Não foi possível buscar a letra. Verifique a conexão."); }
            }
        });
    }
    private String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }
    private void parseLyrics(String raw, boolean synced) {
        List<Long> times = new ArrayList<>(); List<String> lines = new ArrayList<>();
        if (synced) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+):(\\d+(?:[.:]\\d+)?)\\]");
            for (String line : raw.split("\\r?\\n")) {
                java.util.regex.Matcher m=pattern.matcher(line); List<Long> stamps=new ArrayList<>(); int end=0;
                while(m.find()) { double sec=Double.parseDouble(m.group(2).replace(':','.')); stamps.add(Long.valueOf((long)(Integer.parseInt(m.group(1))*60000+sec*1000))); end=m.end(); }
                String lyric=line.substring(Math.min(end,line.length())).trim();
                for(Long t:stamps) { times.add(t); lines.add(lyric); }
            }
        } else for (String line : raw.split("\\r?\\n")) { times.add(0L); lines.add(line); }
        final List<Long> ft=times; final List<String> fl=lines;
        main.post(() -> {
            lyricTimes.clear(); lyricTimes.addAll(ft); lyricTexts.clear(); lyricTexts.addAll(fl);
            lyricsSynced=synced; lyricColumn.removeAllViews(); lyricViews.clear(); activeLine=-1;
            for (String line : fl) {
                TextView t=text(line.isEmpty() ? "♪" : line, synced ? 24 : 18, true);
                t.setTextColor(synced ? Color.GRAY : Color.WHITE); t.setPadding(0,dp(10),0,dp(10));
                lyricColumn.addView(t); lyricViews.add(t);
            }
            message.setVisibility(View.GONE);
            updateLyricHighlight(progressMs);
        });
    }
    private void setLyricsMessage(String value) {
        main.post(() -> {
            if (message == null) return;
            message.setText(value); message.setVisibility(View.VISIBLE);
            if (lyricColumn != null) lyricColumn.removeAllViews();
            lyricViews.clear(); lyricTimes.clear(); lyricTexts.clear(); activeLine=-1;
        });
    }
    private void updateLyricHighlight(long position) {
        if (!lyricsSynced || lyricViews.isEmpty()) return;
        int index=-1;
        for (int i=0;i<lyricTimes.size();i++) { if (lyricTimes.get(i) <= position+250) index=i; else break; }
        if (index == activeLine) return;
        if (activeLine >= 0 && activeLine < lyricViews.size()) {
            lyricViews.get(activeLine).setTextColor(Color.GRAY); lyricViews.get(activeLine).setScaleX(1f); lyricViews.get(activeLine).setScaleY(1f);
        }
        activeLine=index;
        if (index >= 0) {
            TextView line=lyricViews.get(index); line.setTextColor(Color.WHITE); line.setScaleX(1.02f); line.setScaleY(1.02f);
            lyricScroll.post(() -> lyricScroll.smoothScrollTo(0, Math.max(0,line.getTop()-lyricScroll.getHeight()/2)));
        }
    }
    private String formatTime(long ms) {
        long sec=Math.max(0,ms/1000); return String.format(Locale.US,"%d:%02d",sec/60,sec%60);
    }
    private void control(String path, String method) {
        runWorker(() -> {
            try { spotify(path, method, null); main.postDelayed(this::pollPlayback, 500); }
            catch (Exception e) { main.post(() -> Toast.makeText(this, e.getMessage().contains("HTTP 403") ?
                    "Este controle pode exigir Spotify Premium." : "Falha no controle: " + e.getMessage(), Toast.LENGTH_LONG).show()); }
        });
    }
    @Override protected void onDestroy() {
        main.removeCallbacks(pollTask); main.removeCallbacks(clockTask);
        worker.shutdownNow(); super.onDestroy();
    }
}