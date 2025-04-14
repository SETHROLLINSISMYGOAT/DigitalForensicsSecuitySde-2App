package com.example.bigtimeeegoantivirus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class RansomwareProtectionActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var btnStartScan: Button
    private lateinit var logView: TextView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private val firestore = FirebaseFirestore.getInstance()
    private val ransomwareHashes = mutableSetOf<String>()

    private val STORAGE_PERMISSION_CODE = 1001
    private var totalFilesToScan = 0
    private var scannedFiles = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ransomware_protection)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        btnStartScan = findViewById(R.id.btnStartScan)
        logView = findViewById(R.id.logView)

        logView.movementMethod = android.text.method.ScrollingMovementMethod.getInstance()
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        btnStartScan.setOnClickListener {
            if (checkPermission()) {
                fetchThreatSignatures()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchThreatSignatures()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchThreatSignatures() {
        tvScanStatus.text = "Fetching ransomware signatures..."
        progressBar.visibility = View.VISIBLE
        tvProgressPercent.text = ""
        logView.text = ""

        firestore.collection("ransomware_signatures").get()
            .addOnSuccessListener { result ->
                ransomwareHashes.clear()
                for (document in result) {
                    val hash = document.getString("hash")
                    if (!hash.isNullOrEmpty()) {
                        ransomwareHashes.add(hash)
                    }
                }
                if (ransomwareHashes.isNotEmpty()) {
                    tvScanStatus.text = "✅ Signatures loaded. Starting scan..."
                    startFullScan()
                } else {
                    tvScanStatus.text = "⚠️ No ransomware signatures found!"
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                tvScanStatus.text = "❌ Failed to fetch ransomware signatures!"
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startFullScan() {
        val rootDir = Environment.getExternalStorageDirectory()
        scannedFiles = 0
        totalFilesToScan = countFiles(rootDir)

        Thread {
            val threats = scanFiles(rootDir)

            runOnUiThread {
                progressBar.visibility = View.GONE
                tvProgressPercent.text = ""
                tvScanStatus.text = "✅ Full Scan Complete"
                tvScanResult.visibility = View.VISIBLE

                if (threats.isNotEmpty()) {
                    tvScanResult.text = "⚠️ ${threats.size} Threat(s) Quarantined!"
                    triggerSecurityAlert()
                } else {
                    tvScanResult.text = "✅ No threats found!"
                }
            }
        }.start()
    }

    private fun countFiles(directory: File?): Int {
        if (directory == null || !directory.exists() || !directory.canRead()) return 0
        var count = 0
        directory.listFiles()?.forEach {
            if (it.isFile) count++
            else if (it.isDirectory) count += countFiles(it)
        }
        return count
    }

    private fun scanFiles(directory: File): List<String> {
        val threats = mutableListOf<String>()
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                try {
                    if (file.isFile) {
                        val fileHash = hashFile(file)
                        scannedFiles++

                        runOnUiThread {
                            val percent = (scannedFiles * 100 / totalFilesToScan).coerceIn(0, 100)
                            progressBar.progress = percent
                            tvProgressPercent.text = "$percent%"
                            if (scannedFiles % 100 == 0) {
                                logView.append("\nScanned $scannedFiles files...")
                            }
                        }

                        if (ransomwareHashes.contains(fileHash)) {
                            threats.add(file.absolutePath)
                            runOnUiThread { logView.append("\n⚠️ Threat: ${file.name}") }
                            quarantineAndLog(file, fileHash)
                        }
                    } else if (file.isDirectory) {
                        threats.addAll(scanFiles(file))
                    }
                } catch (e: Exception) {
                    Log.e("Scan", "Error reading file: ${file.absolutePath}", e)
                }
            }
        }
        return threats
    }

    private fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun quarantineAndLog(file: File, hash: String) {
        try {
            val quarantineDir = File(getExternalFilesDir(null), "Quarantine")
            if (!quarantineDir.exists()) quarantineDir.mkdirs()

            val quarantinedFile = File(quarantineDir, file.name)
            file.copyTo(quarantinedFile, overwrite = true)
            file.delete()

            val data = hashMapOf(
                "filename" to file.name,
                "path" to file.absolutePath,
                "hash" to hash,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            firestore.collection("quarantined_threats").add(data)
                .addOnSuccessListener { Log.d("Firebase", "Threat logged") }
                .addOnFailureListener { Log.e("Firebase", "Logging failed", it) }

        } catch (e: Exception) {
            Log.e("Quarantine", "Failed to quarantine: ${file.name}", e)
        }
    }

    private fun triggerSecurityAlert() {
        mediaPlayer.start()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SECURITY_ALERTS", "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "SECURITY_ALERTS")
            .setContentTitle("⚠️ Ransomware Detected!")
            .setContentText("Threats quarantined. Check log.")
            .setSmallIcon(R.drawable.ic_warning)
            .build()

        manager.notify(1, notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(800)
        }
    }
}
