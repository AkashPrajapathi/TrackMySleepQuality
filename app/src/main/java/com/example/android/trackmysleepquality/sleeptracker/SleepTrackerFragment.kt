package com.example.android.trackmysleepquality.sleeptracker


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */
class SleepTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_sleep_quality.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sleep_tracker, container, false)

        val application = requireNotNull(this.activity).application

        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao

        val viewModelFactory = SleepTrackerViewModelFactory(dataSource, application)

        val sleepTrackerViewModel =
            ViewModelProvider(
                this, viewModelFactory)[SleepTrackerViewModel::class.java]

        binding.sleepTrackerViewModel = sleepTrackerViewModel

        val adapter = SleepNightAdapter()
        binding.sleepList.adapter = adapter

        sleepTrackerViewModel.nights.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        binding.lifecycleOwner = this

        // Add an Observer on the state variable for Navigating when STOP button is pressed.
        sleepTrackerViewModel.navigateToSleepQuality.observe(viewLifecycleOwner, Observer { night ->
            night?.let {
                // We need to get the navController from this, because button is not ready, and it
                // just has to be a view. For some reason, this only matters if we hit stop again
                // after using the back button, not if we hit stop and choose a quality.
                // Also, in the Navigation Editor, for Quality -> Tracker, check "Inclusive" for
                // popping the stack to get the correct behavior if we press stop multiple times
                // followed by back.
                // Also: https://stackoverflow.com/questions/28929637/difference-and-uses-of-oncreate-oncreateview-and-onactivitycreated-in-fra
                this.findNavController().navigate(
                    SleepTrackerFragmentDirections
                        .actionSleepTrackerFragmentToSleepQualityFragment(night.nightId))
                // Reset state to make sure we only navigate once, even if the device
                // has a configuration change.
                sleepTrackerViewModel.doneNavigating()
            }
        })

        sleepTrackerViewModel.showSnackBarEvent.observe(viewLifecycleOwner, Observer {
            if (it == true) { // Observed state is true.
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    getString(R.string.cleared_message),
                    Snackbar.LENGTH_SHORT // How long to display the message.
                ).show()
                sleepTrackerViewModel.doneShowingSnackbar()
            }
        })

        return binding.root
    }
}