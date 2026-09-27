package com.pocketmind.engine

import android.content.Context
import com.pocketmind.core.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val fraction: Float = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
)

class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val modelsDir: File get() {
        val dir = File(context.filesDir, "models")
        dir.mkdirs()
        return dir
    }

    fun getLocalPath(modelId: String): String = File(modelsDir, "$modelId.gguf").absolutePath

    fun isDownloaded(modelId: String): Boolean = File(modelsDir, "$modelId.gguf").exists()

    fun download(model: ModelInfo): Flow<DownloadProgress> = flow {
        val outFile = File(modelsDir, "${model.id}.gguf")
        val tempFile = File(modelsDir, "${model.id}.tmp")

        val request = Request.Builder().url(model.downloadUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val total = body.contentLength()
        var downloaded = 0L

        body.byteStream().use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    emit(DownloadProgress(model.id, downloaded, total))
                }
            }
        }

        tempFile.renameTo(outFile)
        emit(DownloadProgress(model.id, downloaded, downloaded, 1.0f))
    }.flowOn(Dispatchers.IO)

    fun deleteModel(modelId: String): Boolean {
        return File(modelsDir, "$modelId.gguf").delete()
    }
}
