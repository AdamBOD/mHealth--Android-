package com.example.mhealth;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.exceptions.RealmError;
import io.realm.exceptions.RealmException;

public class BackgroundService extends Service {
    public static boolean serviceRunning = false;
    private static final String TAG = "WatchService(C)";
    private static WatchService watchService = null;
    private static Broadcaster broadcaster = null;

    private static Boolean appInForeground = false;

    private Boolean dataToBeCompiled = false;

    private static int heartrate = 0;
    private static int stepsTaken = 0;
    private static int caloriesBurned = 0;
    private static long sleep = 0;

    private SAAgentV2.RequestAgentCallback watchAgentCallback = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            Log.d("Agent Initialized", "Agent has been successfully initialized");
            watchService = (WatchService) agent;
            watchService.sendData("Init");
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
        broadcaster = new Broadcaster();
        getTileData();
        checkExerciseReset();
        serviceRunning = true;
        return START_STICKY;
    }

    private final IBinder localBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        logData("Service is bound");
        return localBinder;
    }

    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onDestroy() {
        if (watchService != null) {
            watchService.releaseAgent();
            watchService = null;
        }
        serviceRunning = false;
        logData("Service stopped");
        super.onDestroy();
    }

    public void getTileData () {
        logData("Getting tile data");
        Realm.init(getApplicationContext());
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("mHealth.realm")
                .schemaVersion(0)
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        Realm realm = Realm.getDefaultInstance();

        try {
            HeartrateObject heartrateObject = realm.where(HeartrateObject.class).sort("time").findAll().last();
            heartrate = heartrateObject.getHeartrate();
            broadcaster.sendContentUpdate("Heart", String.valueOf(heartrate));

            TempHealthDataObject exerciseObject = realm.where(TempHealthDataObject.class).findFirst();
            stepsTaken = exerciseObject.getStepsTaken();
            caloriesBurned = (int) Math.round(exerciseObject.getCaloriesBurned());
            broadcaster.sendContentUpdate("Steps", String.valueOf(stepsTaken));
            broadcaster.sendContentUpdate("Calories", String.valueOf(caloriesBurned));

            SleepObject sleepObject = realm.where(SleepObject.class).sort("date").findAll().last();
            sleep = sleepObject.getDuration();
            broadcaster.sendContentUpdate("Sleep", sleepToString(sleep));
        } catch (RuntimeException err) {
            logData ("No Realm data");
            broadcaster.sendContentUpdate("Heart", String.valueOf(heartrate));
            broadcaster.sendContentUpdate("Steps", String.valueOf(stepsTaken));
            broadcaster.sendContentUpdate("Calories", String.valueOf(caloriesBurned));
            broadcaster.sendContentUpdate("Sleep", sleepToString(sleep));
        }
        realm.close();
    }

    private void checkExerciseReset () {
        logData("Checking Exercise Reset");
        SharedPreferences preferencesEditor = getSharedPreferences("mHealth", MODE_PRIVATE);
        String lastResetString = preferencesEditor.getString("exerciseReset", new Date().toString());

        if (lastResetString != null) {
            Date lastReset = new Date(lastResetString);

            if (lastReset.getDay() != new Date().getDay()) {
                watchService.setSensorRequest("Reset");
                watchService.findPeers();

                dataToBeCompiled = true;
            }
        } else {
            logData("Last Reset is null");
            SharedPreferences.Editor sharedPreferencesEditor = getApplicationContext().getSharedPreferences("mHealth", MODE_PRIVATE).edit();
            sharedPreferencesEditor.putString("exerciseReset", new Date().toString());
            sharedPreferencesEditor.apply();
        }
    }

    public static void updateAppState (Boolean appState) {
        if (appState) {
            if (broadcaster == null) {
                return;
            }
            broadcaster.sendContentUpdate("Heart", String.valueOf(heartrate));
            broadcaster.sendContentUpdate("Steps", String.valueOf(stepsTaken));
            broadcaster.sendContentUpdate("Calories", String.valueOf(caloriesBurned));
            broadcaster.sendContentUpdate("Sleep", sleepToString(sleep));

            watchService.setSensorRequest("Exercise");
            watchService.findPeers();
        }

        appInForeground = appState;
    }

    private static String sleepToString (long sleepDuration) {
        long sleepMinutes = TimeUnit.MILLISECONDS.toMinutes(sleepDuration);
        String hoursString = " hours ";
        if (sleepMinutes > 600) {
            hoursString = " hrs ";
        }
        return (String.valueOf(sleepMinutes / 60) + hoursString + String.valueOf(sleepMinutes % 60) + " minutes");
    }

    public static void updateData (String type, String data) {
        if (type.equals("Heart")) {
            heartrate = Integer.parseInt(data);
        } else if (type.equals("Steps")) {
            stepsTaken = Integer.parseInt(data);
        } else if (type.equals("Calories")) {
            caloriesBurned = Integer.parseInt(data);
        } else if (type.equals("Sleep")) {
            sleep = Long.parseLong(data);
        }
        broadcaster.sendContentUpdate(type, data);
    }

    private class QueryScheduler {
        private final int interval = 60000; // 1 Minute
        private boolean checkExercise = false;
        private Handler schedulerHandler = new Handler();
        private Runnable runnable = new Runnable(){
            public void run() {
                int currentHours = Calendar.getInstance().getTime().getHours();
                int currentMinutes = Calendar.getInstance().getTime().getMinutes();
                String intervalCheck;

                if (dataToBeCompiled) {
                    compileDailyData();
                    dataToBeCompiled = false;
                }

                if (watchService != null) {
                    if (currentMinutes < 10) {
                        intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(0));
                        if (currentMinutes == 1) {
                            if (currentHours == 0) {
                                logData("Resetting Data");
                                watchService.setSensorRequest("Reset");
                                watchService.findPeers();
                            }
                        }
                    } else {
                        if (currentMinutes == 30 || currentMinutes == 58) {
                            checkExercise = true;
                        } else if (currentMinutes == 59) {
                            compileDailyData();
                        }
                        intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(1));
                    }

                    if (intervalCheck.equals("5") || intervalCheck.equals("0")) {
                            if (checkExercise) {
                                watchService.setSensorRequest("Exercise");
                                watchService.findPeers();
                                checkExercise = false;
                            } else {
                                watchService.setSensorRequest("Heart");
                                watchService.findPeers();
                            }

                    } else if (intervalCheck.equals("3") || intervalCheck.equals("8")){
                        if (checkExercise) {
                            watchService.setSensorRequest("Exercise");
                            watchService.findPeers();
                            checkExercise = false;
                        } else {
                            watchService.setSensorRequest("Sleep");
                            watchService.findPeers();
                        }
                    }
                }  else {
                    logData("WatchServiceAgent is null");
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
                watchService.findPeers();
            }
        }
    }

    public void compileDailyData () {
        logData("Compiling Daily Data");
        Realm.init(getApplicationContext());
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("mHealth.realm")
                .schemaVersion(0)
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        Realm realm = Realm.getDefaultInstance();
        try {
            SleepObject sleepObject = realm.where(SleepObject.class).sort("date").findAll().last();
            ExerciseObject exerciseObject = realm.where(ExerciseObject.class).sort("date").findAll().last();
            long dayinMilliseconds = 1000 * 60 *60 *24;
            Date endDate = new Date();
            Date startDate = new Date(endDate.getTime() - dayinMilliseconds);
            final RealmResults<HeartrateObject> heartrateResults = realm.where(HeartrateObject.class).between("time", startDate, endDate).findAll();

            int minHeartrate = 0;
            int maxHeartrate = 0;
            int averageHeartrate = 0;
            for (int i=0; i < heartrateResults.size(); i++) {
                int heartrateResult = heartrateResults.get(i).getHeartrate();
                if (minHeartrate == 0 && maxHeartrate == 0) {
                    maxHeartrate = heartrateResult;
                    minHeartrate = heartrateResult;
                }

                if (heartrateResult > maxHeartrate) {
                    maxHeartrate = heartrateResult;
                }

                if (heartrateResult < minHeartrate) {
                    minHeartrate = heartrateResult;
                }

                averageHeartrate += heartrateResult;
            }

            averageHeartrate = averageHeartrate / heartrateResults.size();

            final HealthDataObject healthDataObject = new HealthDataObject(minHeartrate, maxHeartrate,
                    averageHeartrate, exerciseObject.getSteps(), exerciseObject.getCaloriesBurned(),
                    sleepObject.getDuration(), new Date());

            logData("Daily Data Object: " + healthDataObject.toString());
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.copyToRealmOrUpdate(healthDataObject);
                }
            });

            final AverageHeartrateObject averageHeartrateObject = new AverageHeartrateObject(averageHeartrate,
                    minHeartrate, maxHeartrate, new Date());
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.copyToRealmOrUpdate(averageHeartrateObject);
                }
            });
            realm.close();
        } catch (RuntimeException err) {
            logData("Error getting daily data (" + err.getMessage() + ")");
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

    public class Broadcaster {
        public Broadcaster () {}

        public void sendContentUpdate (String type, String data) {
            Intent intent = new Intent("contentUpdated");
            intent.putExtra("contentType", type);
            intent.putExtra("data", data);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
