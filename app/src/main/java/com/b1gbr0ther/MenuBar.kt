package com.b1gbr0ther

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.b1gbr0ther.StatisticsActivity

class MenuBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val highlight: View
    private val icons: List<View>
    
    // Language switch button (future implementation)
    // private val languageSwitchButton: FrameLayout?
    // private val languageText: TextView?

    companion object {
        // Navigation index constants for clarity
        const val INDEX_EXPORT = 0
        const val INDEX_DASHBOARD = 1
        const val INDEX_TIMESHEET = 2
        const val INDEX_STATISTICS = 3
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.menu_bar, this, true)

        highlight = findViewById(R.id.selectedPage)

        icons = listOf(
            findViewById(R.id.exportPageMenuButton),      // Index 0
            findViewById(R.id.dashboardPageMenuButton),   // Index 1
            findViewById(R.id.timesheetPageMenuButton),   // Index 2
            findViewById(R.id.statisticsPageMenuButton)   // Index 3
        )

        // Initialize language switch button (when implemented)
        // languageSwitchButton = findViewById(R.id.languageSwitchButton)
        // languageText = findViewById(R.id.languageText)

        setupClicks()
    }

    private fun setupClicks() {
        icons.forEachIndexed { index, view ->
            view.setOnClickListener {
                handleNavigation(index)
            }
        }
        
        // Language switch setup (future implementation)
        // languageSwitchButton?.setOnClickListener {
        //     toggleLanguage()
        // }
    }

    /**
     * Set the active page highlight
     * @param index Navigation index (0=Export, 1=Dashboard, 2=Timesheet, 3=Statistics)
     */
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
            INDEX_EXPORT -> ExportPage::class.java
            INDEX_DASHBOARD -> DashboardActivity::class.java
            INDEX_TIMESHEET -> TimesheetActivity::class.java
            INDEX_STATISTICS -> StatisticsActivity::class.java
            else -> return
        }

        if (currentActivity::class.java != target) {
            val intent = android.content.Intent(context, target)
            context.startActivity(intent)
            currentActivity.overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                0,
                0
            )
        }
    }
    
    // Future implementation for language switching
    // private fun toggleLanguage() {
    //     val currentLanguage = getCurrentLanguage()
    //     val newLanguage = if (currentLanguage == "en") "nl" else "en"
    //     setLanguage(newLanguage)
    //     updateLanguageDisplay(newLanguage)
    // }
    
    // private fun getCurrentLanguage(): String {
    //     // Get current language from SharedPreferences or system
    //     return "en" // Default
    // }
    
    // private fun setLanguage(language: String) {
    //     // Save language preference and apply to app
    //     // This would typically involve:
    //     // 1. Saving to SharedPreferences
    //     // 2. Updating app locale
    //     // 3. Recreating activities to apply changes
    // }
    
    // private fun updateLanguageDisplay(language: String) {
    //     languageText?.text = language.uppercase()
    // }
}
