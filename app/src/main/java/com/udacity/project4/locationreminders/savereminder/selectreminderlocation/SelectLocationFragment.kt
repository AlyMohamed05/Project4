package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.fadeIn
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val foregroundLocationRequestHandler = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { handleForegroundLocationPermissionResponse(it) }

    private val backgroundLocationPermissionHandler = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { handleBackgroundLocationPermissionResponse(it) }

    private val changeLocationSettingHandler = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showUserLocation()
        }
    }

    private var selectedLocationMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        showUserLocation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        binding.selectLocationFab.setOnClickListener { onLocationSelected() }
    }

    private fun onLocationSelected() {
        val selectedLocation = _viewModel.selectedPOI.value!!
        _viewModel.latitude.value = selectedLocation.latLng.latitude
        _viewModel.longitude.value = selectedLocation.latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = selectedLocation.name
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setCustomMapStyle()
        map.isMyLocationEnabled = true
        map.setOnPoiClickListener(this)
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        _viewModel.selectedPOI.value = pointOfInterest
        setMapMarker(pointOfInterest.latLng)
        // fade select location fab in if not visible
        val selectLocationFab = binding.selectLocationFab
        if (selectLocationFab.visibility != View.VISIBLE) {
            selectLocationFab.fadeIn()
        }
    }

    private fun setCustomMapStyle() {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.google_map_style)
            )
        } catch (exception: Resources.NotFoundException) {
            Log.d(TAG, "Couldn't find map style")
        }
    }

    private fun showUserLocation() {
        if (!hasLocationPermissions()) {
            return
        }
        requestLocationAndUpdate()
    }

    private fun setMapMarker(location: LatLng) {
        // First remove the current marker because there is only one position selected
        selectedLocationMarker?.remove()
        selectedLocationMarker = map.addMarker(
            MarkerOptions()
                .position(location)
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationAndUpdate() {
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
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(location: LocationResult?) {
                    location ?: return
                    val lat = location.lastLocation.latitude
                    val lng = location.lastLocation.longitude
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(lat, lng)
                        )
                    )
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                changeLocationSettingHandler.launch(
                    IntentSenderRequest.Builder(exception.resolution).build()
                )
            }
        }
    }

    /**
     * Checks for Needed permissions (Fine location and Background location),
     * if not granted it will send permission requests.
     * @return true if all needed location permissions are granted, else false.
     */
    @SuppressLint("InlinedApi")
    // No need for warning the background location will not be requested on low apis
    private fun hasLocationPermissions(): Boolean {
        val hasForegroundLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasBackgroundLocationPermission =
            if (Build.VERSION.SDK_INT >= 29) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                hasPermission == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        if (!hasForegroundLocationPermission) {
            foregroundLocationRequestHandler.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return false
        }
        if (!hasBackgroundLocationPermission) {
            backgroundLocationPermissionHandler.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return false
        }
        return true
    }

    private fun handleForegroundLocationPermissionResponse(granted: Boolean) {
        if (!granted) {
            _viewModel.showSnackBar.value = "Location access required"
        } else {
            showUserLocation()
        }
    }

    private fun handleBackgroundLocationPermissionResponse(granted: Boolean) {
        if (!granted) {
            _viewModel.showSnackBar.value = "Background location permission required"
        } else {
            showUserLocation()
        }
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
    }
}
