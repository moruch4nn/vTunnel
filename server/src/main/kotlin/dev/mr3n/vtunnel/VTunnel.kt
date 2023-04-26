package dev.mr3n.vtunnel

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.mr3n.vtunnel.tunnel.cachedPingInfo
import dev.mr3n.vtunnel.tunnel.startTunnelingAllocator
import java.net.InetSocketAddress
import java.util.*
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull

@Plugin(id = "vtunnel")
class VTunnel @Inject constructor(val server: ProxyServer, logger: Logger) {
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
    fun on(event: ProxyPingEvent) {
        val virtualHostStr = event.connection.virtualHost.map(InetSocketAddress::getHostString).orElse("").lowercase(Locale.ROOT)
        (forcedHosts(virtualHostStr) ?:tryServer())?.cachedPingInfo().let(event::setPing)
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

        val customForcedHosts = mutableMapOf<String,String>()
        val tryFirst = System.getenv("VTUNNEL_TRY")?.split(",")?:listOf()

        fun forcedHosts(virtualHostsStr: String): RegisteredServer? {
            return customForcedHosts[virtualHostsStr]?.let { SERVER.getServer(it).getOrNull() }
        }

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