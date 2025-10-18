package com.example.dailydrift.uii.mood

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.dailydrift.data.model.MoodEntry
import com.example.dailydrift.databinding.DialogAddMoodBinding

class AddMoodDialogFragment(
    private val onSave: (MoodEntry) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAddMoodBinding
    private var selectedMood: Pair<String, String>? = null

    private val availableMoods = listOf(
        "😊" to "Happy",
        "😄" to "Excited",
        "😐" to "Neutral",
        "😔" to "Sad",
        "😠" to "Angry",
        "😰" to "Anxious",
        "😴" to "Tired",
        "🥰" to "Loved",
        "🤩" to "Amazed",
        "😌" to "Peaceful"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddMoodBinding.inflate(layoutInflater)

        setupMoodSelection()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("How are you feeling?")
            .setPositiveButton("Save", null) // We'll override this later
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateAndSave()) {
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun setupMoodSelection() {
        binding.moodContainer.removeAllViews()

        availableMoods.forEach { (emoji, name) ->
            val moodButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
                text = "$emoji\n$name"
                setTextColor(resources.getColor(com.example.dailydrift.R.color.text_primary, null))
                strokeColor = resources.getColorStateList(com.example.dailydrift.R.color.secondary_500, null)
                strokeWidth = 1
                backgroundTintList = resources.getColorStateList(com.example.dailydrift.R.color.primary_surface, null)
                isAllCaps = false
                textSize = 12f
                setOnClickListener {
                    selectedMood = emoji to name
                    updateMoodSelection()
                }
            }

            binding.moodContainer.addView(moodButton)
        }
    }

    private fun updateMoodSelection() {
        selectedMood?.let { (emoji, name) ->
            binding.tvSelectedMood.text = "$emoji $name"
        }
    }

    private fun validateAndSave(): Boolean {
        if (selectedMood == null) {
            Toast.makeText(requireContext(), "Please select a mood", Toast.LENGTH_SHORT).show()
            return false
        }

        val (emoji, name) = selectedMood!!
        val note = binding.etMoodNote.text.toString().trim()

        val currentTime = System.currentTimeMillis()
        val moodEntry = MoodEntry(
            moodEmoji = emoji,
            moodName = name,
            note = note,
            createdAt = currentTime
        )
        
        android.util.Log.d("AddMoodDialog", "Creating mood with timestamp: $currentTime")

        onSave(moodEntry)
        return true
    }
}