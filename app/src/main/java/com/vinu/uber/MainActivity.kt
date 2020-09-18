package com.vinu.uber

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var switch: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch = findViewById<View>(R.id.switch1) as Switch

        supportActionBar!!.hide() //full screen

        if (auth.currentUser != null) { //IF USER WAS PREVIOUSLY LOGGED IN...
            Toast.makeText(applicationContext, "User already logged in: " + auth.currentUser!!.uid, Toast.LENGTH_SHORT).show()
            redirectActivity()
        } else { //ELSE, SIGN THEM UP...

            //IF NO THERE ARE NO USERS IN THE FIREBASE DATABASE, SIGN UP AN ANONYMOUS USER
            FirebaseDatabase.getInstance().getReference().child("userTypes")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (!dataSnapshot.hasChildren()) { //if userTypes has no data...
                                Toast.makeText(applicationContext, "Adding anonymous users...", Toast.LENGTH_SHORT).show()
                                anonymousSignup() //TODO - cannot signup user through ValueEventListener
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Getting Post failed, log a message
                            Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                        }
                    })

        }
    }

    /** 'Let's Go' button - Rider/Driver switch  */
    fun letsGoClicked(view: View?) {
        redirectActivity()
    }

    fun anonymousSignup() {

        //sign up rider
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    var userID = task.result?.user?.uid
                    if (userID != null) {
                        FirebaseDatabase.getInstance().getReference().child("userTypes").child("riders").child("userID").setValue(userID) // REALTIME DATABASE - creates an "user" folder (if it wasn't previously created) and stores user details inside
                        Toast.makeText(baseContext, "Signed up a rider", Toast.LENGTH_SHORT).show()
                        auth.signOut() //since there is no need to log user in //TODO - cannot signout in the RiderActivity
                    }
                } else {
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }

        //sign up driver
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    var userID = task.result?.user?.uid
                    if (userID != null) {
                        FirebaseDatabase.getInstance().getReference().child("userTypes").child("drivers").child("userID").setValue(userID) // REALTIME DATABASE - creates an "user" folder (if it wasn't previously created) and stores user details inside
                        Toast.makeText(baseContext, "Signed up a driver", Toast.LENGTH_SHORT).show()
                        auth.signOut() //since there is no need to log user in //TODO - cannot signout in the RiderActivity
                    }
                } else {
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }

    }

    fun redirectActivity() {

        if (auth.currentUser != null) { //IF USER WAS PREVIOUSLY LOGGED IN...
            val intent = Intent(this, RiderActivity::class.java) //TODO - user could be a Driver too - pass in 'switch' boolean into RiderActivity and code the logic there
            intent.putExtra("userID", auth.currentUser!!.uid) //TODO
            startActivity(intent)
        } else {

            var userType = ""
            var intent: Intent? = null

            if (switch?.isChecked!!) { //if user is a driver...
                userType = "driver"
//                intent = Intent(this, DriverActivity::class.java)
            } else { //if user is a rider...
                userType = "rider"
                intent = Intent(this, RiderActivity::class.java)
            }

            //to get data (userID from the Firebase Database) depending on the userType and pass it on
            FirebaseDatabase.getInstance().getReference().child("userTypes").child(userType + "s").child("userID")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userID = dataSnapshot.value as String
                        intent?.putExtra("userID", userID)
                        startActivity(intent)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                    }
                })

        }

    }

}