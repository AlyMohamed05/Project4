package com.udacity.project4.locationreminders.reminderslist

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            modules(
                module {
                    single<ReminderDataSource> {
                        FakeDataSource()
                    }
                    viewModel {
                        RemindersListViewModel(
                            get(),
                            get() as ReminderDataSource
                        )
                    }
                }
            )
        }
    }

    @Test
    fun createFabButton_onClicked_navigatesToSaveRemindersFragment() {

        // Given a RemindersListFragment with mocked navController
        val navController = mock(NavController::class.java)
        val remindersListFragment = launchFragmentInContainer<ReminderListFragment>(
            themeResId = R.style.AppTheme
        )
        remindersListFragment.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        // When clicking create fab button
        onView(
            withId(R.id.addReminderFAB)
        ).perform(click())

        // Then nav controller must navigate to saveRemindersFragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun noDataIndicatorAppearsOnEmptyRepository() {

        // Given an empty data source and remindersListFragment
        launchFragmentInContainer<ReminderListFragment>(
            themeResId = R.style.AppTheme
        )

        // Then No Data indicator must appear
        onView(
            withId(R.id.noDataTextView)
        ).check(matches(isDisplayed()))
    }

    @Test
    fun repositoryContainsData_appearsOnScreen() {
        runBlocking {

            // Given a repository with reminders inside
            val reminder = ReminderDTO("test", "test", "location1", null, null)
            val remindersDataSource = get<ReminderDataSource>()
            remindersDataSource.saveReminder(reminder)
            launchFragmentInContainer<ReminderListFragment>(
                themeResId = R.style.AppTheme
            )

            Thread.sleep(800L)

            // Then no data indicator must be invisible
            onView(
                withText("location1")
            ).check(matches(isDisplayed()))
        }
    }

    @Test
    fun shouldReturnError_isTrue_ErrorMessageIsShown() {
        runBlocking {

            // When Reminders repository return an error
            val remindersDataSource = get<ReminderDataSource>() as FakeDataSource
            remindersDataSource.setShouldReturnError(true)
            launchFragmentInContainer<ReminderListFragment>(
                themeResId = R.style.AppTheme
            )

            // Then Error Message should appear
            onView(
                withText("ERROR_TEXT")
            ).check(matches(isDisplayed()))
        }
    }

}