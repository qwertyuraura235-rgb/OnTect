package com.park.reagentkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.park.reagentkeeper.ui.ReagentKeeperApp
import com.park.reagentkeeper.ui.ReagentKeeperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReagentKeeperTheme {
                ReagentKeeperApp()
            }
        }
    }
}
