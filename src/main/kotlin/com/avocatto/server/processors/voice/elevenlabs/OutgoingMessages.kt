package com.avocatto.server.processors.voice.elevenlabs

// Outgoing message data classes

data class ConversationInitiationClientData(
    val type: String = "conversation_initiation_client_data",
    val conversation_config_override: ConversationConfigOverride? = null,
    val custom_llm_extra_body: CustomLLMExtraBody? = null,
    val dynamic_variables: Map<String, String>? = null
)

data class ConversationConfigOverride(
    val agent: AgentConfig? = null,
    val tts: TTSConfig? = null
)

data class AgentConfig(
    val prompt: PromptConfig? = null,
    val first_message: String? = null,
    val language: String? = null
)

data class PromptConfig(
    val prompt: String
)

data class TTSConfig(
    val voice_id: String
)

data class CustomLLMExtraBody(
    val temperature: Double,
    val max_tokens: Int
)

data class UserAudioChunk(
    val user_audio_chunk: String
)

data class PongMessage(
    val type: String = "pong",
    val event_id: Int
)

data class ClientToolResult(
    val type: String = "client_tool_result",
    val tool_call_id: String,
    val result: String,
    val is_error: Boolean
)

// Helper function to send a message over the WebSocket.
// It uses GSON to serialize the message into JSON.


// Example usage: connect and send messages
//object OutgoingMessageExample {
//
//    private val client = HttpClient(CIO) {
//        install(WebSockets)
//    }
//
//    fun connectAndSendMessages(agentId: String, token: String) = runBlocking {
//        client.webSocket(
//            method = HttpMethod.Get,
//            host = "api.elevenlabs.io",
//            port = 8080,
//            path = "/v1/convai/conversation?agent_id=$agentId&token=$token"
//        ) {
//            // 1. Send conversation initiation client data.
//            val convInitData = ConversationInitiationClientData(
//                conversation_config_override = ConversationConfigOverride(
//                    agent = AgentConfig(
//                        prompt = PromptConfig("You are a helpful customer support agent named Alexis."),
//                        first_message = "Hi, I'm Alexis from ElevenLabs support. How can I help you today?",
//                        language = "en"
//                    ),
//                    tts = TTSConfig("21m00Tcm4TlvDq8ikWAM")
//                ),
//                custom_llm_extra_body = CustomLLMExtraBody(
//                    temperature = 0.7,
//                    max_tokens = 150
//                ),
//                dynamic_variables = mapOf(
//                    "user_name" to "John",
//                    "account_type" to "premium"
//                )
//            )
//            sendWsMessage(this, convInitData)
//
//            // 2. Send a user audio chunk.
//            val audioChunkMessage = UserAudioChunk("base64EncodedAudioData==")
//            sendWsMessage(this, audioChunkMessage)
//
//            // 3. Send a pong message.
//            val pongMessage = PongMessage(event_id = 12345)
//            sendWsMessage(this, pongMessage)
//
//            // 4. Send a client tool result.
//            val toolResult = ClientToolResult(
//                tool_call_id = "tool_call_123",
//                result = "Account is active and in good standing",
//                is_error = false
//            )
//            sendWsMessage(this, toolResult)
//        }
//        client.close()
//    }
//}
