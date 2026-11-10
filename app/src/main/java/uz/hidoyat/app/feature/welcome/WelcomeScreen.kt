package uz.hidoyat.app.feature.welcome

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatIcon
import uz.hidoyat.app.core.ui.components.HidoyatIconType
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun WelcomeScreen(
    state: WelcomeUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onAvatarPicked: (Uri) -> Unit,
    onRemoveAvatar: () -> Unit,
    onStart: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val focusManager = LocalFocusManager.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onAvatarPicked(uri) }
    val pickImage = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeHero()

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarPicker(
                    state = state,
                    onPick = pickImage,
                    onRemove = onRemoveAvatar
                )
                Spacer(Modifier.height(24.dp))

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
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = onFirstNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.welcome_first_name)) },
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
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.lastName,
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
                            if (state.canStart) onStart()
                        }
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HidoyatPrimaryButton(
                text = stringResource(R.string.welcome_start),
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (state.firstName.isBlank()) R.string.welcome_name_hint
                    else R.string.welcome_privacy
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeHero() {
    val colors = HidoyatTheme.colors
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
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(colors.onHero.copy(alpha = 0.08f))
                    .border(1.5.dp, colors.gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                HidoyatIcon(HidoyatIconType.Crescent, colors.gold, size = 40.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onHero,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .size(width = 44.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.gold)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onHeroMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AvatarPicker(
    state: WelcomeUiState,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val avatarDesc = stringResource(R.string.welcome_avatar_desc)
    val bitmap = state.avatar
    val image = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .size(112.dp)
            .semantics { contentDescription = avatarDesc }
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(colors.goldContainer)
                .border(BorderStroke(2.dp, colors.gold), CircleShape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initial = state.firstName.trim().take(1).uppercase()
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    HidoyatIcon(
                        HidoyatIconType.Profile,
                        MaterialTheme.colorScheme.primary,
                        size = 44.dp
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(colors.gold)
                .clickable(onClick = onPick),
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
        TextButton(onClick = onPick) {
            Text(
                stringResource(
                    if (state.avatar == null) R.string.welcome_avatar_add
                    else R.string.welcome_avatar_change
                )
            )
        }
        if (state.avatar != null) {
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.welcome_avatar_remove), color = colors.textSecondary)
            }
        }
    }
}
