package com.podtekst.decoder.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.podtekst.decoder.util.TapDetector

/**
 * Слушает тапы по любой текстовой ноде. 4 тапа в окне 1500мс на одной ноде =>
 * запуск OverlayService с расшифровкой подтекста.
 */
class PodtekstAccessibilityService : AccessibilityService() {

    private val tapDetector = TapDetector(requiredTaps = 4, windowMs = 1500L)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "PodtekstAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) return

        val src = event.source ?: return
        val targetId = nodeIdentity(src)
        val text = extractText(src)
        if (text.isNullOrBlank()) {
            src.recycle()
            return
        }

        val fired = tapDetector.onTap(targetId)
        if (!fired) {
            src.recycle()
            return
        }

        val context = collectConversationContext(src)
        val pkg = event.packageName?.toString() ?: "?"

        Log.d(TAG, "4-tap fired pkg=$pkg target='$targetId' textLen=${text.length} ctxLines=${context.size}")
        launchOverlay(text, context, pkg)
        src.recycle()
    }

    override fun onInterrupt() {
        tapDetector.reset()
    }

    private fun nodeIdentity(node: AccessibilityNodeInfo): String {
        val viewId = runCatching { node.viewIdResourceName }.getOrNull() ?: ""
        val bounds = android.graphics.Rect()
        runCatching { node.getBoundsInScreen(bounds) }
        return "$viewId|${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
    }

    private fun extractText(node: AccessibilityNodeInfo): String? {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val t = extractText(child)
            child.recycle()
            if (!t.isNullOrBlank()) return t
        }
        return null
    }

    /**
     * Поднимаемся к контейнеру (RecyclerView/ListView/чат-скроллер) и собираем
     * текст соседних сообщений — до 12 штук, чтобы LLM видела поток диалога.
     */
    private fun collectConversationContext(target: AccessibilityNodeInfo): List<String> {
        var cursor: AccessibilityNodeInfo? = target
        var container: AccessibilityNodeInfo? = null
        var depth = 0
        while (cursor != null && depth < 8) {
            val parent = cursor.parent ?: break
            val cn = parent.className?.toString() ?: ""
            if (cn.contains("RecyclerView") || cn.contains("ListView") ||
                cn.contains("ScrollView") || parent.childCount >= 3
            ) {
                container = parent
                break
            }
            if (cursor !== target) cursor.recycle()
            cursor = parent
            depth++
        }
        container = container ?: return listOf(extractText(target).orEmpty())

        val out = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val child = container.getChild(i) ?: continue
            val t = extractText(child)
            if (!t.isNullOrBlank() && t.length in 2..2000) out += t
            child.recycle()
        }
        if (cursor !== target && cursor !== container) cursor?.recycle()
        if (container !== target) container.recycle()
        return out.takeLast(12)
    }

    private fun launchOverlay(target: String, context: List<String>, pkg: String) {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_TARGET, target)
            putExtra(OverlayService.EXTRA_CONTEXT, ArrayList(context))
            putExtra(OverlayService.EXTRA_PKG, pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startService(intent)
    }

    companion object {
        private const val TAG = "PodtekstA11y"
    }
}
