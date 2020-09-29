package com.vinu.uber

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null

    var riderLatitude: Double? = null
    var riderLongitude: Double? = null

    var driverLatitude: Double? = null
    var driverLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setTitle("Driver's Map")

        riderLatitude = intent.getDoubleExtra("riderLatitude", 0.0)
        riderLongitude = intent.getDoubleExtra("riderLongitude", 0.0)

        driverLatitude = intent.getDoubleExtra("driverLatitude", 0.0)
        driverLongitude = intent.getDoubleExtra("driverLongitude", 0.0)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Clears previous map
        mMap.clear()

        /** Set map for Rider **/
        val riderLocation = Location(LocationManager.GPS_PROVIDER) //creates NEW EMPTY location
        riderLocation.latitude = riderLatitude!! //adds latitude to the empty location!
        riderLocation.longitude = riderLongitude!! //adds longitude to the empty location!
        setLocation(riderLocation, true)

        /** Set map for Driver **/
        val driverLocation = Location(LocationManager.GPS_PROVIDER) //creates NEW EMPTY location
        driverLocation.latitude = driverLatitude!! //adds latitude to the empty location!
        driverLocation.longitude = driverLongitude!! //adds longitude to the empty location!
        setLocation(driverLocation, false)

        /** Move camera to include both markers **/
        val riderAndDriver = LatLngBounds(LatLng(riderLatitude!!, riderLongitude!!), LatLng(driverLatitude!!, driverLongitude!!))
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(riderAndDriver, 200))

    }

    fun setLocation(location: Location?, riderActive: Boolean?) {
        if (location != null) {

            // Change map type
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

            // Add a marker at USER'S LOCATION and move the camera to it!
            val userLocation = LatLng(location.latitude, location.longitude)

            if (riderActive!!) {
                mMap.addMarker(MarkerOptions().position(userLocation).title("Rider's location"))
            } else {
                mMap.addMarker(MarkerOptions().position(userLocation).title("Driver's location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
            }

//            // newLatLngZoom to zoom into map - from 1 to 20, where 1 is totally zoomed out and 20 is totally zoomed in
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
        }
    }

    /** Accept Uber request and Navigate to Google Maps **/
    fun acceptRequest(view: View?) {
        val riderID = intent.getStringExtra("riderID")

        //amend uberRequest tab in Firebase Database
        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(riderID).child("driverAccepted").setValue(true)

        val uri = "http://maps.google.com/maps?saddr=" + driverLatitude + "," + driverLongitude + "&daddr=" + riderLatitude + "," + riderLongitude
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

}