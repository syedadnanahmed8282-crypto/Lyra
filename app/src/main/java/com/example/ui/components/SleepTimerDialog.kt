package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkPurpleSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.FrostedMiniPlayerBg
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SleepTimerDialog(
    activeTimerMs: Long?,
    onSetTimer: (minutes: Int, endOfTrack: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(15, 30, 45, 60, 90)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sleep_timer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FrostedMiniPlayerBg),
            border = BorderStroke(1.dp, FrostedGlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = LavenderPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTimerMs != null) {
                    val statusText = if (activeTimerMs == -1L) {
                        "Music will pause at the end of the current track."
                    } else {
                        val minutes = (activeTimerMs / 1000) / 60
                        val seconds = (activeTimerMs / 1000) % 60
                        String.format("Timer active: %02d:%02d remaining", minutes, seconds)
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Active Timer", color = ErrorRed)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = "Auto-pause playback after specified time:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.take(3).forEach { mins ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onSetTimer(mins, false)
                                    onDismiss()
                                },
                                label = { Text("$mins min", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = FrostedGlassCard,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.drop(3).forEach { mins ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onSetTimer(mins, false)
                                    onDismiss()
                                },
                                label = { Text("$mins min", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = FrostedGlassCard,
                                    labelColor = TextPrimary
                                )
                            )
                        }

                        FilterChip(
                            selected = false,
                            onClick = {
                                onSetTimer(0, true)
                                onDismiss()
                            },
                            label = { Text("End of Track", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = FrostedGlassCard,
                                labelColor = LavenderPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        }
    }
}
