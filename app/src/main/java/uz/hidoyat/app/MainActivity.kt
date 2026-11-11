package uz.hidoyat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import uz.hidoyat.app.core.ui.theme.HidoyatTheme
import uz.hidoyat.app.feature.home.HomeViewModel
import uz.hidoyat.app.feature.profile.ProfileScreen
import uz.hidoyat.app.feature.profile.ProfileViewModel
import uz.hidoyat.app.feature.welcome.WelcomeScreen
import uz.hidoyat.app.feature.welcome.WelcomeViewModel
import uz.hidoyat.app.navigation.HidoyatMainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()
    private val welcomeViewModel: WelcomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HidoyatTheme {
                val completed by welcomeViewModel.completed.collectAsState()
                if (completed) {
                    val state by viewModel.state.collectAsState()
                    HidoyatMainScreen(
                        homeState = state,
                        onToggleGoal = viewModel::toggleGoal,
                        onHomeShown = viewModel::refresh,
                        profileContent = {
                            val profile by profileViewModel.state.collectAsState()
                            ProfileScreen(
                                state = profile,
                                onEdit = profileViewModel::startEdit,
                                onFirstNameChange = profileViewModel::onFirstNameChange,
                                onLastNameChange = profileViewModel::onLastNameChange,
                                onAvatarPicked = profileViewModel::onAvatarPicked,
                                onRemoveAvatar = profileViewModel::removeAvatar,
                                onCancel = profileViewModel::cancelEdit,
                                onSave = { profileViewModel.save(onSaved = viewModel::refresh) }
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
                        onStart = { welcomeViewModel.start(onSaved = viewModel::refresh) }
                    )
                }
            }
        }
    }
}
