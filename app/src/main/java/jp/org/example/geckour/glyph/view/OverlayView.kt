package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView: View {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

    enum class MessageType {
        WAIT_COMMAND
    }

    private val paint = Paint()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        paint.isAntiAlias = true
    }

    fun drawMessage(type: MessageType) {
        // TODO: メッセージ描画
        invalidate()
    }
}