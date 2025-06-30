package com.uilover.project196.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uilover.project196.Model.JobEntity
import com.uilover.project196.Model.UserEntity
import com.uilover.project196.Model.JobApplicationEntity
import com.uilover.project196.Model.JobViewEntity
import com.uilover.project196.Model.MessageEntity
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.Model.ReviewEntity

// KRITERIA: Local Storage menggunakan Database Room
// KRITERIA WAJIB: Local Storage menggunakan Database Room
// Database dengan 7 entitas: JobEntity, UserEntity, JobApplicationEntity, JobViewEntity, MessageEntity, JobAttendanceEntity, ReviewEntity
@Database(
    entities = [JobEntity::class, UserEntity::class, JobApplicationEntity::class, JobViewEntity::class, MessageEntity::class, JobAttendanceEntity::class, ReviewEntity::class],
    version = 15,
    exportSchema = false
)
// KRITERIA: Room Database dengan 7 entitas
abstract class AppDatabase : RoomDatabase() {

    abstract fun jobDao(): JobDao
    abstract fun userDao(): UserDao
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun jobViewDao(): JobViewDao
    abstract fun messageDao(): MessageDao
    abstract fun jobAttendanceDao(): JobAttendanceDao
    abstract fun reviewDao(): ReviewDao


    suspend fun resetAllDatabase() {
        android.util.Log.d("AppDatabase", "=== RESETTING ALL DATABASE STATE ===")
        android.util.Log.d("AppDatabase", "CLEARING: All data including chat messages")

        try {


            android.util.Log.d("AppDatabase", "Clearing job applications...")
            jobApplicationDao().deleteAllApplications()

            android.util.Log.d("AppDatabase", "Clearing job attendance...")
            jobAttendanceDao().deleteAllAttendance()

            android.util.Log.d("AppDatabase", "Clearing job views...")
            jobViewDao().deleteAllViews()


            android.util.Log.d("AppDatabase", "Clearing all chat messages...")
            messageDao().deleteAllMessages()

            android.util.Log.d("AppDatabase", "Clearing reviews...")
            reviewDao().deleteAllReviews()

            android.util.Log.d("AppDatabase", "Clearing jobs...")
            jobDao().deleteAllJobs()


            android.util.Log.d("AppDatabase", "PRESERVING users for account continuity...")


            android.util.Log.d("AppDatabase", "✅ COMPLETE DATABASE RESET FINISHED")
            android.util.Log.d("AppDatabase", "✅ All chat messages deleted, user accounts preserved")
            android.util.Log.d("AppDatabase", "=== DATABASE RESET COMPLETE ===")

        } catch (e: Exception) {
            android.util.Log.e("AppDatabase", "❌ Error during database reset", e)
            throw e
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_applications ADD COLUMN skills TEXT NOT NULL DEFAULT ''")
            }
        }


        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_applications ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }


        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT PRIMARY KEY NOT NULL,
                        chatId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_chatId ON chat_messages (chatId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_senderId ON chat_messages (senderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_timestamp ON chat_messages (timestamp)")
            }
        }


        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN messageType TEXT NOT NULL DEFAULT 'text'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN jobRequestStatus TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN jobId TEXT")
            }
        }


        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN verificationStatus TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN freelancerData TEXT")
            }
        }


        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS job_attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        jobId INTEGER NOT NULL,
                        freelancerId TEXT NOT NULL,
                        attendanceDate TEXT NOT NULL,
                        checkInTime INTEGER,
                        checkOutTime INTEGER,
                        progressReport TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_attendance_jobId ON job_attendance (jobId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_attendance_freelancerId ON job_attendance (freelancerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_attendance_attendanceDate ON job_attendance (attendanceDate)")
            }
        }


        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {


                db.execSQL("""
                    DELETE FROM job_attendance
                    WHERE id NOT IN (
                        SELECT MAX(id)
                        FROM job_attendance
                        GROUP BY jobId, freelancerId, attendanceDate
                    )
                """)


                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_job_attendance_unique ON job_attendance (jobId, freelancerId, attendanceDate)")
            }
        }


        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("DROP INDEX IF EXISTS index_job_attendance_unique")


                db.execSQL("""
                    DELETE FROM job_attendance
                    WHERE id NOT IN (
                        SELECT MAX(id)
                        FROM job_attendance
                        GROUP BY jobId, freelancerId, attendanceDate
                    )
                """)


                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_job_attendance_unique ON job_attendance (jobId, freelancerId, attendanceDate)")
            }
        }


        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reviews (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        businessOwnerId TEXT NOT NULL,
                        reviewerId TEXT NOT NULL,
                        reviewerName TEXT NOT NULL,
                        reviewerTitle TEXT NOT NULL,
                        reviewerExperience TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        reviewText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isVerified INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_businessOwnerId ON reviews (businessOwnerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_reviewerId ON reviews (reviewerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_timestamp ON reviews (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_rating ON reviews (rating)")
            }
        }


        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("DROP TABLE IF EXISTS reviews")


                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reviews (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        businessOwnerId TEXT NOT NULL,
                        reviewerId TEXT NOT NULL,
                        reviewerName TEXT NOT NULL,
                        reviewerTitle TEXT NOT NULL,
                        reviewerExperience TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        reviewText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isVerified INTEGER NOT NULL
                    )
                """)


                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_businessOwnerId ON reviews (businessOwnerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_reviewerId ON reviews (reviewerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_timestamp ON reviews (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_rating ON reviews (rating)")
            }
        }


        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("DROP TABLE IF EXISTS reviews")
                db.execSQL("DROP INDEX IF EXISTS index_reviews_businessOwnerId")
                db.execSQL("DROP INDEX IF EXISTS index_reviews_reviewerId")
                db.execSQL("DROP INDEX IF EXISTS index_reviews_timestamp")
                db.execSQL("DROP INDEX IF EXISTS index_reviews_rating")


                db.execSQL("""
                    CREATE TABLE reviews (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        businessOwnerId TEXT NOT NULL,
                        reviewerId TEXT NOT NULL,
                        reviewerName TEXT NOT NULL,
                        reviewerTitle TEXT NOT NULL,
                        reviewerExperience TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        reviewText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isVerified INTEGER NOT NULL
                    )
                """)


                db.execSQL("CREATE INDEX index_reviews_businessOwnerId ON reviews (businessOwnerId)")
                db.execSQL("CREATE INDEX index_reviews_reviewerId ON reviews (reviewerId)")
                db.execSQL("CREATE INDEX index_reviews_timestamp ON reviews (timestamp)")
                db.execSQL("CREATE INDEX index_reviews_rating ON reviews (rating)")
            }
        }


        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("ALTER TABLE reviews ADD COLUMN companyName TEXT NOT NULL DEFAULT ''")


                db.execSQL("UPDATE reviews SET companyName = 'Chaboksoft' WHERE businessOwnerId = 'user_002'")

                android.util.Log.d("AppDatabase", "Migration 14->15: Added companyName field to reviews table")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "job_finder_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}