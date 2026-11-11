package com.lumina.pdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidContext.applicationContext = this.applicationContext
        initKoin()
        // Only on a fresh start: a recreated Activity still carries the intent it already handled.
        if (savedInstanceState == null) handleViewIntent(this, intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(this, intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}