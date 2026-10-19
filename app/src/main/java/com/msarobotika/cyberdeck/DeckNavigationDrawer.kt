package com.msarobotika.cyberdeck

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable fun DeckNavigationDrawer(route:String,navigate:(String)->Unit,content:@Composable (()->Unit)->Unit) {
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    var expanded by rememberSaveable {mutableStateOf("")}
    fun close(){scope.launch{drawer.close()}}
    fun select(target:String){navigate(target);close()}
    ModalNavigationDrawer(drawerState=drawer,drawerContent={
        ModalDrawerSheet(modifier=Modifier.widthIn(max=360.dp),drawerContainerColor=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f).padding(vertical=12.dp)) {
                        Text("MS ROBOTIKA",style=MaterialTheme.typography.labelSmall,color=DeckCyan)
                        Text("CiberDeck",style=MaterialTheme.typography.headlineSmall)
                        Text("MENÚ DE TRABAJO",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick={close()},modifier=Modifier.semantics{contentDescription="Cerrar menú"}){DeckGlyph("back")}
                }
                deckRoots.take(2).forEach { root -> NavigationDrawerItem(label={Text(root.title)},selected=route==root.route,
                    onClick={select(root.route)},icon={DeckGlyph(root.glyph)},modifier=Modifier.testTag("drawer-root-${root.route}")) }
                NavigationDrawerItem(label={Text("Agente")},selected=route=="Agente",onClick={select("Agente")},icon={DeckGlyph("tools")},modifier=Modifier.testTag("drawer-agent"))
                NavigationDrawerItem(label={Text("Mi móvil")},selected=route=="Móvil",onClick={select("Móvil")},icon={DeckGlyph("report")})
                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("HERRAMIENTAS POR ÁREA",style=MaterialTheme.typography.labelSmall,color=DeckCyan)
                deckTools.filter{it.route !in listOf("Módulos","Móvil","Agente")}.groupBy{it.group}.forEach { (group,tools) ->
                    TextButton(onClick={expanded=if(expanded==group)"" else group},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("drawer-group-$group").semantics{stateDescription=if(expanded==group)"Desplegado" else "Plegado"}) {
                        Text(group,Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurface)
                        Text(if(expanded==group)"−" else "+",color=DeckCyan)
                    }
                    if(expanded==group)tools.forEach{tool -> NavigationDrawerItem(
                        label={Column{Text(tool.title);Text(tool.badge,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},
                        selected=route==tool.route,onClick={select(tool.route)},icon={DeckGlyph(tool.glyph)},modifier=Modifier.testTag("drawer-tool-${tool.route}"))}
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick={select("Módulos")}){Text("Funciones en desarrollo")}
                Text("Local · dispositivos autorizados",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }){content {scope.launch{drawer.open()}}}
    BackHandler(enabled=drawer.isOpen){close()}
}
