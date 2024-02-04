package com.example.flashcards.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.utils.UtilityMethods
import com.example.flashcards.screens.FlashCardsActivity
import com.example.flashcards.decks.Decks
import com.example.flashcards.fragments.DecksFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class DecksAdapter(private val decksFragment: DecksFragment, var data: List<Decks>, private val context: Context) : RecyclerView.Adapter<DecksAdapter.ViewHolder>() {
    private var selectingDecks = false
    var selectedDeckCounter = 0

    private lateinit var adapterViewHolder: ViewHolder

    var filteredData = ArrayList<Decks>()

    init {
        filteredData.addAll(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.details_layout, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        adapterViewHolder = holder

        holder.titleView.text = filteredData[position].title

        val bgColor = filteredData[position].color
        val roundedDrawable = holder.titleView.background as GradientDrawable
        roundedDrawable.setColor(Color.parseColor(bgColor))

        val accentColor = UtilityMethods.determineTextColor(bgColor)
        holder.titleView.setTextColor(accentColor)
        val tintList = ColorStateList.valueOf(accentColor)
        holder.checkBox.buttonTintList = tintList

        holder.titleView.setOnLongClickListener {
            if (!selectingDecks) {
                selectingDecks = true
                decksFragment.deleteFab.visibility = View.VISIBLE
                holder.checkBox.visibility = View.VISIBLE
                holder.checkBox.isChecked = !holder.checkBox.isChecked
                filteredData[position].isSelected = true
                selectedDeckCounter++
                println("Selected Decks: $selectedDeckCounter")
                notifyDataSetChanged()
            }
            true
        }

        holder.titleView.setOnClickListener {
            println(selectingDecks)
            if (selectingDecks) {
                println("In first if block")
                if (holder.checkBox.isChecked) {
                    println("In Second if block")
                    selectedDeckCounter--
                    holder.checkBox.isChecked = false
                } else {
                    println("In second else block")
                    selectedDeckCounter++
                    holder.checkBox.isChecked = true
                }
                filteredData[position].isSelected = !filteredData[position].isSelected
                notifyDataSetChanged()
            } else {
                Toast.makeText(holder.itemView.context, "Deck ID: " + filteredData[position].deckID, Toast.LENGTH_SHORT).show()
                val intent = Intent(holder.itemView.context, FlashCardsActivity::class.java)
                intent.putExtra("deckID", filteredData[position].deckID)
                intent.putExtra("deckTitle", filteredData[position].title)
                holder.itemView.context.startActivity(intent)
            }
            println("Selected Decks: $selectedDeckCounter")
            if (selectedDeckCounter <= 0) {
                holder.checkBox.visibility = View.GONE
                selectingDecks = false
                decksFragment.deleteFab.visibility = View.GONE
            }
        }

        // Set checkbox state based on isSelected flag
        holder.checkBox.isChecked = filteredData[position].isSelected
        holder.checkBox.visibility = if (selectingDecks) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(text: String) {
        val searchText = text.lowercase(Locale.getDefault())
        val tempList = ArrayList<Decks>()

        if (searchText.isEmpty()) {
            tempList.addAll(data)
        } else {
            for (item in data) {
                if (item.title.lowercase(Locale.getDefault()).contains(searchText)) {
                    tempList.add(item)
                }
            }
        }

        filteredData.clear()
        filteredData.addAll(tempList)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newDataList: List<Decks>) {
        filteredData = ArrayList(newDataList)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun deleteSelectedDecks() {
        var size = filteredData.size
        var i = 0
        while (i < size) {
            if (filteredData[i].isSelected) {
                println("Removing Deck: " + filteredData[i].title)
                deleteDeck(filteredData[i])
                filteredData.removeAt(i)
                size = filteredData.size
                i--
            }
            i++
        }
        notifyDataSetChanged()
        adapterViewHolder.checkBox.visibility = View.GONE
        selectingDecks = false
    }

    private fun deleteDeck(deck: Decks) {
        val deckCollectionRef = Firebase.firestore.collection("users").document(deck.userID).collection("decks")

        val newDeckData = mapOf(
            "deleteStatus" to true
        )

        deckCollectionRef.document(deck.deckID)
            .update(newDeckData)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    // Update was successful
                    Toast.makeText(context, "Deck deleted successfully", Toast.LENGTH_SHORT).show()

                } else {
                    val exception = task.exception
                    Toast.makeText(context, "Error deleting deck: " + exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: Button = itemView.findViewById(R.id.title)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

}
