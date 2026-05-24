package com.podtekst.decoder.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.podtekst.decoder.llm.AnalysisPipeline
import com.podtekst.decoder.ui.theme.CyberBg
import com.podtekst.decoder.ui.theme.CyberBlue
import com.podtekst.decoder.ui.theme.CyberBlueDeep
import com.podtekst.decoder.ui.theme.CyberBlueDim
import com.podtekst.decoder.ui.theme.CyberRed
import com.podtekst.decoder.ui.theme.CyberWhite
import kotlin.random.Random

sealed interface OverlayState {
    data object Loading : OverlayState
    data class Ready(val result: AnalysisPipeline.AnalysisResult) : OverlayState
    data class Error(val msg: String) : OverlayState
}

@Composable
fun CyberpunkOverlay(
    target: String,
    state: OverlayState,
    onClose: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "glitch")
    val scan by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )
    val flicker by infinite.animateFloat(
        initialValue = 0.78f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )

    var revealProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        // Glitch-in 600ms.
        val frames = 24
        repeat(frames) {
            kotlinx.coroutines.delay(25)
            revealProgress = (it + 1) / frames.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .background(CyberBg.copy(alpha = 0.92f), shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(CyberBlue, CyberBlueDeep, CyberBlue.copy(alpha = 0.3f))),
                shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
            )
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawScanlines(scan)
                    drawNoise(seed = (pulse * 1000).toInt())
                    drawCornerBrackets(flicker)
                }
            }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HeaderRow(onClose = onClose, flicker = flicker)
            Spacer(Modifier.height(8.dp))
            TargetBlock(target = target, reveal = revealProgress)
            Spacer(Modifier.height(10.dp))
            DividerCyber()
            Spacer(Modifier.height(10.dp))
            when (state) {
                OverlayState.Loading -> LoadingBlock(scan = scan)
                is OverlayState.Error -> ErrorBlock(state.msg)
                is OverlayState.Ready -> ResultBlock(state.result)
            }
        }
    }
}

@Composable
private fun HeaderRow(onClose: () -> Unit, flicker: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(CyberBlue.copy(alpha = flicker))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "PODTEKST :: DECODER //",
            color = CyberBlue.copy(alpha = flicker),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "v0.1",
            color = CyberBlueDeep,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "close", tint = CyberBlue)
        }
    }
}

@Composable
private fun TargetBlock(target: String, reveal: Float) {
    val shown = if (reveal >= 1f) target else scrambleReveal(target, reveal)
    Column {
        Text(
            "[ INPUT.MSG ]",
            color = CyberBlueDeep,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(2.dp))
        GlitchText(text = shown, color = CyberWhite, sizeSp = 14)
    }
}

@Composable
private fun LoadingBlock(scan: Float) {
    Column {
        Text(
            "// ANALYZING SUBTEXT ...",
            color = CyberBlue,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        // Прогресс-полоса.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(CyberBlueDim)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(scan)
                    .height(6.dp)
                    .background(CyberBlue)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "pass 1: facts\npass 2: 3 hypotheses\npass 3: ranking + red flags",
            color = CyberBlueDeep,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ErrorBlock(msg: String) {
    Column {
        Text(
            "!! ERROR ::",
            color = CyberRed,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            msg,
            color = CyberRed.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ResultBlock(r: AnalysisPipeline.AnalysisResult) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scroll).fillMaxWidth()) {
        ConfidencePill(r.confidence)
        Spacer(Modifier.height(8.dp))

        Text("[ SUBTEXT ]", color = CyberBlueDeep, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        GlitchText(text = r.subtext, color = CyberBlue, sizeSp = 16, bold = true)
        Spacer(Modifier.height(10.dp))

        if (r.interpretations.isNotEmpty()) {
            Text("[ 3 INTERPRETATIONS ]", color = CyberBlueDeep, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            r.interpretations.forEachIndexed { i, line ->
                Text(
                    "> ${i + 1}. $line",
                    color = CyberWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.redFlags.isNotEmpty()) {
            Text("[ RED FLAGS ]", color = CyberRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            r.redFlags.forEach { f ->
                Text(
                    "× $f",
                    color = CyberRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (r.replies.isNotEmpty()) {
            Text("[ COUNTER-SCRIPT ]", color = CyberBlueDeep, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            r.replies.forEach { (goal, text) ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        "→ ${goal.replace('_', ' ')}",
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "  «$text»",
                        color = CyberWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidencePill(c: String) {
    val (label, color) = when (c.lowercase()) {
        "high" -> "CONF::HIGH" to CyberBlue
        "med", "medium" -> "CONF::MED" to CyberBlueDeep
        else -> "CONF::LOW" to CyberRed
    }
    Box(
        modifier = Modifier
            .background(CyberBg)
            .border(1.dp, SolidColor(color))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlitchText(text: String, color: Color, sizeSp: Int, bold: Boolean = false) {
    Box {
        // RGB-split: красный левее, синий правее.
        Text(
            text,
            color = Color(0xFFFF0066).copy(alpha = 0.45f),
            fontFamily = FontFamily.Monospace,
            fontSize = sizeSp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 0.dp).then(Modifier),
        )
        Text(
            text,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = sizeSp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 1.5.dp),
        )
    }
}

@Composable
private fun DividerCyber() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, CyberBlue, Color.Transparent)
                )
            )
    )
}

private fun scrambleReveal(text: String, p: Float): String {
    val cutoff = (text.length * p).toInt().coerceIn(0, text.length)
    val rnd = Random(text.hashCode())
    val sb = StringBuilder(text.length)
    for (i in text.indices) {
        if (i < cutoff) sb.append(text[i])
        else {
            val ch = text[i]
            if (ch.isWhitespace()) sb.append(ch) else sb.append(SCRAMBLE_CHARS[rnd.nextInt(SCRAMBLE_CHARS.length)])
        }
    }
    return sb.toString()
}

private const val SCRAMBLE_CHARS = "▓▒░█│┤┐└┴┬├─┼╞╪╫╤╨╧╥╕═╗╝╜╛┘┌█▄▌▐▀123#@$%&"

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScanlines(progress: Float) {
    val h = size.height
    val w = size.width
    val stripeH = 2f
    var y = 0f
    while (y < h) {
        drawRect(
            color = Color(0xFF0080FF).copy(alpha = 0.06f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(w, stripeH),
        )
        y += 4f
    }
    // Бегущая яркая сканлиния
    val movingY = (progress * h) % h
    drawRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.18f),
        topLeft = Offset(0f, movingY),
        size = androidx.compose.ui.geometry.Size(w, 2f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNoise(seed: Int) {
    val rnd = Random(seed)
    val count = (size.width * size.height / 600f).toInt().coerceAtMost(120)
    repeat(count) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val a = 0.04f + rnd.nextFloat() * 0.10f
        drawRect(
            color = Color(0xFFFFFFFF).copy(alpha = a),
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(1f, 1f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBrackets(flicker: Float) {
    val len = 18f
    val stroke = Stroke(width = 2f)
    val c = Color(0xFF00E5FF).copy(alpha = flicker)
    // Top-left
    drawLine(c, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke.width)
    drawLine(c, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke.width)
    // Top-right
    drawLine(c, Offset(size.width - len, 0f), Offset(size.width, 0f), strokeWidth = stroke.width)
    drawLine(c, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke.width)
    // Bottom-left
    drawLine(c, Offset(0f, size.height - len), Offset(0f, size.height), strokeWidth = stroke.width)
    drawLine(c, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke.width)
    // Bottom-right
    drawLine(c, Offset(size.width, size.height - len), Offset(size.width, size.height), strokeWidth = stroke.width)
    drawLine(c, Offset(size.width - len, size.height), Offset(size.width, size.height), strokeWidth = stroke.width)
}
