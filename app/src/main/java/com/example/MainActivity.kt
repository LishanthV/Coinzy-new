package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AuthManager
import com.example.data.database.TransactionDatabase
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceViewModel
import com.example.ui.FinanceViewModelFactory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core persistence setup
        val database = TransactionDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(database.transactionDao())
        val authManager = AuthManager(applicationContext)
        
        // Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this, 
            FinanceViewModelFactory(repository, authManager)
        )[FinanceViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isRegistered by viewModel.isRegistered.collectAsStateWithLifecycle()

                // Navigation override destination
                var screenOverride by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)

                    when {
                        screenOverride == "login" -> {
                            LoginScreen(
                                onLoginSuccess = { password ->
                                    val success = viewModel.loginUser(password)
                                    if (success) {
                                        screenOverride = null
                                    }
                                    success
                                },
                                onNavigateToRegister = {
                                    screenOverride = "register"
                                },
                                modifier = modifier
                            )
                        }
                        screenOverride == "register" -> {
                            RegisterScreen(
                                onRegisterSuccess = { name, email, password, avatarIdx, currency ->
                                    val success = viewModel.registerUser(name, email, password, avatarIdx, currency)
                                    if (success) {
                                        screenOverride = null
                                    }
                                    success
                                },
                                onNavigateToLogin = {
                                    screenOverride = "login"
                                },
                                modifier = modifier
                            )
                        }
                        !isRegistered -> {
                            RegisterScreen(
                                onRegisterSuccess = { name, email, password, avatarIdx, currency ->
                                    viewModel.registerUser(name, email, password, avatarIdx, currency)
                                },
                                onNavigateToLogin = {
                                    screenOverride = "login"
                                },
                                modifier = modifier
                            )
                        }
                        !isLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = { password ->
                                    viewModel.loginUser(password)
                                },
                                onNavigateToRegister = {
                                    screenOverride = "register"
                                },
                                modifier = modifier
                            )
                        }
                        else -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                modifier = modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
