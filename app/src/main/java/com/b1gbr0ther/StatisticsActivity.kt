package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.MenuBar
import com.b1gbr0ther.TaskCategory
import com.b1gbr0ther.easteregg.SnakeGameActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var completionChart: PieChart
    private lateinit var timingChart: BarChart
    private lateinit var categoryChart: PieChart
    private lateinit var totalTasksText: TextView
    private lateinit var completedTasksText: TextView
    private lateinit var completionRateText: TextView
    private lateinit var completionStatsText: TextView
    private lateinit var categoriesStatsText: TextView
    
    private lateinit var databaseManager: DatabaseManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    // Track the theme that was applied when this activity was created
    private var appliedTheme: Int = -1

    private fun getCurrentTextColor(): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return typedValue.data
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContentView(R.layout.activity_statistics)
        
        // Initialize views
        completionChart = findViewById(R.id.completionChart)
        timingChart = findViewById(R.id.timingChart)
        categoryChart = findViewById(R.id.categoryChart)
        totalTasksText = findViewById(R.id.totalTasksText)
        completedTasksText = findViewById(R.id.completedTasksText)
        completionRateText = findViewById(R.id.completionRateText)
        completionStatsText = findViewById(R.id.completionStatsText)
        categoriesStatsText = findViewById(R.id.categoriesStatsText)
        
        // Set up the MenuBar with Statistics as the active page (index 3)
        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(3) // Index 3 for Statistics page (0-based index, 4th item)
        
        databaseManager = DatabaseManager(this)
        setupCharts()
        loadStatistics()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if theme has changed since this activity was created
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && currentTheme != appliedTheme) {
            // Theme has changed, recreate the activity to apply new theme
            recreate()
        }
    }

    private fun setupCharts() {
        // Set up completion chart
        completionChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(getCurrentTextColor())
            centerText = getString(R.string.task_completion)
            setCenterTextSize(14f)
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            animateY(1400, Easing.EaseInOutQuad)
            setNoDataText(getString(R.string.no_task_data_available))
            setNoDataTextColor(getCurrentTextColor())
            
            // Add double-tap detection for Easter egg
            setupChartGestureDetector(this)
        }
        
        // Set up timing chart
        timingChart.apply {
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(arrayOf(getString(R.string.early), getString(R.string.on_time), getString(R.string.late)))
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(false)
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            animateY(1400)
            setNoDataText(getString(R.string.no_timing_data_available))
            setNoDataTextColor(getCurrentTextColor())
            
            // Disable double-tap zoom functionality
            setScaleEnabled(false)
            setPinchZoom(false)
            setDoubleTapToZoomEnabled(false)
            
            // Add double-tap detection for Easter egg
            setupChartGestureDetector(this)
        }
        
        // Creation method chart removed
        
        // Set up category chart
        categoryChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(getCurrentTextColor())
            centerText = "Task Categories"
            setCenterTextSize(14f)
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            animateY(1400, Easing.EaseInOutQuad)
            setNoDataText("No category data available")
            setNoDataTextColor(getCurrentTextColor())
            
            // Add double-tap detection for Easter egg
            setupChartGestureDetector(this)
        }
    }

    private fun loadStatistics() {
        mainScope.launch {
            // Load category statistics
            loadCategoryStatistics()
            
            // Load completion stats
            databaseManager.getCompletedTasksCount { completedCount ->
                databaseManager.getUncompletedTasksCount { uncompletedCount ->
                    val totalTasks = completedCount + uncompletedCount
                    
                    // Update quick stats text
                    totalTasksText.text = numberFormat.format(totalTasks)
                    completedTasksText.text = numberFormat.format(completedCount)
                    val completionRate = if (totalTasks > 0) {
                        (completedCount * 100f / totalTasks).toInt()
                    } else {
                        0
                    }
                    completionRateText.text = "$completionRate%"
                    
                    // Update completion stats text
                    completionStatsText.text = getString(R.string.completion_stats_format, numberFormat.format(completedCount), numberFormat.format(uncompletedCount))
                    
                    // Update completion chart if there's data
                    if (totalTasks > 0) {
                        val completionEntries = listOf(
                            PieEntry(completedCount.toFloat(), ""),
                            PieEntry(uncompletedCount.toFloat(), "")
                        )
                        
                        val completionDataSet = PieDataSet(completionEntries, "").apply {
                            colors = listOf(
                                Color.parseColor("#BB86FC"),  // Purple 200
                                Color.parseColor("#E1BEE7")   // Purple 100
                            )
                            valueTextSize = 12f
                            valueTextColor = Color.WHITE
                        }
                        
                        completionChart.data = PieData(completionDataSet).apply {
                            setDrawValues(false)  // Don't draw values on the chart
                        }
                        completionChart.invalidate()
                    } else {
                        // Clear any previous data
                        completionChart.clear()
                        completionChart.invalidate()
                    }
                }
            }
            
            // Load timing stats
            databaseManager.getEarlyCompletedTasksCount { earlyCount ->
                databaseManager.getOnTimeCompletedTasksCount { onTimeCount ->
                    databaseManager.getLateCompletedTasksCount { lateCount ->
                        val totalTimingTasks = earlyCount + onTimeCount + lateCount
                        
                        if (totalTimingTasks > 0) {
                            val barEntries = listOf(
                                BarEntry(0f, earlyCount.toFloat()),
                                BarEntry(1f, onTimeCount.toFloat()),
                                BarEntry(2f, lateCount.toFloat())
                            )
                            
                            val barDataSet = BarDataSet(barEntries, "").apply {
                                // Use the same purple color for all bars in the timing chart
                                colors = listOf(
                                    Color.parseColor("#6200EE"),  // Purple 500
                                    Color.parseColor("#6200EE"),  // Purple 500
                                    Color.parseColor("#6200EE")   // Purple 500
                                )
                                valueTextSize = 12f
                                valueTextColor = getCurrentTextColor()
                            }
                            
                            timingChart.data = BarData(barDataSet).apply {
                                barWidth = 0.4f
                                setValueTextSize(12f)
                            }
                            timingChart.invalidate()
                        } else {
                            // Clear any previous data
                            timingChart.clear()
                            timingChart.invalidate()
                        }
                    }
                }
            }
            
            // Creation method stats loading removed
        }
    }
    
    /**
     * Load task category statistics and populate the chart
     */
    private fun loadCategoryStatistics() {
        // Define all categories to show in chart
        val categories = TaskCategory.values()
        
        // List to store chart entries and category counts
        val categoryEntries = mutableListOf<PieEntry>()
        val categoryColors = mutableListOf<Int>()
        var totalTasks = 0
        
        // Counter for tracking how many categories have been processed
        var processedCategories = 0
        
        // Map to store category counts for summary text
        val categoryCounts = mutableMapOf<String, Int>()
        
        // For each category, get task count
        categories.forEach { category ->
            databaseManager.getTaskCountByCategory(category) { count ->
                if (count > 0) {
                    // Add entry for chart
                    categoryEntries.add(PieEntry(count.toFloat(), category.displayName))
                    
                    // Add color based on category using purple shades
                    val color = when (category) {
                        TaskCategory.PROFESSIONAL -> Color.parseColor("#6200EE")  // Purple 500
                        TaskCategory.PERSONAL -> Color.parseColor("#BB86FC")    // Purple 200
                        TaskCategory.FAMILY -> Color.parseColor("#3700B3")      // Purple 700
                        TaskCategory.LEISURE -> Color.parseColor("#E1BEE7")     // Purple 100
                        TaskCategory.OTHER -> Color.parseColor("#8E4EC6")       // Game exit button purple
                        else -> Color.parseColor("#5F4B66")                     // Dashboard button background
                    }
                    categoryColors.add(color)
                    
                    // Store count for summary
                    categoryCounts[category.displayName] = count
                    totalTasks += count
                }
                
                // Increment processed count
                processedCategories++
                
                // If all categories processed, update chart
                if (processedCategories == categories.size) {
                    updateCategoryChart(categoryEntries, categoryColors, categoryCounts, totalTasks)
                }
            }
        }
    }
    
    /**
     * Updates the category chart with entries and sets the summary text
     */
    private fun updateCategoryChart(
        categoryEntries: List<PieEntry>, 
        colors: List<Int>, 
        categoryCounts: Map<String, Int>,
        totalTasks: Int
    ) {
        if (categoryEntries.isNotEmpty()) {
            val categoryDataSet = PieDataSet(categoryEntries, "").apply {
                this.colors = colors
                valueTextSize = 12f
                valueTextColor = Color.WHITE
            }
            
            categoryChart.data = PieData(categoryDataSet).apply {
                setDrawValues(false)  // Don't draw values on the chart
            }
            categoryChart.invalidate()
            
            // Generate stats summary text
            val statsBuilder = StringBuilder()
            categoryCounts.entries.sortedByDescending { it.value }.forEach { (category, count) ->
                val percentage = (count * 100f / totalTasks).toInt()
                statsBuilder.append("$category: $count ($percentage%)\n")
            }
            
            categoriesStatsText.text = statsBuilder.toString().trimEnd()
        } else {
            // Clear any previous data
            categoryChart.clear()
            categoryChart.invalidate()
            categoriesStatsText.text = "No category data available"
        }
    }
    
    /**
     * Sets up a gesture detector for double-tap on charts to launch the Snake game Easter egg
     */
    private fun setupChartGestureDetector(view: View) {
        val gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Launch Snake game Easter egg
                launchSnakeGame()
                return true
            }
        })
        
        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            // Return false to ensure the chart's touch events still work
            false
        }
    }
    
    /**
     * Launch the Snake game Easter egg
     */
    private fun launchSnakeGame() {
        // Show a brief toast message
        Toast.makeText(this, "Easter egg found! 🐍", Toast.LENGTH_SHORT).show()
        
        // Launch the Snake game activity
        val intent = Intent(this, SnakeGameActivity::class.java)
        startActivity(intent)
    }
}
