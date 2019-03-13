package com.example.mhealth;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class HealthDataObject extends RealmObject {
    @PrimaryKey
    @Required
    private String UID;
    private int minimumHeartrate;
    private int maximumHeartrate;
    private int averageHeartrate;
    private int stepsTaken;
    private double caloriesBurned;
    private long sleep;
    private Date date;

    public HealthDataObject () {}

    public HealthDataObject (int minimumHeartrate, int maximumHeartrate, int averageHeartrate,
                             int stepsTaken, double caloriesBurned, long sleep, Date date) {
        this.UID = UUID.randomUUID().toString();
        this.minimumHeartrate = minimumHeartrate;
        this.maximumHeartrate = maximumHeartrate;
        this.averageHeartrate = averageHeartrate;
        this.stepsTaken = stepsTaken;
        this.caloriesBurned = caloriesBurned;
        this.sleep = sleep;
        this.date = date;
    }

    public int getMinimumHeartrate() {
        return minimumHeartrate;
    }

    public void setMinimumHeartrate(int minimumHeartrate) {
        this.minimumHeartrate = minimumHeartrate;
    }

    public int getMaximumHeartrate() {
        return maximumHeartrate;
    }

    public void setMaximumHeartrate(int maximumHeartrate) {
        this.maximumHeartrate = maximumHeartrate;
    }

    public int getAverageHeartrate() {
        return averageHeartrate;
    }

    public void setAverageHeartrate(int averageHeartrate) {
        this.averageHeartrate = averageHeartrate;
    }

    public int getStepsTaken() {
        return stepsTaken;
    }

    public void setStepsTaken(int stepsTaken) {
        this.stepsTaken = stepsTaken;
    }

    public double getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(double caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public long getSleep() {
        return sleep;
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
