package com.example.flashcards.viewmodels

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.flashcards.decks.Decks

@SuppressLint("StaticFieldLeak")
class DeckViewModel : ViewModel() {
    var userID: String = ""
    var deckButtons: ArrayList<Decks> = ArrayList()
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
}