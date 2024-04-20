/*
 * Copyright © 2023 Mercedes-Benz Research & Development India. All rights reserved.
 */
package com.vinayak.camerax.presentation.compose

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Composable for scanning QR code.
 */
@Composable
fun QRCodeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview: Preview? = null
    var imageAnalysis: ImageAnalysis? = null
    val cameraProvider = remember {
        ProcessCameraProvider.getInstance(context)
    }
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )
    if (hasCamPermission.not()) {
        LaunchedEffect(key1 = true) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (hasCamPermission) {
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context)
                    preview = Preview.Builder().build()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    preview?.setSurfaceProvider(previewView.surfaceProvider)
                    imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder().setResolutionStrategy(
                                ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                            ).build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    // Specifying format of barcode your app is dealing with, detection will be faster
                    val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val barcodeScanner: BarcodeScanner =
                        BarcodeScanning.getClient(barcodeScannerOptions)
                    imageAnalysis?.setAnalyzer(
                        ContextCompat.getMainExecutor(context)
                    ) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy) {
                            cameraProvider.get().unbind(preview, imageAnalysis)
                        }
                    }
                    try {
                        cameraProvider.get().bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
            )
        } else {
            LaunchedEffect(key1 = true) {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider.get().unbind(preview, imageAnalysis)
        }
    }
}

@SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    unbindCameraResources: () -> Unit
) {
    val inputImage =
        InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.forEach {
                Log.d(QR_CODE_SCREEN, "Barcode Value: ${it.rawValue.toString()}")
                unbindCameraResources()
            }
        }
        .addOnFailureListener {
            Log.e(QR_CODE_SCREEN, "QRCode: ${it.message}")
        }.addOnCompleteListener {
            Log.d(QR_CODE_SCREEN, "QRCode listener complete")
            imageProxy.close()
        }
}

private const val QR_CODE_SCREEN = "QRCodeScreen"
