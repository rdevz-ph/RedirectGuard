package com.rdevzph.redirectguard.ui.prompt

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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

class RedirectConfirmationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourcePkg = intent.getStringExtra(EXTRA_SOURCE_PKG) ?: "Unknown"
        val targetPkg = intent.getStringExtra(EXTRA_TARGET_PKG) ?: "Unknown"
        val sourceName = intent.getStringExtra(EXTRA_SOURCE_NAME) ?: sourcePkg
        val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: targetPkg

        val sourceIcon = getAppIcon(sourcePkg)
        val targetIcon = getAppIcon(targetPkg)

        setContent {
            RedirectGuardTheme {
                ConfirmationPromptScreen(
                    sourcePkg = sourcePkg,
                    targetPkg = targetPkg,
                    sourceName = sourceName,
                    targetName = targetName,
                    sourceIcon = sourceIcon,
                    targetIcon = targetIcon,
                    onBlock = {
                        RedirectGuardAccessibilityService.instance?.returnToPreviousApp()
                        recordAction(sourcePkg, targetPkg, sourceName, targetName, RedirectAction.BLOCKED)
                        finish()
                    },
                    onAllow = {
                        RedirectGuardAccessibilityService.instance?.allowTargetTemporarily(targetPkg)
                        recordAction(sourcePkg, targetPkg, sourceName, targetName, RedirectAction.ALLOWED)
                        finish()
                    },
                    onAlwaysAllowSource = {
                        RedirectGuardApplication.instance.preferencesManager.addWhitelistedSource(sourcePkg)
                        RedirectGuardAccessibilityService.instance?.allowTargetTemporarily(targetPkg)
                        recordAction(sourcePkg, targetPkg, sourceName, targetName, RedirectAction.ALLOWED)
                        finish()
                    }
                )
            }
        }
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
        action: RedirectAction
    ) {
        val record = RedirectRecord(
            id = UUID.randomUUID().toString(),
            sourcePackage = sourcePkg,
            targetPackage = targetPkg,
            sourceAppName = sourceName,
            targetAppName = targetName,
            timestamp = System.currentTimeMillis(),
            actionTaken = action,
            triggerReason = if (action == RedirectAction.BLOCKED) "User Blocked Redirect" else "User Allowed Redirect"
        )
        RedirectGuardApplication.instance.preferencesManager.addRedirectRecord(record)
    }

    companion object {
        const val EXTRA_SOURCE_PKG = "extra_source_pkg"
        const val EXTRA_TARGET_PKG = "extra_target_pkg"
        const val EXTRA_SOURCE_NAME = "extra_source_name"
        const val EXTRA_TARGET_NAME = "extra_target_name"
    }
}

@Composable
fun ConfirmationPromptScreen(
    sourcePkg: String,
    targetPkg: String,
    sourceName: String,
    targetName: String,
    sourceIcon: Drawable?,
    targetIcon: Drawable?,
    onBlock: () -> Unit,
    onAllow: () -> Unit,
    onAlwaysAllowSource: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(15) }

    DisposableEffect(Unit) {
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Do you want to continue to $targetName or return to $sourceName?",
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
                    Text(text = "Return to Previous App ($secondsRemaining s)")
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
