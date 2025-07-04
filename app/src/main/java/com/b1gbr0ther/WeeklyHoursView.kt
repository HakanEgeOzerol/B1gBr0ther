package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.b1gbr0ther.easteregg.DoodleJumpActivity
import com.b1gbr0ther.timetracking.WorkBlock
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeeklyHoursView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val gridPaint = Paint().apply {
        color = "#000000".toColorInt() // black
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val workPaint = Paint().apply {
        color = "#3CBC05".toColorInt() // green
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = "#000000".toColorInt() // black
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val dayTextPaint = Paint().apply {
        color = "#000000".toColorInt() // black
        textSize = 30f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var dailyHoursMap: Map<Int, Float> = emptyMap() // Day index (0-6) to hours worked
    private var maxHoursInWeek: Float = 8f // Default max hours for color scaling
    private var weekStartDate: LocalDate? = null
    
    // Easter egg variables (preserved from original view)
    private var clickCount = 0
    private var lastClickTime = 0L
    private val easterEggClickThreshold = 3
    private val clickTimeWindow = 2000L // 2 seconds

    fun setWorkData(workBlocks: List<WorkBlock>, weekStart: LocalDate? = null) {
        // Convert work blocks to daily hours
        val hoursMap = mutableMapOf<Int, Float>()
        
        // Initialize all days with 0 hours
        for (i in 0..6) {
            hoursMap[i] = 0f
        }
        
        // Sum up hours for each day
        for (block in workBlocks) {
            if (!block.isBreak) { // Only count work time, not breaks
                val dayIndex = block.dayIndex
                val hoursWorked = block.endHour - block.startHour
                // Ensure we're only adding positive hours
                if (hoursWorked > 0) {
                    hoursMap[dayIndex] = (hoursMap[dayIndex] ?: 0f) + hoursWorked
                }
            }
        }
        
        // Find the maximum hours worked in a day for this week
        val maxHours = hoursMap.values.maxOrNull() ?: 8f
        maxHoursInWeek = if (maxHours > 0) maxHours else 8f
        
        dailyHoursMap = hoursMap
        weekStartDate = weekStart
        invalidate() // Redraw with new data
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 40f
        val topPadding = 120f // Extra space for date label

        val dayCount = 7
        val usableWidth = width.toFloat() - padding * 2
        val usableHeight = height.toFloat() - topPadding - padding

        val cellWidth = usableWidth / dayCount
        val cellHeight = usableHeight * 0.6f // Make cells shorter than the full height
        
        val startX = padding
        val startY = topPadding + (usableHeight - cellHeight) / 2 // Center cells vertically
        
        // Update text colors based on theme
        textPaint.color = getTextColor()
        dayTextPaint.color = getTextColor()
        gridPaint.color = getTextColor()
        
        // Draw date range label at the top
        weekStartDate?.let { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            val dateText = formatDateRange(weekStart, weekEnd)
            
            canvas.drawText(
                dateText,
                width.toFloat() / 2,
                topPadding / 2,
                textPaint
            )
        }

        // Draw day squares with color intensity based on hours worked
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (i in 0 until dayCount) {
            val x = startX + i * cellWidth
            
            // Calculate color intensity based on hours worked
            val hoursWorked = (dailyHoursMap[i] ?: 0f).coerceAtLeast(0f) // Ensure hours are not negative
            val alpha = if (hoursWorked > 0) {
                // Scale alpha from 40 (minimum visibility) to 255 (full opacity)
                (40 + (215 * (hoursWorked / maxHoursInWeek))).toInt().coerceIn(40, 255)
            } else {
                0 // Transparent if no hours worked
            }
            
            // Create a color with the calculated alpha
            val workPaintWithAlpha = Paint(workPaint)
            workPaintWithAlpha.alpha = alpha
            
            // Draw the square
            val left = x + cellWidth * 0.1f
            val right = x + cellWidth * 0.9f
            val top = startY
            val bottom = startY + cellHeight
            
            // Draw square outline
            canvas.drawRect(left, top, right, bottom, gridPaint)
            
            // Fill square based on hours worked
            if (hoursWorked > 0) {
                canvas.drawRect(left, top, right, bottom, workPaintWithAlpha)
            }
            
            // Draw day name above the square
            canvas.drawText(
                dayNames[i],
                x + cellWidth / 2,
                top - 20f,
                dayTextPaint
            )
            
            // Draw hours below the square
            val displayHours = hoursWorked.coerceAtLeast(0f) // Ensure we display non-negative hours
            val hoursText = String.format("%.1f h", displayHours)
            canvas.drawText(
                hoursText,
                x + cellWidth / 2,
                bottom + 40f,
                dayTextPaint
            )
        }
    }
    
    private fun formatDateRange(weekStart: LocalDate, weekEnd: LocalDate): String {
        val startDay = weekStart.dayOfMonth
        val endDay = weekEnd.dayOfMonth
        
        return if (weekStart.month == weekEnd.month) {
            // Same month: "1 - 7 June"
            val monthName = weekStart.month.getDisplayName(
                TextStyle.FULL, 
                Locale.getDefault()
            )
            "$startDay - $endDay $monthName"
        } else {
            // Different months: "30 May - 5 June"
            val startMonth = weekStart.month.getDisplayName(
                TextStyle.SHORT, 
                Locale.getDefault()
            )
            val endMonth = weekEnd.month.getDisplayName(
                TextStyle.SHORT, 
                Locale.getDefault()
            )
            "$startDay $startMonth - $endDay $endMonth"
        }
    }
    
    private fun getTextColor(): Int {
        // Try to get the current text color from the theme
        val typedArray = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val textColor = typedArray.getColor(0, "#000000".toColorInt())
        typedArray.recycle()
        return textColor
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleEasterEggClick()
                performClick() // For accessibility
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun handleEasterEggClick() {
        val currentTime = System.currentTimeMillis()
        
        // Reset click count if too much time has passed
        if (currentTime - lastClickTime > clickTimeWindow) {
            clickCount = 0
        }
        
        clickCount++
        lastClickTime = currentTime
        
        // Trigger easter egg if threshold is reached
        if (clickCount >= easterEggClickThreshold) {
            triggerEasterEgg()
            clickCount = 0 // Reset counter
        }
    }

    private fun triggerEasterEgg() {
        // Launch the Doodle Jump game
        val intent = Intent(context, DoodleJumpActivity::class.java)
        context.startActivity(intent)
    }
}
