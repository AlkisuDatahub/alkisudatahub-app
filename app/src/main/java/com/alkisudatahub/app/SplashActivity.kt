package com.alkisudatahub.app

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splashLogo)
        val status = findViewById<TextView>(R.id.splashStatus)

        // Logo entrance: slides up slightly while fading in
        logo.translationY = 60f
        logo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setStartDelay(150)
            .withEndAction {
                status.animate().alpha(1f).setDuration(300).start()
                checkBiometricAndProceed(status)
            }
            .start()
    }

    private fun checkBiometricAndProceed(status: TextView) {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Give the entrance animation a beat to finish before the
                // system fingerprint dialog pops up.
                status.postDelayed({ showBiometricPrompt() }, 400)
            }
            else -> {
                // No fingerprint hardware, none enrolled, or a device that
                // simply can't do it right now — don't lock the user out of
                // their own app, just continue straight in.
                status.text = ""
                goToMainActivity()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                goToMainActivity()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User tapped "Cancel", or hit too many failed attempts, etc.
                // Close the app rather than leaving them stuck on the splash
                // screen — they can reopen the app to try again.
                Toast.makeText(this@SplashActivity, errString, Toast.LENGTH_SHORT).show()
                finishAffinity()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    this@SplashActivity,
                    "Fingerprint not recognized, try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock AlkisuDataHub")
            .setSubtitle("Confirm your fingerprint to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
