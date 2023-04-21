package dev.mr3n.vtunnel

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.mr3n.vtunnel.tunnel.startTunnelingAllocator
import net.kyori.adventure.text.Component
import org.w3c.dom.Text
import java.net.InetSocketAddress
import java.util.*
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull

@Plugin(id = "vtunnel")
class VTunnel @Inject constructor(val server: ProxyServer, val logger: Logger) {
    @Subscribe
    fun on(event: ProxyInitializeEvent) {
        thread { startTunnelingAllocator() }
    }

    @Subscribe
    fun on(event: PlayerChooseInitialServerEvent) {
        val virtualHostStr = event.player.virtualHost.map(InetSocketAddress::getHostString).orElse("").lowercase(Locale.ROOT)
        (customForcedHosts[virtualHostStr]?:tryServer())?.let(event::setInitialServer)
    }

    @Subscribe
    fun on(event: ProxyPingEvent) {
        val virtualHostStr = event.connection.virtualHost.map(InetSocketAddress::getHostString).orElse("").lowercase(Locale.ROOT)
        (customForcedHosts[virtualHostStr]?:tryServer())?.ping()?.get().let(event::setPing)
    }

    @Subscribe
    fun on(event: KickedFromServerEvent) {
        val server = tryServer(event.server.serverInfo.name)
        event.result = KickedFromServerEvent.RedirectPlayer.create(server)
    }

    init {
        SERVER = server
        LOGGER = logger
    }

    companion object {
        lateinit var SERVER: ProxyServer
        lateinit var LOGGER: Logger

        val customForcedHosts = mutableMapOf<String,RegisteredServer>()
        private val tryFirst = System.getenv("VTUNNEL_TRY")?.split(",")?:listOf()

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