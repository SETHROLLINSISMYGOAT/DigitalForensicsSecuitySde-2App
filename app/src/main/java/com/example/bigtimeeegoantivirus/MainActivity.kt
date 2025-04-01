package com.example.bigtimeeegoantivirus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnImages).setOnClickListener { openFileList("images") }
        findViewById<Button>(R.id.btnVideos).setOnClickListener { openFileList("videos") }
        findViewById<Button>(R.id.btnAudio).setOnClickListener { openFileList("audio") }
        findViewById<Button>(R.id.btnDocuments).setOnClickListener { openFileList("documents") }
        findViewById<Button>(R.id.btnLargeFiles).setOnClickListener { openFileList("large") }
        findViewById<Button>(R.id.btnHiddenFiles).setOnClickListener { openFileList("hidden") }
        findViewById<Button>(R.id.btnRecentFiles).setOnClickListener { openFileList("recent") }
        findViewById<Button>(R.id.btnApps).setOnClickListener { openFileList("apps") }
        findViewById<Button>(R.id.btnAdvancedSecurity).setOnClickListener { openSecurityScreen() }
    }

    private fun openFileList(category: String) {
        val intent = Intent(this, FileListActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }

    private fun openSecurityScreen() {
        val intent = Intent(this, SecurityActivity::class.java)
        startActivity(intent)
    }
}