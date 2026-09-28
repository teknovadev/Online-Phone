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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiBubbleBackground
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TeknovaPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TurkmenCyan
import com.example.ui.theme.TurkmenEmerald

private data class SuggestionPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector
)

@Composable
fun EmptyChatView(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        SuggestionPrompt(
            title = "Türkmenistan Kültürü & Tarihi",
            subtitle = "Tarih, gelenekler ve önemli yerler",
            prompt = "Türkmenistan'ın zengin tarihi, kültürü, gelenekleri ve gezilecek önemli yerleri hakkında kapsamlı bilgi ver.",
            icon = Icons.Default.Public
        ),
        SuggestionPrompt(
            title = "Yapay Zekâ & Girişimcilik Fikri",
            subtitle = "TEKNOVA vizyonuyla yenilikçi proje",
            prompt = "Mobil yazılım ve yapay zekâ odaklı geleceğe dönük, yenilikçi bir teknoloji startup fikri ve iş planı öner.",
            icon = Icons.Default.Lightbulb
        ),
        SuggestionPrompt(
            title = "Android & Kotlin Yazılımı",
            subtitle = "Jetpack Compose ve modern mimari",
            prompt = "Android Jetpack Compose ile modern, temiz ve yüksek performanslı kod yazımı için en iyi pratikleri ve kod örneğini göster.",
            icon = Icons.Default.Code
        ),
        SuggestionPrompt(
            title = "Profesyonel E-posta Taslağı",
            subtitle = "İş teklifi veya resmi yazışma",
            prompt = "Yeni bir kurumsal iş ortaklığı teklifi için profesyonel, etkileyici ve nazik bir e-posta taslağı hazırla.",
            icon = Icons.Default.Email
        ),
        SuggestionPrompt(
            title = "Türkmence - Türkçe Dil Desteği",
            subtitle = "Selamlaşma, deyimler ve çeviri",
            prompt = "Türkçe ve Türkmence dilleri arasındaki akrabalık, yaygın selamlaşma cümleleri ve günlük hayatta kullanılan kalıpları listele.",
            icon = Icons.Default.Language
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))

            // AI Glowing Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(TurkmenCyan.copy(alpha = 0.9f), TurkmenEmerald.copy(alpha = 0.7f), BackgroundDark)
                        )
                    )
                    .border(2.dp, TurkmenCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Türkmen AI",
                    tint = TextPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, TeknovaPurple.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TeknovaPurple)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TEKNOVA YAPAY ZEKÂ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TeknovaPurple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Türkmen AI",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Size her konuda rehberlik etmek için hazırız. Sorunuzu yazın veya aşağıdaki konulardan birini seçin.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Önerilen Konular",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = TurkmenCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )
        }

        items(suggestions) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AiBubbleBorder, RoundedCornerShape(16.dp))
                    .clickable { onSelectPrompt(item.prompt) }
                    .testTag("prompt_chip_${item.title.take(6).lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = AiBubbleBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, OutlineDark, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = TurkmenCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Seç",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
