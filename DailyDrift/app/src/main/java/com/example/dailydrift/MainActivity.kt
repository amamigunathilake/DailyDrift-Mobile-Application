package com.example.dailydrift

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dailydrift.databinding.ActivityMainBinding
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EXTRA_OPEN_DEST = "open_tab"
        const val DEST_SETTINGS = "settings"
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if we should skip splash screen (coming from onboarding)
        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        
        if (skipSplash) {
            // Skip splash and go directly to main app
            openNavigation()
            handleOpenDestIntent(intent)
        } else {
            // Show splash screen for 2 seconds, then check for onboarding
            binding.landingRoot.postDelayed({
                if (isFirstLaunch()) {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                    finish()
                } else {
                    openNavigation()
                    handleOpenDestIntent(intent)
                }
            }, 2000) // 2 second splash screen
        }

        binding.landingRoot.setOnClickListener {
            openNavigation()
            // Handle deep-link after nav is visible
            handleOpenDestIntent(intent)
        }

        val shouldShowNavigation = savedInstanceState?.getBoolean("navigation_visible", false)
            ?: (binding.navigationContainer.visibility == View.VISIBLE)

        if (shouldShowNavigation) {
            openNavigation()
        }

        handleOpenDestIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenDestIntent(intent)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            "navigation_visible",
            binding.navigationContainer.visibility == View.VISIBLE
        )
    }

    private fun openNavigation() {
        binding.navigationContainer.visibility = View.VISIBLE
        binding.landingRoot.visibility = View.GONE

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setBackgroundColor(
            resources.getColor(R.color.primary_nav_background, null)
        )
    }

    private fun handleOpenDestIntent(intent: android.content.Intent?) {
        val dest = intent?.getStringExtra(EXTRA_OPEN_DEST) ?: return

        if (dest == DEST_SETTINGS) {
            if (binding.navigationContainer.visibility != View.VISIBLE) {
                openNavigation()
            }

            binding.bottomNavigation.selectedItemId = R.id.nav_settings
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    private fun markAppAsLaunched() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
}
