package com.example.stepwalker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stepwalker.AppTheme
import com.example.stepwalker.WalkState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds

@Composable
fun MapScreen(
    state: WalkState,
    theme: AppTheme,
    onBack: () -> Unit,
    onExportGpx: () -> Unit
) {
    val points = remember(state.route) {
        state.route.map { (lat, lon) -> LatLng(lat, lon) }
    }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(points.size) {
        if (points.isEmpty()) return@LaunchedEffect

        if (points.size == 1) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(points.first(), 16f)
            )
        } else {
            val builder = LatLngBounds.Builder()
            points.forEach(builder::include)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(builder.build(), 80)
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isBuildingEnabled = true,
                isTrafficEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            if (points.isNotEmpty()) {
                Polyline(
                    points = points,
                    color = theme.accent,
                    width = 12f
                )

                Marker(
                    state = MarkerState(position = points.first()),
                    title = "Старт"
                )

                if (points.size > 1) {
                    Marker(
                        state = MarkerState(position = points.last()),
                        title = "Текущая позиция"
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Card {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }

            Button(
                onClick = onExportGpx,
                enabled = points.size >= 2
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Экспорт GPX")
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (points.isEmpty()) "Маршрут пока пуст" else "Точек маршрута: ${points.size}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.tracking) "Трекинг активен" else "Трекинг остановлен",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
