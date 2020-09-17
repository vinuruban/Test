package com.vinu.uber

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.service.autofill.SaveCallback
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class RiderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null

    var counter = 0 //to stop the address string from re rendering on Maps
    var callUberButton: Button? = null
    var uberRequestActive = false //to track whether an uber was called or not


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        callUberButton = findViewById<View>(R.id.callUberButton) as Button
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

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateMap(location)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
            override fun onProviderEnabled(s: String) {}
            override fun onProviderDisabled(s: String) {}
        }

        //CODE BELOW IS THE POP UP WHICH ASKS FOR THE LOCATION PERMISSION WHEN THE APP STARTS, USERS CAN CHOOSE TO ACCEPT/DENY REQUEST

        //CODE BELOW IS THE POP UP WHICH ASKS FOR THE LOCATION PERMISSION WHEN THE APP STARTS, USERS CAN CHOOSE TO ACCEPT/DENY REQUEST
        if (Build.VERSION.SDK_INT < 23) { //IF API < 23, PROVIDE LOCATION and we won't need to manually ask for permission
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //IF PERMISSION WASNT GRANTED, ASK FOR PERMISSION
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else { //IF PERMISSION IS GRANTED, PROVIDE LOCATION
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                //BELOW GETS LAST KNOWN LOCATION AT APP START
                val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastKnownLocation?.let { updateMap(it) }
            }
        }

    }

    /** to avoid repetition of code  */
    fun updateMap(location: Location) {

        // Clears previous map
        mMap.clear()

        // Change map type
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Add a marker at USER'S LOCATION and move the camera to it!
        val userLocation = LatLng(location.latitude, location.longitude)

        // icon(... added to change the colour of the marker
        mMap.addMarker(MarkerOptions().position(userLocation).title("You're here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

        // newLatLngZoom to zoom into map - from 1 to 20, where 1 is totally zoomed out and 20 is totally zoomed in
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))

        //CODE BELOW CONVERTS THE LAT AND LONG TO ACTUAL ADDRESS
        if (counter == 0) {
            val geocoder = Geocoder(applicationContext, Locale.getDefault()) //Locale.getDefault() gets address from the specific country the phone is in
            try {
                val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                //Address[addressLines=[0:"398 Alexandra Ave, Rayners Lane, Harrow HA2 9UF, UK"],feature=398,admin=England,sub-admin=Greater London,locality=null,thoroughfare=Alexandra Avenue,postalCode=HA2 9UF,countryCode=GB,countryName=United Kingdom,hasLatitude=true,latitude=51.572310699999996,hasLongitude=true,longitude=-0.3708033,phone=null,url=null,extras=null]

                // ^ ^ code below will retrieve data from here. We will use getAddressLine(0) since that will simply get the full address
                // ^ ^ e.g. we can do getFeatureName(), getAdminArea(), getThoroughFare(), etc. instead of getAddressLine(0) to get the specifics
                var address: String? = ""
                if (addressList != null && addressList.size > 0) {
                    if (addressList[0].getAddressLine(0) != null) {
                        address = addressList[0].getAddressLine(0)
                        Toast.makeText(applicationContext, address, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            counter = 1
        }
    }

    /** WHEN THE USERS ACCEPT/DENY LOCATION REQUEST, THE CODE BELOW IS EXECUTED  */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //IF PERMISSION WAS GRANTED
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                    val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    updateMap(lastKnownLocation)
                }
            }
        }
    }

    /** Call An Uber  */
    fun callUber(view: View?) {
        Toast.makeText(applicationContext, "works", Toast.LENGTH_SHORT).show()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) { //if there exists a location, send request

                val userID = intent.getStringExtra("userID")
                val uniqueRequestID = UUID.randomUUID().toString()

                //pass in user
                FirebaseDatabase.getInstance().getReference().child("uberRequests").child(uniqueRequestID).child("user").setValue(userID)

                //pass in location
                FirebaseDatabase.getInstance().getReference().child("uberRequests").child(uniqueRequestID).child("latitude").setValue(lastKnownLocation.latitude)
                FirebaseDatabase.getInstance().getReference().child("uberRequests").child(uniqueRequestID).child("longitude").setValue(lastKnownLocation.longitude)

            } else {
                Toast.makeText(applicationContext, "Could not find location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}