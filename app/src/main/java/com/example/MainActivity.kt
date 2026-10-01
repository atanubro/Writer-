package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.draft.ImproveDraftScreen
import com.example.ui.home.HomeScreen
import com.example.ui.newletter.CreateLetterScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.templates.TemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LetterViewModel
import com.example.ui.workspace.WorkspaceScreen

enum class AppScreen {
    HOME,
    WORKSPACE,
    IMPROVE_DRAFT,
    CREATE_LETTER,
    TEMPLATES,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val letterViewModel: LetterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OfficeLetterApp(viewModel = letterViewModel)
                }
            }
        }
    }
}

@Composable
fun OfficeLetterApp(viewModel: LetterViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToWorkspace = { currentScreen = AppScreen.WORKSPACE },
                onNavigateToDraftImprovement = { currentScreen = AppScreen.IMPROVE_DRAFT },
                onNavigateToCreateLetter = { currentScreen = AppScreen.CREATE_LETTER },
                onNavigateToTemplates = { currentScreen = AppScreen.TEMPLATES },
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
            )
        }
        AppScreen.WORKSPACE -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            WorkspaceScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.HOME },
                onNavigateToDraftImprovement = { currentScreen = AppScreen.IMPROVE_DRAFT }
            )
        }
        AppScreen.IMPROVE_DRAFT -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            ImproveDraftScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.HOME },
                onDraftImproved = { currentScreen = AppScreen.WORKSPACE }
            )
        }
        AppScreen.CREATE_LETTER -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            CreateLetterScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.HOME },
                onLetterCreated = { currentScreen = AppScreen.WORKSPACE }
            )
        }
        AppScreen.TEMPLATES -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            TemplatesScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.HOME },
                onTemplateSelected = { currentScreen = AppScreen.WORKSPACE }
            )
        }
        AppScreen.SETTINGS -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.HOME }
            )
        }
    }
}
