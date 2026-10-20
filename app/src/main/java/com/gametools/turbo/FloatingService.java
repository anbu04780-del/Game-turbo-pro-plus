package com.gametools.turbo;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.TrafficStats;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class FloatingService extends Service {
    WindowManager wm; View panel; WindowManager.LayoutParams params;
    Handler handler=new Handler(); TextView stats;
    long lastRx=-1,lastTx=-1;
    Runnable refresher=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
    @Override public void onCreate(){
        super.onCreate();
        if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(10,8,10,8);
        GradientDrawable gd=new GradientDrawable();gd.setColor(Color.argb(242,12,18,32));gd.setCornerRadius(20);gd.setStroke(2,Color.rgb(0,229,255));box.setBackground(gd);
        TextView head=new TextView(this);head.setText("⚡ TURBO  •  DRAG");head.setTextColor(Color.rgb(0,229,255));head.setTypeface(null,Typeface.BOLD);head.setTextSize(14);head.setPadding(8,8,8,8);box.addView(head);
        stats=new TextView(this);stats.setTextColor(Color.WHITE);stats.setTextSize(12);stats.setText("Battery • RAM • Network");stats.setPadding(8,5,8,5);box.addView(stats);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);box.addView(row);
        Button dnd=new Button(this);dnd.setText("DND");row.addView(dnd);
        Button battery=new Button(this);battery.setText("Battery");row.addView(battery);
        Button close=new Button(this);close.setText("✕");row.addView(close);
        dnd.setOnClickListener(v->{
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(!nm.isNotificationPolicyAccessGranted()){
                try { Intent i=new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
                catch(Exception ignored) {}
            } else {
                try {
                    boolean currentlyOn=nm.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_NONE;
                    nm.setInterruptionFilter(currentlyOn?NotificationManager.INTERRUPTION_FILTER_ALL:NotificationManager.INTERRUPTION_FILTER_NONE);
                    Toast.makeText(this,currentlyOn?"DND OFF":"DND ON",Toast.LENGTH_SHORT).show();
                } catch(Exception ignored) {}
            }
        });
        battery.setOnClickListener(v->{try{Intent i=new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}});
        close.setOnClickListener(v->stopSelf());
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        params=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.END;params.x=18;params.y=140;
        wm.addView(box,params);panel=box;
        head.setOnTouchListener(new View.OnTouchListener(){int sx,sy;float tx,ty;
            public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=params.x;sy=params.y;tx=e.getRawX();ty=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=sx+(int)(tx-e.getRawX());params.y=sy+(int)(e.getRawY()-ty);try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}return true;}
                return false;
            }});
        handler.post(refresher);
    }
    void refresh(){
        try{
            Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level=b==null?-1:b.getIntExtra("level",-1),scale=b==null?100:b.getIntExtra("scale",100);
            android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi=new android.app.ActivityManager.MemoryInfo();am.getMemoryInfo(mi);
            long rx=TrafficStats.getTotalRxBytes(),tx=TrafficStats.getTotalTxBytes();
            String net=(lastRx>=0&&rx>=lastRx&&tx>=lastTx)?"\nNet ↓"+((rx-lastRx)/1024)+" ↑"+((tx-lastTx)/1024)+" KB/s":"\nNet: measuring…";
            stats.setText("Battery: "+(level<0?"?":(level*100/Math.max(1,scale))+"%")+"\nRAM free: "+(mi.availMem/1024/1024)+" MB"+net);
            lastRx=rx;lastTx=tx;
        }catch(Exception ignored){}
    }
    @Override public int onStartCommand(Intent i,int flags,int startId){return START_NOT_STICKY;}
    @Override public void onDestroy(){handler.removeCallbacks(refresher);if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
