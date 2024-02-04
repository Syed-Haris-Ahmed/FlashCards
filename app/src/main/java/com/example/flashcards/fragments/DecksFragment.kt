package com.example.flashcards.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout

import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.adapters.DecksAdapter
import com.example.flashcards.R
import com.example.flashcards.utils.UtilityMethods
import com.example.flashcards.decks.DeckButtons
import com.example.flashcards.viewmodels.DeckViewModel
import com.example.flashcards.decks.Decks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DecksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DecksAdapter
    private lateinit var fabMain: FloatingActionButton
    lateinit var deleteFab: FloatingActionButton
    private lateinit var addDeckButton: FloatingActionButton
    private lateinit var sortButton: FloatingActionButton
    private lateinit var searchBar: EditText
    private lateinit var cardView: CardView
    private lateinit var expandedOptions: LinearLayout

    private val deckCollectionRef = Firebase.firestore.collection("decks")
    private val userCollectionRef = Firebase.firestore.collection("users")

    private val deckViewModel: DeckViewModel by activityViewModels()
    private var deckName: String = ""
    private var sortState: Int = 1

    private lateinit var sharedPref: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_decks, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        fabMain = view.findViewById(R.id.fabMain)
        deleteFab = view.findViewById(R.id.deleteButton)
        addDeckButton = view.findViewById(R.id.fabOption1)
        sortButton = view.findViewById(R.id.fabOption2)
        expandedOptions = view.findViewById(R.id.expandedOptions)
        searchBar = view.findViewById(R.id.search_bar)
        cardView = view.findViewById(R.id.cardView)

        sharedPref = requireActivity().getSharedPreferences("login_state", Context.MODE_PRIVATE)
        val userID = sharedPref.getString("userID", "")

        Toast.makeText(requireContext(), userID, Toast.LENGTH_LONG).show()

        setupRecyclerView()

        fabMain.setOnClickListener { toggleOptions() }

        addDeckButton.setOnClickListener {
            addDeckDialog()
        }

        sortButton.setOnClickListener {
             sortState = when (sortState) {
                 1 -> 2
                 2 -> 3
                 else -> 1
             }

            sortDecks()
        }

        deleteFab.setOnClickListener {
            showDeleteDecksDialog()
        }

        searchBar.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Toast.makeText(activity, "Search Text Changed.", Toast.LENGTH_SHORT).show()
                adapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        deckViewModel.swipeRefreshLayout.isRefreshing = false

        return view
    }

    private fun addDeckDialog() {
        val newDeckDialog = Dialog(requireContext())
        newDeckDialog.setContentView(R.layout.dialog_box_new_deck)
        newDeckDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val titleEditText = newDeckDialog.findViewById<EditText>(R.id.deck_title_edit_text)
        val addButton = newDeckDialog.findViewById<ImageButton>(R.id.add_button)
        val cancelButton = newDeckDialog.findViewById<ImageButton>(R.id.cancel_button)

        addButton.setOnClickListener {
            addDeck(titleEditText.text.toString())
            newDeckDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            newDeckDialog.dismiss()
        }

        newDeckDialog.show()
    }

    private fun showDeleteDecksDialog() {
        Toast.makeText(requireContext(), "Delete Button Clicked", Toast.LENGTH_SHORT).show()
        val deleteDialog = Dialog(requireContext())
        deleteDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        deleteDialog.setContentView(R.layout.dialog_confirmation_deletion)
        val confirmButton: ImageButton = deleteDialog.findViewById(R.id.confirmButton)
        val cancelButton: ImageButton = deleteDialog.findViewById(R.id.cancel_button)

        if (adapter.selectedDeckCounter > 0) {
            confirmButton.setOnClickListener {
                adapter.deleteSelectedDecks()
                deckName = ""
                deleteFab.visibility = View.GONE
                deleteDialog.dismiss()
            }

            cancelButton.setOnClickListener {
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        }
    }

    private fun sortDecks() {
        val titleComparator: Comparator<Decks> = compareBy { it.titleInLowerCase }
        val dateAddedComparator: Comparator<Decks> = compareBy { it.dateAdded}
        val lastUpdatedComparator: Comparator<Decks> = compareBy { it.lastUpdated }
        when (sortState) {
            1 -> {
                Log.i("Main Fragment", "Sorting By Name")
                sortButton.setImageResource(R.drawable.ic_sort_by_name)
                UtilityMethods.mergeSortDecks(deckViewModel.deckButtons, titleComparator)
            }

            2 -> {
                Log.i("Main Fragment", "Sorting By Date Added")
                sortButton.setImageResource(R.drawable.ic_sort_by_date_added)
                UtilityMethods.mergeSortDecks(deckViewModel.deckButtons, dateAddedComparator)
            }

            3 -> {
                Log.i("Main Fragment", "Sorting By Name Last Updated")
                sortButton.setImageResource(R.drawable.ic_sort_by_last_updated)
                UtilityMethods.mergeSortDecks(deckViewModel.deckButtons, lastUpdatedComparator)
            }
        }
        adapter.updateData(deckViewModel.deckButtons)
    }

    private fun setupRecyclerView() {
        adapter = DecksAdapter(this, deckViewModel.deckButtons, requireContext())
        recyclerView.layoutManager = GridLayoutManager(activity, 2)
        recyclerView.adapter = adapter
    }

    private fun toggleOptions() {
        val duration = 300L
        if (expandedOptions.visibility == View.GONE) {
            expandedOptions.visibility = View.VISIBLE
            expandedOptions.animate().translationY(100f).setDuration(duration).start()
            val rotateOpen = ObjectAnimator.ofFloat(fabMain, "rotation", 0f, 135f)
            rotateOpen.duration = duration
            rotateOpen.start()

            // Animation to make the FAB bigger
            val scaleOpenX = ObjectAnimator.ofFloat(fabMain, "scaleX", 1f, 1.3f)
            val scaleOpenY = ObjectAnimator.ofFloat(fabMain, "scaleY", 1f, 1.3f)
            scaleOpenX.duration = duration
            scaleOpenY.duration = duration
            scaleOpenX.start()
            scaleOpenY.start()
        } else {
            expandedOptions.animate().translationY(380f).setDuration(duration).withEndAction {
                expandedOptions.visibility = View.GONE
            }.start()
            val rotateClose = ObjectAnimator.ofFloat(fabMain, "rotation", 135f, 0f)
            rotateClose.duration = duration
            rotateClose.start()

            // Animation to make the FAB return to normal size
            val scaleCloseX = ObjectAnimator.ofFloat(fabMain, "scaleX", 1.3f, 1f)
            val scaleCloseY = ObjectAnimator.ofFloat(fabMain, "scaleY", 1.3f, 1f)
            scaleCloseX.duration = duration
            scaleCloseY.duration = duration
            scaleCloseX.start()
            scaleCloseY.start()
        }
    }

    private fun addDeck(title: String) {
        val time = getCurrentTimeAsString()
        val date = getCurrentDateAsString()
        val newDeck = DeckButtons(title, "", date, date, time, deckViewModel.userID)


        val newDeckCollectionRef = userCollectionRef.document(deckViewModel.userID).collection("decks")

        newDeckCollectionRef.add(newDeck)
            .addOnCompleteListener { task ->
                println("New Deck ID: " + task.result.id)
                if (task.isSuccessful) {
                    val id = task.result.id
                    println("$id New Deck")
                    deckViewModel.deckButtons.add(Decks(id, title, title.lowercase(), UtilityMethods.generateColor(), date, date, time, time, deckViewModel.userID))
                    adapter.updateData(deckViewModel.deckButtons)
                    Toast.makeText(requireContext(), "New Deck: $id", Toast.LENGTH_SHORT).show()
                } else {
                    val exception = task.exception
                    Toast.makeText(requireContext(), "Error creating new deck: " + exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getCurrentDateAsString(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentTimeAsString(): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date())
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        val edited = sharedPref.getBoolean("edited", false)

        if (edited) {
            val deckEdited = sharedPref.getString("editedDeckID", "")
            val editedTitle = sharedPref.getString("editedTitle", "")
            for (i in adapter.filteredData) {
                if (i.deckID == deckEdited) {
                    i.title = editedTitle.toString()
                    sharedPref.edit().putBoolean("edited", false).apply()
                    adapter.notifyDataSetChanged()
                    break
                }
            }
        }
    }
}