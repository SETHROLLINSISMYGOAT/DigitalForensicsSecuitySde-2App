package com.example.bigtimeeegoantivirus

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PermissionAuditorActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_auditor)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)

        btnStartScan.setOnClickListener {
            startPermissionAudit()
        }
    }

    private fun startPermissionAudit() {
        tvScanStatus.text = "Scanning app permissions..."
        progressBar.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE

        btnStartScan.postDelayed({
            val permissionsMap = scanPermissions()
            tvScanStatus.text = "Scan Complete"
            progressBar.visibility = View.GONE
            tvScanResult.visibility = View.VISIBLE

            val resultText = buildString {
                append("⚠️ Apps with high-risk permissions:\n")
                permissionsMap.forEach { (app, perms) ->
                    append("$app: ${perms.joinToString(", ")}\n")
                }
            }

            tvScanResult.text = if (permissionsMap.isNotEmpty()) resultText else "✅ No security risks found!"
        }, 3000)
    }

    private fun scanPermissions(): Map<String, List<String>> {
        val highRiskPermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.READ_SMS",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION"
        )

        val result = mutableMapOf<String, List<String>>()
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            val packageName = app.packageName
            try {
                val permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
                if (permissions != null) {
                    val riskyPermissions = permissions.filter { it in highRiskPermissions }
                    if (riskyPermissions.isNotEmpty()) {
                        result[app.loadLabel(pm).toString()] = riskyPermissions
                    }
                }
            } catch (e: Exception) {
                // Ignore system apps that may cause errors
            }
        }
        return result
    }
}
