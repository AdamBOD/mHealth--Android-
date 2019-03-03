package com.example.mhealth;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;


public class HeartrateObject extends RealmObject {
    @PrimaryKey
    @Required
    private String UID;
    private int heartRate;
    private Date time;

    public HeartrateObject () {}

    public HeartrateObject (int heartRate, Date time) {
        this.heartRate = heartRate;
        this.time = time;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public int getHeartrate() {
        return heartRate;
    }

    public void setHeartrate(int heartRate) {
        this.heartRate = heartRate;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
