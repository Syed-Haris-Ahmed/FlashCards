package com.example.flashcards.adapters

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.cards.Cards
import com.example.flashcards.cards.FlashCardButtons
import com.example.flashcards.viewmodels.FlashCardsViewModel
import com.example.flashcards.utils.UtilityMethods
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale


class FlashCardsAdapter(
    val context: Context,
    private var data: ArrayList<Cards>,
    private val cardViewModel: FlashCardsViewModel,
    userID: String,
    deckID: String
) : RecyclerView.Adapter<FlashCardsAdapter.ViewHolder>() {

    private var filteredData = ArrayList<Cards>(data)
    private val flipFlags = MutableList(filteredData.size) { false }
    private val isAnimating = MutableList(filteredData.size) { false }

    private val userCollectionRef = Firebase.firestore.collection("users")
    private val deckCollectionRef = userCollectionRef.document(userID).collection("decks")
    private val cardCollectionRef = deckCollectionRef.document(deckID).collection("cards")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cards_layout, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = filteredData[position]

        holder.bindCard(card)

        holder.itemView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    cardViewModel.swipeRefreshLayout.isEnabled = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cardViewModel.swipeRefreshLayout.isEnabled = true
                }
            }
            false
        }

        holder.titleView.setOnClickListener {
            flipCard(
                holder.titleView,
                holder.editButton,
                holder.descriptionButton,
                holder.deleteButton,
                position
            )
        }

        holder.editButton.setOnClickListener {
            editCardConfirmation(card, position, holder.titleView)
        }

        holder.deleteButton.setOnClickListener {
            deleteConfirmation(card, position)
        }

        holder.descriptionButton.setOnClickListener {
            showDescription(card)
        }
    }

    private fun flipCard(cardButton: FlashCardButtons, editButton: Button, descriptionButton: Button, deleteButton: Button, position: Int) {
        if (isAnimating[position]) return

        val duration: Long = 500 // Animation duration in milliseconds
        isAnimating[position] = true

        if (!flipFlags[position]) {
            // Flip from front to back
            ObjectAnimator.ofFloat(cardButton, "rotationY", 0f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                cardButton.setBackTextAsMain()
                ObjectAnimator.ofFloat(cardButton, "rotationY", -90f, 0f).apply {
                    this.duration = duration / 2
                    start()
                }.doOnEnd {
                    isAnimating[position] = false // Animation completed, set the flag to false
                }
            }

            // Apply the same flip animation to other buttons
            ObjectAnimator.ofFloat(editButton, "rotationY", 0f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(editButton, "rotationY", 90f, 180f).apply {
                    this.duration = duration / 2
                    start()
                }
            }

            ObjectAnimator.ofFloat(descriptionButton, "rotationY", 0f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(descriptionButton, "rotationY", 90f, 180f).apply {
                    this.duration = duration / 2
                    start()
                }
            }

            ObjectAnimator.ofFloat(deleteButton, "rotationY", 0f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(deleteButton, "rotationY", 90f, 180f).apply {
                    this.duration = duration / 2
                    start()
                }
            }

        } else {
            // Flip from back to front
            ObjectAnimator.ofFloat(cardButton, "rotationY", 0f, -90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                cardButton.setFrontTextAsMain()
                ObjectAnimator.ofFloat(cardButton, "rotationY", 90f, 0f).apply {
                    this.duration = duration / 2
                    start()
                }.doOnEnd {
                    isAnimating[position] = false // Animation completed, set the flag to false
                }
            }

            // Apply the same flip animation to other buttons
            ObjectAnimator.ofFloat(editButton, "rotationY", 180f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(editButton, "rotationY", 90f, 0f).apply {
                    this.duration = duration / 2
                    start()
                }
            }

            ObjectAnimator.ofFloat(descriptionButton, "rotationY", 180f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(descriptionButton, "rotationY", 90f, 0f).apply {
                    this.duration = duration / 2
                    start()
                }
            }

            ObjectAnimator.ofFloat(deleteButton, "rotationY", 180f, 90f).apply {
                this.duration = duration / 2
                start()
            }.doOnEnd {
                ObjectAnimator.ofFloat(deleteButton, "rotationY", 90f, 0f).apply {
                    this.duration = duration / 2
                    start()
                }
            }
        }
        flipFlags[position] = !flipFlags[position]
    }

    private fun ViewHolder.bindCard(card: Cards) {
        titleView.text = card.frontText
        titleView.setFrontAndBackText(card.frontText, card.backText)
        titleView.description = card.description

        val roundedDrawable = titleView.background as GradientDrawable
        val bgColor = card.color
        roundedDrawable.setColor(Color.parseColor(bgColor))

        val txtColor = UtilityMethods.determineTextColor(bgColor)
        val colorStateList = ColorStateList.valueOf(txtColor)

        titleView.setTextColor(txtColor)
        editButton.backgroundTintList = colorStateList
        descriptionButton.backgroundTintList = colorStateList
        deleteButton.backgroundTintList = colorStateList
    }

    private fun editCardConfirmation(card: Cards, position: Int, titleView: FlashCardButtons) {
        val oldFrontText = card.frontText
        val oldBackText = card.backText
        val oldDescriptionText = card.description

        val editCardDialog = Dialog(context)
        editCardDialog.setContentView(R.layout.dialog_box_add_card)
        editCardDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val title: TextView = editCardDialog.findViewById(R.id.title)
        title.text = context.getString(R.string.edit_card)

        val newFrontEditText: EditText = editCardDialog.findViewById(R.id.front_text)
        val newBackEditText: EditText = editCardDialog.findViewById(R.id.back_text)
        val newDescriptionEditText: EditText = editCardDialog.findViewById(R.id.description)

        newFrontEditText.setText(oldFrontText)
        newBackEditText.setText(oldBackText)
        newDescriptionEditText.setText(oldDescriptionText)

        val confirmButton: ImageButton = editCardDialog.findViewById(R.id.add_button)
        val cancelButton: ImageButton = editCardDialog.findViewById(R.id.cancel_button)
        confirmButton.setImageResource(R.drawable.ic_edit_button)

        confirmButton.setOnClickListener {
            val newFrontText = newFrontEditText.text.toString()
            val newBackText = newBackEditText.text.toString()
            val newDescriptionText = newDescriptionEditText.text.toString()

            val newUpdateDate = UtilityMethods.getCurrentDateAsString()
            val newUpdateTime = UtilityMethods.getCurrentTimeAsString()

            val updatedFields = mutableMapOf<String, Any>()

            if (newFrontText.isNotEmpty() && newFrontText != oldFrontText) {
                card.frontText = newFrontText
                updatedFields["frontText"] = newFrontText
                titleView.frontText = newFrontText
                if (!flipFlags[position]) titleView.text = newFrontText
            }

            if (newBackText.isNotEmpty() && newBackText != oldBackText) {
                card.backText = newBackText
                updatedFields["backText"] = newBackText
                titleView.backText = newBackText
                if (flipFlags[position]) titleView.text = newBackText
            }

            if (newDescriptionText.isNotEmpty() && newDescriptionText != oldDescriptionText) {
                card.description = newDescriptionText
                updatedFields["description"] = newDescriptionText
                titleView.description = newDescriptionText
            }

            if (updatedFields.isNotEmpty()) {
                card.lastUpdated = newUpdateDate
                card.lastTimeUpdated = newUpdateTime
                updatedFields["lastUpdated"] = newUpdateDate
                updatedFields["lastTimeUpdated"] = newUpdateTime
                editCard(card)
            }

            editCardDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            editCardDialog.dismiss()
        }

        editCardDialog.show()
    }

    private fun showDescription(card: Cards) {
        val descriptionDialog = Dialog(context)
        descriptionDialog.setContentView(R.layout.dialog_box_description)
        descriptionDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val minimizeButton = descriptionDialog.findViewById<Button>(R.id.minimize_button)
        val descriptionText = descriptionDialog.findViewById<TextView>(R.id.description)
        descriptionText.text = card.description
        minimizeButton.setOnClickListener {
            descriptionDialog.dismiss()
        }
        descriptionDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteConfirmation(card: Cards, position: Int) {
        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(R.layout.dialog_confirmation_deletion)
        confirmationDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val confirmationTextView: TextView = confirmationDialog.findViewById(R.id.confirmation_text)
        val confirmButton: ImageButton = confirmationDialog.findViewById(R.id.confirmButton)
        val cancelButton: ImageButton = confirmationDialog.findViewById(R.id.cancel_button)

        var buttonTitle = card.frontText
        if (buttonTitle.length > 27) {
            buttonTitle = buttonTitle.substring(0, 27) + "..."
        }
        confirmationTextView.text = "$buttonTitle will be deleted forever"

        confirmButton.setOnClickListener {
            deleteCard(position)
            confirmationDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            notifyItemChanged(position)
            confirmationDialog.dismiss()
        }

        confirmationDialog.show()
    }

    private fun editCard(card: Cards) {
        val newFrontText = card.frontText
        val newBackText = card.backText
        val newDescriptionText = card.description
        val newUpdateDate = UtilityMethods.getCurrentDateAsString()
        val newUpdateTime = UtilityMethods.getCurrentTimeAsString()

        val newCardData = mutableMapOf<String, Any>()

        if (newFrontText.isNotEmpty()) {
            newCardData["frontText"] = newFrontText
            newCardData["lastUpdated"] = newUpdateDate
            newCardData["lastTimeUpdated"] = newUpdateTime
        }
        if (newBackText.isNotEmpty()) {
            newCardData["backText"] = newBackText
            newCardData["lastUpdated"] = newUpdateDate
            newCardData["lastTimeUpdated"] = newUpdateTime
        }
        if (newDescriptionText.isNotEmpty()) {
            newCardData["description"] = newDescriptionText
            newCardData["lastUpdated"] = newUpdateDate
            newCardData["lastTimeUpdated"] = newUpdateTime
        }

        if (newCardData.isNotEmpty()) {
            cardCollectionRef.document(card.cardID)
                .update(newCardData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Card updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val exception = task.exception
                        Toast.makeText(context, "Error updating card: " + exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    fun getData(position: Int): Cards {
        return filteredData[position]
    }

    fun deleteCard(position: Int) {
        val deletedItem = filteredData.removeAt(position)
        notifyItemRemoved(position)

        val newCardData = mapOf(
            "deleteStatus" to true
        )

        cardCollectionRef.document(deletedItem.cardID)
            .update(newCardData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Card deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    val exception = task.exception
                    Toast.makeText(context, "Error deleting card: " + exception?.message, Toast.LENGTH_LONG).show()
                }
            }

        Toast.makeText(context, "Deleted: ${deletedItem.frontText}", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(text: String) {
        val searchText = text.lowercase(Locale.getDefault())
        val tempList = ArrayList<Cards>()

        if (searchText.isEmpty()) {
            tempList.addAll(data)
        } else {
            for (item in data) {
                if (item.frontText.lowercase(Locale.getDefault()).contains(searchText) ||
                    item.backText.lowercase(Locale.getDefault()).contains(searchText)
                ) {
                    tempList.add(item)
                }
            }
        }

        filteredData.clear()
        filteredData.addAll(tempList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: FlashCardButtons = itemView.findViewById(R.id.card)
        val editButton: Button = itemView.findViewById(R.id.edit_card_button)
        val descriptionButton: Button = itemView.findViewById(R.id.descriptionViewButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newDataList: ArrayList<Cards>) {
        filteredData = ArrayList(newDataList)
        flipFlags.clear()
        flipFlags.addAll(MutableList(filteredData.size) { false })
        isAnimating.clear()
        isAnimating.addAll(MutableList(filteredData.size) { false })
        notifyDataSetChanged()
    }
}
