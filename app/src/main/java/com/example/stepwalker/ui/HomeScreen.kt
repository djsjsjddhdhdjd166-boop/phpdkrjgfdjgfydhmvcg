package com.example.stepwalker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepwalker.AppTheme
import com.example.stepwalker.WalkState

@Composable
fun HomeScreen(
    state: WalkState,
    theme: AppTheme,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onTheme: () -> Unit,
    onHistory: () -> Unit,
    onMap: () -> Unit,
    onExportGpx: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(theme.bgTop, theme.bgBottom)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Сегодня",
                        color = theme.textSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        theme.displayName,
                        color = theme.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onHistory) {
                        Text("История", color = theme.textPrimary)
                    }
                    TextButton(onClick = onMap) {
                        Text("Карта", color = theme.textPrimary)
                    }
                    IconButton(onClick = onTheme) {
                        Text(theme.emoji, fontSize = 28.sp)
                    }
                }
            }

            Spacer(Modifier.height(38.dp))

            Text(
                "${state.steps}",
                color = theme.textPrimary,
                fontSize = 86.sp,
                fontWeight = FontWeight.Thin
            )

            Text(
                "шагов",
                color = theme.textSecondary,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(30.dp))

            ProgressRing(
                progress = (state.steps / 10000f).coerceIn(0f, 1f),
                accent = theme.accent,
                textPrimary = theme.textPrimary,
                textSecondary = theme.textSecondary
            )

            Spacer(Modifier.height(30.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    "Дистанция",
                    "%.2f км".format(state.distanceKm),
                    theme,
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Калории",
                    "${state.calories} ккал",
                    theme,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    "Скорость",
                    "%.1f км/ч".format(state.speedKmh),
                    theme,
                    Modifier.weight(1f)
                )

                val gpsText = if (state.lat != null && state.lon != null) {
                    "%.4f, %.4f".format(state.lat, state.lon)
                } else {
                    "нет сигнала"
                }

                MetricCard(
                    "GPS",
                    gpsText,
                    theme,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            val accuracyText = buildString {
                append(state.accuracyM?.let { "GPS: ±%.1f м".format(it) } ?: "GPS: ожидание")
                state.satellites?.let { append(" · спутников: $it") }
                state.speedAccuracyMps?.let { append(" · V±%.1f м/с".format(it)) }
            }
            Text(
                accuracyText,
                color = theme.textSecondary,
                fontSize = 12.sp
            )

            if (!state.stepSensorAvailable) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Аппаратный шагомер не найден — точный подсчёт шагов недоступен",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onMap,
                enabled = state.route.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.cardBg,
                    contentColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.route.size >= 2) "Открыть карту маршрута" else "Карта появится после GPS-точек")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onExportGpx,
                enabled = state.route.size >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = theme.textPrimary
                )
            ) {
                Text("Экспорт GPX")
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (state.tracking) onStop() else onStart()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accent,
                        contentColor = theme.bgTop
                    ),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (state.tracking) Icons.Default.Stop
                        else Icons.Default.PlayArrow,
                        contentDescription = if (state.tracking) "Стоп" else "Старт",
                        modifier = Modifier.size(32.dp)
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.textPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Сброс",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        label = "ring"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp)
    ) {
        CircularProgressIndicator(
            progress = { animated },
            color = accent,
            trackColor = accent.copy(alpha = 0.15f),
            strokeWidth = 14.dp,
            modifier = Modifier.fillMaxSize()
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animated * 100).toInt()}%",
                color = textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                "цель 10 000",
                color = textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(theme.cardBg)
            .padding(16.dp)
    ) {
        Text(
            label,
            color = theme.textSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = theme.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
