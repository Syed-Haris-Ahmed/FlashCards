package com.example.flashcards.screens

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.flashcards.fragments.FlashCardFragment
import com.example.flashcards.viewmodels.FlashCardsViewModel
import com.example.flashcards.R
import com.example.flashcards.fragments.SettingsFragment
import com.example.flashcards.utils.UtilityMethods
import com.example.flashcards.cards.Cards
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class FlashCardsActivity : AppCompatActivity() {

    private lateinit var bottomNavView: BottomNavigationView

    private lateinit var cardsViewModel: FlashCardsViewModel

    private lateinit var sharedPref: SharedPreferences

    private lateinit var progressBar: ProgressBar

    private val titleComparator: Comparator<Cards> = compareBy { it.frontText }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)

        sharedPref = this.getSharedPreferences("login_state", Context.MODE_PRIVATE)
        val userID = sharedPref.getString("userID", "")

        cardsViewModel = ViewModelProvider(this)[FlashCardsViewModel::class.java]

        val deckID = intent.getStringExtra("deckID")
        val deckTitle = intent.getStringExtra("deckTitle")
        cardsViewModel.deckID = deckID.toString()
        cardsViewModel.deckTitle = deckTitle.toString()

        retrieveCards(userID.toString(), deckID.toString())

        bottomNavView = findViewById(R.id.bottomNavigationView)

        val iconColorStateList = ContextCompat.getColorStateList(this,
            R.color.selector_bottom_nav_icon_color
        )
        bottomNavView.itemIconTintList = iconColorStateList
        bottomNavView.itemTextColor = iconColorStateList
        bottomNavView.menu.clear()
        bottomNavView.inflateMenu(R.menu.bottom_nav_menu_cards)


        // Set a listener to handle item clicks
        @Suppress("DEPRECATION")
        bottomNavView.setOnNavigationItemSelectedListener { menuItem ->
            // Handle item click events here
            when (menuItem.itemId) {
                R.id.cards -> {
                    replaceFragment(FlashCardFragment())
                    true
                }
                R.id.settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        cardsViewModel.swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val distanceToTriggerSyncInDp = 70 // Set your desired offset in dp
        val scale = resources.displayMetrics.density
        val distanceToTriggerSyncInPx = (distanceToTriggerSyncInDp * scale + 0.5f).toInt()

        cardsViewModel.swipeRefreshLayout.setProgressViewOffset(false, 0, distanceToTriggerSyncInPx)

        cardsViewModel.swipeRefreshLayout.setOnRefreshListener {
            cardsViewModel.isShuffled = false
            recreate()
        }
    }

    private fun retrieveCards(userID: String, deckID: String) {
        cardsViewModel.cardButtons.clear()
        cardsViewModel.shuffledData.clear()

        progressBar.visibility = View.VISIBLE

        val cardCollectionRef = Firebase.firestore.collection("users").document(userID).collection("decks").document(deckID).collection("cards")

        val cardButtons = arrayListOf<Cards>()

        val query = cardCollectionRef
            .whereEqualTo("userID", userID)
            .whereEqualTo("deckID", deckID)
            .whereEqualTo("deleteStatus", false)

        query.get().addOnSuccessListener { cardsDocumentSnapshots ->
            val cards = cardsDocumentSnapshots.documents

            for (i in cards) {
                println("Card ID: " + i.id)
                val frontText = i.getString("frontText").toString()
                val backText = i.getString("backText").toString()
                val description = i.getString("description").toString()
                val dateAdded = i.getString("dateAdded").toString()
                val lastUpdated = i.getString("lastUpdated").toString()
                val timeAdded = i.getString("timeAdded").toString()

                cardButtons.add(Cards(i.id, deckID, userID, frontText, backText, description,
                    UtilityMethods.generateColor(), dateAdded, lastUpdated, timeAdded))
            }

            UtilityMethods.mergeSortCards(cardButtons, titleComparator)
            cardsViewModel.cardButtons = cardButtons
            cardsViewModel.shuffledData = cardButtons
            replaceFragment(FlashCardFragment())
            progressBar.visibility = View.GONE
        }.addOnFailureListener { e ->
            // Handle any errors that occurred during the query
            progressBar.visibility = View.GONE
            Log.e("Firestore", "Error querying Firestore", e)
        }
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