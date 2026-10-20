package com.msarobotika.cyberdeck

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test

class QrInspectionTest {
 private fun pixels(text:String):IntArray {val matrix=QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,256,256);return IntArray(256*256){i->if(matrix[i%256,i/256])-0x1000000 else -1}}
 @Test fun decodesRealQrPixels(){val text="https://owned.example/review";assertEquals(text,QrInspection.decodePixels(256,256,pixels(text)))}
 @Test fun decodesInvertedQr(){val text="QR UNIT FIXTURE";assertEquals(text,QrInspection.decodePixels(256,256,pixels(text).map{it xor 0x00ffffff}.toIntArray()))}
 @Test fun reportOmitsWifiCredentialsAndPayload(){val r=QrInspection.inspect("WIFI:T:WPA;S:fixture-net;P:private-fixture-secret;;","a".repeat(64));assertEquals("Configuración Wi-Fi",r.type);val report=r.diagnostic().toString();assertFalse(report.contains("private-fixture-secret"));assertFalse(report.contains("fixture-net"));assertTrue(report.contains("a".repeat(64)))}
 @Test fun linkAnalysisKeepsRealHostAndOmitsFullUrlSecrets(){val r=QrInspection.inspect("https://trusted.example@owned.example/path?token=private-fixture");assertEquals("owned.example",r.link!!.asciiHost);assertEquals(1,r.link.signals);assertFalse(r.diagnostic().toString().contains("private-fixture"));assertFalse(r.diagnostic().toString().contains("/path"))}
 @Test fun specialSchemesAreDataNotActions(){for(value in listOf("intent://fixture","javascript:fixture","data:text/plain,fixture","file:///fixture")){val r=QrInspection.inspect(value);assertEquals("Acción o contenido especial",r.type);assertNull(r.link)}}
 @Test fun unsupportedAndMalformedUrlsDoNotBecomeValidatedLinks(){assertNull(QrInspection.inspect("https://").link);assertEquals("Otro esquema",QrInspection.inspect("tel:123").type);assertEquals("Texto",QrInspection.inspect("unit fixture").type)}
 @Test fun payloadAndPixelLimitsRejectInvalidInput(){assertTrue(runCatching{QrInspection.inspect("")}.isFailure);assertTrue(runCatching{QrInspection.inspect("x".repeat(16385))}.isFailure);assertTrue(runCatching{QrInspection.decodePixels(Int.MAX_VALUE,2,IntArray(0))}.isFailure)}
 @Test fun blankImageHasNoSyntheticResult(){assertTrue(runCatching{QrInspection.decodePixels(32,32,IntArray(1024){-1})}.isFailure)}
 @Test fun agentQrTaskRequiresImageSelectionAndNoNetworkTarget(){val task=AgentTasks.match("Lee un QR de una imagen")!!;assertEquals(AgentTasks.qr,task);assertTrue(task.inputs.qrImage);assertTrue(task.instant);validateAgentInputs(task.session().plan!!,task.inputs);assertFalse(task.steps.any{it.network});assertNull(AgentTasks.match("No leas un QR"))}
 @Test fun plainLinkModeStillRequiresAValidUrl(){assertTrue(runCatching{validateAgentInputs(AgentPlan(listOf(AgentTool.LINK)),AgentInputs())}.isFailure)}
}
