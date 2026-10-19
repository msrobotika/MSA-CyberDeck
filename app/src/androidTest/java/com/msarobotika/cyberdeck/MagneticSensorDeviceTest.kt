package com.msarobotika.cyberdeck
import android.hardware.*
import android.os.Handler
import android.os.HandlerThread
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.time.Instant
import org.junit.Test
import org.junit.Assume.assumeNotNull
import org.junit.Assert.*
class MagneticSensorDeviceTest {
 @Test fun readsActualPhysicalSensorWithoutMockValues(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val manager=context.getSystemService(SensorManager::class.java)
  val sensor=manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);assumeNotNull(sensor)
  val rows=java.util.Collections.synchronizedList(mutableListOf<MagneticSample>())
  val latch=CountDownLatch(3);val thread=HandlerThread("magnetic-physical-test").apply{start()}
  var previous=0L
  val listener=object:SensorEventListener{
   override fun onAccuracyChanged(s:Sensor?,accuracy:Int){}
   override fun onSensorChanged(event:SensorEvent){if(event.values.size>=3 && event.timestamp-previous>=500000000L){previous=event.timestamp;rows+=MagneticSample(Instant.now().toString(),event.timestamp,event.values[0].toDouble(),event.values[1].toDouble(),event.values[2].toDouble(),event.accuracy);latch.countDown()}}
  }
  try{
   assertTrue(manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_NORMAL,Handler(thread.looper)))
   assertTrue("Android supplied no sensor events during the 8 s physical check",latch.await(8,TimeUnit.SECONDS))
   val observed=synchronized(rows){rows.take(3)}
   assertTrue(observed.all{it.magnitude.isFinite() && it.magnitude>=0})
   File(context.filesDir,"validation/magnetic-sensor.json").apply{parentFile?.mkdirs()}.writeText(MagneticPolicy.report(sensor!!.name,observed,null).toEvidenceJson())
  }finally{manager.unregisterListener(listener);thread.quitSafely()}
 }
}
