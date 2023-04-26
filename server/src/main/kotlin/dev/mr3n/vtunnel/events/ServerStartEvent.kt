package dev.mr3n.vtunnel.events

import com.velocitypowered.api.proxy.server.RegisteredServer

class ServerStartEvent(val registeredServer: RegisteredServer, val serverName: String = registeredServer.serverInfo.name)