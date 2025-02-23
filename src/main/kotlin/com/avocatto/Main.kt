package org.avocatto.com.avocatto

import com.avocatto.client.simulateClientConnection
import com.avocatto.server.utils.PlaybackService
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream


suspend fun main() {

    val serviceAccount = FileInputStream("")
    val options: FirebaseOptions = FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()
    FirebaseApp.initializeApp(options)



    val mockEdge = CoroutineScope(Dispatchers.IO).launch {
            Thread.sleep(2*1000)
            println("Connecting a mock edge device in 2 seconds...")
            Thread.sleep(2*1000)
            println("Connecting a mock edge device.")
            simulateClientConnection()
            println("Mock edge device died.")
    }

    println("Starting server...")
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {}
    }
    install(WebSockets)

    configureRouting()
}
