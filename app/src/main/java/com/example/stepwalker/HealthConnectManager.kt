package com.example.stepwalker

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import java.time.Instant

class HealthConnectManager(context: Context) {
    private val client = HealthConnectClient.getOrCreate(context)

    val readStepsPermission =
        HealthPermission.getReadPermission(StepsRecord::class)

    suspend fun readSteps(start: Instant, end: Instant): Long {
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
