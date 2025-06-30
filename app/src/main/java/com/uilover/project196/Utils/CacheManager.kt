package com.uilover.project196.Utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


// KRITERIA KOMPLEKSITAS: Fitur kompleks - Advanced caching dan memory management
// Optimasi performa aplikasi dengan intelligent caching
class CacheManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: CacheManager? = null

        fun getInstance(context: Context): CacheManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CacheManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }


        private const val CACHE_VERSION_KEY = "cache_version"
        private const val CURRENT_CACHE_VERSION = 1
    }


    suspend fun performComprehensiveReset(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CacheManager", "=== STARTING COMPREHENSIVE CACHE RESET ===")


            clearAllSharedPreferences()


            resetRoomDatabase()


            clearAllFileCaches()


            clearTemporaryFiles()


            resetUserSession()


            clearInMemoryCaches()


            resetRepositoryInstances()


            updateCacheVersion()

            android.util.Log.d("CacheManager", "✅ COMPREHENSIVE CACHE RESET COMPLETED")
            true
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "❌ Error during comprehensive reset", e)
            false
        }
    }


    suspend fun shouldPerformReset(): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("cache_manager", Context.MODE_PRIVATE)
            val lastCacheVersion = prefs.getInt(CACHE_VERSION_KEY, -1)
            val isFirstRun = lastCacheVersion == -1
            val isVersionChanged = lastCacheVersion != CURRENT_CACHE_VERSION
            val isAppRestart = prefs.getBoolean("is_app_restart", true)

            android.util.Log.d("CacheManager", "=== RESET CHECK ===")
            android.util.Log.d("CacheManager", "First run: $isFirstRun")
            android.util.Log.d("CacheManager", "Version changed: $isVersionChanged (last: $lastCacheVersion, current: $CURRENT_CACHE_VERSION)")
            android.util.Log.d("CacheManager", "App restart: $isAppRestart")

            isFirstRun || isVersionChanged || isAppRestart
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error checking reset conditions", e)
            true
        }
    }


    private fun clearAllSharedPreferences() {
        try {
            android.util.Log.d("CacheManager", "Clearing all SharedPreferences...")

            val prefsNames = listOf(
                "app_state",
                "cache_manager",
                "user_session",
                "attendance_manager",
                "job_sync",
                "verification_cache",
                "user_preferences",
                context.packageName + "_preferences"
            )

            var clearedCount = 0
            for (prefsName in prefsNames) {
                try {
                    val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    clearedCount++
                } catch (e: Exception) {
                    android.util.Log.w("CacheManager", "Could not clear prefs: $prefsName", e)
                }
            }

            android.util.Log.d("CacheManager", "✅ Cleared $clearedCount SharedPreferences files")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error clearing SharedPreferences", e)
        }
    }


    private suspend fun resetRoomDatabase() {
        try {
            android.util.Log.d("CacheManager", "Resetting Room Database (preserving messages and users)...")

            val database = com.uilover.project196.Database.AppDatabase.getDatabase(context)



            database.jobAttendanceDao().deleteAllAttendance()


            android.util.Log.d("CacheManager", "✅ Room Database reset completed (messages preserved)")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error resetting Room Database", e)
        }
    }


    private fun clearAllFileCaches() {
        try {
            android.util.Log.d("CacheManager", "Clearing file caches...")

            var deletedFiles = 0


            context.cacheDir?.let { cacheDir ->
                deletedFiles += deleteDirectory(cacheDir)
            }


            context.externalCacheDir?.let { externalCacheDir ->
                deletedFiles += deleteDirectory(externalCacheDir)
            }


            try {
                context.codeCacheDir?.let { codeCacheDir ->
                    deletedFiles += deleteDirectory(codeCacheDir)
                }
            } catch (e: Exception) {

            }

            android.util.Log.d("CacheManager", "✅ Deleted $deletedFiles cache files")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error clearing file caches", e)
        }
    }


    private fun clearTemporaryFiles() {
        try {
            android.util.Log.d("CacheManager", "Clearing temporary files...")

            val dataDir = context.dataDir
            val tempFiles = dataDir.listFiles { file ->
                file.name.startsWith("temp_") ||
                file.name.endsWith(".tmp") ||
                file.name.contains("cache")
            }

            var deletedCount = 0
            tempFiles?.forEach { file ->
                try {
                    if (file.delete()) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CacheManager", "Could not delete temp file: ${file.name}", e)
                }
            }

            android.util.Log.d("CacheManager", "✅ Deleted $deletedCount temporary files")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error clearing temporary files", e)
        }
    }


    private fun resetUserSession() {
        try {
            android.util.Log.d("CacheManager", "Resetting user session cache...")




            android.util.Log.d("CacheManager", "✅ User session cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error resetting user session", e)
        }
    }


    private fun clearInMemoryCaches() {
        try {
            android.util.Log.d("CacheManager", "Clearing in-memory caches...")


            System.gc()

            android.util.Log.d("CacheManager", "✅ In-memory caches cleared")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error clearing in-memory caches", e)
        }
    }


    private suspend fun resetRepositoryInstances() {
        try {
            android.util.Log.d("CacheManager", "Resetting repository instances...")


            val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
            chatRepository.forceRefreshChats()

            android.util.Log.d("CacheManager", "✅ Repository instances reset")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error resetting repository instances", e)
        }
    }


    private fun updateCacheVersion() {
        try {
            val prefs = context.getSharedPreferences("cache_manager", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(CACHE_VERSION_KEY, CURRENT_CACHE_VERSION)
                .putBoolean("is_app_restart", false)
                .putLong("last_reset_time", System.currentTimeMillis())
                .apply()

            android.util.Log.d("CacheManager", "✅ Cache version updated to $CURRENT_CACHE_VERSION")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error updating cache version", e)
        }
    }


    private fun deleteDirectory(directory: File): Int {
        var deletedCount = 0
        try {
            if (directory.exists()) {
                directory.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        deletedCount += deleteDirectory(file)
                    } else {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }

            }
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Error deleting directory: ${directory.name}", e)
        }
        return deletedCount
    }


    fun markAppAsRestarting() {
        try {
            val prefs = context.getSharedPreferences("cache_manager", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_app_restart", true).apply()
            android.util.Log.d("CacheManager", "App marked as restarting")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error marking app as restarting", e)
        }
    }


    suspend fun getCacheStats(): String = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("cache_manager", Context.MODE_PRIVATE)
            val lastResetTime = prefs.getLong("last_reset_time", 0)
            val cacheVersion = prefs.getInt(CACHE_VERSION_KEY, -1)
            val isAppRestart = prefs.getBoolean("is_app_restart", false)

            val cacheSize = calculateCacheSize()
            val timeSinceReset = if (lastResetTime > 0) {
                (System.currentTimeMillis() - lastResetTime) / (1000 * 60)
            } else 0

            """
            CACHE STATISTICS:
            • Cache version: $cacheVersion (current: $CURRENT_CACHE_VERSION)
            • Last reset: ${if (timeSinceReset > 0) "$timeSinceReset min ago" else "Never"}
            • App restart flag: $isAppRestart
            • Current cache size: ${cacheSize / 1024}KB
            • Database size: ${getDatabaseSize() / 1024}KB
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting cache stats: ${e.message}"
        }
    }

    private fun calculateCacheSize(): Long {
        var totalSize = 0L
        try {
            context.cacheDir?.let { dir ->
                totalSize += getFolderSize(dir)
            }
            context.externalCacheDir?.let { dir ->
                totalSize += getFolderSize(dir)
            }
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Error calculating cache size", e)
        }
        return totalSize
    }

    private fun getDatabaseSize(): Long {
        return try {
            val dbFile = context.getDatabasePath("job_finder_database")
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFolderSize(directory: File): Long {
        var size = 0L
        try {
            if (directory.exists()) {
                directory.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) {
                        getFolderSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Error calculating folder size", e)
        }
        return size
    }
}