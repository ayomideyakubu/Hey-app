package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SocialViewModel
import com.example.ui.components.HeyBottomBar
import com.example.ui.components.HeyTab
import com.example.ui.components.HeyTopAppBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommentsSheet
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.UserProfileDetailScreen
import com.example.ui.theme.HeyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            if (currentUser == null) {
                HeyTheme {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                }
            } else {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: SocialViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var currentTab by remember { mutableStateOf(HeyTab.FEED) }
    var previousTab by remember { mutableStateOf(HeyTab.FEED) }
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()

    // Custom back handler for selected profile or tabs
    BackHandler(enabled = selectedUserProfile != null || currentTab != HeyTab.FEED) {
        if (selectedUserProfile != null) {
            viewModel.closeUserProfile()
        } else {
            currentTab = previousTab.takeIf { it != currentTab } ?: HeyTab.FEED
        }
    }

    HeyTheme(darkTheme = isDarkMode) {
        if (selectedUserProfile != null) {
            UserProfileDetailScreen(
                user = selectedUserProfile!!,
                viewModel = viewModel,
                onBackClick = { viewModel.closeUserProfile() },
                onPostClick = { post -> viewModel.openComments(post) },
                onMessageClick = {
                    val user = selectedUserProfile!!
                    viewModel.closeUserProfile()
                    viewModel.openChat(user)
                    previousTab = currentTab
                    currentTab = HeyTab.MESSAGES
                }
            )
        } else {
            Scaffold(
                topBar = {
                    if (currentTab != HeyTab.CREATE) {
                        HeyTopAppBar(
                            isDarkTheme = isDarkMode,
                            onToggleDarkTheme = { viewModel.toggleDarkMode() },
                            onMessagesClick = {
                                previousTab = currentTab
                                currentTab = HeyTab.MESSAGES
                            }
                        )
                    }
                },
                bottomBar = {
                    if (currentTab != HeyTab.CREATE) {
                        HeyBottomBar(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                if (tab != currentTab) {
                                    previousTab = currentTab
                                    currentTab = tab
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                when (currentTab) {
                    HeyTab.FEED -> {
                        FeedScreen(
                            viewModel = viewModel,
                            onCreatePostClick = {
                                previousTab = currentTab
                                currentTab = HeyTab.CREATE
                            },
                            onUserClick = { user ->
                                viewModel.openUserProfile(user)
                            },
                            onExploreClick = {
                                previousTab = currentTab
                                currentTab = HeyTab.EXPLORE
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    HeyTab.EXPLORE -> {
                        ExploreScreen(
                            viewModel = viewModel,
                            onUserClick = { user ->
                                viewModel.openUserProfile(user)
                            },
                            onPostClick = { post ->
                                viewModel.openComments(post)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    HeyTab.CREATE -> {
                        CreatePostScreen(
                            viewModel = viewModel,
                            onPostCreated = {
                                currentTab = HeyTab.FEED
                            },
                            onBackClick = {
                                currentTab = previousTab
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    HeyTab.MESSAGES -> {
                        MessagesScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    HeyTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onSignOutClick = {
                                performSignOut(context, credentialManager, coroutineScope)
                            },
                            onPostClick = { post ->
                                viewModel.openComments(post)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                // Real-time Comments Bottom Sheet
                CommentsSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeComments() }
                )
            }
        }
    }
}

fun performSignOut(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        }
    }
}
