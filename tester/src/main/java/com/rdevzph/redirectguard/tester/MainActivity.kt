package com.rdevzph.redirectguard.tester

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REDIRECT_GUARD_PKG = "com.rdevzph.redirectguard"
private const val REDIRECT_GUARD_SERVICE =
    "com.rdevzph.redirectguard/com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService"
private const val REDIRECT_GUARD_SERVICE_SHORT =
    "com.rdevzph.redirectguard/.service.RedirectGuardAccessibilityService"

class MainActivity : ComponentActivity() {

    private val isAppInstalled = mutableStateOf(false)
    private val isServiceActive = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStatus()

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                TesterScreen(
                    isGuardInstalled = isAppInstalled.value,
                    isGuardActive = isServiceActive.value,
                    onRefreshStatus = { updateStatus() },
                    onOpenGuardApp = { openApp(REDIRECT_GUARD_PKG) },
                    onOpenAccessibilitySettings = { openAccessibilitySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        isAppInstalled.value = checkPackageInstalled(this, REDIRECT_GUARD_PKG)
        isServiceActive.value = checkAccessibilityServiceActive(this)
    }

    private fun openApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "App not installed: $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

private fun checkPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun resolveInstalledTikTokPackage(context: Context): String {
    return when {
        checkPackageInstalled(context, "com.ss.android.ugc.trill") -> "com.ss.android.ugc.trill"
        checkPackageInstalled(context, "com.zhiliaoapp.musically") -> "com.zhiliaoapp.musically"
        else -> "com.ss.android.ugc.trill"
    }
}

private fun checkAccessibilityServiceActive(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(":").any {
        it.equals(REDIRECT_GUARD_SERVICE, ignoreCase = true) ||
                it.equals(REDIRECT_GUARD_SERVICE_SHORT, ignoreCase = true)
    }
}

private fun triggerRedirect(
    context: Context,
    targetPackage: String?,
    uriString: String,
    usePackageSpecific: Boolean
) {
    val effectivePackage = if (targetPackage == "com.zhiliaoapp.musically" || targetPackage == "com.ss.android.ugc.trill") {
        resolveInstalledTikTokPackage(context)
    } else {
        targetPackage
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uriString)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (usePackageSpecific && !effectivePackage.isNullOrBlank()) {
                setPackage(effectivePackage)
            }
        }
        context.startActivity(intent)
        Toast.makeText(context, "Redirect fired: $uriString", Toast.LENGTH_SHORT).show()
    } catch (e: ActivityNotFoundException) {
        // If package-specific launch failed, retry with alternate TikTok package or generic intent
        if (usePackageSpecific && !effectivePackage.isNullOrBlank()) {
            val alternatePkg = when (effectivePackage) {
                "com.zhiliaoapp.musically" -> "com.ss.android.ugc.trill"
                "com.ss.android.ugc.trill" -> "com.zhiliaoapp.musically"
                else -> null
            }
            if (alternatePkg != null && checkPackageInstalled(context, alternatePkg)) {
                try {
                    val retryIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                        setPackage(alternatePkg)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(retryIntent)
                    Toast.makeText(context, "Redirect fired via $alternatePkg: $uriString", Toast.LENGTH_SHORT).show()
                    return
                } catch (ignored: Exception) {}
            }

            // Retry generically without package constraint
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(genericIntent)
                Toast.makeText(context, "Redirect fired (generic): $uriString", Toast.LENGTH_SHORT).show()
                return
            } catch (ignored: Exception) {}
        }

        // If custom scheme failed, try fallback to web URL
        if (!uriString.startsWith("http")) {
            val fallbackUrl = when (effectivePackage) {
                "com.shopee.ph" -> "https://shopee.ph/"
                "com.lazada.android" -> "https://www.lazada.com.ph/"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "https://www.tiktok.com/"
                "com.amazon.mShop.android.shopping" -> "https://www.amazon.com/"
                else -> null
            }
            if (fallbackUrl != null) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    Toast.makeText(context, "Custom scheme not supported; opened web URL", Toast.LENGTH_SHORT).show()
                    return
                } catch (ignored: Exception) {}
            }
        }
        Toast.makeText(context, "No app available to handle this intent", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to launch intent: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TesterScreen(
    isGuardInstalled: Boolean,
    isGuardActive: Boolean,
    onRefreshStatus: () -> Unit,
    onOpenGuardApp: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var customUri by remember { mutableStateOf("https://shopee.ph/product/123/456") }
    var customPackage by remember { mutableStateOf("com.shopee.ph") }
    var usePackageSpecific by remember { mutableStateOf(true) }

    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountdownRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isCountdownRunning) {
        if (isCountdownRunning) {
            countdownSeconds = 3
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            isCountdownRunning = false
            triggerRedirect(
                context = context,
                targetPackage = "com.shopee.ph",
                uriString = "https://shopee.ph/sample-deal",
                usePackageSpecific = usePackageSpecific
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Redirect Guard Tester",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefreshStatus) {
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Redirect Guard Status Card
            item {
                StatusOverviewCard(
                    isGuardInstalled = isGuardInstalled,
                    isGuardActive = isGuardActive,
                    onOpenGuardApp = onOpenGuardApp,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
            }

            // Section 2: Mode Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Intent Dispatch Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (usePackageSpecific) {
                                "Direct Package Intent (targets specific app package name)"
                            } else {
                                "Generic Web Intent (dispatched as an open ACTION_VIEW URI)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = usePackageSpecific,
                                onClick = { usePackageSpecific = true },
                                label = { Text("Direct Package") }
                            )
                            FilterChip(
                                selected = !usePackageSpecific,
                                onClick = { usePackageSpecific = false },
                                label = { Text("Generic Web Intent") }
                            )
                        }
                    }
                }
            }

            // Section 3: Preset Shopping App Redirects
            item {
                Text(
                    text = "Preset Target Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                TargetAppCard(
                    appName = "Shopee",
                    packageName = "com.shopee.ph",
                    sampleUrl = "https://shopee.ph/universal-link/product/123/456",
                    customScheme = "shopeeph://",
                    onTriggerUrl = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.shopee.ph",
                            uriString = "https://shopee.ph/universal-link/product/123/456",
                            usePackageSpecific = usePackageSpecific
                        )
                    },
                    onTriggerScheme = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.shopee.ph",
                            uriString = "shopeeph://",
                            usePackageSpecific = usePackageSpecific
                        )
                    }
                )
            }

            item {
                TargetAppCard(
                    appName = "Lazada",
                    packageName = "com.lazada.android",
                    sampleUrl = "https://s.lazada.com.ph/s.testRedirect",
                    customScheme = "lazada://www.lazada.com.ph/",
                    onTriggerUrl = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.lazada.android",
                            uriString = "https://s.lazada.com.ph/s.testRedirect",
                            usePackageSpecific = usePackageSpecific
                        )
                    },
                    onTriggerScheme = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.lazada.android",
                            uriString = "lazada://www.lazada.com.ph/",
                            usePackageSpecific = usePackageSpecific
                        )
                    }
                )
            }

            item {
                val tikTokPkg = remember(context) { resolveInstalledTikTokPackage(context) }
                val tikTokScheme = if (tikTokPkg == "com.ss.android.ugc.trill") "snssdk1180://" else "snssdk1233://"
                val tikTokSampleUrl = "https://shop.tiktok.com/view/product/1729482910"
                TargetAppCard(
                    appName = "TikTok Shop",
                    packageName = tikTokPkg,
                    sampleUrl = tikTokSampleUrl,
                    customScheme = tikTokScheme,
                    onTriggerUrl = {
                        triggerRedirect(
                            context = context,
                            targetPackage = tikTokPkg,
                            uriString = tikTokSampleUrl,
                            usePackageSpecific = usePackageSpecific
                        )
                    },
                    onTriggerScheme = {
                        triggerRedirect(
                            context = context,
                            targetPackage = tikTokPkg,
                            uriString = tikTokScheme,
                            usePackageSpecific = usePackageSpecific
                        )
                    }
                )
            }

            item {
                TargetAppCard(
                    appName = "Amazon Shopping",
                    packageName = "com.amazon.mShop.android.shopping",
                    sampleUrl = "https://www.amazon.com/dp/B000TEST",
                    customScheme = "com.amazon.mobile.shopping.web://",
                    onTriggerUrl = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.amazon.mShop.android.shopping",
                            uriString = "https://www.amazon.com/dp/B000TEST",
                            usePackageSpecific = usePackageSpecific
                        )
                    },
                    onTriggerScheme = {
                        triggerRedirect(
                            context = context,
                            targetPackage = "com.amazon.mShop.android.shopping",
                            uriString = "com.amazon.mobile.shopping.web://",
                            usePackageSpecific = usePackageSpecific
                        )
                    }
                )
            }

            // Section 4: Delayed Auto-Redirect Simulator (simulates background/ad timer popup)
            item {
                Text(
                    text = "Delayed Ad Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Auto-Redirect After 3 Seconds",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulates ads that wait several seconds after loading before triggering an automatic app launch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isCountdownRunning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Firing redirect in $countdownSeconds seconds...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = { isCountdownRunning = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start 3-Second Countdown")
                            }
                        }
                    }
                }
            }

            // Section 5: Custom URI Tester
            item {
                Text(
                    text = "Custom URI Tester",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = customUri,
                            onValueChange = { customUri = it },
                            label = { Text("URI / Deep Link") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customPackage,
                            onValueChange = { customPackage = it },
                            label = { Text("Target Package Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                triggerRedirect(
                                    context = context,
                                    targetPackage = customPackage.ifBlank { null },
                                    uriString = customUri,
                                    usePackageSpecific = usePackageSpecific && customPackage.isNotBlank()
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Custom Intent")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusOverviewCard(
    isGuardInstalled: Boolean,
    isGuardActive: Boolean,
    onOpenGuardApp: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val isReady = isGuardInstalled && isGuardActive
    val statusColor = if (isReady) Color(0xFF1E8E3E) else Color(0xFFD93025)
    val statusText = when {
        !isGuardInstalled -> "Redirect Guard Not Installed"
        !isGuardActive -> "Redirect Guard Inactive (Service Disabled)"
        else -> "Redirect Guard Active & Protecting"
    }
    val descriptionText = when {
        !isGuardInstalled -> "Install Redirect Guard to test redirect interception."
        !isGuardActive -> "The accessibility service is disabled. Enable it in Accessibility Settings or open Redirect Guard."
        else -> "Accessibility service is running. Sample redirects will be intercepted and logged."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isGuardInstalled) {
                    Button(
                        onClick = onOpenGuardApp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open App")
                    }
                }
                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accessibility Settings")
                }
            }
        }
    }
}

@Composable
fun TargetAppCard(
    appName: String,
    packageName: String,
    sampleUrl: String,
    customScheme: String,
    onTriggerUrl: () -> Unit,
    onTriggerScheme: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTriggerUrl,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Web URL", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onTriggerScheme,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deep Link", maxLines = 1)
                }
            }
        }
    }
}
