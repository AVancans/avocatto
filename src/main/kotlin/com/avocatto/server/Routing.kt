package org.avocatto.com.avocatto


import com.avocatto.server.agents.AgentsService
import com.avocatto.server.agents.getDevice
import com.avocatto.server.streaming.StreamingService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.rx3.awaitSingleOrNull

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        webSocket("/stream") {
            val deviceId = call.request.queryParameters["deviceId"]
            val token = call.request.queryParameters["token"]
            println("## Client Connected $deviceId $token ##")

            // TODO auth

            if (deviceId == null || token == null) {
                println("Missing deviceId or token")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing deviceId or token"))
                return@webSocket
            }

            println("Getting device $deviceId...")

            val device = getDevice(deviceId).awaitSingleOrNull()

            if (device == null) {
                println("Device not found")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device not found"))
                return@webSocket
            }

            val agents = AgentsService().also {
                it.listen()
            }

            StreamingService( agents, this ).connect(deviceId)
        }
    }
}
