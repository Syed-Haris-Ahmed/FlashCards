package com.example.flashcards.decks

data class DeckButtons (
    val title: String = "",
    var color: String = "",
    val dateAdded: String = "",
    val lastUpdated: String = "",
    val timeAdded: String = "",
    val userID: String = "",
    val numberOfCards: Int = 0,
    val deleteStatus: Boolean = false
)