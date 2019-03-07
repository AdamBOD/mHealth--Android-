package com.example.mhealth;

import java.util.Date;

import io.realm.RealmObject;

public class ExerciseObject extends RealmObject {
    private int steps;
    private int minutesActive;
    private int caloriesBurned;
    private Date date;

    public ExerciseObject () {}

    public ExerciseObject (int steps, int caloriesBurned, Date date) {
        this.steps = steps;
        this.minutesActive = minutesActive;
        this.caloriesBurned = caloriesBurned;
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getMinutesActive() {
        return minutesActive;
    }

    public void setMinutesActive(int minutesActive) {
        this.minutesActive = minutesActive;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }
}
