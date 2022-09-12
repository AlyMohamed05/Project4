package com.udacity.project4.authentication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val firebaseSignInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { handleFirebaseAuthResult(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        initClickListeners()
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {
            startRemindersActivity()
        }
    }

    private fun startFirebaseLoginUI() {
        val authProviders = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(authProviders)
            .build()
        firebaseSignInLauncher.launch(signInIntent)
    }

    private fun handleFirebaseAuthResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            startRemindersActivity()
        } else {
            Log.d(TAG, "Unexpected error occurred during sign in")
            Log.d(TAG, "Error code : ${result.idpResponse?.error?.errorCode}")
        }
    }

    private fun startRemindersActivity() {
        startActivity(RemindersActivity.newIntent(this))
        finish()
    }

    private fun initClickListeners() {
        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener { startFirebaseLoginUI() }
    }

    companion object {
        private const val TAG = "AuthenticationActivity"
    }
}
