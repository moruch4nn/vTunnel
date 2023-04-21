package dev.mr3n.vtunnel.tunnel

import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerPing

private val cachedPingsInfo = mutableMapOf<String, CachedPingInfo>()

class CachedPingInfo(val registeredServer: RegisteredServer) {
    private var lastUpdate = System.currentTimeMillis()

    private var cachedPing = registeredServer.ping().get()

    fun get(): ServerPing {
        if(System.currentTimeMillis() - this.lastUpdate > 1000) {
            this.cachedPing = registeredServer.ping().get()
            this.lastUpdate = System.currentTimeMillis()
        }
        return cachedPing
    }
}

fun RegisteredServer.cachedPingInfo(): ServerPing {
    return cachedPingsInfo.getOrPut(this.serverInfo.name) { CachedPingInfo(this) }.get()
}