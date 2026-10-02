package com.aydinsogut.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAt")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isDone = 0")
    suspend fun getActive(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isDone = 0 AND triggerAt >= :from ORDER BY triggerAt LIMIT :limit")
    suspend fun getUpcoming(from: Long, limit: Int): List<Reminder>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("UPDATE reminders SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
}
