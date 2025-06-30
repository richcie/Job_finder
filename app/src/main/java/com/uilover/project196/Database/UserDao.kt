package com.uilover.project196.Database

import androidx.room.*
import com.uilover.project196.Model.UserEntity

@Dao
// KRITERIA WAJIB: Room DAO untuk operasi database User
interface UserDao {

    @Query("SELECT * FROM users WHERE isActive = 1")
    suspend fun getAllActiveUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = :role AND isActive = 1")
    suspend fun getUsersByRole(role: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateUser(userId: String)

    @Query("SELECT * FROM users WHERE role = 'freelancer' AND isActive = 1 ORDER BY rating DESC, completedProjects DESC")
    suspend fun getActiveFreelancers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("UPDATE users SET title = :newTitle WHERE userId = :userId")
    suspend fun updateUserTitle(userId: String, newTitle: String): Int

    @Query("UPDATE users SET role = :newRole WHERE userId = :userId")
    suspend fun updateUserRole(userId: String, newRole: String): Int

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}