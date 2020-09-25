package com.vinu.uber

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class DriverActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var requestListView: ListView? = null

    var distanceList: ArrayList<String> = ArrayList() //users who requested an uber will be logged here
    var requests: ArrayList<DataSnapshot> = ArrayList() //to retrieve data from the Firebase Database of the request that's clicked on

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null

    //driver's location
    var driverLatitude: Double? = null
    var driverLongitude: Double? = null

    //rider's location
    var riderLatitude: Double? = null
    var riderLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        /** set location manager and listener **/
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateListView(location)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
            override fun onProviderEnabled(s: String) {}
            override fun onProviderDisabled(s: String) {}
        }

        /** CODE BELOW IS THE POP UP WHICH ASKS FOR THE LOCATION PERMISSION WHEN THE APP STARTS, USERS CAN CHOOSE TO ACCEPT/DENY REQUEST **/
        if (Build.VERSION.SDK_INT < 23) { //IF API < 23, PROVIDE LOCATION and we won't need to manually ask for permission
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //IF PERMISSION WASNT GRANTED, ASK FOR PERMISSION
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else { //IF PERMISSION IS GRANTED, PROVIDE LOCATION
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                //BELOW GETS LAST KNOWN LOCATION AT APP START
                val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                driverLatitude = lastKnownLocation.latitude //TODO - not picked up
                driverLongitude = lastKnownLocation.longitude //TODO - not picked up
            }
        }

        requestListView = findViewById(R.id.listView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, distanceList)
        requestListView?.adapter = adapter

        /** load all requests **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests")
                .addChildEventListener(object :
                        ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        riderLatitude = snapshot.child("latitude").value as Double //within the 'snaps' tab of Firebase Database, we retrieve the lat list of requests
                        riderLongitude = snapshot.child("longitude").value as Double //within the 'snaps' tab of Firebase Database, we retrieve the lat list of requests //TODO - cannot cancel uber sometimes


                        Log.i("sdfsdf", "$riderLatitude : $riderLongitude : $driverLatitude : $driverLongitude")
                        Toast.makeText(applicationContext, "sdfdsf" + riderLatitude + " : " + riderLongitude + " : " + driverLatitude + " : " + driverLongitude, Toast.LENGTH_SHORT).show()



                        distanceList.add(distance(riderLatitude!!, riderLongitude!!, driverLatitude!!, driverLongitude!!, "K").toString() + " K") //TODO - add longitude
                        requests.add(snapshot) //to store data of the request that was clicked on, from the Firebase Database
                        adapter.notifyDataSetChanged()
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        //not needed since we aren't updating data
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        /** to update the UI after the snaps are deleted - see onBackPressed() of OpenSnapActivity **/
                        var index = 0
                        for (snap: DataSnapshot in requests) { //looping through to help find the index. This will be used to delete the entry in 'requestsList' and 'requests' ArrayList
                            if (snap.key == snapshot?.key) {
                                distanceList.removeAt(index)
                                requests.removeAt(index)
                            }
                            index++
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                        //not needed
                    }

                    override fun onCancelled(error: DatabaseError) {
                        //not needed
                    }

                })


        /** When clicking on the request **/
        requestListView?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val snapshot = requests.get(position) //get data of the request that was clicked on

//            var intent = Intent(this, OpenSnapActivity::class.java)
//
//            intent.putExtra("uniqueImageName", snapshot.child("imageName").value as String)
//            intent.putExtra("caption", snapshot.child("caption").value as String)
//            intent.putExtra("snapUUID", snapshot.key) //needed to help delete the snap after viewing it
//
//            startActivity(intent)

            Toast.makeText(applicationContext, "Clicked on the request", Toast.LENGTH_SHORT).show()

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
                    driverLatitude = lastKnownLocation.latitude //TODO - not picked up
                    driverLongitude = lastKnownLocation.longitude //TODO - not picked up
                }
            }
        }
    }

    fun updateListView(location: Location) {
//        requestList.clear() //clear previous requests
//        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, requestList)
//        requestListView?.adapter = adapter
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

//    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double, el1: Double, el2: Double): Double {
//        val R = 6371 // Radius of the earth
//        val latDistance = Math.toRadians(lat2 - lat1)
//        val lonDistance = Math.toRadians(lon2 - lon1)
//        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
//                + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)))
//        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
//        var distance = R * c * 1000 // convert to meters
//        val height = el1 - el2
//        distance = Math.pow(distance, 2.0) + Math.pow(height, 2.0)
//        return Math.sqrt(distance)
//    }

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double, unit: String): Double {
        return if (lat1 == lat2 && lon1 == lon2) {
            0.0
        } else {
            val theta = lon1 - lon2
            var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta))
            dist = Math.acos(dist)
            dist = Math.toDegrees(dist)
            dist = dist * 60 * 1.1515
            if (unit == "K") {
                dist = dist * 1.609344
            } else if (unit == "N") {
                dist = dist * 0.8684
            }
            dist
        }
    }


}