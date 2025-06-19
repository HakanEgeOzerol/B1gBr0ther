package com.b1gbr0ther

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.MenuBar
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
    private lateinit var creationMethodChart: PieChart
    private lateinit var totalTasksText: TextView
    private lateinit var completedTasksText: TextView
    private lateinit var completionRateText: TextView
    private lateinit var completionStatsText: TextView
    
    private lateinit var databaseManager: DatabaseManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContentView(R.layout.activity_statistics)
        
        // Initialize views
        completionChart = findViewById(R.id.completionChart)
        timingChart = findViewById(R.id.timingChart)
        creationMethodChart = findViewById(R.id.creationMethodChart)
        totalTasksText = findViewById(R.id.totalTasksText)
        completedTasksText = findViewById(R.id.completedTasksText)
        completionRateText = findViewById(R.id.completionRateText)
        completionStatsText = findViewById(R.id.completionStatsText)
        
        // Set up the MenuBar with Statistics as the active page (index 3)
        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(3) // Index 3 for Statistics page (0-based index, 4th item)
        
        databaseManager = DatabaseManager(this)
        setupCharts()
        loadStatistics()
    }

    private fun setupCharts() {
        // Set up completion chart
        completionChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            centerText = getString(R.string.task_completion)
            setCenterTextSize(14f)
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            animateY(1400, Easing.EaseInOutQuad)
            setNoDataText(getString(R.string.no_task_data_available))
            setNoDataTextColor(Color.BLACK)
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
            setNoDataTextColor(Color.BLACK)
        }
        
        // Set up creation method chart
        creationMethodChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            centerText = getString(R.string.creation_methods)
            setCenterTextSize(14f)
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            animateY(1400, Easing.EaseInOutQuad)
            setNoDataText(getString(R.string.no_task_creation_data_available))
            setNoDataTextColor(Color.BLACK)
        }
    }

    private fun loadStatistics() {
        mainScope.launch {
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
                                Color.parseColor("#4CAF50"),  // Green
                                Color.parseColor("#F44336")    // Red
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
                                colors = listOf(
                                    Color.parseColor("#4CAF50"),  // Green
                                    Color.parseColor("#2196F3"),  // Blue
                                    Color.parseColor("#F44336")   // Red
                                )
                                valueTextSize = 12f
                                valueTextColor = Color.BLACK
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
            
            // Load creation method stats
            databaseManager.getVoiceCreatedTasksCount { voiceCount ->
                databaseManager.getGestureCreatedTasksCount { gestureCount ->
                    if (voiceCount + gestureCount > 0) {
                        val creationEntries = listOf(
                            PieEntry(voiceCount.toFloat(), getString(R.string.voice)),
                            PieEntry(gestureCount.toFloat(), getString(R.string.gesture))
                        )
                        
                        val creationDataSet = PieDataSet(creationEntries, "").apply {
                            colors = listOf(
                                Color.parseColor("#9C27B0"),  // Purple
                                Color.parseColor("#FF9800")    // Orange
                            )
                            valueTextSize = 12f
                            valueTextColor = Color.WHITE
                        }
                        
                        creationMethodChart.data = PieData(creationDataSet).apply {
                            setValueTextSize(12f)
                            setValueTextColor(Color.WHITE)
                        }
                        creationMethodChart.invalidate()
                    } else {
                        // Clear any previous data
                        creationMethodChart.clear()
                        creationMethodChart.invalidate()
                    }
                }
            }
        }
    }
}
