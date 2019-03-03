package com.example.mhealth;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.samsung.android.sdk.accessory.SAAgentV2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

public class BackgroundService extends Service {
    public static boolean serviceRunning = false;
    private static final String TAG = "WatchService(C)";
    private WatchService watchService = null;

    private SAAgentV2.RequestAgentCallback watchAgentCallback = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            Log.d("Agent Initialized", "Agent has been successfully initialized");
            watchService = (WatchService) agent;
            QueryScheduler queryScheduler = new QueryScheduler();
            queryScheduler.startScheduler();
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
        SAAgentV2.requestAgent(getApplicationContext(), WatchService.class.getName(), watchAgentCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logData("Background Service Started");
        serviceRunning = true;
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
        serviceRunning = false;
        logData("Service stopped");
        super.onDestroy();
    }

    private class QueryScheduler {
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
                //intervalCheck = "0";
                if (intervalCheck.equals("5") || intervalCheck.equals("0")) {
                    //Toast.makeText(getApplicationContext(), "Querying watch", Toast.LENGTH_LONG).show();
                    if (watchService != null) {
                        watchService.findPeers();
                    }  else {
                        logData("WatchServiceAgent is null");
                        //Toast.makeText(getApplicationContext(), "WatchServiceAgent is null", Toast.LENGTH_LONG).show();
                    }

                }
                schedulerHandler.postAtTime(runnable, System.currentTimeMillis()+interval);
                schedulerHandler.postDelayed(runnable, interval);
            }
        };

        private void startScheduler () {
            runnable.run();
            if (watchService != null) {
                watchService.findPeers();
            }  else {
                logData("WatchServiceAgent is null");
                //Toast.makeText(getApplicationContext(), "WatchServiceAgent is null", Toast.LENGTH_LONG).show();
                watchService.findPeers();
            }
        }
    }
    static void logData (String message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AndroidNetworking.post("https://custom-logging-api.herokuapp.com/logs")
                .addJSONObjectBody(jsonObject) // posting json
                .setTag("")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // do anything with response
                    }
                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });
    }
}
