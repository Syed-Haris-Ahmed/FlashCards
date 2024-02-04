package com.example.flashcards.screens

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.flashcards.R
import com.example.flashcards.decks.Decks
import com.example.flashcards.fragments.DecksFragment
import com.example.flashcards.fragments.SettingsFragment
import com.example.flashcards.utils.UtilityMethods
import com.example.flashcards.viewmodels.DeckViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavView: BottomNavigationView

    private lateinit var sharedPref: SharedPreferences

    private lateinit var deckViewModel: DeckViewModel

    private lateinit var progressBar: ProgressBar

    private val titleComparator: Comparator<Decks> = compareBy { it.titleInLowerCase }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deckViewModel = ViewModelProvider(this)[DeckViewModel::class.java]

        bottomNavView = findViewById(R.id.bottomNavigationView)

        progressBar = findViewById(R.id.progressBar)

        // Set the icon tint color using the color selector
        val iconColorStateList = ContextCompat.getColorStateList(this,
            R.color.selector_bottom_nav_icon_color
        )
        bottomNavView.itemIconTintList = iconColorStateList
        bottomNavView.itemTextColor = iconColorStateList
        bottomNavView.menu.clear()
        bottomNavView.inflateMenu(R.menu.bottom_nav_menu_decks)

        sharedPref = this.getSharedPreferences("login_state", Context.MODE_PRIVATE)
        val userID = sharedPref.getString("userID", "")

        deckViewModel.userID = sharedPref.getString("userID", "").toString()

        retrieveDecks(userID.toString())

        deckViewModel.swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val distanceToTriggerSyncInDp = 70 // Set your desired offset in dp
        val scale = resources.displayMetrics.density
        val distanceToTriggerSyncInPx = (distanceToTriggerSyncInDp * scale + 0.5f).toInt()

        deckViewModel.swipeRefreshLayout.setProgressViewOffset(false, 0, distanceToTriggerSyncInPx)

        deckViewModel.swipeRefreshLayout.setOnRefreshListener {
            recreate()
        }

        @Suppress("DEPRECATION")
        bottomNavView.setOnNavigationItemSelectedListener { menuItem ->
            // Handle item click events here
            when (menuItem.itemId) {
                R.id.decks -> {
                    replaceFragment(DecksFragment())
                    true
                }
                R.id.settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun retrieveDecks(userID: String) {
        deckViewModel.deckButtons.clear()
        progressBar.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        val deckCollectionRef = db.collection("users").document(userID).collection("decks")

        val deckButtons = arrayListOf<Decks>()

        val query = deckCollectionRef
            .whereEqualTo("deleteStatus", false)

        query.get().addOnSuccessListener { decksDocumentSnapshots ->
            val decks = decksDocumentSnapshots.documents

            for (i in decks) {
                println("Deck ID: " + i.id)
                val title = i.getString("title")
                println(title)
                val dateAdded = i.getString("dateAdded")
                val lastUpdated = i.getString("lastUpdated")
                val timeAdded = i.getString("timeAdded")
                val numberOfCards = i.getLong("numberOfCards")

                deckButtons.add(Decks(i.id, title.toString(), title.toString().lowercase(),
                    UtilityMethods.generateColor(), dateAdded.toString(), lastUpdated.toString(), timeAdded.toString(), timeAdded.toString(), userID, numberOfCards.toString().toInt()))
            }

            deckViewModel.deckButtons = deckButtons
            UtilityMethods.mergeSortDecks(deckViewModel.deckButtons, titleComparator)
            replaceFragment(DecksFragment())
            progressBar.visibility = View.GONE
        }.addOnFailureListener { e ->
            // Handle any errors that occurred during the query
            progressBar.visibility = View.GONE
            Log.e("Firestore", "Error querying Firestore", e)
        }
    }

    @Suppress("unused")
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        if (network != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        }

        return false
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(com.google.android.material.R.id.container, fragment)
        fragmentTransaction.commit()
    }

    override fun onStop() {
        super.onStop()
        if (!sharedPref.getBoolean("isLoggedIn", true)) {
            Toast.makeText(this, "Logging Out", Toast.LENGTH_SHORT).show()
            sharedPref.edit().putBoolean("isLoggedIn", false).apply()
            UtilityMethods.signOut(this)
        }
    }
}
