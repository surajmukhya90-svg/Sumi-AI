package com.sumi.ai.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class GirlfriendMood {
    NORMAL,         // Pyaari smile
    ANGRY_JEALOUS,  // Laal rang (Red), gussa, ignore karna
    ROMANTIC,       // Phool girana, romantic song, meethi baatein
    UPSET_IGNORE    // Naraz hokar baat na karna
}

object SumiMoodState {
    private val _currentMood = MutableStateFlow(GirlfriendMood.NORMAL)
    val currentMood: StateFlow<GirlfriendMood> = _currentMood

    var ignoreEndTime = 0L

    fun setMood(mood: GirlfriendMood) {
        _currentMood.value = mood
        if (mood == GirlfriendMood.ANGRY_JEALOUS || mood == GirlfriendMood.UPSET_IGNORE) {
            // 20 second tak ignore karegi
            ignoreEndTime = System.currentTimeMillis() + 20000
        }
    }

    fun isIgnoring(): Boolean {
        return System.currentTimeMillis() < ignoreEndTime
    }
}
