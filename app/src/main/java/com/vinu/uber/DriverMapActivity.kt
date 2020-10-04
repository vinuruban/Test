package com.vinu.uber

import android.content.Intent
import android.location.Location
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
import com.google.firebase.database.core.utilities.Utilities


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    /** No need for the bottom since we don't need to find location in this activity. Instead, it is passed from the DriverActivity via Intent**/
//    var locationManager: LocationManager? = null
//    var locationListener: LocationListener? = null

    var riderLatitude: Double? = null
    var riderLongitude: Double? = null
    var riderID: String? = null

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

        val locationAsString = intent.getStringExtra("latLngID")
        riderLatitude = (locationAsString.split(";")[0]+"").toDouble() //within the 'uberRequest' tab of Firebase Database, we retrieve the lat list of requests
        riderLongitude = (locationAsString.split(";")[1]+"").toDouble() //also feasible with deserializer
        riderID = locationAsString.split(";")[2]+"" /** needed in acceptRequest() **/

        driverLatitude = intent.getDoubleExtra("driverLatitude", 0.0)
        driverLongitude = intent.getDoubleExtra("driverLongitude", 0.0)
    }

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
        var riderAndDriver: LatLngBounds?

        if (riderLatitude!! > driverLatitude!!) { /** To avoid the following: "java.lang.IllegalArgumentException: southern latitude exceeds northern latitude (51.574614999999994 > 51.57376166666666)" **/
            riderAndDriver = LatLngBounds(LatLng(driverLatitude!!, driverLongitude!!), LatLng(riderLatitude!!, riderLongitude!!))
        } else {
            riderAndDriver = LatLngBounds(LatLng(riderLatitude!!, riderLongitude!!), LatLng(driverLatitude!!, driverLongitude!!))
        }

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = (width * 0.12).toInt() // offset from edges of the map 12% of screen

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(riderAndDriver, width, height, padding))


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
                mMap.addMarker(MarkerOptions().position(userLocation).title("You're here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)))
            }
        }
    }

    /** Accept Uber request and Navigate to Google Maps **/
    fun acceptRequest(view: View?) {

        //pass driver's location in uberRequest - this will also alert the rider and change the map view in RiderActivity.kt
        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(riderID!!).child("driverLocation").child("latLng").setValue("$driverLatitude;$driverLongitude;")

        val uri = "http://maps.google.com/maps?saddr=" + driverLatitude + "," + driverLongitude + "&daddr=" + riderLatitude + "," + riderLongitude
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

}