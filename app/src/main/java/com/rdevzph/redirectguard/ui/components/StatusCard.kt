package com.rdevzph.redirectguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusFg,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
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
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (actionButtonText != null && onActionClick != null) {
                        Button(
                            onClick = onActionClick,
                            shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
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
