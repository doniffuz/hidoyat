package uz.hidoyat.app.feature.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatAvatar
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSecondaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEdit: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onAvatarPicked: (Uri) -> Unit,
    onRemoveAvatar: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    BackHandler(enabled = state.editing, onBack = onCancel)
    if (state.editing) {
        ProfileEdit(
            state, onFirstNameChange, onLastNameChange, onAvatarPicked,
            onRemoveAvatar, onCancel, onSave
        )
    } else {
        ProfileView(state, onEdit)
    }
}

@Composable
private fun ProfileView(state: ProfileUiState, onEdit: () -> Unit) {
    val colors = HidoyatTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(colors.heroStart, colors.heroEnd)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HidoyatAvatar(state.avatar, state.firstName, size = 120.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onHero,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .size(width = 44.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.gold)
                )
            }
        }
        HidoyatPrimaryButton(
            text = stringResource(R.string.profile_edit),
            onClick = onEdit,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

@Composable
private fun ProfileEdit(
    state: ProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onAvatarPicked: (Uri) -> Unit,
    onRemoveAvatar: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val focusManager = LocalFocusManager.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onAvatarPicked(uri) }
    val pickImage = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val avatarDesc = stringResource(R.string.welcome_avatar_desc)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.card,
        unfocusedContainerColor = colors.card,
        focusedBorderColor = colors.gold,
        unfocusedBorderColor = colors.cardBorder,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = colors.textSecondary,
        cursorColor = colors.gold,
        focusedTextColor = colors.onCard,
        unfocusedTextColor = colors.onCard
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HidoyatSectionTitle(title = stringResource(R.string.profile_edit))
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(112.dp)
                    .semantics { contentDescription = avatarDesc }
            ) {
                HidoyatAvatar(
                    bitmap = state.draftAvatar,
                    name = state.draftFirstName,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(onClick = pickImage)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(colors.gold)
                        .clickable(onClick = pickImage),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = pickImage) {
                    Text(
                        stringResource(
                            if (state.draftAvatar == null) R.string.welcome_avatar_add
                            else R.string.welcome_avatar_change
                        )
                    )
                }
                if (state.draftAvatar != null) {
                    TextButton(onClick = onRemoveAvatar) {
                        Text(
                            stringResource(R.string.welcome_avatar_remove),
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.draftFirstName,
                onValueChange = onFirstNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.welcome_first_name)) },
                isError = state.draftFirstName.isBlank(),
                supportingText = if (state.draftFirstName.isBlank()) {
                    { Text(stringResource(R.string.profile_name_required)) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draftLastName,
                onValueChange = onLastNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.welcome_last_name)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (state.canSave) onSave()
                    }
                )
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HidoyatSecondaryButton(
                    text = stringResource(R.string.profile_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                HidoyatPrimaryButton(
                    text = stringResource(R.string.profile_save),
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
