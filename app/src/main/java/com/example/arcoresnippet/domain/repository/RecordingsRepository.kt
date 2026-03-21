package com.example.arcoresnippet.domain.repository

import android.content.Context
import com.example.arcoresnippet.domain.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class RecordingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }

    fun createNewRecording() : Recording {
        val timestamp = System.currentTimeMillis()
        val file = File(recordingsDir, "rec_$timestamp.mp4")

        return Recording(
            fileName = file.absolutePath
        )
    }

    fun listRecordings(): List<Recording> {
        val a = recordingsDir.listFiles()
        val b = a?.filter { it.extension == "mp4" }

        return b?.map { file ->
                Recording(file.absolutePath)
            }
            ?: emptyList()
    }

    fun deleteAllRecordings(): Boolean {
        return recordingsDir.listFiles()?.forEach { it.delete() } != null
    }

    fun deleteRecording(path: String): Boolean {
        return File(path).delete()
    }
}