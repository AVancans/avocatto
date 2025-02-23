package com.avocatto.server.processors.voice.elevenlabs

import com.google.gson.*
import java.lang.reflect.Type

// Event data classes
data class UserTranscriptEvent(val user_transcript: String)
data class AgentResponseEvent(val agent_response: String)
data class AudioEvent(val audio_base_64: String, val event_id: Int)
data class InternalTentativeAgentResponseEvent(val tentative_agent_response: String)
data class InternalTurnProbabilityEvent(val probability: Double)
data class ConversationInitiationMetadataEvent(
    val conversation_id: String,
    val agent_output_audio_format: String,
    val user_input_audio_format: String
)
data class PingEvent(val event_id: Int, val ping_ms: Int)
data class ClientToolCallEvent(
    val tool_name: String,
    val tool_call_id: String,
    val parameters: Map<String, String>
)

// Base class for incoming messages.
open class IncomingMessage(val type: String)

// Specific message types.
data class UserTranscriptMessage(
    val user_transcription_event: UserTranscriptEvent
) : IncomingMessage("user_transcript")

data class AgentResponseMessage(
    val agent_response_event: AgentResponseEvent
) : IncomingMessage("agent_response")

data class AudioMessage(
    val audio_event: AudioEvent
) : IncomingMessage("audio")

//data class InterruptionMessage() : IncomingMessage("interruption_event")

data class InternalTentativeAgentResponseMessage(
    val tentative_agent_response_internal_event: InternalTentativeAgentResponseEvent
) : IncomingMessage("internal_tentative_agent_response")

data class InternalTurnProbabilityMessage(
    val turn_event: InternalTurnProbabilityEvent
) : IncomingMessage("internal_turn_probability")

data class ConversationInitiationMetadataMessage(
    val conversation_initiation_metadata_event: ConversationInitiationMetadataEvent
) : IncomingMessage("conversation_initiation_metadata")

data class PingMessage(
    val ping_event: PingEvent
) : IncomingMessage("ping")

//data class InterruptionEvent(
//    val ping_event: PingEvent
//) : IncomingMessage("interruption")

data class ClientToolCallMessage(
    val client_tool_call: ClientToolCallEvent
) : IncomingMessage("client_tool_call")

data class InterruptionEvent(
    val event_id: Int? = null
)

data class InterruptionMessage(
    val interruption_event: InterruptionEvent? = null
) : IncomingMessage("interruption")

// Correction event data class.
data class CorrectionEvent(
    val corrected_response: String
)

// Agent response correction message.
data class AgentResponseCorrectionMessage(
    val correction_event: CorrectionEvent
) : IncomingMessage("agent_response_correction")


// Custom deserializer to handle polymorphic deserialization based on the "type" field.
class IncomingMessageDeserializer : JsonDeserializer<IncomingMessage> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IncomingMessage {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString
        return when (type) {
            "agent_response_correction" -> context.deserialize(json, AgentResponseCorrectionMessage::class.java)
            "user_transcript" -> context.deserialize(json, UserTranscriptMessage::class.java)
            "agent_response" -> context.deserialize(json, AgentResponseMessage::class.java)
            "audio" -> context.deserialize(json, AudioMessage::class.java)
            "interruption" -> context.deserialize(json, InterruptionMessage::class.java)
            "internal_tentative_agent_response" -> context.deserialize(json, InternalTentativeAgentResponseMessage::class.java)
            "internal_turn_probability" -> context.deserialize(json, InternalTurnProbabilityMessage::class.java)
            "conversation_initiation_metadata" -> context.deserialize(json, ConversationInitiationMetadataMessage::class.java)
            "ping" -> context.deserialize(json, PingMessage::class.java)
            "client_tool_call" -> context.deserialize(json, ClientToolCallMessage::class.java)
            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}
