package com.example.stepwalker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TrackingStateStore {
    private val _state = MutableStateFlow(WalkState())
    val state: StateFlow<WalkState> = _state.asStateFlow()

    fun set(value: WalkState) { _state.value = value }
    fun update(block: (WalkState) -> WalkState) { _state.value = block(_state.value) }
}
