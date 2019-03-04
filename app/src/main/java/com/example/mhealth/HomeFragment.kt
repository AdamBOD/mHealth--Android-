package com.example.mhealth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onStart() {
        BackgroundService.updateAppState(true)
        if (dataToBeLoaded) {
            tile_Heartrate.text = heartRate
            dataToBeLoaded = false
        }
        super.onStart()
    }

    private val broadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive (context: Context?, intent: Intent) {

            // TODO: Send JSON instead
            if (intent.getStringExtra("contentType").equals("Heart")) {
                val newBPM = intent.getStringExtra("data") + " BPM"
                if (tile_Heartrate == null) {
                    heartRate = newBPM
                    dataToBeLoaded = true
                    return
                }
                tile_Heartrate.text = newBPM
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
        // TODO: Update argument type and name
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
