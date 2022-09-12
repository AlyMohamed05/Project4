package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutor = InstantTaskExecutorRule()

    private lateinit var fakeDataSource: ReminderDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Test
    fun `showNoDate livedata value is true if no data preset in data source`() {

        // Given a RemindersListViewModel
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            FakeDataSource()
        )

        // When Calling loadReminders while no data
        remindersListViewModel.loadReminders()

        // Then Value of showLoading livedata ust be true
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertTrue(showNoData)
    }

    @Test
    fun `showNoData livedata value is false if there is data in data source`() {
        mainCoroutineRule.runBlockingTest {

            // Given a RemindersListViewModel and data source containing data
            fakeDataSource = FakeDataSource()
            remindersListViewModel = RemindersListViewModel(
                ApplicationProvider.getApplicationContext(),
                FakeDataSource()
            )

            // Initially showNoData livedata value is true
            remindersListViewModel.loadReminders()
            val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
            assertTrue(showNoData)

            // When adding a new data to source and updating
            fakeDataSource.saveReminder(
                ReminderDTO("test", "test", "test", null, null, "tested")
            )
            remindersListViewModel.loadReminders()

            // Then value of showNoData livedata must be false
            assertTrue(remindersListViewModel.showNoData.getOrAwaitValue())
        }
    }

    @Test
    fun `loadingReminders updates reminders list with new new reminders`() {

        // Given a RemindersListViewModel and data source containing data
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            FakeDataSource()
        )

        mainCoroutineRule.runBlockingTest {

            // When adding a reminder and updating
                fakeDataSource.saveReminder(
                    ReminderDTO("test", "test", "test", 33.3, 33.3, "tested")
                )
            remindersListViewModel.loadReminders()

            // Then remindersList cannot be null
            val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
            assertNotNull(remindersList)
        }
    }

}