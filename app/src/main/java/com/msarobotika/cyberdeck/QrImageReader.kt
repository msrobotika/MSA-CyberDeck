package com.msarobotika.cyberdeck

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object QrImageReader {
 const val MAX_BYTES=20*1024*1024
 fun boundedBytes(input:InputStream):ByteArray {
  val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
  while(true){val count=input.read(buffer);if(count<0)break;if(count==0)continue;require(out.size().toLong()+count<=MAX_BYTES){"Elige una imagen de hasta 20 MiB"};out.write(buffer,0,count)}
  return out.toByteArray()
 }
 fun read(context:Context,uri:Uri):QrObservation {
  val bytes=context.contentResolver.openInputStream(uri)?.use{boundedBytes(it)}?:error("No se pudo abrir la imagen elegida")
  return readBytes(bytes)
 }
 fun readBytes(bytes:ByteArray):QrObservation {
  require(bytes.size<=MAX_BYTES){"Elige una imagen de hasta 20 MiB"}
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
  require(bounds.outWidth>0 && bounds.outHeight>0 && bounds.outWidth<=50000 && bounds.outHeight<=50000){"Imagen no compatible o dimensiones excesivas"}
  var sample=1
  while((bounds.outWidth.toLong()+sample-1)/sample*((bounds.outHeight.toLong()+sample-1)/sample)>QrInspection.MAX_PIXELS || (maxOf(bounds.outWidth,bounds.outHeight)+sample-1)/sample>2048)sample*=2
  val options=BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=android.graphics.Bitmap.Config.ARGB_8888}
  val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)?:error("No se pudo decodificar la imagen")
  try{
   require(bitmap.width.toLong()*bitmap.height<=QrInspection.MAX_PIXELS){"La imagen decodificada supera el límite"}
   val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
   return QrInspection.inspect(QrInspection.decodePixels(bitmap.width,bitmap.height,pixels),PeerProtocol.sha256(bytes))
  }finally{bitmap.recycle()}
 }
}
