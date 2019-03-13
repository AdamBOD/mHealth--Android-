package com.example.mhealth;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

import java.util.Date;

public class TempHealthDataObject extends RealmObject {
    @PrimaryKey
    private int UID;
    private int stepsTaken;
    private double caloriesBurned;
    private String exerciseObjectUID;
    private String sleepStatus;
    private long sleepTimestamp;
    private Date date;

    public TempHealthDataObject () {}

    public TempHealthDataObject (int stepsTaken, double caloriesBurned, String exerciseObjectUID, String sleepStatus, long sleepTimestamp, Date date) {
        this.UID = 0;
        this.stepsTaken = stepsTaken;
        this.caloriesBurned = caloriesBurned;
        this.exerciseObjectUID = exerciseObjectUID;
        this.sleepStatus = sleepStatus;
        this.sleepTimestamp = sleepTimestamp;
        this.date = date;
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

    public String getExerciseObjectUID() {
        return exerciseObjectUID;
    }

    public void setExerciseObjectUID(String exerciseObjectUID) {
        this.exerciseObjectUID = exerciseObjectUID;
    }

    public String getSleepStatus() {
        return sleepStatus;
    }

    public void setSleepStatus(String sleepStatus) {
        this.sleepStatus = sleepStatus;
    }

    public long getSleepTimestamp() {
        return sleepTimestamp;
    }

    public void setSleepTimestamp(long sleepTimestamp) {
        this.sleepTimestamp = sleepTimestamp;
    }
}
