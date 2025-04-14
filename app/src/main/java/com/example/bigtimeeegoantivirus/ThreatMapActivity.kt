package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.CountDownLatch

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
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            threatMap()
        }
    }

    private fun threatMap() {
        textView.text = "Performing Threat Map Scan..."
        progressBar.visibility = View.VISIBLE
        startScanButton.isEnabled = false

        Thread {
            val rootDir = Environment.getExternalStorageDirectory()
            val allFiles = mutableListOf<File>()
            getAllFilesRecursive(rootDir, allFiles)

            val totalFiles = allFiles.size
            var filesScanned = 0
            var maliciousFiles = 0

            if (totalFiles == 0) {
                runOnUiThread {
                    textView.text = "No files found in storage."
                    progressBar.visibility = View.GONE
                }
                return@Thread
            }

            val latch = CountDownLatch(totalFiles)

            for (file in allFiles) {
                val fileHash = getFileHash(file)
                if (fileHash != null) {
                    checkWithVirusTotal(fileHash, file.name) { isMalicious ->
                        if (isMalicious) {
                            synchronized(this) { maliciousFiles++ }
                        }
                        synchronized(this) {
                            filesScanned++
                            updateScanProgress(filesScanned, totalFiles, maliciousFiles)
                        }
                        latch.countDown()
                    }
                } else {
                    synchronized(this) {
                        filesScanned++
                        updateScanProgress(filesScanned, totalFiles, maliciousFiles)
                    }
                    latch.countDown()
                }

                Thread.sleep(200)
            }

            latch.await()

            runOnUiThread {
                progressBar.visibility = View.GONE
                textView.text = "Scan Complete"
                scanResult.text = if (maliciousFiles > 0) {
                    "$maliciousFiles malicious files detected."
                } else {
                    "No malicious files detected."
                }
                startScanButton.isEnabled = true
            }
        }.start()
    }

    private fun getAllFilesRecursive(dir: File, fileList: MutableList<File>) {
        if (!dir.isDirectory || !dir.canRead()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                getAllFilesRecursive(file, fileList)
            } else {
                fileList.add(file)
            }
        }
    }

    private fun getFileHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            val hashBytes = md.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
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
                Log.e("VirusTotal", "Network error: $e")
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(false)
                        return
                    }

                    val json = JSONObject(it.body?.string() ?: "")
                    val stats = json
                        .optJSONObject("data")
                        ?.optJSONObject("attributes")
                        ?.optJSONObject("last_analysis_stats")

                    val malicious = stats?.optInt("malicious", 0) ?: 0
                    callback(malicious > 0)
                }
            }
        })
    }

    private fun updateScanProgress(filesScanned: Int, totalFiles: Int, maliciousFiles: Int) {
        runOnUiThread {
            val progress = (filesScanned.toFloat() / totalFiles * 100).toInt()
            progressBar.progress = progress
            textView.text = "Scanned $filesScanned of $totalFiles files"
            scanResult.text = "Malicious Files: $maliciousFiles"
        }
    }
}
