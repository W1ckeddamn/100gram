package org.telegram.ui.Components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HundredGramJsonViewerBottomSheet extends BottomSheet {

    private final String rawJson;
    private String formattedJson;
    private String minifiedJson;
    private boolean isMinified;
    private TextView jsonTextView;
    private final BaseFragment fragment;

    public static boolean isJson(CharSequence raw) {
        if (raw == null) return false;
        String text = raw.toString().trim();
        if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
            try {
                if (text.startsWith("{")) {
                    new JSONObject(text);
                } else {
                    new JSONArray(text);
                }
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public HundredGramJsonViewerBottomSheet(BaseFragment fragment, String json) {
        super(fragment.getContext(), false, fragment.getResourceProvider());
        this.fragment = fragment;
        this.rawJson = json != null ? json.trim() : "";

        try {
            if (rawJson.startsWith("{")) {
                JSONObject obj = new JSONObject(rawJson);
                this.formattedJson = obj.toString(2);
                this.minifiedJson = obj.toString();
            } else if (rawJson.startsWith("[")) {
                JSONArray arr = new JSONArray(rawJson);
                this.formattedJson = arr.toString(2);
                this.minifiedJson = arr.toString();
            } else {
                this.formattedJson = rawJson;
                this.minifiedJson = rawJson;
            }
        } catch (Exception e) {
            this.formattedJson = rawJson;
            this.minifiedJson = rawJson;
        }

        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        Context context = fragment.getContext();
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_view_file);
        icon.setColorFilter(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        header.addView(icon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("PrettyJsonTitle", R.string.PrettyJsonTitle));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        header.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        ImageView closeBtn = new ImageView(context);
        closeBtn.setImageResource(R.drawable.msg_close);
        closeBtn.setColorFilter(Theme.getColor(Theme.key_dialogTextGray2, resourcesProvider));
        closeBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), 1));
        closeBtn.setOnClickListener(v -> dismiss());
        header.addView(closeBtn, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL));

        root.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Code content area
        FrameLayout codeContainer = new FrameLayout(context);
        codeContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider)));
        codeContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        ScrollView vScroll = new ScrollView(context);
        HorizontalScrollView hScroll = new HorizontalScrollView(context);

        jsonTextView = new TextView(context);
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        jsonTextView.setTypeface(Typeface.MONOSPACE);
        jsonTextView.setTextIsSelectable(true);
        updateJsonText();

        hScroll.addView(jsonTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        vScroll.addView(hScroll, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        codeContainer.addView(vScroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 260));

        root.addView(codeContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 260, 0, 0, 0, 14));

        // Bottom action buttons
        LinearLayout buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView formatToggleBtn = createButton(context, LocaleController.getString("PrettyJsonMinify", R.string.PrettyJsonMinify), false);
        formatToggleBtn.setOnClickListener(v -> {
            isMinified = !isMinified;
            formatToggleBtn.setText(isMinified ? LocaleController.getString("PrettyJsonCopyFormatted", R.string.PrettyJsonCopyFormatted) : LocaleController.getString("PrettyJsonMinify", R.string.PrettyJsonMinify));
            updateJsonText();
        });
        buttonRow.addView(formatToggleBtn, LayoutHelper.createLinear(0, 44, 1f, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        TextView copyBtn = createButton(context, LocaleController.getString("Copy", R.string.Copy), true);
        copyBtn.setOnClickListener(v -> {
            String toCopy = isMinified ? minifiedJson : formattedJson;
            AndroidUtilities.addToClipboard(toCopy);
            if (fragment != null) {
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("PrettyJsonCopied", R.string.PrettyJsonCopied)).show();
            }
            dismiss();
        });
        buttonRow.addView(copyBtn, LayoutHelper.createLinear(0, 44, 1.2f, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        root.addView(buttonRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
    }

    private void updateJsonText() {
        String content = isMinified ? minifiedJson : formattedJson;
        jsonTextView.setText(highlightJson(content));
    }

    private CharSequence highlightJson(String json) {
        if (json == null || json.isEmpty()) return "";

        SpannableStringBuilder ssb = new SpannableStringBuilder(json);

        // Pattern for JSON tokens:
        // Group 1: key ("key":)
        // Group 2: string ("string")
        // Group 3: number (123, -12.34)
        // Group 4: boolean/null (true, false, null)
        Pattern pattern = Pattern.compile("(\"[^\"]+\"\\s*:)|(\"[^\"]*\")|(-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b)|(\\b(true|false|null)\\b)");
        Matcher matcher = pattern.matcher(json);

        int keyColor = Theme.getColor(Theme.key_chat_messageLinkIn, resourcesProvider);
        int stringColor = 0xFF43A047; // Green
        int numberColor = 0xFFFB8C00; // Orange
        int boolColor = 0xFF8E24AA;   // Purple

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Key
                ssb.setSpan(new ForegroundColorSpan(keyColor), matcher.start(1), matcher.end(1) - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(2) != null) {
                // String
                ssb.setSpan(new ForegroundColorSpan(stringColor), matcher.start(2), matcher.end(2), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(3) != null) {
                // Number
                ssb.setSpan(new ForegroundColorSpan(numberColor), matcher.start(3), matcher.end(3), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(6) != null) {
                // Boolean or null
                ssb.setSpan(new ForegroundColorSpan(boolColor), matcher.start(6), matcher.end(6), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return ssb;
    }

    private TextView createButton(Context context, String text, boolean accent) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);

        int bgColor = accent ? Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider) : Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider);
        int textColor = accent ? Theme.getColor(Theme.key_featuredStickers_buttonText, resourcesProvider) : Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider);

        button.setTextColor(textColor);
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), bgColor));
        return button;
    }
}
