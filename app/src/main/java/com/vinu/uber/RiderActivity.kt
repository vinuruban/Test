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
import android.util.Log
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class RiderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null

    var auth = FirebaseAuth.getInstance() //current user
    var counter = 0 //to stop the address string from re rendering on Maps
    var callUberButton: Button? = null
    var uberRequestActive = false //to track whether an uber was called or not

    //driver's location
    var driverLatitude: Double? = 0.0
    var driverLongitude: Double? = 0.0

    //rider's location
    var riderLatitude: Double? = 0.0
    var riderLongitude: Double? = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setTitle("Rider Map")

        callUberButton = findViewById<View>(R.id.callUberButton) as Button

        /** check if a request was made, and set uberRequestActive status **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(intent.getStringExtra("userID")).child("riderLocation")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) { /** if request exist **/
                        uberRequestActive = true
                        callUberButton?.setText("Cancel Uber")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                }
            })

        /** when driver's location changes, update map (after request is accepted) **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(intent.getStringExtra("userID")).child("driverLocation")
                .addValueEventListener(object : ValueEventListener { //TODO - add location listener in DriverMapActivity.kt and code it to update location in ....child("driverLocation") and wrap it with an if function (if driverLocation exists, then...)
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val riderLocation = Location(LocationManager.GPS_PROVIDER) //creates NEW EMPTY location
                        riderLocation.latitude = riderLatitude!! //adds latitude to the empty location!
                        riderLocation.longitude = riderLongitude!! //adds longitude to the empty location!
                        updateMap(riderLocation)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                    }
                })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) { /** when rider's location changes, update map (before request is accepted) **/
                if(location.latitude != riderLatitude) {
                    if(location.longitude != riderLongitude) {
                        updateMap(location) /** listener is always being triggered when the emulator, so only refresh map if location changes!!! **/
                    }
                }
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
            override fun onProviderEnabled(s: String) {}
            override fun onProviderDisabled(s: String) {}
        }

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

        /** change Map view depending on whether the driver has accepted the Uber request or not **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(auth.currentUser!!.uid).child("driverLocation")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        //rider's location
                        riderLatitude = location.latitude
                        riderLongitude = location.longitude

                        /** if a request wasn't made, or if driver hasn't accepted the Uber request yet... **/
                        if (!dataSnapshot.exists()) {

                            // Add a marker at USER'S LOCATION and move the camera to it!
                            val userLocation = LatLng(riderLatitude!!, riderLongitude!!)

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
                        } else {
                        /** when driver has accepted the Uber request... **/

                            Log.i("driverLocation0", dataSnapshot.value.toString())

                            /** Driver's location **/
                            val locationAsString = dataSnapshot.child("latLng").value as String
                            driverLatitude = (locationAsString.split(";")[0]+"").toDouble() //within the 'uberRequest > driverLocation' tab of Firebase Database, we retrieve the lat list of requests
                            driverLongitude = (locationAsString.split(";")[1]+"").toDouble() //also feasible with deserializer

                            /** Set map for Rider **/
                            val riderLocation = Location(LocationManager.GPS_PROVIDER) //creates NEW EMPTY location
                            riderLocation.latitude = riderLatitude as Double //adds latitude to the empty location!
                            riderLocation.longitude = riderLongitude as Double //adds longitude to the empty location!
                            setLocation(riderLocation, true)

                            /** Set map for Driver **/
                            val driverLocation = Location(LocationManager.GPS_PROVIDER) //creates NEW EMPTY location
                            driverLocation.latitude = driverLatitude as Double //adds latitude to the empty location!
                            driverLocation.longitude = driverLongitude as Double //adds longitude to the empty location!
                            setLocation(driverLocation, false)

                            Log.i("driverLocation", "checkPoint1")


                            /** Move camera to include both markers **/
                            val riderAndDriver = LatLngBounds(LatLng(riderLatitude!!, riderLongitude!!), LatLng(driverLatitude!!, driverLongitude!!))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(riderAndDriver, 200))

                            Log.i("driverLocation", "checkPoint2")


                            /** since request is accepted, rider cannot cancel uber anymore **/
                            callUberButton!!.setText("Your Uber is on its way...")
                            callUberButton!!.setClickable(false)
                            callUberButton!!.setEnabled(false)

                            Log.i("driverLocation", "checkPoint3")

                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                    }
                })
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
        if (uberRequestActive) { //if uber is active, cancel it
            FirebaseDatabase.getInstance().getReference().child("uberRequests").child(auth.currentUser!!.uid).removeValue()
            Toast.makeText(applicationContext, "Uber cancelled", Toast.LENGTH_SHORT).show()
            callUberButton?.setText("Call An Uber")
            uberRequestActive = false
        } else { //else, request it
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) { //if there exists a location, send request

                    //passing as 2 different values didn't work, so passed it together as a String
                    val locationAsString = "${lastKnownLocation.latitude};${lastKnownLocation.longitude};${auth.currentUser!!.uid};" // also feasible with serializer

                    //pass in location
                    FirebaseDatabase.getInstance().getReference().child("uberRequests").child(auth.currentUser!!.uid).child("riderLocation").child("latLngID").setValue(locationAsString)

                    Toast.makeText(applicationContext, "Uber requested", Toast.LENGTH_SHORT).show()

                    callUberButton?.setText("Cancel Uber")
                    uberRequestActive = true

                } else {
                    Toast.makeText(applicationContext, "Could not find location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** set this map type when driver accepts the uber request **/
    fun setLocation(location: Location?, riderActive: Boolean?) {
        if (location != null) {

            // Change map type
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

            // Add a marker at USER'S LOCATION and move the camera to it!
            val userLocation = LatLng(location.latitude, location.longitude)

            if (riderActive!!) {
                mMap.addMarker(MarkerOptions().position(userLocation).title("You're here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
            } else {
                mMap.addMarker(MarkerOptions().position(userLocation).title("Driver's location").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)))
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Toast.makeText(
                applicationContext,
                "Logged out ${auth.currentUser?.uid}",
                Toast.LENGTH_SHORT
        ).show()
        auth.signOut()
    }
}