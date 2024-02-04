package com.example.flashcards.decks

data class Decks(
    val deckID: String = "",
    var title: String = "",
    var titleInLowerCase: String = "",
    var color: String = "",
    val dateAdded: String = "",
    val lastUpdated: String = "",
    val timeAdded: String = "",
    var lastTimeUpdated: String = "",
    val userID: String = "",
    val numberOfCards: Int = 0,
    var deleteStatus: Boolean? = false,
    var isSelected: Boolean = false
)