package com.ucsc.codescribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ucsc.codescribe.navigation.CodeScribeNavHost
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.theme.CodeScribeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as CodeScribeApp).container
        val viewModelFactory = AppViewModelFactory(container)

        setContent {
            CodeScribeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CodeScribeNavHost(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}