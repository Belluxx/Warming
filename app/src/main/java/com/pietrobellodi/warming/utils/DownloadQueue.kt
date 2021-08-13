package com.pietrobellodi.warming.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

/**
 * This class manages multiple downloads sequentially
 * and thanks to the QueueWatcher allows to manage
 * the downloaded data step by step
 *
 * @param delay an optional parameter that allows to
 * decide how many milliseconds to wait before
 * downloading the next element
 */
class DownloadQueue(private val delay: Long = 0L) {

    /**
     * This class manages a single download
     *
     * @param url the url from which to gather data
     * @param id the unique identifier of the download
     */
    class Downloader(private val url: String, private val id: Int) {

        /**
         * Interface to manage the completion of a download
         */
        interface DownloadListener {
            fun onDownloadCompleted(data: String, id: Int)
            fun onDownloadError(e: Exception, id: Int)
        }

        lateinit var listener: DownloadListener

        fun download() {
            GlobalScope.launch(Dispatchers.Main) {
                establishConnection()
            }
        }

        private suspend fun establishConnection() {
            withContext(Dispatchers.IO) {
                try {
                    val data = URL(url).readText()
                    listener.onDownloadCompleted(data, id)
                } catch (e: IOException) {
                    listener.onDownloadError(e, id)
                }
            }
        }
    }

    /**
     * Interface to manage the completion of a step (download)
     * or an error or the completion of all downloads
     */
    interface QueueWatcher {
        fun onStepCompleted(data: String, id: Int)
        fun onStepError(id: Int)

        fun onAllCompleted()
    }

    private val queue = arrayListOf<Downloader>()
    lateinit var watcher: QueueWatcher

    /**
     * Adds an url to the queue
     *
     * @param url the url to download
     */
    fun add(url: String) {
        val downloader = Downloader(url, queue.size)
        downloader.listener = object : Downloader.DownloadListener {
            override fun onDownloadCompleted(data: String, id: Int) {
                watcher.onStepCompleted(data, id)
                goNext()
            }

            override fun onDownloadError(e: Exception, id: Int) {
                logError("Download error: $e")
                watcher.onStepError(id)
                goNext()
            }
        }
        queue.add(downloader)
    }

    /**
     * Proceeds to the next download in the queue
     */
    private fun goNext() {
        if (delay == 0L) {
            if (queue.isNotEmpty()) queue.removeFirst().download()
            else watcher.onAllCompleted()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                if (queue.isNotEmpty()) queue.removeFirst().download()
                else watcher.onAllCompleted()
            }, delay)
        }
    }

    /**
     * Starts the queue and downloads all urls
     */
    fun start() {
        goNext()
    }

    private fun logError(s: String) {
        Log.e("Error", ">>> ERROR: $s")
    }

}