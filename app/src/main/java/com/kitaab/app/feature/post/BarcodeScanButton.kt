package com.kitaab.app.feature.post

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kitaab.app.ui.theme.Teal500
import java.util.concurrent.Executors

/**
 * Drop-in scan button + dialog. Place anywhere a barcode trigger is needed.
 * Handles permission request internally.
 *
 * @param isFetchingBookDetails show spinner on button while ISBN lookup is in progress
 * @param onIsbnScanned called with the raw ISBN string when a barcode is detected
 */
@Composable
fun BarcodeScanButton(
    isFetchingBookDetails: Boolean,
    onIsbnScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showScanner by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasCameraPermission = granted
            if (granted) showScanner = true
        }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
        ) {
            BarcodeScannerView(
                onBarcodeDetected = { isbn ->
                    showScanner = false
                    onIsbnScanned(isbn)
                },
                onDismiss = { showScanner = false },
            )
        }
    }

    OutlinedButton(
        onClick = {
            if (hasCameraPermission) {
                showScanner = true
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Teal500),
    ) {
        Icon(
            Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            tint = Teal500,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Scan barcode",
            color = Teal500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        if (isFetchingBookDetails) {
            Spacer(modifier = Modifier.size(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Teal500,
                strokeWidth = 2.dp,
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
        )

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

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                            ),
                        ),
                    )
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun ScannerFrame() {
    val cornerColor = Color.White
    val frameSize = 240.dp
    val cornerLength = 32.dp
    val cornerThickness = 3.dp

    Box(modifier = Modifier.size(frameSize)) {
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Box(
                modifier =
                    Modifier
                        .size(cornerLength, cornerThickness)
                        .background(cornerColor),
            )
            Box(
                modifier =
                    Modifier
                        .size(cornerThickness, cornerLength)
                        .background(cornerColor),
            )
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Box(
                modifier =
                    Modifier
                        .size(cornerLength, cornerThickness)
                        .align(Alignment.TopEnd)
                        .background(cornerColor),
            )
            Box(
                modifier =
                    Modifier
                        .size(cornerThickness, cornerLength)
                        .align(Alignment.TopEnd)
                        .background(cornerColor),
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(
                modifier =
                    Modifier
                        .size(cornerLength, cornerThickness)
                        .align(Alignment.BottomStart)
                        .background(cornerColor),
            )
            Box(
                modifier =
                    Modifier
                        .size(cornerThickness, cornerLength)
                        .align(Alignment.BottomStart)
                        .background(cornerColor),
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Box(
                modifier =
                    Modifier
                        .size(cornerLength, cornerThickness)
                        .align(Alignment.BottomEnd)
                        .background(cornerColor),
            )
            Box(
                modifier =
                    Modifier
                        .size(cornerThickness, cornerLength)
                        .align(Alignment.BottomEnd)
                        .background(cornerColor),
            )
        }
    }
}
