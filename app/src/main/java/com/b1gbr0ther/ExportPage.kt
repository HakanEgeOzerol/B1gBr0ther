package com.b1gbr0ther

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ExportPage : AppCompatActivity() {
    private lateinit var menuBar: MenuBar
    private lateinit var exportButton: Button
    private lateinit var recordingsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_page)

        // Initialize views
        menuBar = findViewById(R.id.menuBar)
        exportButton = findViewById(R.id.exportButton)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)

        // Setup menu bar
        menuBar.setActivePage(0) // Set export page as active

        // Setup recycler view
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Add adapter for recordings list

        // Setup export button
        exportButton.setOnClickListener {
            handleExport()
        }
    }

    private fun handleExport() {
        // TODO: Implement export functionality
        // This could include:
        // 1. Getting selected recordings
        // 2. Converting to desired format
        // 3. Saving to device or sharing
        Toast.makeText(this, "Export functionality coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        menuBar.setActivePage(0) // Ensure correct menu item is highlighted
    }
}
