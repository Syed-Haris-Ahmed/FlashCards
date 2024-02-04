package com.example.flashcards.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.adapters.FlashCardsAdapter

class SwipeToDeleteCallback(private val adapter: FlashCardsAdapter) : ItemTouchHelper.SimpleCallback(
    0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Return false because we're not handling moves
        return false
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition

        val confirmationDialog = Dialog(adapter.context)
        confirmationDialog.setContentView(R.layout.dialog_confirmation_deletion)
        confirmationDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val confirmationTextView: TextView = confirmationDialog.findViewById(R.id.confirmation_text)
        val confirmButton: ImageButton = confirmationDialog.findViewById(R.id.confirmButton)
        val cancelButton: ImageButton = confirmationDialog.findViewById(R.id.cancel_button)

        var buttonTitle = adapter.getData(position).frontText
        if (buttonTitle.length > 27) {
            buttonTitle = buttonTitle.substring(0, 27) + "..."
        }
        confirmationTextView.text = "$buttonTitle will be deleted forever"

        confirmButton.setOnClickListener {
            adapter.deleteCard(position)
            confirmationDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            adapter.notifyItemChanged(position)
            confirmationDialog.dismiss()
        }

        confirmationDialog.show()
    }

}
