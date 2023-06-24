package dev.mr3n.vtunnel

import dev.mr3n.paperallinone.nms.NmsUtils
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
import java.lang.reflect.Method
import java.net.Socket
import kotlin.concurrent.thread

@Suppress("unused")
class VTunnel: JavaPlugin(), Listener {
    private val minecraftServer: Any = NmsUtils.nmsClass("MinecraftServer").getMethod("getServer").accessible().invoke(null)
    private var setUsesAuthentication: Method = minecraftServer::class.java.getMethod("setUsesAuthentication", Boolean::class.java).accessible()

    override fun onEnable() {
        thread { runBlocking { startWebSocketClient() } }
        this.server.pluginManager.registerEvents(this, this)
        // 認証サーバーをオフにする(Proxyを挟むため)
        this.setUsesAuthentication.invoke(minecraftServer, false)
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
                   sendSerialized(AuthFrame(token))
                   logger.info("vTunnelサーバーに接続しました。")
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
            logger.warning("vTunnelとの接続が切断されたため5秒後に再接続を行います。")
            Thread.sleep(5000)
        }
    }
}