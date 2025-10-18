package com.example.dailydrift.uii.mood

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailydrift.data.model.MoodEntry
import com.example.dailydrift.data.repo.MoodRepo
import com.example.dailydrift.R
import android.content.Intent

import com.example.dailydrift.databinding.FragmentMoodBinding

class MoodFragment : Fragment() {

    private var _binding: FragmentMoodBinding? = null
    private val binding get() = _binding!!
    private val moodEntries = mutableListOf<MoodEntry>()
    private lateinit var moodRepo: MoodRepo
    private lateinit var moodAdapter: MoodAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moodRepo = MoodRepo(requireContext())

        setupRecyclerView()
        setupClickListeners()
        loadMoodEntries()



    }


    private fun setupRecyclerView() {
        moodAdapter = MoodAdapter(moodEntries) { moodEntry ->
            showDeleteConfirmation(moodEntry)
        }

        binding.rvMoodEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnLogMood.setOnClickListener {
            showAddMoodDialog()
        }

        binding.btnLogMood.setOnLongClickListener {
            shareText(buildWeeklyMoodSummary())
            true
        }

        binding.btnShareWeek.setOnClickListener {
            shareText(buildWeeklyMoodSummary())
        }
    }


    private fun loadMoodEntries() {
        val entries = moodRepo.getMoodEntries().sortedByDescending { it.createdAt }
        moodEntries.clear()
        moodEntries.addAll(entries)
        moodAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun showAddMoodDialog() {
        val dialog = AddMoodDialogFragment { newMoodEntry ->
            moodRepo.addMoodEntry(newMoodEntry)
            loadMoodEntries()
        }
        dialog.show(parentFragmentManager, "AddMoodDialog")
    }

    private fun showDeleteConfirmation(moodEntry: MoodEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?")
            .setPositiveButton("Delete") { _, _ ->
                moodRepo.deleteMoodEntry(moodEntry.id)
                loadMoodEntries()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        binding.emptyState.visibility = if (moodEntries.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMoodEntries.visibility = if (moodEntries.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildWeeklyMoodSummary(): String {
        val lastWeek = moodRepo.getMoodEntriesForWeek()
        if (lastWeek.isEmpty()) return "No mood entries this week."

        val counts = lastWeek.groupingBy { it.moodName }.eachCount()
            .entries.sortedByDescending { it.value }

        return buildString {
            append("My weekly mood summary:\n")
            counts.forEach { append("• ${it.key}: ${it.value}\n") }
        }
    }

    private fun shareText(text: String) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(share, getString(R.string.share_chooser_title)))
    }


}