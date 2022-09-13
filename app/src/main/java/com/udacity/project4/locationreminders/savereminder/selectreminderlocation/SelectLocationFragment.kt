package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.checkLocationPermission
import com.udacity.project4.utils.fadeIn
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, GoogleMap.OnPoiClickListener,
    GoogleMap.OnMapLongClickListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    @SuppressLint("MissingPermission")
    private val locationPermissionRequestHandler = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            map.isMyLocationEnabled = true
        } else {
            _viewModel.showToast.value = "Permission required"
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setCustomMapStyle()
        if (checkLocationPermission(requireContext())) {
            map.isMyLocationEnabled = true
        } else {
            locationPermissionRequestHandler.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        map.setOnPoiClickListener(this)
        map.setOnMapLongClickListener(this)
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        selectLocation(pointOfInterest)
    }

    override fun onMapLongClick(location: LatLng) {
        val pointOfInterest = PointOfInterest(
            LatLng(location.latitude, location.longitude),
            "custom Location",
            "custom location"
        )
        selectLocation(pointOfInterest)
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

    private fun setMapMarker(location: LatLng) {
        // First remove the current marker because there is only one position selected
        selectedLocationMarker?.remove()
        selectedLocationMarker = map.addMarker(
            MarkerOptions()
                .position(location)
        )
    }

    private fun selectLocation(pointOfInterest: PointOfInterest) {
        _viewModel.selectedPOI.value = pointOfInterest
        setMapMarker(pointOfInterest.latLng)
        // fade select location fab in if not visible
        val selectLocationFab = binding.selectLocationFab
        if (selectLocationFab.visibility != View.VISIBLE) {
            selectLocationFab.fadeIn()
        }
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
    }
}
