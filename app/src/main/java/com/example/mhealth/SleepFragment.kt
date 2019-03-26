package com.example.mhealth

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_sleep.*
import com.example.mhealth.BackgroundService.sleepToString

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
    private var healthDataObjects: RealmResults<HealthDataObject>? = null
    private var sumSleep: Int = 0
    private var averageSleep: Int = 0
    private var maxSleep: Int = 0
    private var minSleep: Int = 0

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
        sumSleep  = 0
        averageSleep = 0
        maxSleep = 0
        minSleep = 0

        healthDataObjects = MainActivity.getHistoricalData()
        val values = ArrayList<Entry>()
        for (i in 0..healthDataObjects!!.size - 1) {
            val entry = Entry(i.toFloat(), healthDataObjects!![i]!!.sleep.toFloat(), healthDataObjects!![i]!!.date.toString())
            values.add(entry)

            var dailySleep: Int = healthDataObjects!![i]!!.sleep.toInt()
            sumSleep += dailySleep

            if (minSleep == 0 && maxSleep == 0) {
                minSleep = dailySleep
                maxSleep = dailySleep
            }

            if (dailySleep > maxSleep) {
                maxSleep = dailySleep
            }

            if (dailySleep < minSleep) {
                minSleep = dailySleep
            }
        }

        if (healthDataObjects!!.size > 0) {
            averageSleep = sumSleep / healthDataObjects!!.size
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
        lineData.isHighlightEnabled = false

        sleep_Chart.data = LineData(lineData)
        sleep_Chart.setDrawGridBackground(false)
        sleep_Chart.setDrawBorders(false)
        sleep_Chart.setDrawMarkers(false)
        sleep_Chart.disableScroll()
        sleep_Chart.axisLeft.axisMinimum = 0f

        if (maxSleep <= 480) {
            sleep_Chart.axisLeft.axisMaximum = 500f
        }

        sleep_Chart.xAxis.isEnabled = false
        sleep_Chart.axisLeft.isEnabled = true
        sleep_Chart.axisRight.isEnabled = false
        sleep_Chart.description.text = ""
        sleep_Chart.legend.isEnabled = false

        sleep_Chart.axisLeft.setDrawAxisLine(false)
        sleep_Chart.axisLeft.setDrawGridLines(false)
        sleep_Chart.axisLeft.setDrawLabels(false)

        sleep_Chart.isDoubleTapToZoomEnabled = false
        sleep_Chart.isScaleYEnabled = false

        sleep_Chart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {

            }

            override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {

            }

            override fun onChartLongPressed(me: MotionEvent) {

            }

            override fun onChartDoubleTapped(me: MotionEvent) {

            }

            override fun onChartSingleTapped(me: MotionEvent) {

            }

            override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {

            }

            override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {
                if (sleep_Chart.scaleX >= 6) {
                    sleep_Chart.data.setDrawValues(true)
                    sleep_Chart.data.setValueTextSize(15f)
                } else {
                    sleep_Chart.data.setDrawValues(false)
                    sleep_Chart.data.setValueTextSize(0f)
                }
            }

            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {

            }
        }

        val averageLimit = LimitLine(480f, "8 Hours")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        sleep_Chart.axisLeft.limitLines.add(0, averageLimit)
        sleep_Chart.axisLeft.setDrawLimitLinesBehindData(true)

        average_Sleep.text = sleepToString(averageSleep.toLong())
        min_Sleep.text = sleepToString(minSleep.toLong())
        max_Sleep.text = sleepToString(maxSleep.toLong())
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
                SleepFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
