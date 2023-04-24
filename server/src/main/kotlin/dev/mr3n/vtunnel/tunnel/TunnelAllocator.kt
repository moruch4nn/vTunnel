package dev.mr3n.vtunnel.tunnel

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.mr3n.vtunnel.VTunnel
import dev.mr3n.vtunnel.model.AuthFrame
import dev.mr3n.vtunnel.model.InitAuthModel
import dev.mr3n.vtunnel.tunnel.tcp.BridgeNotifierConnection
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.jvm.optionals.getOrNull

val connections = Collections.synchronizedMap(LinkedHashMap<String, BridgeNotifierConnection>())


val algorithm = Algorithm.HMAC512(System.getenv("VTUNNEL_SECRET"))
val verifier = JWT.require(algorithm).acceptExpiresAt(5).build()

private fun Routing.setupWebSocket() {
    webSocket("/vtunnel") {
        try {
            val authInfo: AuthFrame = receiveDeserialized()
            val jwt = verifier.verify(authInfo.token)
            val initAuth = InitAuthModel(jwt)
            val serverName = initAuth.name
            if(connections.containsKey(serverName)) { return@webSocket close() }
            val forcedHosts = initAuth.forcedHosts

            val thisConnection = BridgeNotifierConnection(this)

            VTunnel.SERVER.getServer(serverName)?.getOrNull()?.serverInfo?.let(VTunnel.SERVER::unregisterServer)
            val info = ServerInfo(serverName, InetSocketAddress("localhost",thisConnection.publicPort))
            val registeredServer = VTunnel.SERVER.registerServer(info)
            forcedHosts.forEach { VTunnel.customForcedHosts[it] = registeredServer.serverInfo.name }

            try {
                connections[initAuth.name] = thisConnection

                VTunnel.LOGGER.info("${serverName}との新しいブリッジコネクションを確立しました。")

                for (frame in incoming) {  }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                VTunnel.LOGGER.info("${initAuth.name}とのブリッジコネクションが切断されました。")

                forcedHosts.forEach(VTunnel.customForcedHosts::remove)
                VTunnel.SERVER.unregisterServer(info)

                connections.remove(initAuth.name)
            }
        } catch (_: Exception) { }
    }
}

fun startTunnelingAllocator() {
    embeddedServer(
        factory = Netty,
        port = 60000,
        host = "0.0.0.0",
        module = {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            routing { this.setupWebSocket() }
        }
    ).start(true)
}