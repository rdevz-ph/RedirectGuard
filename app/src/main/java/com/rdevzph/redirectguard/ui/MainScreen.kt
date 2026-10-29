package com.rdevzph.redirectguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdevzph.redirectguard.model.AccessibilityHealthState
import com.rdevzph.redirectguard.model.GuardedApp
import com.rdevzph.redirectguard.model.InterceptionMode
import com.rdevzph.redirectguard.model.InterceptionStrategy
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.ui.components.AppsView
import com.rdevzph.redirectguard.ui.components.LogsView
import com.rdevzph.redirectguard.ui.components.M3ExpressiveFloatingToolbar
import com.rdevzph.redirectguard.ui.components.StatusCard
import com.rdevzph.redirectguard.util.LogLevel
import com.rdevzph.redirectguard.ui.theme.StatusActive
import com.rdevzph.redirectguard.ui.theme.StatusActiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusInactive
import com.rdevzph.redirectguard.ui.theme.StatusInactiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusWarning
import com.rdevzph.redirectguard.ui.theme.StatusWarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSetupWizard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isGuardEnabled by viewModel.isGuardEnabled.collectAsState()
    val interceptionMode by viewModel.interceptionMode.collectAsState()
    val interceptionStrategy by viewModel.interceptionStrategy.collectAsState()
    val guardAllExternalApps by viewModel.guardAllExternalApps.collectAsState()
    val whitelistSystemApps by viewModel.whitelistSystemApps.collectAsState()
    val guardedPackages by viewModel.guardedPackages.collectAsState()
    val whitelistedPackages by viewModel.whitelistedPackages.collectAsState()
    val recentRedirects by viewModel.recentRedirects.collectAsState()

    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val accessibilityHealth by viewModel.accessibilityHealth.collectAsState()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()
    val isShizukuInstalled by viewModel.isShizukuInstalled.collectAsState()
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val isShizukuAuthorized by viewModel.isShizukuAuthorized.collectAsState()
    val showStatusNotification by viewModel.showStatusNotification.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState()

    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val appLogs by viewModel.appLogs.collectAsState()
    val showRedirectLogs by viewModel.showRedirectLogs.collectAsState()
    val showDebugLogs by viewModel.showDebugLogs.collectAsState()
    val showErrorLogs by viewModel.showErrorLogs.collectAsState()
    val showWarnLogs by viewModel.showWarnLogs.collectAsState()
    val logSearchQuery by viewModel.logSearchQuery.collectAsState()

    var showLimitationsDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }
    var showGuardAllWarningDialog by remember { mutableStateOf(false) }
    var showDisableWhitelistWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            MainScreenTab.DASHBOARD -> "Redirect Guard"
                            MainScreenTab.APPS -> "Applications"
                            MainScreenTab.LOGS -> "Logs & Activity"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSetupWizard) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Setup and Requirements"
                        )
                    }
                    IconButton(onClick = { showLimitationsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About and limitations"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainScreenTab.DASHBOARD -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Master Protection Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGuardEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isGuardEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isGuardEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGuardEnabled) "Protection Active" else "Protection Paused",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGuardEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isGuardEnabled) {
                                    "Intercepting automatic ad redirects to external apps."
                                } else {
                                    "Redirect Guard is currently disabled."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGuardEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isGuardEnabled,
                            onCheckedChange = { viewModel.toggleGuard(it) }
                        )
                    }
                }
            }

            // Section: System Services & Permissions
            item {
                Text(
                    text = "System Services & Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Accessibility Service Health Card
            item {
                val hasShizukuPrivilege = isShizukuRunning && isShizukuAuthorized
                when (accessibilityHealth) {
                    AccessibilityHealthState.HEALTHY -> {
                        StatusCard(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Accessibility Service",
                            statusText = "Active & Healthy",
                            isSuccess = true,
                            description = "Monitors app window transitions to detect unwanted redirects. Running normally.",
                            secondaryButtonText = "Accessibility Settings",
                            onSecondaryActionClick = { viewModel.openAccessibilitySettings() },
                            tertiaryButtonText = "Open App Info",
                            onTertiaryActionClick = { viewModel.openAppInfoSettings() }
                        )
                    }
                    AccessibilityHealthState.KILLED_DESYNCED -> {
                        StatusCard(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Accessibility Service",
                            statusText = "Killed / Desynced",
                            isSuccess = false,
                            statusBgOverride = StatusInactiveContainer,
                            statusFgOverride = StatusInactive,
                            description = "Android stopped the background service process while the setting remained enabled. Force stop the app first to clear the stuck state, then re-open the app and use 1-Tap Enable or Accessibility Settings.",
                            actionButtonText = if (hasShizukuPrivilege) "Force Stop App (Shizuku)" else "Open App Info (Force Stop)",
                            onActionClick = {
                                if (hasShizukuPrivilege) {
                                    viewModel.forceStopAppViaShizuku()
                                } else {
                                    viewModel.openAppInfoSettings()
                                }
                            },
                            secondaryButtonText = if (hasShizukuPrivilege) "1-Tap Enable (Shizuku)" else "Accessibility Settings",
                            onSecondaryActionClick = {
                                if (hasShizukuPrivilege) {
                                    viewModel.enableAccessibilityViaShizuku()
                                } else {
                                    viewModel.openAccessibilitySettings()
                                }
                            },
                            tertiaryButtonText = if (hasShizukuPrivilege) "Accessibility Settings" else "Open App Info",
                            onTertiaryActionClick = {
                                if (hasShizukuPrivilege) {
                                    viewModel.openAccessibilitySettings()
                                } else {
                                    viewModel.openAppInfoSettings()
                                }
                            }
                        )
                    }
                    AccessibilityHealthState.DISABLED -> {
                        StatusCard(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Accessibility Service",
                            statusText = "Not Running",
                            isSuccess = false,
                            description = "Required to detect app transitions when an ad triggers an external shopping application. Does not read screen text or private data.",
                            actionButtonText = if (hasShizukuPrivilege) "1-Tap Enable (Shizuku)" else "Enable in Settings",
                            onActionClick = {
                                if (hasShizukuPrivilege) {
                                    viewModel.enableAccessibilityViaShizuku()
                                } else {
                                    viewModel.openAccessibilitySettings()
                                }
                            },
                            secondaryButtonText = if (hasShizukuPrivilege) "Accessibility Settings" else "Open App Info",
                            onSecondaryActionClick = {
                                if (hasShizukuPrivilege) {
                                    viewModel.openAccessibilitySettings()
                                } else {
                                    viewModel.openAppInfoSettings()
                                }
                            },
                            tertiaryButtonText = if (hasShizukuPrivilege) "Open App Info" else null,
                            onTertiaryActionClick = { viewModel.openAppInfoSettings() }
                        )
                    }
                }
            }

            // Overlay Permission Card
            item {
                val hasShizukuPrivilege = isShizukuRunning && isShizukuAuthorized
                StatusCard(
                    icon = Icons.Default.Layers,
                    title = "Display Over Other Apps",
                    statusText = if (canDrawOverlays) "Granted" else "Action Needed",
                    isSuccess = canDrawOverlays,
                    description = "Required to present the confirmation prompt on top of whatever app you are using.",
                    actionButtonText = if (!canDrawOverlays) "Open Overlay Settings" else null,
                    onActionClick = { viewModel.openOverlaySettings() },
                    secondaryButtonText = if (!canDrawOverlays && hasShizukuPrivilege) "Grant via Shizuku" else null,
                    onSecondaryActionClick = { viewModel.grantOverlayViaShizuku() }
                )
            }

            // Battery Optimization Card
            item {
                val hasShizukuPrivilege = isShizukuRunning && isShizukuAuthorized
                StatusCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Optimization",
                    statusText = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Optimized (Restricted)",
                    isSuccess = isIgnoringBatteryOptimizations,
                    description = if (isIgnoringBatteryOptimizations) {
                        "Background protection is unrestricted. This helps prevent Android from stopping the accessibility service in the background."
                    } else {
                        "Android may stop or restrict the background accessibility service to save battery. Setting to Unrestricted helps keep protection active."
                    },
                    actionButtonText = if (!isIgnoringBatteryOptimizations) "Unrestrict Battery" else null,
                    onActionClick = { viewModel.requestIgnoreBatteryOptimizations() },
                    secondaryButtonText = if (!isIgnoringBatteryOptimizations && hasShizukuPrivilege) "Whitelist via Shizuku" else null,
                    onSecondaryActionClick = { viewModel.whitelistBatteryOptimizationViaShizuku() }
                )
            }

            // Shizuku Card (Required)
            item {
                val shizukuStatusText = when {
                    isShizukuRunning && isShizukuAuthorized -> "Running & Authorized"
                    isShizukuRunning -> "Action Needed (Authorize)"
                    isShizukuInstalled -> "Action Needed (Start Shizuku)"
                    else -> "Action Needed (Install Shizuku)"
                }
                val shizukuSuccess = isShizukuRunning && isShizukuAuthorized

                StatusCard(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Shizuku Privileged Bridge (Required)",
                    statusText = shizukuStatusText,
                    isSuccess = shizukuSuccess,
                    description = "Required to immediately terminate redirected app processes and stop deep links from running in the background.",
                    actionButtonText = if (isShizukuRunning && !isShizukuAuthorized) {
                        "Authorize Shizuku"
                    } else if (!isShizukuRunning || !isShizukuInstalled) {
                        "Open Shizuku"
                    } else null,
                    onActionClick = {
                        if (isShizukuRunning && !isShizukuAuthorized) {
                            viewModel.requestShizukuPermission()
                        } else {
                            viewModel.openShizukuApp()
                        }
                    }
                )
            }

            // Status Notification Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (showStatusNotification && areNotificationsEnabled) {
                                                Icons.Default.NotificationsActive
                                            } else {
                                                Icons.Default.NotificationsOff
                                            },
                                            contentDescription = null,
                                            tint = if (showStatusNotification && areNotificationsEnabled) {
                                                StatusActive
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Status Notification",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (showStatusNotification) "Show status in notification bar" else "Notification hidden",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = showStatusNotification,
                                onCheckedChange = { viewModel.toggleStatusNotification(it) }
                            )
                        }

                        if (!areNotificationsEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Notifications are disabled in system settings.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    TextButton(onClick = { viewModel.openNotificationSettings() }) {
                                        Text("Enable", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                        }
                    }
                }
            }

            // Section: Protection Mode
            item {
                Text(
                    text = "Interception Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "When an external redirect is caught:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = interceptionMode == InterceptionMode.CONFIRMATION_DIALOG,
                                onClick = { viewModel.setInterceptionMode(InterceptionMode.CONFIRMATION_DIALOG) },
                                label = { Text("Ask Confirmation") },
                                leadingIcon = if (interceptionMode == InterceptionMode.CONFIRMATION_DIALOG) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = interceptionMode == InterceptionMode.AUTO_RETURN,
                                onClick = { viewModel.setInterceptionMode(InterceptionMode.AUTO_RETURN) },
                                label = { Text("Instant Auto-Return") },
                                leadingIcon = if (interceptionMode == InterceptionMode.AUTO_RETURN) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (interceptionMode == InterceptionMode.CONFIRMATION_DIALOG) {
                                "Displays a prompt asking whether to proceed or return to the previous app."
                            } else {
                                "Immediately sends you back to your previous app without displaying a prompt."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interception Mechanism Info Card (Shizuku Required)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Interception Mechanism: Shizuku Process Kill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Redirected applications are immediately terminated using Shizuku (am force-stop). This cleanly stops deep links and aggressive ads from persisting in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!isShizukuRunning || !isShizukuAuthorized) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Shizuku is required to terminate redirected apps",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Please start and authorize Shizuku for redirect protection.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    if (isShizukuRunning && !isShizukuAuthorized) {
                                        TextButton(onClick = { viewModel.requestShizukuPermission() }) {
                                            Text("Authorize")
                                        }
                                    } else {
                                        TextButton(onClick = { viewModel.openShizukuApp() }) {
                                            Text("Open")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Guarded Apps
            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guarded Applications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(onClick = { viewModel.selectTab(MainScreenTab.APPS) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Manage Apps")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        val installedGuardedCount = installedApps.count { guardedPackages.contains(it.packageName) }
                        Text(
                            text = if (installedGuardedCount > 0) {
                                "${installedGuardedCount} application" + (if (installedGuardedCount > 1) "s" else "") + " currently guarded against external redirects."
                            } else {
                                "No applications are currently guarded."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage guard and whitelist settings for installed applications in the Applications tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.selectTab(MainScreenTab.APPS) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage in Applications")
                        }
                    }
                }
            }

            val userWhitelistedPackages = whitelistedPackages.filter { pkg ->
                val app = installedApps.find { it.packageName == pkg }
                app != null && !app.isSystemApp
            }

            // Whitelisted Sources Section (External User Apps Only)
            if (userWhitelistedPackages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whitelisted Sources (${userWhitelistedPackages.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { viewModel.removeWhitelistedSources(userWhitelistedPackages) }) {
                            Text("Clear All")
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Redirects originating from these apps are never intercepted because they were marked as always allowed. Tap the remove icon to enable redirect protection for that app again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            userWhitelistedPackages.take(5).forEach { pkg ->
                                val appItem = installedApps.find { it.packageName == pkg }
                                val appName = appItem?.appName ?: if (pkg == "com.android.chrome") "Google Chrome" else pkg
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = pkg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeWhitelistedSource(pkg) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove from whitelist",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (userWhitelistedPackages.size > 5) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.selectTab(MainScreenTab.APPS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View all ${userWhitelistedPackages.size} in Applications tab")
                                }
                            }
                        }
                    }
                }
            }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
                MainScreenTab.APPS -> {
                    AppsView(
                        installedApps = installedApps,
                        guardedPackages = guardedPackages,
                        whitelistedPackages = whitelistedPackages,
                        whitelistSystemApps = whitelistSystemApps,
                        guardAllExternalApps = guardAllExternalApps,
                        onToggleWhitelistSystemApps = { isChecked ->
                            if (!isChecked) {
                                showDisableWhitelistWarningDialog = true
                            } else {
                                viewModel.toggleWhitelistSystemApps(true)
                            }
                        },
                        onToggleGuardAll = { isChecked ->
                            if (isChecked) {
                                showGuardAllWarningDialog = true
                            } else {
                                viewModel.toggleGuardAll(false)
                            }
                        },
                        onToggleGuarded = { pkg, guarded -> viewModel.togglePackageGuarded(pkg, guarded) },
                        onToggleWhitelisted = { pkg, whitelisted -> viewModel.togglePackageWhitelisted(pkg, whitelisted) },
                        isPackageWhitelisted = { viewModel.isPackageWhitelisted(it) },
                        isPermanentWhitelisted = { viewModel.isPermanentWhitelisted(it) },
                        isLoading = isLoadingApps,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainScreenTab.LOGS -> {
                    LogsView(
                        redirectRecords = recentRedirects,
                        appLogs = appLogs,
                        showRedirects = showRedirectLogs,
                        showDebug = showDebugLogs,
                        showErrors = showErrorLogs,
                        showWarnings = showWarnLogs,
                        onToggleRedirects = { viewModel.toggleShowRedirectLogs(it) },
                        onToggleDebug = { viewModel.toggleShowDebugLogs(it) },
                        onToggleErrors = { viewModel.toggleShowErrorLogs(it) },
                        onToggleWarnings = { viewModel.toggleShowWarnLogs(it) },
                        searchQuery = logSearchQuery,
                        onSearchQueryChange = { viewModel.setLogSearchQuery(it) },
                        onClearAll = { viewModel.clearAllLogs() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            val installedGuardedCount = installedApps.count { guardedPackages.contains(it.packageName) }
            val visibleErrorCount = if (showErrorLogs) appLogs.count { it.level == LogLevel.ERROR } else 0
            val visibleLogsCount = (if (showRedirectLogs) recentRedirects.size else 0) +
                appLogs.count { entry ->
                    when (entry.level) {
                        LogLevel.DEBUG -> showDebugLogs
                        LogLevel.ERROR -> showErrorLogs
                        LogLevel.WARN -> showWarnLogs
                        LogLevel.INFO -> true
                    }
                }

            M3ExpressiveFloatingToolbar(
                selectedTab = selectedTab,
                onSelectTab = { viewModel.selectTab(it) },
                onRefresh = { viewModel.refreshStatus() },
                guardedCount = installedGuardedCount,
                errorCount = visibleErrorCount,
                totalLogsCount = visibleLogsCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }

    // About, Developer Info & Architecture Dialog
    if (showLimitationsDialog) {
        AlertDialog(
            onDismissRequest = { showLimitationsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Redirect Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.appVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Developer & Repository Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Developer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "rdevz-ph",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "GitHub Repository",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "https://github.com/rdevz-ph/RedirectGuard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { viewModel.openRepositoryUrl() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open on GitHub",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.openIssuesUrl() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Report an Issue",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showHowItWorksDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "How It Works & Limitations",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.openRedirectTesterUrl() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Redirect Tester",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.openDonateUrl() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Donate",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.openPrivacyPolicyUrl() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLimitationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showHowItWorksDialog) {
        AlertDialog(
            onDismissRequest = { showHowItWorksDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "How It Works & Limitations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How Redirect Guard Works",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Redirect Guard uses Android's Accessibility Service to observe window transitions. When a browser, game, or ad-supported app opens an external shopping application, the service detects the transition within milliseconds and either displays a confirmation dialog or sends you immediately back to your previous app.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Android Security Limitations",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "On non-rooted Android, third-party apps cannot hook the system activity manager to pause an activity launch before it begins. Therefore, Redirect Guard acts as an immediate post-launch checkpoint, stopping you from engaging with the ad and restoring your previous screen instantly.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Role of Shizuku",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Shizuku provides access to Android ADB shell commands without root. In Redirect Guard, Shizuku is used to streamline granting overlay permissions and terminating target app processes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHowItWorksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Warning dialog before enabling Guard All External Apps
    if (showGuardAllWarningDialog) {
        AlertDialog(
            onDismissRequest = { showGuardAllWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Enable Guard All External Apps?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Guard All External Apps intercepts transitions to every external application on your device.\n\nThis can block legitimate app redirects and shortcuts, such as tapping notification messages in Messenger or opening browser logins inside games.\n\nIf you use apps or games that rely on redirects, keep this option disabled and guard only suspicious apps instead.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleGuardAll(true)
                        showGuardAllWarningDialog = false
                    }
                ) {
                    Text("Enable Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGuardAllWarningDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Warning dialog before disabling Whitelist System Apps
    if (showDisableWhitelistWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDisableWhitelistWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Disable System Apps Whitelist?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Disabling this removes redirect protection exemptions for all built-in system applications.\n\nSystem transitions (such as opening Settings, System UI panels, or system dialogs) may be intercepted, potentially causing unexpected app closures.\n\nWe recommend keeping system apps whitelisted for device stability.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleWhitelistSystemApps(false)
                        showDisableWhitelistWarningDialog = false
                    }
                ) {
                    Text("Disable Anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDisableWhitelistWarningDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
