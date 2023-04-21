package dev.mr3n.vtunnel.tunnel.tcp

import com.auth0.jwt.JWT
import dev.mr3n.vtunnel.model.AuthFrame
import dev.mr3n.vtunnel.model.TCPAuthModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.Timer
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

object BridgeConnection {
    val port = 60001
    val serverSocket = ServerSocket(port)

    private val timer = Timer()

    private val base64Encoder = Base64.getEncoder()

    private val waitCons = CopyOnWriteArrayList<WaitConnectionInfo>()

    fun wait(con: (Socket)->Unit): String {
        val id = base64Encoder.encode("${UUID.randomUUID()}${UUID.randomUUID()}".encodeToByteArray()).decodeToString()
        val info = WaitConnectionInfo(con, id, System.currentTimeMillis())
        this.waitCons.add(info)
        timer.schedule(1000 * 10) { this@BridgeConnection.waitCons.remove(info) }
        return id
    }

    val thread = thread {
        while (true) {
            val socket = serverSocket.accept()
            try {
                val buffer = ByteArray(60000)
                val len = socket.getInputStream().read(buffer)
                if(len == -1) { continue }
                val authInfo = TCPAuthModel(JWT.decode(Json.decodeFromString<AuthFrame>(String(buffer, 0, len)).token))

                val info = waitCons.find { it.id == authInfo.id }?:continue
                info.run(socket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}