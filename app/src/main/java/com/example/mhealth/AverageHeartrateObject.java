package com.example.mhealth;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class AverageHeartrateObject extends RealmObject {
    @PrimaryKey
    @Required
    private String UID;
    private int averageHeartrate;
    private int minHeartrate;
    private int maxHeartrate;
    private Date date;

    public AverageHeartrateObject () {}

    public AverageHeartrateObject (int averageHeartrate, int minHeartrate, int maxHeartrate, Date date) {
        this.UID = UUID.randomUUID().toString();
        this.averageHeartrate = averageHeartrate;
        this.minHeartrate = minHeartrate;
        this.maxHeartrate = maxHeartrate;
        this.date = date;
    }

    public int getAverageHeartrate() {
        return averageHeartrate;
    }

    public void setAverageHeartrate(int averageHeartrate) {
        this.averageHeartrate = averageHeartrate;
    }

    public int getMinHeartrate() {
        return minHeartrate;
    }

    public void setMinHeartrate(int minHeartrate) {
        this.minHeartrate = minHeartrate;
    }

    public int getMaxHeartrate() {
        return maxHeartrate;
    }

    public void setMaxHeartrate(int maxHeartrate) {
        this.maxHeartrate = maxHeartrate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
