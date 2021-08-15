package com.pietrobellodi.warming

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.pietrobellodi.warming.utils.CardData
import com.pietrobellodi.warming.utils.CardsAdapter
import com.pietrobellodi.warming.utils.FileDownloader


class MainActivity : AppCompatActivity() {

    // Constants
    private val LINE_WIDTH = 4f
    private val ASSET_URLS_NAME = "datasets.txt"

    // Variables
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardsData: ArrayList<CardData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initArrays()
        initLayout()
        init()
    }

    // Initialization functions
    private fun initArrays() {
        cardsData = arrayListOf()
    }

    private fun initLayout() {
        recyclerView = findViewById(R.id.graphs_rv)
        recyclerView.adapter = CardsAdapter(cardsData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
    }

    private fun init() {
        if (isNetworkAvailable()) { // If the device has an internet connection
            startDatasetDownloads()
        } else {
            // If there is no internet connection, notify the user
            toast("No internet connection")
            setSubtitle("No internet connection")
        }
    }

    private fun startDatasetDownloads() {
        // Read datasets asset text file
        val datasetsInfo = arrayListOf<String>()
        application.assets.open(ASSET_URLS_NAME).bufferedReader().useLines { lines ->
            lines.forEach {
                datasetsInfo.add(it)
            }
        }

        // Download data
        downloadDataset(datasetsInfo.first().split(','), true)
        for (info in datasetsInfo.drop(1)) {
            downloadDataset(info.split(','), false)
        }
    }

    private fun downloadDataset(info: List<String>, first: Boolean = false) {
        FileDownloader(info[5], object : FileDownloader.DownloadListener {
            override fun onDownloadCompleted(data: String) {
                manageCsvData(
                    data,
                    info[0],
                    info[1].toInt(),
                    info[2].toInt(),
                    info[3].toInt(),
                    info[4],
                    first
                )
            }

            override fun onDownloadError(e: Exception) {
                toast("Cannot download ${info[0]}")
            }
        }).download()
    }

    // Functions used for extraction and visualization of the data
    private fun manageCsvData(
        data: String,
        title: String,
        entriesToDrop: Int,
        valuesColumn: Int,
        labelsColumn: Int,
        color: String,
        first: Boolean = false
    ) {
        val result = extractData(data, entriesToDrop, valuesColumn, labelsColumn, color, first)
        createCard(title, result.first, result.second)
    }

    private fun extractData(
        data: String,
        entriesToDrop: Int,
        valuesColumn: Int,
        labelsColumn: Int,
        color: String,
        first: Boolean
    ): Pair<LineDataSet, List<String>> {
        // Clean data
        var entries = data.split('\n')
        entries = entries.drop(entriesToDrop)
        if (entries.last().isBlank()) entries = entries.dropLast(1)

        // Extract needed data
        val size = entries.size
        val values = Array(size) { 0f }
        val labels = Array(size) { "NO_LABEL" }
        var i = 0
        entries.forEach {
            val rowData = trimList(it.split(','))
            values[i] = rowData[valuesColumn].toFloat()
            labels[i] = rowData[labelsColumn].substring(0..3)
            i++
        }

        // Pack data into LineDataSet
        i = 0
        val valuesDatasetArray = Array(size) { Entry(0f, 0f) }
        for (value in values) {
            valuesDatasetArray[i] = Entry(i.toFloat(), value)
            i++
        }
        val valuesDataset = LineDataSet(valuesDatasetArray.toList(), "Values")

        // Customize dataset
        with(valuesDataset) {
            this.color = Color.parseColor(color)
            if (first) this.lineWidth = LINE_WIDTH else this.lineWidth = LINE_WIDTH / 2.75f
            this.mode = LineDataSet.Mode.LINEAR

            this.setDrawValues(false)
            this.setDrawHighlightIndicators(false)
            this.setDrawCircles(false)
        }

        return Pair(valuesDataset, labels.toList())
    }

    private fun createCard(title: String, dataset: LineDataSet, labels: List<String>) {
        cardsData.add(CardData(title, dataset, labels))
        recyclerUpdate()
    }

    // Low level functions for small tasks
    private fun recyclerUpdate() {
        runOnUiThread {
            recyclerView.adapter!!.notifyItemInserted(recyclerView.adapter!!.itemCount - 1)
        }
    }

    private fun trimList(list: List<String>): List<String> {
        val trimmedList = arrayListOf<String>()
        for (string in list) {
            trimmedList.add(string.trim())
        }
        return trimmedList
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_buttons, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_credits -> {
            val intent = Intent(this, CreditsActivity::class.java)
            startActivity(intent)
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun setSubtitle(s: String) {
        runOnUiThread {
            supportActionBar?.subtitle = s
        }
    }

    private fun toast(s: String) {
        runOnUiThread {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}