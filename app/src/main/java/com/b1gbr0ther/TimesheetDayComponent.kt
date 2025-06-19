package com.b1gbr0ther

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import androidx.cardview.widget.CardView

class TimesheetDayComponent @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  private val dayText: TextView
  private val hoursText: TextView
  private val card: CardView

  init {
    orientation = VERTICAL
    LayoutInflater.from(context).inflate(R.layout.component_timesheet_day, this, true)
    dayText = findViewById(R.id.dayNumberText)
    hoursText = findViewById(R.id.hoursWorkedText)
    card = findViewById(R.id.timesheetDayCard)
  }

  fun setDayNumber(day: String) {
    dayText.text = day
  }

  fun setHoursWorked(hours: String) {
    hoursText.text = "$hours h"

    val hourInt: Int

    if (hours.contains("h")) {
      hourInt = hours.replace("h", "").trim().toInt()
    } else {
      hourInt = hours.trim().toInt()
    }

    val color: Int


    if (hourInt <= 1) {
      color = Color.parseColor("#8F8194")
    } else if (hourInt == 2) {
      color = Color.parseColor("#887A90")
    } else if (hourInt == 3) {
      color = Color.parseColor("#82738B")
    } else if (hourInt == 4) {
      color = Color.parseColor("#7B6D86")
    } else if (hourInt == 5) {
      color = Color.parseColor("#756681")
    } else if (hourInt == 6) {
      color = Color.parseColor("#6F5F7C")
    } else if (hourInt == 7) {
      color = Color.parseColor("#695877")
    } else {
      color = Color.parseColor("#5F4B66")
    }

    card.setCardBackgroundColor(color)
  }

  override fun setOnClickListener(l: OnClickListener?) {
    super.setOnClickListener(l)
    card.setOnClickListener(l)
  }
}
