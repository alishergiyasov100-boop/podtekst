package com.podtekst.decoder.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.podtekst.decoder.canDrawOverlays
import com.podtekst.decoder.data.Settings
import com.podtekst.decoder.service.PodtekstAccessibilityService
import com.podtekst.decoder.ui.theme.CyberBg
import com.podtekst.decoder.ui.theme.CyberBlue
import com.podtekst.decoder.ui.theme.CyberBlueDeep
import com.podtekst.decoder.ui.theme.CyberBlueDim
import com.podtekst.decoder.ui.theme.CyberRed
import com.podtekst.decoder.ui.theme.CyberWhite
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onOpenA11y: () -> Unit,
    onOpenOverlay: () -> Unit,
    onTryOverlay: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val relayUrl by Settings.relayUrl(ctx).collectAsState(initial = Settings.DEFAULT_RELAY_URL)
    val relayKey by Settings.relayKey(ctx).collectAsState(initial = "")
    val model by Settings.model(ctx).collectAsState(initial = Settings.DEFAULT_MODEL)

    var urlField by remember { mutableStateOf(relayUrl) }
    var keyField by remember { mutableStateOf(relayKey) }
    var modelField by remember { mutableStateOf(model) }
    LaunchedEffect(relayUrl) { urlField = relayUrl }
    LaunchedEffect(relayKey) { keyField = relayKey }
    LaunchedEffect(model) { modelField = model }

    val a11yEnabled = isA11yEnabled(ctx)
    val overlayEnabled = ctx.canDrawOverlays()

    Box(modifier = Modifier.fillMaxSize().background(CyberBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(20.dp)
        ) {
            BannerTitle()
            Spacer(Modifier.height(16.dp))
            Caption("// СТАТУС РАЗРЕШЕНИЙ")
            Spacer(Modifier.height(8.dp))
            StatusRow("Accessibility (4-tap listener)", a11yEnabled, onClick = onOpenA11y)
            Spacer(Modifier.height(6.dp))
            StatusRow("Draw over other apps", overlayEnabled, onClick = onOpenOverlay)

            Spacer(Modifier.height(20.dp))
            Caption("// LLM-RELAY (PocketQwal или совместимый)")
            Spacer(Modifier.height(8.dp))
            CyberField(label = "URL", value = urlField, onChange = { urlField = it })
            Spacer(Modifier.height(6.dp))
            CyberField(label = "API key (опц.)", value = keyField, onChange = { keyField = it })
            Spacer(Modifier.height(6.dp))
            CyberField(label = "Model", value = modelField, onChange = { modelField = it })
            Spacer(Modifier.height(10.dp))
            CyberButton("СОХРАНИТЬ") {
                scope.launch {
                    Settings.setRelayUrl(ctx, urlField)
                    Settings.setRelayKey(ctx, keyField)
                    Settings.setModel(ctx, modelField)
                }
            }

            Spacer(Modifier.height(20.dp))
            Caption("// ТЕСТ ОВЕРЛЕЯ")
            Spacer(Modifier.height(8.dp))
            CyberButton("ЗАПУСТИТЬ ДЕМО") { onTryOverlay() }

            Spacer(Modifier.height(20.dp))
            Caption("// КАК ПОЛЬЗОВАТЬСЯ")
            Spacer(Modifier.height(8.dp))
            HelpText(
                "1. Выдай два разрешения выше.\n" +
                    "2. Открой любой мессенджер (Telegram, WhatsApp, SMS, VK).\n" +
                    "3. Тапни 4 раза подряд по сообщению, которое хочешь расшифровать.\n" +
                    "4. Поверх экрана появится киберпанк-плашка с подтекстом.\n" +
                    "\n" +
                    "Контекст диалога (до 12 ближних сообщений) берётся локально через\n" +
                    "Accessibility и уходит ТОЛЬКО на твой указанный relay-URL."
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BannerTitle() {
    Column {
        Text(
            "PODTEKST",
            color = CyberBlue,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
        )
        Text(
            ":: subtext decoder · v0.1",
            color = CyberBlueDeep,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        color = CyberBlueDeep,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (ok) CyberBlue else CyberRed, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(if (ok) CyberBlue else CyberRed)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = CyberWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .background(if (ok) CyberBlueDim else CyberRed.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                if (ok) "OK" else "OFF",
                color = if (ok) CyberBlue else CyberRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!ok) {
            Button(
                onClick = onClick,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlueDim,
                    contentColor = CyberBlue,
                ),
            ) {
                Text("FIX", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CyberField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(
            label,
            color = CyberBlueDeep,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBlue)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                cursorBrush = SolidColor(CyberBlue),
                textStyle = TextStyle(
                    color = CyberWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CyberButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CyberBlueDim,
            contentColor = CyberBlue,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBlue, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
    ) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun HelpText(text: String) {
    Text(
        text,
        color = CyberWhite.copy(alpha = 0.85f),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
}

private fun isA11yEnabled(ctx: android.content.Context): Boolean {
    val expected = ctx.packageName + "/" + PodtekstAccessibilityService::class.java.name
    val enabled = android.provider.Settings.Secure.getString(
        ctx.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
