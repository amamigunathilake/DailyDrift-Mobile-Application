package com.example.dailydrift.uii.habits

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailydrift.R
import com.example.dailydrift.data.model.Habit
import com.example.dailydrift.data.repo.HabitRepo
import com.example.dailydrift.databinding.FragmentHabitsBinding
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDate
import java.time.ZoneId

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitRepo: HabitRepo

    private val todayList = mutableListOf<Habit>()
    private val pastList = mutableListOf<Habit>()
    private lateinit var todayAdapter: HabitsAdapter
    private lateinit var pastAdapter: HabitsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        habitRepo = HabitRepo(requireContext())

        if (habitRepo.getLastResetDate().isEmpty()) {
            habitRepo.setLastResetDate(habitRepo.getCurrentDateString())
        }
        habitRepo.checkAndResetIfNewDay()

        setupRecyclerViews()
        setupClickListeners()
        setupDateDisplay()
        setupCalendar()
        loadHabits()
    }

    private fun setupRecyclerViews() {
        // Today
        todayAdapter = HabitsAdapter(
            todayList,
            onHabitClick = { toggleHabitCompletion(it) },
            onHabitLongClick = { showHabitOptions(it) },
            onHabitEdit = { showEditHabitDialog(it) },
            onHabitDelete = { showDeleteConfirmation(it) }
        )
        binding.rvToday.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayAdapter
        }

        // Past
        pastAdapter = HabitsAdapter(
            pastList,
            onHabitClick = { toggleHabitCompletion(it) },
            onHabitLongClick = { showHabitOptions(it) },
            onHabitEdit = { showEditHabitDialog(it) },
            onHabitDelete = { showDeleteConfirmation(it) }
        )
        binding.rvPast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos in todayList.indices) {
                    val removed = todayList[pos]
                    habitRepo.deleteHabit(removed.id)
                    // Post to avoid touching the adapter during layout pass
                    binding.rvToday.post { loadHabits() }
                    Snackbar.make(binding.root, getString(R.string.habit_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo)) {
                            habitRepo.restoreHabit(removed, null)
                            binding.rvToday.post { loadHabits() }
                        }.show()
                }
            }
        }).attachToRecyclerView(binding.rvToday)

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos in pastList.indices) {
                    val removed = pastList[pos]
                    habitRepo.deleteHabit(removed.id)
                    binding.rvPast.post { loadHabits() }
                    Snackbar.make(binding.root, getString(R.string.habit_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo)) {
                            habitRepo.restoreHabit(removed, null)
                            binding.rvPast.post { loadHabits() }
                        }.show()
                }
            }
        }).attachToRecyclerView(binding.rvPast)
    }

    private fun setupClickListeners() {
        binding.btnAddHabit.setOnClickListener { showAddHabitDialog() }
        binding.btnEmptyAddHabit.setOnClickListener { showAddHabitDialog() }
    }

    private fun setupDateDisplay() {
        val currentDate = LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
        binding.tvDate.text = currentDate.format(formatter)
    }

    private fun setupCalendar() {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L) // Get Monday of current week
        
        val calendarDays = listOf(
            binding.btnMon,
            binding.btnTue,
            binding.btnWed,
            binding.btnThu,
            binding.btnFri,
            binding.btnSat,
            binding.btnSun
        )
        
        val dayAbbreviations = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayView = calendarDays[i]
            
            // Set the text with day abbreviation and date
            dayView.text = "${dayAbbreviations[i]}\n${date.dayOfMonth}"
            
            // Highlight today
            if (date == today) {
                dayView.setBackgroundResource(R.drawable.calendar_today_bg)
                dayView.setTextColor(resources.getColor(R.color.text_on_primary, null))
            } else {
                dayView.setBackgroundResource(R.drawable.calendar_other_bg)
                dayView.setTextColor(resources.getColor(R.color.calendar_text, null))
            }
            
            // Add click listener for day selection
            dayView.setOnClickListener {
                onDaySelected(date)
            }
        }
    }
    
    private fun onDaySelected(selectedDate: LocalDate) {
        // Update the date display
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
        binding.tvDate.text = selectedDate.format(formatter)
        
        // Reload habits for the selected date
        loadHabitsForDate(selectedDate)
        
        // Update calendar highlighting
        updateCalendarHighlighting(selectedDate)
    }
    
    private fun updateCalendarHighlighting(selectedDate: LocalDate) {
        val today = LocalDate.now()
        val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value - 1L)
        
        val calendarDays = listOf(
            binding.btnMon,
            binding.btnTue,
            binding.btnWed,
            binding.btnThu,
            binding.btnFri,
            binding.btnSat,
            binding.btnSun
        )
        
        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayView = calendarDays[i]
            
            if (date == selectedDate) {
                dayView.setBackgroundResource(R.drawable.calendar_today_bg)
                dayView.setTextColor(resources.getColor(R.color.text_on_primary, null))
            } else {
                dayView.setBackgroundResource(R.drawable.calendar_other_bg)
                dayView.setTextColor(resources.getColor(R.color.calendar_text, null))
            }
        }
    }
    
    private fun loadHabitsForDate(date: LocalDate) {
        val all = habitRepo.getHabits()
        
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val newToday = mutableListOf<Habit>()
        val newPast = mutableListOf<Habit>()
        
        all.forEach { h ->
            val t = h.createdAt
            if (t in dayStart until dayEnd) {
                if (h.isCompleted) {
                    newPast.add(h) // Completed habits go to "Done" section
                } else {
                    newToday.add(h) // Incomplete habits go to "To Do" section
                }
            }
        }
        
        newToday.sortBy { it.name.lowercase() }
        newPast.sortByDescending { it.createdAt }
        
        todayList.clear(); todayList.addAll(newToday)
        pastList.clear(); pastList.addAll(newPast)
        
        binding.rvToday.post { todayAdapter.notifyDataSetChanged() }
        binding.rvPast.post { pastAdapter.notifyDataSetChanged() }
        
        val showToday = todayList.isNotEmpty()
        val showPast = pastList.isNotEmpty()
        binding.rvToday.visibility = if (showToday) View.VISIBLE else View.GONE
        binding.rvPast.visibility = if (showPast) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (!showToday && !showPast) View.VISIBLE else View.GONE
    }

    private fun loadHabits() {
        loadHabitsForDate(LocalDate.now())
    }

    private fun toggleHabitCompletion(habit: Habit) {
        val updated = habit.copy(
            completedCount = if (habit.isCompleted) 0 else habit.targetCount,
            isCompleted = !habit.isCompleted
        )
        habitRepo.updateHabit(updated)

        val idxToday = todayList.indexOfFirst { it.id == updated.id }
        if (idxToday != -1) {
            todayList[idxToday] = updated
            binding.rvToday.post { todayAdapter.notifyItemChanged(idxToday) }
            return
        }
        val idxPast = pastList.indexOfFirst { it.id == updated.id }
        if (idxPast != -1) {
            pastList[idxPast] = updated
            binding.rvPast.post { pastAdapter.notifyItemChanged(idxPast) }
            return
        }

        binding.rvToday.post { loadHabits() }
    }

    private fun showAddHabitDialog() {
        AddHabitDialogFragment(
            existingHabit = null,
            onSave = { newHabit ->
                habitRepo.addHabit(newHabit)
                binding.rvToday.post { loadHabits() }
            },
            onDelete = null
        ).show(parentFragmentManager, "AddHabitDialog")
    }

    private fun showHabitOptions(habit: Habit) {
        val options = arrayOf(getString(R.string.edit), getString(R.string.delete))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.habit_options))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditHabitDialog(habit)
                    1 -> showDeleteConfirmation(habit)
                }
            }
            .show()
    }

    private fun showEditHabitDialog(habit: Habit) {
        AddHabitDialogFragment(
            existingHabit = habit,
            onSave = { updatedHabit ->
                habitRepo.updateHabit(updatedHabit)
                val idxToday = todayList.indexOfFirst { it.id == updatedHabit.id }
                if (idxToday != -1) {
                    todayList[idxToday] = updatedHabit
                    binding.rvToday.post { todayAdapter.notifyItemChanged(idxToday) }
                } else {
                    val idxPast = pastList.indexOfFirst { it.id == updatedHabit.id }
                    if (idxPast != -1) {
                        pastList[idxPast] = updatedHabit
                        binding.rvPast.post { pastAdapter.notifyItemChanged(idxPast) }
                    } else {
                        binding.rvToday.post { loadHabits() }
                    }
                }
            },
            onDelete = { habitToDelete ->
                habitRepo.deleteHabit(habitToDelete.id)
                binding.rvToday.post { loadHabits() }
            }
        ).show(parentFragmentManager, "EditHabitDialog")
    }

    private fun showDeleteConfirmation(habit: Habit) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_habit))
            .setMessage(getString(R.string.delete_habit_message, habit.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                habitRepo.deleteHabit(habit.id)
                binding.rvToday.post { loadHabits() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        habitRepo.checkAndResetIfNewDay()
        binding.rvToday.post { loadHabits() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
