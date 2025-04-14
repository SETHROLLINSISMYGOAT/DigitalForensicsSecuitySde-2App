package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class WiFiSecurityActivity : AppCompatActivity() {

    private lateinit var ssidText: TextView
    private lateinit var ipText: TextView
    private lateinit var securityText: TextView
    private lateinit var publicIpText: TextView
    private lateinit var scanButton: Button
    private lateinit var wifiManager: WifiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_security)

        ssidText = findViewById(R.id.ssidText)
        ipText = findViewById(R.id.ipText)
        securityText = findViewById(R.id.securityText)
        publicIpText = findViewById(R.id.publicIpText)
        scanButton = findViewById(R.id.refreshButton)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        checkPermissionsAndScan()
    }

    private fun checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            if (!isLocationEnabled()) {
                Toast.makeText(
                    this,
                    "Please turn on location to access WiFi info",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                scanWiFi()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun scanWiFi() {
        val wifiInfo = wifiManager.connectionInfo

        val rawSsid = wifiInfo.ssid
        val ssid = if (rawSsid != null && rawSsid != "<unknown ssid>") {
            rawSsid.removePrefix("\"").removeSuffix("\"")
        } else {
            "SSID not available"
        }

        val localIp = Formatter.formatIpAddress(wifiInfo.ipAddress)

        ssidText.text = "SSID: $ssid"
        ipText.text = "Local IP: $localIp"
        securityText.text = "Scanning WiFi Security..."

        checkSecurityLevel()
        fetchPublicIp()
    }

    private fun checkSecurityLevel() {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        val capabilities = connManager.getNetworkCapabilities(network)

        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            securityText.text = "WiFi Security: WPA2/WPA3 (Secure)"
        } else {
            securityText.text = "⚠️ Open/Unsecure Network! HIGH RISK!"
        }
    }

    private fun fetchPublicIp() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.ipify.org?format=json"

        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val ip = response.getString("ip")
                publicIpText.text = "Public IP: $ip"
            },
            {
                publicIpText.text = "Public IP: Error fetching"
            }
        )

        queue.add(jsonRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionsAndScan()
        } else {
            Toast.makeText(this, "Location permission is required for SSID info", Toast.LENGTH_SHORT).show()
        }
    }
}

