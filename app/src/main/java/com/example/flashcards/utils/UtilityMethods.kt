package com.example.flashcards.utils

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.example.flashcards.cards.Cards
import com.example.flashcards.decks.Decks
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class UtilityMethods {
    companion object {
        private fun calculateBrightness(hexColor: String): Double {
            // Remove the '#' symbol from the hex color code
            val color = hexColor.removePrefix("#")

            // Convert the hex color to RGB values
            val red = color.substring(0, 2).toInt(16)
            val green = color.substring(2, 4).toInt(16)
            val blue = color.substring(4, 6).toInt(16)

            // Calculate the brightness using the RGB values

            return (red * 299 + green * 587 + blue * 114) / 1000.0
        }

        @JvmStatic
        fun determineTextColor(hexColor: String): Int {
            val brightness = calculateBrightness(hexColor)

            // Compare the brightness to a threshold and return white or black accordingly
            return if (brightness > 128) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }

        @JvmStatic
        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            }

            return false
        }

        @JvmStatic
        fun generateColor(): String {
            val stringColor = StringBuilder()
            stringColor.append('#')

            for (i in 0..5) {
                var color = Random.nextInt(0, 16)

                if (color >= 10) {
                    color += 55
                    val hex = color.toChar()
                    stringColor.append(hex)
                }
                else {
                    stringColor.append(color)
                }
            }

            return stringColor.toString()
        }

        @JvmStatic
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            }

            return false
        }

        @JvmStatic
        fun getCurrentDateAsString(): String {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            return dateFormat.format(Date())
        }

        @JvmStatic
        fun getCurrentTimeAsString(): String {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return timeFormat.format(Date())
        }

        @JvmStatic
        fun mergeSortCards(cards: ArrayList<Cards>, comparator: Comparator<Cards>) {
            if (cards.size <= 1) {
                return
            }

            val middle = cards.size / 2
            val left = ArrayList(cards.subList(0, middle))
            val right = ArrayList(cards.subList(middle, cards.size))

            mergeSortCards(left, comparator)
            mergeSortCards(right, comparator)

            mergeCards(cards, left, right, comparator)
        }

        @JvmStatic
        private fun mergeCards(cards: ArrayList<Cards>, left: ArrayList<Cards>, right: ArrayList<Cards>, comparator: Comparator<Cards>) {
            var leftIndex = 0
            var rightIndex = 0
            var mergedIndex = 0

            while (leftIndex < left.size && rightIndex < right.size) {
                if (comparator.compare(left[leftIndex], right[rightIndex]) < 0) {
                    cards[mergedIndex] = left[leftIndex]
                    leftIndex++
                } else {
                    cards[mergedIndex] = right[rightIndex]
                    rightIndex++
                }
                mergedIndex++
            }

            while (leftIndex < left.size) {
                cards[mergedIndex] = left[leftIndex]
                leftIndex++
                mergedIndex++
            }

            while (rightIndex < right.size) {
                cards[mergedIndex] = right[rightIndex]
                rightIndex++
                mergedIndex++
            }
        }

        @JvmStatic
        fun mergeSortDecks(decks: ArrayList<Decks>, comparator: Comparator<Decks>) {
            val TAG = "Utility Methods"
            Log.e(TAG, "Sorting Decks Using $comparator")

            if (decks.size <= 1) {
                return
            }

            val middle = decks.size / 2
            val left = ArrayList(decks.subList(0, middle))
            val right = ArrayList(decks.subList(middle, decks.size))

            mergeSortDecks(left, comparator)
            mergeSortDecks(right, comparator)

            mergeDecks(decks, left, right, comparator)
        }

        @JvmStatic
        private fun mergeDecks(decks: ArrayList<Decks>, left: ArrayList<Decks>, right: ArrayList<Decks>, comparator: Comparator<Decks>) {
            var leftIndex = 0
            var rightIndex = 0
            var mergedIndex = 0

            while (leftIndex < left.size && rightIndex < right.size) {
                if (comparator.compare(left[leftIndex], right[rightIndex]) <= 0) {
                    decks[mergedIndex] = left[leftIndex]
                    leftIndex++
                } else {
                    decks[mergedIndex] = right[rightIndex]
                    rightIndex++
                }
                mergedIndex++
            }

            while (leftIndex < left.size) {
                decks[mergedIndex] = left[leftIndex]
                leftIndex++
                mergedIndex++
            }

            while (rightIndex < right.size) {
                decks[mergedIndex] = right[rightIndex]
                rightIndex++
                mergedIndex++
            }
        }

        @JvmStatic
        fun signOut(context: Context) {
            FirebaseAuth.getInstance().signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut()
        }
    }
}