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
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance() //current user
    var switch: Switch? = null
    var userType = "" //to track whether it's a Rider or a Driver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch = findViewById<View>(R.id.switch1) as Switch

        supportActionBar!!.hide() //full screen

        if (auth.currentUser != null) { //IF USER WAS PREVIOUSLY LOGGED IN...
            //TODO
            Toast.makeText(applicationContext, "USER EXISTS " + auth.currentUser!!.uid, Toast.LENGTH_SHORT).show()
            redirectActivity()
        } else { //ELSE, SIGN THEM UP...
            Toast.makeText(applicationContext, "USER doesn't EXIST", Toast.LENGTH_SHORT).show()

            anonymousSignup()
        }
    }

    /** 'Let's Go' button - Rider/Driver switch  */
    fun letsGoClicked(view: View?) {

        //set userType
        userType = if (switch?.isChecked!!) { //if switch is pointed at driver...
            "driver"
        } else {
            "rider"
        }

        redirectActivity()

    }

    fun anonymousSignup() {

        //sign up rider
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    var userID = task.result?.user?.uid
                    if (userID != null) {
                        FirebaseDatabase.getInstance().getReference().child("users").child(userID).child("userType").setValue("rider") // REALTIME DATABASE - creates an "user" folder (if it wasn't previously created) and stores user details inside
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
                            FirebaseDatabase.getInstance().getReference().child("users").child(userID).child("userType").setValue("driver") // REALTIME DATABASE - creates an "user" folder (if it wasn't previously created) and stores user details inside
                        }
                    } else {
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }

    }

    fun redirectActivity() {
        if (switch?.isChecked!!) { //if user is a driver...
            //to do
        } else { //if user is a rider...
            val intent = Intent(this, RiderActivity::class.java)
            intent.putExtra("userID", auth.currentUser!!.uid)
            startActivity(intent)
        }
    }

}