package com.example.dailydrift.uii.mood


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailydrift.data.model.MoodEntry
import com.example.dailydrift.databinding.ItemMoodBinding
import java.text.SimpleDateFormat
import java.util.*

class MoodAdapter(
    private val moodEntries: List<MoodEntry>,
    private val onMoodLongClick: (MoodEntry) -> Unit
) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val binding = ItemMoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        holder.bind(moodEntries[position])
    }

    override fun getItemCount() = moodEntries.size

    inner class MoodViewHolder(private val binding: ItemMoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: MoodEntry) {
            binding.apply {
                tvMoodEmoji.text = entry.moodEmoji
                tvMoodName.text = entry.moodName
                tvMoodNote.text = entry.note.ifEmpty { "No note" }

                // Format date and time
                val date = Date(entry.createdAt)
                val dateText = dateFormat.format(date)
                val timeText = timeFormat.format(date)
                
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - entry.createdAt
                val timeDiffMinutes = timeDiff / (1000 * 60)
                
                tvMoodDate.text = if (timeDiffMinutes < 1) {
                    "Just now"
                } else if (timeDiffMinutes < 60) {
                    "${timeDiffMinutes}m ago"
                } else {
                    "$dateText • $timeText"
                }

                root.setOnLongClickListener {
                    onMoodLongClick(entry)
                    true
                }
            }
        }
    }
}