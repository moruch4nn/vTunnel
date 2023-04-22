package dev.mr3n.vtunnel.tunnel

import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import dev.mr3n.vtunnel.VTunnel

private val cachedPingsInfo = mutableMapOf<String, CachedPingInfo>()

class CachedPingInfo(val registeredServer: RegisteredServer) {
    private var lastUpdate = System.currentTimeMillis()

    private var cachedPing = this.getNonCachedInfo()

    private fun getNonCachedInfo(): ServerPing = this.registeredServer.ping().get().asBuilder()
        .onlinePlayers(VTunnel.SERVER.playerCount)
        .maximumPlayers(VTunnel.SERVER.configuration.showMaxPlayers)
        .clearSamplePlayers()
        .build()

    fun get(): ServerPing {
        if(System.currentTimeMillis() - this.lastUpdate > 1000) {
            this.cachedPing = this.getNonCachedInfo()
            this.lastUpdate = System.currentTimeMillis()
        }
        return cachedPing
    }
}

fun RegisteredServer.cachedPingInfo(): ServerPing {
    return cachedPingsInfo.getOrPut(this.serverInfo.name) { CachedPingInfo(this) }.get()
}