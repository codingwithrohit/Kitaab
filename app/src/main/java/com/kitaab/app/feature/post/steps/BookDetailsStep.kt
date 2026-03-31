package com.kitaab.app.feature.post.steps

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kitaab.app.feature.auth.kitaabTextFieldColors
import com.kitaab.app.feature.post.BarcodeScanButton
import com.kitaab.app.feature.post.PostUiState
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmMuted
import java.util.concurrent.Executors

private val EXAM_TAGS = listOf("JEE", "NEET", "UPSC", "CAT", "GATE", "College", "Other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailsStep(
    state: PostUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onPublisherChanged: (String) -> Unit,
    onEditionChanged: (String) -> Unit,
    onSubjectChanged: (String) -> Unit,
    onIsbnScanned: (String) -> Unit,
    onExamTagToggled: (String) -> Unit,
    onHasSolutionsToggled: () -> Unit,
    onHasNotesToggled: () -> Unit,
) {
    val focusManager = LocalFocusManager.current


    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        BarcodeScanButton(
            isFetchingBookDetails = state.isFetchingBookDetails,
            onIsbnScanned = onIsbnScanned,
        )

        if (state.isbn.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "ISBN: ${state.isbn}", fontSize = 12.sp, color = WarmMuted)
        }

        if (state.bookNotFound) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Text(
                    text = "Book not found in database — please fill details manually",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = { Text("Book title *") },
            isError = state.titleError != null,
            supportingText = state.titleError?.let { { Text(it) } },
            leadingIcon = { Icon(Icons.Outlined.Book, null, modifier = Modifier.size(18.dp)) },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.author,
            onValueChange = onAuthorChanged,
            label = { Text("Author") },
            leadingIcon = { Icon(Icons.Outlined.Person, null, modifier = Modifier.size(18.dp)) },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.publisher,
            onValueChange = onPublisherChanged,
            label = { Text("Publisher") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.BusinessCenter,
                    null,
                    modifier = Modifier.size(18.dp)
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.edition,
            onValueChange = onEditionChanged,
            label = { Text("Edition") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.subject,
            onValueChange = onSubjectChanged,
            label = { Text("Subject") },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Exam tags",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Optional — helps students find your book", fontSize = 13.sp, color = WarmMuted)
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EXAM_TAGS.forEach { tag ->
                val selected = tag in state.examTags
                FilterChip(
                    selected = selected,
                    onClick = { onExamTagToggled(tag) },
                    label = {
                        Text(
                            text = tag,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        )
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal50,
                            selectedLabelColor = Teal900,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = Teal500,
                            selectedBorderWidth = 1.5.dp,
                        ),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        ToggleRow(
            label = "Has solutions manual",
            checked = state.hasSolutions,
            onToggle = onHasSolutionsToggled
        )
        Spacer(modifier = Modifier.height(12.dp))
        ToggleRow(
            label = "Has handwritten notes",
            checked = state.hasNotes,
            onToggle = onHasNotesToggled
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Teal500,
                    checkedTrackColor = Teal50,
                    checkedBorderColor = Teal500,
                ),
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // Full screen camera preview
        AndroidView(
            factory = { ctx ->
                val previewView =
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview =
                        Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                    val executor = Executors.newSingleThreadExecutor()
                    val barcodeScanner = BarcodeScanning.getClient()
                    val imageAnalysis =
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(executor) { imageProxy ->
                                    if (!scanned) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image =
                                                InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees,
                                                )
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    barcodes.firstOrNull {
                                                        it.format == Barcode.FORMAT_EAN_13 ||
                                                                it.format == Barcode.FORMAT_EAN_8
                                                    }?.rawValue?.let { isbn ->
                                                        if (!scanned) {
                                                            scanned = true
                                                            onBarcodeDetected(isbn)
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Dark gradient overlay at top for status bar area
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        ),
                    )
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
        )

        // Close button — top right
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Close scanner",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        // Scanning frame + instruction in centre
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Scanning viewfinder frame
            ScannerFrame()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Point at book barcode",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // Bottom gradient overlay
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    )
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ScannerFrame() {
    val cornerColor = Color.White
    val frameSize = 240.dp
    val cornerLength = 32.dp
    val cornerThickness = 3.dp

    Box(modifier = Modifier.size(frameSize)) {
        // Dimmed area is handled by camera contrast — just draw the corner brackets
        // Top-left
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Box(
                modifier = Modifier
                    .size(cornerLength, cornerThickness)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .size(cornerThickness, cornerLength)
                    .background(cornerColor)
            )
        }
        // Top-right
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Box(
                modifier = Modifier
                    .size(cornerLength, cornerThickness)
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .size(cornerThickness, cornerLength)
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
        }
        // Bottom-left
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(
                modifier = Modifier
                    .size(cornerLength, cornerThickness)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .size(cornerThickness, cornerLength)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
        }
        // Bottom-right
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Box(
                modifier = Modifier
                    .size(cornerLength, cornerThickness)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .size(cornerThickness, cornerLength)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
        }
    }
}
