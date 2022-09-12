package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutor = InstantTaskExecutorRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Test
    fun `validateAndSaveReminder() returns false if reminder is not valid`() {

        // Given a SaveReminderViewModel and fake data source
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        // When trying to add invalid reminder
        val invalidReminder = ReminderDataItem(
            "",
            "test",
            "test",
            null,
            null,
            "testId"
        )
        val addedReminder = saveReminderViewModel.validateAndSaveReminder(invalidReminder)

        // Then reminder must not be added
        assertFalse(addedReminder)
    }

    @Test
    fun `validateEnteredData return true when reminder is valid`() {

        // Given a SaveReminderViewModel and fake data source
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        // When trying to add valid reminder
        val validReminder = ReminderDataItem(
            "test",
            "test",
            "test",
            null,
            null,
            "testId"
        )

        // Then reminder must be valid and value must be true
        val valid = saveReminderViewModel.validateEnteredData(validReminder)
        assertTrue(valid)
    }

    @Test
    fun `validateEnteredData return false when reminder is not valid`() {

        // Given a SaveReminderViewModel and fake data source
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        // When trying to add invalid reminder
        val invalidReminder = ReminderDataItem(
            "",
            "test",
            "test",
            null,
            null,
            "testId"
        )

        // Then reminder cannot be valid
        val valid = saveReminderViewModel.validateEnteredData(invalidReminder)
        assertFalse(valid)
    }

    @Test
    fun `saving a valid reminder adds the reminder to the data source`() {

        // Given a SaveReminderViewModel and fake data source
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        mainCoroutineRule.runBlockingTest {
            // When trying to add valid reminder
            val validReminder = ReminderDataItem(
                "test",
                "test",
                "test",
                null,
                null,
                "testId"
            )
            val valid = saveReminderViewModel.validateAndSaveReminder(validReminder)
            assertTrue(valid)

            // Then data source must be updated
            mainCoroutineRule
            val reminders = fakeDataSource.getReminders() as Result.Success
            assertEquals(1, reminders.data.size)
        }

    }

    @Test
    fun `onClear sets all livedata to null`() {

        // Given a SaveReminderViewModel
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        // When setting values to mutable live data the calling onClear()
        saveReminderViewModel.reminderTitle.value = "test"
        saveReminderViewModel.reminderDescription.value = "test"
        saveReminderViewModel.reminderSelectedLocationStr.value = "test"
        saveReminderViewModel.latitude.value = 0.0
        saveReminderViewModel.onClear()

        // Livedata values must be null
        assertNull(saveReminderViewModel.reminderTitle.value)
        assertNull(saveReminderViewModel.reminderDescription.value)
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNull(saveReminderViewModel.latitude.value)
    }
}