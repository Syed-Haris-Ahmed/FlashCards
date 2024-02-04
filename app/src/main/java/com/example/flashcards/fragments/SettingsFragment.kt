package com.example.flashcards.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.flashcards.R
import com.example.flashcards.screens.LoginActivity
import com.example.flashcards.utils.UtilityMethods
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class SettingsFragment : Fragment() {

    private lateinit var logOutButton: Button
    private lateinit var deckTrashButton: Button
    private lateinit var cardTrashButton: Button
    private lateinit var userDetailsButton: Button

    private lateinit var usernameTextView: TextView

    private lateinit var profilePicView: CircleImageView

    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedPref = requireActivity().getSharedPreferences("login_state", Context.MODE_PRIVATE)

        val profilePicBytes = getProfilePicture(sharedPref.getString("userID", ""))

        profilePicView = view.findViewById(R.id.profile_image)

        if (!profilePicBytes.isNullOrBlank()) {
            profilePicView.setImageBitmap(BitmapFactory.decodeByteArray(profilePicBytes.toByteArray(), 0, profilePicBytes.toByteArray().size))
        }

        userDetailsButton = view.findViewById(R.id.user_details_button)
        logOutButton = view.findViewById(R.id.log_out_button)
        deckTrashButton = view.findViewById(R.id.deck_trash_button)
        cardTrashButton = view.findViewById(R.id.card_trash_button)

        usernameTextView = view.findViewById(R.id.username_text_view)

        usernameTextView.text = sharedPref.getString("username", "")

        userDetailsButton.setOnClickListener {
            TODO()
        }

        logOutButton.setOnClickListener {
            logOut()
        }

        deckTrashButton.setOnClickListener {
            replaceFragment(DeckTrashFragment())
        }

        cardTrashButton.setOnClickListener {
            replaceFragment(FlashCardTrashFragment())
        }

        return view
    }

    private fun getProfilePicture(userID: String?) : String? {
        if (userID == null) {
            Log.i("Error: ", "User not found")
            return null
        }

        val db = FirebaseFirestore.getInstance()
        val user = db.collection("users").document(userID)

        var profilePicByteCode: String? = null

        user.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    profilePicByteCode = documentSnapshot.get("profilePicture").toString()
                } else {
                    Log.e("Error: ", "No profile pic")
                }
            }
            .addOnFailureListener {
                Log.e("Error: ", "Error while retrieving pic")
            }
        return profilePicByteCode
    }

    private fun logOut() {
        // Clear login state in shared preferences
        sharedPref.edit().putBoolean("isLoggedIn", false).apply()

        UtilityMethods.signOut(requireContext())

        // Redirect to the login screen
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)

        // Finish the hosting activity (MainActivity) to prevent the user from going back
        requireActivity().finish()
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(com.google.android.material.R.id.container, fragment)
        transaction.addToBackStack(null) // Add to back stack to allow navigation back
        transaction.commit()
    }
}