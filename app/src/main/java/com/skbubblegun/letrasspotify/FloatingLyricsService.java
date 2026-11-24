package com.skbubblegun.letrasspotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FloatingLyricsService extends Service {
    public static final String ACTION_UPDATE = "com.skbubblegun.letrasspotify.UPDATE_OVERLAY";
    private static final String ACTION_CONTROL = "com.skbubblegun.letrasspotify.OVERLAY_CONTROL";
    private static final String CHANNEL = "floating_lyrics";
    private WindowManager wm;
    private View panel;
    private TextView title, artist, lyric, elapsed, duration;
    private ProgressBar progress;
    private RecordView record;
    private boolean attached;

    @Override public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        if (Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this)) createOverlay();
        else stopSelf();
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE.equals(intent.getAction())) {
            updateText(intent.getStringExtra("title"), intent.getStringExtra("artist"),
                    intent.getStringExtra("lyric"), intent.getBooleanExtra("playing", false),
                    intent.getLongExtra("position", 0), intent.getLongExtra("duration", 0));
        }
        return START_STICKY;
    }
    private void startForegroundNotification() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Letras flutuantes", NotificationManager.IMPORTANCE_LOW));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Letras do Spotify").setContentText("Janela flutuante ativa").setOngoing(true);
        startForeground(7413, b.build());
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }
    private void createOverlay() {
        if (attached) return;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(10)); card.setBackgroundColor(Color.rgb(19,19,22));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView handle = label("━━━━━━━━", 12, true); handle.setTextColor(Color.GRAY);
        TextView brand = label("  LETRAS DO SPOTIFY", 11, true); brand.setTextColor(Color.rgb(30,215,96));
        Button close = controlButton("×"); close.setTextSize(19);
        header.addView(handle); header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1)); header.addView(close);
        card.addView(header);
        title = label("Abra o Spotify", 14, true); title.setPadding(0, dp(4), 0, 0); card.addView(title);
        artist = label("", 11, false); artist.setTextColor(Color.LTGRAY); card.addView(artist);
        lyric = label("Aguardando música…", 20, true); lyric.setGravity(Gravity.CENTER);
        lyric.setMinHeight(dp(76)); lyric.setPadding(dp(4), dp(8), dp(4), dp(8)); card.addView(lyric);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        card.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        LinearLayout timeRow = new LinearLayout(this); timeRow.setGravity(Gravity.CENTER_VERTICAL);
        elapsed = label("0:00", 10, false); elapsed.setTextColor(Color.LTGRAY);
        duration = label("0:00", 10, false); duration.setTextColor(Color.LTGRAY);
        timeRow.addView(elapsed); timeRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1)); timeRow.addView(duration);
        card.addView(timeRow);
        LinearLayout footer = new LinearLayout(this); footer.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = controlButton("⏮"), pause = controlButton("Ⅱ"), next = controlButton("⏭");
        footer.addView(prev, new LinearLayout.LayoutParams(dp(58), dp(48)));
        footer.addView(pause, new LinearLayout.LayoutParams(dp(58), dp(48)));
        footer.addView(next, new LinearLayout.LayoutParams(dp(58), dp(48)));
        record = new RecordView(this); footer.addView(record, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(footer);
        prev.setOnClickListener(v -> sendControl("previous"));
        pause.setOnClickListener(v -> sendControl("toggle"));
        next.setOnClickListener(v -> sendControl("next"));
        close.setOnClickListener(v -> stopSelf());
        DragTouchListener drag = new DragTouchListener(); handle.setOnTouchListener(drag); brand.setOnTouchListener(drag);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(310), WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL; p.y = dp(90);
        wm.addView(card, p); panel = card; attached = true;
    }
    private Button controlButton(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(16);
        b.setAllCaps(false); b.setPadding(0,0,0,0); b.setBackgroundColor(Color.TRANSPARENT); return b;
    }
    private void sendControl(String command) {
        Intent i = new Intent(ACTION_CONTROL); i.setPackage(getPackageName()); i.putExtra("command", command); sendBroadcast(i);
    }
    private String time(long ms) { long s=Math.max(0,ms/1000); return String.format(java.util.Locale.US,"%d:%02d",s/60,s%60); }
    private void updateText(String t, String a, String l, boolean playing, long position, long total) {
        if (!attached) createOverlay();
        if (title != null) title.setText(t == null ? "" : t);
        if (artist != null) artist.setText(a == null ? "" : a);
        if (lyric != null) lyric.setText(l == null || l.isEmpty() ? "♪" : l);
        if (progress != null) progress.setProgress(total > 0 ? (int)Math.min(1000, Math.max(0, position * 1000 / total)) : 0);
        if (elapsed != null) elapsed.setText(time(position));
        if (duration != null) duration.setText(time(total));
        if (record != null) record.setPlaying(playing);
    }
    private class DragTouchListener implements View.OnTouchListener {
        int x, y; float tx, ty;
        @Override public boolean onTouch(View v, MotionEvent e) {
            WindowManager.LayoutParams p = (WindowManager.LayoutParams)panel.getLayoutParams();
            if (e.getAction() == MotionEvent.ACTION_DOWN) { x=p.x; y=p.y; tx=e.getRawX(); ty=e.getRawY(); return true; }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                p.x=x+(int)(e.getRawX()-tx); p.y=y+(int)(e.getRawY()-ty); wm.updateViewLayout(panel,p); return true;
            }
            return e.getAction() == MotionEvent.ACTION_UP;
        }
    }
    private static class RecordView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler();
        private boolean spinning; private float angle;
        private final Runnable tick = new Runnable() { @Override public void run() { if (spinning) { angle=(angle+10)%360; invalidate(); handler.postDelayed(this, 25); } } };
        RecordView(android.content.Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        void setPlaying(boolean value) {
            if (spinning == value) return;
            spinning=value; handler.removeCallbacks(tick); if (spinning) handler.post(tick); invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float x=getWidth()/2f, y=getHeight()/2f, r=Math.min(x,y)-2;
            c.save(); c.rotate(angle,x,y);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(5,5,7)); c.drawCircle(x,y,r,paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dpStatic(getContext(),1.2f)); paint.setColor(Color.rgb(75,75,82));
            for(int i=1;i<7;i++) c.drawCircle(x,y,r*i/7f,paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(30,215,96)); c.drawCircle(x,y,r*.25f,paint);
            paint.setColor(Color.WHITE); c.drawCircle(x,y,r*.055f,paint);
            c.restore();
        }
        private static float dpStatic(android.content.Context c,float n) { return n*c.getResources().getDisplayMetrics().density; }
    }
    @Override public void onDestroy() {
        getSharedPreferences("spotify_native", MODE_PRIVATE).edit().putBoolean("overlay_enabled", false).apply();
        if (record != null) record.setPlaying(false);
        if (attached && wm != null) { wm.removeView(panel); attached=false; }
        stopForeground(true); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}