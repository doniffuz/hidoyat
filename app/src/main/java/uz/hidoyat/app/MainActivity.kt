package uz.hidoyat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import uz.hidoyat.app.core.ui.theme.HidoyatTheme
import uz.hidoyat.app.data.local.SettingsStore
import uz.hidoyat.app.data.local.ThemeMode
import uz.hidoyat.app.feature.home.HomeViewModel
import uz.hidoyat.app.feature.prayer.PrayerViewModel
import uz.hidoyat.app.feature.profile.ProfileScreen
import uz.hidoyat.app.feature.profile.ProfileViewModel
import uz.hidoyat.app.feature.welcome.WelcomeScreen
import uz.hidoyat.app.feature.welcome.WelcomeViewModel
import uz.hidoyat.app.navigation.HidoyatMainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val prayerViewModel: PrayerViewModel by viewModels()
    private val welcomeViewModel: WelcomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = SettingsStore(this)

        setContent {
            var themeMode by remember {
                mutableStateOf(settingsStore.themeMode)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            HidoyatTheme(darkTheme = darkTheme) {
                val completed by welcomeViewModel.completed.collectAsState()

                if (completed) {
                    val state by viewModel.state.collectAsState()
                    val prayerState by prayerViewModel.state.collectAsState()

                    HidoyatMainScreen(
                        homeState = state,
                        prayerState = prayerState,
                        prayerViewModel = prayerViewModel,
                        onToggleGoal = viewModel::toggleGoal,
                        onHomeShown = {
                            viewModel.refresh()
                            prayerViewModel.onScreenShown()
                        },
                        profileContent = {
                            val profile by profileViewModel.state.collectAsState()

                            ProfileScreen(
                                state = profile,
                                themeMode = themeMode,
                                onThemeChange = { newMode ->
                                    themeMode = newMode
                                    settingsStore.themeMode = newMode
                                },
                                onEdit = profileViewModel::startEdit,
                                onFirstNameChange = profileViewModel::onFirstNameChange,
                                onLastNameChange = profileViewModel::onLastNameChange,
                                onAvatarPicked = profileViewModel::onAvatarPicked,
                                onRemoveAvatar = profileViewModel::removeAvatar,
                                onCancel = profileViewModel::cancelEdit,
                                onSave = {
                                    profileViewModel.save(
                                        onSaved = viewModel::refresh
                                    )
                                }
                            )
                        }
                    )
                } else {
                    val welcomeState by welcomeViewModel.state.collectAsState()

                    WelcomeScreen(
                        state = welcomeState,
                        onFirstNameChange = welcomeViewModel::onFirstNameChange,
                        onLastNameChange = welcomeViewModel::onLastNameChange,
                        onAvatarPicked = welcomeViewModel::onAvatarPicked,
                        onRemoveAvatar = welcomeViewModel::removeAvatar,
                        onStart = {
                            welcomeViewModel.start(
                                onSaved = viewModel::refresh
                            )
                        }
                    )
                }
            }
        }
    }
}
