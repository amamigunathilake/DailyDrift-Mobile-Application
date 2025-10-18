package com.example.dailydrift.uii.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailydrift.R
import com.example.dailydrift.data.model.Habit
import com.example.dailydrift.data.pref.Keys
import com.example.dailydrift.data.pref.Prefs
import com.example.dailydrift.data.repo.HabitRepo
import com.example.dailydrift.data.repo.MoodRepo
import com.example.dailydrift.databinding.FragmentHomeBinding
import com.example.dailydrift.uii.habits.AddHabitDialogFragment
import com.example.dailydrift.uii.habits.HabitsAdapter
import com.example.dailydrift.uii.mood.AddMoodDialogFragment
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitRepo: HabitRepo
    private lateinit var moodRepo: MoodRepo

    private val todayHabits = mutableListOf<Habit>()
    private lateinit var todayAdapter: HabitsAdapter

    private val dateFormatter by lazy {
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var midnightRunnablePosted = false

    private val dateTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateDate()
            habitRepo.checkAndResetIfNewDay()
            loadToday()
            updateTodayProgress()
            scheduleMidnightTick() // reschedule in case TZ changed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        habitRepo = HabitRepo(requireContext())
        moodRepo = MoodRepo(requireContext())

        val prefs = Prefs(requireContext())
        android.util.Log.d("Backup", "MOODS=" + prefs.getString(Keys.MOOD_ENTRIES_KEY, "[]"))
        android.util.Log.d("Backup", "HABITS=" + prefs.getString(Keys.HABITS_KEY, "[]"))

        setupRecycler()
        setupClicks()

        if (habitRepo.getLastResetDate().isEmpty()) {
            habitRepo.setLastResetDate(habitRepo.getCurrentDateString())
        }
        habitRepo.checkAndResetIfNewDay()

        updateDate()
        loadToday()
        updateTodayProgress()
        updateMoodChart()
    }


    private fun updateDate() {
        // Date display removed from new layout
        // val currentDate = LocalDate.now(ZoneId.systemDefault())
        // binding.tvDate.text = currentDate.format(dateFormatter)
    }

    private fun setupRecycler() {
        todayAdapter = HabitsAdapter(
            todayHabits,
            onHabitClick = { toggle(it) },
            onHabitLongClick = { showOptions(it) },
            onHabitEdit = { edit(it) },
            onHabitDelete = { delete(it) }
        )
        binding.rvToday.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        binding.btnAddHabit.setOnClickListener {
            AddHabitDialogFragment(
                existingHabit = null,
                onSave = { habit ->
                    habitRepo.addHabit(habit)
                    loadToday()
                    updateTodayProgress()
                },
                onDelete = null
            ).show(parentFragmentManager, "AddHabitFromHome")
        }

        binding.btnManageHabits.setOnClickListener {
            requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_habits
        }

        binding.btnShareHowYouFeel.setOnClickListener {
            AddMoodDialogFragment { entry ->
                moodRepo.addMoodEntry(entry)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.mood_saved_toast),
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity()
                    .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                    .selectedItemId = R.id.nav_mood
            }.show(parentFragmentManager, "AddMoodFromHome")
        }
    }

    private fun loadToday() {
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val todays = habitRepo.getHabits().filter { h ->
            h.createdAt in startOfToday until startOfTomorrow
        }

        todayHabits.clear()
        todayHabits.addAll(todays.sortedBy { it.name.lowercase() })
        todayAdapter.notifyDataSetChanged()

        binding.emptyState.visibility = if (todayHabits.isEmpty()) View.VISIBLE else View.GONE
        binding.rvToday.visibility = if (todayHabits.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateTodayProgress() {
        val total = todayHabits.size
        val completed = todayHabits.count { it.isCompleted }
        val progress = if (total > 0) (completed * 100) / total else 0

        // Update circular progress
        binding.progressBar.progress = progress
        binding.tvPercent.text = "${progress}%"
        
        // Update statistics
        binding.tvCompleted.text = completed.toString()
        binding.tvNotDone.text = (total - completed).toString()
        
        // Calculate best streak (simplified - you can enhance this)
        val bestStreak = calculateBestStreak()
        binding.tvBestStreak.text = bestStreak.toString()
        
        // Update best habit this week
        updateBestHabitThisWeek()
    }
    
    private fun calculateBestStreak(): Int {
        // Simplified calculation - you can enhance this with actual streak tracking
        val completed = todayHabits.count { it.isCompleted }
        return if (completed > 0) completed else 0
    }
    
    private fun updateBestHabitThisWeek() {
        // Find the habit with highest completion rate
        val bestHabit = todayHabits.maxByOrNull { habit ->
            if (habit.targetCount > 0) (habit.completedCount * 100) / habit.targetCount else 0
        }
        
        if (bestHabit != null) {
            val completionRate = if (bestHabit.targetCount > 0) (bestHabit.completedCount * 100) / bestHabit.targetCount else 0
            binding.tvBestHabitName.text = bestHabit.name
            binding.tvBestHabitPercent.text = "${completionRate}%"
        } else {
            binding.tvBestHabitName.text = "No habits yet"
            binding.tvBestHabitPercent.text = "0%"
        }
    }
    
    private fun updateMoodChart() {
        val moodEntries = moodRepo.getMoodEntriesForWeek()
        val today = LocalDate.now(ZoneId.systemDefault())
        
        // Get the last 7 days
        val days = mutableListOf<LocalDate>()
        for (i in 6 downTo 0) {
            days.add(today.minusDays(i.toLong()))
        }
        
        // Day labels and emojis
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayViews = listOf(
            binding.moodDay1, binding.moodDay2, binding.moodDay3, binding.moodDay4,
            binding.moodDay5, binding.moodDay6, binding.moodDay7
        )
        val emojiViews = listOf(
            binding.moodEmoji1, binding.moodEmoji2, binding.moodEmoji3, binding.moodEmoji4,
            binding.moodEmoji5, binding.moodEmoji6, binding.moodEmoji7
        )
        val barViews = listOf(
            binding.moodBar1, binding.moodBar2, binding.moodBar3, binding.moodBar4,
            binding.moodBar5, binding.moodBar6, binding.moodBar7
        )
        
        // Update each day
        for (i in 0..6) {
            val date = days[i]
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Find mood entry for this day
            val dayMood = moodEntries.find { it.createdAt in dayStart until dayEnd }
            
            // Update day label
            dayViews[i].text = dayLabels[i]
            
            // Update emoji
            if (dayMood != null) {
                emojiViews[i].text = dayMood.moodEmoji
                emojiViews[i].visibility = View.VISIBLE
                // Set bar height based on mood (simplified)
                val layoutParams = barViews[i].layoutParams
                layoutParams.height = 120 // Default height
                barViews[i].layoutParams = layoutParams
            } else {
                emojiViews[i].text = "😐" // Neutral face for no entry
                emojiViews[i].visibility = View.VISIBLE
                val layoutParams = barViews[i].layoutParams
                layoutParams.height = 60 // Smaller height for no entry
                barViews[i].layoutParams = layoutParams
            }
        }
        
        // Update stats
        if (moodEntries.isNotEmpty()) {
            // Calculate average mood (simplified - using first emoji as average)
            val averageMood = moodEntries.firstOrNull()?.moodEmoji ?: "😐"
            binding.tvAverageMood.text = averageMood
            
            // Find best mood
            val bestMood = moodEntries.maxByOrNull { it.moodEmoji }?.moodEmoji ?: "😐"
            binding.tvBestMood.text = bestMood
            
            // Total entries
            binding.tvMoodEntries.text = moodEntries.size.toString()
        } else {
            binding.tvAverageMood.text = "😐"
            binding.tvBestMood.text = "😐"
            binding.tvMoodEntries.text = "0"
        }
    }

    private fun toggle(habit: Habit) {
        val updated = habit.copy(
            isCompleted = !habit.isCompleted,
            completedCount = if (habit.isCompleted) 0 else habit.targetCount
        )
        habitRepo.updateHabit(updated)
        loadToday()
        updateTodayProgress()
        updateMoodChart()
    }

    private fun showOptions(habit: Habit) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.habit_options))
            .setItems(arrayOf(getString(R.string.edit), getString(R.string.delete))) { _, which ->
                when (which) {
                    0 -> edit(habit)
                    1 -> delete(habit)
                }
            }.show()
    }

    private fun edit(habit: Habit) {
        AddHabitDialogFragment(
            existingHabit = habit,
            onSave = { updated ->
                habitRepo.updateHabit(updated)
                loadToday()
                updateTodayProgress()
                updateMoodChart()
            },
            onDelete = { toDelete ->
                habitRepo.deleteHabit(toDelete.id)
                loadToday()
                updateTodayProgress()
                updateMoodChart()
            }
        ).show(parentFragmentManager, "EditHabitFromHome")
    }

    private fun delete(habit: Habit) {
        habitRepo.deleteHabit(habit.id)
        loadToday()
        updateTodayProgress()
        updateMoodChart()
    }


    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        requireContext().registerReceiver(dateTimeReceiver, f)
        scheduleMidnightTick()
        updateDate()
    }

    override fun onResume() {
        super.onResume()
        habitRepo.checkAndResetIfNewDay()
        updateDate()
        loadToday()
        updateTodayProgress()
        updateMoodChart()
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(dateTimeReceiver)
        } catch (_: IllegalArgumentException) { }
        cancelMidnightTick()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun scheduleMidnightTick() {
        if (midnightRunnablePosted) return
        val zone = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()
        val nextMidnightMs = LocalDate.now(zone)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val delay = max(1L, nextMidnightMs - nowMs)

        mainHandler.postDelayed({
            midnightRunnablePosted = false
            updateDate()
            habitRepo.checkAndResetIfNewDay()
            loadToday()
            updateTodayProgress()
            scheduleMidnightTick()
        }, delay)
        midnightRunnablePosted = true
    }

    private fun cancelMidnightTick() {
        mainHandler.removeCallbacksAndMessages(null)
        midnightRunnablePosted = false
    }
}
