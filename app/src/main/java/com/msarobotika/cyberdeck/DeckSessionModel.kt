package com.msarobotika.cyberdeck

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory observation survives a configuration change, never retains an Activity or reader. */
class DeckSessionModel:ViewModel() {
    val nfc=MutableStateFlow(NfcSnapshot())
}
