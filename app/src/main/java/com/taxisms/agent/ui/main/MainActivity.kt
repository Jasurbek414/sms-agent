package com.taxisms.agent.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.taxisms.agent.ui.dashboard.DashboardScreen
import com.taxisms.agent.ui.dashboard.DashboardViewModel
import com.taxisms.agent.ui.theme.SmsAgentTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Ilovaning asosiy kirish ekrani.
 * Kerakli ruxsatnomalarni so'raydi va UI qismini boshlaydi.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    // Ruxsatnomalar ro'yxati
    private val requiredPermissions = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    // Ruxsat so'rovchisi callback
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Ruxsatlar berildi", Toast.LENGTH_SHORT).show()
            dashboardViewModel.loadSimInfo() // SIMlarni qayta tekshirish
        } else {
            Toast.makeText(this, "SMS Agent ishlashi uchun SMS ruxsati zarur!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ruxsatlarni tekshirish va so'rash
        checkAndRequestPermissions()

        setContent {
            SmsAgentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        mainViewModel = mainViewModel,
                        dashboardViewModel = dashboardViewModel
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.checkServiceStatus()
        mainViewModel.checkNotificationListenerPermission()
        dashboardViewModel.loadSimInfo()
    }
}
