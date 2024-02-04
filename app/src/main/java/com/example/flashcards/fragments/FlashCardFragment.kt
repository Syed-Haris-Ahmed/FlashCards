package com.example.flashcards.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.adapters.FlashCardsAdapter
import com.example.flashcards.viewmodels.FlashCardsViewModel
import com.example.flashcards.R
import com.example.flashcards.utils.SwipeToDeleteCallback
import com.example.flashcards.utils.UtilityMethods
import com.example.flashcards.cards.Cards
import com.example.flashcards.cards.CardsButtons
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlashCardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FlashCardsAdapter
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabOption1: FloatingActionButton
    private lateinit var fabOption2: FloatingActionButton
    private lateinit var expandedOptions: LinearLayout
    private lateinit var editDeckButton: Button
    private lateinit var sortByButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var searchBar: SearchView

    private var sortState: Int = 1

    private var userCollectionRef = Firebase.firestore.collection("users")

    private lateinit var dialog: Dialog

    private lateinit var sharedPref: SharedPreferences
    private lateinit var deckID: String

    private val cardsViewModel: FlashCardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_flashcards, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        fabMain = view.findViewById(R.id.fabMain)
        fabOption1 = view.findViewById(R.id.fabOption1)
        fabOption2 = view.findViewById(R.id.fabOption2)
        expandedOptions = view.findViewById(R.id.expandedOptions)
        sortByButton = view.findViewById(R.id.sort_by_button)
        toolbar = view.findViewById(R.id.toolbar)
        searchBar = view.findViewById(R.id.search_cards_button)

        val mainActivity = activity as? AppCompatActivity
        mainActivity?.setSupportActionBar(toolbar)
        mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed() // This will navigate back to the previous activity
        }

        sharedPref = requireActivity().getSharedPreferences("login_state", Context.MODE_PRIVATE)
        val userID = sharedPref.getString("userID", "")
        cardsViewModel.userID = userID.toString()
        deckID = cardsViewModel.deckID
        toolbar.title = cardsViewModel.deckTitle

        Toast.makeText(requireContext(), userID, Toast.LENGTH_LONG).show()

        editDeckButton = view.findViewById(R.id.edit_deck_button)

        editDeckButton.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_edit_deck)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val newDeckNameEditText: EditText = dialog.findViewById(R.id.deck_title_edit_text)
            val doneButton: ImageButton = dialog.findViewById(R.id.update_button)
            val cancelButton: ImageButton = dialog.findViewById(R.id.cancel_button)

            doneButton.setOnClickListener {
                updateDeck(deckID, newDeckNameEditText.text.toString())
                dialog.dismiss()
            }

            cancelButton.setOnClickListener {

                dialog.dismiss()
            }

            dialog.show()
        }

        setupRecyclerView()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Check if the RecyclerView is at the top
                val isAtTop = !recyclerView.canScrollVertically(-1)

                // Enable/disable swipe-to-refresh based on scroll position
                cardsViewModel.swipeRefreshLayout.isEnabled = isAtTop
            }
        })

        searchBar.setOnSearchClickListener {
            Toast.makeText(requireContext(), "search bar width: " + searchBar.width, Toast.LENGTH_SHORT).show()
            animateSearchViewWidth(1004)
        }

        searchBar.setOnCloseListener {
            Toast.makeText(requireContext(), "search bar width: " + searchBar.width, Toast.LENGTH_SHORT).show()
            animateSearchViewWidth(69)
            false
        }

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText.toString())
                return false
            }
        })

        sortByButton.setOnClickListener {
            sortState = when (sortState) {
                1 -> 2
                2 -> 3
                else -> 1
            }

            sortCards()
        }

        fabMain.setOnClickListener { toggleOptions() }

        fabOption1.setOnClickListener {
            dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_box_add_card)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val frontTextField: EditText = dialog.findViewById(R.id.front_text)
            val backTextField: EditText = dialog.findViewById(R.id.back_text)
            val descriptionTextField: EditText = dialog.findViewById(R.id.description)
            val cancelButton: ImageButton = dialog.findViewById(R.id.cancel_button)
            val addButton: ImageButton = dialog.findViewById(R.id.add_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            addButton.setOnClickListener {
                if (frontTextField.text.isNotEmpty() && backTextField.text.isNotEmpty()) {
                    addCard(userID.toString(), deckID, frontTextField.text.toString(), backTextField.text.toString(), descriptionTextField.text.toString())
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        fabOption2.setOnClickListener {
            cardsViewModel.isShuffled = if (cardsViewModel.isShuffled) {
                adapter.updateData(cardsViewModel.cardButtons)
                Toast.makeText(activity, "Normal Data", Toast.LENGTH_SHORT).show()
                fabOption2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
                false
            } else {
                cardsViewModel.shuffledData = cardsViewModel.cardButtons.shuffled() as ArrayList<Cards>
                adapter.updateData(cardsViewModel.shuffledData)
                Toast.makeText(activity, "Shuffled Data", Toast.LENGTH_SHORT).show()
                fabOption2.setColorFilter(ContextCompat.getColor(requireContext(),
                    R.color.app_green
                ))
                true
            }
        }

        cardsViewModel.swipeRefreshLayout.isRefreshing = false

        return view
    }

    private fun animateSearchViewWidth(targetWidth: Int) {
        val animator = ValueAnimator.ofInt(searchBar.width, targetWidth)
        animator.addUpdateListener { valueAnimator ->
            val layoutParams = searchBar.layoutParams
            layoutParams.width = valueAnimator.animatedValue as Int
            searchBar.layoutParams = layoutParams
        }
        animator.duration = 300
        animator.start()
    }

    private fun sortCards() {
        val titleComparator: Comparator<Cards> = compareBy { it.frontText }
        val dateAddedComparator: Comparator<Cards> = compareBy( { it.dateAdded }, { it.timeAdded } )
        val lastUpdatedComparator: Comparator<Cards> = compareBy( { it.lastUpdated }, { it.lastTimeUpdated } )
        when (sortState) {
            1 -> {
                Log.i("Main Fragment", "Sorting By Name")
                sortByButton.setBackgroundResource(R.drawable.ic_sort_by_name)
                cardsViewModel.cardButtons.reverse()
                UtilityMethods.mergeSortCards(cardsViewModel.cardButtons, titleComparator)
                for (i in cardsViewModel.cardButtons) {
                    println(i.frontText)
                }
            }

            2 -> {
                Log.i("Main Fragment", "Sorting By Date Added")
                sortByButton.setBackgroundResource(R.drawable.ic_sort_by_date_added)
                UtilityMethods.mergeSortCards(cardsViewModel.cardButtons, dateAddedComparator)
                cardsViewModel.cardButtons.reverse()
                for (i in cardsViewModel.cardButtons) {
                    println(i.frontText + "-----" + i.dateAdded + "-----" + i.timeAdded)
                }
            }

            3 -> {
                Log.i("Main Fragment", "Sorting By Name Last Updated")
                sortByButton.setBackgroundResource(R.drawable.ic_sort_by_last_updated)
                UtilityMethods.mergeSortCards(cardsViewModel.cardButtons, lastUpdatedComparator)
                cardsViewModel.cardButtons.reverse()
                for (i in cardsViewModel.cardButtons) {
                    println(i.frontText + "-----" + i.lastUpdated)
                }
            }
        }
        adapter.updateData(cardsViewModel.cardButtons)
    }

    private fun updateDeck(deckID: String, newTitle: String) {
        val newDeckData = mapOf(
            "title" to newTitle,
            "lastUpdated" to getCurrentDateAsString()
            // Add other fields you want to update here
        )

        val deckCollectionRef = userCollectionRef.document(cardsViewModel.userID).collection("decks")

        deckCollectionRef.document(deckID)
            .update(newDeckData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update was successful
                    Toast.makeText(requireContext(), "Deck updated successfully", Toast.LENGTH_SHORT).show()
                    toolbar.title = newTitle
                    val edit = sharedPref.edit()
                    edit.putBoolean("edited", true)
                    edit.putString("editedDeckID", deckID)
                    edit.putString("editedTitle", newTitle)
                    edit.apply()
                } else {
                    val exception = task.exception
                    Toast.makeText(requireContext(), "Error updating deck: " + exception?.message, Toast.LENGTH_LONG).show()
                    sharedPref.edit().putBoolean("edited", false).apply()
                }
            }

        if (!UtilityMethods.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Deck updated successfully", Toast.LENGTH_SHORT).show()
            toolbar.title = newTitle
            val edit = sharedPref.edit()
            edit.putBoolean("edited", true)
            edit.putString("editedDeckID", deckID)
            edit.putString("editedTitle", newTitle)
            edit.apply()
        }
    }

    private fun addCard(userID: String, deckID: String, enteredFontText: String, enteredBackText: String, enteredDescription: String) {
        val time = getCurrentTimeAsString()
        val date = getCurrentDateAsString()
        val newCard = CardsButtons(userID, deckID, enteredFontText, enteredBackText, enteredDescription, "", date, date, time, false, time)

        val newCardCollectionRef = userCollectionRef.document(cardsViewModel.userID).collection("decks").document(cardsViewModel.deckID).collection("cards")

        newCardCollectionRef.add(newCard)
            .addOnCompleteListener { task ->
                println("New Card ID: " + task.result.id)
                if (task.isSuccessful) {
                    val id = task.result.id
                    println("$id New Card")
                    Toast.makeText(requireContext(), "New Card: $id", Toast.LENGTH_SHORT).show()
                } else {
                    val exception = task.exception
                    Toast.makeText(requireContext(), "Error creating new card: " + exception?.message, Toast.LENGTH_LONG).show()
                }
                val newCardButton = Cards(task.result.id, deckID, userID, enteredFontText, enteredBackText, enteredDescription, UtilityMethods.generateColor(), date, date, time, false, time)
                if (cardsViewModel.isShuffled) {
                    cardsViewModel.shuffledData.add(newCardButton)
                    adapter.updateData(cardsViewModel.shuffledData)
                }
                else {
                    cardsViewModel.cardButtons.add(newCardButton)
                    adapter.updateData(cardsViewModel.cardButtons)
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

    private fun setupRecyclerView() {
        if (cardsViewModel.isShuffled) {
            adapter = FlashCardsAdapter(requireContext(), cardsViewModel.shuffledData, cardsViewModel, cardsViewModel.userID, cardsViewModel.deckID)
            fabOption2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.app_green))
        }
        else {
            adapter = FlashCardsAdapter(requireContext(), cardsViewModel.cardButtons, cardsViewModel, cardsViewModel.userID, cardsViewModel.deckID)
            fabOption2.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        recyclerView.layoutManager = GridLayoutManager(activity, 1)
        recyclerView.adapter = adapter

        val swipeToDeleteCallback = SwipeToDeleteCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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
            expandedOptions.animate().translationY(390f).setDuration(duration).withEndAction {
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
}