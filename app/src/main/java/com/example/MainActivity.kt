package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.PermissionRequestScreen
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.LyraTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LyraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepObsidian
                ) {
                    val mainViewModel: MainViewModel = viewModel()
                    val playerViewModel: PlayerViewModel = viewModel()

                    val isPermissionGranted by mainViewModel.isPermissionGranted.collectAsState()

                    // Check initial permission state
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }

                    if (hasPermission || isPermissionGranted) {
                        MainScreen(
                            mainViewModel = mainViewModel,
                            playerViewModel = playerViewModel
                        )
                    } else {
                        PermissionRequestScreen(
                            onPermissionGranted = {
                                mainViewModel.setPermissionGranted(true)
                            },
                            onContinueWithDemo = {
                                mainViewModel.setPermissionGranted(true)
                            }
                        )
                    }
                }
            }
        }
    }
}
