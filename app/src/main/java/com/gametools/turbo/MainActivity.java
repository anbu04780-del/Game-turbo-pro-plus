package com.gametools.turbo;

import android.app.*;
import android.app.NotificationManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView batteryText, perfText, netText;
    Handler handler = new Handler();
    long lastRx=-1,lastTx=-1;
    final int bg=Color.rgb(10,14,25), panel=Color.rgb(22,29,48), cyan=Color.rgb(0,229,255);
    Runnable refresher = new Runnable(){ public void run(){ updateStats(); handler.postDelayed(this,1000); }};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(18,22,18,18); root.setBackgroundColor(bg);
        scroll.addView(root); setContentView(scroll);
        text(root,"⚡ GAME TURBO PRO",27,cyan,true);
        text(root,"FLOATING GAMING TOOLKIT • v3.0",12,Color.LTGRAY,false);
        text(root,"Use the mini panel while playing. Some features need Android permission.",12,Color.LTGRAY,false);
        card("🪟 FLOATING WINDOW","Movable overlay with live network, battery and quick controls.","Enable Floating Window",v->enableFloating());
        card("🎮 GAME MODE","Launch a supported game if installed, or choose another app.","Choose / Launch Game",v->chooseGame());
        card("📶 NET SPEED","Live estimate of total phone network traffic.","Refresh Network Stats",v->updateStats());
        card("📊 PERFORMANCE MODE","Shows available RAM. Android controls real CPU/GPU and FPS.","Performance Details",v->showPerformance());
        card("🔋 BATTERY MODE","Open Android Battery Saver settings.","Open Battery Saver",v->batterySettings());
        card("🌙 DND MODE","Toggle Do Not Disturb after granting Notification Policy access.","Toggle DND",v->toggleDnd());
        batteryText=text(root,"🔋 Battery: reading…",15,Color.WHITE,true);
        perfText=text(root,"📊 RAM available: reading…",15,Color.WHITE,false);
        netText=text(root,"🌐 Network: measuring…",15,Color.WHITE,false);
        text(root,"Note: no root tricks or fake FPS boost. Android/device manufacturer controls performance.",11,Color.LTGRAY,false);
        handler.post(refresher);
    }
    TextView text(LinearLayout p,String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(4,7,4,7);p.addView(t);return t;
    }
    void card(String title,String desc,String action,View.OnClickListener listener){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(14,10,14,12);
        GradientDrawable gd=new GradientDrawable();gd.setColor(panel);gd.setCornerRadius(20);gd.setStroke(1,Color.rgb(42,64,91));box.setBackground(gd);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,7,0,7);root.addView(box,lp);
        text(box,title,17,Color.WHITE,true);text(box,desc,12,Color.LTGRAY,false);
        Button btn=new Button(this);btn.setText(action);btn.setTextColor(Color.BLACK);btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan));box.addView(btn);btn.setOnClickListener(listener);
    }
    void enableFloating(){
        if(!Settings.canDrawOverlays(this)){
            new AlertDialog.Builder(this).setTitle("Allow floating window")
            .setMessage("Enable 'Display over other apps' for Game Turbo Floating Pro, then return and tap the button again.")
            .setPositiveButton("Open Settings",(d,w)->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))))
            .setNegativeButton("Cancel",null).show();
        }else{
            startService(new Intent(this,FloatingService.class));
            Toast.makeText(this,"Floating panel started. Drag its title bar to move it.",Toast.LENGTH_LONG).show();
        }
    }
    void chooseGame(){
        String[] games={"Free Fire (if installed)","BGMI (if installed)","Call of Duty Mobile (if installed)","Choose an installed app"};
        new AlertDialog.Builder(this).setTitle("Game Mode").setItems(games,(d,n)->{
            String pkg=n==0?"com.dts.freefireth":n==1?"com.pubg.imobile":n==2?"com.activision.callofduty.shooter":null;
            if(pkg==null){ Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_HOME);startActivity(i);return; }
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch!=null)startActivity(launch);else Toast.makeText(this,"Game not found. Open it from your launcher.",Toast.LENGTH_LONG).show();
        }).show();
    }
    void showPerformance(){
        android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi=new android.app.ActivityManager.MemoryInfo();am.getMemoryInfo(mi);
        String s="Available RAM: "+(mi.availMem/1024/1024)+" MB\nTotal RAM: "+(mi.totalMem/1024/1024)+" MB\nLow memory: "+mi.lowMemory;
        perfText.setText("📊 "+s.replace("\n"," • "));
        new AlertDialog.Builder(this).setTitle("Performance details").setMessage(s+"\n\nAndroid apps without special system privileges cannot force a real CPU/GPU or FPS boost.").setPositiveButton("OK",null).show();
    }
    void batterySettings(){
        try{startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));}
        catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    }
    void toggleDnd(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(!nm.isNotificationPolicyAccessGranted()){
            new AlertDialog.Builder(this).setTitle("Allow DND access")
            .setMessage("Android requires Notification Policy access to switch Do Not Disturb.")
            .setPositiveButton("Open Settings",(d,w)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
            .setNegativeButton("Cancel",null).show();return;
        }
        try{
            boolean active=nm.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_NONE;
            nm.setInterruptionFilter(active?NotificationManager.INTERRUPTION_FILTER_NONE:NotificationManager.INTERRUPTION_FILTER_ALL);
            Toast.makeText(this,active?"Do Not Disturb ON":"Do Not Disturb OFF",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"DND could not be changed on this device.",Toast.LENGTH_LONG).show();}
    }
    void updateStats(){
        try{
            Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(b!=null){int l=b.getIntExtra("level",-1),s=b.getIntExtra("scale",100);if(l>=0)batteryText.setText("🔋 Battery: "+(l*100/Math.max(1,s))+"%");}
        }catch(Exception ignored){}
        android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi=new android.app.ActivityManager.MemoryInfo();am.getMemoryInfo(mi);
        perfText.setText("📊 RAM available: "+(mi.availMem/1024/1024)+" MB");
        long rx=android.net.TrafficStats.getTotalRxBytes(),tx=android.net.TrafficStats.getTotalTxBytes();
        if(lastRx>=0&&rx>=lastRx&&tx>=lastTx)netText.setText("🌐 Net: ↓ "+((rx-lastRx)/1024)+" KB/s  ↑ "+((tx-lastTx)/1024)+" KB/s");
        else netText.setText("🌐 Net: measuring…");
        lastRx=rx;lastTx=tx;
    }
    @Override protected void onDestroy(){handler.removeCallbacks(refresher);super.onDestroy();}
}
