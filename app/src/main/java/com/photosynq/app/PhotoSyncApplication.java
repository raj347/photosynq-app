package com.photosynq.app;

import android.app.Application;
import android.os.Handler;
import android.widget.Toast;

import com.photosynq.app.utils.NxDebugEngine;

/**
 * (c) Nexus-Computing GmbH Switzerland, 2015
 * Created by Manuel Di Cerbo on 10.11.15.
 */
public class PhotoSyncApplication extends Application {

    private final NxDebugEngine mDebugEngine = new NxDebugEngine();
    public static PhotoSyncApplication sApplication;
    private MainActivity mMainActivity;

    public void log(String message, String buffer, String fileName){
        mDebugEngine.dbg(message, buffer, fileName);
    }

    public void uploadLog(){
        mDebugEngine.uploadLogs();
    }

    public void registerActivity(MainActivity activity){
        mMainActivity = activity;
    }


    @Override
    public void onCreate() {
        sApplication = this;
        super.onCreate();
    }

    public void unRegisterActivity() {
        mMainActivity = null;
    }

    public void toast(final String fmt, final String ... args){
        if(mMainActivity != null){
            new Handler(mMainActivity.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mMainActivity, String.format(fmt, args), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
