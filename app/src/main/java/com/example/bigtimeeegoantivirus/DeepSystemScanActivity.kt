package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class DeepSystemScanActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button
    private lateinit var tvScanResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_system_scan)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)
        tvScanResult = findViewById(R.id.tvScanResult)

        btnStartScan.setOnClickListener {
            if (checkPermissions()) {
                performDeepScan()
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                performDeepScan()
            } else {
                Log.e("Permissions", "Permissions not granted.")
            }
        }
    }

    private fun performDeepScan() {
        tvScanStatus.text = "Performing Deep Scan..."
        progressBar.visibility = View.VISIBLE
        btnStartScan.isEnabled = false


        Thread {
            val directory = Environment.getExternalStorageDirectory()
            val files = directory.listFiles()
            val totalFiles = files?.size ?: 0
            var filesScanned = 0

            files?.forEach { file ->
                val fileHash = getFileHash(file)
                if (fileHash != null) {
                    checkWithVirusTotal(fileHash)
                }
                filesScanned++

                runOnUiThread {
                    val progress = (filesScanned / totalFiles.toFloat() * 100).toInt()
                    progressBar.progress = progress
                }


                Thread.sleep(200)
            }


            runOnUiThread {
                progressBar.visibility = View.GONE
                tvScanStatus.text = "Scan Complete"
                tvScanResult.visibility = View.VISIBLE
                tvScanResult.text = "✅ No threats found in system files!"
                btnStartScan.isEnabled = true
            }
        }.start()
    }

    private fun getFileHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(file)
            val bytes = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(bytes).also { bytesRead = it } != -1) {
                md.update(bytes, 0, bytesRead)
            }
            val hashBytes = md.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: IOException) {
            null
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }

    private fun checkWithVirusTotal(fileHash: String) {
        val apiKey = "870df8edc61e52371c259645ea8370a766b29310d4b6e827f95deff7de35486d"
        val url = "https://www.virustotal.com/api/v3/files/$fileHash"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-apikey", apiKey)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("VirusTotal", "Error checking file: $e")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string())
                    val data = jsonResponse.getJSONObject("data")
                    val attributes = data.getJSONObject("attributes")
                    val lastAnalysisStats = attributes.getJSONObject("last_analysis_stats")
                    val maliciousCount = lastAnalysisStats.getInt("malicious")
                    if (maliciousCount > 0) {
                        runOnUiThread {
                            tvScanResult.text = "⚠️ Malicious file detected!"
                        }
                    }
                }
            }
        })
    }
}
