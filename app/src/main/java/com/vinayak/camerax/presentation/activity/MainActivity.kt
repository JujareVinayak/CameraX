package com.vinayak.camerax.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vinayak.camerax.presentation.compose.QRCodeScreen
import com.vinayak.camerax.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Uncomment each composable at a time for demo.
                // ImageCaptureUseCase()
                // ImageCaptureWithActivityLauncher()
                // QRCodeScreen()
            }
        }
    }
}