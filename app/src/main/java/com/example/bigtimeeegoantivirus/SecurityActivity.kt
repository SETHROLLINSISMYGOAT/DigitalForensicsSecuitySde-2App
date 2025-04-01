package com.example.bigtimeeegoantivirus


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SecurityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_security)

        findViewById<Button>(R.id.btnPhishingDetection).setOnClickListener {
            startActivity(Intent(this, PhishingDetectionActivity::class.java))
        }

        findViewById<Button>(R.id.btnWiFiSecurity).setOnClickListener {
            startActivity(Intent(this, WiFiSecurityActivity::class.java))
        }

        findViewById<Button>(R.id.btnMalwareScanner).setOnClickListener {
            startActivity(Intent(this, MalwareScannerActivity::class.java))
        }

        findViewById<Button>(R.id.btnRansomwareProtection).setOnClickListener {
            startActivity(Intent(this, RansomwareProtectionActivity::class.java))
        }

        findViewById<Button>(R.id.btnDataBreachScanner).setOnClickListener {
            startActivity(Intent(this, DataBreachScannerActivity::class.java))
        }

        findViewById<Button>(R.id.btnSystemVulnerability).setOnClickListener {
            startActivity(Intent(this, SystemVulnerabilityActivity::class.java))
        }

        findViewById<Button>(R.id.btnPermissionAuditor).setOnClickListener {
            startActivity(Intent(this, PermissionAuditorActivity::class.java))
        }

        findViewById<Button>(R.id.btnThreatMap).setOnClickListener {
            startActivity(Intent(this, ThreatMapActivity::class.java))
        }

        findViewById<Button>(R.id.btnCloudSecurityScan).setOnClickListener {
            startActivity(Intent(this, CloudSecurityScanActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeepSystemScan).setOnClickListener {
            startActivity(Intent(this, DeepSystemScanActivity::class.java))
        }

        findViewById<Button>(R.id.btnBigScan).setOnClickListener {
            startActivity(Intent(this, BigScanActivity::class.java))
        }
    }
}
