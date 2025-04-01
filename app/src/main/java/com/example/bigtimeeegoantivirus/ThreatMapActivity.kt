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

class ThreatMapActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startScanButton: Button
    private lateinit var scanResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_threat_map)

        textView = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressBar)
        startScanButton = findViewById(R.id.startScanButton)
        scanResult = findViewById(R.id.scanResult)

        startScanButton.setOnClickListener {
            if (checkPermissions()) {
                threatMap()
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
                threatMap()
            } else {
                Log.e("Permissions", "Permissions not granted.")
            }
        }
    }

    private fun threatMap() {
        textView.text = "Performing Threat Map Scan..."
        progressBar.visibility = View.VISIBLE
        startScanButton.isEnabled = false

        Thread {
            val directory = Environment.getExternalStorageDirectory()
            val files = directory.listFiles()
            var totalFiles = 0
            var filesScanned = 0
            var maliciousFiles = 0

            if (files != null && files.isNotEmpty()) {
                totalFiles = files.size

                files.forEach { file ->
                    if (file.canRead()) {
                        val fileHash = getFileHash(file)
                        if (fileHash != null) {
                            checkWithVirusTotal(fileHash, file.name) { isMalicious ->
                                if (isMalicious) {
                                    maliciousFiles++
                                }
                                filesScanned++
                                updateScanProgress(filesScanned, totalFiles, maliciousFiles)
                            }
                        }
                    } else {
                        filesScanned++
                        updateScanProgress(filesScanned, totalFiles, maliciousFiles)
                    }
                    Thread.sleep(200)
                }
            } else {
                runOnUiThread {
                    textView.text = "No files found in the directory."
                    progressBar.visibility = View.GONE
                }
                return@Thread
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                textView.text = "Scan Complete"
                if (maliciousFiles > 0) {
                    scanResult.text = "$maliciousFiles Malicious files detected."
                } else {
                    scanResult.text = "No malicious files detected."
                }
                startScanButton.isEnabled = true
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

    private fun checkWithVirusTotal(fileHash: String, fileName: String, callback: (Boolean) -> Unit) {
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
                callback(false)
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
                            Log.i("VirusTotal", "File '$fileName' is malicious.")
                        }
                        callback(true)
                    } else {
                        callback(false)
                    }
                } else {
                    runOnUiThread {
                        Log.e("VirusTotal", "Error: ${response.code}")
                    }
                    callback(false)
                }
            }
        })
    }

    private fun updateScanProgress(filesScanned: Int, totalFiles: Int, maliciousFiles: Int) {
        runOnUiThread {
            val progress = (filesScanned / totalFiles.toFloat() * 100).toInt()
            progressBar.progress = progress

            textView.text = "Scanned $filesScanned of $totalFiles files"
            scanResult.text = "Malicious Files: $maliciousFiles"
        }
    }
}
