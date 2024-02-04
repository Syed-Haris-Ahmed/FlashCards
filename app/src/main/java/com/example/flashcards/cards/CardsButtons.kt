package com.example.flashcards.cards

data class CardsButtons (
    val userID: String = "",
    val deckID: String = "",
    val frontText: String = "",
    val backText: String = "",
    val description: String = "",
    var color: String = "",
    val dateAdded: String = "",
    val lastUpdated: String = "",
    val timeAdded: String = "",
    val deleteStatus: Boolean = false,
    var lastTimeUpdated: String = ""
)