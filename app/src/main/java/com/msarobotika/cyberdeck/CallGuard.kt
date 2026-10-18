package com.msarobotika.cyberdeck

import android.app.*
import android.app.role.RoleManager
import android.content.*
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.*
import java.time.Instant

class CallJournal(context:Context) {
    private val prefs=context.getSharedPreferences("call-guard-private",Context.MODE_PRIVATE)
    fun marked()=prefs.getStringSet("marked",emptySet())!!.toSet()
    fun mark(number:String,enabled:Boolean){val n=CallPolicy.number(number)?:return;val values=marked().toMutableSet();if(enabled)values.add(n)else values.remove(n);prefs.edit().putStringSet("marked",values.take(200).toSet()).apply()}
    fun records():List<JSONObject> = runCatching{val a=JSONArray(prefs.getString("events","[]"));(0 until a.length()).map{a.getJSONObject(it)}}.getOrDefault(emptyList())
    @Synchronized fun append(number:String,status:Int,marked:Boolean){val event=JSONObject().put("number",number).put("at",Instant.now().toString()).put("verification",status).put("marked",marked);prefs.edit().putString("events",JSONArray((listOf(event)+records()).take(100)).toString()).apply()}
    fun clear(){prefs.edit().remove("events").apply()}
    fun diagnostic()=DiagnosticResult("calls","Este móvil",Instant.now().toString(),"Registro local de llamadas observadas",listOf("Alcance" to "Solo eventos entregados por Android desde la activación; no es el historial del teléfono", "Eventos" to JSONArray(records()).toString(2)))
    companion object {
        fun available(c:Context)=Build.VERSION.SDK_INT>=29 && c.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)==true
        fun active(c:Context)=available(c) && c.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}

/** System-only binding. Allow the call immediately; optional local warning never rejects it. */
class CallGuardService:CallScreeningService() {
    override fun onScreenCall(details:Call.Details) {
        if(Build.VERSION.SDK_INT>=29 && details.callDirection!=Call.Details.DIRECTION_INCOMING)return
        respondToCall(details,CallResponse.Builder().setDisallowCall(false).setRejectCall(false).setSkipCallLog(false).setSkipNotification(false).build())
        if(Build.VERSION.SDK_INT<29)return
        runCatching {
            val raw=details.handle?.takeIf{it.scheme=="tel"}?.schemeSpecificPart.orEmpty()
            val number=CallPolicy.number(raw).orEmpty()
            val status=if(Build.VERSION.SDK_INT>=30)details.callerNumberVerificationStatus else 0
            val journal=CallJournal(this);val marked=number.isNotEmpty() && number in journal.marked()
            journal.append(number,status,marked)
            if(CallPolicy.warn(marked,status)) {
                val manager=getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel("call-review","Avisos de llamadas",NotificationManager.IMPORTANCE_HIGH))
                val intent=Intent(this,MainActivity::class.java).putExtra("open_calls",true)
                val pending=PendingIntent.getActivity(this,910,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val text=if(marked)"Coincide con tu lista de revisión" else "La verificación del operador ha fallado; revisa la llamada"
                if(NotificationManagerCompat.from(this).areNotificationsEnabled())manager.notify(910,NotificationCompat.Builder(this,"call-review").setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Revisar llamada · "+CallPolicy.masked(number)).setContentText(text).setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
            }
        }
    }
}
