package com.b1gbr0ther

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout

class MenuBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val highlight: View
    private val icons: List<View>

    init {
        LayoutInflater.from(context).inflate(R.layout.menu_bar, this, true)

        highlight = findViewById(R.id.selectedPage)

        icons = listOf(
            findViewById(R.id.exportPageMenuButton),
            findViewById(R.id.dashboardPageMenuButton),
            findViewById(R.id.timesheetPageMenuButton),
        )

        setupClicks()
    }

    private fun setupClicks() {
        icons.forEachIndexed { index, view ->
            view.setOnClickListener {
                handleNavigation(index)
            }
        }
    }

    fun setActivePage(index: Int) {
        moveHighlightTo(index)
    }

    private fun moveHighlightTo(index: Int) {
        val view = icons.getOrNull(index) ?: return
        val params = highlight.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = view.id
        params.topToTop = view.id
        params.endToEnd = view.id
        highlight.layoutParams = params
    }

    private fun handleNavigation(index: Int) {
        val currentActivity = context as? android.app.Activity ?: return

        val target = when (index) {
            0 -> ExportPage::class.java
            1 -> DashboardActivity::class.java
            2 -> {
                Toast.makeText(context, "Timesheet page coming soon", Toast.LENGTH_SHORT).show()
                return
            }
            else -> return
        }

        if (currentActivity::class.java != target) {
            val intent = android.content.Intent(context, target)
            context.startActivity(intent)
            currentActivity.overridePendingTransition(0, 0)
        }
    }
}
