package com.privacyblur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyblur.app.data.PrivacyStrength
import com.privacyblur.app.data.RevealAreaSize
import com.privacyblur.app.data.RevealDuration
import com.privacyblur.app.data.RevealMode
import com.privacyblur.app.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isEnabled by viewModel.isProtectionEnabled.collectAsState()
    val protectedApps by viewModel.protectedPackages.collectAsState()
    val revealMode by viewModel.revealMode.collectAsState()
    val revealDuration by viewModel.revealDuration.collectAsState()
    val revealAreaSize by viewModel.revealAreaSize.collectAsState()
    val privacyStrength by viewModel.privacyStrength.collectAsState()

    val hasOverlay by viewModel.hasOverlayPermission.collectAsState()
    val hasAccessibility by viewModel.hasAccessibilityPermission.collectAsState()
    val hasNotification by viewModel.hasNotificationPermission.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // App Title
        Text(
            text = "Privacy Blur",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Screen privacy overlay for sensitive apps",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Large Switch Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertiling,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy Blur",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isEnabled) "Protection Active" else "Protection Paused",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.toggleProtection(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protected Apps Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Protected Apps",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${protectedApps.size} apps configured (WhatsApp included)",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Button(
                        onClick = { viewModel.navigateTo(MainViewModel.Screen.PROTECTED_APPS) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage Apps")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reveal Mode Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reveal Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = revealMode == RevealMode.TAP,
                        onClick = { viewModel.setRevealMode(RevealMode.TAP) },
                        label = { Text("Tap to Reveal") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = revealMode == RevealMode.HOLD,
                        onClick = { viewModel.setRevealMode(RevealMode.HOLD) },
                        label = { Text("Hold to Reveal") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (revealMode == RevealMode.TAP) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reveal Duration",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RevealDuration.values().forEach { dur ->
                            FilterChip(
                                selected = revealDuration == dur,
                                onClick = { viewModel.setRevealDuration(dur) },
                                label = { Text(dur.label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Reveal Area Size",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RevealAreaSize.values().forEach { size ->
                        FilterChip(
                            selected = revealAreaSize == size,
                            onClick = { viewModel.setRevealAreaSize(size) },
                            label = { Text(size.label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Strength Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Privacy Strength",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Current: ${privacyStrength.label} (${privacyStrength.description})",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrivacyStrength.values().forEach { str ->
                        FilterChip(
                            selected = privacyStrength == str,
                            onClick = { viewModel.setPrivacyStrength(str) },
                            label = { Text(str.label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    title = "Overlay Permission",
                    isGranted = hasOverlay,
                    onGrantClick = { viewModel.requestOverlayPermission() }
                )
                PermissionRow(
                    title = "Accessibility Permission",
                    isGranted = hasAccessibility,
                    onGrantClick = { viewModel.requestAccessibilityPermission() }
                )
                PermissionRow(
                    title = "Notification Permission",
                    isGranted = hasNotification,
                    onGrantClick = { viewModel.refreshPermissionStatus() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Privacy Assurance Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Privacy Blur does not read or store your messages.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isGranted) FontWeight.Normal else FontWeight.SemiBold
                )
            )
        }
        if (!isGranted) {
            TextButton(onClick = onGrantClick) {
                Text("Enable")
            }
        }
    }
}
