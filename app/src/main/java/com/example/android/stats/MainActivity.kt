package com.example.android.stats

import android.Manifest.permission.READ_CALL_LOG
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.stats.adapters.CallsAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private val callsData = mutableListOf<CallLogInfo>()
    private val spinnerValues = mutableListOf<CharSequence>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (getRuntimePermissions()) {
            setUpView()
        }
    }

    private fun setUpView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CallsAdapter(callsData)

        spinnerValues.addAll(resources.getTextArray(R.array.call_filters))
        val spinner: Spinner = findViewById(R.id.spinner)
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
        callsData.clear()
        callsData.addAll(getCallLogs(applicationContext, startDate = timeRange.first, endDate = timeRange.second))
        (recyclerView.adapter as CallsAdapter).notifyDataSetChanged()
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
        if (requestCode == PERMISSIONS_CODE && grantResults.all { it == PERMISSION_GRANTED }) {
            setUpView()
        } else {
            finish()
        }
    }

    companion object {
        const val PERMISSIONS_CODE = 123
    }
}
