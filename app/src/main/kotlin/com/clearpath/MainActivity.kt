package com.clearpath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.clearpath.ui.navigation.ClearPathNavGraph
import com.clearpath.ui.theme.ClearPathTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClearPathTheme {
                ClearPathNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
