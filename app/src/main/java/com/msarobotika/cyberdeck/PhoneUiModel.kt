package com.msarobotika.cyberdeck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** Tool state survives configuration and lazy-list disposal; never retains an Activity. */
class PhoneUiModel(private val saved:SavedStateHandle):ViewModel(){
 val advanced=saved.getStateFlow("phone.advanced",false)
 val section=saved.getStateFlow("phone.section","Seguridad")
 val selected=saved.getStateFlow<String?>("phone.selected",null)
 val before=saved.getStateFlow("phone.before","")
 val observedAt=saved.getStateFlow("phone.observedAt","")
 val showAll=saved.getStateFlow("phone.showAll",false)
 fun advanced(value:Boolean){saved["phone.advanced"]=value}
 fun section(value:String){require(value in setOf("Seguridad","Conexión","Recursos","Hardware","Plan"));saved["phone.section"]=value}
 fun showAll(value:Boolean){saved["phone.showAll"]=value}
 fun open(check:PhoneCheck,at:String){saved["phone.before"]=check.value;saved["phone.observedAt"]=at;saved["phone.selected"]=PhoneBriefPolicy.key(check)}
 fun back(){saved["phone.selected"]=null}
}
