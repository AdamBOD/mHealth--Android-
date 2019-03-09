package com.example.mhealth;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class SleepObject extends RealmObject {
    @PrimaryKey
    @Required
    private String UID;
    private long duration;
    private Date date;

    public SleepObject () {}

    public SleepObject (long duration, Date date) {
        this.UID = UUID.randomUUID().toString();
        this.duration = duration;
        this.date = date;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
