package dev.mr3n.vtunnel

import dev.mr3n.paperallinone.nms.NmsUtils.accessible
import dev.mr3n.paperallinone.task.runTaskTimer
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
import java.util.Properties
import kotlin.concurrent.thread

@Suppress("unused")
class VTunnel: JavaPlugin(), Listener {
    private val minecraftServer: Any = Class.forName("net.minecraft.server.MinecraftServer").getMethod("getServer").accessible().invoke(null)

    private var vTunnelThread: Thread? = null

    private var isStartedWatchDog = false

    /**
     * vTunnelのwatchDogを開始する
     */
    private fun startWatchDog() {
        // watchDogがすでに起動している場合はreturn
        if(this.isStartedWatchDog) { return }
        this.isStartedWatchDog = true
        logger.info("watchDogを起動しました。")
        this.runTaskTimer(20*5L, 20*5L) { _ ,_ ->
            if(vTunnelThread?.isAlive == true) { return@runTaskTimer }
            logger.info("vTunnelが切断されたため再接続を試みます。")
            this.vTunnelThread = thread(name = "vtunnel-client") { runBlocking { startWebSocketClient() } }
        }
    }

    override fun onEnable() {
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
                    logger.warning("Change the online-mode value in the server.properties to false.")
                    logger.warning("============================================================")
                    server.shutdown()
                }
            }
        }

        // Bungeeの設定を有効にする
        Class.forName("org.spigotmc.SpigotConfig").getField("bungee").set(null, true)

        val threads = Thread.getAllStackTraces().keys

        val thread = threads.filter { it.isAlive }.find { it.name == "vtunnel-client" }

        if(thread != null) {
            this.vTunnelThread = thread
            logger.info("すでに別スレッドでvTunnelが稼働しているため接続をスキップします(これは正常な処理です。)")
        }
        this.startWatchDog()
    }

    val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    suspend fun startWebSocketClient() {
        val clientProperties = getResource("client.properties")
        val properties = Properties()
        try {
            properties.load(clientProperties.bufferedReader())
        } catch (_: Exception) {}


        val host = System.getenv("VTUNNEL_HOST")?:properties.getProperty("host", "play.akamachi.net")
        val token = System.getenv("VTUNNEL_TOKEN")?:properties.getProperty("token")?:throw Exception("vtunnel-tokenが設定されていません。")
        try {
            client.webSocket(host = host, port = 60000, path = "/vtunnel") {
                // Successfully connected to server
                sendSerialized(AuthFrame(token))
                logger.info("vTunnelサーバーへの接続が正常に完了しました。")
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
    }

    companion object {

    }
}