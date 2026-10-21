package com.msarobotika.cyberdeck

import android.app.role.RoleManager
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable fun CallGuardPanel(save:(DiagnosticResult)->Unit) {
    val context=LocalContext.current
    val journal=remember{CallJournal(context)}
    var active by remember{mutableStateOf(CallJournal.active(context))}
    var events by remember{mutableStateOf(journal.records())}
    var marked by remember{mutableStateOf(journal.marked())}
    var input by rememberSaveable{mutableStateOf("")}
    var error by rememberSaveable{mutableStateOf("")}
    var deleting by remember{mutableStateOf(false)}
    fun refresh(){active=CallJournal.active(context);events=journal.records();marked=journal.marked()}
    val role=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){refresh()}
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){refresh()}
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner){val listener=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)refresh()};owner.lifecycle.addObserver(listener);onDispose{owner.lifecycle.removeObserver(listener)}}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("GUARDIA DE LLAMADAS",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
        Text(if(active)"Activada en Android" else "Pendiente de activación",style=MaterialTheme.typography.headlineSmall)
        Text("Avisos basados en tu lista y en la verificación del operador. Procesamiento local, sin grabar audio ni bloquear llamadas.")
        DeckDisclosure("Qué puede comprobar", "Solo llamadas entrantes entregadas por Android desde que activas el servicio. No consulta contactos ni sube números a Internet. La verificación del operador no identifica al dueño ni garantiza que sea fiable. Sin verificación no significa fraude. Android puede excluir contactos, números ocultos y llamadas de apps. Al activarla eliges CiberDeck como app de filtrado y puedes sustituir la que tienes ahora; se revoca en aplicaciones predeterminadas.")
        if(!active) Button(enabled=CallJournal.available(context),onClick={if(Build.VERSION.SDK_INT>=29)role.launch(context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))}){Text("Activar en Android")}
        if(active)OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))}){Text("Gestionar o desactivar en Android")}
        if(Build.VERSION.SDK_INT>=33)OutlinedButton(onClick={notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)}){Text("Permitir avisos")}
        Text("Avisos: verificación fallida o coincidencia exacta con tu lista. Si Android deniega notificaciones, solo queda el registro local.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(input,{input=it.take(40);error=""},label={Text("Número para revisar")},supportingText={Text("Incluye el prefijo tal como llega; no se adivina el país")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Button(onClick={val n=CallPolicy.number(input);if(n==null)error="Introduce entre 3 y 20 cifras, con + opcional" else{journal.mark(n,true);input="";refresh()}}){Text("Añadir a mi lista")}
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
        marked.sorted().forEach{n->Row(Modifier.fillMaxWidth()){Text(n,Modifier.weight(1f));TextButton(onClick={journal.mark(n,false);refresh()}){Text("Quitar")}}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={refresh()}){Text("Actualizar registro")};OutlinedButton(enabled=events.isNotEmpty(),onClick={save(journal.diagnostic())}){Text("Guardar")}}
        Text("${events.size} llamadas observadas · máximo 100",color=DeckCyan)
        if(events.isEmpty())Text("Sin llamadas registradas. No se importan llamadas antiguas ni se generan ejemplos.")
        events.forEach{e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(e.optString("number").ifBlank{"Número no disponible"},style=MaterialTheme.typography.titleMedium)
            Text(CallPolicy.verification(e.optInt("verification")))
            if(e.optBoolean("marked"))Text("Estaba en tu lista al recibirla",color=MaterialTheme.colorScheme.tertiary)
            Text(e.optString("at"),style=MaterialTheme.typography.bodySmall)
        }}}
        TextButton(enabled=events.isNotEmpty(),onClick={deleting=true}){Text("Borrar registro local")}
    }
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("¿Borrar las llamadas observadas?")},text={Text("Solo elimina el registro privado de CiberDeck. Mantiene tu lista y el historial de Android.")},confirmButton={TextButton(onClick={journal.clear();refresh();deleting=false}){Text("Borrar")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancelar")}})
}
