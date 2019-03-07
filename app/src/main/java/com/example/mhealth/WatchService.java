/*
Much of the code in this service has been adapted from the example application
that samsung packages with their Samsung Accessory Protocol SDK
 */

package com.example.mhealth;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;

import static com.example.mhealth.BackgroundService.logData;
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

    private String lastSleepStatus;
    private int lastSleepTimestamp;


    public WatchService(Context context) {
        super(TAG, context, SASOCKET_CLASS);
        mContext = context;

        SA gWatch = new SA();

        realmDBHandler = new RealmDBHandler();
        httpHandler = new HTTPHandler();

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
            Log.e("Watch Error","Disconnected");
        } else if (result == SAAgentV2.FINDPEER_SERVICE_NOT_FOUND) {
            logData ("FINDPEER_SERVICE_NOT_FOUND");
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
            //sendData("Sleep");
        } else if (result == SAAgentV2.CONNECTION_ALREADY_EXIST) {
            Log.d("Watch Success","Connected");
            sendData(getSensorRequest());
            //sendData("Sleep");
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
            final String message = new String(data);

            logData ("Received data: " + message);

            if (!receivedData) {
                if (!message.equals("undefined")) {
                    if (!message.equals("Error getting data from watch.")) {
                        receivedData = true;
                        retryConnection = false;
                        JSONObject jsonObject;

                        if (getSensorRequest().equals("Heart")) {
                            jsonObject = new JSONObject();
                            try {
                                jsonObject.put("userID", 200);
                                jsonObject.put("heartbeat", Integer.parseInt(message));
                                jsonObject.put("stepsTaken", 1200);
                                jsonObject.put("caloriesBurned", 2200);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            httpHandler.postData(jsonObject);

                            HeartrateObject heartrateObject = new HeartrateObject(Integer.parseInt(message), new Date());

                            updateData("Heart", String.valueOf(heartrateObject.getHeartrate()));

                            realmDBHandler.addToDB(heartrateObject);
                        } else if (getSensorRequest().equals("Exercise")) {
                            try {
                                 jsonObject = new JSONObject(message);

                                ExerciseObject exerciseObject = new ExerciseObject(
                                        Integer.parseInt(jsonObject.getJSONObject("stepCount").toString()),
                                        Integer.parseInt(jsonObject.getJSONObject("calories").toString()),
                                        new Date());

                                updateData("Steps", String.valueOf(exerciseObject.getSteps()));

                                realmDBHandler.addToDB(exerciseObject);
                            } catch (JSONException e) {
                                logData("Error converting Exercise data to JSON object (" + e.getMessage() + ")");
                                e.printStackTrace();
                            }
                        } else if (getSensorRequest().equals("Sleep")) {
                            try {
                                jsonObject = new JSONObject(message);

                                String currentStatus = jsonObject.getJSONObject("status").toString();

                                if (!lastSleepStatus.equals(currentStatus)) {
                                    if (lastSleepStatus.equals("ASLEEP")) {
                                        int duration = (Integer.parseInt(
                                                jsonObject.getJSONObject("timestamp").toString()
                                        ) - lastSleepTimestamp) / 60;

                                        SleepObject sleepObject = new SleepObject(duration, new Date());
                                        updateData("Sleep", String.valueOf(sleepObject.getDuration()));
                                        realmDBHandler.addToDB(sleepObject);
                                    } else if (lastSleepStatus.equals("AWAKE")) {
                                        lastSleepStatus = currentStatus;
                                        lastSleepTimestamp = Integer.parseInt(
                                                jsonObject.getJSONObject("timestamp").toString());
                                    } else {
                                        logData("Error getting Sleep data");
                                    }
                                }
                            } catch (JSONException e) {
                                logData("Error converting Sleep data to JSON object (" + e.getMessage() + ")");
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (!retryConnection) {
                            retryConnection = true;
                            logData ("Error getting data from watch, trying again");
                            sendData("Heart");
                        } else {
                            retryConnection = false;
                            logData ("Error getting data from watch, terminating");
                        }
                    }
                } else {
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

    public class LocalBinder extends Binder {
        public WatchService getService() {
            return WatchService.this;
        }
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean sendData(final String data) {
        boolean retvalue = false;
        if (mConnectionHandler != null) {
            try {
                mConnectionHandler.send(WATCH_CHANNEL_ID, data.getBytes());
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

    public boolean closeConnection() {
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
            /*RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                    .name("mHealth.realm")
                    .build();*/
        }

        public void addToDB (final HeartrateObject heartrate) {
            Realm.init(getApplicationContext());
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute (Realm realm) {
                    realm.copyToRealmOrUpdate(heartrate);
                }
            });
        }

        public void addToDB (SleepObject sleep) {

        }

        public void addToDB (ExerciseObject exercise) {

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
}

