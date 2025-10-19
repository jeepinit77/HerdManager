package com.jumblemint.cows.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.jumblemint.cows.R
import com.jumblemint.cows.ui.components.DataMergeDialog
import com.jumblemint.cows.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onNavigateBack: () -> Unit,
    onSignInSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isGoogleSignedIn = uiState.currentUser?.isLocalUser == false

    var pendingAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                pendingAccount = account
                coroutineScope.launch {
                    val (hasLocalData, hasServerData) = authViewModel.checkForExistingData(account.id)
                    if (hasLocalData || hasServerData) {
                        authViewModel.showDataMergeDialog(hasLocalData, hasServerData)
                    } else {
                        authViewModel.signInWithGoogle(account)
                    }
                }
            } catch (e: ApiException) {
                authViewModel.setError("Google sign-in failed: ${e.statusCode} - ${e.message}")
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            authViewModel.setError("Sign-in attempt failed.")
        }
    }

    LaunchedEffect(uiState.isSignedIn, uiState.currentUser) {
        if (uiState.isSignedIn && uiState.currentUser?.isLocalUser == false) {
            onSignInSuccess()
            authViewModel.clearError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isGoogleSignedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✓ Signed in as ${uiState.currentUser?.displayName ?: uiState.currentUser?.email ?: "User"}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your data is set to sync across devices.",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        } else {
            Text(
                text = "Sign in with your Google account to sync your cattle data across devices and enable collaboration features.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            val isFirebaseConfigured = remember {
                context.getString(R.string.default_web_client_id) != "YOUR_WEB_CLIENT_ID_HERE" &&
                        context.getString(R.string.default_web_client_id).isNotBlank()
            }

            if (isFirebaseConfigured) {
                Button(
                    onClick = {
                        val signInIntent = authViewModel.getGoogleSignInIntent(context)
                        launcher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading && pendingAccount != null) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(painter = painterResource(id = com.google.android.gms.base.R.drawable.googleg_standard_color_18), contentDescription = "Google logo", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "⚠️ Firebase Configuration Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Google Sign-In requires proper Firebase setup. Please ensure your `google-services.json` is correctly configured with an OAuth client ID for Google Sign-In.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
                Button(
                    onClick = { authViewModel.signInAsDemoGoogleUser() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(painter = painterResource(id = com.google.android.gms.base.R.drawable.googleg_standard_color_18), contentDescription = "Google logo", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Demo: Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "What Gets Synced:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val syncFeatures = listOf(
                    "Cattle records & details",
                    "Activities & health records",
                    "Pasture data & assignments",
                    "Notes & observations",
                    "Tag color configurations",
                    "Custom activity types",
                    "Automatic cloud backup",
                    "Real-time herd sharing (with other signed-in users)"
                )
                syncFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircleOutline, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(feature, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }
            }
        }

        uiState.error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!isGoogleSignedIn) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Continue without signing in", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.showDataMergeDialog) {
        DataMergeDialog(
            onDismiss = {
                authViewModel.hideDataMergeDialog()
                pendingAccount = null
            },
            onOptionSelected = { option ->
                pendingAccount?.let { account ->
                    authViewModel.signInWithDataMergeOption(account, option)
                }
            },
            hasLocalData = uiState.hasLocalData,
            hasServerData = uiState.hasServerData
        )
    }
}
