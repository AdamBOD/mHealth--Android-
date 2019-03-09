package com.example.mhealth

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.fragment_sleep.*
import java.util.concurrent.ThreadLocalRandom

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HeartFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HeartFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SleepFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sleep, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createChart()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createChart () {
        val values = ArrayList<Entry>()
        for (i in 0..50) {
            values.add(Entry(i.toFloat(), ThreadLocalRandom.current().nextInt(100, 850 + 1).toFloat()))
        }
        val lineData = LineDataSet (values, "Time Slept")
        lineData.fillColor = Color.parseColor("#1976D2")
        lineData.color = Color.parseColor("#1976D2")
        lineData.lineWidth = 2f
        lineData.setDrawFilled(true)
        val drawable = context?.let { ContextCompat.getDrawable(it, R.drawable.chart_background) }
        lineData.fillDrawable = drawable
        lineData.valueTextSize = 0f
        lineData.setDrawValues(false)
        lineData.setDrawCircles(false)

        sleep_Chart.data = LineData(lineData)
        sleep_Chart.setDrawGridBackground(false)
        sleep_Chart.setDrawBorders(false)
        sleep_Chart.setDrawMarkers(false)
        sleep_Chart.disableScroll()
        sleep_Chart.axisLeft.axisMinimum = 20f //TODO - Average minus certain amount
        sleep_Chart.xAxis.isEnabled = false
        sleep_Chart.axisLeft.isEnabled = true
        sleep_Chart.axisRight.isEnabled = false
        sleep_Chart.description.text = ""
        sleep_Chart.legend.isEnabled = false

        sleep_Chart.axisLeft.setDrawAxisLine(false)
        sleep_Chart.axisLeft.setDrawGridLines(false)
        sleep_Chart.axisLeft.setDrawLabels(false)

        val averageLimit = LimitLine(480f, "Target")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        sleep_Chart.axisLeft.limitLines.add(0, averageLimit)
        sleep_Chart.axisLeft.setDrawLimitLinesBehindData(true)
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
                SleepFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
