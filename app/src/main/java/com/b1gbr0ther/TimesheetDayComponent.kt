package com.b1gbr0ther

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class TimesheetDayComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val dayText: TextView
    private val hoursText: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.component_timesheet_day, this, true)
        dayText = findViewById(R.id.dayNumberText)
        hoursText = findViewById(R.id.hoursWorkedText)
    }

    fun setDayNumber(day: String) {
        dayText.text = day
    }

    fun setHoursWorked(hours: String) {
        hoursText.text = "$hours h"
    }
}
