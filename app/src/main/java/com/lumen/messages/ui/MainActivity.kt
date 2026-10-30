@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.lumen.messages.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.lumen.messages.data.ThemeMode
import com.lumen.messages.ui.screens.MoneyScreen
import com.lumen.messages.ui.screens.SettingsScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lumen.messages.notify.Notifier
import com.lumen.messages.ui.glass.AuroraBackground
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.screens.ConversationListScreen
import com.lumen.messages.ui.screens.NewMessageScreen
import com.lumen.messages.ui.screens.OnboardingScreen
import com.lumen.messages.ui.screens.ThreadScreen
import com.lumen.messages.ui.theme.LumenTheme
import com.lumen.messages.ui.theme.LumenSurface
import com.lumen.messages.ui.theme.surfaceFor
import com.lumen.messages.ui.screens.ContactScreen
import com.lumen.messages.ui.screens.BottomTabBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val surface = surfaceFor(vm.prefs.themeMode, systemDark)
            val dark = surface != LumenSurface.White
            // Status-bar icons must contrast with the chosen screen colour, not the system one.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            LumenTheme(vm.prefs, surface) {
                LumenRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshState()
    }

    /** Opens a conversation from a notification, or from other apps (sms: links, Share). */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val threadId = intent.getLongExtra(Notifier.EXTRA_THREAD, -1L)
        if (threadId > 0) {
            vm.openThreadById(threadId, fromNotification = true)
            return
        }
        when (intent.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                val data: Uri = intent.data ?: return
                if (data.scheme !in setOf("sms", "smsto", "mms", "mmsto")) return
                val ssp = data.schemeSpecificPart.orEmpty()
                val address = ssp.substringBefore('?').split(',', ';').firstOrNull()?.trim().orEmpty()
                val body = intent.getStringExtra("sms_body")
                    ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: ssp.substringAfter("body=", "").takeIf { it.isNotEmpty() }?.let { Uri.decode(it) }
                if (address.isNotEmpty()) vm.startConversationWith(address, body)
                else vm.openNewMessage(body)
            }
            Intent.ACTION_SEND -> {
                vm.openNewMessage(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
        }
    }
}

@Composable
fun LumenRoot(vm: AppViewModel) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refreshState() }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.refreshState()
        // Becoming the default SMS app grants SMS access; ask for the rest right away.
        if (vm.isDefaultSms) permissionLauncher.launch(vm.requiredPermissions())
    }

    val requestDefault: () -> Unit = {
        val rm = context.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
        } else {
            vm.refreshState()
        }
    }
    val requestPermissions: () -> Unit = { permissionLauncher.launch(vm.requiredPermissions()) }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        val showOnboarding = !vm.onboarded || !vm.hasSmsAccess
        AnimatedContent(
            targetState = showOnboarding,
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(spring(0.85f, 200f), initialScale = 1.06f))
                    .togetherWith(fadeOut(tween(300)) + scaleOut(tween(400), targetScale = 0.92f))
            },
            label = "onboarding",
        ) { onboarding ->
            if (onboarding) {
                OnboardingScreen(
                    isDefault = vm.isDefaultSms,
                    hasSms = vm.hasSmsAccess,
                    extrasGranted = vm.hasSmsAccess && vm.hasContacts,
                    onMakeDefault = requestDefault,
                    onGrant = requestPermissions,
                    onContinue = { vm.finishOnboarding() },
                )
            } else {
                MainScreens(vm, requestDefault, requestContacts = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                })
            }
        }
    }
}

@Composable
private fun MainScreens(vm: AppViewModel, requestDefault: () -> Unit, requestContacts: () -> Unit) {
    BackHandler(enabled = vm.screen != Screen.List) { vm.back() }

    val rootBackdrop = rememberBackdropState()
    Box(Modifier.fillMaxSize()) {
        SharedTransitionLayout(Modifier.fillMaxSize().backdropSource(rootBackdrop)) {
            AnimatedContent(
                targetState = vm.screen,
                transitionSpec = {
                    val from = initialState.depth
                    val to = targetState.depth
                    val t: ContentTransform = when {
                        // Switching tabs: soft cross-fade with a hint of depth.
                        from == to -> (fadeIn(tween(260, delayMillis = 60)) + scaleIn(spring(0.9f, 380f), initialScale = 0.985f))
                            .togetherWith(fadeOut(tween(140)))
                        // Going deeper: the new screen glides in, the old one eases back.
                        to > from -> (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { (it * 0.35f).toInt() } +
                            fadeIn(tween(220, delayMillis = 30)))
                            .togetherWith(
                                slideOutHorizontally(spring(dampingRatio = 1f, stiffness = 420f)) { -(it * 0.12f).toInt() } +
                                    fadeOut(tween(180)) + scaleOut(tween(300), targetScale = 0.96f)
                            )
                        else -> (slideInHorizontally(spring(dampingRatio = 1f, stiffness = 420f)) { -(it * 0.12f).toInt() } +
                            fadeIn(tween(240)) + scaleIn(spring(0.9f, 420f), initialScale = 0.96f))
                            .togetherWith(
                                slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { (it * 0.35f).toInt() } +
                                    fadeOut(tween(160))
                            )
                    }
                    t.targetContentZIndex = if (to >= from) 1f else 0f
                    t.using(SizeTransform(clip = false))
                },
                label = "screens",
            ) { target ->
                when (target) {
                    Screen.List -> ConversationListScreen(
                        vm = vm,
                        sharedScope = this@SharedTransitionLayout,
                        animScope = this@AnimatedContent,
                        onRequestDefault = requestDefault,
                    )
                    is Screen.Thread -> ThreadScreen(
                        vm = vm,
                        threadId = target.threadId,
                        sharedScope = this@SharedTransitionLayout,
                        animScope = this@AnimatedContent,
                    )
                    is Screen.NewMessage -> NewMessageScreen(vm, onRequestContacts = requestContacts)
                    Screen.Money -> MoneyScreen(vm)
                    Screen.Settings -> SettingsScreen(vm, onRequestDefault = requestDefault)
                    is Screen.Contact -> ContactScreen(vm)
                }
            }
        }

        // Floating glass tab bar, only on the three main tabs.
        AnimatedVisibility(
            visible = vm.screen.depth == 0 && vm.selection.isEmpty(),
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomTabBar(
                current = vm.screen,
                onSelect = vm::selectTab,
                onCompose = { vm.openNewMessage() },
                backdrop = rootBackdrop,
            )
        }
    }
}
