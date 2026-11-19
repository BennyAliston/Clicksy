package com.clicksy.keyboard.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Event bridge to communicate permission results from [VoicePermissionActivity]
 * back to the active InputMethodService.
 */
object VoicePermissionBridge {
    var onPermissionGranted: (() -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    fun notifyPermissionResult(granted: Boolean) {
        if (granted) {
            onPermissionGranted?.invoke()
        } else {
            onPermissionDenied?.invoke()
        }
    }
}

/**
 * A transparent trampoline activity that requests the RECORD_AUDIO permission.
 * Because InputMethodService is a Service and cannot directly launch permission dialogs,
 * this lightweight activity requests the runtime permission and reports the outcome
 * back to [VoicePermissionBridge].
 */
class VoicePermissionActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        VoicePermissionBridge.notifyPermissionResult(isGranted)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            VoicePermissionBridge.notifyPermissionResult(true)
            finish()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        fun requestPermission(context: Context, onGranted: () -> Unit, onDenied: () -> Unit) {
            VoicePermissionBridge.onPermissionGranted = onGranted
            VoicePermissionBridge.onPermissionDenied = onDenied
            val intent = Intent(context, VoicePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
