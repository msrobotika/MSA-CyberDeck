package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable fun DiagnosticResultCard(result:DiagnosticResult,busy:Boolean,save:(DiagnosticResult)->Unit,copySummary:(String)->Unit) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    var expanded by rememberSaveable(result){mutableStateOf(false)}
    var query by rememberSaveable(result){mutableStateOf("")}
    var copyStatus by remember(result){mutableStateOf("")}
    Card(Modifier.fillMaxWidth().testTag("diagnostic-result")){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("RESULTADO · ${result.kind.uppercase(java.util.Locale.ROOT)}",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
            SelectionContainer{Text(result.summary,style=MaterialTheme.typography.titleMedium)}
            SelectionContainer{Text(result.target,style=MaterialTheme.typography.bodyMedium)}
            Text("Lectura: ${displayTime(result.checkedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled=!busy,onClick={save(result)},modifier=Modifier.fillMaxWidth().testTag("diagnostic-save")){Text("Guardar resultado como evidencia")}
            TextButton(onClick={copyStatus=runCatching{copySummary(DiagnosticPresentation.summary(result));"Resumen copiado · sin campos técnicos"}.getOrElse{"No se pudo copiar el resumen"}},modifier=Modifier.testTag("diagnostic-copy")){Text("Copiar resumen")}
            if(copyStatus.isNotBlank())Text(copyStatus,Modifier.semantics{liveRegion=LiveRegionMode.Polite},style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("diagnostic-details-toggle")){
                Text(if(expanded)"Ocultar detalles técnicos" else "Ver detalles técnicos · ${result.details.size}")
            }
            if(expanded){
                OutlinedTextField(query,{query=it},label={Text("Buscar en este resultado")},singleLine=true,
                    keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={focus.clearFocus()}),modifier=Modifier.fillMaxWidth().testTag("diagnostic-details-search"))
                val visible=DiagnosticPresentation.details(result,query)
                Text("${visible.size} de ${result.details.size} campos · el guardado conserva todos",style=MaterialTheme.typography.bodySmall)
                if(query.isNotBlank())TextButton(onClick={query=""}){Text("Mostrar todos los campos")}
                if(visible.isEmpty())Text(if(result.details.isEmpty())"Esta observación no incluye campos adicionales." else "Sin campos que coincidan. Cambia o borra la búsqueda.",style=MaterialTheme.typography.bodySmall)
                visible.forEach{(key,value)->
                    HorizontalDivider()
                    Text(key,color=DeckCyan,style=MaterialTheme.typography.labelLarge)
                    SelectionContainer{Text(value,style=MaterialTheme.typography.bodyMedium)}
                }
            }
        }
    }
}
