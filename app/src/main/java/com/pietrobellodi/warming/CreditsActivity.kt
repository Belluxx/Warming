package com.pietrobellodi.warming

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CreditsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)

        findViewById<Button>(R.id.flaticon_btn).setOnClickListener {
            openURL("https://www.flaticon.com/authors/good-ware")
        }

        findViewById<Button>(R.id.goddard_btn).setOnClickListener {
            openURL("https://data.giss.nasa.gov/gistemp/graphs/")
        }

        findViewById<Button>(R.id.noaa_btn).setOnClickListener {
            openURL("https://gml.noaa.gov/ccgg/trends/global.html")
        }

        findViewById<Button>(R.id.nsidc_btn).setOnClickListener {
            openURL("https://climate.nasa.gov/vital-signs/arctic-sea-ice/")
        }

        findViewById<Button>(R.id.mpandroidchart_btn).setOnClickListener {
            openURL("https://github.com/PhilJay/MPAndroidChart")
        }

        findViewById<Button>(R.id.datahub_btn).setOnClickListener {
            openURL("https://datahub.io/collections/climate-change")
        }
    }

    private fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}