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
import kotlinx.android.synthetic.main.progress_overlay.*
import kotlinx.android.synthetic.main.stats.*
import kotlinx.android.synthetic.main.stats_total.*
import kotlinx.coroutines.*
import java.time.LocalDateTime

class StatsFragment<T>(private var statsProvider: StatsProvider<T>) : Fragment(), CoroutineScope by MainScope() {
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

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (statsProvider.onRuntimePermissionsUpdated(requestCode, permissions, grantResults)) {
            timeRange?.let { onRangeSelected(it) }
        } else {
            Log.e("StatsFragment", "NotEnoughPermissions to display data")
        }
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
        chart.isDoubleTapToZoomEnabled = false
        chart.isScaleYEnabled = true
        chart.isScaleXEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = false
        chart.setHighlighter(object : HorizontalBarHighlighter(chart) {
            override fun buildHighlights(set: IDataSet<*>, dataSetIndex: Int, xVal: Float, rounding: DataSet.Rounding): List<Highlight> {
                var entries: List<Entry> = set.getEntriesForXValue(xVal)
                if (entries.isEmpty()) {
                    // Try to find closest x-value
                    val closest: Entry? = set.getEntryForXValue(xVal, Float.NaN, rounding)
                    entries = closest?.let { listOf(it) } ?: emptyList()
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
                showHideDetailedStats(false)
            }

            @Suppress("UNCHECKED_CAST")
            override fun onValueSelected(e: Entry, h: Highlight) {
                if (e.data == null) {
                    return
                }
                displayDetailedStats(e.data as T)
            }
        })

        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.textColor = activity!!.getColor(R.color.design_default_color_on_primary)
        xAxis.isEnabled = true
        xAxis.granularity = 1f

        val yAxis = chart.axisLeft
        yAxis.setDrawAxisLine(true)
        yAxis.setLabelCount(5, true)
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
        this.timeRange = timeRange
        missing_permissions_card.visibility = View.GONE
        showHideDetailedStats(false)
        resetChart(chart)

        launch {
            // See https://heartbeat.fritz.ai/handling-background-tasks-with-kotlin-coroutines-in-android-a674bab7a951
            fetchDataInBackground(timeRange)
        }
    }

    private fun onDataFetched(data: List<T>) {
        total_amount.text = statsProvider.computeTotal(data)

        if (data.isEmpty()) {
            chart.data = null
            chart.invalidate()
            return
        }

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
        dataset.colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.JOYFUL_COLORS.toList() + ColorTemplate.COLORFUL_COLORS.toList()
        dataset.valueTextSize = 10f
        dataset.setDrawValues(false)

        val barData = BarData(dataset)
        barData.barWidth = 0.9f

        chart.data = barData
        chart.invalidate()
    }

    private fun displayDetailedStats(data: T) {
        showHideDetailedStats(true)
        statsProvider.showDetailedStats(detailed_stats_card, data)
    }

    private fun onMissingPermissions() {
        missing_permissions_card.visibility = View.VISIBLE
        missing_permissions_text.text = statsProvider.getMissingPermissionsMessage()
    }

    private suspend fun fetchDataInBackground(timeRange: Pair<LocalDateTime, LocalDateTime>) {
        if (!statsProvider.checkRuntimePermissions()) {
            withContext(Dispatchers.Main) {
                onMissingPermissions()
            }
            return
        }

        withContext(Dispatchers.Main) {
            progress_overlay.animate(View.VISIBLE, 0.4f, 200)
        }

        // Fetch data in IO dispatcher, then display them in Main dispatcher when available
        val data = withContext(Dispatchers.IO) {
            statsProvider.getDataForRange(timeRange)
        }
        withContext(Dispatchers.Main) {
            progress_overlay.animate(View.GONE, 0f, 200)
            onDataFetched(data)
        }
    }

    private fun resetChart(chart: HorizontalBarChart) {
        chart.fitScreen()
        chart.clear()
    }

    private fun showHideDetailedStats(show: Boolean) {
        details_placeholder_card.visibility = if (show) View.GONE else View.VISIBLE
        detailed_stats_card.visibility = if (show) View.VISIBLE else View.GONE
    }
}