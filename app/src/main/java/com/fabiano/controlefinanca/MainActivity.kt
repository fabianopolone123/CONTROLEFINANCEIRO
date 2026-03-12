package com.fabiano.controlefinanca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fabiano.controlefinanca.ui.FinanceApp
import com.fabiano.controlefinanca.ui.theme.ControleFinancaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControleFinancaTheme {
                FinanceApp()
            }
        }
    }
}
