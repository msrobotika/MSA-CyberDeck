package com.msarobotika.cyberdeck

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory observation survives a configuration change, never retains an Activity or reader. */
class DeckSessionModel:ViewModel() {
    val nfc=MutableStateFlow(NfcSnapshot())
    val exposure=MutableStateFlow(ExposureState())
    val magnetic=MutableStateFlow<List<MagneticSample>>(emptyList())
    val magneticLive=MutableStateFlow(false)
    val magneticReference=MutableStateFlow<Double?>(null)
    var exposureRunner:ExposureRunner?=null
    val ai=MutableStateFlow(LocalAiState())
    val agent=MutableStateFlow(AgentSession())
    // Draft survives Activity recreation and lazy item disposal, without writing spoken text to disk.
    val agentDraft=MutableStateFlow("")
    val agentRecent=MutableStateFlow<List<AgentExchange>>(emptyList())
    val agentSpeakReplies=MutableStateFlow(false)
    val agentLastSpokenId=MutableStateFlow("")
    val agentMode=MutableStateFlow(false)
    val aiSource=MutableStateFlow<SavedAiContext?>(null)
    val aiRequest=MutableStateFlow<AiRequest?>(null)
    val aiImporting=MutableStateFlow(false)
    val aiImportStatus=MutableStateFlow("")
    val aiModel=MutableStateFlow<AiModelInfo?>(null)
    val apk=MutableStateFlow<ApkSnapshot?>(null)
    val phone=MutableStateFlow<PhoneSnapshot?>(null)
    val diagnostic=MutableStateFlow<DiagnosticResult?>(null)
    val qr=MutableStateFlow<QrObservation?>(null)
    val samples=MutableStateFlow<List<ResourceSample>>(emptyList())
    val phoneLive=MutableStateFlow(false)
}
