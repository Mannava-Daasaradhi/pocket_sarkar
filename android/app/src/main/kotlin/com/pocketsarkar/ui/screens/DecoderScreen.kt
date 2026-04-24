package com.pocketsarkar.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketsarkar.modules.decoder.DecoderResult
import com.pocketsarkar.modules.decoder.DocumentInput
import com.pocketsarkar.modules.decoder.RedFlag
import com.pocketsarkar.modules.decoder.RiskLevel
import com.pocketsarkar.ui.viewmodels.DecoderUiState
import com.pocketsarkar.ui.viewmodels.DecoderViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderScreen(
    onBack: () -> Unit,
    viewModel: DecoderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Document picker (image + PDF) ─────────────────────────────────────────
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val input = when {
            mimeType.startsWith("image/") -> DocumentInput.GalleryImage(uri)
            mimeType == "application/pdf" -> DocumentInput.PdfFile(uri)
            else                          -> DocumentInput.GalleryImage(uri) // best effort
        }
        viewModel.analyze(input)
    }

    // ── Camera permission ─────────────────────────────────────────────────────
    var cameraPermissionGranted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    )}
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Document Decoder")
                        Text(
                            "दस्तावेज़ डीकोडर",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState is DecoderUiState.Success || uiState is DecoderUiState.Error) {
                            viewModel.reset()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is DecoderUiState.Success) {
                        IconButton(onClick = {
                            shareAnalysis(context, (uiState as DecoderUiState.Success).result)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share analysis")
                        }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "decoder_state",
        ) { state ->
            when (state) {
                is DecoderUiState.Idle -> {
                    IdleContent(
                        cameraPermissionGranted = cameraPermissionGranted,
                        onRequestPermission    = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onCapture              = { bitmap -> viewModel.analyze(DocumentInput.CameraImage(bitmap)) },
                        onPickDocument         = { documentPickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onPasteText            = { text -> viewModel.analyze(DocumentInput.PlainText(text)) },
                    )
                }
                is DecoderUiState.Processing,
                is DecoderUiState.Streaming -> {
                    val partial = (state as? DecoderUiState.Streaming)?.partial
                    ProcessingContent(partial = partial)
                }
                is DecoderUiState.Success -> {
                    ResultsContent(
                        result  = state.result,
                        onRetry = { viewModel.reset() },
                    )
                }
                is DecoderUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.reset() },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Idle — camera preview + upload button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    cameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCapture: (Bitmap) -> Unit,
    onPickDocument: () -> Unit,
    onPasteText: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    var pasteMode by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {

        // Camera section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (cameraPermissionGranted) {
                // CameraX preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureRef.value = imageCapture
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            } catch (e: Exception) {
                                // Camera bind failed — handled gracefully
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Capture button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    IconButton(
                        onClick = {
                            imageCaptureRef.value?.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = image.toBitmap()
                                        image.close()
                                        onCapture(bitmap)
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        // TODO: surface error to user
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                            .border(3.dp, Color.Gray, CircleShape),
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture document",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            } else {
                // No permission — show request button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Camera access needed to scan documents",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRequestPermission) {
                        Text("Allow Camera")
                    }
                }
            }
        }

        // Bottom action row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!pasteMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick  = onPickDocument,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload PDF / Image")
                    }
                    FilledTonalButton(
                        onClick  = { pasteMode = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Paste Text")
                    }
                }
            } else {
                OutlinedTextField(
                    value = pastedText,
                    onValueChange = { pastedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste document text here...") },
                    minLines = 3,
                    maxLines = 6,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { pasteMode = false; pastedText = "" },
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (pastedText.isNotBlank()) onPasteText(pastedText.trim())
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pastedText.isNotBlank(),
                    ) { Text("Analyze") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Processing — spinner + progress message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProcessingContent(partial: String?) {
    val spinMessages = listOf(
        "Reading document…",
        "Checking for red flags…",
        "Calculating risk…",
        "Identifying your rights…",
        "Almost done…",
    )
    var msgIndex by remember { mutableStateOf(0) }
    LaunchedEffect(partial?.length) {
        if ((partial?.length ?: 0) % 80 == 0) {
            msgIndex = (msgIndex + 1) % spinMessages.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp).rotate(rotation),
            strokeWidth = 5.dp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = spinMessages[msgIndex],
            style = MaterialTheme.typography.titleMedium,
        )
        if (!partial.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Receiving AI response…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Results — risk badge, red flags, rights, questions, action
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultsContent(
    result: DecoderResult,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Risk Badge ────────────────────────────────────────────────────────
        RiskBadgeCard(result = result)

        // ── Summary ───────────────────────────────────────────────────────────
        if (result.summary.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(result.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Red Flags ─────────────────────────────────────────────────────────
        if (result.redFlags.isNotEmpty()) {
            RedFlagsSection(flags = result.redFlags)
        }

        // ── Your Rights ───────────────────────────────────────────────────────
        if (result.userRights.isNotEmpty()) {
            ExpandableSection(
                title = "Your Rights 🛡️",
                items = result.userRights,
            )
        }

        // ── Ask Before Signing ────────────────────────────────────────────────
        if (result.suggestedQuestions.isNotEmpty()) {
            ExpandableSection(
                title = "Ask Before Signing ❓",
                items = result.suggestedQuestions,
            )
        }

        // ── Action Card ───────────────────────────────────────────────────────
        if (result.actionRequired.isNotBlank()) {
            ActionCard(action = result.actionRequired)
        }

        // ── Scan Another ─────────────────────────────────────────────────────
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan Another Document")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RiskBadgeCard(result: DecoderResult) {
    val (bg, emoji, label, textColor) = when (result.riskLevel) {
        RiskLevel.GREEN  -> listOf(Color(0xFF1B5E20), "🟢", "SAFE",      Color.White)
        RiskLevel.YELLOW -> listOf(Color(0xFFF57F17), "🟡", "CAUTION",   Color.White)
        RiskLevel.RED    -> listOf(Color(0xFFB71C1C), "🔴", "HIGH RISK", Color.White)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = bg as Color),
        shape    = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = emoji as String,
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = label as String,
                style     = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color     = textColor as Color,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Risk Score: ${result.riskScore}/100",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(result.documentType, textColor)
                InfoChip(result.languageDetected, textColor)
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, textColor: Color) {
    if (text.isBlank()) return
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.25f),
    ) {
        Text(
            text      = text,
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style     = MaterialTheme.typography.labelMedium,
            color     = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RedFlagsSection(flags: List<RedFlag>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "⚠️ Red Flags (${flags.size})",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.error,
        )
        flags.forEach { flag ->
            RedFlagCard(flag = flag)
        }
    }
}

@Composable
private fun RedFlagCard(flag: RedFlag) {
    val (borderColor, badgeText, badgeColor) = when (flag.severity.uppercase()) {
        "HIGH"  -> Triple(Color(0xFFB71C1C), "HIGH",   Color(0xFFB71C1C))
        "LOW"   -> Triple(Color(0xFF1565C0), "LOW",    Color(0xFF1565C0))
        else    -> Triple(Color(0xFFF57F17), "MEDIUM", Color(0xFFF57F17))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = badgeColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text      = flag.clause.ifBlank { "Problematic term detected" },
                        style     = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color     = badgeColor,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.1f),
                ) {
                    Text(
                        text     = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = badgeColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (flag.risk.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = flag.risk,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ExpandableSection(title: String, items: List<String>) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text      = title,
                    style     = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("•", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text  = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(action: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "What to Do 📋",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint     = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick  = onRetry,
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Share helper
// ─────────────────────────────────────────────────────────────────────────────

private fun shareAnalysis(context: Context, result: DecoderResult) {
    val sb = StringBuilder()
    sb.appendLine("📋 Pocket Sarkar — Document Analysis")
    sb.appendLine("════════════════════════════════")
    sb.appendLine("Document: ${result.documentType}")
    sb.appendLine("Language: ${result.languageDetected}")
    sb.appendLine("Risk: ${result.riskLevel} (${result.riskScore}/100)")
    sb.appendLine()
    sb.appendLine("Summary:")
    sb.appendLine(result.summary)
    if (result.redFlags.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("⚠️ Red Flags:")
        result.redFlags.forEach { flag ->
            sb.appendLine("• [${flag.severity}] ${flag.clause}")
            sb.appendLine("  ${flag.risk}")
        }
    }
    if (result.userRights.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("🛡️ Your Rights:")
        result.userRights.forEach { sb.appendLine("• $it") }
    }
    if (result.suggestedQuestions.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("❓ Questions to Ask:")
        result.suggestedQuestions.forEach { sb.appendLine("• $it") }
    }
    sb.appendLine()
    sb.appendLine("📌 What to Do:")
    sb.appendLine(result.actionRequired)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type    = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Document Analysis by Pocket Sarkar")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Analysis"))
}
