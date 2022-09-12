package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersDao: RemindersDao

    @Before
    fun setup() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        remindersDao = remindersDatabase.reminderDao()
    }

    @After
    fun clean() {
        remindersDatabase.close()
    }

    @Test
    fun deleteAllReminders_actuallyDeletesAllReminders() = runBlockingTest {

        // Given a dao with 5 reminders
        repeat(5) {
            val reminder = ReminderDTO("test", "test", "test", null, null)
            remindersDao.saveReminder(reminder)
        }
        val initialRemindersList = remindersDao.getReminders()
        assertThat(initialRemindersList.size, `is`(5))

        // When calling deleteAllReminders
        remindersDao.deleteAllReminders()

        // Then calling getReminders must return an empty list
        val currentRemindersList = remindersDao.getReminders()
        assertThat(currentRemindersList.isEmpty(), `is`(true))
    }

    @Test
    fun reminderDto_canBeRetrievedById() = runBlockingTest {

        // Given a remindersDto with a reminder with known id
        val reminder = ReminderDTO("test", "test", "test", null, null)
        val reminderId = reminder.id
        remindersDao.saveReminder(reminder)

        // When attempting to retrieve the reminder by id
        val retrievedReminder = remindersDao.getReminderById(reminderId)

        // Then retrievedReminder can't be null and must have the same id , title ,description
        assertThat(retrievedReminder , notNullValue())
        assertThat(retrievedReminder!!.id, `is`(reminderId))
        assertThat(retrievedReminder.title, `is`(reminder.title))
        assertThat(retrievedReminder.description, `is`(reminder.description))
    }

    @Test
    fun attemptingToRetrieveReminderNotExisting_returnsNullWithNoErrors() = runBlockingTest {

        // When trying to get reminder that doesn't exist is dao
        val reminder = remindersDao.getReminderById("randomId")

        // Then reminder must be null
        assertThat(reminder == null, `is`(true))
    }

    @Test
    fun getRemindersOnEmptyDB_returnsEmptyRemindersList() {
        runBlockingTest {

            // When calling get reminders on empty Dao
            val reminders = remindersDao.getReminders()

            // Then reminders must be empty list
            assertThat(reminders.isEmpty(), `is`(true))
        }
    }
}