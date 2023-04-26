package dev.mr3n.vtunnel.tunnel

import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerPing
import dev.mr3n.vtunnel.VTunnel
import kotlin.jvm.optionals.getOrNull

private val cachedPingsInfo = mutableMapOf<String, CachedPingInfo>()

internal class CachedPingInfo(val serverName: String) {
    private var lastUpdate = System.currentTimeMillis()

    private var cachedPing = this.getNonCachedInfo()

    private fun getNonCachedInfo(): ServerPing? {
        try {
            return (VTunnel.SERVER.getServer(serverName)?.getOrNull()?:return null)
                .ping().get().asBuilder()
                .onlinePlayers(VTunnel.SERVER.playerCount)
                .maximumPlayers(VTunnel.SERVER.configuration.showMaxPlayers)
                .also { if(!it.favicon.isPresent) { VTunnel.SERVER.configuration.favicon.ifPresent(it::favicon) } }
                .clearSamplePlayers()
                .build()
        } catch (_: Exception) {
            return null
        }
    }

    fun get(): ServerPing? {
        if(System.currentTimeMillis() - this.lastUpdate > 1000) {
            this.cachedPing = this.getNonCachedInfo()
            this.lastUpdate = System.currentTimeMillis()
        }
        return cachedPing
    }
}

fun RegisteredServer.cachedPingInfo(): ServerPing? {
    return cachedPingsInfo.getOrPut(this.serverInfo.name) { CachedPingInfo(this.serverInfo.name) }.get()
}