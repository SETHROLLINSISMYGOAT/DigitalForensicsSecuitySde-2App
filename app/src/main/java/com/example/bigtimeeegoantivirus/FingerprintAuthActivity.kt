package com.example.bigtimeeegoantivirus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class FingerprintAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint_auth)

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1000)
            return
        }

        checkBiometricSupport()
    }

    private fun checkBiometricSupport() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> showBiometricPrompt()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                showToastAndExit("No biometric sensor found.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                showToastAndExit("Biometric sensor is not available.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                showToastAndExit("No fingerprints enrolled. Please register fingerprint in device settings.")
            else -> showToastAndExit("Biometric authentication not supported.")
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Fingerprint Verified ✅", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@FingerprintAuthActivity, MainActivity::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Fingerprint not recognized ❌", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                showToastAndExit("Authentication error: $errString")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Access")
            .setSubtitle("Verify your fingerprint to continue")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showToastAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
