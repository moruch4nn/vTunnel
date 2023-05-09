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
        try {
            this.streams.add(inputStream)
            this.streams.add(outputStream)
            val buffer = ByteArray(60000)
            while (true) {
                val len = inputStream.read(buffer)
                if(len == -1) { break }
                outputStream.write(buffer,0,len)
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            this.close()
        }
    }
    fun closeProcess(process: ()->Unit) { this.closeProcesses.add(process) }
    override fun close() {
        // すでにPacketTransferが閉じている場合はreturn
        if(this.isClosed) { return }
        // PacketTransferを閉じている状態にする
        this.isClosed = true
        // すべてのStreamを閉じる(InputStream,OutputStreamなど)
        this.streams.forEach(Closeable::close)
        // socket1が閉じていない場合は閉じる閉じる
        if(!this.socket1.isClosed) { this.socket1.close() }
        // socket2が閉じていない場合は閉じる
        if(!this.socket2.isClosed) { this.socket2.close() }
        // close時に実行するすべての処理を実行
        this.closeProcesses.forEach { it.invoke() }
    }
}