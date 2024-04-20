package com.example.camerax.presentation.compose

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

@Composable
fun ImageCapture() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider = remember {
        ProcessCameraProvider.getInstance(context)
    }
    var preview: Preview?
    var imageCapture: ImageCapture? = null
    ConstraintLayout {
        val (cameraPreview, captureButton) = createRefs()
        AndroidView(
            modifier = Modifier
                .fillMaxHeight(0.8F)
                .constrainAs(cameraPreview) {
                    top.linkTo(parent.top, margin = 16.dp)
                    bottom.linkTo(captureButton.top)
                },
            factory = { context ->
                val previewView = PreviewView(context)
                val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                ).build()
                preview = Preview.Builder().setResolutionSelector(resolutionSelector).build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                preview?.setSurfaceProvider(previewView.surfaceProvider)
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setResolutionSelector(resolutionSelector).build()
                try {
                    cameraProvider.get().bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                previewView
            }
        )
        Button(modifier = Modifier.constrainAs(captureButton) {
            bottom.linkTo(parent.bottom, margin = 16.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }, onClick = {
            val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
            takePhoto(
                fileName,
                imageCapture!!,
                File(context.filesDir.absolutePath),
                ContextCompat.getMainExecutor(context),
                {
                    Log.d("TAG123", "ImageCapture: ${it.path}")
                }
            ) {

            }
        }) {
            Text("Capture Image")
        }
    }
}

private fun takePhoto(
    filenameFormat: String,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {

    val photoFile = File(
        outputDirectory,
        filenameFormat
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) {
            Log.e("kilo", "Take photo error:", exception)
            onError(exception)
        }

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            onImageCaptured(savedUri)
        }
    })
}
