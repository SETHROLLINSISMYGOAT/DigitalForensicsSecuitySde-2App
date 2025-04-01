package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
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
            Log.d("CloudSecurityScan", "Scan button clicked!")
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            val startTime = System.currentTimeMillis()

            val fileHashes = scanFullStorageForHashes()

            withContext(Dispatchers.Main) {
                if (fileHashes.isEmpty()) {
                    tvScanStatus.text = "✅ No files found to scan!"
                    progressBar.visibility = View.GONE
                    btnStartScan.isEnabled = true
                } else {
                    tvScanStatus.text = "Scanning ${fileHashes.size} files..."
                    progressBar.max = fileHashes.size
                    progressBar.progress = 0

                    fileHashes.forEachIndexed { index, fileHash ->
                        scanFileWithVirusTotal(fileHash)
                        withContext(Dispatchers.Main) {
                            progressBar.progress = index + 1
                        }
                    }
                }
            }

            val endTime = System.currentTimeMillis()
            Log.d("CloudSecurityScan", "Total scan time: ${(endTime - startTime) / 1000}s")
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
            Log.e("CloudSecurityScan", "Error hashing file: ${file.name}", e)
            null
        }
    }

    private fun scanFileWithVirusTotal(fileHash: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://www.virustotal.com/api/v3/files/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(VirusTotalApiService::class.java)

                val response = apiService.getFileReport(fileHash, "Bearer $virusTotalApiKey").execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.data.attributes.lastAnalysisStats.malicious > 0) {
                            tvScanStatus.text = "❌ Malware detected!"
                            tvScanResult.text = "Malware found in hash: $fileHash"
                            Log.d("CloudSecurityScan", "Malware detected: $fileHash")
                        } else {
                            tvScanStatus.text = "✅ No malware in $fileHash"
                        }
                    } else {
                        tvScanStatus.text = "❌ Error scanning hash: $fileHash"
                        Log.e("CloudSecurityScan", "API Response Error: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvScanStatus.text = "❌ Failed to connect to VirusTotal"
                    Log.e("CloudSecurityScan", "API Call Failed", e)
                }
            }
        }
    }


    interface VirusTotalApiService {
        @GET("{fileHash}")
        fun getFileReport(@Path("fileHash") fileHash: String, @Header("x-apikey") apiKey: String): Call<VirusTotalResponse>
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
