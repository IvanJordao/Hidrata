package com.ivanjordao.hidrata.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivanjordao.hidrata.logic.Hydration
import com.ivanjordao.hidrata.logic.Settings
import com.ivanjordao.hidrata.notify.Reminders

private val GOALS = listOf(2.0, 2.5, 3.0, 3.5, 4.0)
private val CUSTOM = listOf(50, 100, 150, 250, 350, 400)

private fun litros(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString().replace('.', ',')
private fun parseLitros(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

/** Formulário de configurações — usado no primeiro acesso (boas-vindas) e na folha de configurações. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsForm(
    initial: Settings?,
    first: Boolean,
    notifyOn: Boolean,
    onSave: (goalMl: Int, wake: String, sleep: String) -> Unit,
    onToggleNotify: () -> Unit,
    onReset: () -> Unit,
) {
    val s = initial ?: Settings(3000, "06:00", "22:00", false)
    var goalText by remember { mutableStateOf(litros(s.goalMl / 1000.0)) }
    var wake by remember { mutableStateOf(s.wake) }
    var sleep by remember { mutableStateOf(s.sleep) }
    var error by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }

    Text(if (first) "Bem-vindo ao Hidrata 💧" else "Configurações", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.text)
    Text(
        if (first) "Defina sua meta diária e sua rotina. O app distribui a meta ao longo do período em que você está acordado."
        else "Ajuste sua meta e sua rotina.",
        color = C.muted, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
    )

    FieldLabel("Meta diária (litros)")
    OutlinedTextField(
        value = goalText, onValueChange = { goalText = it },
        singleLine = true, placeholder = { Text("Ex.: 3") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GOALS.forEach { g ->
            FilterChip(
                selected = parseLitros(goalText) == g,
                onClick = { goalText = litros(g) },
                label = { Text("${litros(g)} L") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = C.w1.copy(alpha = .25f)),
            )
        }
    }
    Text("Referência comum: cerca de 35 ml por kg de peso corporal.", fontSize = 12.sp, color = C.muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { FieldLabel("Acordo às"); TimeField(wake) { wake = it } }
        Column(Modifier.weight(1f)) { FieldLabel("Durmo às"); TimeField(sleep) { sleep = it } }
    }
    Text(error, color = C.bad, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))

    Button(
        onClick = {
            val g = parseLitros(goalText)
            error = when {
                g == null || g < 0.5 || g > 10 -> "Informe uma meta entre 0,5 e 10 litros."
                wake == sleep -> "Os horários de acordar e dormir precisam ser diferentes."
                else -> ""
            }
            if (error.isEmpty()) onSave((g!! * 1000).toInt(), wake, sleep)
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.w2, contentColor = Color.White),
    ) { Text(if (first) "Começar" else "Salvar", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }

    if (!first) {
        HorizontalDivider(Modifier.padding(vertical = 18.dp), color = C.stroke)
        OutlinedButton(onClick = onToggleNotify, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (notifyOn) "🔔 Notificações: ativadas (toque p/ desativar)" else "🔔 Ativar notificações", color = C.text)
        }
        Text(
            "Com as notificações ativas, o Hidrata confere sua meta a cada ${Reminders.INTERVAL_MIN} min, " +
                "mesmo com o app fechado, e avisa quando você está prestes a ficar ou já ficou abaixo dela.",
            fontSize = 12.sp, color = C.muted, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Apagar registros de hoje", color = C.bad)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Apagar registros?") },
            text = { Text("Apagar todos os registros de hoje?") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Apagar", color = C.bad) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.muted, modifier = Modifier.padding(bottom = 6.dp))
}

/** Campo de horário: ao tocar, abre o seletor de horas nativo do Android. */
@Composable
private fun TimeField(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).background(Color(0x0FFFFFFF)).border(1.dp, C.stroke, shape)
            .clickable {
                val (h, m) = value.split(":").map { it.toInt() }
                TimePickerDialog(context, { _, hh, mm -> onChange("%02d:%02d".format(hh, mm)) }, h, m, true).show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) { Text(value, fontSize = 17.sp, color = C.text) }
}

/** Diálogo "Outra quantidade". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomAmountDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    fun submit() {
        val v = text.trim().toIntOrNull()
        if (v == null || v <= 0 || v > 5000) error = "Informe um valor entre 1 e ${Hydration.num(5000)} ml." else onAdd(v)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.sheet,
        title = { Text("Outra quantidade") },
        text = {
            Column {
                Text("Quantos ml você bebeu?", color = C.muted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter(Char::isDigit) },
                    singleLine = true, placeholder = { Text("Ex.: 350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CUSTOM.forEach { v -> FilterChip(selected = text == v.toString(), onClick = { text = v.toString() }, label = { Text("$v ml") }) }
                }
                if (error.isNotEmpty()) Text(error, color = C.bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text("Adicionar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
