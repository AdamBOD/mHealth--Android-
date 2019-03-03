package com.example.mhealth;

import java.util.Date;

import io.realm.RealmObject;

public class SleepObject extends RealmObject {
    private int duration;
    private Date date;

    public SleepObject () {}

    public SleepObject (int duration, Date date) {
        this.duration = duration;
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
