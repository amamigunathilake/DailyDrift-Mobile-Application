package com.example.dailydrift.uii.hydration

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailydrift.R
import com.example.dailydrift.data.model.WaterEntry
import com.example.dailydrift.data.pref.Prefs
import com.example.dailydrift.data.repo.HydrationRepo
import com.example.dailydrift.databinding.FragmentHydrationBinding
import com.example.dailydrift.receivers.HydrationReminderReceiver
import java.util.Calendar

class HydrationFragment : Fragment() {

    private var _binding: FragmentHydrationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefs: Prefs
    private lateinit var hydrationRepo: HydrationRepo
    private lateinit var historyAdapter: WaterEntryAdapter

    private val reminderIntervals = arrayOf(
        "30 minutes" to 30,
        "1 hour" to 60,
        "2 hours" to 120,
        "3 hours" to 180,
        "4 hours" to 240
    )

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scheduleHydrationReminder()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notifications permission denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHydrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        hydrationRepo = HydrationRepo(requireContext())

        setupClickListeners()
        setupHistoryList()
        loadCurrentSettings()
        refreshHydrationUI()
        updateCalendar()
    }

    private fun setupHistoryList() {
        historyAdapter = WaterEntryAdapter(mutableListOf()) { entry ->
            confirmDelete(entry)
        }
        binding.rvWaterHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnHydrationTarget.setOnClickListener { showHydrationGoalDialog() }
        binding.btnHydrationPlan.setOnClickListener { showHydrationIntervalDialog() }
        binding.btnHydrationRecord.setOnClickListener { showCustomAmountDialog() }

        binding.btnQuick250.setOnClickListener { addWater(250) }
        binding.btnQuick500.setOnClickListener { addWater(500) }
        binding.btnQuick1000.setOnClickListener { addWater(1000) }
        binding.btnQuickCustom.setOnClickListener { showCustomAmountDialog() }

        // Reminder settings
        binding.switchHydration.setOnCheckedChangeListener { _, isChecked ->
            prefs.saveBoolean("hydration_enabled", isChecked)
            if (isChecked) {
                ensurePostNotificationsPermission { scheduleHydrationReminder() }
            } else {
                cancelHydrationReminder()
            }
        }

        binding.hydrationIntervalContainer.setOnClickListener { showHydrationIntervalDialog() }
        binding.btnSetInterval.setOnClickListener { showHydrationIntervalDialog() }
        binding.btnSetGoal.setOnClickListener { showHydrationGoalDialog() }
    }

    private fun addWater(amount: Int) {
        hydrationRepo.add(amount)
        refreshHydrationUI()
    }

    private fun confirmDelete(entry: WaterEntry) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete entry?")
            .setMessage("Remove ${entry.amountMl} ml at ${java.text.SimpleDateFormat("h:mm a").format(java.util.Date(entry.createdAt))}?")
            .setPositiveButton("Delete") { _, _ ->
                hydrationRepo.delete(entry.id)
                refreshHydrationUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshHydrationUI() {
        val total = hydrationRepo.totalToday()
        val goal = prefs.getInt("hydration_goal_ml", 2000)
        val percent = if (goal > 0) (total * 100) / goal else 0

        binding.tvHydrationPercent.text = "${percent}%"
        binding.tvHydrationProgress.text = "${total}ml of ${goal}ml"
        
        // Update progress bar if it exists
        // binding.pbHydration?.progress = percent.coerceIn(0, 100)

        historyAdapter.replace(hydrationRepo.getToday())
    }

    private fun updateCalendar() {
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        binding.tvCurrentMonth.text = monthNames[calendar.get(Calendar.MONTH)]
        
        // Update day numbers (simplified - you can enhance this)
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        binding.hydrationDay1.text = "${today - 3}"
        binding.hydrationDay2.text = "${today - 2}"
        binding.hydrationDay3.text = "${today - 1}"
        binding.hydrationDay4.text = "$today"
        binding.hydrationDay5.text = "${today + 1}"
        binding.hydrationDay6.text = "${today + 2}"
        binding.hydrationDay7.text = "${today + 3}"
    }

    private fun showCustomAmountDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(5))
            hint = "e.g., 300"
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Custom Amount (ml)")
            .setView(input)
            .setPositiveButton("Add") { dlg, _ ->
                val v = input.text?.toString()?.trim()?.toIntOrNull()
                if (v != null && v in 50..3000) addWater(v)
                else Toast.makeText(
                    requireContext(),
                    "Enter 50–3000 ml",
                    Toast.LENGTH_SHORT
                ).show()
                dlg.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHydrationGoalDialog() {
        val current = prefs.getInt("hydration_goal_ml", 2000)
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(5))
            hint = "e.g., 2000"
            setText(current.toString())
            setSelection(text?.length ?: 0)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Daily Goal (ml)")
            .setView(input)
            .setPositiveButton("Save") { dlg, _ ->
                val value = input.text?.toString()?.trim()?.toIntOrNull()
                if (value != null && value in 200..10000) {
                    prefs.saveInt("hydration_goal_ml", value)
                    refreshHydrationUI()
                    Toast.makeText(
                        requireContext(),
                        "Daily goal set to ${value} ml",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Enter a value between 200 and 10000 ml",
                        Toast.LENGTH_LONG
                    ).show()
                }
                dlg.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHydrationIntervalDialog() {
        val intervals = reminderIntervals.map { it.first }.toTypedArray()
        val currentInterval = prefs.getInt("hydration_interval", 60)
        val currentIndex = reminderIntervals.indexOfFirst { it.second == currentInterval }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hydration Reminder Interval")
            .setSingleChoiceItems(intervals, currentIndex) { dlg, which ->
                val selectedInterval = reminderIntervals[which].second
                saveHydrationInterval(selectedInterval)
                dlg.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun saveHydrationInterval(intervalMinutes: Int) {
        prefs.saveInt("hydration_interval", intervalMinutes)
        Toast.makeText(
            requireContext(),
            "Reminder interval set to ${reminderIntervals.find { it.second == intervalMinutes }?.first}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun ensurePostNotificationsPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted(); return
        }
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(requireContext(), perm) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) onGranted() else requestPostNotifications.launch(perm)
    }

    private fun scheduleHydrationReminder() {
        val intervalMinutes = prefs.getInt("hydration_interval", 60)
        val intervalMillis = intervalMinutes * 60 * 1000L

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val startTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, intervalMinutes)
        }.timeInMillis

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            startTime,
            intervalMillis,
            pendingIntent
        )
        
        Toast.makeText(
            requireContext(),
            "Hydration reminders enabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun cancelHydrationReminder() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        Toast.makeText(
            requireContext(),
            "Hydration reminders disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadCurrentSettings() {
        val hydrationEnabled = prefs.getBoolean("hydration_enabled", false)
        binding.switchHydration.isChecked = hydrationEnabled

        val intervalMinutes = prefs.getInt("hydration_interval", 60)
        updateHydrationIntervalSummary(intervalMinutes)

        val goal = prefs.getInt("hydration_goal_ml", 2000)
        binding.tvHydrationGoalValue.text = "${goal} ml"
    }

    private fun updateHydrationIntervalSummary(intervalMinutes: Int) {
        val intervalText = reminderIntervals.find { it.second == intervalMinutes }?.first ?: "1 hour"
        binding.tvHydrationIntervalValue.text = intervalText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
