package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.adapters.DecksTrashAdapter
import com.example.flashcards.viewmodels.DeckViewModel
import com.example.flashcards.decks.Decks
import com.example.flashcards.adapters.DecksAdapter
import com.example.flashcards.utils.UtilityMethods
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.LinkedList

class DeckTrashFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DecksTrashAdapter

    private val deletedDecksList = ArrayList<Decks>()

    private val deckCollectionRef = Firebase.firestore.collection("decks")

    private val deckViewModel: DeckViewModel by activityViewModels()

    lateinit var restoreFab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_decks, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)

        //Load decks which were deleted
        loadDeletedDecks()

        restoreFab = view.findViewById(R.id.deleteButton)

        restoreFab.setOnClickListener {
            adapter.restoreSelectedDecks()
            restoreFab.visibility = View.GONE
        }

        return view
    }

    private fun loadDeletedDecks() {
        val query = deckCollectionRef
            .whereEqualTo("userID", deckViewModel.userID)
            .whereEqualTo("deleteStatus", true)

        query.get().addOnSuccessListener { decksDocumentSnapshots ->
            val decks = decksDocumentSnapshots.documents

            for (i in decks) {
                println("Deck ID: " + i.id)
                val title = i.getString("title").toString()
                println(title)
                val dateAdded = i.getString("dateAdded").toString()
                val lastUpdated = i.getString("lastUpdated").toString()
                val timeAdded = i.getString("timeAdded").toString()
                val lastTimeUpdated = i.getString("lastTimeUpdated").toString()
                val numberOfCards = i.getLong("numberOfCards").toString()
                val deleteStatus = i.getBoolean("deleteStatus")

                deletedDecksList.add(Decks(i.id, title, title.lowercase(),
                    UtilityMethods.generateColor(), dateAdded, lastUpdated, timeAdded, lastTimeUpdated, deckViewModel.userID, numberOfCards.toInt(), deleteStatus))
            }
            //Loading the decks on the UI
            setUpRecyclerView()
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error querying Firestore", e)
        }
    }

    private fun setUpRecyclerView() {
        adapter = DecksTrashAdapter(this, deletedDecksList, requireContext(), deckViewModel)
        recyclerView.layoutManager = GridLayoutManager(activity, 2)
        recyclerView.adapter = adapter
    }
}