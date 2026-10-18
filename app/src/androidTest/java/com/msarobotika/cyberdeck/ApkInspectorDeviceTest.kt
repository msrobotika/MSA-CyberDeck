package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class ApkInspectorDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun inspectsOwnInstalledApkWithoutLaunchingInstaller(){
  ui.openDeck("APK")
  ui.onNodeWithText("Inspeccionar CiberDeck instalada").performScrollTo().performClick()
  ui.waitUntil(20000){ViewModelProvider(ui.activity)[DeckSessionModel::class.java].apk.value!=null}
  val r=ViewModelProvider(ui.activity)[DeckSessionModel::class.java].apk.value!!
  assertEquals(ui.activity.packageName,r.packageName)
  assertEquals(File(ui.activity.applicationInfo.sourceDir).length(),r.fingerprint.bytes)
  assertEquals(64,r.fingerprint.sha256.length)
  assertTrue(r.signers.isNotEmpty());assertTrue(r.signers.all{it.length==64})
  assertTrue(r.debug);assertTrue(r.permissions.any{it.name=="android.permission.INTERNET"})
  assertTrue(r.exported.any{it.first.endsWith("CallGuardService") && it.second.contains("BIND_SCREENING_SERVICE")})
  val before=ui.activity.cacheDir.listFiles().orEmpty().filter{it.name.startsWith("ciberdeck-apk-")}.map{it.name}.toSet()
  val copied=ApkInspector.fromUri(ui.activity,android.net.Uri.fromFile(File(ui.activity.applicationInfo.sourceDir)))
  assertEquals(r.fingerprint.sha256,copied.fingerprint.sha256)
  assertEquals(before,ui.activity.cacheDir.listFiles().orEmpty().filter{it.name.startsWith("ciberdeck-apk-")}.map{it.name}.toSet())
  val invalid=File.createTempFile("apk-invalid-fixture-",".apk",ui.activity.cacheDir)
  try{invalid.writeText("not an apk");assertTrue(runCatching{ApkInspector.inspectFile(ui.activity,invalid)}.isFailure)}finally{invalid.delete()}
  ui.onNodeWithText("Guardar informe APK").performScrollTo().assertIsDisplayed()
  ui.activityRule.scenario.recreate()
  assertEquals(r.fingerprint.sha256,ViewModelProvider(ui.activity)[DeckSessionModel::class.java].apk.value?.fingerprint?.sha256)
 }
}
