package com.example.dailydrift.uii.habits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dailydrift.R
import com.example.dailydrift.data.model.Habit
import com.example.dailydrift.databinding.ItemHabitBinding

class HabitsAdapter(
    private val habits: MutableList<Habit>,
    private val onHabitClick: (Habit) -> Unit,
    private val onHabitLongClick: (Habit) -> Unit,
    private val onHabitEdit: (Habit) -> Unit,
    private val onHabitDelete: (Habit) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    // Habit color mapping
    private val habitColors = mapOf(
        "water" to R.color.habit_water,
        "run" to R.color.habit_run,
        "plants" to R.color.habit_plants,
        "meditate" to R.color.habit_meditate,
        "done" to R.color.habit_done
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount() = habits.size

    fun replaceItemById(updated: Habit): Int {
        val idx = habits.indexOfFirst { it.id == updated.id }
        if (idx != -1) habits[idx] = updated
        return idx
    }

    inner class HabitViewHolder(private val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit) = with(binding) {
            tvHabitName.text = habit.name

            // Set card background color based on habit name
            val colorRes = getHabitColor(habit.name)
            val color = ContextCompat.getColor(root.context, colorRes)
            root.setCardBackgroundColor(color)

            // Set action icon based on completion status
            if (habit.isCompleted) {
                ivAction.setImageResource(R.drawable.ic_delete)
                ivAction.contentDescription = "Delete habit"
            } else {
                ivAction.setImageResource(R.drawable.ic_check)
                ivAction.contentDescription = "Complete habit"
            }

            // Set click listeners
            ivAction.setOnClickListener {
                if (habit.isCompleted) {
                    onHabitDelete(habit)
                } else {
                    onHabitClick(habit)
                }
            }

            ivEdit.setOnClickListener {
                onHabitEdit(habit)
            }

            root.setOnLongClickListener {
                onHabitLongClick(habit)
                true
            }
        }

        private fun getHabitColor(habitName: String): Int {
            val lowerName = habitName.lowercase()
            return when {
                lowerName.contains("water") -> R.color.habit_water
                lowerName.contains("run") || lowerName.contains("exercise") -> R.color.habit_run
                lowerName.contains("plant") -> R.color.habit_plants
                lowerName.contains("meditate") -> R.color.habit_meditate
                else -> R.color.habit_water // default color
            }
        }
    }
}
