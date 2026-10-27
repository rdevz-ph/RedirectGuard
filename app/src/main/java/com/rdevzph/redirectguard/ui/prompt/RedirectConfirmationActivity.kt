package com.rdevzph.redirectguard.ui.prompt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.rdevzph.redirectguard.RedirectGuardApplication
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.model.RedirectRecord
import com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService
import com.rdevzph.redirectguard.ui.theme.RedirectGuardTheme
import com.rdevzph.redirectguard.ui.theme.StatusInactive
import java.util.UUID

data class PromptData(
    val sourcePkg: String,
    val targetPkg: String,
    val sourceName: String,
    val targetName: String,
    val deepLinkUri: String?,
    val sourceIcon: Drawable?,
    val targetIcon: Drawable?
)

class RedirectConfirmationActivity : ComponentActivity() {

    private var isAllowedByUser = false
    private var promptData by mutableStateOf<PromptData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateFromIntent(intent)

        val blockAction = {
            val current = promptData
            if (current != null) {
                recordAction(current.sourcePkg, current.targetPkg, current.sourceName, current.targetName, RedirectAction.BLOCKED, current.deepLinkUri)
                RedirectGuardApplication.instance.shizukuHelper.forceStopPackage(current.targetPkg)
                RedirectGuardAccessibilityService.instance?.bringPackageToFront(current.sourcePkg)
            }
            finish()
        }

        val openTargetAction = {
            val current = promptData
            if (current != null) {
                isAllowedByUser = true
                RedirectGuardAccessibilityService.instance?.allowTargetTemporarily(current.targetPkg, current.sourcePkg)
                recordAction(current.sourcePkg, current.targetPkg, current.sourceName, current.targetName, RedirectAction.ALLOWED, current.deepLinkUri)
                try {
                    val targetIntent = if (!current.deepLinkUri.isNullOrBlank()) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(current.deepLinkUri)).apply {
                            setPackage(current.targetPkg)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        packageManager.getLaunchIntentForPackage(current.targetPkg)
                    }

                    if (targetIntent != null) {
                        try {
                            startActivity(targetIntent)
                        } catch (e: Exception) {
                            val fallback = packageManager.getLaunchIntentForPackage(current.targetPkg)
                            if (fallback != null) {
                                startActivity(fallback)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            blockAction()
        }

        setContent {
            RedirectGuardTheme {
                val data = promptData
                if (data != null) {
                    ConfirmationPromptScreen(
                        sourcePkg = data.sourcePkg,
                        targetPkg = data.targetPkg,
                        sourceName = data.sourceName,
                        targetName = data.targetName,
                        deepLinkUri = data.deepLinkUri,
                        sourceIcon = data.sourceIcon,
                        targetIcon = data.targetIcon,
                        onBlock = blockAction,
                        onAllow = openTargetAction,
                        onAlwaysAllowSource = {
                            RedirectGuardApplication.instance.preferencesManager.addWhitelistedSource(data.sourcePkg)
                            openTargetAction()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isAllowedByUser = false
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent) {
        val sourcePkg = intent.getStringExtra(EXTRA_SOURCE_PKG) ?: "Unknown"
        val targetPkg = intent.getStringExtra(EXTRA_TARGET_PKG) ?: "Unknown"
        val sourceName = intent.getStringExtra(EXTRA_SOURCE_NAME) ?: sourcePkg
        val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: targetPkg
        val deepLinkUri = intent.getStringExtra(EXTRA_DEEP_LINK_URI)
        promptData = PromptData(
            sourcePkg = sourcePkg,
            targetPkg = targetPkg,
            sourceName = sourceName,
            targetName = targetName,
            deepLinkUri = deepLinkUri,
            sourceIcon = getAppIcon(sourcePkg),
            targetIcon = getAppIcon(targetPkg)
        )
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun recordAction(
        sourcePkg: String,
        targetPkg: String,
        sourceName: String,
        targetName: String,
        action: RedirectAction,
        deepLinkUri: String?
    ) {
        val record = RedirectRecord(
            id = UUID.randomUUID().toString(),
            sourcePackage = sourcePkg,
            targetPackage = targetPkg,
            sourceAppName = sourceName,
            targetAppName = targetName,
            timestamp = System.currentTimeMillis(),
            actionTaken = action,
            triggerReason = if (action == RedirectAction.BLOCKED) "User Blocked Redirect" else "User Allowed Redirect",
            deepLinkUri = deepLinkUri
        )
        RedirectGuardApplication.instance.preferencesManager.addRedirectRecord(record)
    }

    override fun onPause() {
        super.onPause()
        if (!isAllowedByUser && !isFinishing) {
            val targetPkg = promptData?.targetPkg ?: intent.getStringExtra(EXTRA_TARGET_PKG)
            if (!targetPkg.isNullOrBlank()) {
                RedirectGuardApplication.instance.shizukuHelper.forceStopPackage(targetPkg)
            }
        }
    }

    companion object {
        const val EXTRA_SOURCE_PKG = "extra_source_pkg"
        const val EXTRA_TARGET_PKG = "extra_target_pkg"
        const val EXTRA_SOURCE_NAME = "extra_source_name"
        const val EXTRA_TARGET_NAME = "extra_target_name"
        const val EXTRA_DEEP_LINK_URI = "extra_deep_link_uri"
    }
}

@Composable
fun ConfirmationPromptScreen(
    sourcePkg: String,
    targetPkg: String,
    sourceName: String,
    targetName: String,
    deepLinkUri: String?,
    sourceIcon: Drawable?,
    targetIcon: Drawable?,
    onBlock: () -> Unit,
    onAllow: () -> Unit,
    onAlwaysAllowSource: () -> Unit
) {
    var secondsRemaining by remember(targetPkg, sourcePkg) { mutableIntStateOf(15) }

    DisposableEffect(targetPkg, sourcePkg) {
        val timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                onBlock()
            }
        }.start()

        onDispose {
            timer.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Alert",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Redirect Guard Confirmation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "An application attempted an automatic external redirect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // App transition comparison card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Source app row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (sourceIcon != null) {
                                Image(
                                    bitmap = sourceIcon.toBitmap(96, 96).asImageBitmap(),
                                    contentDescription = sourceName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Originating App",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = sourceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Arrow down
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Redirecting to",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Target app row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (targetIcon != null) {
                                Image(
                                    bitmap = targetIcon.toBitmap(96, 96).asImageBitmap(),
                                    contentDescription = targetName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target Application",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = targetName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!deepLinkUri.isNullOrBlank()) {
                    var isDeepLinkExpanded by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Captured Deep Link",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDeepLinkExpanded) "Hidden by default for privacy" else "Hidden for privacy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { isDeepLinkExpanded = !isDeepLinkExpanded }
                                ) {
                                    Text(
                                        text = if (isDeepLinkExpanded) "Hide" else "View",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            if (isDeepLinkExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SelectionContainer(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = deepLinkUri,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val context = LocalContext.current
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Deep Link", deepLinkUri))
                                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Link",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Do you want to continue to $targetName or stay in $sourceName?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Button(
                    onClick = onBlock,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusInactive,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Stay in $sourceName ($secondsRemaining s)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Allow & Open $targetName")
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onAlwaysAllowSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Always allow redirects from $sourceName",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
