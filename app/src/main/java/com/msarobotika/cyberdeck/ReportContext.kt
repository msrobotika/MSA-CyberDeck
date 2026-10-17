package com.msarobotika.cyberdeck

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ReportContext(val name:String="",val scope:String="",val notes:String="") {
    fun validate(){require(name.length<=160 && scope.length<=2000 && notes.length<=6000){"El contexto supera el tamaño permitido"}}
    fun markdown():String {
        validate()
        fun block(value:String)=value.replace("\r","").lineSequence().joinToString("\n"){"> "+it.replace("```","ʼʼʼ")}
        return buildString {
            if(name.isNotBlank())append("## Trabajo\n\n${block(name)}\n\n")
            if(scope.isNotBlank())append("## Alcance declarado\n\n${block(scope)}\n\n")
            if(notes.isNotBlank())append("## Notas y conclusiones del autor\n\n${block(notes)}\n\n")
        }
    }
}
class ReportDraftStore(context:Context) {
    private val prefs=context.getSharedPreferences("report-draft",Context.MODE_PRIVATE)
    fun read()=ReportContext(prefs.getString("name","").orEmpty(),prefs.getString("scope","").orEmpty(),prefs.getString("notes","").orEmpty())
    fun save(value:ReportContext){value.validate();check(prefs.edit().putString("name",value.name).putString("scope",value.scope).putString("notes",value.notes).commit()){ "No se pudo guardar el borrador" }}
}
@Composable fun ReportContextPanel(context:ReportContext,changed:(ReportContext)->Unit,save:()->Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Contexto del trabajo",style=MaterialTheme.typography.titleMedium)
            Text("Añade el ámbito y tus conclusiones al informe. Se guardan solo en este móvil.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(context.name,{changed(context.copy(name=it.take(160)))},label={Text("Nombre del trabajo")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(context.scope,{changed(context.copy(scope=it.take(2000)))},label={Text("Alcance acordado")},minLines=2,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(context.notes,{changed(context.copy(notes=it.take(6000)))},label={Text("Notas y conclusiones")},minLines=3,modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick=save,modifier=Modifier.fillMaxWidth()){Text("Guardar contexto")}
        }
    }
}
