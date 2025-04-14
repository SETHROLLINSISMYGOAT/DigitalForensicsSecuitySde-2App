package com.example.bigtimeeegoantivirus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore

class PermissionAuditorActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button
    private lateinit var listView: ListView

    private val firestore = FirebaseFirestore.getInstance()

    private val highRiskPermissions = listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_SMS",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    private val safeApps = listOf(
        "com.google.android.gms", "com.whatsapp", "com.facebook.katana"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_auditor)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)
        listView = findViewById(R.id.listView)

        btnStartScan.setOnClickListener {
            startPermissionAudit()
        }
    }

    private fun startPermissionAudit() {
        tvScanStatus.text = "🔍 Scanning app permissions..."
        progressBar.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE

        btnStartScan.postDelayed({
            val permissionsMap = scanPermissions()
            tvScanStatus.text = "✅ Scan Complete"
            progressBar.visibility = View.GONE
            tvScanResult.visibility = View.VISIBLE

            if (permissionsMap.isNotEmpty()) {
                tvScanResult.text = "⚠️ High-Risk Apps Detected!"
                showResults(permissionsMap)
                saveToFirebase(permissionsMap)
                sendSecurityAlert(permissionsMap.size)
            } else {
                tvScanResult.text = "✅ No security risks found!"
                listView.adapter = null
            }
        }, 3000)
    }

    private fun scanPermissions(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            val packageName = app.packageName
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageName !in safeApps) {
                try {
                    val permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
                    if (permissions != null) {
                        val riskyPermissions = permissions.filter { it in highRiskPermissions }
                        if (riskyPermissions.isNotEmpty()) {
                            result[app.loadLabel(pm).toString()] = riskyPermissions
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PermissionAudit", "Error checking $packageName", e)
                }
            }
        }
        return result
    }

    private fun showResults(permissionsMap: Map<String, List<String>>) {
        val listItems = permissionsMap.map { (appName, perms) ->
            "🔒 $appName\n    ➤ ${perms.joinToString("\n    ➤ ")}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
        listView.adapter = adapter
    }

    private fun saveToFirebase(permissionsMap: Map<String, List<String>>) {
        val timestamp = System.currentTimeMillis()
        val data = hashMapOf(
            "timestamp" to timestamp,
            "riskApps" to permissionsMap
        )

        firestore.collection("audit_logs").add(data)
            .addOnSuccessListener {
                Log.d("Firebase", "✅ Scan results saved successfully!")
            }
            .addOnFailureListener {
                Log.e("Firebase", "❌ Failed to save scan results!", it)
            }
    }

    private fun sendSecurityAlert(threatCount: Int) {
        val channelId = "SECURITY_ALERTS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Security Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ Security Alert!")
            .setContentText("$threatCount high-risk apps detected.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }
}
