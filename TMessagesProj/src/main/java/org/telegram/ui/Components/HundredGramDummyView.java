package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import org.telegram.messenger.MessageObject;

@SuppressLint("ViewConstructor")
public class HundredGramDummyView extends View {
    private MessageObject messageObject;

    public HundredGramDummyView(Context context) {
        super(context);
        setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    public MessageObject getMessageObject() {
        return messageObject;
    }

    public void setMessageObject(MessageObject messageObject) {
        this.messageObject = messageObject;
    }
}
