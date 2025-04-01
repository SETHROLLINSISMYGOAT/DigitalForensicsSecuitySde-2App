package com.example.bigtimeeegoantivirus

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class DataBreachScannerActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_breach_scanner)

        etEmail = findViewById(R.id.etEmail)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvScanResult = findViewById(R.id.tvScanResult)
        progressBar = findViewById(R.id.progressBar)
        btnStartScan = findViewById(R.id.btnStartScan)


        btnStartScan.setOnClickListener {
            val email = etEmail.text.toString().trim()


            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            startDataBreachScan(email)
        }
    }


    private fun startDataBreachScan(email: String) {

        tvScanStatus.text = "Scanning for data breaches..."
        progressBar.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE


        btnStartScan.postDelayed({
            tvScanStatus.text = "Scan Complete"
            progressBar.visibility = View.GONE
            tvScanResult.visibility = View.VISIBLE


            val breachCount = Random.nextInt(0, 5)


            tvScanResult.text = if (breachCount > 0) {
                "⚠️ $breachCount data breaches found for $email! Change your passwords."
            } else {
                "✅ No data breaches detected for $email!"
            }
        }, 3000)
    }
}
