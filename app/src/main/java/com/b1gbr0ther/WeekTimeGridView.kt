package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.b1gbr0ther.easteregg.DoodleJumpActivity

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

    private var dataBlocks: List<WorkBlock> = emptyList()
    
    // Easter egg variables
    private var clickCount = 0
    private var lastClickTime = 0L
    private val easterEggClickThreshold = 5 // Number of clicks needed
    private val clickTimeWindow = 3000L // 3 seconds

    fun setWorkData(newData: List<WorkBlock>) {
        dataBlocks = newData
        invalidate() //redraws with new data
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 80f

        val hoursStart = 0f
        val hoursEnd = 24f
        val totalHours = hoursEnd - hoursStart
        val rowCount = 7

        val usableWidth = width.toFloat() - padding * 2
        val usableHeight = height.toFloat() - padding * 2

        val cellWidth = usableWidth / totalHours
        val cellHeight = usableHeight / rowCount

        val startX = padding
        val startY = padding

        val bottomY = startY + (rowCount - 1) * cellHeight

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleEasterEggClick()
                return true
            }
        }
        return super.onTouchEvent(event)
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
