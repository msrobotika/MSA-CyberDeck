package com.msarobotika.cyberdeck

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserDelivery(val id:String,val name:String)

/** User-started, bounded receiver; never restarts or listens after force-stop/task removal. */
class BrowserTransferService:Service() {
    companion object {
        private const val START="com.msarobotika.cyberdeck.RECEIVE_START"
        private const val STOP="com.msarobotika.cyberdeck.RECEIVE_STOP"
        const val SHOW_RECEIVER="show-browser-receiver"
        private const val CHANNEL="local-reception"
        private const val NOTIFICATION=42
        private val current=MutableStateFlow(BrowserState())
        private val delivery=MutableStateFlow<BrowserDelivery?>(null)
        val state=current.asStateFlow()
        val lastDelivery=delivery.asStateFlow()
        private var instance:BrowserTransferService?=null
        fun start(context:Context) {
            ContextCompat.startForegroundService(context,Intent(context,BrowserTransferService::class.java).setAction(START))
        }
        fun stop(context:Context){context.stopService(Intent(context,BrowserTransferService::class.java))}
        fun decide(accept:Boolean){instance?.receiver?.decide(accept)}
    }
    private lateinit var receiver:BrowserReceiver
    private var destroying=false
    override fun onCreate() {
        super.onCreate();instance=this
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Recepción local",NotificationManager.IMPORTANCE_LOW))
        receiver=BrowserReceiver(this,{ value ->
            current.value=value
            if(value.active) {
                if(android.os.Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification(value.offer!=null))
            } else if(!destroying){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
        },{name,bytes->
            val entry=EvidenceStore(this).saveBytes(name,bytes).last()
            delivery.value=BrowserDelivery(entry.id,entry.name)
        })
    }
    private fun notification(pending:Boolean):Notification {
        val open=PendingIntent.getActivity(this,43,
            Intent(this,MainActivity::class.java).putExtra(SHOW_RECEIVER,true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,44,Intent(this,BrowserTransferService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_transfer_notification)
            .setContentTitle("CyberDeck · recepción local activa")
            .setContentText(if(pending)"Abre CyberDeck para aceptar o rechazar el archivo" else "HTTP en tu red de confianza · sesión de 2 minutos")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0,"Detener recepción",stop).build()
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==STOP){receiver.stop();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY}
        if(intent?.action!=START){stopSelf();return START_NOT_STICKY}
        if(current.value.active)return START_NOT_STICKY
        try {
            ServiceCompat.startForeground(this,NOTIFICATION,notification(false),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            receiver.start()
        } catch(error:Exception) {
            current.value=BrowserState(status="No se pudo iniciar la recepción: ${error.message}")
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
        }
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent:Intent?) {receiver.stop("Recepción cerrada al cerrar CyberDeck");stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onTimeout(startId:Int,fgsType:Int){receiver.stop("Recepción cerrada por el límite de Android");stopSelf()}
    override fun onDestroy() {
        destroying=true;receiver.close();instance=null;stopForeground(STOP_FOREGROUND_REMOVE)
        if(current.value.active)current.value=BrowserState(status="Recepción cerrada · crea una sesión nueva")
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
