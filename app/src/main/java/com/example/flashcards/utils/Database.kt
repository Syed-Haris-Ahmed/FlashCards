package com.example.flashcards.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Database(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "test3.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users table
        db.execSQL(
            "CREATE TABLE Users(" +
                    "    user_id INTEGER PRIMARY KEY, " +
                    "    username TEXT UNIQUE NOT NULL, " +
                    "    email TEXT UNIQUE NOT NULL" +
                    ")"
        )

        db.execSQL(
            "CREATE TABLE Decks (" +
                    "    deck_id INTEGER NOT NULL, " +
                    "    user_id INTEGER NOT NULL, " +
                    "    title TEXT NOT NULL, " +
                    "    color TEXT, " +
                    "    number_of_cards NUMBER, " +
                    "    date_added DATE NOT NULL, " +
                    "    last_updated DATE NOT NULL, " +
                    "    synced TEXT NOT NULL DEFAULT 'false', " +
                    "    FOREIGN KEY (user_id) REFERENCES Users (user_id)," +
                    "    PRIMARY KEY (deck_id, user_id)" +
                    ")"
        )

        db.execSQL(
            "CREATE TABLE Cards ("+
                    "    card_id INTEGER NOT NULL, " +
                    "    deck_id INTEGER NOT NULL, " +
                    "    front_text TEXT NOT NULL, " +
                    "    back_text TEXT, " +
                    "    description TEXT, " +
                    "    color TEXT, " +
                    "    date_added DATE NOT NULL, " +
                    "    last_updated DATE NOT NULL, " +
                    "    synced TEXT NOT NULL DEFAULT 'false', " +
                    "    FOREIGN KEY (deck_id) REFERENCES Decks (deck_id), " +
                    "    PRIMARY KEY (card_id, deck_id) " +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade the schema of the database here
    }
}







