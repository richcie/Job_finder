package com.uilover.project196

import android.app.Application
import kotlinx.coroutines.*


// Application class untuk inisialisasi global aplikasi
// Setup database dan dependency injection
class JobFinderApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        android.util.Log.d("JobFinderApplication", "=== APPLICATION STARTING ===")


        applicationScope.launch {
            try {
                android.util.Log.d("JobFinderApplication", "Starting comprehensive app restart reset...")


                android.util.Log.d("JobFinderApplication", "Resetting all database state...")
                val database = com.uilover.project196.Database.AppDatabase.getDatabase(this@JobFinderApplication)
                database.resetAllDatabase()


                android.util.Log.d("JobFinderApplication", "Initializing repositories...")
                val mainRepository = com.uilover.project196.Repository.MainRepository.getInstance()
                mainRepository.initializeDatabase(this@JobFinderApplication)


                android.util.Log.d("JobFinderApplication", "Initializing dummy reviews for Sarah Johnson's business...")
                val reviewRepository = com.uilover.project196.Repository.ReviewRepository.getInstance(this@JobFinderApplication)
                reviewRepository.initializeDummyReviews("user_002")


                android.util.Log.d("JobFinderApplication", "Clearing all SharedPreferences...")
                clearAllSharedPreferences()


                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this@JobFinderApplication)
                android.util.Log.d("JobFinderApplication", "Cache manager initialized: ${cacheManager.javaClass.simpleName}")

                if (cacheManager.shouldPerformReset()) {
                    android.util.Log.d("JobFinderApplication", "Performing background cache optimization...")
                    cacheManager.performComprehensiveReset()
                }


                clearFileCaches()

                android.util.Log.d("JobFinderApplication", "✅ COMPLETE APP STATE RESET FINISHED")
                android.util.Log.d("JobFinderApplication", "✅ Application initialization completed with fresh state")

            } catch (e: Exception) {
                android.util.Log.e("JobFinderApplication", "Error during application initialization", e)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()

        android.util.Log.w("JobFinderApplication", "Low memory detected - clearing caches")


        applicationScope.launch {
            try {
                val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this@JobFinderApplication)




                System.gc()

                android.util.Log.d("JobFinderApplication", "Emergency cache cleanup completed")
            } catch (e: Exception) {
                android.util.Log.e("JobFinderApplication", "Error during low memory cleanup", e)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
                android.util.Log.d("JobFinderApplication", "UI hidden - light memory cleanup")

                System.gc()
            }
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                android.util.Log.w("JobFinderApplication", "Memory pressure detected (level: $level) - aggressive cleanup")

                applicationScope.launch {
                    try {

                        cacheDir?.listFiles()?.forEach { file ->
                            try {
                                if (file.name.contains("cache") || file.name.endsWith(".tmp")) {
                                    file.delete()
                                }
                            } catch (e: Exception) {

                            }
                        }
                        System.gc()
                    } catch (e: Exception) {
                        android.util.Log.e("JobFinderApplication", "Error during memory trim", e)
                    }
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        android.util.Log.d("JobFinderApplication", "Application terminating - marking for restart")

        try {

            val cacheManager = com.uilover.project196.Utils.CacheManager.getInstance(this)
            cacheManager.markAppAsRestarting()


            applicationScope.cancel()

        } catch (e: Exception) {
            android.util.Log.e("JobFinderApplication", "Error during application termination", e)
        }
    }


    private fun clearAllSharedPreferences() {
        try {

            val sharedPrefNames = listOf(
                "user_session",
                "user_status",
                "app_preferences",
                "cache_preferences",
                "attendance_manager",
                "job_cache",
                "${packageName}_preferences"
            )

            for (prefName in sharedPrefNames) {
                try {
                    val prefs = getSharedPreferences(prefName, MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    android.util.Log.d("JobFinderApplication", "Cleared SharedPreferences: $prefName")
                } catch (e: Exception) {
                    android.util.Log.w("JobFinderApplication", "Could not clear SharedPreferences: $prefName", e)
                }
            }

            android.util.Log.d("JobFinderApplication", "✅ All SharedPreferences cleared")

        } catch (e: Exception) {
            android.util.Log.e("JobFinderApplication", "Error clearing SharedPreferences", e)
        }
    }


    private fun clearFileCaches() {
        try {

            cacheDir?.listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("JobFinderApplication", "Could not delete cache file: ${file.name}", e)
                }
            }


            externalCacheDir?.listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("JobFinderApplication", "Could not delete external cache file: ${file.name}", e)
                }
            }

            android.util.Log.d("JobFinderApplication", "✅ All file caches cleared")

        } catch (e: Exception) {
            android.util.Log.e("JobFinderApplication", "Error clearing file caches", e)
        }
    }
}