package com.example.bigtimeeegoantivirus

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class CloudSecurityScanActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button
    private lateinit var tvScanResult: TextView

    private val virusTotalApiKey = "870df8edc61e52371c259645ea8370a766b29310d4b6e827f95deff7de35486d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_security_scan)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)
        tvScanResult = findViewById(R.id.tvScanResult)

        checkStoragePermission()

        btnStartScan.setOnClickListener {
            performRealTimeScan()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }
    }

    private fun performRealTimeScan() {
        btnStartScan.isEnabled = false
        tvScanStatus.text = "Scanning entire storage for malware..."
        progressBar.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            val fileHashes = scanFullStorageForHashes()

            Log.d("CloudScan", "Total files to scan: ${fileHashes.size}")

            withContext(Dispatchers.Main) {
                if (fileHashes.isEmpty()) {
                    tvScanStatus.text = "✅ No files found to scan!"
                    progressBar.visibility = View.GONE
                    btnStartScan.isEnabled = true
                    return@withContext
                }

                progressBar.max = fileHashes.size
                progressBar.progress = 0
            }

            var malwareCount = 0

            fileHashes.forEachIndexed { index, fileHash ->
                Log.d("CloudScan", "Scanning hash #${index + 1}: $fileHash")

                val isMalicious = scanFileWithVirusTotal(fileHash)
                if (isMalicious) malwareCount++

                withContext(Dispatchers.Main) {
                    progressBar.progress = index + 1
                    tvScanStatus.text = "Scanning file ${index + 1} of ${fileHashes.size}"
                }
            }

            withContext(Dispatchers.Main) {
                tvScanStatus.text = if (malwareCount > 0) {
                    "❌ $malwareCount malware(s) found!"
                } else {
                    "✅ No malware detected!"
                }
                tvScanResult.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                btnStartScan.isEnabled = true
            }
        }
    }

    private fun scanFullStorageForHashes(): List<String> {
        val storageDir = Environment.getExternalStorageDirectory()
        val files = getAllFiles(storageDir)
        return files.mapNotNull { getFileHash(it) }
    }

    private fun getAllFiles(directory: File): List<File> {
        val filesList = mutableListOf<File>()
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                filesList.add(file)
            } else if (file.isDirectory) {
                filesList.addAll(getAllFiles(file))
            }
        }
        return filesList
    }

    private fun getFileHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("CloudScan", "Error hashing file: ${file.name}", e)
            null
        }
    }

    private suspend fun scanFileWithVirusTotal(fileHash: String): Boolean {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.virustotal.com/api/v3/files/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(VirusTotalApiService::class.java)

            val response = apiService.getFileReport(fileHash, "Bearer $virusTotalApiKey").execute()

            if (response.isSuccessful) {
                val result = response.body()
                val maliciousCount = result?.data?.attributes?.lastAnalysisStats?.malicious ?: 0
                if (maliciousCount > 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CloudSecurityScanActivity, "⚠ Malware found!", Toast.LENGTH_LONG).show()
                        showMalwareNotification(fileHash)
                    }
                    return true
                }
            } else {
                Log.e("CloudScan", "API error: ${response.errorBody()?.string()}")
            }
            false
        } catch (e: Exception) {
            Log.e("CloudScan", "API call failed", e)
            false
        }
    }

    private fun showMalwareNotification(fileHash: String) {
        val channelId = "malware_alert_channel"
        val channelName = "Malware Alerts"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifies when malware is found"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, CloudSecurityScanActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("❌ Malware Detected")
            .setContentText("Malicious file hash: $fileHash")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(fileHash.hashCode(), notification)
    }

    interface VirusTotalApiService {
        @GET("{fileHash}")
        fun getFileReport(
            @Path("fileHash") fileHash: String,
            @Header("x-apikey") apiKey: String
        ): Call<VirusTotalResponse>
    }

    data class VirusTotalResponse(
        val data: VirusTotalData
    )

    data class VirusTotalData(
        val attributes: VirusTotalAttributes
    )

    data class VirusTotalAttributes(
        val lastAnalysisStats: VirusTotalAnalysisStats
    )

    data class VirusTotalAnalysisStats(
        val malicious: Int
    )
}
