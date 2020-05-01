package com.example.android.stats

import android.Manifest.permission.READ_CALL_LOG
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.android.stats.calls.*
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var menu: Menu
    private val spinnerValues = mutableListOf<CharSequence>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.topAppBar))
        layoutInflater.inflate(R.layout.call_stats, scrollView)

        if (getRuntimePermissions()) {
            setUpView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        this.menu = menu!!
        menuInflater.inflate(R.menu.top_app_bar, menu)

        if (checkSelfPermission(READ_CALL_LOG) == PERMISSION_GRANTED) {
            setUpMenu()
        }
        return true
    }

    private fun setUpView() {
        val chart: HorizontalBarChart = findViewById(R.id.callers_chart)
        chart.description.isEnabled = false
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(false)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.xOffset = -345f
        xAxis.isEnabled = true

        val yAxis = chart.axisLeft
        yAxis.setDrawAxisLine(true)
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return Duration.of(value.toLong(), ChronoUnit.SECONDS).toPrettyString()
            }
        }

        val yRight = chart.axisRight
        yRight.isEnabled = false

        chart.animateY(TimeUnit.SECONDS.toMillis(2).toInt())
    }

    private fun setUpMenu() {
        val spinner: Spinner = menu.findItem(R.id.spinner).actionView as Spinner
        spinnerValues.addAll(resources.getTextArray(R.array.call_filters))
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerValues)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position)
                if (getText(R.string.custom) == selectedItem) {
                    selectCustomTimeRange()
                    return
                }

                val timeRange = when (selectedItem) {
                    getText(R.string.today) -> getTodayTimeRange()
                    getText(R.string.this_week) -> getThisWeekTimeRange()
                    getText(R.string.last_week) -> getLastWeekTimeRange()
                    else -> null
                }
                if (timeRange != null) {
                    updateCallsData(timeRange)
                }
            }
        }
    }

    private fun updateCallsData(timeRange: Pair<LocalDateTime, LocalDateTime>) {
        val callLogs = getCallLogs(
            applicationContext,
            startDate = timeRange.first,
            endDate = timeRange.second
        )
        val callStats = generateStats(callLogs)
        var sortedIndividualCallStats = callStats.individualCalls.sortedByDescending(IndividualCallStats::totalTime)
        // Limit to 10 visible items
        if (sortedIndividualCallStats.size > 10) {
            val toMerge = sortedIndividualCallStats.subList(10, sortedIndividualCallStats.size)
            val reduced = toMerge.reduce { left, right ->
                right.calls.forEach { left.addCall(it) }
                left.name = "Other"
                left
            }
            sortedIndividualCallStats = sortedIndividualCallStats.subList(0, 9) + reduced
        }

        val totalCallsView: TextView = findViewById(R.id.total_calls)
        totalCallsView.text = Duration.of(callStats.totalTime, ChronoUnit.SECONDS).toPrettyString()

        val chart: HorizontalBarChart = findViewById(R.id.callers_chart)
        val xValues = sortedIndividualCallStats.map(IndividualCallStats::name).reversed()
        chart.xAxis.labelCount = xValues.size
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)

        val entries = sortedIndividualCallStats.map { BarEntry(xValues.indexOf(it.name).toFloat(), it.totalTime.toFloat()) }
        val dataset = BarDataSet(entries, "Time per caller")
        dataset.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataset.valueTextSize = 10f
        dataset.setDrawValues(false)

        val barData = BarData(dataset)
        barData.barWidth = 0.9f
        chart.data = barData
        chart.invalidate()
    }

    private fun refreshSpinner(timeRange: Pair<LocalDateTime, LocalDateTime>) {
        if (spinnerValues.last() != getText(R.string.custom)) {
            spinnerValues.removeAt(spinnerValues.lastIndex)
        }
        spinnerValues.add("From ${toDayMonth(timeRange.first)} to ${toDayMonth(timeRange.second)}")

        val spinner: Spinner = findViewById(R.id.spinner)
        (spinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        spinner.setSelection(spinnerValues.lastIndex)
    }

    private fun selectCustomTimeRange() {
        val pickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
        val now = Calendar.getInstance()
        pickerBuilder.setSelection(androidx.core.util.Pair(now.timeInMillis, now.timeInMillis))

        val picker = pickerBuilder.build()
        picker.addOnPositiveButtonClickListener {
            val selectedDates = Pair(toLocalDateTime(it.first!!), toLocalDateTime(it.second!!))
            refreshSpinner(selectedDates)
            updateCallsData(selectedDates)
        }

        picker.show(supportFragmentManager, picker.toString())
    }

    private fun getRuntimePermissions(): Boolean {
        if (checkSelfPermission(READ_CALL_LOG) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(READ_CALL_LOG), PERMISSIONS_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PERMISSION_GRANTED }) {
            setUpView()
            setUpMenu()
        } else {
            finish()
        }
    }

    companion object {
        const val PERMISSIONS_CODE = 123
    }
}
