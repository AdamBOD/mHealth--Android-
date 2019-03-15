package com.example.mhealth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
    }

    override fun onStart() {
        BackgroundService.updateAppState(true)
        if (dataToBeLoaded) {
            tile_Heartrate.text = heartRate
            tile_StepsTaken.text = stepsTaken
            tile_CaloriesBurned.text = caloriesBurned
            tile_Sleep.text = sleep
            dataToBeLoaded = false
        }
        super.onStart()
    }

    private val broadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive (context: Context?, intent: Intent) {
            if (intent.getStringExtra("contentType").equals("Heart")) {
                val newBPM = intent.getStringExtra("data") + " BPM"
                if (tile_Heartrate == null) {
                    heartRate = newBPM
                    dataToBeLoaded = true
                    return
                }
                tile_Heartrate.text = newBPM
            } else if (intent.getStringExtra("contentType").equals("Steps")) {
                val newSteps = intent.getStringExtra("data") + " Steps"
                if (tile_StepsTaken == null) {
                    stepsTaken = newSteps
                    dataToBeLoaded = true
                    return
                }
                tile_StepsTaken.text = newSteps
            } else if (intent.getStringExtra("contentType").equals("Calories")) {
                val calories = intent.getStringExtra("data") + " kCal"
                if (tile_Heartrate == null) {
                    caloriesBurned = calories
                    dataToBeLoaded = true
                    return
                }
                tile_CaloriesBurned.text = calories
            } else if (intent.getStringExtra("contentType").equals("Sleep")) {
                val timeSlept = intent.getStringExtra("data")
                if (tile_Sleep == null) {
                    sleep = timeSlept
                    dataToBeLoaded = true
                    return
                }
                tile_Sleep.text = timeSlept
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
