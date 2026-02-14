package com.fishai.chatzen.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.fishai.chatzen.ui.utils.scaleOnPress

import androidx.compose.ui.res.stringResource
import com.fishai.chatzen.R
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.fishai.chatzen.ui.theme.NavBarBackground
import com.fishai.chatzen.ui.theme.NavBarBorder

enum class Screen(val route: String, val icon: ImageVector, val labelResId: Int) {
    CHAT("chat", Icons.Default.Chat, R.string.screen_chat),
    USAGE("usage", Icons.Default.DataUsage, R.string.screen_usage),
    SETTINGS("settings", Icons.Default.Settings, R.string.screen_settings)
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onInputClick: () -> Unit,
    onLongInputClick: () -> Unit = {},
    isInputExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .height(80.dp),
        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else NavBarBackground,
        tonalElevation = 0.dp
    ) {
        // Custom layout for parallel items: Chat, Input, Usage
        // Use Box to ensure Chat and Usage buttons stay in fixed positions (left and right)
        // while Input button stays in the center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Chat Icon (Left aligned relative to center or absolute left?)
            // To be robust, let's use a Row with weighted spacers or Boxes.
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Slot
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val chatSelected = currentRoute == Screen.CHAT.route
                    val chatInteractionSource = remember { MutableInteractionSource() }
                    
                    IconButton(
                        onClick = { onNavigate(Screen.CHAT.route) },
                        modifier = Modifier.scaleOnPress(chatInteractionSource),
                        interactionSource = chatInteractionSource
                    ) {
                        Icon(
                            imageVector = Screen.CHAT.icon,
                            contentDescription = stringResource(Screen.CHAT.labelResId),
                            tint = if (chatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Center Slot
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Input Button (Center) - Visible on Chat and Usage screens
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentRoute == Screen.CHAT.route || currentRoute == Screen.USAGE.route,
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        // Use Surface + Icon to mimic FAB for custom gesture control
                        Surface(
                            modifier = Modifier
                                .width(52.dp)
                                .height(44.dp)
                                .scaleOnPress(interactionSource)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            val press = androidx.compose.foundation.interaction.PressInteraction.Press(offset)
                                            interactionSource.emit(press)
                                            val released = tryAwaitRelease()
                                            if (released) {
                                                interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
                                            } else {
                                                interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Cancel(press))
                                            }
                                        },
                                        onTap = { onInputClick() },
                                        onLongPress = { onLongInputClick() }
                                    )
                                },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 6.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_type))
                            }
                        }
                    }
                }

                // Right Slot
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val usageSelected = currentRoute == Screen.USAGE.route
                    val usageInteractionSource = remember { MutableInteractionSource() }
                    
                    IconButton(
                        onClick = { onNavigate(Screen.USAGE.route) },
                        modifier = Modifier.scaleOnPress(usageInteractionSource),
                        interactionSource = usageInteractionSource
                    ) {
                        Icon(
                            imageVector = Screen.USAGE.icon,
                            contentDescription = stringResource(Screen.USAGE.labelResId),
                            tint = if (usageSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
