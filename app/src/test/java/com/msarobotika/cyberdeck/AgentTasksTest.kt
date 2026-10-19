package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class AgentTasksTest {
 @Test fun phoneTasksNeedNoEspOrLoadedModel(){
  for(task in AgentTasks.all){assertTrue(task.steps.none{it==AgentTool.USB || it==AgentTool.RADIO});assertEquals("Tarea del móvil",task.session().origin);assertTrue(task.session().modelHash.isEmpty())}
  assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.RESOURCES,AgentTool.REVIEW,AgentTool.SELF_APK),AgentTasks.phone.steps)
 }
 @Test fun ordinaryPhoneRequestsResolveLocallyIncludingExplicitNoEsp(){
  listOf("Revisa mi móvil","Analiza mi teléfono","Revisa mi móvil y el estado de su conexión","Revisa mi móvil sin ESP32").forEach{assertEquals(it,AgentTasks.phone,AgentTasks.match(it))}
 }
 @Test fun phoneConnectionIsNotAnEspWifiScan(){listOf("Comprueba mi conexión","Analiza mi wifi","Ver internet").forEach{assertEquals(AgentTasks.connection,AgentTasks.match(it))}}
 @Test fun proximityAndFileTasksResolveWithoutModel(){assertEquals(AgentTasks.ble,AgentTasks.match("Busca dispositivos Bluetooth cercanos"));assertEquals(AgentTasks.nfc,AgentTasks.match("Lee una etiqueta NFC"));assertEquals(AgentTasks.apk,AgentTasks.match("Inspecciona una APK"))}
 @Test fun ambiguousNegativeAndDestructiveRequestsNeverAutoMatch(){listOf("No revises mi móvil","Busca Bluetooth y lee NFC","Borra todo","Instala esta APK","Conecta al ESP32","Haz cualquier cosa").forEach{assertNull(it,AgentTasks.match(it))}}
 @Test fun linkKeepsExplicitTargetAndRequiresReview(){val task=AgentTasks.match("Revisa este enlace https://example.com/path?q=own")!!;assertEquals("https://example.com/path?q=own",task.inputs.url);assertFalse(task.instant);assertFalse(task.inputs.networkApproved)}
 @Test fun modelCannotAddUnrequestedOrNegatedHardware(){val hardware=AgentPlan(listOf(AgentTool.USB));assertTrue(runCatching{AgentTasks.checkHardwareRequest("Revisa mi móvil",hardware)}.isFailure);assertTrue(runCatching{AgentTasks.checkHardwareRequest("Revisa sin ESP32",hardware)}.isFailure);assertEquals(hardware,AgentTasks.checkHardwareRequest("Identifica el ESP32 conectado por USB",hardware));val phone=AgentPlan(listOf(AgentTool.PHONE));assertEquals(phone,AgentTasks.checkHardwareRequest("Revisa mi móvil",phone))}
 @Test fun presetsThatNeedTargetsOrConsentDoNotAutoExecute(){assertFalse(AgentTasks.link.instant);assertFalse(AgentTasks.lan.instant);assertTrue(AgentTasks.lan.steps.any{it.network});assertFalse(AgentTasks.ble.inputs.bleGatt)}
}
