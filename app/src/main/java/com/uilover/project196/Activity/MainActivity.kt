package com.uilover.project196.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.uilover.project196.Adapter.MainViewPagerAdapter
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ActivityMainBinding
import com.uilover.project196.ViewModel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : AppCompatActivity(), UserSession.LoginStateListener {
    lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: MainViewPagerAdapter
    private var currentIndicators: List<View> = listOf()
    private var currentTexts: List<View> = listOf()
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        UserSession.init(this)
        UserSession.addLoginStateListener(this)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.initializeDatabase(this)


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this@MainActivity)
                if (cacheManager.shouldPerformReset()) {
                    val resetSuccess = cacheManager.performComprehensiveReset()
                    withContext(Dispatchers.Main) {
                        if (resetSuccess) {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "🔄 App data reset for optimal performance",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                initializeJobSync()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error during app initialization", e)
            }
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )


        val initialTab = getInitialTabIndex(intent)

        setupViewPager()
        setupBottomNavigation(initialTab)


        setupManualCacheManagement()
    }

    private fun getInitialTabIndex(intent: Intent): Int {
        val userRole = UserSession.getUserRole()
        val isFreelancer = userRole == UserSession.ROLE_FREELANCER

        return when {
            intent.getBooleanExtra("OPEN_PROFILE", false) -> {
                if (isFreelancer) 5 else 4
            }
            intent.getBooleanExtra("NAVIGATE_TO_CHAT", false) -> {
                if (isFreelancer) 4 else 3
            }
            else -> 0
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = MainViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.isUserInputEnabled = true


        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigation(position)
            }
        })
    }

    private fun setupBottomNavigation(initialTab: Int = 0) {
        val userRole = UserSession.getUserRole()
        val isFreelancer = userRole == UserSession.ROLE_FREELANCER


        binding.jobsNav.visibility = if (isFreelancer) View.VISIBLE else View.GONE
        currentIndicators = if (isFreelancer) {
            listOf(
                binding.homeIndicator,
                binding.explorerIndicator,
                binding.jobsIndicator,
                binding.bookmarkIndicator,
                binding.chatIndicator,
                binding.profileIndicator
            )
        } else {

            listOf(
                binding.homeIndicator,
                binding.explorerIndicator,
                binding.bookmarkIndicator,
                binding.chatIndicator,
                binding.profileIndicator
            )
        }

        currentTexts = if (isFreelancer) {
            listOf(
                binding.homeText,
                binding.explorerText,
                binding.jobsText,
                binding.bookmarkText,
                binding.chatText,
                binding.profileText
            )
        } else {
            listOf(
                binding.homeText,
                binding.explorerText,
                binding.bookmarkText,
                binding.chatText,
                binding.profileText
            )
        }

        binding.homeNav.setOnClickListener {
            binding.viewPager.currentItem = 0
        }

        binding.explorerNav.setOnClickListener {
            binding.viewPager.currentItem = 1
        }

        if (isFreelancer) {
            binding.jobsNav.setOnClickListener {
                binding.viewPager.currentItem = 2
            }

            binding.bookmarkNav.setOnClickListener {
                binding.viewPager.currentItem = 3
            }

            binding.chatNav.setOnClickListener {
                binding.viewPager.currentItem = 4
            }

            binding.profileNav.setOnClickListener {
                binding.viewPager.currentItem = 5
            }
        } else {
            binding.bookmarkNav.setOnClickListener {
                binding.viewPager.currentItem = 2
            }

            binding.chatNav.setOnClickListener {
                binding.viewPager.currentItem = 3
            }

            binding.profileNav.setOnClickListener {
                binding.viewPager.currentItem = 4
            }
        }

        val maxTabs = if (isFreelancer) 6 else 5
        val safeInitialTab = if (initialTab >= maxTabs) 0 else initialTab
        binding.viewPager.setCurrentItem(safeInitialTab, false)
        updateBottomNavigation(safeInitialTab)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val initialTab = getInitialTabIndex(intent)
        binding.viewPager.currentItem = initialTab
    }

    private fun updateBottomNavigation(position: Int) {
        val userRole = UserSession.getUserRole()
        val isFreelancer = userRole == UserSession.ROLE_FREELANCER


        val currentIcons = if (isFreelancer) {
            listOf(
                binding.homeIcon,
                binding.explorerIcon,
                binding.jobsIcon,
                binding.bookmarkIcon,
                binding.chatIcon,
                binding.profileIcon
            )
        } else {
            listOf(
                binding.homeIcon,
                binding.explorerIcon,
                binding.bookmarkIcon,
                binding.chatIcon,
                binding.profileIcon
            )
        }


        currentIndicators.forEach { it.visibility = View.INVISIBLE }
        currentTexts.forEach { (it as android.widget.TextView).setTextColor(getColor(com.uilover.project196.R.color.darkGrey)) }
        currentIcons.forEach { it.setColorFilter(getColor(com.uilover.project196.R.color.darkGrey), PorterDuff.Mode.SRC_IN) }


        if (position < currentIndicators.size && position < currentTexts.size && position < currentIcons.size) {
            currentIndicators[position].visibility = View.VISIBLE
            (currentTexts[position] as android.widget.TextView).setTextColor(getColor(com.uilover.project196.R.color.purple))
            currentIcons[position].setColorFilter(getColor(com.uilover.project196.R.color.purple), PorterDuff.Mode.SRC_IN)
        }
    }


    override fun onLoginStateChanged(isLoggedIn: Boolean) {

        refreshNavigationAfterRoleChange()
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        refreshNavigationAfterRoleChange()
    }

    private fun refreshNavigationAfterRoleChange() {

        viewPagerAdapter = MainViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter


        val currentTab = binding.viewPager.currentItem


        val userRole = UserSession.getUserRole()
        val isFreelancer = userRole == UserSession.ROLE_FREELANCER


        val safeCurrentTab = when {
            isFreelancer && currentTab >= 6 -> 5
            !isFreelancer && currentTab >= 5 -> 4
            else -> currentTab
        }

        setupBottomNavigation(safeCurrentTab)
    }

    override fun onDestroy() {
        super.onDestroy()

        UserSession.removeLoginStateListener(this)


        if (isFinishing) {
            try {
                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this)
                cacheManager.markAppAsRestarting()
                android.util.Log.d("MainActivity", "App marked as restarting for next launch")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error marking app as restarting", e)
            }
        }
    }

    private fun setupManualCacheManagement() {

        binding.homeNav.setOnLongClickListener {
            showCacheManagementDialog()
            true
        }
    }

    private fun showCacheManagementDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cache Management")
        builder.setMessage("Choose an operation:\n\n• RESET: Comprehensive cache & data reset\n• STATS: Show cache statistics\n• FORCE: Force reset on next restart")

        builder.setPositiveButton("RESET") { _, _ ->
            performManualCacheReset()
        }
        builder.setNeutralButton("STATS") { _, _ ->
            showCacheStats()
        }
        builder.setNegativeButton("FORCE") { _, _ ->
            forceCacheResetOnNextRestart()
        }
        builder.show()
    }

    private fun showCacheStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this@MainActivity)
                val stats = cacheManager.getCacheStats()

                withContext(Dispatchers.Main) {
                    val fullStatus = """
                        $stats

                        USER INFO:
                        • Current user: ${UserSession.getUserId()}
                        • User role: ${UserSession.getUserRole()}
                        • Is logged in: ${UserSession.isLoggedIn()}

                        Check logs for detailed information.
                    """.trimIndent()

                    android.widget.Toast.makeText(this@MainActivity, fullStatus, android.widget.Toast.LENGTH_LONG).show()
                }

                android.util.Log.d("MainActivity", "=== CACHE STATISTICS ===")
                android.util.Log.d("MainActivity", stats)
                android.util.Log.d("MainActivity", "Current user: ${UserSession.getUserId()}")
                android.util.Log.d("MainActivity", "User role: ${UserSession.getUserRole()}")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@MainActivity, "Error getting cache stats: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                android.util.Log.e("MainActivity", "Error getting cache stats", e)
            }
        }
    }

    private fun performManualCacheReset() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("MainActivity", "=== PERFORMING MANUAL CACHE RESET ===")

                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this@MainActivity)
                val resetSuccess = cacheManager.performComprehensiveReset()

                withContext(Dispatchers.Main) {
                    if (resetSuccess) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "✅ COMPREHENSIVE CACHE RESET COMPLETED!\n• All caches cleared\n• Database reset\n• Memory optimized\n• App performance restored",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "❌ Cache reset failed. Check logs for details.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                android.util.Log.d("MainActivity", "Manual cache reset completed: $resetSuccess")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@MainActivity, "❌ Reset failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                android.util.Log.e("MainActivity", "❌ Error during manual cache reset", e)
            }
        }
    }

    private fun forceCacheResetOnNextRestart() {
        try {
            val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this)
            cacheManager.markAppAsRestarting()

            android.widget.Toast.makeText(
                this,
                "🔄 Cache reset scheduled for next app restart",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            android.util.Log.d("MainActivity", "Cache reset scheduled for next restart")
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Error scheduling reset: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            android.util.Log.e("MainActivity", "Error scheduling cache reset", e)
        }
    }


    private suspend fun initializeJobSync() = withContext(Dispatchers.IO) {
        try {
            val jobSyncHelper = com.uilover.project196.Utils.JobSyncHelper.getInstance()
            jobSyncHelper.initialize(this@MainActivity)


            val lastSyncTime = getSharedPreferences("app_state", MODE_PRIVATE)
                .getLong("last_job_sync", 0)
            val currentTime = System.currentTimeMillis()
            val hoursSinceLastSync = (currentTime - lastSyncTime) / (1000 * 60 * 60)

            if (hoursSinceLastSync >= 1) {
                val syncSuccess = jobSyncHelper.syncAllJobsToDatabase()
                if (syncSuccess) {
                    getSharedPreferences("app_state", MODE_PRIVATE)
                        .edit()
                        .putLong("last_job_sync", currentTime)
                        .apply()
                }
                android.util.Log.d("MainActivity", "Job sync completed: success=$syncSuccess")
            } else {
                android.util.Log.d("MainActivity", "Skipping job sync - last sync was ${hoursSinceLastSync} hours ago")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error during job sync", e)
        }
    }
}