package com.msarobotika.cyberdeck

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val DeckCyan = Color(0xFF71E5F4)
val DeckMint = Color(0xFF8EE7BD)
val DeckViolet = Color(0xFFB6AAFF)

@Composable fun DeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = DeckCyan, onPrimary = Color(0xFF062B32), primaryContainer = Color(0xFF123A46),
        secondary = DeckViolet, tertiary = DeckMint, background = Color(0xFF090F1A),
        surface = Color(0xFF111D2E), surfaceVariant = Color(0xFF1B2A40),
        surfaceContainerLowest=Color(0xFF090F1A),surfaceContainerLow=Color(0xFF0E1828),
        surfaceContainer=Color(0xFF111D2E),surfaceContainerHigh=Color(0xFF162439),surfaceContainerHighest=Color(0xFF1B2A40),
        secondaryContainer=Color(0xFF20394D),onSecondaryContainer=Color(0xFFD4F4FF),
        onSurface = Color(0xFFE9F0FC), onSurfaceVariant = Color(0xFFB4C2D8),
        outline = Color(0xFF3B506B), error = Color(0xFFFF99A6)),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontSize=30.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold),
            headlineSmall = androidx.compose.ui.text.TextStyle(fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.SemiBold),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize=21.sp,lineHeight=27.sp,fontWeight=FontWeight.SemiBold),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize=15.sp,lineHeight=22.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize=14.sp,lineHeight=20.sp),
            bodySmall = androidx.compose.ui.text.TextStyle(fontSize=12.sp,lineHeight=18.sp),
            labelLarge = androidx.compose.ui.text.TextStyle(fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold)), content=content)
}

data class RootDestination(val route: String, val title: String, val glyph: String)
val deckRoots = listOf(RootDestination("Resumen","Inicio","home"), RootDestination("Herramientas","Herramientas","tools"),
    RootDestination("Evidencias","Evidencias","files"), RootDestination("Informes","Informe","report"))
fun deckRoot(route: String) = if (deckRoots.any { it.route == route }) route else "Herramientas"

@Composable fun DeckGlyph(kind: String, modifier: Modifier = Modifier.size(24.dp), tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val s=size.width; val w=(s*.065f).coerceAtLeast(1f)
        fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(tint,Offset(s*x,s*y),Offset(s*a,s*b),w)
        when(kind) {
            "menu" -> {line(.15f,.25f,.85f,.25f);line(.15f,.5f,.85f,.5f);line(.15f,.75f,.85f,.75f)}
            "home" -> { line(.12f,.45f,.5f,.14f);line(.5f,.14f,.88f,.45f);line(.24f,.4f,.24f,.86f);line(.24f,.86f,.76f,.86f);line(.76f,.86f,.76f,.4f);line(.43f,.86f,.43f,.6f);line(.43f,.6f,.59f,.6f) }
            "files","report" -> { val p=Path().apply{moveTo(s*.24f,s*.12f);lineTo(s*.63f,s*.12f);lineTo(s*.8f,s*.3f);lineTo(s*.8f,s*.88f);lineTo(s*.24f,s*.88f);close()};drawPath(p,tint,style=Stroke(w));line(.36f,.43f,.66f,.43f);line(.36f,.59f,.66f,.59f);if(kind=="report")line(.36f,.73f,.56f,.73f) }
            "back" -> { line(.7f,.15f,.3f,.5f);line(.3f,.5f,.7f,.85f) }
            "radio" -> { for(i in 1..3)drawCircle(tint.copy(alpha=.25f+i*.2f),s*.13f*i,Offset(s*.5f,s*.5f),style=Stroke(w));line(.5f,.5f,.77f,.25f) }
            "transfer" -> { line(.12f,.34f,.84f,.34f);line(.84f,.34f,.65f,.15f);line(.84f,.34f,.65f,.53f);line(.88f,.73f,.16f,.73f);line(.16f,.73f,.35f,.54f);line(.16f,.73f,.35f,.92f) }
            else -> { for(x in listOf(.23f,.5f,.77f))for(y in listOf(.23f,.5f,.77f))drawCircle(tint,s*.055f,Offset(s*x,s*y)) }
        }
    }
}

@Composable fun DeckShell(route: String, busy: Boolean, message: String, navigate: (String)->Unit, receiving:Boolean=false,content: @Composable ()->Unit) {
    val selected=deckRoot(route)
    val focus=LocalFocusManager.current
    val wide=LocalConfiguration.current.screenWidthDp >= 600
    val keyboard=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val tool=deckTools.firstOrNull { it.route==route }
    fun go(target:String){focus.clearFocus();navigate(target)}
    BackHandler(enabled=route!="Resumen") { go(if(tool!=null) "Herramientas" else "Resumen") }
    DeckNavigationDrawer(route,navigate) { openMenu ->
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            if(wide) NavigationRail(containerColor=MaterialTheme.colorScheme.background,windowInsets=WindowInsets(0,0,0,0)) {
                Spacer(Modifier.height(16.dp))
                deckRoots.forEach { root -> NavigationRailItem(selected=selected==root.route,onClick={go(root.route)},
                    modifier=Modifier.testTag("navigate-${root.route}"),icon={DeckGlyph(root.glyph)},label={Text(root.title)}) }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={focus.clearFocus();openMenu()},modifier=Modifier.semantics{contentDescription="Abrir menú"}){DeckGlyph("menu")}
                    if(tool!=null) IconButton(onClick={go("Herramientas")},modifier=Modifier.semantics{contentDescription="Volver a herramientas"}) { DeckGlyph("back") }
                    Column(Modifier.weight(1f).padding(start=12.dp)) {
                        Text("MSA ROBOTIKA",fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,letterSpacing=1.sp)
                        Text(tool?.title ?: when(route){"Resumen"->"CiberDeck";"Herramientas"->"Tu kit de campo";"Evidencias"->"Archivo de evidencias";else->"Informe de trabajo"},style=MaterialTheme.typography.titleLarge)
                    }
                }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(receiving && route!="Recibir")TextButton(onClick={go("Recibir")},modifier=Modifier.fillMaxWidth()) {
                    Text("Recepción activa · abrir control",color=DeckMint)
                }
                if(message.isNotBlank() && !message.matches(Regex("\\d+ evidencias guardadas")) && message!="Datos locales · sin sensores enlazados") {
                    Text(message,Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=6.dp).semantics{liveRegion=LiveRegionMode.Polite},
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                Box(Modifier.weight(1f).fillMaxWidth()){content()}
                if(!wide && !keyboard) NavigationBar(containerColor=MaterialTheme.colorScheme.background,tonalElevation=0.dp,windowInsets=WindowInsets(0,0,0,0)) {
                    deckRoots.forEach { root -> NavigationBarItem(selected=selected==root.route,onClick={go(root.route)},
                        modifier=Modifier.testTag("navigate-${root.route}"),icon={DeckGlyph(root.glyph,tint=if(selected==root.route) DeckCyan else MaterialTheme.colorScheme.onSurfaceVariant)},
                        label={Text(if(root.route=="Herramientas")"Kit" else root.title,fontSize=11.sp,lineHeight=14.sp,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)},
                        colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer)) }
                }
            }
        }
    }
    }
}

@Composable fun DeckSection(title:String, subtitle:String="", action:String="", click:()->Unit={}) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(action.isNotBlank())TextButton(onClick=click){Text(action)}
    }
}

@Composable fun DeckDisclosure(title:String, text:String) {
    var open by remember { mutableStateOf(false) }
    Column { TextButton(onClick={open=!open}){Text(if(open) "Ocultar $title" else title)}
        if(open) SelectionContainer {Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
}

fun displayBytes(value:Long):String = when {
    value<1024 -> "$value B"
    value<1048576 -> String.format(Locale.ROOT,"%.1f KiB",value/1024.0)
    else -> String.format(Locale.ROOT,"%.1f MiB",value/1048576.0)
}
fun displayTime(value:String):String = runCatching {DateTimeFormatter.ofPattern("dd MMM · HH:mm",Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.parse(value))}.getOrDefault(value)
