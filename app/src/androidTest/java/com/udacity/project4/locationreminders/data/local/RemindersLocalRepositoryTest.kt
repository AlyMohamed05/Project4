package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutor = InstantTaskExecutorRule()

    private var testDispatcher = TestCoroutineDispatcher()

    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersDao: RemindersDao
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun setup() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        remindersDao = remindersDatabase.reminderDao()
        remindersLocalRepository = RemindersLocalRepository(
            remindersDao,
            testDispatcher
        )
    }

    @After
    fun clean() {
        remindersDatabase.close()
    }

    @Test
    fun getReminders_returnEmptyRemindersList() = testDispatcher.runBlockingTest {

        // When calling getReminders on remindersLocalRepository
        val reminders = remindersLocalRepository.getReminders()

        // Then it must succeed and have empty reminders list
        assertThat(reminders, instanceOf(Result.Success::class.java))
        val result = (reminders as Result.Success).data
        assertThat(result.isEmpty(), `is`(true))
    }

    @Test
    fun getReminderById_notExist_returnsResultWithError() = testDispatcher.runBlockingTest {

        // When trying to access reminder that doesn't exist
        val result = remindersLocalRepository.getReminder("random_id")

        // Then result must be instance of Result.Error
        assertThat(result, instanceOf(Result.Error::class.java))
    }

    @Test
    fun getReminderById_reminderExists_returnsResultSuccessWithReminder() =
        testDispatcher.runBlockingTest {

            // Adding reminder to remindersLocalRepository
            val reminder = ReminderDTO("test", "test", "test", null, null)
            remindersLocalRepository.saveReminder(reminder)

            // When attempting to retrieve reminder from repo
            val result = remindersLocalRepository.getReminder(reminder.id)

            // Then value of result must be success and must contain a reminder
            assertThat(result, instanceOf(Result.Success::class.java))
            val retrievedReminder = (result as Result.Success).data
            assertThat(retrievedReminder.title, `is`(reminder.title))
            assertThat(retrievedReminder.id, `is`(reminder.id))
        }

    @Test
    fun deleteAllReminders_ActullyDeleteAllRemindersInRepository() =
        testDispatcher.runBlockingTest {

            // Given a repository with 33 reminder in it
            repeat(33) {
                val reminder = ReminderDTO("test", "test", "test", null, null)
                remindersLocalRepository.saveReminder(reminder)
            }
            val result1 = remindersLocalRepository.getReminders()
            assertThat(result1, instanceOf(Result.Success::class.java))
            val reminders = (result1 as Result.Success).data
            assertThat(reminders.size, `is`(33))

            // When calling deleteAllReminders on repo
            remindersLocalRepository.deleteAllReminders()

            // Then getReminders must succeed and return empty list
            val result2 = remindersLocalRepository.getReminders()
            assertThat(result2, instanceOf(Result.Success::class.java))
            val newList = (result2 as Result.Success).data
            assertThat(newList.isEmpty(), `is`(true))
        }
}