package com.example.stepwalker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepwalker.AppTheme
import com.example.stepwalker.data.WalkEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    theme: AppTheme,
    history: List<WalkEntity>,
    onClose: () -> Unit
) {
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
            .padding(24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "История",
                color = theme.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClose) {
                Text("Назад", color = theme.textPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            Text("Прогулок пока нет", color = theme.textSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { walk ->
                    Column(
                        Modifier.fillMaxWidth()
                            .background(theme.cardBg, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            fmt.format(Date(walk.startedAt)),
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${walk.steps} шагов · %.2f км".format(walk.distanceKm),
                            color = theme.textSecondary
                        )
                        Text(
                            "${walk.calories} ккал · %.1f км/ч".format(walk.avgSpeedKmh),
                            color = theme.textSecondary
                        )
                    }
                }
            }
        }
    }
}
