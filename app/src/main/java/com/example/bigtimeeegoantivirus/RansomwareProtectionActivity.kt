package com.example.bigtimeeegoantivirus


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RansomwareProtectionActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ransomware_protection)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)

        btnStartScan.setOnClickListener {
            startRansomwareScan()
        }
    }

    private fun startRansomwareScan() {
        tvScanStatus.text = "Scanning for ransomware threats..."
        progressBar.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE

        // Simulating a scan delay
        btnStartScan.postDelayed({
            tvScanStatus.text = "Scan Complete"
            progressBar.visibility = View.GONE
            tvScanResult.visibility = View.VISIBLE
            tvScanResult.text = "No ransomware detected ✅"
        }, 3000)
    }
}
