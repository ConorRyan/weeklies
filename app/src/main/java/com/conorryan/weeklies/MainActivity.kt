package com.conorryan.weeklies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.conorryan.weeklies.ui.WeekliesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as WeekliesApplication).graph
        setContent {
            WeekliesApp(graph)
        }
    }
}
