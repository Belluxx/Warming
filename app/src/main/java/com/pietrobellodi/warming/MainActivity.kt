package com.pietrobellodi.warming

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
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
import com.pietrobellodi.warming.utils.DownloadQueue


class MainActivity : AppCompatActivity() {

    // URLs
    private val TEMPERATURE_DATA_URL =
        "https://data.giss.nasa.gov/gistemp/graphs/graph_data/Global_Mean_Estimates_based_on_Land_and_Ocean_Data/graph.csv"
    private val CO2_DATA_URL = "https://gml.noaa.gov/webdata/ccgg/trends/co2/co2_annmean_gl.csv"
    private val ARCTIC_ICE_HOST = "https://climate.nasa.gov"
    private val ARCTIC_ICE_EXTRACTION_URL = "$ARCTIC_ICE_HOST/vital-signs/arctic-sea-ice/"

    // Constants
    private val LINE_WIDTH = 4f

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
        val queue = DownloadQueue()
        if (isNetworkAvailable()) { // If the device has an internet connection
            // Add urls to the queue
            queue.add(TEMPERATURE_DATA_URL)
            queue.add(CO2_DATA_URL)
            queue.add(ARCTIC_ICE_EXTRACTION_URL)

            // Setup queue's watcher and its events
            queue.watcher = object : DownloadQueue.QueueWatcher {
                override fun onStepCompleted(data: String, id: Int) {
                    when (id) {
                        0 -> {
                            manageCsvData(
                                data,
                                "Average temperature anomaly (°C)",
                                3,
                                2,
                                0,
                                "#FF0000",
                                true
                            )
                        }
                        1 -> {
                            manageCsvData(
                                data,
                                "CO2 concentration (ppm)",
                                56,
                                1,
                                0,
                                "#333333"
                            )
                        }
                        2 -> {
                            extractArcticIceURL(data)
                        }
                    }
                }

                override fun onStepError(id: Int) {
                    when (id) {
                        0 -> {
                            toast("Cannot download average temperature anomaly")
                        }
                        1 -> {
                            toast("Cannot download CO2 concentration")
                        }
                        2 -> {
                            toast("Cannot download arctic ice area")
                        }
                    }
                }

                override fun onAllCompleted() {
                    setSubtitle("All data downloaded")
                }
            }

            // Start the queue
            setSubtitle("Downloading data...")
            queue.start()
        } else {
            // If there is no internet connection, notify the user
            toast("No internet connection")
            setSubtitle("No internet connection")
        }
    }

    // Functions used for extraction and visualization of the data
    private fun extractArcticIceURL(page: String) {
        // Extract the CSV url from NASA's website
        val pattern = "/system/internal_resources/details/original/.*\\.csv".toRegex()
        val extractedUrl: String
        if (page.contains(pattern)) {
            extractedUrl = ARCTIC_ICE_HOST.plus(pattern.find(page)!!.value)
        } else {
            logError("Cannot extract arctic ice data")
            return
        }

        // Download arctic ice data
        val queue = DownloadQueue()
        queue.add(extractedUrl)
        queue.watcher = object : DownloadQueue.QueueWatcher {
            override fun onStepCompleted(data: String, id: Int) {
                manageCsvData(data, "Arctic ice area (million km2)", 1, 5, 0, "#0000FF")
            }

            override fun onStepError(id: Int) {
                toast("Cannot download arctic ice area")
            }

            override fun onAllCompleted() {}
        }
        queue.start()
    }

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
            labels[i] = rowData[labelsColumn]
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

    private fun logInfo(s: String) {
        Log.d("Info", ">>> INFO: $s")
    }

    private fun logError(s: String) {
        Log.e("Error", ">>> ERROR: $s")
    }
}