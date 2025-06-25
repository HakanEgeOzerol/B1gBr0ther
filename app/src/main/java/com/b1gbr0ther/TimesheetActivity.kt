package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import com.b1gbr0ther.data.database.DatabaseManager
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
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class TimesheetActivity : AppCompatActivity() {

  private lateinit var databaseManager: com.b1gbr0ther.data.database.DatabaseManager
  private var dailyHoursMap: Map<java.time.LocalDate, Float> = emptyMap()

  private lateinit var currentYearMonth: YearMonth

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(LocaleHelper.onAttach(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Apply saved theme before setting content view
    ThemeManager.applyTheme(this)
    
    enableEdgeToEdge()
    setContentView(R.layout.activity_timesheet)

    databaseManager = com.b1gbr0ther.data.database.DatabaseManager(applicationContext)

    // first determine which month to load
    currentYearMonth = java.time.YearMonth.now()
    // load data for current month then build UI
    loadMonthDataAndRender()

    val menu = findViewById<MenuBar>(R.id.menuBar)
    menu.setActivePage(2)

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timesheetLayout)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    // updateCalendarUI() will be called after DB load

    val yearButton = findViewById<Button>(R.id.yearSelector)
    val monthButton = findViewById<Button>(R.id.monthSelector)

    yearButton.setOnClickListener {
      showYearOverlay()
    }

    monthButton.setOnClickListener {
      showMonthOverlay()
    }

    findViewById<Button>(R.id.b1gBr0therButton).setOnClickListener {
      val intent = Intent(this, SettingsActivity::class.java)
      startActivity(intent)
    }
  }

  private fun setCurrentMonth(month: Int) {
    currentYearMonth = java.time.YearMonth.of(currentYearMonth.year, month)
    loadMonthDataAndRender()
  }

  private fun setCurrentYear(year: Int) {
    currentYearMonth = java.time.YearMonth.of(year, currentYearMonth.monthValue)
    loadMonthDataAndRender()
  }

  private fun loadMonthDataAndRender() {
    databaseManager.getDailyWorkHoursForMonth(currentYearMonth) { map ->
      dailyHoursMap = map
      updateCalendarUI()
    }
  }

  private fun updateCalendarUI() {
    val yearButton = findViewById<Button>(R.id.yearSelector)
    yearButton.text = currentYearMonth.year.toString()

    val monthButton = findViewById<Button>(R.id.monthSelector)
    monthButton.text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    val calendarGrid = findViewById<GridLayout>(R.id.calendarGridDays)
    calendarGrid.removeAllViews()

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDay = currentYearMonth.atDay(1)
    val startDayOfWeek = firstDay.dayOfWeek.value % 7 // Convert to 0-based (Sunday = 0, Monday = 1, etc.)

    val maxHours = if (dailyHoursMap.isNotEmpty()) dailyHoursMap.values.maxOrNull() ?: 0f else 0f
    for (i in 0 until startDayOfWeek) {
      val emptyView = layoutInflater.inflate(R.layout.empty_day, calendarGrid, false)
      calendarGrid.addView(emptyView)
    }

    for (day in 1..daysInMonth) {
      val block = layoutInflater.inflate(R.layout.component_timesheet_day, calendarGrid, false)
      block.findViewById<TextView>(R.id.dayNumberText).text = day.toString()
      val date = currentYearMonth.atDay(day)
      val hrs = dailyHoursMap[date] ?: 0f
      block.findViewById<TextView>(R.id.hoursWorkedText).text = if (hrs > 0f) String.format("%.2fh", hrs) else "-"

      // set heat-map background intensity
      val ratio = if (maxHours > 0) (hrs / maxHours).coerceIn(0f, 1f) else 0f
      val alpha = (40 + ratio * 215).toInt() // 40 (very light) to 255 (opaque)
      val heatColor = android.graphics.Color.argb(alpha, 95, 75, 102) // purple
      block.setBackgroundColor(heatColor)

      block.setOnClickListener {
        val theDate = currentYearMonth.atDay(day)
        showDayTasksOverlay(theDate)
      }

      calendarGrid.addView(block)
    }
  }

  private fun showYearOverlay() {
    val overlayView = layoutInflater.inflate(R.layout.year_overlay, null)
    val container = overlayView.findViewById<LinearLayout>(R.id.yearButtonContainer)

    val dialog = AlertDialog.Builder(this)
      .setView(overlayView)
      .setCancelable(true)
      .create()

    //change this to fetch the years from the database. Look at the earliest date and latest, and assign them in here, tho this works for now, until the year 2033
    for (year in 2024..2033) {
      val button = layoutInflater.inflate(R.layout.year_button, container, false) as Button
      button.text = year.toString()

      button.setOnClickListener {
        Toast.makeText(this, getString(R.string.you_picked_year, year.toString()), Toast.LENGTH_SHORT).show()
        setCurrentYear(year)
        dialog.dismiss()
      }

      container.addView(button)
    }

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialog.show()

    //force custom width and position, cus for some reason doing it in the xml doesn't work??
    val window = dialog.window
    window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setGravity(Gravity.TOP or Gravity.START)
    window?.attributes = window?.attributes?.apply {
      x = 40 //change position x axis
      y = 230 // change position y axis
    }
  }

  private fun showMonthOverlay() {
    val overlayView = layoutInflater.inflate(R.layout.month_overlay, null)
    val container = overlayView.findViewById<LinearLayout>(R.id.monthButtonContainer)

    val dialog = AlertDialog.Builder(this)
      .setView(overlayView)
      .setCancelable(true)
      .create()

    val months = listOf(
      getString(R.string.january), getString(R.string.february), getString(R.string.march), getString(R.string.april), 
      getString(R.string.may), getString(R.string.june), getString(R.string.july), getString(R.string.august), 
      getString(R.string.september), getString(R.string.october), getString(R.string.november), getString(R.string.december)
    )

    for ((index, name) in months.withIndex()) {
      val button = layoutInflater.inflate(R.layout.month_button, container, false) as Button
      button.text = name

      button.setOnClickListener {
        Toast.makeText(this, getString(R.string.you_picked_month, name), Toast.LENGTH_SHORT).show()
        setCurrentMonth(index + 1)
        dialog.dismiss()
      }

      container.addView(button)
    }

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    dialog.show()

    //force custom width and position, cus for some reason doing it in the xml doesn't work??
    val window = dialog.window
    window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setGravity(Gravity.TOP or Gravity.START)
    window?.attributes = window?.attributes?.apply {
      x = 690
      y = 230
    }
  }

  //this is for the overlay of the day
  private fun showDayTasksOverlay(date: java.time.LocalDate) {
    val dialogView = layoutInflater.inflate(R.layout.day_overlay, null)
    val dialog = AlertDialog.Builder(this)
      .setView(dialogView)
      .setCancelable(true)
      .create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    val container = dialogView.findViewById<LinearLayout>(R.id.taskContainer)
    val title = dialogView.findViewById<TextView>(R.id.overlayTitle)
    title.text = "Tasks on ${date.month.name.lowercase().replaceFirstChar{it.uppercase()}} ${date.dayOfMonth}"

    // Fetch tasks from DB
    databaseManager.getTasksForDate(date) { tasks ->
      if (tasks.isEmpty()) {
        val empty = android.widget.TextView(this).apply {
          text = "No tasks"
          setPadding(8,8,8,8)
        }
        container.addView(empty)
      } else {
        for (task in tasks) {
          val durationMin = java.time.Duration.between(task.startTime, task.endTime).toMinutes()
          val hrs = durationMin / 60
          val mins = durationMin % 60
          val durationStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
          val taskRow = android.widget.TextView(this).apply {
            text = "${task.taskName} - $durationStr"
            setPadding(8,8,8,8)
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
          }
          container.addView(taskRow)
        }
      }
    }

    dialog.show()
  }

  private fun saveSelectedMonthYear() {
    val prefs = getSharedPreferences("TimesheetPrefs", MODE_PRIVATE)
    prefs.edit {
      putInt("selectedYear", currentYearMonth.year)
      putInt("selectedMonth", currentYearMonth.monthValue)
    }
  }

  private fun loadSelectedMonthYear(): YearMonth {
    val prefs = getSharedPreferences("TimesheetPrefs", MODE_PRIVATE)
    val year = prefs.getInt("selectedYear", 2021) //default year is 2021
    val month = prefs.getInt("selectedMonth", 5)  //default month is May
    return YearMonth.of(year, month)
  }
}
