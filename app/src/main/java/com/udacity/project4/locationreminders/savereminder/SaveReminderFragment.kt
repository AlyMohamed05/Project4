package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.checkBackgroundLocationPermission
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val backgroundPermissionRequestHandler = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkLocationSettingsThenSave()
        } else {
            _viewModel.showSnackBar.value = "background location is required to create geofence"
        }
    }

    private val changeLocationSettingsHandler = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            saveReminder()
        }
    }

    private lateinit var geofencingClient: GeofencingClient
    private val geofencingPendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            requireContext(),
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @SuppressLint("InlinedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            if (!checkBackgroundLocationPermission(requireContext())) {
                backgroundPermissionRequestHandler.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return@setOnClickListener
            }
            checkLocationSettingsThenSave()
        }
        geofencingClient = LocationServices.getGeofencingClient(requireContext())
    }

    private fun saveReminder() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        val reminderItem = ReminderDataItem(
            title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude
        )
        val saved = _viewModel.validateAndSaveReminder(reminderItem)
        if (saved) {
            createGeofence(
                reminderItem.id,
                latitude!!,
                longitude!!
            )
        }
    }

    /**
     * It checks if needed location settings is available and in case it is,
     * it will save the reminder and create a geofence.
     */
    private fun checkLocationSettingsThenSave() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 50000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val locationSettingsClient = LocationServices.getSettingsClient(requireContext())
        val task = locationSettingsClient.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            saveReminder()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                changeLocationSettingsHandler.launch(
                    IntentSenderRequest.Builder(exception.resolution).build()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGeofence(
        geofenceId: String,
        latitude: Double,
        longitude: Double
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latitude, longitude, 150f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        geofencingClient.addGeofences(
            geofenceRequest,
            geofencingPendingIntent
        ).run {
            addOnSuccessListener {
                Log.d(TAG, "Added geofence")
            }
            addOnFailureListener {
                Log.d(TAG, "Failed to add geofence")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val TAG = "SaveReminderFragment"
    }
}