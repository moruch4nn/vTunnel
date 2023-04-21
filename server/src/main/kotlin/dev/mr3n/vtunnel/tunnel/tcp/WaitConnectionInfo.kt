package dev.mr3n.vtunnel.tunnel.tcp

import java.net.Socket

class WaitConnectionInfo(val run: (Socket)->Unit, val id: String, val createdAt: Long = System.currentTimeMillis())