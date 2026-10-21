package com.sahandsoft.englishlearner.ui.screens.tools

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.SahandTopBar

@Composable
fun AboutScreen(nav: NavHostController) {
    val context = LocalContext.current

    fun openTelegram() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SahandSoft")))
        } catch (_: Exception) {
        }
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "دربارهٔ برنامه", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(
                        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF0F766E), androidx.compose.ui.graphics.Color(0xFF115E59))),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📚", style = MaterialTheme.typography.displayLarge)
            }
            Text("زبان‌آموز سهند", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "نسخهٔ ۱.۰.۰ — یادگیری انگلیسی برای فارسی‌زبانان، کاملاً آفلاین",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("محتوای آموزشی", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("• ۵۰۰ واژه با معنی، تلفظ فارسی و جملهٔ مثال", style = MaterialTheme.typography.bodyMedium)
                    Text("• ۳۰ داستان آموزشی در سه سطح", style = MaterialTheme.typography.bodyMedium)
                    Text("• ۱۰ کتاب داستان، هرکدام ۵۰ صفحه", style = MaterialTheme.typography.bodyMedium)
                    Text("• ۱۶ درس گرامر با تمرین و آزمون", style = MaterialTheme.typography.bodyMedium)
                    Text("• ۱۰ دیالوگ مکالمهٔ واقعی", style = MaterialTheme.typography.bodyMedium)
                    Text("• بازی‌ها، مرور هوشمند و آزمون‌های بی‌پایان", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("سازنده", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("دانیال افشاری", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    EnText("Danial Afshari", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تلگرام: ", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { openTelegram() }) {
                            EnText("t.me/SahandSoft", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("🔒", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "حریم خصوصی کودکان",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "این برنامه برای کودکان هم ایمن است: بدون تبلیغ، بدون خرید درون‌برنامه‌ای، بدون ثبت‌نام و حساب کاربری، بدون اینترنت و بدون ارسال یا ذخیرهٔ هیچ داده‌ای بیرون از دستگاه.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("🔊", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "دربارهٔ تلفظ (شفاف باشید!)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "صدای تلفظ از موتور گفتار (TTS) خود دستگاه شما پخش می‌شود. کیفیت و در دسترس بودن صدا به موتور نصب‌شده روی گوشی بستگی دارد و ممکن است تلفظ در همهٔ دستگاه‌ها طبیعی و کامل نباشد. اگر صدا شنیده نشد، از تنظیمات گوشی موتور گفتار انگلیسی را نصب یا به‌روزرسانی کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("قدردانی", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فونت وزیرمتن (Vazirmatn) با مجوز آزاد OFL استفاده شده است. با تشکر از سازندگان این فونت زیبا.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "ساخته‌شده با ❤️ برای زبان‌آموزان ایرانی",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
