package com.dreamy.mobile.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Records the microphone to an AAC/MP4 file (`audio/mp4`, accepted by the transcription endpoint). */
class VoiceRecorder @Inject constructor(@param:ApplicationContext private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    /** Throws if the microphone is unavailable. */
    fun start() {
        release()
        // Not cacheDir: Android may purge the cache under storage pressure before the upload.
        // The view model deletes the file once it is transcribed or discarded.
        val dir = File(context.noBackupFilesDir, "recordings").apply { mkdirs() }
        val file = File.createTempFile("note", ".m4a", dir)
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioSamplingRate(44_100)
            next.setAudioEncodingBitRate(64_000)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
        } catch (e: Exception) {
            next.release()
            file.delete()
            throw e
        }
        recorder = next
        output = file
    }

    /** Stops recording and returns the file, or null if nothing usable was captured. */
    fun stop(): File? {
        val current = recorder ?: return null
        val file = output
        recorder = null
        output = null
        val ok = runCatching { current.stop() }.isSuccess
        current.release()
        if (!ok) file?.delete()
        return file.takeIf { ok }
    }

    fun release() {
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
        output?.delete()
        output = null
    }
}
