package com.rashodi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.data.settings.AppSettings
import com.rashodi.app.ui.LocalAppContainer
import com.rashodi.app.ui.nav.RashodiNavHost
import com.rashodi.app.ui.theme.RashodiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as RashodiApp).container

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = settings.darkTheme

            LaunchedEffect(dark) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                RashodiTheme(darkTheme = dark) {
                    RashodiNavHost()
                }
            }
        }
    }
}
