package com.pietrobellodi.warming.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

/**
 * This class manages a single download
 *
 * @param url the url from which to gather data
 * @param listener the listener that notifies about download
 * completion or errors
 */
class FileDownloader(var url: String, private var listener: DownloadListener) {
    /**
     * Interface to manage the completion or errors of a download
     */
    interface DownloadListener {
        fun onDownloadCompleted(data: String)
        fun onDownloadError(e: Exception)
    }

    /**
     * Starts the download
     */
    fun download() {
        GlobalScope.launch(Dispatchers.Main) {
            establishConnection()
        }
    }

    private suspend fun establishConnection() {
        withContext(Dispatchers.IO) {
            try {
                val data = URL(url).readText()
                listener.onDownloadCompleted(data)
            } catch (e: IOException) {
                listener.onDownloadError(e)
            }
        }
    }
}