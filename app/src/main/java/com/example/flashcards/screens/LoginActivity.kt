package com.example.flashcards.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flashcards.R
import com.example.flashcards.utils.UtilityMethods
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var confirmButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var signupButton: TextView

    private lateinit var loginStatus: TextView
    private lateinit var usernameOrEmail: EditText
    private lateinit var password: EditText

    private var uid = ""
    private var email = ""
    private var username = ""

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignButton: SignInButton

    private lateinit var sharedPref: SharedPreferences

    private lateinit var dialog: Dialog

    private lateinit var auth: FirebaseAuth

    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setUpListeners()

        scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            while (true) {
                updateConnectionStatus()
                delay(2500) // Delay for 2.5 seconds
            }
        }

        auth = Firebase.auth
        setupGoogleSignIn()

        sharedPref = getSharedPreferences("login_state", Context.MODE_PRIVATE)
    }

    private fun initializeViews() {
        dialog = Dialog(this@LoginActivity)
        dialog.setContentView(R.layout.dialog_box)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        confirmButton = dialog.findViewById(R.id.confirmButton)
        cancelButton = dialog.findViewById(R.id.cancel_button)
        signupButton = findViewById(R.id.sign_up)
        loginButton = findViewById(R.id.loginButton)
        googleSignButton = findViewById(R.id.google_sign_in_option)

        loginStatus = findViewById(R.id.loginStatus)

        usernameOrEmail = findViewById(R.id.username)
        password = findViewById(R.id.password)
    }

    private fun setUpListeners() {
        signupButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        googleSignButton.setOnClickListener {
            if (UtilityMethods.isInternetAvailable(applicationContext)) {
                signInWithGoogle()
            }
        }

        val textWatchers = arrayOf(usernameOrEmail, password)
        textWatchers.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    usernameOrEmail.setBackgroundResource(R.drawable.ic_border)
                    password.setBackgroundResource(R.drawable.ic_border)
                    loginStatus.visibility = View.INVISIBLE
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        loginButton.setOnClickListener {
            if (UtilityMethods.isInternetAvailable(applicationContext) && usernameOrEmail.text.isNotEmpty() && password.text.isNotEmpty()) {
                scope.launch {
                    val success = loginUser(usernameOrEmail.text.toString(), password.text.toString())
                    if (success) {
                        saveLoginState()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        showLoginError()
                        clearLoginState()
                    }
                }
            } else {
                showLoginError()
            }
        }
    }

    private suspend fun loginUser(usernameOrEmail: String, password: String): Boolean {
        val regexPattern = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")

        val signInFunction: suspend (String, String) -> Boolean =
            signInFunction@{ userEmail, userPassword ->
                try {
                    val result = auth.signInWithEmailAndPassword(userEmail, userPassword).await()
                    this.uid = result.user!!.uid
                    return@signInFunction result.user != null
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@signInFunction false
                }
            }

        return if (!regexPattern.matches(usernameOrEmail)) {
            val email = retrieveEmail(usernameOrEmail)
            this.email = email
            this.username = usernameOrEmail
            signInFunction(email, password)
        } else {
            val username = retrieveUsername(usernameOrEmail)
            this.username = username
            this.email = usernameOrEmail
            signInFunction(usernameOrEmail, password)
        }
    }

    private fun retrieveUsername(userID: String): String {
        var username = ""
        val userCollectionRef = FirebaseFirestore.getInstance().collection("users")
        val query = userCollectionRef.whereEqualTo("userID", userID).limit(1)
        query.get()
            .addOnSuccessListener { task ->
                val snapshot = task.documents[0]
                username = snapshot.getString("username").toString()
            }
        return username
    }

    private suspend fun retrieveEmail(usernameOrEmail: String): String {
        val userCollectionRef = FirebaseFirestore.getInstance().collection("users")
        val query = userCollectionRef.whereEqualTo("username", usernameOrEmail).limit(1)

        return try {
            val snapshot = query.get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].getString("email").toString()
            } else {
                "false"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "false"
        }
    }

    private fun showLoginError() {
        usernameOrEmail.setBackgroundResource(R.drawable.ic_error_border)
        password.setBackgroundResource(R.drawable.ic_error_border)

        loginStatus.text = getString(R.string.invalid_username_email_or_password)
        loginStatus.visibility = View.VISIBLE

        loginButton.error()
    }

    private fun updateConnectionStatus() {
        val connectionStatus: TextView = findViewById(R.id.connection_status)
        if (UtilityMethods.isInternetAvailable(applicationContext)) {
            animateConnectionStatus(connectionStatus, getString(R.string.back_online), R.color.app_green)
        } else {
            animateConnectionStatus(connectionStatus, getString(R.string.no_internet_connection), R.color.error_red)
        }
    }

    private fun animateConnectionStatus(view: TextView, text: String, colorResource: Int) {
        val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, if (colorResource == R.color.app_green) 0f else 58f)
        view.text = text
        view.setBackgroundResource(colorResource)
        animator.duration = 500
        animator.startDelay = if (colorResource == R.color.app_green) 1500 else 0
        animator.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = FirebaseAuth.getInstance().currentUser

                            val newUserCheck = task.result?.additionalUserInfo?.isNewUser
                            if (newUserCheck == true && firebaseUser != null) {
                                val userCollectionRef = Firebase.firestore.collection("users")
                                val newUser = userCollectionRef.document(firebaseUser!!.uid)
                                val userData = mapOf(
                                    "userID" to firebaseUser.uid,
                                    "username" to firebaseUser.displayName,
                                    "email" to firebaseUser.email
                                )
                                newUser.set(userData)
                                    .addOnCompleteListener {firestoreTask ->
                                        if (firestoreTask.isSuccessful) {
                                            Toast.makeText(this, "New user data created", Toast.LENGTH_SHORT).show()
                                        }
                                        else {
                                            Toast.makeText(this, "Failed to create new user data" + firestoreTask.exception, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }

                            confirmButton.setOnClickListener {
                                saveLoginStateGoogle(firebaseUser, account)
                                dialog.dismiss()
                                startActivity(Intent(this, MainActivity::class.java))
                            }

                            cancelButton.setOnClickListener {
                                clearLoginStateGoogle(firebaseUser, account)
                                dialog.dismiss()
                                startActivity(Intent(this, MainActivity::class.java))
                            }

                            dialog.show()

                        } else {
                            Toast.makeText(this@LoginActivity, "Something went wrong 1", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            Toast.makeText(this@LoginActivity, "Something went wrong 2", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 100)
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun Button.error() {
        val shakeAnimation = ObjectAnimator.ofFloat(this, "translationX", 0f, 20f, -20f, 20f, -20f, 12f, -12f, 6f, -6f, 0f)
        shakeAnimation.duration = 500
        shakeAnimation.interpolator = AccelerateInterpolator()

        val colorAnimator = ValueAnimator.ofArgb(Color.WHITE, Color.RED, Color.WHITE)
        colorAnimator.duration = 500
        colorAnimator.addUpdateListener { animator ->
            this.setTextColor(animator.animatedValue as Int)
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(shakeAnimation, colorAnimator)
        animatorSet.start()
    }

    private fun checkLoginState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "User is logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveLoginState() {
        val edit = sharedPref.edit()
        edit.putBoolean("isLoggedIn", true)
        edit.putString("userID", this.uid)
        edit.putString("email", this.email)
        edit.putString("username", this.username)
        edit.apply()
    }

    private fun clearLoginState() {
        val edit = sharedPref.edit()
        edit.putBoolean("isLoggedIn", false)
        edit.putString("userID", "")
        edit.putString("email", "")
        edit.putString("username", "")
        edit.apply()
    }

    private fun saveLoginStateGoogle(user: FirebaseUser?, account: GoogleSignInAccount?) {
        val edit = sharedPref.edit()
        edit.putBoolean("isLoggedIn", true)
        edit.putString("userID", user?.uid)
        edit.putString("email", account?.email)
        edit.putString("username", account?.displayName)
        edit.apply()
    }

    private fun clearLoginStateGoogle(user: FirebaseUser?, account: GoogleSignInAccount?) {
        val edit = sharedPref.edit()
        edit.putBoolean("isLoggedIn", false)
        edit.putString("userID", user?.uid)
        edit.putString("email", account?.email)
        edit.putString("username", account?.displayName)
        edit.apply()
    }

    override fun onResume() {
        super.onResume()
        checkLoginState()
    }

    override fun onStart() {
        super.onStart()
        checkLoginState()
    }

    override fun onPause() {
        super.onPause()
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}