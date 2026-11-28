package com.ivanjordao.hidrata.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivanjordao.hidrata.R
import com.ivanjordao.hidrata.logic.DayState
import com.ivanjordao.hidrata.logic.Entry
import com.ivanjordao.hidrata.logic.Hydration
import com.ivanjordao.hidrata.logic.Snapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
private val cardShape = RoundedCornerShape(22.dp)

/** Equivalente ao .card do CSS: fundo translúcido, borda fina e cantos arredondados. */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    background: Brush = Brush.linearGradient(listOf(C.card, C.card)),
    borderColor: Color = C.stroke,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(cardShape).background(background).border(1.dp, borderColor, cardShape).padding(18.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(title: String, small: String = "") {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = C.text, modifier = Modifier.weight(1f))
        Text(small, color = C.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- Cabeçalho ----------------
@Composable
fun Header(now: LocalDateTime, onSettings: () -> Unit) {
    val h = now.hour
    val hello = if (h < 12) "Bom dia ☀️" else if (h < 18) "Boa tarde 🌤️" else "Boa noite 🌙"
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)).replaceFirstChar { it.titlecase(ptBR) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(hello, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = C.text)
            Text(date, fontSize = 14.sp, color = C.muted)
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(C.card)
                .border(1.dp, C.stroke, RoundedCornerShape(14.dp)).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) { Text("⚙️", fontSize = 20.sp) }
    }
}

// ---------------- Copo com água animada ----------------
@Composable
fun Glass(drunk: Int, goal: Int, bumpKey: Int) {
    val level by animateFloatAsState((drunk.toFloat() / goal).coerceIn(0f, 1f), tween(1200, easing = FastOutSlowInEasing), label = "nivel")
    val inf = rememberInfiniteTransition(label = "onda")
    val phase by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "fase")
    val phase2 by inf.animateFloat(1f, 0f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "fase2")
    val scale = remember { Animatable(1f) }
    LaunchedEffect(bumpKey) {
        if (bumpKey > 0) {
            scale.animateTo(1.05f, tween(150)); scale.animateTo(.98f, tween(150)); scale.animateTo(1f, tween(200))
        }
    }

    Box(
        Modifier.size(236.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(24.dp, CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x17FFFFFF), Color(0x05FFFFFF))))
            .border(6.dp, Color(0x14FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (level > 0f) {
                val top = size.height * (1f - level)
                drawPath(wave(size, top - 4.dp.toPx(), phase2, 5.dp.toPx()), C.w1.copy(alpha = .45f))
                drawPath(
                    wave(size, top, phase, 6.dp.toPx()),
                    Brush.verticalGradient(listOf(C.w1, C.w2), startY = top, endY = size.height),
                )
            }
            // brilho do vidro
            drawRect(Brush.linearGradient(listOf(Color(0x29FFFFFF), Color.Transparent), Offset.Zero, Offset(size.width * .55f, size.height * .55f)))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Hydration.num(drunk), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 48.sp)
            Text("ml", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .85f))
            Text("de ${Hydration.fmt(goal)}", fontSize = 14.sp, color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(top = 6.dp))
            Text(
                "${(drunk * 100.0 / goal).roundToInt()}%",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.padding(top = 10.dp).clip(CircleShape).background(Color(0x29FFFFFF)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Superfície da água: uma senoide que "anda" conforme [phase] vai de 0 a 1. */
private fun wave(size: Size, top: Float, phase: Float, amp: Float): Path = Path().apply {
    moveTo(0f, top)
    var x = 0f
    while (x <= size.width) {
        lineTo(x, top + amp * sin(2 * PI * (x / size.width * 2 + phase)).toFloat())
        x += 4f
    }
    lineTo(size.width, size.height); lineTo(0f, size.height); close()
}

// ---------------- Card de status (no ritmo / abaixo da meta) ----------------
@Composable
fun StatusCard(r: Snapshot) {
    val color = when (r.state) {
        DayState.OK, DayState.DONE -> C.ok
        DayState.BEHIND -> C.bad
        DayState.LATE -> C.warn
        DayState.IDLE -> C.muted
    }
    val behind = r.state == DayState.BEHIND
    val pulse by rememberInfiniteTransition(label = "pulso")
        .animateFloat(1f, .3f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")

    HCard(
        background = if (behind) Brush.verticalGradient(listOf(C.bad.copy(alpha = .12f), C.card)) else Brush.linearGradient(listOf(C.card, C.card)),
        borderColor = if (behind) C.bad.copy(alpha = .45f) else C.stroke,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color.copy(alpha = if (behind) pulse else 1f)))
            Spacer(Modifier.width(10.dp))
            Text(r.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = C.text)
        }
        Text(r.text, color = Color(0xFFC9D7F2), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))

        // Barra de progresso com o marcador "meta agora"
        val fill = (r.drunk.toFloat() / r.goal).coerceIn(0f, 1f)
        val mark = (r.expected.toFloat() / r.goal).coerceIn(0f, 1f)
        val animFill by animateFloatAsState(fill, tween(1000), label = "fill")
        val animMark by animateFloatAsState(mark, tween(1000), label = "mark")
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 34.dp)) {
            val w = maxWidth
            Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0x14FFFFFF)))
            Box(Modifier.fillMaxWidth(animFill).height(12.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(C.w2, C.w1))))
            Box(Modifier.offset(x = w * animMark - 1.5.dp, y = (-6).dp).width(3.dp).height(24.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
            Box(
                Modifier.offset(x = (w * animMark - 40.dp).coerceIn(0.dp, w - 80.dp), y = (-30).dp).width(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "meta agora", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.clip(CircleShape).background(Color(0x24FFFFFF)).padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val mid = r.sch.wakeAt.plusSeconds(java.time.Duration.between(r.sch.wakeAt, r.sch.sleepAt).seconds / 2)
            Text(Hydration.hhmm(r.sch.wakeAt), fontSize = 11.sp, color = C.muted)
            Text(Hydration.hhmm(mid), fontSize = 11.sp, color = C.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(Hydration.hhmm(r.sch.sleepAt), fontSize = 11.sp, color = C.muted)
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Bebido", Hydration.fmt(r.drunk), Modifier.weight(1f))
            Stat("Esperado agora", Hydration.fmt(r.expected), Modifier.weight(1f))
            Stat("Falta p/ meta", Hydration.fmt(maxOf(0, r.goal - r.drunk)), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x0DFFFFFF)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 11.sp, color = C.muted, textAlign = TextAlign.Center)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.text, modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------- Botões de copos ----------------
data class Cup(val ml: Int, val label: String)

val CUPS = listOf(
    Cup(200, "Copo pequeno"),
    Cup(300, "Copo"),
    Cup(500, "Garrafinha"),
    Cup(750, "Garrafa"),
    Cup(1000, "1 litro"),
)

@Composable
fun CupsCard(onAdd: (Int) -> Unit, onCustom: () -> Unit) {
    HCard {
        CardTitle("Registrar água", "toque para adicionar")
        val max = CUPS.last().ml.toFloat()
        val items: List<Cup?> = CUPS + null   // null = botão "Outro"
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c ->
                        if (c != null) {
                            CupButton(Hydration.fmt(c.ml), c.label, Modifier.weight(1f), { CupIcon(.25f + .7f * c.ml / max) }) { onAdd(c.ml) }
                        } else {
                            CupButton("Outro", "valor livre", Modifier.weight(1f), { PlusIcon() }, onCustom)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CupButton(title: String, sub: String, modifier: Modifier, icon: @Composable () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape).background(C.card2).border(1.dp, C.stroke, shape).clickable(onClick = onClick)
            .padding(top = 14.dp, bottom = 12.dp, start = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = C.text)
        Text(sub, fontSize = 11.sp, color = C.muted)
    }
}

/** Copinho desenhado, com nível de água proporcional ao tamanho (como o cupSvg do PWA). */
@Composable
private fun CupIcon(level: Float) {
    Canvas(Modifier.size(34.dp)) {
        val u = size.width / 32f
        val cup = Path().apply {
            moveTo(7 * u, 4 * u); lineTo(25 * u, 4 * u); lineTo(22.6f * u, 27.2f * u)
            lineTo(20.6f * u, 29 * u); lineTo(11.4f * u, 29 * u); lineTo(9.4f * u, 27.2f * u); close()
        }
        clipPath(cup) { drawRect(C.w1, topLeft = Offset(0f, (26 - 20 * level) * u)) }
        drawPath(cup, Color(0xFFCFE6FF), style = Stroke(1.8f * u, join = StrokeJoin.Round))
    }
}

@Composable
private fun PlusIcon() {
    Canvas(Modifier.size(34.dp)) {
        val u = size.width / 32f
        val st = Stroke(2 * u, cap = StrokeCap.Round)
        drawCircle(Color(0xFFCFE6FF), 12 * u, style = st)
        drawLine(Color(0xFFCFE6FF), Offset(16 * u, 10 * u), Offset(16 * u, 22 * u), 2 * u, StrokeCap.Round)
        drawLine(Color(0xFFCFE6FF), Offset(10 * u, 16 * u), Offset(22 * u, 16 * u), 2 * u, StrokeCap.Round)
    }
}

// ---------------- Histórico do dia ----------------
@Composable
fun HistoryCard(entries: List<Entry>, onRemove: (Entry) -> Unit) {
    HCard {
        CardTitle("Hoje", if (entries.isEmpty()) "" else "${entries.size} registro${if (entries.size > 1) "s" else ""}")
        if (entries.isEmpty()) {
            Text(
                "Nenhum registro ainda. Bora beber água! 💧", color = C.muted, fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { e ->
                val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.t), ZoneId.systemDefault())
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x0AFFFFFF)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(C.w1.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_stat_drop), contentDescription = null, tint = C.w1, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(Hydration.fmt(e.ml), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = C.text)
                        Text("às ${Hydration.hhmm(time)}", fontSize = 12.sp, color = C.muted)
                    }
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).clickable { onRemove(e) },
                        contentAlignment = Alignment.Center,
                    ) { Text("✕", color = C.muted, fontSize = 16.sp) }
                }
            }
        }
    }
}

// ---------------- Banner "abaixo da meta" ----------------
@Composable
fun BehindBanner(r: Snapshot, onDrink: () -> Unit, onSnooze: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().shadow(18.dp, shape).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF97316)))).padding(16.dp),
    ) {
        Row {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                Text("💧", fontSize = 22.sp)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Você está abaixo da meta!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(
                    "Já são ${Hydration.hhmm(r.now)} e você deveria ter bebido ${Hydration.fmt(r.expected)}. " +
                        "Você bebeu ${Hydration.fmt(r.drunk)} — faltam ${Hydration.fmt(r.expected - r.drunk)}.",
                    fontSize = 14.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = .95f), modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BannerButton("Bebi 300 ml", Color.White, Color(0xFFBE123C), Modifier.weight(1f), onDrink)
            BannerButton("Lembrar em 30 min", Color(0x2EFFFFFF), Color.White, Modifier.weight(1f), onSnooze)
        }
    }
}

@Composable
private fun BannerButton(text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg).clickable(onClick = onClick).padding(10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
}
