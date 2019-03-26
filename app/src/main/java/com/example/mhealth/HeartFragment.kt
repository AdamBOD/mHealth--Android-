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
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_heart.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HeartFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HeartFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HeartFragment : Fragment() {
    private var healthDataObjects: RealmResults<HealthDataObject>? = null
    private var sumHeartrate: Int = 0
    private var averageHeartrate: Int = 0
    private var maxHeartrate: Int = 0
    private var minHeartrate: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_heart, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createChart()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createChart () {
        sumHeartrate = 0
        averageHeartrate = 0
        minHeartrate = 0
        maxHeartrate = 0

        healthDataObjects = MainActivity.getHistoricalData()
        val values = ArrayList<Entry>()
        for (i in 0..healthDataObjects!!.size - 1) {
            val entry = Entry(i.toFloat(), healthDataObjects!![i]!!.averageHeartrate.toFloat(), healthDataObjects!![i]!!.date.toString())
            values.add(entry)

            var dailyHeartrate: Int = healthDataObjects!![i]!!.averageHeartrate.toInt()
            sumHeartrate += dailyHeartrate

            if (minHeartrate == 0 && maxHeartrate == 0) {
                minHeartrate = dailyHeartrate
                maxHeartrate = dailyHeartrate
            }

            if (dailyHeartrate > maxHeartrate) {
                maxHeartrate = dailyHeartrate
            }

            if (dailyHeartrate < minHeartrate) {
                minHeartrate = dailyHeartrate
            }
        }

        if (healthDataObjects!!.size > 0) {
            averageHeartrate = sumHeartrate / healthDataObjects!!.size
        }

        val lineData = LineDataSet (values, "Heart Rate")
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

        heart_Chart.data = LineData(lineData)
        heart_Chart.setDrawGridBackground(false)
        heart_Chart.setDrawBorders(false)
        heart_Chart.setDrawMarkers(false)
        heart_Chart.disableScroll()
        heart_Chart.axisLeft.axisMinimum = 20f
        heart_Chart.xAxis.isEnabled = false
        heart_Chart.axisLeft.isEnabled = true
        heart_Chart.axisRight.isEnabled = false
        heart_Chart.description.text = ""
        heart_Chart.legend.isEnabled = false

        heart_Chart.axisLeft.setDrawAxisLine(false)
        heart_Chart.axisLeft.setDrawGridLines(false)
        heart_Chart.axisLeft.setDrawLabels(false)

        heart_Chart.isDoubleTapToZoomEnabled = false
        heart_Chart.isScaleYEnabled = false

        heart_Chart.onChartGestureListener = object : OnChartGestureListener {
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
                if (heart_Chart.scaleX >= 6) {
                    heart_Chart.data.setDrawValues(true)
                    heart_Chart.data.setValueTextSize(15f)
                } else {
                    heart_Chart.data.setDrawValues(false)
                    heart_Chart.data.setValueTextSize(0f)
                }
            }

            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {

            }
        }

        val averageLimit = LimitLine(70f, "70 BPM")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        heart_Chart.axisLeft.limitLines.add(0, averageLimit)
        heart_Chart.axisLeft.setDrawLimitLinesBehindData(true)

        average_Heart.text = "$averageHeartrate BPM"
        min_Heart.text = "$minHeartrate BPM"
        max_Heart.text = "$maxHeartrate BPM"
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                HeartFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
