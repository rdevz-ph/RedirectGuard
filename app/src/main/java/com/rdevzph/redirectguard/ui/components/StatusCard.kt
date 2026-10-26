package com.rdevzph.redirectguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdevzph.redirectguard.ui.theme.StatusActive
import com.rdevzph.redirectguard.ui.theme.StatusActiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusWarning
import com.rdevzph.redirectguard.ui.theme.StatusWarningContainer

@Composable
fun StatusCard(
    icon: ImageVector,
    title: String,
    statusText: String,
    isSuccess: Boolean,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    tertiaryButtonText: String? = null,
    onTertiaryActionClick: (() -> Unit)? = null,
    statusBgOverride: Color? = null,
    statusFgOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val statusBg = statusBgOverride ?: if (isSuccess) StatusActiveContainer else StatusWarningContainer
    val statusFg = statusFgOverride ?: if (isSuccess) StatusActive else StatusWarning

    Card(
        modifier = modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusFg,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = statusBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if ((actionButtonText != null && onActionClick != null) ||
                (secondaryButtonText != null && onSecondaryActionClick != null)
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (actionButtonText != null && onActionClick != null) {
                        Button(
                            onClick = onActionClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = actionButtonText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (secondaryButtonText != null && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = secondaryButtonText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            if (tertiaryButtonText != null && onTertiaryActionClick != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onTertiaryActionClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tertiaryButtonText,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
