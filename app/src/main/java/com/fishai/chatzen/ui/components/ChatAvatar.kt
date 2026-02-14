package com.fishai.chatzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fishai.chatzen.data.model.Role
import java.util.Locale

@Composable
fun ChatAvatar(
    role: Role,
    modelName: String? = null,
    userAvatarUri: String? = null,
    modifier: Modifier = Modifier
) {
    // Fixed shape, independent of global settings
    val shape = RoundedCornerShape(12.dp)
    
    val containerColor = if (role == Role.USER) 
        MaterialTheme.colorScheme.secondaryContainer 
    else 
        Color.Black
        
    val contentColor = if (role == Role.USER)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        Color.White

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, if (role == Role.USER) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, shape),
        contentAlignment = Alignment.Center
    ) {
        if (role == Role.USER) {
            if (userAvatarUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userAvatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "I",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        } else {
            // AI Avatar
            val letter = modelName?.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "A"
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
