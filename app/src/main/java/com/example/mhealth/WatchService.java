/*
Much of the code in this service has been adapted from the example application
that samsung packages with their Samsung Accessory Protocol SDK
 */

package com.example.mhealth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static android.content.Context.MODE_PRIVATE;
import static com.example.mhealth.BackgroundService.logData;
import static com.example.mhealth.BackgroundService.setResetExercise;
import static com.example.mhealth.BackgroundService.updateData;

public class WatchService extends SAAgentV2 {
    private static final String TAG = "WatchService(C)";
    private static final int WATCH_CHANNEL_ID = 104;
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private ServiceConnection mConnectionHandler = null;
    private static RealmDBHandler realmDBHandler = null;
    private HTTPHandler httpHandler = null;
    private Handler mHandler = new Handler();
    private Context mContext = null;
    private boolean receivedData = false;
    private boolean retryConnection = false;
    private String sensorRequest;

    private int lastStepCount;
    private int lastRefreshedStepCount;
    private double lastCalories;
    private double lastRefreshedCalories;
    private String exerciseObjectID;

    private String lastSleepStatus;
    private long lastSleepTimestamp;

    private TempHealthDataObject previousData;


    public WatchService(Context context) {
        super(TAG, context, SASOCKET_CLASS);
        mContext = context;

        SA gWatch = new SA();

        realmDBHandler = new RealmDBHandler();
        httpHandler = new HTTPHandler();

        getPreviousData();

        try {
            gWatch.initialize(mContext);
        } catch (SsdkUnsupportedException e) {
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception except) {
            except.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
        }
    }

    private void getPreviousData () {
        TempHealthDataObject previousDataObject = null;
        try {
            previousDataObject = realmDBHandler.getHealthData();
        } catch (RuntimeException err) {
            logData("Error getting previous data on boot (" + err.getMessage() + ")");
        }

        if (previousDataObject != null) {
            previousData = new TempHealthDataObject(previousDataObject.getStepsTaken(),
                    previousDataObject.getCaloriesBurned(),
                    previousDataObject.getExerciseObjectUID(),
                    previousDataObject.getSleepStatus(),
                    previousDataObject.getSleepTimestamp(),
                    previousDataObject.getRefreshedStepsTaken(),
                    previousDataObject.getRefreshedCaloriesBurned(),
                    new Date());

            lastStepCount = previousData.getStepsTaken();
            lastCalories = previousData.getCaloriesBurned();
            exerciseObjectID = previousData.getExerciseObjectUID();
            lastSleepStatus = previousData.getSleepStatus();
            lastSleepTimestamp = previousData.getSleepTimestamp();
            lastRefreshedStepCount = previousDataObject.getRefreshedStepsTaken();
            lastRefreshedCalories = previousDataObject.getRefreshedCaloriesBurned();
        } else {
            lastStepCount = 0;
            lastCalories = 0;
            exerciseObjectID = null;
            lastSleepStatus = null;
            lastSleepTimestamp = 0;
            lastRefreshedStepCount = 0;
            lastRefreshedCalories = 0;
            previousData = new TempHealthDataObject();
        }
    }

    public String getSensorRequest() {
        return sensorRequest;
    }

    public void setSensorRequest(String sensorRequest) {
        this.sensorRequest = sensorRequest;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if ((result == SAAgentV2.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgentV2.FINDPEER_DEVICE_NOT_CONNECTED) {
            logData ("FINDPEER_DEVICE_NOT_CONNECTED");
            if (getSensorRequest().equals("Reset")) {
                setResetExercise(true);
            }
            Log.e("Watch Error","Disconnected");
        } else if (result == SAAgentV2.FINDPEER_SERVICE_NOT_FOUND) {
            logData ("FINDPEER_SERVICE_NOT_FOUND");
            if (getSensorRequest().equals("Reset")) {
                setResetExercise(true);
            }
            Log.e("Watch Error","Disconnected");
        } else {
            //Toast.makeText(getApplicationContext(), "Could not find the watch", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        if (result == SAAgentV2.CONNECTION_SUCCESS) {
            this.mConnectionHandler = (ServiceConnection) socket;
            Log.d("Watch Success","Connected");
            sendData(getSensorRequest());
        } else if (result == SAAgentV2.CONNECTION_ALREADY_EXIST) {
            Log.d("Watch Success","Connected");
            sendData(getSensorRequest());
        } else if (result == SAAgentV2.CONNECTION_DUPLICATE_REQUEST) {
            //Toast.makeText(mContext, "CONNECTION_DUPLICATE_REQUEST", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(mContext, "Failed to connect", Toast.LENGTH_LONG).show();
            logData ("Failed to connect");
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peers != null) {
                    if (status == SAAgentV2.PEER_AGENT_AVAILABLE) {
                        //Toast.makeText(getApplicationContext(), "PEER_AGENT_AVAILABLE", Toast.LENGTH_LONG).show();
                    } else {
                        //Toast.makeText(getApplicationContext(), "PEER_AGENT_UNAVAILABLE", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            if (data == null) {
                logData("Empty Response, retrying");
                sendData("Retry");
                return;
            }
            String message = new String(data);
            if (!receivedData) {
                if (!message.equals("undefined")) {
                    if (!message.equals("Error getting data from watch.")) {
                        if (message.equals("ExerciseReset")) {
                            logData("Exercise Reset");
                            setResetExercise(false);
                            setSensorRequest("Exercise");
                            findPeers();
                            return;
                        }
                        receivedData = true;
                        retryConnection = false;
                        JsonObject receivedObject = new JsonParser().parse(message).getAsJsonObject();
                        logData(receivedObject.toString());

                        if (message.equals("{}")) {
                            return;
                        }

                        String sentType = receivedObject.get("type").getAsString();

                        if (sentType.equals("Heart")) {
                            JSONObject healthData = new JSONObject();
                            int averageHeartRate = receivedObject.get("heartrate").getAsInt();
                            if (averageHeartRate <= 0) {
                                return;
                            }
                            try {
                                healthData.put("userID", 200);
                                healthData.put("heartbeat", averageHeartRate);
                                healthData.put("stepsTaken", 1200);
                                healthData.put("caloriesBurned", 2200);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            httpHandler.postData(healthData);

                            HeartrateObject heartrateObject = new HeartrateObject(averageHeartRate, new Date());

                            updateData("Heart", String.valueOf(heartrateObject.getHeartrate()));

                            realmDBHandler.addToDB(heartrateObject);
                        } else if (sentType.equals("Exercise")) {
                            Gson g = new Gson();
                            ExerciseData exerciseData = g.fromJson(message, ExerciseData.class);
                            boolean addObject = false;
                            ExerciseObject exerciseObject;
                            if (exerciseData.getStepCount() < lastStepCount) {
                                SharedPreferences preferencesEditor = getApplicationContext().getSharedPreferences("mHealth", MODE_PRIVATE);
                                String lastResetString = preferencesEditor.getString("exerciseReset", new Date().toString());
                                if (new Date().getDate() != new Date(lastResetString).getDate()) {
                                    addObject = true;
                                    lastStepCount = (int) exerciseData.getStepCount();
                                    lastCalories = exerciseData.getCalories();

                                    lastRefreshedStepCount = 0;
                                    lastRefreshedCalories = 0;
                                } else {
                                    if (lastRefreshedStepCount == 0 || lastRefreshedCalories == 0) {
                                        lastStepCount += exerciseData.getStepCount();
                                        lastCalories += exerciseData.getCalories();

                                        lastRefreshedStepCount = (int) exerciseData.getStepCount();
                                        lastRefreshedCalories = exerciseData.getCalories();
                                    } else {
                                        lastStepCount += (exerciseData.getStepCount() - lastRefreshedStepCount);
                                        lastCalories += (exerciseData.getCalories() - lastRefreshedCalories);

                                        lastRefreshedStepCount = (int) exerciseData.getStepCount();
                                        lastRefreshedCalories = exerciseData.getCalories();
                                    }
                                }

                                exerciseObject = new ExerciseObject(
                                        lastStepCount,
                                        lastCalories,
                                        new Date());
                                realmDBHandler.addToDB(exerciseObject);
                            } else {
                                lastStepCount = (int) exerciseData.getStepCount();
                                lastCalories = exerciseData.getCalories();

                                previousData.setStepsTaken(lastStepCount);
                                previousData.setCaloriesBurned(lastCalories);

                                realmDBHandler.setHealthData(previousData);

                                exerciseObject = new ExerciseObject(
                                        lastStepCount,
                                        lastCalories,
                                        new Date());
                            }

                            updateData("Steps", String.valueOf(exerciseObject.getSteps()));
                            updateData("Calories", String.valueOf((int) Math.round(exerciseObject.getCaloriesBurned())));

                            if (addObject || exerciseObjectID == null) {
                                exerciseObjectID = exerciseObject.getUID();
                                previousData.setExerciseObjectUID(exerciseObjectID);
                                realmDBHandler.addToDB(exerciseObject);
                                realmDBHandler.setHealthData(previousData);

                                SharedPreferences.Editor sharedPreferencesEditor = getApplicationContext().getSharedPreferences("mHealth", MODE_PRIVATE).edit();
                                sharedPreferencesEditor.putString("exerciseReset", new Date().toString());
                                sharedPreferencesEditor.apply();
                            } else {
                                exerciseObject.setUID(exerciseObjectID);
                                realmDBHandler.addToDB(exerciseObject);
                            }
                        } else if (sentType.equals("Sleep")) {
                            Gson g = new Gson();
                            SleepData sleepData = g.fromJson(message, SleepData.class);
                            String currentStatus = sleepData.getStatus();

                            if (lastSleepStatus != null) {
                                if (!lastSleepStatus.equals(currentStatus)) {
                                    if (lastSleepStatus.equals("ASLEEP")) {
                                        if (new Date().getTime() - sleepData.getTimestamp() >= 360000) {
                                            long duration = ((sleepData.getTimestamp() - lastSleepTimestamp) / 1000) / 60;
                                            if (duration > 20) {
                                                logData("Duration of Sleep: " + String.valueOf(duration));
                                                SleepObject sleepObject = new SleepObject(duration, new Date());
                                                realmDBHandler.addToDB(sleepObject);
                                            }

                                            lastSleepStatus = currentStatus;
                                            previousData.setSleepStatus(lastSleepStatus);
                                            realmDBHandler.setHealthData(previousData);
                                        }
                                    } else if (lastSleepStatus.equals("AWAKE")) {
                                        lastSleepStatus = currentStatus;
                                        lastSleepTimestamp = sleepData.getTimestamp();

                                        previousData.setSleepStatus(lastSleepStatus);
                                        previousData.setSleepTimestamp(lastSleepTimestamp);
                                        realmDBHandler.setHealthData(previousData);
                                    } else {
                                        logData("Error getting Sleep data");
                                    }
                                }
                            } else {
                                lastSleepStatus = currentStatus;
                                previousData.setSleepStatus(lastSleepStatus);
                                realmDBHandler.setHealthData(previousData);
                            }
                        }
                    } else {
                        if (getSensorRequest().equals("Heart")) {
                            if (!retryConnection) {
                                retryConnection = true;
                                receivedData = true;
                                logData ("Error getting data from watch, trying again");
                                sendData("Retry");
                            } else {
                                retryConnection = false;
                                receivedData = true;
                                logData ("Error getting data from watch, terminating");
                            }
                        } else {
                            sendData("Retry");
                        }
                    }
                } else {
                    receivedData = true;
                    logData ("Error getting data from watch, data undefined, retrying");
                    sendData("Retry");
                }
            }
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            closeConnection();
        }
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean sendData(final String data) {
        boolean retvalue = false;
        if (mConnectionHandler != null && data != null) {
            try {
                mConnectionHandler.send(WATCH_CHANNEL_ID, data.getBytes());
                if (getSensorRequest().equals("Heart")) {
                    retryConnection = true;
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!receivedData && retryConnection) {
                                setSensorRequest("Retry");
                                sendData(getSensorRequest());
                                retryConnection = false;
                            } else if (!receivedData && !retryConnection) {
                                logData("Cancelling heart rate check");
                                setSensorRequest("StopHeart");
                            }
                        }
                    };

                    setTimeout(runnable, 30000);
                }
                logData("Querying: " + getSensorRequest());
                receivedData = false;
                retvalue = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("Sent: ", data);
        }
        return retvalue;
    }


    //Async Timeout code from https://stackoverflow.com/questions/26311470/what-is-the-equivalent-of-javascript-settimeout-in-java
    private static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }

    private boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            return true;
        } else {
            return false;
        }
    }

    /*Method taken from official example app provided with Samsung Accessory Protocol SDK*/
    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }

    public class RealmDBHandler {
        public RealmDBHandler () {}

        public void prepareRealmDB () {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
        }

        public void addToDB (final HeartrateObject heartrate) {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.copyToRealmOrUpdate(heartrate);
                }
            });
            realm.close();
        }

        public void addToDB (final SleepObject sleepData) {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();

            Date endDate = new Date();
            Date startDate = new Date();
            startDate.setHours(0);
            startDate.setMinutes(0);
            startDate.setSeconds(0);
            final SleepObject sleepResult;
            SleepObject sleepResults;
            try {
                sleepResults = realm.where(SleepObject.class).between("date", startDate, endDate).findFirst();
            } catch (RuntimeException err) {
                logData("Error getting previous sleep object from the day");
                sleepResults = null;
            }

            sleepResult = sleepResults;
            if (sleepResult != null) {
                sleepData.setDuration(sleepData.getDuration() + sleepResult.getDuration());
            }

            updateData("Sleep", String.valueOf(sleepData.getDuration()));

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.copyToRealmOrUpdate(sleepData);
                }
            });

            if (sleepResults != null) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute (Realm realm) {
                        sleepResult.deleteFromRealm();
                    }
                });
            }
            realm.close();
        }

        public void addToDB (final ExerciseObject exerciseData) {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.insertOrUpdate(exerciseData);
                }
            });
            realm.close();
        }

        public TempHealthDataObject getHealthData () {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();
            try {
                TempHealthDataObject healthData = realm.where(TempHealthDataObject.class).findFirst();
                realm.close();
                return healthData;
            } catch (RuntimeException err) {
                logData("Previous data empty");
                return null;
            }

        }
        public void setHealthData (final TempHealthDataObject healthData) {
            Realm.init(getApplicationContext());
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.insertOrUpdate(healthData);
                }
            });
            realm.close();
        }

    }

    public class HTTPHandler {
        public HTTPHandler () {}

        public void postData (JSONObject data) {
            AndroidNetworking.post("https://mhealth-api-fyp.herokuapp.com/data")
                    .addJSONObjectBody(data) // posting json
                    .setTag("")
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONArray(new JSONArrayRequestListener() {
                        @Override
                        public void onResponse(JSONArray response) {
                            // do anything with response

                            logData ("Successfully sent data");
                        }
                        @Override
                        public void onError(ANError error) {
                            // handle error
                            logData ("Failed to send data (" + error.getMessage() + ")" + "(" + error.getErrorDetail() + ")");
                        }
                    });
        }
    }

    class HeartData {
        private String type;
        private int heartrate;

        public HeartData (String type, int heartrate) {
            this.type = type;
            this.heartrate = heartrate;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getHeartrate() {
            return heartrate;
        }

        public void setHeartrate(int heartrate) {
            this.heartrate = heartrate;
        }
    }

    class SleepData {
        private String type;
        private String status;
        private long timestamp;

        public SleepData (String type, String status, int timestamp) {
            this.type = type;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    class ExerciseData {
        private String type;
        private long stepCount;
        private double calories;
        private double frequency;

        public ExerciseData (String type, long stepCount, double calories, double frequency) {
            this.type = type;
            this.stepCount = stepCount;
            this.calories = calories;
            this.frequency = frequency;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getStepCount() {
            return stepCount;
        }

        public void setStepCount(long stepCount) {
            this.stepCount = stepCount;
        }

        public double getCalories() {
            return calories;
        }

        public void setCalories(double calories) {
            this.calories = calories;
        }

        public double getFrequency() {
            return frequency;
        }

        public void setFrequency(double frequency) {
            this.frequency = frequency;
        }
    }
}

