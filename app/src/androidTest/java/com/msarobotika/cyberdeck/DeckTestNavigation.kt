package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule

fun ComposeContentTestRule.openDeck(route:String) {
    if(route in listOf("Resumen","Herramientas","Evidencias","Informes")) {
        onNodeWithTag("navigate-$route").performClick()
    } else {
        onNodeWithTag("navigate-Herramientas").performClick()
        onNodeWithTag("deck-list").performScrollToIndex(0)
        onNodeWithText("Buscar una herramienta").performTextClearance()
        onNodeWithText("Buscar una herramienta").performImeAction()
        onNodeWithTag("deck-list").performScrollToNode(hasTestTag("tool-$route"))
        onNodeWithTag("tool-$route").performClick()
    }
}
