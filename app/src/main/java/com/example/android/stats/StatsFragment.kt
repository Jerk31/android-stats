package com.example.android.stats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.android.stats.MainActivity.Companion.SELECTED_RANGE
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.highlight.HorizontalBarHighlighter
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.stats.*
import java.time.LocalDateTime

class StatsFragment<T>(private var statsProvider: StatsProvider<T>) : Fragment() {
    private var timeRange: Pair<LocalDateTime, LocalDateTime>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.stats, container, false)
        setUpView(view)

        @Suppress("UNCHECKED_CAST")
        timeRange = arguments?.getSerializable(SELECTED_RANGE) as Pair<LocalDateTime, LocalDateTime>?

        if (!statsProvider.checkRuntimePermissions()) {
            statsProvider.requestPermissions()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        LayoutInflater.from(detailed_stats_card.context).inflate(statsProvider.getDetailedStatsLayout(), detailed_stats_card)
    }

    override fun onResume() {
        super.onResume()
        timeRange?.let { onRangeSelected(it) }
    }

    private fun setUpView(v: View) {
        val totalImage: ImageView = v.findViewById(R.id.total_image)
        totalImage.setImageResource(statsProvider.getTotalIcon())

        val totalText: TextView = v.findViewById(R.id.total_text)
        totalText.text = statsProvider.getTotalText()

        val chartText: TextView = v.findViewById(R.id.chart_text)
        chartText.text = statsProvider.getStatsText()

        val chart: HorizontalBarChart = v.findViewById(R.id.chart)
        chart.description.isEnabled = false
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(false)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setHighlighter(object : HorizontalBarHighlighter(chart) {
            override fun buildHighlights(set: IDataSet<*>, dataSetIndex: Int, xVal: Float, rounding: DataSet.Rounding): List<Highlight> {
                var entries: List<Entry> = set.getEntriesForXValue(xVal)
                if (entries.isEmpty()) {
                    // Try to find closest x-value
                    val closest: Entry = set.getEntryForXValue(xVal, Float.NaN, rounding)
                    entries = listOf(closest)
                }

                return entries.map { e ->
                    val pixels = mChart.getTransformer(set.axisDependency).getPixelForValues(e.y, e.x)
                    Highlight(
                        e.x, e.y,
                        pixels.x.toFloat(), pixels.y.toFloat(),
                        dataSetIndex, set.axisDependency
                    )
                }.toList()
            }
        })
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
            }

            @Suppress("UNCHECKED_CAST")
            override fun onValueSelected(e: Entry, h: Highlight) {
                if (e.data == null) {
                    return
                }
                displayDetailedStats(e.data as T)
            }
        })
        chart.legend.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.textColor = activity!!.getColor(R.color.design_default_color_on_primary)
        xAxis.isEnabled = true

        val yAxis = chart.axisLeft
        yAxis.setDrawAxisLine(true)
        yAxis.textColor = activity!!.getColor(R.color.design_default_color_on_primary)
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return statsProvider.formatY(value)
            }
        }

        val yRight = chart.axisRight
        yRight.isEnabled = false
    }

    fun onRangeSelected(timeRange: Pair<LocalDateTime, LocalDateTime>) {
        detailed_stats_card.visibility = View.INVISIBLE
        chart.highlightValue(null)

        this.timeRange = timeRange
        val data = statsProvider.getDataForRange(timeRange)

        val totalCallsView: TextView = view!!.findViewById(R.id.total_amount)
        totalCallsView.text = statsProvider.computeTotal(data)

        val chart: HorizontalBarChart = view!!.findViewById(R.id.chart)
        val xValues = statsProvider.getXValues(data)
        chart.xAxis.labelCount = xValues.size
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)

        val entries = data.map {
            BarEntry(
                xValues.indexOf(statsProvider.dataToX().invoke(it)).toFloat(),
                statsProvider.dataToY().invoke(it),
                it
            )
        }

        val dataset = BarDataSet(entries, "Time per caller")
        dataset.colors = ColorTemplate.COLORFUL_COLORS.toList() + ColorTemplate.JOYFUL_COLORS.toList() + ColorTemplate.MATERIAL_COLORS.toList()
        dataset.valueTextSize = 10f
        dataset.setDrawValues(false)

        val barData = BarData(dataset)
        barData.barWidth = 0.9f
        chart.data = barData
        chart.invalidate()
    }

    fun displayDetailedStats(data: T) {
        detailed_stats_card.visibility = View.VISIBLE
        statsProvider.showDetailedStats(detailed_stats_card, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (statsProvider.onRuntimePermissionsUpdated(requestCode, permissions, grantResults)) {
            timeRange?.let { onRangeSelected(it) }
        } else {
            Log.e("StatsFragment", "NotEnoughPermissions to display data")
        }
    }
}