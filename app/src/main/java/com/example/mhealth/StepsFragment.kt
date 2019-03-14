package com.example.mhealth

import android.content.Context
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
import kotlinx.android.synthetic.main.fragment_steps.*
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
class StepsFragment : Fragment() {
    private var healthDataObjects: RealmResults<HealthDataObject>? = null
    private var sumSteps: Int = 0
    private var averageSteps: Int = 0
    private var maxSteps: Int = 0
    private var minSteps: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_steps, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createChart()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createChart () {
        healthDataObjects = MainActivity.getHistoricalData()
        val values = ArrayList<Entry>()
        for (i in 0..healthDataObjects!!.size - 1) {
            val entry = Entry(i.toFloat(), healthDataObjects!![i]!!.stepsTaken.toFloat(), healthDataObjects!![i]!!.date.toString())
            values.add(entry)

            var dailySteps: Int = healthDataObjects!![i]!!.stepsTaken.toInt()
            sumSteps += dailySteps

            if (minSteps == 0 && maxSteps == 0) {
                minSteps = dailySteps
                maxSteps = dailySteps
            }

            if (dailySteps > maxSteps) {
                maxSteps = dailySteps
            }

            if (dailySteps < minSteps) {
                minSteps = dailySteps
            }
        }

        if (healthDataObjects!!.size > 0) {
            averageSteps = sumSteps / healthDataObjects!!.size
        }

        values.add (Entry(4f, 6500f, "02/03"))
        val lineData = LineDataSet (values, "Steps Taken")
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

        steps_Chart.data = LineData(lineData)
        steps_Chart.setDrawGridBackground(false)
        steps_Chart.setDrawBorders(false)
        steps_Chart.setDrawMarkers(false)
        steps_Chart.disableScroll()
        steps_Chart.axisLeft.axisMinimum = 20f //TODO - Average minus certain amount
        steps_Chart.xAxis.isEnabled = false
        steps_Chart.axisLeft.isEnabled = true
        steps_Chart.axisRight.isEnabled = false
        steps_Chart.description.text = ""
        steps_Chart.legend.isEnabled = false

        steps_Chart.axisLeft.setDrawAxisLine(false)
        steps_Chart.axisLeft.setDrawGridLines(false)
        steps_Chart.axisLeft.setDrawLabels(false)

        steps_Chart.isDoubleTapToZoomEnabled = false
        steps_Chart.isScaleYEnabled = false

        steps_Chart.onChartGestureListener = object : OnChartGestureListener {
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
                if (steps_Chart.scaleX >= 6) {
                    steps_Chart.data.setDrawValues(true)
                    steps_Chart.data.setValueTextSize(15f)
                } else {
                    steps_Chart.data.setDrawValues(false)
                    steps_Chart.data.setValueTextSize(0f)
                }
            }

            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {

            }
        }

        val averageLimit = LimitLine(6000f, "Target")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        steps_Chart.axisLeft.limitLines.add(0, averageLimit)
        steps_Chart.axisLeft.setDrawLimitLinesBehindData(true)

        average_Steps.text = "$averageSteps Steps"
        min_Steps.text = "$minSteps Steps"
        max_Steps.text = "$maxSteps Steps"
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
                StepsFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
