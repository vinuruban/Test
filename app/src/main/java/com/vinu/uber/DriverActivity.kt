package com.vinu.uber

import android.Manifest
import android.content.Intent
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
import kotlin.properties.Delegates

class DriverActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var requestListView: ListView? = null

    var distanceList: ArrayList<String> = ArrayList() //users who requested an uber will be logged here
    var adapter: ArrayAdapter<String>? = null
    var requests: ArrayList<DataSnapshot> = ArrayList() //to retrieve data from the Firebase Database of the request that's clicked on

    /** although map isn't used, we still need to get location of Rider and Driver to calculate distance. Thus, the below is needed **/
    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null /** needed when location changes **/

    //driver's location
    var driverLatitude: Double? = 0.0
    var driverLongitude: Double? = 0.0

    //rider's location
    var riderLatitude: Double? = 0.0
    var riderLongitude: Double? = 0.0

    var riderID: String? = null

    var mSnapshot: DataSnapshot? = null

    var requestAcceptedStatus: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        requestListView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, distanceList)
        requestListView?.adapter = adapter

        /** set location manager and listener **/
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener { /** what to do when location changes **/
            override fun onLocationChanged(location: Location) {
            if(location.latitude != driverLatitude) {
                if (location.longitude != driverLongitude) {
                    distanceList.clear() //clear previous data

                    driverLatitude = location.latitude //gets driver's location
                    driverLongitude = location.longitude //gets driver's location

                    /************ Once both driver and rider locations are found, calculate distance! ************/
                    distanceList.add(distance(riderLatitude!!, riderLongitude!!, driverLatitude!!, driverLongitude!!, "K").toString() + "km") //calculates distance between rider and driver
                    requests.add(mSnapshot!!) //to store data of the request that was clicked on, from the Firebase Database
                    adapter!!.notifyDataSetChanged()
                    /** to update listview with the new driverLatitude & driverLongitude **/

                    /** after the request is accepted and when driverLocation changes, this will update it in Firebase database to help refresh rider's map **/
                    FirebaseDatabase.getInstance().getReference().child("uberRequests").child(riderID!!).child("driverLocation")
                            .addChildEventListener(object :
                                    ChildEventListener {
                                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                                    if (snapshot.exists()) {
                                        FirebaseDatabase.getInstance().getReference().child("uberRequests").child(riderID!!).child("driverLocation").child("latLng").setValue("$driverLatitude;$driverLongitude;")
                                    }
                                }
                                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                                override fun onChildRemoved(snapshot: DataSnapshot) {}
                                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                                override fun onCancelled(error: DatabaseError) {}

                            })

                }
            }
        }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
            override fun onProviderEnabled(s: String) {}
            override fun onProviderDisabled(s: String) {}
        }

        /** load all locations **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests")
                .addChildEventListener(object :
                        ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) { /** when uber request is made **/

                        Log.i("snapshot of riderLocation", snapshot.value.toString())

                        mSnapshot = snapshot

                        /************ GET RIDER'S LOCATION ************/
                        val locationAsString = snapshot.child("riderLocation").child("latLngID").value as String
                        riderLatitude = (locationAsString.split(";")[0]+"").toDouble() //within the 'uberRequest' tab of Firebase Database, we retrieve the lat list of requests
                        riderLongitude = (locationAsString.split(";")[1]+"").toDouble() //also feasible with deserializer
                        riderID = locationAsString.split(";")[2]+"" /** needed in acceptRequest() **/

                        /************ GET DRIVERS'S LOCATION ************/
                        // CODE BELOW IS THE POP UP WHICH ASKS FOR THE LOCATION PERMISSION WHEN THE APP STARTS, USERS CAN CHOOSE TO ACCEPT/DENY REQUEST
                        if (Build.VERSION.SDK_INT < 23) { //IF API < 23, PROVIDE LOCATION and we won't need to manually ask for permission
                            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                        } else {
                            if (ContextCompat.checkSelfPermission(this@DriverActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //IF PERMISSION WASNT GRANTED, ASK FOR PERMISSION
                                requests.add(snapshot) /** snapshot passed in here instead since it isn't accessible outside **/
                                ActivityCompat.requestPermissions(this@DriverActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1) /** this triggers the onRequestPermissionsResult() **/
                            } else { //IF PERMISSION IS GRANTED, PROVIDE LOCATION
                                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                                //BELOW GETS LAST KNOWN LOCATION AT APP START
                                val lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                driverLatitude = lastKnownLocation.latitude //gets driver's location
                                driverLongitude = lastKnownLocation.longitude //gets driver's location

                                /************ Once both driver and rider locations are found, calculate distance! ************/
                                distanceList.add(distance(riderLatitude!!, riderLongitude!!, driverLatitude!!, driverLongitude!!, "K").toString() + "km") //calculates distance between rider and driver
                                requests.add(snapshot) //to store data of the request that was clicked on, from the Firebase Database
                                adapter!!.notifyDataSetChanged() /** to update listview with the new driverLatitude & driverLongitude **/
                            }
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        //not needed since we aren't updating data
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        /** to update the UI after the uber request gets cancelled **/
                        var index = 0
                        for (snap: DataSnapshot in requests) { //looping through to help find the index. This will be used to delete the entry in 'requestsList' and 'requests' ArrayList
                            if (snap.key == snapshot?.key) {
                                distanceList.removeAt(index)
                                requests.removeAt(index)
                            }
                            index++
                        }
                        adapter!!.notifyDataSetChanged()
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

            Log.i("snapshot of uberRequests", snapshot.value.toString())

            var intent = Intent(this, DriverMapActivity::class.java)

            //rider's location
            intent.putExtra("latLngID", snapshot.child("riderLocation").child("latLngID").value as String)

            //driver's location
            intent.putExtra("driverLatitude", driverLatitude)
            intent.putExtra("driverLongitude", driverLongitude)

            startActivity(intent)
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
                    driverLatitude = lastKnownLocation.latitude
                    driverLongitude = lastKnownLocation.longitude

                    /************ Once both driver and rider locations are found, calculate distance! ************/
                    distanceList.add(distance(riderLatitude!!, riderLongitude!!, driverLatitude!!, driverLongitude!!, "K").toString() + "km") //calculates distance between rider and driver
                    adapter!!.notifyDataSetChanged() /** to update listview with the new driverLatitude & driverLongitude **/
                }
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

    /** calculates distance between rider and driver **/
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
            String.format("%.2f", dist).toDouble() //rounds dist to 2 decimal places
        }
    }


}