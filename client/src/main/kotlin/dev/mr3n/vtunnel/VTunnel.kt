package dev.mr3n.vtunnel

import dev.mr3n.paperallinone.nms.NmsUtils.accessible
import dev.mr3n.vtunnel.model.AuthFrame
import dev.mr3n.vtunnel.model.NewConnectionNotify
import dev.mr3n.vtunnel.tcp.PacketTransfer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.net.Socket
import kotlin.concurrent.thread

@Suppress("unused")
class VTunnel: JavaPlugin(), Listener {
    private val minecraftServer: Any = Class.forName("net.minecraft.server.MinecraftServer").getMethod("getServer").accessible().invoke(null)

    override fun onEnable() {
        thread { runBlocking { startWebSocketClient() } }
        this.server.pluginManager.registerEvents(this, this)
        // 認証サーバーをオフにする(Proxyを挟むため)

        if(this.server.onlineMode) {
            try {
                val setUsesAuthentication = minecraftServer::class.java.getMethod("setUsesAuthentication", Boolean::class.java).accessible()
                setUsesAuthentication.invoke(minecraftServer, false)
            } catch (_: Exception) {
                try {
                    val g = minecraftServer::class.java.getMethod("d", Boolean::class.java).accessible()
                    g.invoke(minecraftServer, false)
                } catch (_: Exception) {
                    logger.warning("============================================================")
                    logger.warning("Change the online-mode item in the server.properties to false.")
                    logger.warning("============================================================")
                    server.shutdown()
                }
            }
        }

        // Bungeeの設定を有効にする
        Class.forName("org.spigotmc.SpigotConfig").getField("bungee").set(null, true)
    }

    val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    suspend fun startWebSocketClient() {
        val host = System.getenv("VTUNNEL_HOST")?:"akamachi.net"
        val token = System.getenv("VTUNNEL_TOKEN")
        while (true) {
           try {
               client.webSocket(host = host, port = 60000, path = "/vtunnel") {
                   // Successfully connected to server
                   sendSerialized(AuthFrame(token))
                   while (true) {
                       val newConn: NewConnectionNotify = receiveDeserialized()
                       try {
                           val bridgeSocket = Socket(host, newConn.port)
                           val outputSocket = bridgeSocket.getOutputStream()
                           outputSocket.write(Json.encodeToString(AuthFrame(newConn.token)).toByteArray())
                           outputSocket.flush()
                           val clientSocket = Socket("127.0.0.1", server.port)
                           PacketTransfer(bridgeSocket, clientSocket)
                       } catch (e: Exception) {
                           e.printStackTrace()
                       }
                   }
               }
           } catch (_: Exception) { }
            Thread.sleep(30000) // when disconnected from the velocity, wait 30 seconds then reconnecting to velocity
        }
    }
}