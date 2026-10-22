package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramRegexSearchController;
import org.telegram.messenger.HundredGramRegexSearchController.Template;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramRegexPresetsBottomSheet extends BottomSheet {

    public interface OnTemplateSelectedListener {
        void onTemplateSelected(String pattern);
    }

    private final OnTemplateSelectedListener listener;
    private LinearLayout customContainer;

    public HundredGramRegexPresetsBottomSheet(Context context, OnTemplateSelectedListener listener) {
        super(context, true);
        this.listener = listener;

        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        NestedScrollView scrollView = new NestedScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(24));

        // Header Title
        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("RegexSearchPresets", R.string.RegexSearchPresets));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        contentLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 16));

        // Built-in Templates Section
        ArrayList<Template> builtIn = HundredGramRegexSearchController.getBuiltInTemplates();
        for (Template t : builtIn) {
            contentLayout.addView(createTemplateRow(context, t));
        }

        // Custom Templates Section Header & Add Button
        LinearLayout customHeaderLayout = new LinearLayout(context);
        customHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        customHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView customHeader = new TextView(context);
        customHeader.setText(LocaleController.getString("RegexCustomTemplates", R.string.RegexCustomTemplates));
        customHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        customHeader.setTypeface(AndroidUtilities.bold());
        customHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        customHeaderLayout.addView(customHeader, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView addBtn = new TextView(context);
        addBtn.setText("+ " + LocaleController.getString("RegexAddCustom", R.string.RegexAddCustom));
        addBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addBtn.setTypeface(AndroidUtilities.bold());
        addBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        addBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        addBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), 0, Theme.getColor(Theme.key_listSelector)));
        addBtn.setOnClickListener(v -> showAddCustomDialog(context));
        customHeaderLayout.addView(addBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        contentLayout.addView(customHeaderLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 8));

        customContainer = new LinearLayout(context);
        customContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(customContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        refreshCustomTemplates(context);

        scrollView.addView(contentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        setCustomView(scrollView);
    }

    private void refreshCustomTemplates(Context context) {
        customContainer.removeAllViews();
        ArrayList<Template> custom = HundredGramRegexSearchController.getCustomTemplates();
        for (Template t : custom) {
            customContainer.addView(createTemplateRow(context, t));
        }
    }

    private View createTemplateRow(Context context, Template template) {
        FrameLayout root = new FrameLayout(context);
        root.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(template.title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textCol.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView patternView = new TextView(context);
        patternView.setText(template.pattern);
        patternView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        patternView.setTypeface(Typeface.MONOSPACE);
        patternView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        patternView.setSingleLine(true);
        patternView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(patternView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        if (!TextUtils.isEmpty(template.description)) {
            TextView descView = new TextView(context);
            descView.setText(template.description);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            textCol.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        root.addView(textCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, 0, 0, template.isCustom ? 40 : 0, 0));

        if (template.isCustom) {
            ImageView deleteBtn = new ImageView(context);
            deleteBtn.setImageResource(R.drawable.msg_delete);
            deleteBtn.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_text_RedRegular), PorterDuff.Mode.SRC_IN));
            deleteBtn.setScaleType(ImageView.ScaleType.CENTER);
            deleteBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            deleteBtn.setOnClickListener(v -> {
                HundredGramRegexSearchController.deleteCustomTemplate(template.id);
                refreshCustomTemplates(context);
            });
            root.addView(deleteBtn, LayoutHelper.createFrame(36, 36, Gravity.END | Gravity.CENTER_VERTICAL));
        }

        root.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemplateSelected(template.pattern);
            }
            dismiss();
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3));
        root.setLayoutParams(lp);

        return root;
    }

    private void showAddCustomDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("RegexAddCustom", R.string.RegexAddCustom));

        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        final EditText nameField = new EditText(context);
        nameField.setHint(LocaleController.getString("RegexTemplateName", R.string.RegexTemplateName));
        nameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameField.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        nameField.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        form.addView(nameField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        final EditText patternField = new EditText(context);
        patternField.setHint(LocaleController.getString("RegexTemplatePattern", R.string.RegexTemplatePattern));
        patternField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        patternField.setTypeface(Typeface.MONOSPACE);
        patternField.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        patternField.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        form.addView(patternField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final TextView errorView = new TextView(context);
        errorView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        errorView.setVisibility(View.GONE);
        form.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        builder.setView(form);
        builder.setPositiveButton(LocaleController.getString("RegexSave", R.string.RegexSave), null);
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = nameField.getText().toString().trim();
            String pattern = patternField.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                nameField.setError(LocaleController.getString("RegexTemplateName", R.string.RegexTemplateName));
                return;
            }
            if (TextUtils.isEmpty(pattern)) {
                patternField.setError(LocaleController.getString("RegexTemplatePattern", R.string.RegexTemplatePattern));
                return;
            }
            String validationError = HundredGramRegexSearchController.validateRegex(pattern);
            if (validationError != null) {
                errorView.setText(LocaleController.getString("RegexSyntaxError", R.string.RegexSyntaxError) + ": " + validationError);
                errorView.setVisibility(View.VISIBLE);
                return;
            }
            HundredGramRegexSearchController.saveCustomTemplate(title, pattern);
            refreshCustomTemplates(context);
            dialog.dismiss();
        });
    }
}
