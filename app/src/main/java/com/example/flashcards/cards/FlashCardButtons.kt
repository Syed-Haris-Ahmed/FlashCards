package com.example.flashcards.cards

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class FlashCardButtons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {
    var frontText: String? = null
    var backText: String? = null
    var description: String? = null

    fun setFrontAndBackText(frontText: String, backText: String) {
        this.frontText = frontText
        this.backText = backText
    }

    fun setFrontTextAsMain() {
        this.text = frontText
    }

    fun setBackTextAsMain() {
        this.text = backText
    }
}