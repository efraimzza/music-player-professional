package com.example.musicplayer;
import android.app.Application;
import java.io.FileWriter;
import android.icu.text.SimpleDateFormat;
import java.io.IOException;
import android.os.Process;
import java.util.Date;
import android.content.Intent;

public class App extends Application {
    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    //startActivity(new Intent(getApplicationContext(),debug.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    /*Intent intent = new Intent(getApplicationContext(), MDMSettingsActivity.class);
                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     intent.putExtra("err","");
                     //intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                     startActivity(intent);*/
                     Intent intent = new Intent(getApplicationContext(), debug.class);
                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     intent.putExtra("error", android.util.Log.getStackTraceString(throwable)+throwable.getStackTrace()[0].getClassName()+throwable.getStackTrace()[0].getMethodName()+throwable.getStackTrace()[0].getLineNumber()+throwable.getStackTrace()[0].getFileName());
                     startActivity(intent);
                    /*try {
                        String LOG_PATH = "/storage/emulated/0/log.txt";
                        FileWriter writer = new FileWriter(LOG_PATH, true);
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        writer.write("[" + time + "] " + throwable.toString() + "\n");
                        writer.close();
                    } catch (IOException ee) {
                        // silent
                    }*/
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            });
        super.onCreate();
        
    }
    
    
    
    
}
