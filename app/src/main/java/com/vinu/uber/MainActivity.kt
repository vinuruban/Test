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
import com.google.firebase.auth.*
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var switch: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch = findViewById<View>(R.id.switch1) as Switch

        supportActionBar!!.hide() //full screen

        /** IF USER WAS PREVIOUSLY LOGGED IN... **/
        if (auth.currentUser != null) {
            Toast.makeText(applicationContext, "User already logged in: " + auth.currentUser!!.uid, Toast.LENGTH_SHORT).show()
            redirectActivity()
        }

//        /** IF NO THERE ARE NO USERS IN THE FIREBASE DATABASE, SIGN UP AN ANONYMOUS USER **/
//        FirebaseDatabase.getInstance().getReference().child("userTypes")
//                .addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        if (!dataSnapshot.hasChildren()) { //if userTypes has no data...
//                            Toast.makeText(applicationContext, "Adding anonymous users...", Toast.LENGTH_SHORT).show()
//                            anonymousSignup()
//                        }
//                    }
//
//                    override fun onCancelled(databaseError: DatabaseError) {
//                        // Getting Post failed, log a message
//                        Log.w("Error", "loadPost:onCancelled", databaseError.toException())
//                    }
//                })

    }

    /** 'Let's Go' button - Rider/Driver switch  */
    fun letsGoClicked(view: View?) {
        redirectActivity()
    }

    fun redirectActivity() {

        if (auth.currentUser != null) { //IF USER WAS PREVIOUSLY LOGGED IN...
            val intent = Intent(this, RiderActivity::class.java) //TODO - user could be a Driver too - pass in 'switch' boolean into RiderActivity and code the logic there
            startActivity(intent)
        } else { /** when letsGoClicked is triggered **/

            var userType = ""
            var intent: Intent? = null

            if (switch?.isChecked!!) { //if user is a driver...
                userType = "driver"
//                intent = Intent(this, DriverActivity::class.java)
            } else { //if user is a rider...
                userType = "rider"
                intent = Intent(this, RiderActivity::class.java)
            }

            /** to get data (userID from the Firebase Database) depending on the userType and pass it on **/
            FirebaseDatabase.getInstance().getReference().child("userTypes").child(userType + "s").child("userID")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) { /** if drivers/riders exist **/
                            val userID = dataSnapshot.value as String

                            anonymousLogin(userType) /** logs user in - firebase auth **/
                            Toast.makeText(applicationContext, "Logged in: " + userID, Toast.LENGTH_SHORT).show()

                            startActivity(intent)
                        }
                        else { /** if drivers/riders don't exist in the database, sign them up **/
                            anonymousSignup(userType)
                            /** since new user is created and new dataSnapshot is required, the addValueEventListener code is called again below **/
                            FirebaseDatabase.getInstance().getReference().child("userTypes").child(userType + "s").child("userID")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot2: DataSnapshot) {
                                            if (dataSnapshot2.exists()) { /** if drivers/riders exist **/
                                            val userID = dataSnapshot2.value as String
                                                startActivity(intent)
                                            }
                                        }

                                        override fun onCancelled(databaseError2: DatabaseError) {
                                            // Getting Post failed, log a message
                                            Log.w("Error", "loadPost:onCancelled", databaseError2.toException())
                                        }
                                    })
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w("Error", "loadPost:onCancelled", databaseError.toException())
                    }
                })

        }

    }

    fun anonymousSignup(userType: String) {

        auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        var userID = task.result?.user?.uid
                        FirebaseDatabase.getInstance().getReference().child("userTypes").child(userType  + "s").child("userID").setValue(userID) // REALTIME DATABASE - creates an "user" folder (if it wasn't previously created) and stores user details inside

                        /** Convert an anonymous account to a permanent account - this will let us keep track of the account when logging out, and then sign them in again when required **/
                        val credential = EmailAuthProvider.getCredential("${userType}@${userType}.com", "123456")
                        task.result?.user?.linkWithCredential(credential)
                                ?.addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        Log.i("AnonymousToPermanent", "linkWithCredential:success")
                                    } else {
                                        Log.i("AnonymousToPermanent", "linkWithCredential:fail")
                                    }
                                }

                        Toast.makeText(baseContext, "Signed up a ${userType}: ${userID}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }

    }

    fun anonymousLogin(userType: String) {
        auth.signInWithEmailAndPassword("${userType}@${userType}.com", "123456")
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) { //IF USER ALREADY EXISTS, LOG THEM IN
                        Log.i("AnonymousLogin", "SignInWithEmailAndPassword:success")
                    } else {
                        Log.i("AnonymousLogin", "SignInWithEmailAndPassword:success")
                    }
                }
    }

}