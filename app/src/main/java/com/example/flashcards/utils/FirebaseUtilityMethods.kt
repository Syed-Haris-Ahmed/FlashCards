package com.example.flashcards.utils

import com.example.flashcards.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseUtilityMethods {
    companion object {
        @JvmStatic
        fun signUpWithEmailPassword(username: String, email: String, password: String, callback: (Boolean) -> Unit) {
            val usersCollectionRef = Firebase.firestore.collection("users")
            val auth = FirebaseAuth.getInstance()
            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(username, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            val uid = user?.uid
                            if (uid != null) {
                                val userDoc = usersCollectionRef.document(user.uid)

                                val userData = mapOf(
                                    "userID" to user.uid,
                                    "username" to username,
                                    "email" to email
                                )

                                userDoc.set(userData)
                                    .addOnCompleteListener { firestoreTask ->
                                        if (firestoreTask.isSuccessful) {
                                            callback(true)
                                        } else {
                                            callback(false)
                                        }
                                    }
                            }
                        } else {
                            callback(false)
                        }
                    }
            } else {
                callback(false)
            }
        }
    }
}