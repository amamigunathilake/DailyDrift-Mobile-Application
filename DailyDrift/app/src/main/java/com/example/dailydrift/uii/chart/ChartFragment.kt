package com.example.dailydrift.uii.chart

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.dailydrift.R
import com.example.dailydrift.data.model.MoodEntry
import com.example.dailydrift.data.repo.MoodRepo
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class ChartFragment : Fragment() {

    private lateinit var moodRepo: MoodRepo
    private lateinit var lineChart: com.github.mikephil.charting.charts.LineChart
    private lateinit var tvChartPlaceholder: View
    private lateinit var tvAverageMood: android.widget.TextView
    private lateinit var tvTotalEntries: android.widget.TextView
    private lateinit var tvCommonMood: android.widget.TextView

    private val moodScores = mapOf(
        "Excited" to 7f,
        "Happy" to 6f,
        "Amazed" to 5f,
        "Loved" to 4f,
        "Peaceful" to 3f,
        "Neutral" to 2f,
        "Tired" to 1f,
        "Sad" to 0f,
        "Anxious" to -1f,
        "Angry" to -2f
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moodRepo = MoodRepo(requireContext())

        lineChart = view.findViewById(R.id.lineChart)
        tvChartPlaceholder = view.findViewById(R.id.tvChartPlaceholder)
        tvAverageMood = view.findViewById(R.id.tvAverageMood)
        tvTotalEntries = view.findViewById(R.id.tvTotalEntries)
        tvCommonMood = view.findViewById(R.id.tvCommonMood)

        setupChart()
        loadMoodData()
    }

    private fun setupChart() {
        // Configure chart appearance
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setNoDataText("No mood data available")
        lineChart.setNoDataTextColor(Color.GRAY)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.gridColor = Color.LTGRAY
        xAxis.gridLineWidth = 1f
        xAxis.textColor = Color.DKGRAY
        xAxis.textSize = 12f
        xAxis.setLabelCount(7, true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val date = Date(value.toLong())
                val format = SimpleDateFormat("MMM dd", Locale.getDefault())
                return format.format(date)
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.gridLineWidth = 1f
        leftAxis.textColor = Color.DKGRAY
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = -2f
        leftAxis.axisMaximum = 7f
        leftAxis.setLabelCount(5, true)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    7 -> "Excited"
                    6 -> "Happy"
                    5 -> "Amazed"
                    4 -> "Loved"
                    3 -> "Peaceful"
                    2 -> "Neutral"
                    1 -> "Tired"
                    0 -> "Sad"
                    -1 -> "Anxious"
                    -2 -> "Angry"
                    else -> ""
                }
            }
        }

        lineChart.axisRight.isEnabled = false
    }

    private fun loadMoodData() {
        val moodEntries = moodRepo.getMoodEntriesForWeek()

        if (moodEntries.isEmpty()) {
            showPlaceholder()
            return
        }

        showChart()
        setupChartData(moodEntries)
        updateStats(moodEntries)
    }

    private fun showPlaceholder() {
        lineChart.visibility = View.GONE
        tvChartPlaceholder.visibility = View.VISIBLE
    }

    private fun showChart() {
        lineChart.visibility = View.VISIBLE
        tvChartPlaceholder.visibility = View.GONE
    }

    private fun setupChartData(moodEntries: List<MoodEntry>) {
        val entries = mutableListOf<Entry>()

        val sortedEntries = moodEntries.sortedBy { it.createdAt }

        sortedEntries.forEach { moodEntry ->
            val score = moodScores[moodEntry.moodName] ?: 2f
            val entry = Entry(moodEntry.createdAt.toFloat(), score)
            entries.add(entry)
        }

        val dataSet = LineDataSet(entries, "Mood Score")
        dataSet.color = Color.parseColor("#8B5CF6") // Purple color
        dataSet.setCircleColor(Color.parseColor("#8B5CF6"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 6f
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#8B5CF6")
        dataSet.fillAlpha = 50

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        if (moodEntries.isNotEmpty()) {
            val sortedEntries = moodEntries.sortedBy { it.createdAt }
            val minTime = sortedEntries.first().createdAt.toFloat()
            val maxTime = sortedEntries.last().createdAt.toFloat()

            val timeRange = maxTime - minTime
            val padding = timeRange * 0.1f // 10% padding

            lineChart.xAxis.axisMinimum = minTime - padding
            lineChart.xAxis.axisMaximum = maxTime + padding
        }

        lineChart.animateXY(1000, 1000)
        lineChart.invalidate()
    }

    private fun updateStats(moodEntries: List<MoodEntry>) {
        tvTotalEntries.text = moodEntries.size.toString()

        val scores = moodEntries.mapNotNull { moodScores[it.moodName]?.toDouble() }
        val average = if (scores.isNotEmpty()) scores.average() else 2.0 // Neutral fallback
        val nearestMood = moodScores.minBy { kotlin.math.abs(it.value - average.toFloat()) }.key
        tvAverageMood.text = getMoodEmoji(nearestMood)

        val moodCounts = moodEntries.groupingBy { it.moodName }.eachCount()
        val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Neutral"
        tvCommonMood.text = getMoodEmoji(mostCommonMood)
    }


    private fun getMoodEmoji(moodName: String): String {
        return when (moodName) {
            "Excited" -> "🤩"
            "Happy" -> "😊"
            "Amazed" -> "😮"
            "Loved" -> "🥰"
            "Peaceful" -> "😌"
            "Neutral" -> "😐"
            "Tired" -> "😴"
            "Sad" -> "😔"
            "Anxious" -> "😰"
            "Angry" -> "😠"
            else -> "😐"
        }
    }
}