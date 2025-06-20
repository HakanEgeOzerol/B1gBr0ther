package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.b1gbr0ther.easteregg.DoodleJumpActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeekTimeGridView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val gridPaint = Paint().apply {
        color = "#000000".toColorInt() //black
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val workPaint = Paint().apply {
        color = "#3CBC05".toColorInt() //green
        style = Paint.Style.FILL
    }

    private val breakPaint = Paint().apply {
        color = "#D30D10".toColorInt() //red
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = "#000000".toColorInt() //black
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var dataBlocks: List<WorkBlock> = emptyList()
    private var weekStartDate: LocalDate? = null
    
    // Easter egg variables
    private var clickCount = 0
    private var lastClickTime = 0L
    private val easterEggClickThreshold = 3 // Reduced to 3 clicks for easier access
    private val clickTimeWindow = 2000L // 2 seconds

    fun setWorkData(newData: List<WorkBlock>, weekStart: LocalDate? = null) {
        dataBlocks = newData
        weekStartDate = weekStart
        invalidate() //redraws with new data
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 80f
        val topPadding = 120f // Extra space for date label

        val hoursStart = 0f
        val hoursEnd = 24f
        val totalHours = hoursEnd - hoursStart
        val rowCount = 7

        val usableWidth = width.toFloat() - padding * 2
        val usableHeight = height.toFloat() - topPadding - padding

        val cellWidth = usableWidth / totalHours
        val cellHeight = usableHeight / rowCount

        val startX = padding
        val startY = topPadding

        val bottomY = startY + (rowCount - 1) * cellHeight
        
        // Draw date range label at the top
        weekStartDate?.let { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            val dateText = formatDateRange(weekStart, weekEnd)
            
            // Update text color based on theme
            textPaint.color = getTextColor()
            
            canvas.drawText(
                dateText,
                width.toFloat() / 2,
                topPadding / 2,
                textPaint
            )
        }

        //draw horizontal lines
        for (i in 0 until rowCount) {
            val y = startY + i * cellHeight
            canvas.drawLine(startX - 50f, y, startX + usableWidth + 50f, y, gridPaint)
        }

        //draw vertical lines
        for (i in 0..totalHours.toInt() step 2) {
            val x = startX + i * cellWidth
            canvas.drawLine(x, startY - 50f, x, bottomY + 50f, gridPaint)
        }

        //draw green and red bars
        for (block in dataBlocks) {
            val y = startY + block.dayIndex * cellHeight
            val barHeight = 10f
            val top = y - barHeight / 2
            val bottom = y + barHeight / 2

            val left = startX + (block.startHour - hoursStart) * cellWidth
            val right = startX + (block.endHour - hoursStart) * cellWidth

            val paint = if (block.isBreak) breakPaint else workPaint
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
    
    private fun formatDateRange(weekStart: LocalDate, weekEnd: LocalDate): String {
        val startDay = weekStart.dayOfMonth
        val endDay = weekEnd.dayOfMonth
        
        return if (weekStart.month == weekEnd.month) {
            // Same month: "1 - 7 June"
            val monthName = weekStart.month.getDisplayName(
                java.time.format.TextStyle.FULL, 
                Locale.getDefault()
            )
            "$startDay - $endDay $monthName"
        } else {
            // Different months: "30 May - 5 June"
            val startMonth = weekStart.month.getDisplayName(
                java.time.format.TextStyle.SHORT, 
                Locale.getDefault()
            )
            val endMonth = weekEnd.month.getDisplayName(
                java.time.format.TextStyle.SHORT, 
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
