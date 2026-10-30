package com.lumen.messages.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.delay

@Composable
fun ContactScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val header = vm.header ?: return
    val stats = vm.contactStats
    var confirmDelete by remember { mutableStateOf(false) }
    val hasNumber = header.address.any { it.isDigit() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = { vm.back() }, size = 42.dp)
            }
            Rise(0) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(130.dp)
                            .background(
                                Brush.radialGradient(listOf(p.accentA.copy(alpha = 0.25f), Color.Transparent)),
                                CircleShape,
                            )
                    )
                    Avatar(header.name, header.address, header.photoUri, 96.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Rise(1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(header.title, style = LType.title.copy(color = p.text, fontSize = 22.sp), textAlign = TextAlign.Center)
                    if (header.name != null) {
                        Text(header.address, style = LType.snippet.copy(color = p.textDim))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Rise(2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Messages", stats?.total?.toString() ?: "–", Modifier.weight(1f))
                    StatTile("Received", stats?.received?.toString() ?: "–", Modifier.weight(1f))
                    StatTile("Sent", stats?.sent?.toString() ?: "–", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Rise(3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("OTPs", stats?.otps?.toString() ?: "–", Modifier.weight(1f))
                    StatTile("Bank alerts", stats?.transactions?.toString() ?: "–", Modifier.weight(1f))
                    StatTile(
                        "Since",
                        stats?.takeIf { it.total > 0 }?.let { Fmt.listTime(context, it.firstDate) } ?: "–",
                        Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Rise(4) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column {
                        if (hasNumber) {
                            ActionRow(Icons.Rounded.Call, "Call") {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(header.address))))
                                }
                            }
                            Line()
                        }
                        ActionRow(Icons.Rounded.ContentCopy, "Copy ${if (hasNumber) "number" else "sender ID"}") {
                            clipboard.setText(AnnotatedString(header.address))
                        }
                        if (header.name == null && hasNumber) {
                            Line()
                            ActionRow(Icons.Rounded.PersonAdd, "Add to contacts") {
                                runCatching {
                                    context.startActivity(
                                        Intent(ContactsContract.Intents.Insert.ACTION)
                                            .setType(ContactsContract.RawContacts.CONTENT_TYPE)
                                            .putExtra(ContactsContract.Intents.Insert.PHONE, header.address)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Rise(5) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    ActionRow(Icons.Rounded.Delete, "Delete conversation", danger = true) { confirmDelete = true }
                }
            }
            Spacer(Modifier.height(30.dp))
        }

        GlassConfirm(
            visible = confirmDelete,
            backdrop = null,
            title = "Delete this conversation?",
            message = "All messages with ${header.title} will be removed from this phone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                vm.deleteCurrentThread()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Rise(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(40L * index)
        a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 260f))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = a.value.coerceIn(0f, 1f)
            translationY = (1f - a.value) * 24.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = LType.micro.copy(color = p.textDim))
            Spacer(Modifier.height(2.dp))
            Text(value, style = LType.name.copy(color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) p.danger else p.text, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = LType.name.copy(color = if (danger) p.danger else p.text, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        if (!danger) Icon(Icons.Rounded.ChevronRight, null, tint = p.textFaint)
    }
}

@Composable
private fun Line() {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(start = 51.dp).height(1.dp).background(p.divider))
}
