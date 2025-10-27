package com.jumblemint.cows.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jumblemint.cows.R
import com.jumblemint.cows.data.model.User
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthService(
    private val context: Context,
    private val repository: CattleRepository
) {

    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: Flow<Boolean> = _isSignedIn.asStateFlow()
    
    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    init {
        // Check for local user first
        val localUser = getLocalUser()
        if (localUser != null) {
            _currentUser.value = localUser
            _isSignedIn.value = true
        }

        // Listen to Firebase auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                clearLocalUser()
                serviceScope.launch {
                    val user = buildUserFromFirebase(firebaseUser)
                    emitUser(user)
                }
            } else if (_currentUser.value == null) {
                // No Firebase user and no local user - create a local user
                signInAsLocalUser()
            }
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Firebase user is null")

            clearLocalUser()
            val user = buildUserFromFirebase(firebaseUser)
            emitUser(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut() {
        try {
            // Clear local user if exists
            if (_currentUser.value?.isLocalUser == true) {
                clearLocalUser()
                _currentUser.value = null
                _isSignedIn.value = false
            } else {
                // Sign out from Firebase and Google
                auth.signOut()
                googleSignInClient.signOut().await()
                
                // Automatically sign in as local user after signing out from Google
                signInAsLocalUser()
            }
        } catch (e: Exception) {
            // Log error but don't throw - we want to sign out locally even if remote fails
            // Still try to sign in as local user
            signInAsLocalUser()
        }
    }
    
    fun signInAsLocalUser(displayName: String? = null): User {
        val localUserId = prefs.getString("local_user_id", null) ?: UUID.randomUUID().toString()
        val user = User(
            uid = localUserId,
            email = "",
            displayName = displayName ?: "Local User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastSyncAt = 0L,
            isLocalUser = true,
            isPremium = false
        )
        
        // Save local user to preferences
        prefs.edit()
            .putString("local_user_id", localUserId)
            .putString("local_user_name", user.displayName)
            .putBoolean("is_local_user", true)
            .apply()
        
        _currentUser.value = user
        _isSignedIn.value = true
        
        return user
    }
    
    suspend fun startUserSync(syncService: com.jumblemint.cows.sync.SyncService) {
        val currentUser = _currentUser.value ?: run {
            println("Cannot start sync: No current user")
            return
        }
        
        // Only sync for non-local users (Google signed-in users)
        if (currentUser.isLocalUser) {
            println("Skipping sync for local user: ${currentUser.displayName}")
            return
        }
        
        println("Starting sync for user: ${currentUser.displayName} (${currentUser.uid})")
        
        try {
            // Start real-time sync for the user
            println("Starting real-time sync...")
            syncService.startRealtimeSync(currentUser.uid)
            
            // Perform initial sync
            println("Performing initial sync...")
            val result = syncService.syncUserData(currentUser.uid)
            if (result.isSuccess) {
                println("Sync completed successfully for user ${currentUser.uid}")
                val recordedSync = syncService.lastSyncTime.first()
                val syncTime = recordedSync ?: System.currentTimeMillis()
                _currentUser.value = _currentUser.value?.copy(lastSyncAt = syncTime)
            } else {
                val exception = result.exceptionOrNull()
                // Log sync failure but don't crash
                println("Sync failed for user ${currentUser.uid}: ${exception?.message}")
                println("Exception type: ${exception?.javaClass?.simpleName}")
                exception?.printStackTrace()
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            println("Failed to start sync for user ${currentUser.uid}: ${e.message}")
            println("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }
    }
    
    suspend fun signInAsDemoGoogleUser(): User {
        // Create a demo Google user for testing purposes
        val demoUser = User(
            uid = "demo_google_user_123",
            email = "demo@example.com",
            displayName = "Demo Google User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastSyncAt = System.currentTimeMillis(),
            isLocalUser = false,
            isPremium = false
        )
        
        // Clear any local user data
        clearLocalUser()
        
        _currentUser.value = demoUser
        _isSignedIn.value = true
        
        // Note: Demo user will use local data only, no sync needed
        
        return demoUser
    }
    
    private fun getLocalUser(): User? {
        if (!prefs.getBoolean("is_local_user", false)) return null
        
        val localUserId = prefs.getString("local_user_id", null) ?: return null
        val displayName = prefs.getString("local_user_name", "Local User")
        
        return User(
            uid = localUserId,
            email = "",
            displayName = displayName,
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastSyncAt = 0L,
            isLocalUser = true,
            isPremium = false
        )
    }
    
    private fun clearLocalUser() {
        prefs.edit()
            .remove("local_user_id")
            .remove("local_user_name")
            .remove("is_local_user")
            .apply()
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    private suspend fun buildUserFromFirebase(firebaseUser: FirebaseUser): User {
        val existing = repository.getUserById(firebaseUser.uid)
        val base = existing ?: firebaseUser.toUser()
        val updated = base.copy(
            email = firebaseUser.email ?: base.email,
            displayName = firebaseUser.displayName ?: base.displayName,
            photoUrl = firebaseUser.photoUrl?.toString() ?: base.photoUrl
        )

        if (existing == null) {
            repository.insertUser(updated)
        } else if (updated != existing) {
            repository.updateUser(updated)
        }

        return updated
    }

    private fun emitUser(user: User) {
        _currentUser.value = user
        _isSignedIn.value = true
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            uid = uid,
            email = email ?: "",
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            createdAt = System.currentTimeMillis()
        )
    }
}