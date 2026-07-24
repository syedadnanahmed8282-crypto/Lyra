package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.service.MusicService
import com.example.ui.theme.DarkPurpleSurface
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.FrostedMiniPlayerBg
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerDialog(
    info: MusicService.EqualizerInfo,
    onPresetSelected: (Short) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onBassBoostChanged: (Short) -> Unit,
    onVirtualizerChanged: (Short) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("equalizer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FrostedMiniPlayerBg),
            border = BorderStroke(1.dp, FrostedGlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = LavenderPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Audio Equalizer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets row
                if (info.presets.isNotEmpty()) {
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(info.presets) { index, presetName ->
                            val isSelected = info.currentPreset.toInt() == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPresetSelected(index.toShort()) },
                                label = { Text(presetName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LavenderPrimary,
                                    selectedLabelColor = DarkPurpleSurface,
                                    containerColor = FrostedGlassCard,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Frequency Bands Sliders
                if (info.numberOfBands > 0) {
                    Text(
                        text = "Frequency Bands",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    for (b in 0 until info.numberOfBands) {
                        val bandIndex = b.toShort()
                        val freqHz = if (b < info.centerFreqs.size) info.centerFreqs[b] else 0
                        val currentLevel = if (b < info.bandLevels.size) info.bandLevels[b] else 0.toShort()

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${currentLevel / 100} dB",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LavenderPrimary
                                )
                            }

                            Slider(
                                value = currentLevel.toFloat(),
                                onValueChange = { newValue ->
                                    onBandLevelChanged(bandIndex, newValue.toInt().toShort())
                                },
                                valueRange = info.minLevel.toFloat()..info.maxLevel.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = LavenderPrimary,
                                    activeTrackColor = LavenderPrimary,
                                    inactiveTrackColor = SurfaceCard
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Bass Boost & 3D Virtualizer Sliders
                Text(
                    text = "Effects",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Boost", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${info.bassStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = NeonPink)
                    }
                    Slider(
                        value = info.bassStrength.toFloat(),
                        onValueChange = { onBassBoostChanged(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonPink,
                            inactiveTrackColor = SurfaceCard
                        )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3D Virtualizer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${info.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = NeonPurple)
                    }
                    Slider(
                        value = info.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChanged(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPurple,
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = SurfaceCard
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
