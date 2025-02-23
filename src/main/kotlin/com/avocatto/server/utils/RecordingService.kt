package com.avocatto.server.utils

import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

object RecordingService {
    private val audioFormat = AudioFormat(16000.0f, 16, 1, true, false)

    fun recordAndStreamVoiceObservable(): io.reactivex.rxjava3.core.Observable<String> {
        return io.reactivex.rxjava3.core.Observable.create { emitter ->
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            val microphone = AudioSystem.getLine(info) as TargetDataLine
            try {
                microphone.open(audioFormat)
                microphone.start()
                emitter.setCancellable {
                    microphone.stop()
                    microphone.close()
                }
                val bufferSize = 4096
                val buffer = ByteArray(bufferSize)
                while (!emitter.isDisposed) {
                    val bytesRead = microphone.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val chunk = buffer.copyOf(bytesRead)
                        val encodedAudio = Base64.getEncoder().encodeToString(chunk)
                        emitter.onNext(encodedAudio)
                    }
                }
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }


}