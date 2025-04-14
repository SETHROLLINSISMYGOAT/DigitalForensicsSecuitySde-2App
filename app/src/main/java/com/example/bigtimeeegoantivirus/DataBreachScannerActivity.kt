package com.example.bigtimeeegoantivirus

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

class DataBreachScannerActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStartScan: Button

    private val virusTotalApiKey = "870df8edc61e52371c259645ea8370a766b29310d4b6e827f95deff7de35486d"

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

            if (!email.contains("@")) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val domain = email.substringAfter("@")
            startScan(domain)
        }
    }

    private fun startScan(domain: String) {
        tvScanStatus.text = "Scanning domain: $domain"
        tvScanResult.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://www.virustotal.com/api/v3/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(VirusTotalApi::class.java)
                val response = service.getDomainReport(virusTotalApiKey, domain)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvScanResult.visibility = View.VISIBLE

                    if (response.isSuccessful && response.body() != null) {
                        val stats = response.body()!!.data.attributes.last_analysis_stats
                        if (stats.malicious > 0 || stats.suspicious > 0) {
                            tvScanResult.text = "⚠️ Domain linked to malicious activity.\nMalicious: ${stats.malicious}, Suspicious: ${stats.suspicious}"
                            Toast.makeText(applicationContext, "⚠️ Warning: This domain is flagged!", Toast.LENGTH_LONG).show()
                        } else {
                            tvScanResult.text = "✅ No known threats for $domain"
                        }
                    } else {
                        val error = response.errorBody()?.string()
                        tvScanResult.text = "❌ Error retrieving data from VirusTotal"
                        Log.e("VirusTotal", "API error: $error")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvScanResult.visibility = View.VISIBLE
                    tvScanResult.text = "❌ Scan failed: ${e.message}"
                }
            }
        }
    }

    interface VirusTotalApi {
        @GET("domains/{domain}")
        suspend fun getDomainReport(
            @Header("x-apikey") apiKey: String,
            @Path("domain") domain: String
        ): Response<DomainResponse>
    }

    data class DomainResponse(
        val data: DomainData
    )

    data class DomainData(
        val attributes: DomainAttributes
    )

    data class DomainAttributes(
        val last_analysis_stats: AnalysisStats
    )

    data class AnalysisStats(
        val harmless: Int,
        val malicious: Int,
        val suspicious: Int,
        val undetected: Int
    )
}
