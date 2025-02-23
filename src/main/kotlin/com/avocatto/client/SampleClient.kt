package com.avocatto.client

import com.avocatto.server.processors.voice.elevenlabs.*
import com.avocatto.server.utils.PlaybackService
import com.avocatto.server.utils.RecordingService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.*

data class AudioDataMessage(val type: String = "audio_data", val buffer: String)
private val playback = PlaybackService()


/* Simulated edge device */
suspend fun simulateClientConnection() = withContext(Dispatchers.IO) {


        val sampleClient = SampleClient()


        val streamingJob = launch {
            println("Connecting to server...")
            sampleClient.connect("AD5TF33vdbRmZ9JaN4TQ")
        }


        /* Use Mic to record and forward to server */
        val recordingJob = launch {
            RecordingService.recordAndStreamVoiceObservable().subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                        sampleClient.sendText("""{"type":"audio_data", "buffer": "$it"}""")
                }
            }
        }


        /* Use Mic to record and forward to server */
        val playbackJob = launch {
            playback.playAudioStream()
        }



    joinAll(streamingJob, recordingJob, playbackJob)
}


class SampleClient {

    private var session: DefaultClientWebSocketSession? = null
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }




    fun connect(deviceId: String, token: String = "") = runBlocking {
        client.webSocket(
            urlString = "ws://127.0.0.1:8080/stream?deviceId=${deviceId}&token=${token}"
        ) {
            println("Mock client connected!")
            session = this@webSocket

            val incomingJob = launch {
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                handleIncomingMessage(text)
                            }
                            is Frame.Ping -> send(Frame.Pong(frame.readBytes()))
                            else -> { /* Handle other frame types if needed */ }
                        }
                    }
                } catch (e: Exception) {
                    println("Error in incoming loop: $e")
                } finally {
                    println("Disconnected from server")
                }
            }

            joinAll(incomingJob)
        }
        client.close()
        session = null
        println("Websocket closed!")
    }

    fun close() {
        session = null
        client.close()
    }

    suspend fun <T> sendWsMessage(message: T, gson: Gson = Gson()) {
        try {
            val json = gson.toJson(message)
            session?.send(Frame.Text(json))?.also {
                println("client >  server")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendText(message: String) {
        try {
            session?.send(Frame.Text(message))?.also {
                println("client >  server")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    val gson: Gson = GsonBuilder().create()

    suspend fun handleIncomingMessage(json: String) {
        withContext(Dispatchers.IO) {
            println("client received buffer")
            val message = gson.fromJson(json, AudioDataMessage::class.java)
            val decodedBytes = Base64.getDecoder().decode(message.buffer)
            playback.pipedOutput.write(decodedBytes)
        }
    }


}