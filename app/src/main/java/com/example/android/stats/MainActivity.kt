package com.example.android.stats

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentTransaction
import com.example.android.stats.calls.CallsStats
import com.example.android.stats.network.AppNetworkStats
import com.example.android.stats.usage.AppUsageStats
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.time.LocalDateTime
import java.util.*


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var selectedNavItem = -1
    private lateinit var fragment: StatsFragment<*>
    private val spinnerValues = mutableListOf<CharSequence>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(topAppBar)

        val drawerToggle = ActionBarDrawerToggle(this, drawer_layout, topAppBar, R.string.drawer_open, R.string.drawer_close)
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        if (savedInstanceState == null) {
            onNavigationItemSelected(nav_view.menu.findItem(R.id.nav_calls))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.top_app_bar, menu)
        setUpMenu(menu!!)
        return true
    }

    private fun setUpMenu(menu: Menu) {
        val spinner: Spinner = menu.findItem(R.id.spinner).actionView as Spinner
        spinnerValues.addAll(resources.getTextArray(R.array.time_filters))
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerValues)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem: String = parent?.getItemAtPosition(position) as String
                if (getText(R.string.custom) == selectedItem) {
                    selectCustomTimeRange()
                    return
                }
                fragment.onRangeSelected(getSelectedTimeRange(selectedItem))
            }
        }
    }

    private fun getSelectedTimeRange(selection: String?): Pair<LocalDateTime, LocalDateTime> {
        return when (selection) {
            null, getText(R.string.today) -> getTodayTimeRange()
            getText(R.string.this_week) -> getThisWeekTimeRange()
            getText(R.string.last_week) -> getLastWeekTimeRange()
            else -> getTimeRangeFromString(selection)
        }
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
            val startTime = toLocalDateTime(it.first!!)
            val endTime = toLocalDateTime(it.second!!)
            refreshSpinner(Pair(startTime, endTime))
        }

        picker.show(supportFragmentManager, picker.toString())
    }

    override fun onBackPressed() {
        val drawer = drawer_layout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == selectedNavItem) {
            drawer_layout.closeDrawer(GravityCompat.START)
            return false
        }

        val statsProvider = when (item.itemId) {
            R.id.nav_calls -> CallsStats(this)
            R.id.nav_app_usage -> AppUsageStats(this)
            R.id.nav_app_network -> AppNetworkStats(this)
            else -> throw IllegalStateException("Case not handled!")
        }

        showFragment(StatsFragment(statsProvider))
        topAppBar.title = statsProvider.getPageTitle()
        selectedNavItem = item.itemId
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showFragment(fragment: StatsFragment<*>) {
        this.fragment = fragment

        val spinner: Spinner = findViewById(R.id.spinner)
        val selectedTimeRange = getSelectedTimeRange(spinner.selectedItem as String?)
        val arguments = bundleOf(Pair(SELECTED_RANGE, selectedTimeRange))
        fragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_main, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    companion object {
        const val SELECTED_RANGE = "selected_range"
    }
}
