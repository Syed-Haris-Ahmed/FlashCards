package com.example.flashcards.cards

data class Cards (
    val cardID: String = "",
    val deckID: String = "",
    val userID: String = "",
    var frontText: String = "",
    var backText: String = "",
    var description: String = "",
    var color: String = "",
    val dateAdded: String = "",
    var lastUpdated: String = "",
    val timeAdded: String = "",
    val deleteStatus: Boolean = false,
    var lastTimeUpdated: String = ""
)