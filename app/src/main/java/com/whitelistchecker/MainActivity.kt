package com.whitelistchecker

import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitelistchecker.domain.checker.CellularNetworkProvider
import com.whitelistchecker.domain.checker.MobileSiteChecker
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.ui.main.MainScreen
import com.whitelistchecker.ui.main.MainViewModel
import com.whitelistchecker.ui.theme.WhiteListCheckerTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val cellularNetworkProvider = CellularNetworkProvider(connectivityManager)
        val mobileSiteChecker = MobileSiteChecker()
        val classifier = WhitelistStateClassifier()
        val useCase = WhitelistCheckUseCase(
            connectivityManager = connectivityManager,
            cellularNetworkProvider = cellularNetworkProvider,
            mobileSiteChecker = mobileSiteChecker,
            classifier = classifier,
        )
        MainViewModelFactory(useCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhiteListCheckerTheme {
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private class MainViewModelFactory(
        private val whitelistCheckUseCase: WhitelistCheckUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(whitelistCheckUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
