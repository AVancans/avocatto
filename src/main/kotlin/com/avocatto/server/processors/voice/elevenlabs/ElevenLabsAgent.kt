package com.avocatto.server.processors.voice.elevenlabs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ElevenLabsAgent(
    private val systemPrompt: String? = null,
    private val firstMessage: String? = null
    ) {


    private var session: DefaultClientWebSocketSession? = null
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }
    private val agentResponseStream = BehaviorSubject.create<String>()

    fun getSession(): DefaultClientWebSocketSession? = session

    fun listenToAgentResponse(): Observable<String> {
        return agentResponseStream
    }

    fun connectPrivate(agentId: String, token: String = "") = runBlocking {


        client.webSocket(
            urlString = "wss://api.elevenlabs.io/v1/convai/conversation?agent_id=$agentId&token=$token"
        ) {
            println("Elevenlabs connected!")
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
                    println("Listening for-loop complete.")
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
                print("server > elevenlabs ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    // Create a Gson instance registering our custom deserializer.
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(IncomingMessage::class.java, IncomingMessageDeserializer())
        .create()

    // Generic handler: Parses the JSON string to an IncomingMessage and dispatches to the appropriate handler.
    suspend fun handleIncomingMessage(json: String) {
        val message = gson.fromJson(json, IncomingMessage::class.java)
        when (message) {
            is UserTranscriptMessage -> handleUserTranscript(message)
            is AgentResponseMessage -> handleAgentResponse(message)
            is AudioMessage -> handleAudioMessage(message)
            is InternalTentativeAgentResponseMessage -> handleInternalTentativeAgentResponse(message)
            is InternalTurnProbabilityMessage -> handleInternalTurnProbability(message)
            is ConversationInitiationMetadataMessage -> handleConversationInitiationMetadata(message)
            is PingMessage -> handlePing(message)
            is ClientToolCallMessage -> handleClientToolCall(message)
            else -> println("Unhandled message type: ${message.type}")
        }
    }

    // Mock handler functions for each message type.
    fun handleUserTranscript(message: UserTranscriptMessage) {
        println("User: ${message.user_transcription_event.user_transcript}")
    }

    fun handleAgentResponse(message: AgentResponseMessage) {
        println("Agent: ${message.agent_response_event.agent_response}")
    }

    fun handleAudioMessage(message: AudioMessage) {
        println("handleAudioMessage")
        agentResponseStream.onNext(message.audio_event.audio_base_64)
    }

    fun handleInternalTentativeAgentResponse(message: InternalTentativeAgentResponseMessage) {
        println("Mock: Handling internal tentative agent response: ${message.tentative_agent_response_internal_event.tentative_agent_response}")
    }

    fun handleInternalTurnProbability(message: InternalTurnProbabilityMessage) {
        println("Mock: Handling turn probability: ${message.turn_event.probability}")
    }

    suspend fun handleConversationInitiationMetadata(message: ConversationInitiationMetadataMessage) {
        println("< Handling conversation initiation metadata, conversation ID: ${message.conversation_initiation_metadata_event.conversation_id}")
        println(message)
        ConversationInitiationClientData(
            conversation_config_override = ConversationConfigOverride(
                agent = AgentConfig(
                    prompt = systemPrompt?.let { PromptConfig(it) },
                    first_message = firstMessage
                )
            )
        ).let {
            println("Registering elevenlabs converstation with system prompt: ")
            println("$it")
            sendWsMessage(it)
        }
    }

    suspend fun handlePing(message: PingMessage) {
        sendWsMessage(
            PongMessage(event_id = message.ping_event.event_id)
        )
        println("< Ping (${message.ping_event.ping_ms}ms)")
    }

    fun handleClientToolCall(message: ClientToolCallMessage) {
        println("Mock: Handling client tool call: tool name ${message.client_tool_call.tool_name}, parameters: ${message.client_tool_call.parameters}")
    }


}
