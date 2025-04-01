package com.example.bigtimeeegoantivirus

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.io.File
import java.security.MessageDigest

class BigScanActivity : AppCompatActivity() {

    private lateinit var tvScanStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button
    private lateinit var tvScanResult: TextView
    private lateinit var database: DatabaseReference

    private val malwareHashes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_big_scan)

        tvScanStatus = findViewById(R.id.tvScanStatus)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)
        tvScanResult = findViewById(R.id.tvScanResult)

        database = FirebaseDatabase.getInstance().reference.child("malware_hashes")

        btnStartScan.setOnClickListener {
            if (isInternetAvailable()) {
                fetchMalwareHashesAndScan()
            } else {
                tvScanStatus.text = "❌ No internet connection!"
                progressBar.visibility = View.GONE
                btnStartScan.isEnabled = true
            }
        }
    }

    private fun fetchMalwareHashesAndScan() {
        tvScanStatus.text = "Fetching latest threat database..."
        progressBar.visibility = View.VISIBLE
        btnStartScan.isEnabled = false
        malwareHashes.clear()

        database.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                malwareHashes.add(child.key.toString())
            }
            if (malwareHashes.isNotEmpty()) {
                performBigScan()
            } else {
                tvScanStatus.text = "⚠️ No threat database found!"
                progressBar.visibility = View.GONE
                btnStartScan.isEnabled = true
            }
        }.addOnFailureListener {
            tvScanStatus.text = "❌ Error fetching malware database!"
            progressBar.visibility = View.GONE
            btnStartScan.isEnabled = true
        }
    }

    private fun performBigScan() {
        tvScanStatus.text = "Scanning storage..."
        tvScanResult.visibility = View.GONE

        val storageDir = Environment.getExternalStorageDirectory()
        val files = getAllFiles(storageDir)

        progressBar.max = files.size
        progressBar.progress = 0
        var detectedThreats = ""

        files.forEachIndexed { index, file ->
            val fileHash = getFileHash(file)
            if (malwareHashes.contains(fileHash)) {
                detectedThreats += "⚠️ Infected: ${file.name}\n"
            }
            progressBar.progress = index + 1
        }

        progressBar.visibility = View.GONE
        btnStartScan.isEnabled = true
        tvScanStatus.text = "Scan Complete"

        tvScanResult.text = if (detectedThreats.isEmpty()) "✅ No threats detected!" else detectedThreats
        tvScanResult.visibility = View.VISIBLE
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

    private fun getFileHash(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val inputStream = file.inputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
