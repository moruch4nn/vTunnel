package dev.mr3n.vtunnel.tunnel.tcp

import com.auth0.jwt.JWT
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.mr3n.vtunnel.VTunnel
import dev.mr3n.vtunnel.model.NewConnectionNotify
import dev.mr3n.vtunnel.tcp.PacketTransfer
import dev.mr3n.vtunnel.tunnel.PortManager
import dev.mr3n.vtunnel.tunnel.algorithm
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.jvm.optionals.getOrNull

class BridgeNotifierConnection(val webSocketServerSession: DefaultWebSocketServerSession): Thread() {
    val publicPort = PortManager.port()
    private val public = ServerSocket(publicPort)

    private val transfers = CopyOnWriteArraySet<PacketTransfer>()

    override fun run() {
        while(true) {
            val socket1 = public.accept()
            try {
                val id = BridgeConnection.wait { socket2 ->
                    val packetTransfer = PacketTransfer(socket1,socket2)
                    transfers.add(packetTransfer)

                    packetTransfer.closeProcess { this.transfers.remove(packetTransfer) }
                    packetTransfer.closeProcess { PortManager.free(publicPort) }
                }

                val token = JWT.create().withClaim("id", id).sign(algorithm)
                runBlocking { webSocketServerSession.sendSerialized(NewConnectionNotify(BridgeConnection.port,token)) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init { this.start() }
}