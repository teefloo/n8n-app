package com.n8n.mobilemanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.ui.navigation.N8nNavigation
import com.n8n.mobilemanager.ui.theme.N8nMobileManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        
        // Keep splash screen visible while loading
        var isLoading = true
        splashScreen.setKeepOnScreenCondition { isLoading }
        
        // Load preferences
        lifecycleScope.launch {
            // Give time for initial data load
            kotlinx.coroutines.delay(500)
            isLoading = false
        }

        setContent {
            // Observe theme preference
            val themeMode by preferencesManager.themeMode.collectAsState(
                initial = PreferencesManager.ThemeMode.SYSTEM
            )
            
            val darkTheme = when (themeMode) {
                PreferencesManager.ThemeMode.LIGHT -> false
                PreferencesManager.ThemeMode.DARK -> true
                PreferencesManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            N8nMobileManagerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    N8nNavigation()
                }
            }
        }
    }
}
