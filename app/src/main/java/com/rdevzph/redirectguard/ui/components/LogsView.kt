package com.rdevzph.redirectguard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.model.RedirectRecord
import com.rdevzph.redirectguard.ui.theme.StatusActive
import com.rdevzph.redirectguard.ui.theme.StatusActiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusInactive
import com.rdevzph.redirectguard.ui.theme.StatusInactiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusWarning
import com.rdevzph.redirectguard.ui.theme.StatusWarningContainer
import com.rdevzph.redirectguard.util.AppLogEntry
import com.rdevzph.redirectguard.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface DisplayLogItem {
    val timestamp: Long

    data class Redirect(val record: RedirectRecord) : DisplayLogItem {
        override val timestamp: Long get() = record.timestamp
    }

    data class AppLog(val entry: AppLogEntry) : DisplayLogItem {
        override val timestamp: Long get() = entry.timestamp
    }
}

@Composable
fun LogsView(
    redirectRecords: List<RedirectRecord>,
    appLogs: List<AppLogEntry>,
    showRedirects: Boolean,
    showDebug: Boolean,
    showErrors: Boolean,
    showWarnings: Boolean,
    onToggleRedirects: (Boolean) -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    onToggleErrors: (Boolean) -> Unit,
    onToggleWarnings: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val allItems = remember(redirectRecords, appLogs, showRedirects, showDebug, showErrors, showWarnings, searchQuery) {
        val list = mutableListOf<DisplayLogItem>()

        if (showRedirects) {
            redirectRecords.forEach { record ->
                if (searchQuery.isBlank() ||
                    record.sourceAppName.contains(searchQuery, ignoreCase = true) ||
                    record.targetAppName.contains(searchQuery, ignoreCase = true) ||
                    record.deepLinkUri?.contains(searchQuery, ignoreCase = true) == true ||
                    record.triggerReason.contains(searchQuery, ignoreCase = true)
                ) {
                    list.add(DisplayLogItem.Redirect(record))
                }
            }
        }

        appLogs.forEach { entry ->
            val matchesLevel = when (entry.level) {
                LogLevel.DEBUG -> showDebug
                LogLevel.ERROR -> showErrors
                LogLevel.WARN -> showWarnings
                LogLevel.INFO -> true
            }

            if (matchesLevel) {
                if (searchQuery.isBlank() ||
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                    entry.message.contains(searchQuery, ignoreCase = true)
                ) {
                    list.add(DisplayLogItem.AppLog(entry))
                }
            }
        }

        list.sortedByDescending { it.timestamp }
    }

    val redirectCount = if (showRedirects) redirectRecords.size else 0
    val debugCount = if (showDebug) appLogs.count { it.level == LogLevel.DEBUG } else 0
    val errorCount = if (showErrors) appLogs.count { it.level == LogLevel.ERROR } else 0
    val warnCount = if (showWarnings) appLogs.count { it.level == LogLevel.WARN } else 0

    fun copyLogsToClipboard() {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("=== RedirectGuard Logs (Generated: ${timeFormat.format(Date())}) ===\n\n")

        allItems.forEach { item ->
            when (item) {
                is DisplayLogItem.Redirect -> {
                    val r = item.record
                    sb.append("[${timeFormat.format(Date(r.timestamp))}] [REDIRECT] ${r.sourceAppName} (${r.sourcePackage}) -> ${r.targetAppName} (${r.targetPackage}) | Action: ${r.actionTaken} | Reason: ${r.triggerReason}\n")
                    if (!r.deepLinkUri.isNullOrBlank()) {
                        sb.append("   URI: ${r.deepLinkUri}\n")
                    }
                }
                is DisplayLogItem.AppLog -> {
                    val e = item.entry
                    sb.append("[${timeFormat.format(Date(e.timestamp))}] [${e.level}] [${e.tag}] ${e.message}\n")
                    if (e.throwable != null) {
                        sb.append("   ${e.throwable.stackTraceToString()}\n")
                    }
                }
            }
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("RedirectGuard Logs", sb.toString()))
        Toast.makeText(context, "Copied ${allItems.size} logs to clipboard", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Activity & Debug Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Showing ${allItems.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { copyLogsToClipboard() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Copy", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onClearAll,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Clear", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Filter logs by text, package, or URL...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter toggles row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showRedirects,
                            onClick = { onToggleRedirects(!showRedirects) },
                            label = { Text("Redirects ($redirectCount)") },
                            leadingIcon = if (showRedirects) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        FilterChip(
                            selected = showDebug,
                            onClick = { onToggleDebug(!showDebug) },
                            label = { Text("Debug Log.d ($debugCount)") },
                            leadingIcon = if (showDebug) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )

                        FilterChip(
                            selected = showErrors,
                            onClick = { onToggleErrors(!showErrors) },
                            label = { Text("Errors Log.e ($errorCount)") },
                            leadingIcon = if (showErrors) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )

                        FilterChip(
                            selected = showWarnings,
                            onClick = { onToggleWarnings(!showWarnings) },
                            label = { Text("Warnings Log.w ($warnCount)") },
                            leadingIcon = if (showWarnings) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusWarningContainer,
                                selectedLabelColor = StatusWarning
                            )
                        )
                    }
                }
            }
        }

        if (allItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching logs found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try adjusting filter toggles or trigger app redirects to see logs appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = allItems,
                key = { item ->
                    when (item) {
                        is DisplayLogItem.Redirect -> "redirect_${item.record.id}_${item.record.timestamp}"
                        is DisplayLogItem.AppLog -> "applog_${item.entry.id}"
                    }
                }
            ) { item ->
                when (item) {
                    is DisplayLogItem.Redirect -> {
                        RedirectLogCard(
                            record = item.record,
                            onCopy = {
                                val text = "Redirect: ${item.record.sourceAppName} -> ${item.record.targetAppName}\nReason: ${item.record.triggerReason}\nLink: ${item.record.deepLinkUri ?: "none"}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Redirect Record", text))
                                Toast.makeText(context, "Copied redirect details", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    is DisplayLogItem.AppLog -> {
                        AppLogCard(
                            entry = item.entry,
                            onCopy = {
                                val text = "[${item.entry.level}] ${item.entry.tag}: ${item.entry.message}${item.entry.throwable?.let { "\n" + it.stackTraceToString() } ?: ""}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("App Log", text))
                                Toast.makeText(context, "Copied log line", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedirectLogCard(
    record: RedirectRecord,
    onCopy: () -> Unit
) {
    val timeString = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
        .format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "REDIRECT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.weight(1f))

                val (badgeBg, badgeFg, label) = when (record.actionTaken) {
                    RedirectAction.BLOCKED -> Triple(StatusInactiveContainer, StatusInactive, "Blocked")
                    RedirectAction.ALLOWED -> Triple(StatusActiveContainer, StatusActive, "Allowed")
                    RedirectAction.PROMPTED -> Triple(StatusWarningContainer, StatusWarning, "Prompted")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Text(
                text = record.triggerReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!record.deepLinkUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Link: ${record.deepLinkUri}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppLogCard(
    entry: AppLogEntry,
    onCopy: () -> Unit
) {
    var expandedStackTrace by remember { mutableStateOf(false) }

    val timeString = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        .format(Date(entry.timestamp))

    val (badgeBg, badgeFg, label) = when (entry.level) {
        LogLevel.DEBUG -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary, "Log.d")
        LogLevel.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Log.e")
        LogLevel.WARN -> Triple(StatusWarningContainer, StatusWarning, "Log.w")
        LogLevel.INFO -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Log.i")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            if (entry.level == LogLevel.ERROR) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.level == LogLevel.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontFamily = FontFamily.Monospace
            )

            if (entry.throwable != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedStackTrace = !expandedStackTrace }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (expandedStackTrace) "Hide Stacktrace" else "Show Stacktrace (${entry.throwable.localizedMessage ?: "Error"})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        AnimatedVisibility(visible = expandedStackTrace) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.throwable.stackTraceToString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
