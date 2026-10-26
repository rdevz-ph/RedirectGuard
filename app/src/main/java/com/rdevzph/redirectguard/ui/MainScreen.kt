package com.rdevzph.redirectguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
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
import com.rdevzph.redirectguard.ui.components.StatusCard
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

    var showLimitationsDialog by remember { mutableStateOf(false) }
    var showAppPickerModal by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Redirect Guard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh status"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
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

                    TextButton(onClick = { showAppPickerModal = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add App")
                    }
                }
            }

            // Preset shopping apps list
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
                        // Whitelist System Apps toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Whitelist System Apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Ignore built-in apps and panels (such as Smart Panel, Settings, System UI)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = whitelistSystemApps,
                                onCheckedChange = { viewModel.toggleWhitelistSystemApps(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Guard All Apps toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guard All External Apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Intercept transitions to any external app, not just shopping apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = guardAllExternalApps,
                                onCheckedChange = { viewModel.toggleGuardAll(it) }
                            )
                        }

                        if (!guardAllExternalApps) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Shopping App Presets",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GuardedApp.DEFAULT_SHOPPING_PRESETS.forEach { preset ->
                                val isChecked = guardedPackages.contains(preset.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.togglePackageGuarded(preset.packageName, !isChecked)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = preset.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            viewModel.togglePackageGuarded(preset.packageName, it)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Whitelisted Sources Section
            if (whitelistedPackages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whitelisted Sources (${whitelistedPackages.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { viewModel.clearWhitelistedSources() }) {
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
                            whitelistedPackages.forEach { pkg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (pkg == "com.android.chrome") "Google Chrome" else pkg,
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
                        }
                    }
                }
            }

            // Section: Recent Activity Log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Interceptions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (recentRedirects.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (recentRedirects.isEmpty()) {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No redirects recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "When an ad attempts to open an external app, details will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(recentRedirects) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = record.sourceAppName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "to",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = record.targetAppName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val timeString = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                    .format(Date(record.timestamp))

                                Text(
                                    text = "$timeString • ${record.triggerReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!record.deepLinkUri.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Link: ${record.deepLinkUri}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val (badgeBg, badgeFg, label) = when (record.actionTaken) {
                                RedirectAction.BLOCKED -> Triple(StatusInactiveContainer, StatusInactive, "Blocked")
                                RedirectAction.ALLOWED -> Triple(StatusActiveContainer, StatusActive, "Allowed")
                                RedirectAction.PROMPTED -> Triple(StatusWarningContainer, StatusWarning, "Prompted")
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
                                Text("Open on GitHub", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

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
                TextButton(onClick = { showLimitationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // App Picker Dialog
    if (showAppPickerModal) {
        AlertDialog(
            onDismissRequest = { showAppPickerModal = false },
            title = {
                Text(
                    text = "Add Application to Guard List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search installed apps") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredApps = installedApps.filter {
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                                it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isGuarded = guardedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.togglePackageGuarded(app.packageName, !isGuarded)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (app.isSystemApp) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "System",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = isGuarded,
                                    onCheckedChange = {
                                        viewModel.togglePackageGuarded(app.packageName, it)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPickerModal = false }) {
                    Text("Done")
                }
            }
        )
    }
}
