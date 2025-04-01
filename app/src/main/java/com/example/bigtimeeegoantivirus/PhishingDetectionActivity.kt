package com.example.bigtimeeegoantivirus

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.*
import com.android.volley.toolbox.*
import org.json.JSONObject

class PhishingDetectionActivity : AppCompatActivity() {
    private lateinit var urlInput: EditText
    private lateinit var checkButton: Button
    private lateinit var resultText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phishing_detection)

        urlInput = findViewById(R.id.urlInput)
        checkButton = findViewById(R.id.checkButton)
        resultText = findViewById(R.id.resultText)
        progressBar = findViewById(R.id.progressBar)

        checkButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPhishing(url)
        }
    }

    private fun checkPhishing(url: String) {
        progressBar.visibility = View.VISIBLE
        resultText.text = ""

        val apiKey = "870df8edc61e52371c259645ea8370a766b29310d4b6e827f95deff7de35486d"
        val apiUrl = "https://www.virustotal.com/api/v3/urls"

        val encodedUrl = android.util.Base64.encodeToString(url.toByteArray(), android.util.Base64.NO_WRAP)

        val requestQueue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(Method.POST, apiUrl,
            { response ->
                val jsonResponse = JSONObject(response)
                val analysisId = jsonResponse.getJSONObject("data").getString("id")
                checkScanResult(analysisId)
            },
            { error ->
                progressBar.visibility = View.GONE
                resultText.text = "❌ Error submitting URL!"
                resultText.setTextColor(ContextCompat.getColor(this, R.color.red))
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("x-apikey" to apiKey)
            }

            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf("url" to url)
            }
        }

        requestQueue.add(postRequest)
    }

    private fun checkScanResult(analysisId: String) {
        val apiKey = "870df8edc61e52371c259645ea8370a766b29310d4b6e827f95deff7de35486d"
        val resultUrl = "https://www.virustotal.com/api/v3/analyses/$analysisId"

        val requestQueue = Volley.newRequestQueue(this)

        val getRequest = object : JsonObjectRequest(Method.GET, resultUrl, null,
            { response ->
                progressBar.visibility = View.GONE
                val stats = response.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats")
                val malicious = stats.getInt("malicious")
                val suspicious = stats.getInt("suspicious")

                if (malicious > 0 || suspicious > 0) {
                    resultText.text = "⚠️ Warning! This URL is unsafe."
                    resultText.setTextColor(ContextCompat.getColor(this, R.color.red))
                } else {
                    resultText.text = "✅ This URL is safe!"
                    resultText.setTextColor(ContextCompat.getColor(this, R.color.green))
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                resultText.text = "❌ Error checking results!"
                resultText.setTextColor(ContextCompat.getColor(this, R.color.red))
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("x-apikey" to apiKey)
            }
        }

        requestQueue.add(getRequest)
    }
}
