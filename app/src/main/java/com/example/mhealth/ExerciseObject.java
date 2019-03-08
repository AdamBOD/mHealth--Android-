package com.example.mhealth;

import java.util.Date;

import io.realm.RealmObject;

public class ExerciseObject extends RealmObject {
    private int steps;
    private double caloriesBurned;
    private Date date;

    public ExerciseObject () {}

    public ExerciseObject (int steps, double caloriesBurned, Date date) {
        this.steps = steps;
        this.caloriesBurned = caloriesBurned;
        this.date = date;
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
