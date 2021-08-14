package com.pietrobellodi.warming.utils

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.pietrobellodi.warming.R

/**
 * This class is the adapter for the RecyclerView that
 * shows the cards with graphs and titles
 *
 * @param cardData a list of all the data needed to
 * create the cards
 */
class CardsAdapter(private val cardData: ArrayList<CardData>) :
    RecyclerView.Adapter<CardsAdapter.CardHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        val cardView =
            LayoutInflater.from(parent.context).inflate(R.layout.graph_card, parent, false)
        return CardHolder(cardView)
    }

    /**
     * Called when the RecyclerView needs to manage a new card.
     * Specifically, it assigns title string to the title TextView
     * and the dataset to the chart
     *
     * @param holder object that holds title and chart widgets
     * @param position index of the added card
     */
    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        val data = cardData[position]

        with(holder.titleTv) {
            text = data.title
        }

        with(holder.chart) {
            legend.isEnabled = false
            description.isEnabled = false
            isScaleYEnabled = false

            axisRight.isEnabled = false
            axisLeft.textColor = Color.parseColor("#000000")
            axisLeft.setDrawGridLines(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = IndexAxisValueFormatter(data.chartLabels)
            xAxis.textColor = Color.parseColor("#000000")
        }

        holder.chart.data = LineData(data.chartDataset)
        holder.chart.invalidate()
        holder.chart.setVisibleXRangeMinimum(5f)

        logInfo("Chart <${data.title}> loaded!")
    }

    override fun getItemCount(): Int {
        return cardData.size
    }

    class CardHolder(cardView: View) : RecyclerView.ViewHolder(cardView) {
        val titleTv: TextView = cardView.findViewById(R.id.card_title)
        val chart: LineChart = cardView.findViewById(R.id.card_chart)
    }

    private fun logInfo(s: String) {
        Log.d("Info", ">>> INFO: $s")
    }

}