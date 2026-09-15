package com.kafappstore.ciphervault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.R
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Procedural Glyph Themes for non-repetitive ASCII Loading Bars.
 * Contains exclusively non-Persian and non-Arabic character sets:
 * Katakana, Hanzi/Kanji, Hangul, Runes, Math symbols, Sci-Fi glyphs, Blocks, ASCII hackers.
 */
object GlyphThemes {
    // 1. Japanese Katakana & Cyber Kanji
    val KATAKANA = listOf("ア", "カ", "サ", "タ", "ナ", "ハ", "マ", "ヤ", "ラ", "ワ", "ツ", "シ", "ヲ", "エ", "コ", "テ", "ソ", "ロ")
    val CYBER_KANJI = listOf("零", "壹", "密", "鍵", "暗", "碼", "核", "魂", "電", "網", "龍", "鬼", "影", "光", "鏡", "虛")

    // 2. Korean Hangul Jamo & Security Words
    val HANGUL = listOf("암", "호", "화", "보", "안", "터", "미", "널", "해", "킹", "시", "스", "템", "코", "어", "루", "트", "키")

    // 3. Ancient / Cyber Runes & Greek
    val GREEK_SCI_FI = listOf("Ω", "Ψ", "Δ", "Σ", "λ", "π", "Φ", "Θ", "Ξ", "Γ", "μ", "Ϙ", "Ͽ", "ᚠ", "ᚢ", "ᚦ", "ᚨ", "ᚱ", "ᚲ")

    // 4. Cybernetic Symbols
    val CYBER_SYMBOLS = listOf("⚡", "✦", "☠", "☣", "☢", "§", "◈", "◇", "※", "∞", "≠", "≈", "⌘", "⌥", "⚙", "◉", "▲", "★")

    // 5. Classic Blocks & Gradients
    val BLOCKS = listOf("█", "▓", "▒", "░", "■", "▀", "▄", "▌", "▐")

    // 6. Hacker ASCII Tokens
    val HACKER_ASCII = listOf("#", "$", "@", "%", "&", "*", "W", "X", "0", "1", "+", "=", ">", "^", "~", "!", "Z", "K")

    // 7. Hex Byte Tokens
    val HEX_TOKENS = listOf("0x", "FF", "A9", "00", "7E", "C4", "B8", "1F", "E2", "9C", "3D", "4A")

    // Bracket styles
    val BRACKET_PAIRS = listOf(
        Pair("[", "]"),
        Pair("<", ">"),
        Pair("{", "}"),
        Pair("|", "|"),
        Pair("«", "»"),
        Pair("【", "】"),
        Pair("⟦", "⟧")
    )

    // Unfilled background characters
    val UNFILLED_CHARS = listOf(".", "·", "-", "░", ":", " ")

    /**
     * Represents a randomly generated configuration for the loading bar so that
     * every single startup looks visually unique and fresh!
     */
    data class LoadingBarConfig(
        val name: String,
        val openBracket: String,
        val closeBracket: String,
        val fillGlyph: String,
        val unfilledGlyph: String,
        val barLength: Int,
        val accentColor: Color,
        val morphGlyphs: List<String>
    )

    fun generateRandomConfig(): LoadingBarConfig {
        val bracket = BRACKET_PAIRS.random()
        val unfilled = UNFILLED_CHARS.random()
        val themeChoice = Random.nextInt(7)

        val (themeName, fillGlyph, morphPool, accent) = when (themeChoice) {
            0 -> Quad("Katakana Stream", KATAKANA.random(), KATAKANA, MatrixGreenPrimary)
            1 -> Quad("Cyber Kanji Matrix", CYBER_KANJI.random(), CYBER_KANJI, CyberCyan)
            2 -> Quad("Hangul Security Node", HANGUL.random(), HANGUL, MatrixGreenPrimary)
            3 -> Quad("Greek / Quantum Runes", GREEK_SCI_FI.random(), GREEK_SCI_FI, CyberCyan)
            4 -> Quad("Cybernetic Glyphs", CYBER_SYMBOLS.random(), CYBER_SYMBOLS, CyberAmber)
            5 -> Quad("Matrix Block Cascade", BLOCKS.random(), BLOCKS, MatrixGreenPrimary)
            else -> Quad("Terminal Hacker ASCII", HACKER_ASCII.random(), HACKER_ASCII, MatrixGreenPrimary)
        }

        return LoadingBarConfig(
            name = themeName,
            openBracket = bracket.first,
            closeBracket = bracket.second,
            fillGlyph = fillGlyph,
            unfilledGlyph = unfilled,
            barLength = 22,
            accentColor = accent,
            morphGlyphs = morphPool
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

/**
 * Procedural Terminal Scenarios: Random terminal boot stories for authentic cyber vibes.
 */
object BootScenarios {
    data class TerminalLine(
        val prefix: String,
        val text: String,
        val colorType: TerminalColor = TerminalColor.NORMAL
    )

    enum class TerminalColor {
        NORMAL,
        HIGHLIGHT,
        SUCCESS,
        WARN,
        CYAN
    }

    private val PROMPTS = listOf(
        "root@ciphervault-core:~# ./bootstrap_kernel.sh",
        "operator@node-0x7F:~# ./bypass_handshake.sh",
        "sysadmin@fs-subsystem:~# ./mount_crypt_enclave.sh",
        "anon@dark-relay-7:~# ./quantum_entropy.sh",
        "ghost@mesh-grid:~# ./zeroize_memory.sh",
        "netsec@gateway-01:~# ./inject_crypto_pipeline.sh"
    )

    fun getRandomPrompt(): String = PROMPTS.random()

    fun generateScenarioLogs(): List<TerminalLine> {
        val pid = Random.nextInt(1000, 9999)
        val memAddr = "0x" + Random.nextInt(0x10000000, 0x7FFFFFFF).toString(16).uppercase()
        val entropy = "0x" + (1..8).map { "0123456789ABCDEF".random() }.joinToString("")
        val ipNode = "${Random.nextInt(10, 192)}.${Random.nextInt(1, 254)}.${Random.nextInt(1, 254)}.${Random.nextInt(2, 250)}"

        val scenarios = listOf(
            // Scenario A: Cryptographic Enclave Initialization
            listOf(
                TerminalLine("[INIT]", "Kernel v6.12-ciphervault initializing (PID $pid)...", TerminalColor.NORMAL),
                TerminalLine("[ADDR]", "Memory mapped: $memAddr - ring-0 buffer allocated", TerminalColor.CYAN),
                TerminalLine("[RNG]", "Entropy pool harvested: hardware PRNG ($entropy)", TerminalColor.NORMAL),
                TerminalLine("[CRYPTO]", "Cascaded Layer 1: AES-256-GCM hardware vector [LOADED]", TerminalColor.HIGHLIGHT),
                TerminalLine("[CRYPTO]", "Cascaded Layer 2: ChaCha20-Poly1305 256-bit stream [LOADED]", TerminalColor.HIGHLIGHT),
                TerminalLine("[CRYPTO]", "Cascaded Layer 3: Dynamic Salted XOR Transformation [LOADED]", TerminalColor.HIGHLIGHT),
                TerminalLine("[STREAM]", "1GB+ streaming chunk pipeline: 128KB memory bound [ONLINE]", TerminalColor.SUCCESS),
                TerminalLine("[SECURE]", "Zeroization watchdog arming: all traces cleared on exit", TerminalColor.WARN),
                TerminalLine("[READY]", "All security protocols active. Granting terminal access...", TerminalColor.SUCCESS)
            ),
            // Scenario B: Deep Network Node Bypass
            listOf(
                TerminalLine("[BOOT]", "System daemon spawned under PID $pid...", TerminalColor.NORMAL),
                TerminalLine("[NETWORK]", "Probing dark relay node $ipNode:443...", TerminalColor.CYAN),
                TerminalLine("[HANDSHAKE]", "TLS 1.3 ephemeral ECDH key exchange [VERIFIED]", TerminalColor.HIGHLIGHT),
                TerminalLine("[CIPHER]", "Anti-tamper sandbox verification: OK", TerminalColor.NORMAL),
                TerminalLine("[STORE]", "Mounting Room SQLite encrypted DB repository", TerminalColor.NORMAL),
                TerminalLine("[BUFFER]", "Ring allocator buffer @ $memAddr: 0 leaks detected", TerminalColor.CYAN),
                TerminalLine("[AUTH]", "Root authorization token parsed without telemetry", TerminalColor.HIGHLIGHT),
                TerminalLine("[STATUS]", "Local enclave sandbox isolated. Terminal ready.", TerminalColor.SUCCESS)
            ),
            // Scenario C: Quantum Entropy & Stream Engine
            listOf(
                TerminalLine("[START]", "Cold bootstrap sequence started (Core PID $pid)...", TerminalColor.NORMAL),
                TerminalLine("[QUANTUM]", "Entropy seed injection: SHA3-512 / BLAKE2b cascade", TerminalColor.CYAN),
                TerminalLine("[DRIVER]", "Initializing .cvault container streaming driver", TerminalColor.NORMAL),
                TerminalLine("[IO-CHUNK]", "Chunk size locked to 128 KB - Safe for multi-GB archives", TerminalColor.HIGHLIGHT),
                TerminalLine("[ENCLAVE]", "Stack protector canary active at $memAddr", TerminalColor.NORMAL),
                TerminalLine("[KEY-STORE]", "Pepper security matrix synchronized with private store", TerminalColor.HIGHLIGHT),
                TerminalLine("[PROBE]", "Root compromise checks passed: execution clean", TerminalColor.WARN),
                TerminalLine("[TERMINAL]", "Launching graphical secure interface...", TerminalColor.SUCCESS)
            )
        )

        return scenarios.random()
    }
}

/**
 * Animated Cyber Digital Rain Canvas with Multilingual Glyphs (Matrix Style)
 * Excludes Persian and Arabic characters completely.
 */
@Composable
fun CyberMatrixRainCanvas(
    modifier: Modifier = Modifier,
    alpha: Float = 0.22f
) {
    // Large pool of authentic cyber glyphs
    val glyphPool = remember {
        (GlyphThemes.KATAKANA + GlyphThemes.CYBER_KANJI + GlyphThemes.HANGUL +
                GlyphThemes.GREEK_SCI_FI + GlyphThemes.CYBER_SYMBOLS + GlyphThemes.HACKER_ASCII)
    }

    val transition = rememberInfiniteTransition(label = "matrix_rain_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_phase"
    )

    // Pre-generate random columns offsets and glyph sequences
    val columnsData = remember {
        List(25) { colIndex ->
            val speed = Random.nextFloat() * 0.8f + 0.6f
            val startYOffset = Random.nextFloat() * 1000f
            val glyphs = List(30) { glyphPool.random() }
            Triple(speed, startYOffset, glyphs)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val colWidth = width / columnsData.size

        val paint = android.graphics.Paint().apply {
            textSize = 36f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        columnsData.forEachIndexed { i, (speed, startOffset, glyphs) ->
            val x = i * colWidth + 8f
            val currentY = ((phase * height * speed) + startOffset) % (height + 600f) - 300f

            glyphs.forEachIndexed { gIndex, char ->
                val charY = currentY - (gIndex * 42f)
                if (charY in -50f..height + 50f) {
                    // Head character is bright neon white/cyan, trailing characters fade to matrix green
                    when (gIndex) {
                        0 -> {
                            paint.color = android.graphics.Color.argb(
                                (255 * alpha * 1.5f).toInt().coerceIn(0, 255),
                                230, 255, 240
                            )
                        }
                        1, 2 -> {
                            paint.color = android.graphics.Color.argb(
                                (230 * alpha).toInt().coerceIn(0, 255),
                                0, 255, 128
                            )
                        }
                        else -> {
                            val fade = (1f - (gIndex.toFloat() / glyphs.size)).coerceIn(0.1f, 1f)
                            paint.color = android.graphics.Color.argb(
                                (180 * alpha * fade).toInt().coerceIn(0, 255),
                                0, 180, 80
                            )
                        }
                    }
                    drawContext.canvas.nativeCanvas.drawText(char, x, charY, paint)
                }
            }
        }

        // Horizontal scanline overlay (classic CRT monitor feel)
        val scanlineGap = 6f
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += scanlineGap
        }
    }
}

/**
 * Main Cyber Splash Screen Component
 */
@Composable
fun CyberSplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Generate random configuration on launch so EVERY time is unique!
    val barConfig = remember { GlyphThemes.generateRandomConfig() }
    val promptText = remember { BootScenarios.getRandomPrompt() }
    val scenarioLogs = remember { BootScenarios.generateScenarioLogs() }

    // Progress animation state
    var progress by remember { mutableFloatStateOf(0f) }
    var displayedLogCount by remember { mutableIntStateOf(0) }
    var morphGlyph by remember { mutableStateOf(barConfig.fillGlyph) }
    var statusText by remember { mutableStateOf("INITIALIZING CORE ENCLAVE...") }
    var byteCounter by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // Blinking cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorVisible by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Glitch flicker effect on title
    val glitchPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch_phase"
    )

    // Main animation driver coroutine
    LaunchedEffect(Unit) {
        // Step 1: Initial typing hesitation
        delay(200)

        // Step 2: Line by line terminal typing & progress bar filling
        val totalSteps = scenarioLogs.size
        for (i in 0 until totalSteps) {
            displayedLogCount = i + 1
            listState.animateScrollToItem(displayedLogCount)

            // Random burst in progress
            val targetProgress = ((i + 1).toFloat() / totalSteps)
            while (progress < targetProgress) {
                progress += 0.04f
                if (progress > targetProgress) progress = targetProgress

                // Rapid morphing character at the leading edge
                morphGlyph = barConfig.morphGlyphs.random()
                byteCounter += Random.nextInt(128, 512)

                delay(30)
            }

            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}

            delay(Random.nextLong(120, 240))
        }

        // Step 3: Final fill to 100%
        statusText = "SYSTEM DECRYPTED // TERMINAL UNLOCKED"
        while (progress < 1.0f) {
            progress += 0.08f
            morphGlyph = barConfig.morphGlyphs.random()
            delay(25)
        }
        progress = 1.0f

        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}

        // Hold briefly at 100% before transition
        delay(400)
        onFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040805))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap anywhere speeds up or immediately skips
                onFinish()
            }
    ) {
        // Animated Falling Digital Glyph Rain
        CyberMatrixRainCanvas(alpha = 0.20f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Header & Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, MatrixGreenPrimary), RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D1F13)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ciphervault_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        val isGlitching = glitchPhase > 0.94f
                        Text(
                            text = if (isGlitching) "C1PH3R_V4ULT" else "CIPHERVAULT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isGlitching) CyberCyan else MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "KERNEL PROTOCOL // v3.5",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF63876B),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                // Skip Button
                CyberSecondaryButton(
                    text = "SKIP >>",
                    icon = Icons.Default.FastForward,
                    onClick = onFinish,
                    accentColor = MatrixGreenPrimary,
                    depth = 3.dp,
                    testTag = "splash_skip_button"
                )
            }

            // Middle Section: Terminal Output Window
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE050B07))
                    .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Terminal Title Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .drawBehind {
                                drawLine(
                                    color = MatrixBorderNeon,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MatrixGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SECURE_ROOT_SHELL // TTY-1",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MatrixGreenPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = barConfig.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Prompt command line
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Stream of logs
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(scenarioLogs.take(displayedLogCount)) { line ->
                            val textColor = when (line.colorType) {
                                BootScenarios.TerminalColor.NORMAL -> Color(0xFF8BAE92)
                                BootScenarios.TerminalColor.HIGHLIGHT -> MatrixGreenPrimary
                                BootScenarios.TerminalColor.SUCCESS -> Color(0xFF4EFA8A)
                                BootScenarios.TerminalColor.WARN -> CyberAmber
                                BootScenarios.TerminalColor.CYAN -> CyberCyan
                            }

                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = line.prefix + " ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MatrixGreenPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Blinking terminal block cursor
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "> ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MatrixGreenPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                )
                                if (cursorVisible > 0.4f) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 8.dp, height = 13.dp)
                                            .background(MatrixGreenPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: The Dynamic Non-Repetitive ASCII Glyphic Loading Bar!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xF008130B))
                    .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                // Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFA5CCA9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )

                    val percentage = (progress * 100).toInt()
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = barConfig.accentColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Build the ASCII Glyphic Progress String
                val filledSlots = (progress * barConfig.barLength).toInt().coerceIn(0, barConfig.barLength)
                val unfilledSlots = (barConfig.barLength - filledSlots).coerceAtLeast(0)

                // Fill string with the unique glyph; the active head slot morphs dynamically
                val filledPart = StringBuilder()
                for (s in 0 until filledSlots) {
                    if (s == filledSlots - 1 && progress < 1.0f) {
                        filledPart.append(morphGlyph)
                    } else {
                        filledPart.append(barConfig.fillGlyph)
                    }
                }
                val unfilledPart = barConfig.unfilledGlyph.repeat(unfilledSlots)

                // The complete ASCII formatted loading bar: [####################]
                val asciiBar = "${barConfig.openBracket}$filledPart$unfilledPart${barConfig.closeBracket}"

                Text(
                    text = asciiBar,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = barConfig.accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Telemetry line below the bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GLYPH: [${barConfig.fillGlyph}]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6B8A74),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = "BUFFER: ${byteCounter}KB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6B8A74),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = "ENTROPY: OK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MatrixGreenPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}
