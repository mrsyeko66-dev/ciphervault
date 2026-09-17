package com.kafappstore.ciphervault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.kafappstore.ciphervault.data.AppLockType
import com.kafappstore.ciphervault.ui.components.AppAuthenticationOverlay
import com.kafappstore.ciphervault.ui.components.BiometricLockOverlay
import com.kafappstore.ciphervault.util.BiometricAuthHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kafappstore.ciphervault.data.SettingsRepository
import com.kafappstore.ciphervault.data.db.CipherVaultDatabase
import com.kafappstore.ciphervault.data.db.ProjectRepository
import com.kafappstore.ciphervault.ui.components.MatrixRainCanvas
import com.kafappstore.ciphervault.ui.screens.AboutScreen
import com.kafappstore.ciphervault.ui.screens.CyberSplashScreen
import com.kafappstore.ciphervault.ui.screens.DecryptScreen
import com.kafappstore.ciphervault.ui.screens.EditorScreen
import com.kafappstore.ciphervault.ui.screens.EncryptScreen
import com.kafappstore.ciphervault.ui.screens.ProjectsScreen
import com.kafappstore.ciphervault.ui.screens.SettingsScreen
import com.kafappstore.ciphervault.ui.theme.CipherVaultTheme
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.viewmodel.CipherViewModel

class MainActivity : FragmentActivity() {

    private var activeViewModel: CipherViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(applicationContext)
        val database = CipherVaultDatabase.getDatabase(applicationContext)
        val projectRepo = ProjectRepository(database.projectDao())

        // Apply initial FLAG_SECURE based on security settings
        if (settingsRepo.settings.value.screenSecurityEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            val viewModel: CipherViewModel = viewModel(
                factory = CipherViewModel.Factory(settingsRepo, projectRepo)
            )
            activeViewModel = viewModel

            androidx.compose.runtime.LaunchedEffect(Unit) {
                handleIntent(intent, viewModel)
                viewModel.initAppLock()
            }

            val settings by viewModel.settings.collectAsState()
            val isShowingSplash by viewModel.isShowingSplash.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()

            // Dynamic FLAG_SECURE synchronization with user preference
            androidx.compose.runtime.LaunchedEffect(settings.screenSecurityEnabled) {
                if (settings.screenSecurityEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Trigger biometric prompt only if lockType is BIOMETRIC when locked
            androidx.compose.runtime.LaunchedEffect(isAppLocked, settings.lockType) {
                if (isAppLocked && settings.lockType == AppLockType.BIOMETRIC) {
                    triggerBiometricAuth(viewModel)
                }
            }

            CipherVaultTheme(themeMode = settings.themeMode) {
                if (isAppLocked) {
                    AppAuthenticationOverlay(
                        settings = settings,
                        onBiometricClick = {
                            triggerBiometricAuth(viewModel)
                        },
                        onVerifyPasscode = { passcode ->
                            viewModel.verifyPasscode(passcode)
                        },
                        onUnlockSuccess = {
                            viewModel.unlockApp()
                        }
                    )
                } else {
                    AnimatedContent(
                        targetState = isShowingSplash,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "splash_to_main_transition"
                    ) { showingSplash ->
                        if (showingSplash) {
                            CyberSplashScreen(
                                onFinish = { viewModel.finishSplash() }
                            )
                        } else {
                            MainAppScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeViewModel?.let { vm ->
            if (vm.settings.value.biometricLockEnabled && !vm.isShowingSplash.value) {
                vm.lockApp()
            }
        }
    }

    private fun triggerBiometricAuth(viewModel: CipherViewModel) {
        if (BiometricAuthHelper.isBiometricAvailable(this)) {
            BiometricAuthHelper.promptBiometric(
                activity = this,
                onSuccess = {
                    viewModel.unlockApp()
                },
                onError = { _ ->
                    // Keep locked
                }
            )
        } else {
            // If device has no biometric or PIN enrolled, unlock automatically with security notice
            viewModel.unlockApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeViewModel?.let { vm ->
            handleIntent(intent, vm)
        }
    }

    private fun handleIntent(intent: Intent?, viewModel: CipherViewModel) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.handleIncomingSharedText(sharedText)
                        viewModel.finishSplash()
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                if (dataUri != null) {
                    viewModel.handleIncomingFileUri(dataUri, contentResolver)
                    viewModel.finishSplash()
                }
            }
        }
    }
}

enum class MainBottomNav {
    TERMINAL,
    EDITOR,
    PROJECTS,
    SETTINGS,
    ABOUT
}

@Composable
fun MainAppScreen(viewModel: CipherViewModel) {
    var selectedBottomNav by remember { mutableStateOf(MainBottomNav.TERMINAL) }
    var selectedCryptoTab by remember { mutableIntStateOf(0) } // 0: Encrypt, 1: Decrypt

    val pendingNav by viewModel.pendingNavigateTab.collectAsState()
    LaunchedEffect(pendingNav) {
        pendingNav?.let { tabIndex ->
            selectedBottomNav = MainBottomNav.TERMINAL
            selectedCryptoTab = tabIndex
            viewModel.clearPendingNavigation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle Matrix Digital Rain Canvas Overlay
        MatrixRainCanvas(alpha = 0.09f)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CipherTopBar()
            },
            bottomBar = {
                CipherBottomNavigation(
                    currentScreen = selectedBottomNav,
                    onScreenSelected = { selectedBottomNav = it }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = selectedBottomNav,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bottom_nav_transition"
                ) { targetNav ->
                    when (targetNav) {
                        MainBottomNav.TERMINAL -> {
                            TerminalMainContainer(
                                selectedTab = selectedCryptoTab,
                                onTabSelected = { selectedCryptoTab = it },
                                onNavigateToEditor = { selectedBottomNav = MainBottomNav.EDITOR },
                                viewModel = viewModel
                            )
                        }
                        MainBottomNav.EDITOR -> {
                            EditorScreen(
                                viewModel = viewModel,
                                onNavigateToEncrypt = {
                                    selectedBottomNav = MainBottomNav.TERMINAL
                                    selectedCryptoTab = 0
                                }
                            )
                        }
                        MainBottomNav.PROJECTS -> {
                            ProjectsScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { selectedBottomNav = MainBottomNav.EDITOR },
                                onNavigateToEncrypt = {
                                    selectedBottomNav = MainBottomNav.TERMINAL
                                    selectedCryptoTab = 0
                                }
                            )
                        }
                        MainBottomNav.SETTINGS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                        MainBottomNav.ABOUT -> {
                            AboutScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CipherTopBar() {
    Surface(
        color = Color(0xEE09110B),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(BorderStroke(1.dp, MatrixBorderNeon))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.5.dp, MatrixGreenPrimary), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1C13)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ciphervault_logo),
                        contentDescription = "Application Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "CIPHERVAULT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MatrixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "V3.5 // STREAMING & PROJECTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6B8A74),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Online Security Status indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F2618))
                    .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MatrixGreenPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SECURED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MatrixGreenPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun TerminalMainContainer(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToEditor: () -> Unit,
    viewModel: CipherViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Two Big Primary Tabs: "ENCRYPT" and "DECRYPT"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xCC0A140E),
            contentColor = MatrixGreenPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (selectedTab == 0) MatrixGreenPrimary else CyberCyan,
                    height = 3.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MatrixBorderNeon)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (selectedTab == 0) MatrixGreenPrimary else Color(0xFF5A7864),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENCRYPT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (selectedTab == 0) MatrixGreenPrimary else Color(0xFF5A7864),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("tab_encrypt")
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (selectedTab == 1) CyberCyan else Color(0xFF5A7864),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DECRYPT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (selectedTab == 1) CyberCyan else Color(0xFF5A7864),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("tab_decrypt")
            )
        }

        // Active Tab Screen Content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_content_transition"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> EncryptScreen(viewModel = viewModel, onNavigateToEditor = onNavigateToEditor)
                1 -> DecryptScreen(viewModel = viewModel, onNavigateToEditor = onNavigateToEditor)
            }
        }
    }
}

@Composable
private fun CipherBottomNavigation(
    currentScreen: MainBottomNav,
    onScreenSelected: (MainBottomNav) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xF0080F0B),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MatrixBorderNeon))
            .navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = currentScreen == MainBottomNav.TERMINAL,
            onClick = { onScreenSelected(MainBottomNav.TERMINAL) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Terminal",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = MatrixGreenPrimary,
                indicatorColor = MatrixGreenPrimary,
                unselectedIconColor = Color(0xFF6B8A74),
                unselectedTextColor = Color(0xFF6B8A74)
            ),
            modifier = Modifier.testTag("nav_terminal")
        )

        NavigationBarItem(
            selected = currentScreen == MainBottomNav.EDITOR,
            onClick = { onScreenSelected(MainBottomNav.EDITOR) },
            icon = {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Editor",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Editor",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = MatrixGreenPrimary,
                indicatorColor = MatrixGreenPrimary,
                unselectedIconColor = Color(0xFF6B8A74),
                unselectedTextColor = Color(0xFF6B8A74)
            ),
            modifier = Modifier.testTag("nav_editor")
        )

        NavigationBarItem(
            selected = currentScreen == MainBottomNav.PROJECTS,
            onClick = { onScreenSelected(MainBottomNav.PROJECTS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Projects",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = MatrixGreenPrimary,
                indicatorColor = MatrixGreenPrimary,
                unselectedIconColor = Color(0xFF6B8A74),
                unselectedTextColor = Color(0xFF6B8A74)
            ),
            modifier = Modifier.testTag("nav_projects")
        )

        NavigationBarItem(
            selected = currentScreen == MainBottomNav.SETTINGS,
            onClick = { onScreenSelected(MainBottomNav.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = MatrixGreenPrimary,
                indicatorColor = MatrixGreenPrimary,
                unselectedIconColor = Color(0xFF6B8A74),
                unselectedTextColor = Color(0xFF6B8A74)
            ),
            modifier = Modifier.testTag("nav_settings")
        )

        NavigationBarItem(
            selected = currentScreen == MainBottomNav.ABOUT,
            onClick = { onScreenSelected(MainBottomNav.ABOUT) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = MatrixGreenPrimary,
                indicatorColor = MatrixGreenPrimary,
                unselectedIconColor = Color(0xFF6B8A74),
                unselectedTextColor = Color(0xFF6B8A74)
            ),
            modifier = Modifier.testTag("nav_about")
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "CipherVault: $name", modifier = modifier)
}
