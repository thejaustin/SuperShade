package com.supershade.domain.notification

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class NotificationInteractionType(val scoreDelta: Float) {
    CLICK(2.5f),
    REPLY(4.0f),
    PIN(5.0f),
    UNPIN(-1.0f),
    SNOOZE(0.8f),
    FAST_DISMISS(-1.8f),
    DISMISS(-0.6f),
    CLEAR_ALL(-0.3f),
}

data class InteractionEvent(
    val packageName: String,
    val channelId: String?,
    val type: NotificationInteractionType,
    val timestamp: Long = System.currentTimeMillis(),
)

data class AppAffinityScore(
    val packageName: String,
    val score: Float,
    val interactionCount: Int,
    val lastUpdated: Long,
) {
    val isPromoted: Boolean get() = score >= 5.0f && interactionCount >= 3
    val isDemotedToSilent: Boolean get() = score <= -5.0f && interactionCount >= 4
}

/**
 * Anthropic-style autonomous evaluation-feedback loop for notification priority.
 *
 * Rather than static rule sets, the loop:
 * 1. Observes user actions (taps, quick dismisses, replies, snoozes).
 * 2. Evaluates statistical weights in a periodic background loop.
 * 3. Self-corrects affinity scores and dynamically adjusts classification.
 * 4. Feeds directly into CategoryEngine and UI priority re-ranking.
 */
class AdaptivePriorityLoop(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val interactionChannel = MutableSharedFlow<InteractionEvent>(extraBufferCapacity = 64)
    private val _scores = MutableStateFlow<Map<String, AppAffinityScore>>(emptyMap())
    val scores: StateFlow<Map<String, AppAffinityScore>> = _scores.asStateFlow()

    private val localScores = ConcurrentHashMap<String, Float>()
    private val interactionCounts = ConcurrentHashMap<String, Int>()

    init {
        loadPersistedScores()
        startFeedbackLoop()
    }

    fun recordInteraction(
        packageName: String,
        channelId: String?,
        type: NotificationInteractionType,
    ) {
        interactionChannel.tryEmit(InteractionEvent(packageName, channelId, type))
    }

    fun getAffinity(packageName: String): Float = localScores[packageName] ?: 0f

    fun isAutoSilent(packageName: String): Boolean {
        val score = localScores[packageName] ?: 0f
        val count = interactionCounts[packageName] ?: 0
        return score <= -5.0f && count >= 4
    }

    private fun startFeedbackLoop() {
        // Stream processor for real-time interaction feedback
        scope.launch {
            interactionChannel.collect { event ->
                val current = localScores[event.packageName] ?: 0f
                val count = interactionCounts[event.packageName] ?: 0
                val decay = 0.98f // gentle historical decay
                val newScore = (current * decay + event.type.scoreDelta).coerceIn(-15f, 15f)
                localScores[event.packageName] = newScore
                interactionCounts[event.packageName] = count + 1

                publishCurrentState()
            }
        }

        // Periodic evaluation loop: normalizes scores and commits snapshot every 60s
        scope.launch {
            while (isActive) {
                delay(60_000L)
                evaluateAndPersistLoop()
            }
        }
    }

    private fun publishCurrentState() {
        val map = localScores.mapValues { (pkg, score) ->
            AppAffinityScore(
                packageName = pkg,
                score = score,
                interactionCount = interactionCounts[pkg] ?: 0,
                lastUpdated = System.currentTimeMillis(),
            )
        }
        _scores.value = map
    }

    private fun evaluateAndPersistLoop() {
        publishCurrentState()
        try {
            val prefs = context.getSharedPreferences("supershade_adaptive_loop", Context.MODE_PRIVATE)
            val json = JSONObject()
            localScores.forEach { (pkg, score) ->
                val count = interactionCounts[pkg] ?: 0
                json.put(pkg, JSONObject().apply {
                    put("score", score.toDouble())
                    put("count", count)
                })
            }
            prefs.edit().putString("affinity_json", json.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadPersistedScores() {
        try {
            val prefs = context.getSharedPreferences("supershade_adaptive_loop", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("affinity_json", null) ?: return
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val obj = json.getJSONObject(pkg)
                localScores[pkg] = obj.optDouble("score", 0.0).toFloat()
                interactionCounts[pkg] = obj.optInt("count", 0)
            }
            publishCurrentState()
        } catch (_: Exception) {}
    }
}
