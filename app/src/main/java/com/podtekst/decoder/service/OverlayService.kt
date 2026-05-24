package com.podtekst.decoder.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.podtekst.decoder.PodtekstApp
import com.podtekst.decoder.R
import com.podtekst.decoder.data.Settings
import com.podtekst.decoder.llm.AnalysisPipeline
import com.podtekst.decoder.llm.RelayClient
import com.podtekst.decoder.ui.overlay.CyberpunkOverlay
import com.podtekst.decoder.ui.overlay.OverlayState
import com.podtekst.decoder.ui.theme.PodtekstTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle: Lifecycle get() = dispatcher.lifecycle

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var analysisJob: Job? = null

    private var wm: WindowManager? = null
    private var hostView: View? = null

    private val state = mutableStateOf<OverlayState>(OverlayState.Loading)
    private val target = mutableStateOf("")

    override fun onCreate() {
        savedStateController.performRestore(null)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()
        if (intent == null) return START_NOT_STICKY

        val text = intent.getStringExtra(EXTRA_TARGET).orEmpty()
        val ctx = intent.getStringArrayListExtra(EXTRA_CONTEXT).orEmpty()
        val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
        if (text.isBlank()) return START_NOT_STICKY

        target.value = text
        state.value = OverlayState.Loading
        showOverlay()
        runAnalysis(text, ctx, pkg)
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val notif: Notification = NotificationCompat.Builder(this, PodtekstApp.CHANNEL_OVERLAY)
            .setContentTitle("Подтекст")
            .setContentText("Расшифровка активна")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun showOverlay() {
        if (hostView != null) return
        val windowManager = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        wm = windowManager

        val layoutFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PodtekstTheme {
                    CyberpunkOverlay(
                        target = target.value,
                        state = state.value,
                        onClose = { stopOverlay() },
                    )
                }
            }
        }

        runCatching { windowManager.addView(view, params) }
            .onFailure { Log.e(TAG, "addView failed: $it") }
        hostView = view
    }

    private fun runAnalysis(target: String, ctx: List<String>, pkg: String) {
        analysisJob?.cancel()
        analysisJob = scope.launch {
            try {
                val (url, key, model) = Settings.read(this@OverlayService)
                val client = RelayClient(url, key, model)
                val pipeline = AnalysisPipeline(client)
                val ctxFiltered = ctx.filter { it != target }
                val result = pipeline.analyze(target, ctxFiltered)
                state.value = OverlayState.Ready(result)
            } catch (t: Throwable) {
                Log.e(TAG, "analysis error in pkg=$pkg", t)
                state.value = OverlayState.Error(t.message ?: t::class.java.simpleName)
            }
        }
    }

    private fun stopOverlay() {
        val v = hostView
        if (v != null) runCatching { wm?.removeView(v) }
        hostView = null
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return null
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        analysisJob?.cancel()
        scope.cancel()
        val v = hostView
        if (v != null) runCatching { wm?.removeView(v) }
        hostView = null
        store.clear()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TARGET = "target"
        const val EXTRA_CONTEXT = "context"
        const val EXTRA_PKG = "pkg"
        private const val NOTIF_ID = 4321
        private const val TAG = "OverlaySvc"
    }
}
