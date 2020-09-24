package com.vinu.uber

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class DriverActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var requestListView: ListView? = null
    var requestList: ArrayList<Double> = ArrayList() //users who requested an uber will be logged here
    var requests: ArrayList<DataSnapshot> = ArrayList() //to retrieve data from the Firebase Database of the request that's clicked on


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        requestListView = findViewById(R.id.listView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, requestList)
        requestListView?.adapter = adapter

        /** load all requests **/
        FirebaseDatabase.getInstance().getReference().child("uberRequests")
                .addChildEventListener(object :
                        ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val latitude = snapshot.child("latitude").value as Double //within the 'snaps' tab of Firebase Database, we retrieve the lat list of requests
                        val longitude = snapshot.child("longitude").value as Double //within the 'snaps' tab of Firebase Database, we retrieve the lat list of requests

                        requestList.add(latitude) //TODO - add longitude
                        requests.add(snapshot) //to store data of the request that was clicked on, from the Firebase Database
                        adapter.notifyDataSetChanged()
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        //not needed since we aren't updating data
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) { /** to update the UI after the snaps are deleted - see onBackPressed() of OpenSnapActivity **/
                    var index = 0
                        for(snap: DataSnapshot in requests) { //looping through to help find the index. This will be used to delete the entry in 'requestsList' and 'requests' ArrayList
                            if (snap.key == snapshot?.key) {
                                requestList.removeAt(index)
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