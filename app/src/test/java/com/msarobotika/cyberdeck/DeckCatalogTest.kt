package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class DeckCatalogTest {
 @Test fun recentToolsAreMostRecentFirstWithoutDuplicates(){assertEquals(listOf("NFC","USB","Red"),DeckCatalog.recent(listOf("USB","NFC","Red"),"NFC"))}
 @Test fun recentToolsDiscardUnknownAndRootRoutes(){assertEquals(listOf("USB"),DeckCatalog.recent(listOf("USB","Resumen","unknown"),"Herramientas"))}
 @Test fun recentToolsStayBoundedAcrossRestoration(){assertEquals(listOf("NFC","USB","Red"),DeckCatalog.recent(listOf("NFC","USB","NFC","Red","Web")))}
 @Test fun everyExistingToolHasExactlyOneArea(){
  val routes=DeckCatalog.areas.flatMap{it.routes}
  assertEquals(routes.size,routes.distinct().size)
  assertEquals(deckTools.map{it.route}.toSet(),routes.toSet())
 }
 @Test fun searchIgnoresAccentsAndCase(){assertEquals(listOf("Magnético"),DeckCatalog.tools("","MAGNETÓMETRO").map{it.route})}
 @Test fun searchFindsTasksOutsideSelectedArea(){assertTrue(DeckCatalog.tools("hardware","transferir").any{it.route=="Recibir"})}
 @Test fun searchSupportsMultipleWords(){assertEquals(listOf("Sesiones"),DeckCatalog.tools("","transferir cifrado").map{it.route})}
 @Test fun emptyQueryShowsOnlySelectedArea(){assertEquals(listOf("Bluetooth","NFC"),DeckCatalog.tools("proximity"," ").map{it.route})}
 @Test fun overviewDoesNotDumpAllTools(){assertTrue(DeckCatalog.tools("","").isEmpty());assertEquals(deckTools,DeckCatalog.tools("all",""))}
 @Test fun unmatchedInputDoesNotInventATool(){assertTrue(DeckCatalog.tools("all","not-a-real-tool-937").isEmpty())}
}
