package com.example.dailydrift.uii.habits

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.dailydrift.data.model.Habit
import com.example.dailydrift.databinding.DialogAddHabitBinding

class AddHabitDialogFragment(
    private val existingHabit: Habit? = null,
    private val onSave: (Habit) -> Unit,
    private val onDelete: ((Habit) -> Unit)? = null
) : DialogFragment() {

    private lateinit var binding: DialogAddHabitBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddHabitBinding.inflate(layoutInflater)

        if (savedInstanceState != null && existingHabit == null) {
            binding.etHabitName.setText(savedInstanceState.getString(KEY_NAME, ""))
            binding.etHabitDescription.setText(savedInstanceState.getString(KEY_DESC, ""))
            binding.etTargetCount.setText(savedInstanceState.getString(KEY_TARGET, DEFAULT_TARGET))
        }

        existingHabit?.let { habit ->
            binding.etHabitName.setText(habit.name)
            binding.etHabitDescription.setText(habit.description)
            binding.etTargetCount.setText(habit.targetCount.toString())
            
            binding.btnDeleteHabit.visibility = android.view.View.VISIBLE
            binding.btnDeleteHabit.setOnClickListener {
                showDeleteConfirmation(habit)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingHabit != null) "Edit Habit" else "Add New Habit")
            .setPositiveButton("Save", null) // We'll override this later
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        binding.etTargetCount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val ok = validateAndSave()
                if (ok) dialog.dismiss()
                true
            } else false
        }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (existingHabit == null) {
            outState.putString(KEY_NAME, binding.etHabitName.text?.toString() ?: "")
            outState.putString(KEY_DESC, binding.etHabitDescription.text?.toString() ?: "")
            outState.putString(KEY_TARGET, binding.etTargetCount.text?.toString() ?: DEFAULT_TARGET)
        }
    }

    private fun validateAndSave(): Boolean {
        val name = binding.etHabitName.text.toString().trim()
        val description = binding.etHabitDescription.text.toString().trim()
        val targetCountText = binding.etTargetCount.text.toString().trim().ifEmpty { DEFAULT_TARGET }

        // Validation
        if (TextUtils.isEmpty(name)) {
            showError(binding.etHabitName, "Habit name is required")
            return false
        }

        if (TextUtils.isEmpty(targetCountText)) {
            showError(binding.etTargetCount, "Target count is required")
            return false
        }

        val targetCount = try {
            targetCountText.toInt()
        } catch (e: NumberFormatException) {
            showError(binding.etTargetCount, "Please enter a valid number")
            return false
        }

        if (targetCount <= 0) {
            showError(binding.etTargetCount, "Target count must be at least 1")
            return false
        }

        val habit = if (existingHabit != null) {
            existingHabit.copy(
                name = name,
                description = description,
                targetCount = targetCount
            )
        } else {
            Habit(
                name = name,
                description = description,
                targetCount = targetCount
            )
        }

        onSave(habit)
        return true
    }

    private fun showError(editText: EditText, message: String) {
        editText.error = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(habit: Habit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                onDelete?.invoke(habit)
                dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val KEY_NAME = "state_name"
        private const val KEY_DESC = "state_desc"
        private const val KEY_TARGET = "state_target"
        private const val DEFAULT_TARGET = "1"
    }
}