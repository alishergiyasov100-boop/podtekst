package com.podtekst.decoder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.podtekst.decoder.ui.screen.MainScreen
import com.podtekst.decoder.ui.theme.PodtekstTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PodtekstTheme {
                MainScreen(
                    onOpenA11y = { openA11ySettings() },
                    onOpenOverlay = { openOverlaySettings() },
                    onTryOverlay = { previewOverlay() },
                )
            }
        }
    }

    private fun openA11ySettings() {
        startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
        }
    }

    private fun previewOverlay() {
        val demo = arrayListOf(
            "Привет, ты где?",
            "Опять пропал на весь день",
            "Я же говорила, мне это важно",
            "Ну как знаешь, я уже привыкла",
        )
        val intent = Intent(this, com.podtekst.decoder.service.OverlayService::class.java).apply {
            putExtra(com.podtekst.decoder.service.OverlayService.EXTRA_TARGET, "Ну как знаешь, я уже привыкла")
            putStringArrayListExtra(com.podtekst.decoder.service.OverlayService.EXTRA_CONTEXT, demo)
            putExtra(com.podtekst.decoder.service.OverlayService.EXTRA_PKG, packageName)
        }
        startService(intent)
    }
}

fun Context.canDrawOverlays(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        AndroidSettings.canDrawOverlays(this)
    else true
