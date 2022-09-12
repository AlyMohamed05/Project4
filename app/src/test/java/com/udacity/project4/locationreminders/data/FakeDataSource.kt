package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource : ReminderDataSource {

    private val reminders = mutableListOf<ReminderDTO>()
    private var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Couldn't load reminders")
        }
        return Result.Success(reminders.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = reminders.find { reminderDTO ->
            reminderDTO.id == id
        }
        return if (reminder != null) {
            Result.Success(reminder)
        } else {
            Result.Error("reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    fun setShouldReturnError(returnError: Boolean) {
        shouldReturnError = returnError
    }

}