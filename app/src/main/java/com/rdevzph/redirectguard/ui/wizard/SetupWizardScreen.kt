package com.rdevzph.redirectguard.ui.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdevzph.redirectguard.model.AccessibilityHealthState
import com.rdevzph.redirectguard.ui.MainViewModel
import com.rdevzph.redirectguard.ui.theme.StatusActive
import com.rdevzph.redirectguard.ui.theme.StatusActiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusWarning
import com.rdevzph.redirectguard.ui.theme.StatusWarningContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    viewModel: MainViewModel,
    canDismiss: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isShizukuInstalled by viewModel.isShizukuInstalled.collectAsState()
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val isShizukuAuthorized by viewModel.isShizukuAuthorized.collectAsState()
    val accessibilityHealth by viewModel.accessibilityHealth.collectAsState()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState()
    val isAllRequirementsMet by viewModel.isAllRequirementsMet.collectAsState()

    val isAccessibilityActive = accessibilityHealth != AccessibilityHealthState.DISABLED

    val completedSteps = listOf(
        isShizukuAuthorized,
        isAccessibilityActive,
        canDrawOverlays,
        areNotificationsEnabled,
        isIgnoringBatteryOptimizations
    ).count { it }

    val totalSteps = 5
    val hasPendingOtherSteps = isShizukuAuthorized && completedSteps < totalSteps

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Setup & Requirements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (canDismiss) {
                        IconButton(onClick = onComplete) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Setup"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = onComplete,
                        enabled = isAllRequirementsMet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isAllRequirementsMet) {
                                    "Enter Redirect Guard"
                                } else {
                                    "Complete all requirements (${totalSteps - completedSteps} remaining)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isAllRequirementsMet) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = if (isAllRequirementsMet) "All Requirements Satisfied" else "Initial Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllRequirementsMet) StatusActive else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAllRequirementsMet) {
                                "Redirect Guard has all required permissions and services configured. You can now enter the dashboard."
                            } else {
                                "Redirect Guard needs the following permissions and services to detect and block unwanted ad redirects properly."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$completedSteps of $totalSteps steps completed",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAllRequirementsMet) StatusActiveContainer else StatusWarningContainer
                            ) {
                                Text(
                                    text = if (isAllRequirementsMet) "Ready" else "Pending",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllRequirementsMet) StatusActive else StatusWarning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { completedSteps.toFloat() / totalSteps.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isAllRequirementsMet) StatusActive else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Quick 1-Tap Enable All via Shizuku Banner
            if (hasPendingOtherSteps) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1-Tap Configure via Shizuku",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Shizuku is authorized. You can automatically configure all remaining permissions with a single tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.enableAllRequirementsViaShizuku() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1-Tap Enable All Remaining")
                            }
                        }
                    }
                }
            }

            // Step 1: Shizuku Service
            item {
                WizardStepCard(
                    stepNumber = "1",
                    title = "Shizuku Service",
                    description = "Required to terminate redirected apps and configure permissions without root.",
                    icon = Icons.Default.Security,
                    isSatisfied = isShizukuAuthorized,
                    statusText = when {
                        isShizukuAuthorized -> "Authorized & Running"
                        isShizukuRunning -> "Running (Needs Authorization)"
                        isShizukuInstalled -> "Installed (Service Stopped)"
                        else -> "Not Installed"
                    },
                    primaryActionText = when {
                        isShizukuAuthorized -> null
                        isShizukuRunning -> "Authorize Shizuku"
                        isShizukuInstalled -> "Open Shizuku App"
                        else -> "Install Shizuku"
                    },
                    onPrimaryAction = when {
                        isShizukuAuthorized -> null
                        isShizukuRunning -> { { viewModel.requestShizukuPermission() } }
                        else -> { { viewModel.openShizukuApp() } }
                    },
                    secondaryActionText = if (isShizukuRunning && !isShizukuAuthorized) "Open Shizuku" else null,
                    onSecondaryAction = if (isShizukuRunning && !isShizukuAuthorized) { { viewModel.openShizukuApp() } } else null
                )
            }

            // Step 2: Accessibility Service
            item {
                WizardStepCard(
                    stepNumber = "2",
                    title = "Accessibility Service",
                    description = "Required to observe app switches and catch unauthorized redirects when they happen.",
                    icon = Icons.Default.AccessibilityNew,
                    isSatisfied = isAccessibilityActive,
                    statusText = if (isAccessibilityActive) "Active & Monitoring" else "Service Disabled",
                    primaryActionText = if (!isAccessibilityActive && isShizukuAuthorized) "1-Tap Enable (Shizuku)" else null,
                    onPrimaryAction = if (!isAccessibilityActive && isShizukuAuthorized) {
                        { viewModel.enableAccessibilityViaShizuku() }
                    } else null,
                    secondaryActionText = if (!isAccessibilityActive) "Accessibility Settings" else null,
                    onSecondaryAction = if (!isAccessibilityActive) {
                        { viewModel.openAccessibilitySettings() }
                    } else null
                )
            }

            // Step 3: Display Over Other Apps
            item {
                WizardStepCard(
                    stepNumber = "3",
                    title = "Display Over Other Apps",
                    description = "Required to display the safety confirmation prompt over the redirected app.",
                    icon = Icons.Default.Layers,
                    isSatisfied = canDrawOverlays,
                    statusText = if (canDrawOverlays) "Permission Granted" else "Permission Missing",
                    primaryActionText = if (!canDrawOverlays && isShizukuAuthorized) "1-Tap Grant (Shizuku)" else null,
                    onPrimaryAction = if (!canDrawOverlays && isShizukuAuthorized) {
                        { viewModel.grantOverlayViaShizuku() }
                    } else null,
                    secondaryActionText = if (!canDrawOverlays) "Overlay Settings" else null,
                    onSecondaryAction = if (!canDrawOverlays) {
                        { viewModel.openOverlaySettings() }
                    } else null
                )
            }

            // Step 4: Notifications
            item {
                WizardStepCard(
                    stepNumber = "4",
                    title = "Notifications",
                    description = "Required for the background protection status notification and redirect alerts.",
                    icon = Icons.Default.NotificationsActive,
                    isSatisfied = areNotificationsEnabled,
                    statusText = if (areNotificationsEnabled) "Permission Granted" else "Permission Missing",
                    primaryActionText = if (!areNotificationsEnabled && isShizukuAuthorized) "1-Tap Grant (Shizuku)" else null,
                    onPrimaryAction = if (!areNotificationsEnabled && isShizukuAuthorized) {
                        { viewModel.grantNotificationViaShizuku() }
                    } else null,
                    secondaryActionText = if (!areNotificationsEnabled) "Notification Settings" else null,
                    onSecondaryAction = if (!areNotificationsEnabled) {
                        { viewModel.openNotificationSettings() }
                    } else null
                )
            }

            // Step 5: Battery Optimization Exemption
            item {
                WizardStepCard(
                    stepNumber = "5",
                    title = "Battery Optimization Exemption",
                    description = "Prevents Android from stopping the background accessibility service during idle periods.",
                    icon = Icons.Default.BatteryChargingFull,
                    isSatisfied = isIgnoringBatteryOptimizations,
                    statusText = if (isIgnoringBatteryOptimizations) "Exemption Active" else "Optimization Restricted",
                    primaryActionText = if (!isIgnoringBatteryOptimizations && isShizukuAuthorized) "1-Tap Exempt (Shizuku)" else null,
                    onPrimaryAction = if (!isIgnoringBatteryOptimizations && isShizukuAuthorized) {
                        { viewModel.whitelistBatteryOptimizationViaShizuku() }
                    } else null,
                    secondaryActionText = if (!isIgnoringBatteryOptimizations) "Request Exemption" else null,
                    onSecondaryAction = if (!isIgnoringBatteryOptimizations) {
                        { viewModel.requestIgnoreBatteryOptimizations() }
                    } else null
                )
            }
        }
    }
}

@Composable
fun WizardStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    isSatisfied: Boolean,
    statusText: String,
    primaryActionText: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusBg = if (isSatisfied) StatusActiveContainer else StatusWarningContainer
    val statusFg = if (isSatisfied) StatusActive else StatusWarning

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            if (isSatisfied) {
                StatusActive.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Step Badge
                Surface(
                    shape = CircleShape,
                    color = if (isSatisfied) StatusActiveContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSatisfied) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusActive,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isSatisfied && (primaryActionText != null || secondaryActionText != null)) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (primaryActionText != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = primaryActionText, maxLines = 1)
                        }
                    }

                    if (secondaryActionText != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = if (primaryActionText != null) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = secondaryActionText, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
