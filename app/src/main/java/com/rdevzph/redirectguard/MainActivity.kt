package com.rdevzph.redirectguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.rdevzph.redirectguard.ui.MainScreen
import com.rdevzph.redirectguard.ui.MainViewModel
import com.rdevzph.redirectguard.ui.theme.RedirectGuardTheme
import com.rdevzph.redirectguard.ui.wizard.SetupWizardScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        requestBatteryOptimizationIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            RedirectGuardTheme {
                val isAllRequirementsMet by viewModel.isAllRequirementsMet.collectAsState()
                val hasCompletedSetup by viewModel.hasCompletedSetup.collectAsState()
                var manuallyOpenedWizard by remember { mutableStateOf(false) }

                if (!isAllRequirementsMet || !hasCompletedSetup || manuallyOpenedWizard) {
                    SetupWizardScreen(
                        viewModel = viewModel,
                        canDismiss = isAllRequirementsMet && hasCompletedSetup,
                        onComplete = {
                            if (isAllRequirementsMet) {
                                viewModel.setSetupCompleted(true)
                                manuallyOpenedWizard = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenSetupWizard = { manuallyOpenedWizard = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
                return
            }
        }
        requestBatteryOptimizationIfNeeded()
    }

    private fun requestBatteryOptimizationIfNeeded() {
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        if (!powerManager.isIgnoringBatteryOptimizations(packageName) && !viewModel.hasPromptedBatteryExemption()) {
            viewModel.setHasPromptedBatteryExemption(true)
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore if not supported on specific OEM
            }
        }
    }
}