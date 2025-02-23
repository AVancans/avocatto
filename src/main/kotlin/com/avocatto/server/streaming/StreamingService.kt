package com.avocatto.server.streaming

import com.avocatto.server.agents.*
import com.avocatto.server.processors.voice.elevenlabs.ElevenLabsAgent
import com.avocatto.server.processors.voice.elevenlabs.UserAudioChunk
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.rx3.awaitSingle

/** Incoming message types **/
data class IncomingUserAudioData(val type: String, val buffer: String)
data class IncomingPing(val type: String)

/** Outgoing message types **/
data class OutgoingAgentAudioData(val type: String = "audio_data", val buffer: String)
data class OutgoingPing(val type: String = "ping")

class StreamingService(
    private val agentsService: AgentsService,
    private val session: DefaultWebSocketServerSession
) {

    private val gson = Gson()
    private lateinit var device: Device

    private val userStream: BehaviorSubject<String> = BehaviorSubject.create()

    suspend fun connect(deviceId: String) {

        this.device = getDevice(deviceId).awaitSingle()

        listenToDevice(deviceId).distinct { it.agent  }.subscribe { device ->
            this.device = device
            reconnectToAgent(device)
        }


        withContext(Dispatchers.IO)
        {
            val listen = launch {
                listenerUser()
            }
            joinAll(listen)
        }
    }

    private fun reconnectToAgent(device: Device)  {
        val agent: Agent = agentsService.getById(device.agent) ?: throw Exception()

        if (agent.voice == "ElevenLabs") {

            val voiceAgent = ElevenLabsAgent(
                agent.system_prompt,
                agent.first_message
            )

            // Make underlying agent customizable
            val voiceAgentJob = CoroutineScope(Dispatchers.IO).launch {
                voiceAgent.connectPrivate("")
            }

            voiceAgent.listenToAgentResponse().subscribe { base64 ->
                println("<AGENT DATA *****>")
                CoroutineScope(Dispatchers.IO).launch {
                    sendAgentAudioData(base64)
                }
            }

            userStream.subscribe { base64 ->
                CoroutineScope(Dispatchers.IO).launch {
                    voiceAgent.sendWsMessage(UserAudioChunk(user_audio_chunk = base64))
                }
            }

        } else {
            throw NotImplementedError()
        }

    }


    private suspend fun listenerUser() = with(session) {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val receivedText = frame.readText()
                try {
                    val jsonObject = gson.fromJson(receivedText, JsonObject::class.java)
                    val messageType = jsonObject.get("type").asString
                    when (messageType) {
                        "audio_data" -> {
                            val message = gson.fromJson(receivedText, IncomingUserAudioData::class.java)
                            userStream.onNext(message.buffer)
                        }
                        "ping" -> {
                            val message = gson.fromJson(receivedText, IncomingPing::class.java)
                            // Process ping, for example by sending back a ping
                            println("Received ping")
                        }
                        else -> {
                            println("Unknown message type: $messageType")
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing message: $e")
                }
            }
        }
    }

    suspend private fun sendAgentAudioData(buffer: String) = with(session) {
        val message = OutgoingAgentAudioData(buffer = buffer)
        val json = gson.toJson(message)
        println("server > client ${json}")
        send(Frame.Text(json))
    }

    suspend private fun sendPing() = with(session) {
        val message = OutgoingPing()
        val json = gson.toJson(message)
        send(Frame.Text(json))
    }



}