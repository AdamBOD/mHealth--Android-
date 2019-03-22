package com.example.mhealth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.mhealth.BackgroundService.sleepToString
import kotlinx.android.synthetic.main.fragment_home.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HomeFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HomeFragment : Fragment() {
    private var dataToBeLoaded: Boolean = false
    private var heartRate: String = ""
    private var stepsTaken: String = ""
    private var caloriesBurned: String = ""
    private var sleep: String = ""
    private var healthRating: String = ""
    private var healthRecommendation: String = ""
    private var stepsProgress: Int = 0
    private var caloriesProgress: Int = 0
    private var sleepProgress: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        LocalBroadcastManager.getInstance(activity!!.applicationContext).registerReceiver(broadcastReceiver,
                IntentFilter("contentUpdated"))
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val outputValues = MainActivity.getReccomendation()
        health_Rating.text = outputValues[0]
        health_Recommendation.text = outputValues[1]
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStart() {
        BackgroundService.updateAppState(true)
        if (dataToBeLoaded) {
            tile_Heartrate.text = heartRate
            tile_StepsTaken.text = stepsTaken
            tile_CaloriesBurned.text = caloriesBurned
            tile_Sleep.text = sleep

            steps_Bar.progress = stepsProgress
            if (stepsProgress == 100) {
                steps_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                        android.graphics.PorterDuff.Mode.SRC_IN)
            }

            calories_Bar.progress = caloriesProgress
            if (caloriesProgress == 100) {
                calories_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                        android.graphics.PorterDuff.Mode.SRC_IN)
            }

            sleep_Bar.progress = sleepProgress
            if (sleepProgress == 100) {
                sleep_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                        android.graphics.PorterDuff.Mode.SRC_IN)
            }

            dataToBeLoaded = false
        }
        super.onStart()
    }

    private val broadcastReceiver = object: BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive (context: Context?, intent: Intent) {
            if (intent.getStringExtra("contentType").equals("Heart")) {
                val newBPM = intent.getStringExtra("data") + " BPM"
                heartRate = newBPM
                if (tile_Heartrate == null) {
                    dataToBeLoaded = true
                    return
                }
                tile_Heartrate.text = newBPM
            } else if (intent.getStringExtra("contentType").equals("Steps")) {
                val stepsInt = intent.getStringExtra("data").toInt()

                if (stepsInt >= 6000) {
                    stepsProgress = 100
                    if (steps_Bar != null) {
                        steps_Bar.progress = stepsProgress
                        steps_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                                android.graphics.PorterDuff.Mode.SRC_IN)
                    } else {
                        dataToBeLoaded = true
                    }
                } else {
                    stepsProgress = ((stepsInt.toDouble() / 6000.toDouble()) * 100).toInt()
                    if (steps_Bar != null) {
                        steps_Bar.progress = stepsProgress
                    } else {
                        dataToBeLoaded = true
                    }
                }

                val newSteps = intent.getStringExtra("data") + " Steps"
                stepsTaken = newSteps
                if (tile_StepsTaken == null) {
                    dataToBeLoaded = true
                    return
                }
                tile_StepsTaken.text = newSteps
            } else if (intent.getStringExtra("contentType").equals("Calories")) {
                val caloriesInt = intent.getStringExtra("data").toInt()

                if (caloriesInt >= 260) {
                    caloriesProgress = 100
                    if (calories_Bar != null) {
                        calories_Bar.progress = caloriesProgress
                        calories_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                                android.graphics.PorterDuff.Mode.SRC_IN)
                    } else {
                        dataToBeLoaded = true
                    }

                } else {
                    caloriesProgress = ((caloriesInt.toDouble() / 260.toDouble()) * 100).toInt()
                    if (calories_Bar != null) {
                        calories_Bar.progress = caloriesProgress
                    } else {
                        dataToBeLoaded = true
                    }
                }

                val calories = intent.getStringExtra("data") + " kCal"
                caloriesBurned = calories
                if (tile_Heartrate == null) {
                    dataToBeLoaded = true
                    return
                }
                tile_CaloriesBurned.text = calories
            } else if (intent.getStringExtra("contentType").equals("Sleep")) {
                val sleepInt = intent.getStringExtra("data").toInt()

                if (sleepInt >= 450) {
                    sleepProgress = 100
                    if (sleep_Bar != null) {
                        sleep_Bar.progress = sleepProgress
                        sleep_Bar.progressDrawable.setColorFilter(Color.parseColor("#1fd219"),
                                android.graphics.PorterDuff.Mode.SRC_IN)
                    } else {
                        dataToBeLoaded = true
                    }
                } else {
                    sleepProgress = ((sleepInt.toDouble() / 450.toDouble()) * 100).toInt()
                    if (sleep_Bar != null) {
                        sleep_Bar.progress = sleepProgress
                    } else {
                        dataToBeLoaded = true
                    }
                }

                var timeSlept = intent.getStringExtra("data")
                sleep = sleepToString(timeSlept.toLong())
                timeSlept = sleep
                if (tile_Sleep == null) {
                    dataToBeLoaded = true
                    return
                }
                tile_Sleep.text = timeSlept
            } else if (intent.getStringExtra("contentType").equals("Rating")) {
                val rating = intent.getStringExtra("data")
                healthRating = rating
                if (health_Rating == null) {
                    dataToBeLoaded = true
                    return
                }
                health_Rating.text = rating
            } else if (intent.getStringExtra("contentType").equals("Recommendation")) {
                val recommendation = intent.getStringExtra("data")
                healthRecommendation = recommendation
                if (health_Recommendation == null) {
                    dataToBeLoaded = true
                    return
                }
                health_Recommendation.text = recommendation
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                HomeFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
