package com.example.mhealth;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ExerciseObject extends RealmObject {
    @PrimaryKey
    @Required
    private String UID;
    private int steps;
    private double caloriesBurned;
    private Date date;

    public ExerciseObject () {}

    public ExerciseObject (int steps, double caloriesBurned, Date date) {
        this.UID = UUID.randomUUID().toString();
        this.steps = steps;
        this.caloriesBurned = caloriesBurned;
        this.date = date;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public double getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }
}
