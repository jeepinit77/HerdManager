package com.jumblemint.cows.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.android.gms.common.api.ApiException
import com.jumblemint.cows.R
import com.jumblemint.cows.ui.components.DataMergeDialog
import com.jumblemint.cows.ui.components.DataMergeOption
import com.jumblemint.cows.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onNavigateBack: () -> Unit,
    onSignInSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is already signed in with Google
    val isGoogleSignedIn = uiState.currentUser?.isLocalUser == false
    val isLocalUser = uiState.currentUser?.isLocalUser == true
    
    // Store the account for data merge dialog
    var pendingAccount by remember { mutableStateOf<com.google.android.gms.auth.api.signin.GoogleSignInAccount?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                pendingAccount = account
                
                // Check for existing data and show merge dialog
                coroutineScope.launch {
                    val (hasLocalData, hasServerData) = authViewModel.checkForExistingData(account.id)
                    if (hasLocalData || hasServerData) {
                        authViewModel.showDataMergeDialog(hasLocalData, hasServerData)
                    } else {
                        // No data conflict, proceed with normal sign-in
                        authViewModel.signInWithGoogle(account)
                    }
                }
            } catch (e: ApiException) {
                authViewModel.setError("Google sign-in failed: ${e.statusCode} - ${e.message}")
            }
        } else {
            authViewModel.setError("Sign-in was cancelled")
        }
    }
    
    // Navigate on successful login
    LaunchedEffect(uiState.isSignedIn, uiState.currentUser) {
        if (uiState.isSignedIn && uiState.currentUser?.isLocalUser == false) {
            onSignInSuccess()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Sign In & Sync") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Logo/Icon
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Cattle Manager Logo",
                modifier = Modifier.size(100.dp)
            )
            
            // Title
            Text(
                text = if (isGoogleSignedIn) "Account & Sync" else "Sign In & Sync",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            // Debug Info Card (temporary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Debug Info:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Current User: ${uiState.currentUser?.displayName ?: "None"}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Is Local User: ${uiState.currentUser?.isLocalUser}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Is Signed In: ${uiState.isSignedIn}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Is Loading: ${uiState.isLoading}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Error: ${uiState.error ?: "None"}",
                        fontSize = 12.sp
                    )
                }
            }
            
            // Premium Notice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📢 Future Premium Feature",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign-in and sync features are currently free, but may become premium features in future updates. Enjoy them while they're available to everyone!",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // Conditional content based on sign-in status
            if (isGoogleSignedIn) {
                // Already signed in with Google
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✓ Signed in as ${uiState.currentUser?.displayName ?: "User"}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your data is syncing across devices",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Sync status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Data Sync Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your cattle data is automatically synced across all your devices. Any changes you make will be available on your other devices within minutes.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                
                // Sign out button
                OutlinedButton(
                    onClick = {
                        authViewModel.signOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
            } else {
                // Not signed in with Google (local user or no user)
                Text(
                    text = "Sign in to sync your cattle data across devices and collaborate with your team.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                // Check if Firebase is properly configured
                val isFirebaseConfigured = context.getString(R.string.default_web_client_id) != "YOUR_WEB_CLIENT_ID_HERE"
                
                if (isFirebaseConfigured) {
                    // Google Sign-In Button
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
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Placeholder - you'd want a Google icon
                                    contentDescription = "Google",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Firebase not configured - show demo button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "⚠️ Firebase Configuration Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Google Sign-In requires proper Firebase configuration. The google-services.json file needs to include OAuth client configuration for Google Sign-In to work.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    
                    // Demo button that simulates sign-in
                    Button(
                        onClick = {
                            authViewModel.signInAsDemoGoogleUser()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Demo: Sign in with Google",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Back button (always available)
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isGoogleSignedIn) "Back to Settings" else "Continue without signing in",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Error message
            uiState.error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Features list
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Sync Features:",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val features = listOf(
                        "Sync data across all your devices",
                        "Share herds with team members",
                        "Real-time collaboration",
                        "Automatic cloud backup",
                        "Access from anywhere"
                    )
                    
                    features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = feature,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Data Merge Dialog
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
                authViewModel.hideDataMergeDialog()
                pendingAccount = null
            },
            hasLocalData = uiState.hasLocalData,
            hasServerData = uiState.hasServerData
        )
    }
}