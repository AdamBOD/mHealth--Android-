package com.example.mhealth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.samsung.android.sdk.accessory.SAAgentV2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class BackgroundService extends Service {
    public static boolean serviceRunning = false;
    private static final String TAG = "WatchService(C)";
    private static WatchService watchService = null;
    private static Broadcaster broadcaster = null;

    private static Boolean appInForeground = false;

    private Boolean dataToBeCompiled = false;
    private static Boolean exerciseReset = false;

    private static int heartrate = 0;
    private static int stepsTaken = 0;
    private static int caloriesBurned = 0;
    private static long sleep = 0;

    private String CHANNEL_ID = "mHealthChannel";

    private Interpreter interpreter;

    private SAAgentV2.RequestAgentCallback watchAgentCallback = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            logData("Agent has been successfully initialized");
            watchService = (WatchService) agent;
            QueryScheduler queryScheduler = new QueryScheduler();
            queryScheduler.startScheduler();
            checkExerciseReset();
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

        try {
            interpreter = new Interpreter(loadModelFile());
        } catch (Exception err) {
            logData("Error setting up TensorFlow (" + err.getMessage() + ")");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logData("Background Service Started");
        broadcaster = new Broadcaster();
        getTileData();
        serviceRunning = true;

        //Foreground Service code from https://codinginflow.com/tutorials/android/foreground-service
        Intent serviceIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, serviceIntent, 0);

        Notification serviceNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("mHealth")
                .setContentText("Reduce stress")
                .setSmallIcon(R.mipmap.ic_small)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher))
                .setPriority(Notification.PRIORITY_MIN)
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .build();

        //startForeground(1, serviceNotification);

        float[] inputValues = new float[11];
        inputValues[0] = 65;
        inputValues[1] = 100;
        inputValues[2] = 85;
        inputValues[3] = 6000;
        inputValues[4] = 265;
        inputValues[5] = 455;
        inputValues[6] = 1;
        inputValues[7] = 1;
        inputValues[8] = inputValues[4] / 6000;
        inputValues[9] = inputValues[5] / inputValues[2];
        inputValues[10] = inputValues[3] / inputValues[2];

        getMLOutput(inputValues);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
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
        SharedPreferences preferencesEditor = getSharedPreferences("mHealth", MODE_PRIVATE);
        String lastResetString = preferencesEditor.getString("exerciseReset", new Date().toString());

        if (lastResetString != null) {
            Date lastReset = new Date(lastResetString);

            if (lastReset.getDate() != new Date().getDate()) {
                watchService.setSensorRequest("Reset");
                watchService.findPeers();
                exerciseReset = true;

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

    public static String sleepToString (long sleepDuration) {
        long sleepMinutes = sleepDuration;
        String hoursString = " hours ";
        if (sleepMinutes < 60) {
            return ("0 hours "+ String.valueOf(sleepMinutes) + " minutes");
        }

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
                    if (exerciseReset) {
                        watchService.setSensorRequest("Reset");
                        watchService.findPeers();
                    }

                    if (currentMinutes < 10) {
                        intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(0));
                        if (currentMinutes == 1) {
                            if (currentHours == 0) {
                                logData("Resetting Data");
                                exerciseReset = true;
                                watchService.setSensorRequest("Reset");
                                watchService.findPeers();
                            }
                        }
                    } else {
                        if (currentMinutes == 30 || currentMinutes == 58) {
                            checkExercise = true;
                        }
                        intervalCheck = Character.toString(String.valueOf(currentMinutes).charAt(1));
                    }

                    if (currentHours == 23) {
                        if (currentMinutes == 59) {
                            compileDailyData();
                        }
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

    private void sendNotification (String title, String data) {
        /*Notification notificationBuilder = new NotificationCompat.Builder(this)
                .setAutoCancel((true));*/
    }

    public static void setResetExercise (Boolean reset) {
        exerciseReset = reset;
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

            float[] inputValues = new float[11];
            inputValues[0] = minHeartrate;
            inputValues[1] = maxHeartrate;
            inputValues[2] = averageHeartrate;
            inputValues[3] = exerciseObject.getSteps();
            inputValues[4] = (int) exerciseObject.getCaloriesBurned();
            inputValues[5] = sleepObject.getDuration();
            inputValues[6] = (sleepObject.getDuration() >= 450) ? 1 : 0;
            inputValues[7] = (exerciseObject.getSteps() >= 6000) ? 1 : 0;
            inputValues[8] = inputValues[4] / exerciseObject.getSteps();
            inputValues[9] = inputValues[5] / inputValues[2];
            inputValues[10] = inputValues[3] / inputValues[2];

            getMLOutput(inputValues);

        } catch (RuntimeException err) {
            logData("Error getting daily data (" + err.getMessage() + ")");
        }
    }

    private void getMLOutput (float[] inputArray) {
        float[][] outputArray = new float[1][2];

        interpreter.run (inputArray, outputArray);

        logData(String.valueOf("Unhealthy: " + outputArray[0][0] + " Healthy: " + outputArray[0][1]));

        if (outputArray[0][0] > outputArray[0][1]) {
            logData("Unhealthy");
        } else if (outputArray[0][0] < outputArray[0][1]) {
            logData("Healthy");
        }
    }

    private MappedByteBuffer loadModelFile () throws IOException {
        AssetFileDescriptor fileDescriptor = getApplicationContext().getAssets().openFd("mHealth_Model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    static void logData (String message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
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
        }).start();
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
