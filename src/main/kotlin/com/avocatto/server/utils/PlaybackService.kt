package com.avocatto.server.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class PlaybackService {

    private val audioFormat = AudioFormat(16000.0f, 16, 1, true, false)
    val pipedOutput = PipedOutputStream()
    val pipedInput = PipedInputStream(pipedOutput)


     suspend fun playAudioStream() {
        withContext(Dispatchers.IO) {
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
            val sourceDataLine = AudioSystem.getLine(info) as SourceDataLine
            sourceDataLine.open(audioFormat)
            sourceDataLine.start()

            val buffer = ByteArray(4096)
            try {
                while (true) {
                    val bytesRead = pipedInput.read(buffer)
                    if (bytesRead == -1) break
                    sourceDataLine.write(buffer, 0, bytesRead)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sourceDataLine.drain()
                sourceDataLine.stop()
                sourceDataLine.close()
            }
        }
    }
}