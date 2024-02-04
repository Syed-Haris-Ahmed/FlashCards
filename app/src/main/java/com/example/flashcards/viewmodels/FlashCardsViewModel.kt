package com.example.flashcards.viewmodels

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.flashcards.cards.Cards

class FlashCardsViewModel : ViewModel() {
    var shuffledData: ArrayList<Cards> = ArrayList()
    var cardButtons: ArrayList<Cards> = ArrayList()
    var deckID = ""
    var userID = ""
    var deckTitle = ""
    var isShuffled = false
    @SuppressLint("StaticFieldLeak")
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
}