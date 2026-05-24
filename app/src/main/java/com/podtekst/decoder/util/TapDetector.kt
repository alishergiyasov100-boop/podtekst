package com.podtekst.decoder.util

import android.os.SystemClock

/**
 * Считает 4 быстрых тапа в окне [windowMs] на одной "цели" (id строки).
 * Не привязан к UI-потоку; вызывайте onTap из любого потока.
 */
class TapDetector(
    private val requiredTaps: Int = 4,
    private val windowMs: Long = 1500L,
) {
    private var lastTargetId: String? = null
    private var count: Int = 0
    private var firstTapAt: Long = 0L

    /** Возвращает true ровно один раз, когда набралось [requiredTaps] на одной цели в окне. */
    @Synchronized
    fun onTap(targetId: String): Boolean {
        val now = SystemClock.uptimeMillis()
        if (targetId != lastTargetId || now - firstTapAt > windowMs) {
            lastTargetId = targetId
            count = 1
            firstTapAt = now
            return false
        }
        count++
        if (count >= requiredTaps) {
            reset()
            return true
        }
        return false
    }

    @Synchronized
    fun reset() {
        lastTargetId = null
        count = 0
        firstTapAt = 0L
    }
}
