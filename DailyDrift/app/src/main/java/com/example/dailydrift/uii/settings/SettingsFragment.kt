package com.example.dailydrift.uii.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dailydrift.data.pref.Prefs
import com.example.dailydrift.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _settingsBinding: FragmentSettingsBinding? = null
    private val settingsBinding get() = _settingsBinding!!
    private lateinit var prefs: Prefs

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Handle notification permission granted
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
        _settingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return settingsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        setupClickListeners()
        loadCurrentSettings()
    }

    private fun setupClickListeners() {
        settingsBinding.themeContainer.setOnClickListener { showThemeDialog() }

        settingsBinding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference(isChecked)
        }
    }

    private fun loadCurrentSettings() {
        val currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        updateThemeSummary(currentTheme)

        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        settingsBinding.switchNotifications.isChecked = notificationsEnabled
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System Default", "Light Mode", "Dark Mode")
        val currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, getThemeIndex(currentTheme)) { dlg, which ->
                val selectedTheme = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                applyTheme(selectedTheme)
                dlg.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun getThemeIndex(themeMode: Int): Int = when (themeMode) {
        AppCompatDelegate.MODE_NIGHT_NO -> 1
        AppCompatDelegate.MODE_NIGHT_YES -> 2
        else -> 0
    }

    private fun applyTheme(themeMode: Int) {
        prefs.saveInt("theme_mode", themeMode)
        AppCompatDelegate.setDefaultNightMode(themeMode)
        updateThemeSummary(themeMode)
    }

    private fun updateThemeSummary(themeMode: Int) {
        val themeName = when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light Mode"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark Mode"
            else -> "System Default"
        }
        settingsBinding.tvThemeValue.text = themeName
    }

    private fun saveNotificationPreference(isEnabled: Boolean) {
        prefs.saveBoolean("notifications_enabled", isEnabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _settingsBinding = null
    }
}