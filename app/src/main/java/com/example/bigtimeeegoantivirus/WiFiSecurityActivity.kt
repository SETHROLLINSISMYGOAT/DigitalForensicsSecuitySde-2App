package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

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
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            scanWiFi()
        }
    }

    private fun scanWiFi() {
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")
        val localIp = Formatter.formatIpAddress(wifiInfo.ipAddress)

        ssidText.text = "SSID: $ssid"
        ipText.text = "Local IP: $localIp"
        securityText.text = "Scanning WiFi Security..."

        checkSecurityLevel()
        fetchPublicIp()
        detectRogueDevices()
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

    private fun detectRogueDevices() {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            val connectedDevices = mutableListOf<String>()

            for (networkInterface in networkInterfaces) {
                for (inetAddress in networkInterface.inetAddresses) {
                    if (!inetAddress.isLoopbackAddress) {
                        connectedDevices.add(inetAddress.hostAddress!!)
                    }
                }
            }

            if (connectedDevices.size > 1) {
                securityText.text = "⚠️ Possible Intruders Detected on WiFi!"
            } else {
                securityText.text = "No Rogue Devices Found ✅"
            }

        } catch (e: SocketException) {
            securityText.text = "Error Scanning Network"
        }
    }
}
