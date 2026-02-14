package com.fishai.chatzen.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fishai.chatzen.ui.components.AppBottomNavigation
import com.fishai.chatzen.ui.components.Screen
import com.fishai.chatzen.ui.screens.ChatScreen
import com.fishai.chatzen.ui.screens.ChatViewModel
import com.fishai.chatzen.ui.screens.SettingsScreen
import com.fishai.chatzen.ui.screens.SettingsViewModel
import com.fishai.chatzen.ui.screens.UsageScreen

import com.fishai.chatzen.ui.screens.UsageViewModel

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.fishai.chatzen.ui.screens.ServiceProvidersListScreen
import com.fishai.chatzen.ui.screens.CustomProviderListScreen
import com.fishai.chatzen.ui.screens.ProviderDetailScreen
import com.fishai.chatzen.ui.screens.CustomProviderDetailScreen
import com.fishai.chatzen.ui.screens.WebSearchSettingsScreen
import com.fishai.chatzen.ui.screens.OpenClawSettingsScreen
import com.fishai.chatzen.ui.screens.OcrSettingsScreen
import com.fishai.chatzen.ui.screens.ThemeSettingsScreen
import com.fishai.chatzen.ui.screens.AboutScreen
import com.fishai.chatzen.ui.screens.WebViewScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ChatZenApp(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    usageViewModel: UsageViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.CHAT.route
    val chatUiState by chatViewModel.uiState.collectAsState()
    var isBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.CHAT.route) {
            isBottomBarVisible = true
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomBarVisible && currentRoute != Screen.SETTINGS.route && 
                          currentRoute != "service_providers" && 
                          currentRoute != "custom_provider_list" &&
                          currentRoute != "provider_detail" &&
                          currentRoute != "custom_provider_detail" &&
                          currentRoute != "web_search_settings" &&
                          currentRoute != "openclaw_settings" &&
                          currentRoute != "ocr_settings" &&
                          currentRoute != "theme_settings" &&
                          currentRoute != "about_screen" &&
                          currentRoute?.startsWith("webview_screen") != true,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.shrinkVertically()
            ) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onInputClick = {
                        if (currentRoute != Screen.CHAT.route) {
                            navController.navigate(Screen.CHAT.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        chatViewModel.setInputExpanded(true)
                    },
                    onLongInputClick = {
                        val url = chatUiState.openClawWebUiUrl
                        if (!url.isNullOrEmpty()) {
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            navController.navigate("webview_screen?url=$encodedUrl")
                        }
                    },
                    isInputExpanded = chatUiState.isInputExpanded
                )
            }
        },
        floatingActionButton = {}, // FAB is now integrated into BottomNavigation
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.CHAT.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 }
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 }
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }
                )
            }
        ) {
            composable(Screen.CHAT.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onSettingsClick = {
                        // Collapse input box if it's expanded before navigating
                        if (chatUiState.isInputExpanded) {
                            chatViewModel.setInputExpanded(false)
                        }
                        navController.navigate(Screen.SETTINGS.route)
                    },
                    onNavigateToWebView = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("webview_screen?url=$encodedUrl")
                    }
                )
            }
            composable(Screen.USAGE.route) {
                UsageScreen(viewModel = usageViewModel)
            }
            composable(Screen.SETTINGS.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToServiceProviders = { navController.navigate("service_providers") },
                    onNavigateToWebSearchSettings = { navController.navigate("web_search_settings") },
                    onNavigateToOpenClawSettings = { navController.navigate("openclaw_settings") },
                    onNavigateToOcrSettings = { navController.navigate("ocr_settings") },
                    onNavigateToThemeSettings = { navController.navigate("theme_settings") },
                    onNavigateToAbout = { navController.navigate("about_screen") }
                )
            }
            composable("service_providers") {
                ServiceProvidersListScreen(
                    viewModel = settingsViewModel,
                    onNavigateToDetail = { isCustom ->
                        if (isCustom) {
                            navController.navigate("custom_provider_detail")
                        } else {
                            navController.navigate("provider_detail")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("custom_provider_list") {
                CustomProviderListScreen(
                    viewModel = settingsViewModel,
                    onNavigateToDetail = {
                        navController.navigate("custom_provider_detail")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("provider_detail") {
                ProviderDetailScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("custom_provider_detail") {
                CustomProviderDetailScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("web_search_settings") {
                WebSearchSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("theme_settings") {
                ThemeSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("openclaw_settings") {
                OpenClawSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ocr_settings") {
                OcrSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about_screen") {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "webview_screen?url={url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                WebViewScreen(
                    url = url,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
