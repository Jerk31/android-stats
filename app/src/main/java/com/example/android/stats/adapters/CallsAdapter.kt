package com.example.android.stats.adapters

import android.provider.CallLog.Calls
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.android.stats.CallLogInfo
import com.example.android.stats.android.R
import com.example.android.stats.android.inflate
import com.example.android.stats.android.toPrettyString
import com.example.android.stats.inflate
import com.example.android.stats.toPrettyString
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class CallsAdapter(private val callLogs: List<CallLogInfo>) : RecyclerView.Adapter<CallsAdapter.CallLogHolder>() {
    override fun getItemCount() = callLogs.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogHolder {
        val inflatedView = parent.inflate(R.layout.recyclerview_item_row, false)
        return CallLogHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CallLogHolder, position: Int) {
        holder.bindCallLog(callLogs[position])
    }

    class CallLogHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var callLog: CallLogInfo? = null

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
//            val context = view.context
//            val showLogIntent = Intent(context, MainActivity::class.java)
//            showLogIntent.putExtra(CALL_LOG_KEY, callLog)
//            context.startActivity(showLogIntent)
            Log.d("AAA", "Clicked!")
        }

        fun bindCallLog(callLog: CallLogInfo) {
            this.callLog = callLog
            view.name.text = callLog.name ?: callLog.number
            view.number.text = callLog.number
            view.type.text = toString(callLog.callType)

            val dateFormatter = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.FRANCE)
            view.date.text = dateFormatter.format(callLog.date)

            view.duration.text = Duration.of(callLog.duration, ChronoUnit.SECONDS).toPrettyString()
        }

        private fun toString(type: Int): String {
            return when (type) {
                Calls.OUTGOING_TYPE -> "Outgoing"
                Calls.INCOMING_TYPE -> "Incoming"
                Calls.BLOCKED_TYPE -> "Blocked"
                else -> "Other"
            }
        }

        companion object {
            const val CALL_LOG_KEY = "CALL_LOG"
        }
    }
}