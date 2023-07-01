package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
    private val database: SleepDatabaseDao,
    application: Application) : AndroidViewModel(application) {

    private var tonight = MutableLiveData<SleepNight?>()

    val nights = database.getAllNights()

    val startButtonVisible = tonight.map{
        null == it
    }
    val stopButtonVisible = tonight.map{
        null != it
    }
    val clearButtonVisible = nights.map {
        it.isNotEmpty()
    }

    private var _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    /**
     * Converted nights to Spanned for displaying.
     */
    val nightsString = nights.map { nights ->
        formatNights(nights, application.resources)
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()

    val navigateToSleepQuality: LiveData<SleepNight?>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    /**
     *  Handling the case of the stopped app or forgotten recording,
     *  the start and end times will be the same.j
     *
     *  If the start time and end time are not the same, then we do not have an unfinished
     *  recording.
     */
    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    private suspend fun clear() {
        database.clear()
    }

    private suspend fun update(night: SleepNight) {
        database.update(night)
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
    }

    /**
     * Executes when the START button is clicked.
     */
    fun onStartTracking() {
        viewModelScope.launch {
            // Create a new night, which captures the current time,
            // and insert it into the database.
            val newNight = SleepNight()

            insert(newNight)

            tonight.value = getTonightFromDatabase()
        }
    }

    /**
     * Executes when the STOP button is clicked.
     */
    fun onStopTracking() {
        viewModelScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch(),
            // not the lambda.
            val oldNight = tonight.value ?: return@launch

            // Update the night in the database to add the end time.
            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }

    /**
     * Executes when the CLEAR button is clicked.
     */
    fun onClear() {
        viewModelScope.launch {
            // Clear the database table.
            clear()
            // And clear tonight since it's no longer in the database
            tonight.value = null
            _showSnackbarEvent.value = true
        }
    }
}