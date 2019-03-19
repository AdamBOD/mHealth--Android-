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
import kotlinx.android.synthetic.main.fragment_calories.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HeartFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HeartFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class CaloriesFragment : Fragment() {
    private var healthDataObjects: RealmResults<HealthDataObject>? = null
    private var sumCaloriesBurned: Int = 0
    private var averageCaloriesBurned: Int = 0
    private var maxCaloriesBurned: Int = 0
    private var minCaloriesBurned: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calories, container, false)
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createChart()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createChart () {
        sumCaloriesBurned = 0
        averageCaloriesBurned = 0
        minCaloriesBurned = 0
        maxCaloriesBurned = 0

        healthDataObjects = MainActivity.getHistoricalData()
        val values = ArrayList<Entry>()
        for (i in 0..healthDataObjects!!.size - 1) {
            val entry = Entry(i.toFloat(), healthDataObjects!![i]!!.caloriesBurned.toFloat(), healthDataObjects!![i]!!.date.toString())
            values.add(entry)

            var dailyCaloriesBurned: Int = healthDataObjects!![i]!!.caloriesBurned.toInt()
            sumCaloriesBurned += dailyCaloriesBurned

            if (minCaloriesBurned == 0 && maxCaloriesBurned == 0) {
                minCaloriesBurned = dailyCaloriesBurned
                maxCaloriesBurned = dailyCaloriesBurned
            }

            if (dailyCaloriesBurned > maxCaloriesBurned) {
                maxCaloriesBurned = dailyCaloriesBurned
            }

            if (dailyCaloriesBurned < minCaloriesBurned) {
                minCaloriesBurned = dailyCaloriesBurned
            }
        }

        if (healthDataObjects!!.size > 0) {
            averageCaloriesBurned = sumCaloriesBurned / healthDataObjects!!.size
        }

        val lineData = LineDataSet (values, "Calories Burned")
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

        calories_Chart.data = LineData(lineData)
        calories_Chart.setDrawGridBackground(false)
        calories_Chart.setDrawBorders(false)
        calories_Chart.setDrawMarkers(false)
        calories_Chart.disableScroll()
        calories_Chart.axisLeft.axisMinimum = 0f

        if (maxCaloriesBurned <= 270) {
            calories_Chart.axisLeft.axisMaximum = 300f
        }

        calories_Chart.xAxis.isEnabled = false
        calories_Chart.axisLeft.isEnabled = true
        calories_Chart.axisRight.isEnabled = false
        calories_Chart.description.text = ""
        calories_Chart.legend.isEnabled = false

        calories_Chart.axisLeft.setDrawAxisLine(false)
        calories_Chart.axisLeft.setDrawGridLines(false)
        calories_Chart.axisLeft.setDrawLabels(false)

        calories_Chart.isDoubleTapToZoomEnabled = false
        calories_Chart.isScaleYEnabled = false

        calories_Chart.onChartGestureListener = object : OnChartGestureListener {
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
                if (calories_Chart.scaleX >= 6) {
                    calories_Chart.data.setDrawValues(true)
                    calories_Chart.data.setValueTextSize(15f)
                } else {
                    calories_Chart.data.setDrawValues(false)
                    calories_Chart.data.setValueTextSize(0f)
                }
            }

            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {

            }
        }

        val averageLimit = LimitLine(260f, "Target")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        calories_Chart.axisLeft.limitLines.add(0, averageLimit)
        calories_Chart.axisLeft.setDrawLimitLinesBehindData(true)

        average_Calories.text = "$averageCaloriesBurned kCal"
        min_Calories.text = "$minCaloriesBurned kCal"
        max_Calories.text = "$maxCaloriesBurned kCal"
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
                CaloriesFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
