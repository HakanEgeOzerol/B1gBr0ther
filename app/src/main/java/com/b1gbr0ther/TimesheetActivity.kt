package com.b1gbr0ther

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import android.app.AlertDialog
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class TimesheetActivity : AppCompatActivity() {

  private lateinit var currentYearMonth: YearMonth

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_timesheet)

    val menu = findViewById<MenuBar>(R.id.menuBar)
    menu.setActivePage(2)

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timesheetLayout)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    currentYearMonth = loadSelectedMonthYear()
    updateCalendarUI()

    val yearButton = findViewById<Button>(R.id.yearSelector)
    val monthButton = findViewById<Button>(R.id.monthSelector)

    yearButton.setOnClickListener {
      showYearOverlay()
    }

    monthButton.setOnClickListener {
      showMonthOverlay()
    }
  }

  private fun setCurrentMonth(month: Int) {
    currentYearMonth = YearMonth.of(currentYearMonth.year, month)
    saveSelectedMonthYear()
    updateCalendarUI()
  }

  private fun setCurrentYear(year: Int) {
    currentYearMonth = YearMonth.of(year, currentYearMonth.monthValue)
    saveSelectedMonthYear()
    updateCalendarUI()
  }

  private fun updateCalendarUI() {
    val yearButton = findViewById<Button>(R.id.yearSelector)
    yearButton.text = currentYearMonth.year.toString()

    val monthButton = findViewById<Button>(R.id.monthSelector)
    monthButton.text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    val calendarGrid = findViewById<GridLayout>(R.id.calendarGridDays)
    calendarGrid.removeAllViews()

    val inflater = LayoutInflater.from(this)
    val startDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = currentYearMonth.lengthOfMonth()

    for (i in 0 until startDayOfWeek) {
      val emptyView = inflater.inflate(R.layout.empty_day, calendarGrid, false)
      calendarGrid.addView(emptyView)
    }

    for (day in 1..daysInMonth) {
      val block = TimesheetDayComponent(this)
      block.setDayNumber(day.toString())
      block.setHoursWorked("${(1..8).random()}")

      block.setOnClickListener {
        showDayTasksOverlay(day, currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
      }

      calendarGrid.addView(block)
    }
  }

  private fun showYearOverlay() {
    val overlayView = layoutInflater.inflate(R.layout.year_overlay, null)
    val container = overlayView.findViewById<LinearLayout>(R.id.yearButtonContainer)
    var selectedYear: Int? = null

    val dialog = AlertDialog.Builder(this)
      .setView(overlayView)
      .setCancelable(true)
      .create()

    //change this to fetch the years from the database. Look at the earliest date and latest, and assign them in here, tho this works for now, until the year 2033
    for (year in 2024..2033) {
      val button = layoutInflater.inflate(R.layout.year_button, container, false) as Button
      button.text = year.toString()

      button.setOnClickListener {
        selectedYear = year
        Toast.makeText(this, "You picked $year", Toast.LENGTH_SHORT).show()
        setCurrentYear(year)
        dialog.dismiss()
      }

      container.addView(button)
    }

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialog.show()

    //force custom width and position, cus for some reason doing it in the xml doesnt work??
    val window = dialog.window
    window?.setLayout(350, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setGravity(Gravity.TOP or Gravity.START)
    window?.attributes = window?.attributes?.apply {
      x = 40 //change position x axis
      y = 230 // change position y axis
    }
  }

  private fun showMonthOverlay() {
    val overlayView = layoutInflater.inflate(R.layout.month_overlay, null)
    val container = overlayView.findViewById<LinearLayout>(R.id.monthButtonContainer)
    val chosenText = overlayView.findViewById<TextView>(R.id.monthChosenText)

    val dialog = AlertDialog.Builder(this)
      .setView(overlayView)
      .setCancelable(true)
      .create()

    val months = listOf(
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    )

    for ((index, name) in months.withIndex()) {
      val button = layoutInflater.inflate(R.layout.month_button, container, false) as Button
      button.text = name

      button.setOnClickListener {
        Toast.makeText(this, "You picked $name", Toast.LENGTH_SHORT).show()
        setCurrentMonth(index + 1)
        dialog.dismiss()
      }

      container.addView(button)
    }

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    dialog.show()

    //force custom width and position, cus for some reason doing it in the xml doesnt work??
    val window = dialog.window
    window?.setLayout(350, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setGravity(Gravity.TOP or Gravity.START)
    window?.attributes = window?.attributes?.apply {
      x = 690
      y = 230
    }
  }

  //this is for the overlay of the day
  private fun showDayTasksOverlay(day: Int, monthName : String) {
    val dialogView = layoutInflater.inflate(R.layout.day_overlay, null)
    val dialog = android.app.AlertDialog.Builder(this)
      .setView(dialogView)
      .setCancelable(true)
      .create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    val container = dialogView.findViewById<LinearLayout>(R.id.taskContainer)
    val title = dialogView.findViewById<TextView>(R.id.overlayTitle)
    title.text = "Tasks on $monthName $day"

    // Dummy task data, replace this with tasks and hours fetched form db
    val tasks = listOf(
      "Making the design" to "1h",
      "Building the frontend" to "1h",
      "Building the backend" to "1h 30m",
      "Writing unit tests" to "30m"
    )

    for ((name, duration) in tasks) {
      val taskRow = TextView(this).apply {
        text = "$name - $duration"
        setPadding(8, 8, 8, 8)
        textSize = 16f
        setTextColor(android.graphics.Color.BLACK)
      }
      container.addView(taskRow)
    }

    dialog.show()
  }

  private fun saveSelectedMonthYear() {
    val prefs = getSharedPreferences("TimesheetPrefs", MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putInt("selectedYear", currentYearMonth.year)
    editor.putInt("selectedMonth", currentYearMonth.monthValue)
    editor.apply()
  }

  private fun loadSelectedMonthYear(): YearMonth {
    val prefs = getSharedPreferences("TimesheetPrefs", MODE_PRIVATE)
    val year = prefs.getInt("selectedYear", 2021) //default year is 2021
    val month = prefs.getInt("selectedMonth", 5)  //default month is May
    return YearMonth.of(year, month)
  }

}
