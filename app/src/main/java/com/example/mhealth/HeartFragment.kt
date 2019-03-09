package com.example.mhealth

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.components.LimitLine
import kotlinx.android.synthetic.main.fragment_heart.*
import android.support.v4.content.ContextCompat
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.components.Description
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
class HeartFragment : Fragment() {

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
        val values = ArrayList<Entry>()
        for (i in 0..50) {
            values.add(Entry(i.toFloat(), ThreadLocalRandom.current().nextInt(50, 90 + 1).toFloat()))
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

        heart_Chart.data = LineData(lineData)
        heart_Chart.setDrawGridBackground(false)
        heart_Chart.setDrawBorders(false)
        heart_Chart.setDrawMarkers(false)
        heart_Chart.disableScroll()
        heart_Chart.axisLeft.axisMinimum = 20f //TODO - Average minus certain amount
        heart_Chart.xAxis.isEnabled = false
        heart_Chart.axisLeft.isEnabled = true
        heart_Chart.axisRight.isEnabled = false
        heart_Chart.description.text = ""
        heart_Chart.legend.isEnabled = false

        heart_Chart.axisLeft.setDrawAxisLine(false)
        heart_Chart.axisLeft.setDrawGridLines(false)
        heart_Chart.axisLeft.setDrawLabels(false)

        val averageLimit = LimitLine(60f, "Target")
        averageLimit.lineWidth = 4f
        averageLimit.lineColor = Color.parseColor("#9E9E9E")
        averageLimit.enableDashedLine(30f, 10f, 0f)
        averageLimit.labelPosition = LimitLabelPosition.RIGHT_BOTTOM
        averageLimit.textSize = 15f

        heart_Chart.axisLeft.limitLines.add(0, averageLimit)
        heart_Chart.axisLeft.setDrawLimitLinesBehindData(true)
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
                HeartFragment().apply {
                    arguments = Bundle().apply {}
                }
    }
}
