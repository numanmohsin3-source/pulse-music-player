package com.pulsemusic.player.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.dsp.EqPreset
import com.pulsemusic.player.dsp.EqSettings
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulsePink
import com.pulsemusic.player.ui.theme.PulseSurface
import com.pulsemusic.player.ui.theme.PulseSurfaceAlt
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    settings: EqSettings,
    isPlaying: Boolean,
    isProDspActive: Boolean,
    onToggleDsp: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBassChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onCompressorToggle: (Boolean) -> Unit,
    onLimiterToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Master DSP Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PulseSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Studio DSP Equalizer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isProDspActive) {
                        "10-Band Dynamics + Multi-Band Compressor Active"
                    } else {
                        "TikTok Mastering Sound Engine Ready"
                    },
                    color = PulseCyan,
                    fontSize = 11.5.sp
                )
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = onToggleDsp,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = PulseCyan
                )
            )
        }

        // Animated Spectrum Bar Card
        SpectrumVisualizerCard(
            bandsDb = settings.bandsDb,
            isActive = settings.enabled && isPlaying
        )

        // Presets Row
        Column {
            Text(
                text = "Sound Profiles",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqPreset.entries.forEach { preset ->
                    val selected = settings.preset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset) },
                        label = {
                            Text(
                                text = preset.displayName,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (preset == EqPreset.TIKTOK_PUNCH) PulsePink else PulseCyan,
                            selectedLabelColor = if (preset == EqPreset.TIKTOK_PUNCH) Color.White else Color.Black,
                            containerColor = PulseSurface,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }

        // 10-Band Vertical Graphic Equalizer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PulseSurface)
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("10-Band Equalizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("+12dB / -12dB", color = Color(0xFF64748B), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EqSettings.BAND_LABELS.forEachIndexed { idx, freqLabel ->
                    val gain = settings.bandsDb.getOrElse(idx) { 0f }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${if (gain > 0f) "+" else ""}${gain.roundToInt()}",
                            color = if (gain >= 0f) PulseCyan else PulsePink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        VerticalEqSlider(
                            value = gain,
                            enabled = settings.enabled,
                            onValueChange = { onBandChange(idx, it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = freqLabel,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // TikTok Punch & Mastering FX Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PulseSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TikTok Mastering & Surround FX",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            DspSliderRow(
                label = "Deep Bass Punch",
                valueText = "${settings.bassBoostStrength / 10}%",
                value = settings.bassBoostStrength.toFloat(),
                range = 0f..1000f,
                enabled = settings.enabled,
                accentColor = PulsePink,
                onValueChange = { onBassChange(it.roundToInt()) }
            )

            DspSliderRow(
                label = "Loudness & Punch Gain",
                valueText = "+${String.format(Locale.US, "%.1f", settings.loudnessGainMb / 100f)} dB",
                value = settings.loudnessGainMb.toFloat(),
                range = 0f..1000f,
                enabled = settings.enabled,
                accentColor = PulseCyan,
                onValueChange = { onLoudnessChange(it.roundToInt()) }
            )

            DspSliderRow(
                label = "3D Stereo Surround",
                valueText = "${settings.virtualizerStrength / 10}%",
                value = settings.virtualizerStrength.toFloat(),
                range = 0f..1000f,
                enabled = settings.enabled,
                accentColor = Color(0xFFA855F7),
                onValueChange = { onVirtualizerChange(it.roundToInt()) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Multi-Band Compressor", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Text("Balances tight bass with crystal-clear vocals", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = settings.compressorEnabled,
                    enabled = settings.enabled,
                    onCheckedChange = onCompressorToggle
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Zero-Clip Peak Limiter", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Text("Prevents speaker distortion at high volume", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = settings.limiterEnabled,
                    enabled = settings.enabled,
                    onCheckedChange = onLimiterToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(68.dp))
    }
}

@Composable
private fun VerticalEqSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = -12f..12f,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = PulseCyan,
            activeTrackColor = PulsePink,
            inactiveTrackColor = PulseSurfaceAlt
        ),
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width / 2 - placeable.height / 2),
                        y = -(placeable.height / 2 - placeable.width / 2)
                    )
                }
            }
            .width(150.dp)
            .height(28.dp)
    )
}

@Composable
private fun DspSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(valueText, color = accentColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = PulseSurfaceAlt
            )
        )
    }
}

@Composable
private fun SpectrumVisualizerCard(
    bandsDb: List<Float>,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF17132B), Color(0xFF0F172A))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 20) {
            val eqBandGain = bandsDb.getOrElse(i / 2) { 0f }
            val normalizedEq = ((eqBandGain + 12f) / 24f).coerceIn(0.15f, 1f)
            val animWave = if (isActive) {
                (0.45f + 0.55f * ((sin(phase * 2f + i * 0.65f) + 1f) / 2f))
            } else {
                0.35f
            }
            val barFraction = (normalizedEq * animWave).coerceIn(0.12f, 1f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((50f * barFraction).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(PulseCyan, PulsePink)
                        )
                    )
            )
        }
    }
}
