package dev.mr3n.vtunnel.tunnel

object PortManager {
    private val ports = (60002..61000)

    private val usingPorts = mutableListOf<Int>()

    fun port(): Int {
        var port: Int
        do { port = ports.random() } while (port in usingPorts)
        return port
    }

    fun free(port: Int) = usingPorts.remove(port)
}