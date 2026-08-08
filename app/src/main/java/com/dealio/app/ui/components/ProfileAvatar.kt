package com.dealio.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dealio.app.data.ApiResult
import com.dealio.app.data.AuthRepository
import com.dealio.app.data.TokenStore
import com.dealio.app.data.api.ApiClient
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealBright
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The signed-in person's picture, and the machinery to change it.
 *
 * Held outside any one screen's ViewModel because the picture belongs to the
 * account rather than to a role: the customer profile page and the builder's
 * settings page draw the same thing from the same place, and the builder screen
 * has no ViewModel at all. The current URL is read from — and written back to —
 * [TokenStore], which is the app's only copy of the signed-in user between
 * logins, so a new photo shows everywhere at once instead of after a re-login.
 */
class ProfileAvatarState(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val tokenStore = TokenStore(context)
    private val repo = AuthRepository(ApiClient.authApi, tokenStore)

    var url by mutableStateOf(tokenStore.avatarUrl)
        private set
    var uploading by mutableStateOf(false)
        private set

    /** Last thing that happened, for a snackbar. Read once and cleared. */
    var message by mutableStateOf<String?>(null)

    fun consumeMessage(): String? = message.also { message = null }

    fun upload(uri: Uri) {
        uploading = true
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    bytes?.let { it to (context.contentResolver.getType(uri) ?: "image/jpeg") }
                }.getOrNull()
            }
            if (picked == null) {
                uploading = false
                message = "Could not read that image."
                return@launch
            }
            val (bytes, mime) = picked
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            when (val r = repo.uploadAvatar(bytes, "avatar.$ext", mime)) {
                is ApiResult.Success -> {
                    url = r.data.avatarUrl
                    message = "Profile picture updated"
                }
                is ApiResult.Error -> message = r.message
            }
            uploading = false
        }
    }

    fun remove() {
        uploading = true
        scope.launch {
            when (val r = repo.removeAvatar()) {
                is ApiResult.Success -> {
                    url = null
                    message = "Profile picture removed"
                }
                is ApiResult.Error -> message = r.message
            }
            uploading = false
        }
    }
}

@Composable
fun rememberProfileAvatarState(scope: CoroutineScope): ProfileAvatarState {
    val context = LocalContext.current.applicationContext
    return remember(scope) { ProfileAvatarState(context, scope) }
}

/**
 * Round profile picture with a camera badge.
 *
 * The two targets do different things, which is the whole point of having two:
 * the **picture** opens it full screen, the **badge** opens the gallery to
 * replace it. Both used to launch the picker, so there was no way to actually
 * look at the photo you had just set — and tapping your own face to be asked for
 * a new file is a small hostility.
 *
 * With no photo yet there is nothing to view, so the picture falls back to
 * opening the picker: an empty avatar's only useful action is filling it. Same
 * rule the CP credential card uses.
 *
 * Falls back to the person's initials rather than a grey silhouette — an empty
 * avatar should still say who it belongs to.
 */
@Composable
fun ProfileAvatar(
    name: String?,
    state: ProfileAvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    ringColor: Color = Color.White.copy(alpha = 0.35f),
    badgeColor: Color = Teal,
) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(state::upload)
    }
    val photo = resolveUrl(state.url)
    var viewing by remember { mutableStateOf(false) }

    Box(modifier.size(size), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .border(2.dp, ringColor, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TealBright, Teal)))
                .clickable(enabled = !state.uploading) {
                    if (photo != null) viewing = true else pick.launch("image/*")
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.uploading -> CircularProgressIndicator(
                    Modifier.size(size / 3), color = Color.White, strokeWidth = 2.dp,
                )
                photo != null -> AsyncImage(
                    model = photo,
                    contentDescription = name,
                    modifier = Modifier.size(size).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                else -> Text(
                    initialsOf(name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.32f).sp,
                )
            }
        }
        // The badge is what makes the picture look changeable — a bare circle
        // reads as decoration, and nobody taps decoration.
        Box(
            Modifier
                .size(size / 3.2f)
                .background(badgeColor, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .clickable(enabled = !state.uploading) { pick.launch("image/*") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.PhotoCamera, "Change profile picture",
                tint = Color.White, modifier = Modifier.size(size / 6.5f),
            )
        }
    }

    if (viewing && photo != null) {
        FullScreenPhoto(
            url = photo,
            label = name,
            onChange = { viewing = false; pick.launch("image/*") },
            onDismiss = { viewing = false },
        )
    }
}

/**
 * The profile picture, big.
 *
 * Fitted rather than cropped — the avatar everywhere else is a circle with the
 * edges taken off, so the one place you open it deliberately is the one place
 * the whole photo should be visible.
 */
@Composable
private fun FullScreenPhoto(
    url: String,
    label: String?,
    onChange: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                // Tapping the backdrop closes, which is what every photo viewer
                // on the phone already does.
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            // A column rather than aligned children with inset padding: inside a
            // Dialog the window reports no system-bar insets, so
            // systemBarsPadding() adds nothing and a bottom-aligned button sits
            // half under the navigation bar. Fixed vertical padding and a
            // weighted image put the controls where they belong on any device.
            Column(
                Modifier
                    .fillMaxSize()
                    // Bottom clearance is deliberately larger than the top. A
                    // Dialog with usePlatformDefaultWidth = false fills the
                    // window, which extends under the navigation bar, and the
                    // window reports no insets to pad against — so a symmetric
                    // gap leaves the button under the gesture bar.
                    .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Outlined.Close, "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(onClick = onDismiss)
                            .padding(8.dp)
                            .size(22.dp),
                    )
                }
                AsyncImage(
                    model = url,
                    contentDescription = label,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    "Change photo",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onChange)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}
