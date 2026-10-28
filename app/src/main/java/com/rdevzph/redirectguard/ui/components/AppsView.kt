package com.rdevzph.redirectguard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.rdevzph.redirectguard.model.GuardedApp
import com.rdevzph.redirectguard.ui.InstalledAppItem
import com.rdevzph.redirectguard.ui.theme.StatusActive
import com.rdevzph.redirectguard.ui.theme.StatusActiveContainer
import com.rdevzph.redirectguard.ui.theme.StatusWarning
import com.rdevzph.redirectguard.ui.theme.StatusWarningContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppsFilterTab {
    ALL,
    GUARDED,
    WHITELISTED,
    PRESETS,
    USER,
    SYSTEM
}

@Composable
fun AppsView(
    installedApps: List<InstalledAppItem>,
    guardedPackages: Set<String>,
    whitelistedPackages: Set<String>,
    whitelistSystemApps: Boolean,
    guardAllExternalApps: Boolean,
    onToggleWhitelistSystemApps: (Boolean) -> Unit,
    onToggleGuardAll: (Boolean) -> Unit,
    onToggleGuarded: (packageName: String, guarded: Boolean) -> Unit,
    onToggleWhitelisted: (packageName: String, whitelisted: Boolean) -> Unit,
    isPackageWhitelisted: (packageName: String) -> Boolean,
    isPermanentWhitelisted: (packageName: String) -> Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppsFilterTab.ALL) }

    val guardedCount = installedApps.count { guardedPackages.contains(it.packageName) }
    val whitelistedCount = installedApps.count { isPackageWhitelisted(it.packageName) }
    val presetCount = installedApps.count { app -> GuardedApp.DEFAULT_SHOPPING_PRESETS.any { it.packageName == app.packageName } }
    val userCount = installedApps.count { !it.isSystemApp }
    val systemCount = installedApps.count { it.isSystemApp }

    val filteredApps = remember(installedApps, searchQuery, selectedFilter, guardedPackages, whitelistedPackages) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                AppsFilterTab.ALL -> true
                AppsFilterTab.GUARDED -> guardedPackages.contains(app.packageName)
                AppsFilterTab.WHITELISTED -> isPackageWhitelisted(app.packageName)
                AppsFilterTab.PRESETS -> GuardedApp.DEFAULT_SHOPPING_PRESETS.any { it.packageName == app.packageName }
                AppsFilterTab.USER -> !app.isSystemApp
                AppsFilterTab.SYSTEM -> app.isSystemApp
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search installed applications") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == AppsFilterTab.ALL,
                onClick = { selectedFilter = AppsFilterTab.ALL },
                label = { Text("All (${installedApps.size})") }
            )
            FilterChip(
                selected = selectedFilter == AppsFilterTab.GUARDED,
                onClick = { selectedFilter = AppsFilterTab.GUARDED },
                label = { Text("Guarded ($guardedCount)") }
            )
            FilterChip(
                selected = selectedFilter == AppsFilterTab.WHITELISTED,
                onClick = { selectedFilter = AppsFilterTab.WHITELISTED },
                label = { Text("Whitelisted ($whitelistedCount)") }
            )
            FilterChip(
                selected = selectedFilter == AppsFilterTab.PRESETS,
                onClick = { selectedFilter = AppsFilterTab.PRESETS },
                label = { Text("Shopping Presets ($presetCount)") }
            )
            FilterChip(
                selected = selectedFilter == AppsFilterTab.USER,
                onClick = { selectedFilter = AppsFilterTab.USER },
                label = { Text("User Apps ($userCount)") }
            )
            FilterChip(
                selected = selectedFilter == AppsFilterTab.SYSTEM,
                onClick = { selectedFilter = AppsFilterTab.SYSTEM },
                label = { Text("System ($systemCount)") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System & Guard All Protection Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                            text = "Exempt built-in apps and panels from redirect protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = whitelistSystemApps,
                        onCheckedChange = onToggleWhitelistSystemApps
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Guard All External Apps toggle
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
                            text = "Intercept transitions to all user-installed applications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = guardAllExternalApps,
                        onCheckedChange = onToggleGuardAll
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading installed applications...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No applications found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No apps match \"$searchQuery\"" else "No applications match the selected filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Application Protection Rules",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Guarded apps trigger redirect protection when opened. Whitelisted apps are completely exempt from interception and cannot be selected for the guard list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(filteredApps, key = { it.packageName }) { app ->
                    val isGuarded = guardedPackages.contains(app.packageName)
                    val isWhitelisted = isPackageWhitelisted(app.packageName)
                    val isPermanent = isPermanentWhitelisted(app.packageName)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isWhitelisted) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else if (isGuarded) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            }
                        )
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
                                AppIconImage(
                                    packageName = app.packageName,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isWhitelisted) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = StatusActiveContainer
                                            ) {
                                                Text(
                                                    text = if (isPermanent) "Permanent Whitelist" else "Whitelisted",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusActive,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (isGuarded) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = StatusWarningContainer
                                            ) {
                                                Text(
                                                    text = "Guarded",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusWarning,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        val isShoppingPreset = remember(app.packageName) {
                                            GuardedApp.DEFAULT_SHOPPING_PRESETS.any { it.packageName == app.packageName }
                                        }
                                        if (isShoppingPreset) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "Shopping Preset",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (app.isSystemApp) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "System",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isWhitelisted) {
                                    Text(
                                        text = "Whitelisted (exempt from guard)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                FilterChip(
                                    selected = isGuarded,
                                    onClick = { onToggleGuarded(app.packageName, !isGuarded) },
                                    enabled = !isWhitelisted,
                                    label = { Text("Guard") },
                                    leadingIcon = {
                                        if (isGuarded) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                FilterChip(
                                    selected = isWhitelisted,
                                    onClick = { onToggleWhitelisted(app.packageName, !isWhitelisted) },
                                    enabled = !isPermanent,
                                    label = { Text("Whitelist") },
                                    leadingIcon = {
                                        if (isWhitelisted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconImage(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(96, 96).asImageBitmap()
                iconBitmap = bitmap
            } catch (_: Exception) {
                iconBitmap = null
            }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
