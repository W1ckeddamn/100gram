package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiController;
import org.telegram.messenger.ai.Role;
import org.telegram.messenger.ai.Suggestions;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

public class HundredGramAiPromptBottomSheet extends BottomSheet {

    private final BaseFragment parentFragment;
    private final String messageText;
    private final EditTextBoldCursor inputField;
    private Role selectedRole = Suggestions.ASSISTANT.getRole();

    public HundredGramAiPromptBottomSheet(BaseFragment fragment, MessageObject messageObject) {
        this(fragment, getMessageText(messageObject));
    }

    public HundredGramAiPromptBottomSheet(BaseFragment fragment, String text) {
        super(fragment.getParentActivity(), true, fragment.getResourceProvider());
        this.parentFragment = fragment;
        this.messageText = text != null ? text : "";

        Context context = fragment.getParentActivity();
        int bgKey = Theme.key_dialogBackground;
        setBackgroundColor(getThemedColor(bgKey));
        fixNavigationBar(getThemedColor(bgKey));
        setApplyTopPadding(false);

        LinearLayout contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // Header
        FrameLayout headerLayout = new FrameLayout(context);
        TextView titleView = new TextView(context);
        titleView.setText("100gram AI");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        headerLayout.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 0, 0, 48, 0));

        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.ic_close_white);
        closeButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogTextBlack), PorterDuff.Mode.SRC_IN));
        ScaleStateListAnimator.apply(closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        headerLayout.addView(closeButton, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.END));

        contentView.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Quote Box (Message excerpt)
        if (!TextUtils.isEmpty(messageText)) {
            LinearLayout quoteLayout = new LinearLayout(context);
            quoteLayout.setOrientation(LinearLayout.HORIZONTAL);
            quoteLayout.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.multAlpha(getThemedColor(Theme.key_dialogTextBlack), 0.06f)));
            quoteLayout.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));

            View bar = new View(context);
            bar.setBackgroundColor(getThemedColor(Theme.key_featuredStickers_addButton));
            quoteLayout.addView(bar, LayoutHelper.createLinear(3, LayoutHelper.MATCH_PARENT, 0, 0, 8, 0));

            TextView quoteText = new TextView(context);
            quoteText.setText(messageText.length() > 200 ? messageText.substring(0, 200) + "…" : messageText);
            quoteText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            quoteText.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            quoteText.setMaxLines(3);
            quoteText.setEllipsize(TextUtils.TruncateAt.END);
            quoteLayout.addView(quoteText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            contentView.addView(quoteLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        }

        // Quick action chips
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsLayout = new LinearLayout(context);
        chipsLayout.setOrientation(LinearLayout.HORIZONTAL);

        addChip(chipsLayout, R.drawable.msg_edit, LocaleController.getString("AiChipSummarize", R.string.AiChipSummarize), Suggestions.SUMMARIZER.getRole(), true);
        addChip(chipsLayout, R.drawable.msg_instant, LocaleController.getString("AiChipProofread", R.string.AiChipProofread), Suggestions.PROOFREADER.getRole(), false);
        addChip(chipsLayout, R.drawable.msg_bot, LocaleController.getString("AskAssistant", R.string.AskAssistant), Suggestions.ASSISTANT.getRole(), false);
        addChip(chipsLayout, R.drawable.msg_translate, LocaleController.getString("AiChipTranslate", R.string.AiChipTranslate), Suggestions.TRANSLATOR.getRole(), false);

        chipsScroll.addView(chipsLayout, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START));
        contentView.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Input field for custom prompt
        inputField = new EditTextBoldCursor(context);
        inputField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        inputField.setHint(LocaleController.getString("AiPromptHint", R.string.AiPromptHint));
        inputField.setHintTextColor(getThemedColor(Theme.key_dialogTextGray3));
        inputField.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        inputField.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.multAlpha(getThemedColor(Theme.key_dialogTextBlack), 0.05f)));
        inputField.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        inputField.setMaxLines(4);
        contentView.addView(inputField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Submit Button
        ButtonWithCounterView sendButton = new ButtonWithCounterView(context, fragment.getResourceProvider());
        sendButton.setText(LocaleController.getString("AiGenerate", R.string.AiGenerate), false);
        sendButton.setOnClickListener(v -> {
            String customPrompt = inputField.getText() != null ? inputField.getText().toString().trim() : "";
            String fullPrompt;
            if (!TextUtils.isEmpty(customPrompt)) {
                fullPrompt = !TextUtils.isEmpty(messageText) ? customPrompt + "\n\n" + messageText : customPrompt;
            } else {
                fullPrompt = messageText;
            }

            dismiss();
            if (parentFragment != null && !TextUtils.isEmpty(fullPrompt)) {
                parentFragment.showDialog(new HundredGramAiResponseBottomSheet(parentFragment, fullPrompt, selectedRole));
            }
        });
        contentView.addView(sendButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        setCustomView(contentView);
    }

    private void addChip(LinearLayout container, int iconRes, String title, Role role, boolean autoSelect) {
        TextView chip = new TextView(container.getContext());
        chip.setText(title);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f)));
        chip.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        ScaleStateListAnimator.apply(chip);

        Runnable updateIcon = () -> {
            boolean isSelected = selectedRole == role;
            int tintColor = isSelected ? getThemedColor(Theme.key_featuredStickers_buttonText) : getThemedColor(Theme.key_featuredStickers_addButton);
            Drawable icon = ContextCompat.getDrawable(container.getContext(), iconRes);
            if (icon != null) {
                icon = icon.mutate();
                icon.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
                icon.setBounds(0, 0, AndroidUtilities.dp(14), AndroidUtilities.dp(14));
                chip.setCompoundDrawables(icon, null, null, null);
                chip.setCompoundDrawablePadding(AndroidUtilities.dp(5));
            }
        };

        chip.setOnClickListener(v -> {
            selectedRole = role;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f)));
                    ((TextView) child).setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
                    Object tag = child.getTag();
                    if (tag instanceof Runnable) {
                        ((Runnable) tag).run();
                    }
                }
            }
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_featuredStickers_addButton)));
            chip.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
            updateIcon.run();

            // Instant prompt if empty
            if (role == Suggestions.SUMMARIZER.getRole()) {
                inputField.setText(LocaleController.getString("SummarizeThisMessage", R.string.SummarizeThisMessage));
            } else if (role == Suggestions.PROOFREADER.getRole()) {
                inputField.setText(LocaleController.getString("ProofreadThisMessage", R.string.ProofreadThisMessage));
            } else if (role == Suggestions.TRANSLATOR.getRole()) {
                inputField.setText(LocaleController.getString("TranslateThisMessage", R.string.TranslateThisMessage));
            }
        });

        chip.setTag(updateIcon);
        if (autoSelect) {
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_featuredStickers_addButton)));
            chip.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        }
        updateIcon.run();

        container.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private static String getMessageText(MessageObject msg) {
        if (msg == null) return "";
        CharSequence text = msg.messageText != null ? msg.messageText : msg.caption;
        if (text == null && msg.messageOwner != null) {
            text = msg.messageOwner.message;
        }
        return text != null ? text.toString() : "";
    }
}
