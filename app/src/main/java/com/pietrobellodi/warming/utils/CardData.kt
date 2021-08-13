package com.pietrobellodi.warming.utils

import com.github.mikephil.charting.data.LineDataSet

/**
 * This class is used as an information container
 * for RecyclerView's cards that contain a title
 * and a chart
 *
 * @param title the title of the card
 * @param chartDataset the dataset that contains graph's data
 * @param chartLabels the dataset that contains graph's labels
 */
data class CardData(var title: String, var chartDataset: LineDataSet, var chartLabels: List<String>)
