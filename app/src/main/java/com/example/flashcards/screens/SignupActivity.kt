package com.example.flashcards.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flashcards.R
import com.example.flashcards.utils.FirebaseUtilityMethods
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences

    private lateinit var usernameStatus: ImageView
    private lateinit var emailStatus: ImageView
    private lateinit var passwordStatus: ImageView
    private lateinit var confirmpwStatus: ImageView

    private lateinit var usernameStatusText: TextView
    private lateinit var emailStatusText: TextView
    private lateinit var passwordStatusText: TextView
    private lateinit var confirmpwStatusText: TextView

    private lateinit var signupButton: Button
    private lateinit var loginButton: TextView

    private var usernameFlag: Boolean = false
    private var emailFlag: Boolean = false
    private var passwordFlag: Boolean = false
    private var confirmpwFlag: Boolean = false

    private var username: String? = null
    private var email: String? = null
    private var password: String? = null

    private var usernameEmailList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        loadUsernamesAndEmails(
            onLoaded = {
                println("Users Loaded")
            },
            onError = { e ->
                println("Bravo Six Going Dark")
                e.printStackTrace()
            }
        )

        sharedPref = getSharedPreferences("login_state", Context.MODE_PRIVATE)

        initializeViews()

        setListeners()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setListeners() {
        val fields = mapOf(
            R.id.username to "Username",
            R.id.email to "Email",
            R.id.password to "Password",
            R.id.confirm_password to "Confirm Password"
        )

        for ((fieldId) in fields) {
            val fieldEditText: EditText = findViewById(fieldId)
            fieldEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val fieldValue = s.toString()
                    // Use fieldId to identify the currently changing EditText
                    when (fieldId) {
                        R.id.username -> validateField(fieldValue, fieldEditText, usernameStatus, usernameStatusText,
                            R.string.username_can_only_contain_letters_digits_underscores_dashes_and_spaces,
                            Regex("^[A-Za-z0-9_\\- ]+$")
                        )
                        R.id.email -> validateField(fieldValue, fieldEditText, emailStatus, emailStatusText,
                            R.string.enter_a_valid_email,
                            Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")
                        )
                        R.id.password -> validateField(fieldValue, fieldEditText, passwordStatus, passwordStatusText,
                            R.string.must_have_8_characters_1_lowercase_letter_1_digit,
                            Regex("^(?=.*[a-z])(?=.*\\d).{8,}\$")
                        )
                        R.id.confirm_password -> validateConfirmPassword(fieldValue, fieldEditText)
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        if (usernameFlag && emailFlag && passwordFlag && confirmpwFlag && checkUsernameOrEmail(username.toString()) && checkUsernameOrEmail(email.toString())) {
            println("Adding user")
            FirebaseUtilityMethods.signUpWithEmailPassword(username.toString(), email.toString(), password.toString()) { success ->
                if (success) {
                    showToast(getString(R.string.user_created_successfully))
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    showToast(getString(R.string.failed_to_create_user))
                    signupButton.error()
                }
            }

        } else {
            signupButton.error()
        }
    }

    private fun initializeViews() {
        usernameStatus = findViewById(R.id.username_status)
        emailStatus = findViewById(R.id.email_status)
        passwordStatus = findViewById(R.id.password_status)
        confirmpwStatus = findViewById(R.id.confirmpw_status)

        usernameStatusText = findViewById(R.id.usernameST)
        emailStatusText = findViewById(R.id.emailST)
        passwordStatusText = findViewById(R.id.passwordST)
        confirmpwStatusText = findViewById(R.id.confirmpwST)

        signupButton = findViewById(R.id.sign_upButton)
        loginButton = findViewById(R.id.login)
    }

    private fun loadUsernamesAndEmails(onLoaded: (List<Pair<String, String>>) -> Unit, onError: (Exception) -> Unit) {
        println("Loading usernames and emails")
        val db = FirebaseFirestore.getInstance()
        val usersCollectionRef = db.collection("users")

        this.usernameEmailList = mutableListOf()

        usersCollectionRef.get()
            .addOnSuccessListener { querySnapshot ->

                for (document in querySnapshot.documents) {
                    val username = document.getString("username") ?: ""
                    val email = document.getString("email") ?: ""
                    println("email: $email || username: $username")
                    this.usernameEmailList.add(username to email)
                }

                onLoaded(this.usernameEmailList)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    private fun checkUsernameOrEmail(usernameOrEmail: String): Boolean {
        // Search for the username or email in the list
        val searchResult = this.usernameEmailList.find { (username, email) ->
            username == usernameOrEmail || email == usernameOrEmail
        }

        // If the search result is null, then the username or email was not found in the list
        return searchResult == null
    }

    private fun validateField(
        fieldValue: String,
        fieldEditText: EditText,
        statusImageView: ImageView,
        statusTextView: TextView,
        errorResourceId: Int,
        regexPattern: Regex? = null
    ) {
        if (fieldValue.isEmpty()) {
            fieldEditText.setBackgroundResource(R.drawable.ic_border)
            statusImageView.visibility = View.INVISIBLE
            statusTextView.visibility = View.INVISIBLE
        } else {
            val isValid = regexPattern?.matches(fieldValue) ?: true && checkUsernameOrEmail(fieldValue)
            val test = checkUsernameOrEmail(fieldValue)
            Toast.makeText(this, test.toString(), Toast.LENGTH_SHORT).show()
            val statusDrawable = if (isValid) R.drawable.check_green else R.drawable.error_red
            statusImageView.setBackgroundResource(statusDrawable)
            fieldEditText.setBackgroundResource(if (isValid) R.drawable.ic_green_border else R.drawable.ic_error_border)
            if (!isValid) {
                statusTextView.text = getString(errorResourceId)
                statusTextView.visibility = View.VISIBLE
            } else {
                statusTextView.visibility = View.INVISIBLE
            }
        }
    }

    private fun validateConfirmPassword(confirmPW: String, confirmpwEditText: EditText) {
        val regexPattern = Regex("^(?=.*[a-z])(?=.*\\d).{8,}\$")

        val passwordField = findViewById<EditText>(R.id.password)
        if (confirmPW.isEmpty()) {
            confirmpwFlag = false
            confirmpwEditText.setBackgroundResource(R.drawable.ic_border)
            confirmpwStatusText.visibility = View.INVISIBLE
            confirmpwStatus.visibility = View.INVISIBLE
        } else {
            if (confirmPW != passwordField.text.toString() || !regexPattern.matches(passwordField.text.toString())) {
                confirmpwFlag = false
                confirmpwEditText.setBackgroundResource(R.drawable.ic_error_border)
                confirmpwStatusText.text = getString(R.string.password_does_not_match)
                confirmpwStatusText.visibility = View.VISIBLE
                confirmpwStatus.setBackgroundResource(R.drawable.error_red)
            } else {
                this.password = passwordField.text.toString()
                confirmpwFlag = true
                confirmpwEditText.setBackgroundResource(R.drawable.ic_green_border)
                confirmpwStatusText.visibility = View.INVISIBLE
                confirmpwStatus.setBackgroundResource(R.drawable.check_green)
            }
            confirmpwStatus.visibility = View.VISIBLE
        }
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
}
