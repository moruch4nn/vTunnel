package dev.mr3n.vtunnel

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.mr3n.vtunnel.tunnel.cachedPingInfo
import dev.mr3n.vtunnel.tunnel.startTunnelingAllocator
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull

@Plugin(id = "vtunnel")
class VTunnel @Inject constructor(val server: ProxyServer, logger: Logger) {

    fun coverAddress(conn: Any) {
        val inetAddress = InetAddress.getByName("0.0.0.0")
        val inetSocketAddress = InetSocketAddress(inetAddress, 0)
        val mcConn = conn::class.java.getDeclaredField("connection")
            .also { it.isAccessible = true }.get(conn)
        mcConn::class.java.getDeclaredField("remoteAddress")
            .also { it.isAccessible = true }.set(mcConn, inetSocketAddress)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun on(event: ProxyInitializeEvent) {
        thread { startTunnelingAllocator() }
    }

    @Subscribe(order = PostOrder.FIRST)
    fun on(event: PlayerChooseInitialServerEvent) {
        val virtualHostStr = event.player.virtualHost.map(InetSocketAddress::getHostString).orElse("").lowercase(Locale.ROOT)
        (forcedHosts(virtualHostStr) ?:tryServer())?.let(event::setInitialServer)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun on(event: PreLoginEvent) {
        val conn = event.connection
        playerAddresses[event.username] = event.connection.remoteAddress
        val delegate = conn::class.java.getDeclaredField("delegate")
            .also { it.isAccessible = true }.get(conn)
        this.coverAddress(delegate)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun on(event: ProxyPingEvent) {
        coverAddress(event.connection)
        val virtualHostStr = event.connection.virtualHost.map(InetSocketAddress::getHostString).orElse("").lowercase(Locale.ROOT)
        (forcedHosts(virtualHostStr)?:tryServer())?.cachedPingInfo()?.let(event::setPing)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun on(event: KickedFromServerEvent) {
        val server = tryServer(event.server.serverInfo.name)
        event.result = KickedFromServerEvent.RedirectPlayer.create(server)
    }

    init {
        SERVER = server
        LOGGER = logger
    }

    companion object {
        internal lateinit var SERVER: ProxyServer
        internal lateinit var LOGGER: Logger

        private val playerAddresses = mutableMapOf<String, InetSocketAddress>()
        val customForcedHosts = mutableMapOf<String,String>()
        val tryFirst = (System.getenv("VTUNNEL_TRY")?.split(",")?:listOf()).toMutableList()

        fun forcedHosts(virtualHostsStr: String): RegisteredServer? {
            return customForcedHosts[virtualHostsStr]?.let { SERVER.getServer(it).getOrNull() }
        }

        fun getIpAddress(username: String): InetSocketAddress? = playerAddresses[username]

        fun tryServer(vararg ignore: String): RegisteredServer? {
            val formattedIgnore = ignore.map { it.lowercase() }
            tryFirst.map(SERVER::getServer)
                .mapNotNull(Optional<RegisteredServer>::getOrNull)
                .forEach { server ->
                    if(ignore.isEmpty()) { return server }
                    if(formattedIgnore.contains(server.serverInfo.name.lowercase())) { return@forEach }
                    return server
                }
            return null
        }
    }
}