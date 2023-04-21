package dev.mr3n.vtunnel.tcp

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.thread

class PacketTransfer(private val socket1: Socket, private val socket2: Socket): Closeable {
    private val streams = CopyOnWriteArraySet<Closeable>()
    var isClosed = false
    private val closeProcesses = CopyOnWriteArraySet<()->Unit>()
    val thread1 = thread {
        this.transfer(socket1.getInputStream(),socket2.getOutputStream())
    }
    val thread2 = thread {
        this.transfer(socket2.getInputStream(),socket1.getOutputStream())
    }
    private fun transfer(inputStream: InputStream, outputStream: OutputStream) {
        this.streams.add(inputStream)
        this.streams.add(outputStream)
        val buffer = ByteArray(60000)
        while (true) {
            try {
                val len = inputStream.read(buffer)
                if(len == -1) { break }
                outputStream.write(buffer,0,len)
                outputStream.flush()
            } catch (_: Exception) { break }
        }
        this.close()
    }
    fun closeProcess(process: ()->Unit) { this.closeProcesses.add(process) }
    override fun close() {
        if(this.isClosed) { return }
        this.isClosed = true
        this.streams.forEach(Closeable::close)
        if(socket1.isClosed) { this.socket1.close() }
        if(socket2.isClosed) { this.socket2.close() }
        this.closeProcesses.forEach { it.invoke() }
    }
}