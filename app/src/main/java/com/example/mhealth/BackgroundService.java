package com.example.mhealth;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.accessory.SAAgentV2;

import java.util.Calendar;

public class BackgroundService extends Service {
    private static final String TAG = "WatchService(C)";
    private WatchService watchService = null;

    private SAAgentV2.RequestAgentCallback watchAgentCallback = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            Log.d("Agent Initialized", "Agent has been successfully initialized");
            watchService = (WatchService) agent;
        }

        @Override
        public void onError(int errorCode, String message) {
            Log.e(TAG, "Agent initialization error: " + errorCode + ". ErrorMsg: " + message);
        }
    };

    public BackgroundService() {
    }

    @Override
    public void onCreate () {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SAAgentV2.requestAgent(getApplicationContext(), WatchService.class.getName(), watchAgentCallback);
        //watchService.findPeers();
        //watchService.sendData("Heart");
        QueryScheduler queryScheduler = new QueryScheduler();
        queryScheduler.startScheduler();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        // Clean up connections
        if (watchService != null) {
            watchService.releaseAgent();
            watchService = null;
        }
        super.onDestroy();
    }

    public class QueryScheduler {
        private final int interval = 60000; // 1 Minute
        private Handler schedulerHandler = new Handler();
        private Runnable runnable = new Runnable(){
            public void run() {
                int currentMinutes = Calendar.getInstance().getTime().getMinutes();
                String intervalCheck;
                if (currentMinutes < 10) {
                    intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(0));
                } else {
                    intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(1));
                }

                // TODO: Remove this reassignment of the intervalCheck variable
                intervalCheck = "0";
                if (intervalCheck.equals("5") || intervalCheck.equals("0")) {
                    Toast.makeText(getApplicationContext(), "Querying watch", Toast.LENGTH_LONG).show();
                    if (watchService != null) {
                        watchService.findPeers();
                    }  else {
                        Toast.makeText(getApplicationContext(), "WatchServiceAgent is null", Toast.LENGTH_LONG).show();
                    }

                }
                schedulerHandler.postAtTime(runnable, System.currentTimeMillis()+interval);
                schedulerHandler.postDelayed(runnable, interval);
            }
        };

        public void startScheduler () {
            runnable.run();
        }
    }
}
