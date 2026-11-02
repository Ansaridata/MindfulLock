package com.example.mindfullock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private lateinit var editReason: EditText
    private lateinit var editMinutes: EditText
    private lateinit var editSeconds: EditText
    private lateinit var btnStart: Button
    private lateinit var btnTemp60s: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, LockAdminReceiver::class.java)

        editReason = findViewById(R.id.editReason)
        editMinutes = findViewById(R.id.editMinutes)
        editSeconds = findViewById(R.id.editSeconds)
        btnStart = findViewById(R.id.btnStart)
        btnTemp60s = findViewById(R.id.btnTemp60s)

        checkPermissions()

        btnStart.setOnClickListener {
            val reason = editReason.text.toString().trim()
            val mins = editMinutes.text.toString().toIntOrNull() ?: 0
            val secs = editSeconds.text.toString().toIntOrNull() ?: 0
            val totalSeconds = (mins * 60) + secs

            if (reason.isEmpty()) {
                Toast.makeText(this, "Please enter your reason!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (totalSeconds <= 0) {
                Toast.makeText(this, "Please set a duration greater than 0s", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startLockSession(reason, totalSeconds)
        }

        btnTemp60s.setOnClickListener {
            startLockSession("Emergency 60s Pass", 60)
        }
    }

    private fun startLockSession(reason: String, seconds: Int) {
        val serviceIntent = Intent(this, SessionService::class.java).apply {
            putExtra("REASON", reason)
            putExtra("SECONDS", seconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Session started: $seconds sec", Toast.LENGTH_SHORT).show()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun checkPermissions() {
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable MindfulLock to automatically lock the screen when your time expires."
                )
            }
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
