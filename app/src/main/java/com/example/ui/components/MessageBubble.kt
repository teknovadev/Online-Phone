package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.AiBubbleBackground
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TurkmenCyan
import com.example.ui.theme.TurkmenEmerald
import com.example.ui.theme.UserBubbleGradientEnd
import com.example.ui.theme.UserBubbleGradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageEntity,
    isLastAssistantMessage: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val timeFormatted = formatTime(message.timestamp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(if (isUser) "user_message_row" else "ai_message_row"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(TurkmenCyan, TurkmenEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Türkmen AI",
                        tint = BackgroundDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Body
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                if (isUser) {
                    UserMessageCard(
                        content = message.content,
                        timeFormatted = timeFormatted
                    )
                } else {
                    AiMessageCard(
                        content = message.content,
                        isError = message.isError,
                        timeFormatted = timeFormatted,
                        isLastAssistantMessage = isLastAssistantMessage,
                        onCopy = { onCopy(message.content) },
                        onRegenerate = onRegenerate
                    )
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Kullanıcı",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageCard(
    content: String,
    timeFormatted: String
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 4.dp
    )

    Column(
        modifier = Modifier
            .clip(bubbleShape)
            .background(
                Brush.linearGradient(
                    listOf(UserBubbleGradientStart, UserBubbleGradientEnd)
                )
            )
            .border(width = 1.dp, color = TurkmenCyan.copy(alpha = 0.4f), shape = bubbleShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SelectionContainer {
            Text(
                text = content,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = timeFormatted,
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun AiMessageCard(
    content: String,
    isError: Boolean,
    timeFormatted: String,
    isLastAssistantMessage: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp
    )

    Column(
        modifier = Modifier
            .clip(bubbleShape)
            .background(if (isError) ErrorRed.copy(alpha = 0.12f) else AiBubbleBackground)
            .border(
                width = 1.dp,
                color = if (isError) ErrorRed.copy(alpha = 0.6f) else AiBubbleBorder,
                shape = bubbleShape
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Hata",
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Yanıt Üretilemedi",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SelectionContainer {
            Text(
                text = content,
                color = if (isError) TextPrimary.copy(alpha = 0.9f) else TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontFamily = FontFamily.Default
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom action row: Timestamp, Copy button, Regenerate button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeFormatted,
                color = TextTertiary,
                fontSize = 11.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCopy() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("copy_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = TurkmenCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kopyala",
                        color = TurkmenCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isLastAssistantMessage) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Regenerate button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRegenerate() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("regenerate_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yeniden Dene",
                            tint = TurkmenEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Yeniden Dene",
                            color = TurkmenEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
