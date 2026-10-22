package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiClient;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.Role;
import org.telegram.messenger.ai.Service;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

public class HundredGramAiResponseBottomSheet extends BottomSheet {

    private final BaseFragment parentFragment;
    private final TextView responseTextView;
    private final TextView subtitleTextView;
    private final RadialProgressView progressView;
    private final ButtonWithCounterView copyButton;
    private final ButtonWithCounterView insertButton;
    private final ChatActivity chatActivity;

    private HundredGramAiClient.Cancellable activeCancellable;
    private String fullResult = "";

    public HundredGramAiResponseBottomSheet(BaseFragment fragment, String prompt, Role role) {
        super(fragment.getParentActivity(), true, fragment.getResourceProvider());
        this.parentFragment = fragment;
        this.chatActivity = (fragment instanceof ChatActivity) ? (ChatActivity) fragment : null;

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
        LinearLayout titleBlock = new LinearLayout(context);
        titleBlock.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText("100gram AI");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleBlock.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitleTextView = new TextView(context);
        Service selected = HundredGramAiConfig.getSelectedService();
        String modelName = selected != null ? selected.getName() : "AI Assistant";
        String roleName = role != null ? role.getName() : "Assistant";
        subtitleTextView.setText(modelName + " • " + roleName);
        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        titleBlock.addView(subtitleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        headerLayout.addView(titleBlock, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 0, 0, 48, 0));

        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.ic_close_white);
        closeButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogTextBlack), PorterDuff.Mode.SRC_IN));
        ScaleStateListAnimator.apply(closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        headerLayout.addView(closeButton, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.END));

        contentView.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Progress bar
        progressView = new RadialProgressView(context, fragment.getResourceProvider());
        progressView.setSize(AndroidUtilities.dp(28));
        progressView.setProgressColor(getThemedColor(Theme.key_dialogTextBlack));
        contentView.addView(progressView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 12));

        // Response body in ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        responseTextView = new TextView(context);
        responseTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        responseTextView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        responseTextView.setTextIsSelectable(true);
        responseTextView.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        scrollView.addView(responseTextView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        contentView.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f, 0, 0, 0, 12));

        // Buttons bottom bar
        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        copyButton = new ButtonWithCounterView(context, fragment.getResourceProvider());
        copyButton.setText(LocaleController.getString("Copy", R.string.Copy), false);
        copyButton.setVisibility(View.GONE);
        copyButton.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(fullResult)) {
                AndroidUtilities.addToClipboard(fullResult);
                if (BulletinFactory.canShowBulletin(parentFragment)) {
                    BulletinFactory.of(parentFragment).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            }
        });
        buttonsLayout.addView(copyButton, LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, chatActivity != null ? 8 : 0, 0));

        if (chatActivity != null) {
            insertButton = new ButtonWithCounterView(context, fragment.getResourceProvider());
            insertButton.setText(LocaleController.getString("ApplyChanges", R.string.ApplyChanges), false);
            insertButton.setVisibility(View.GONE);
            insertButton.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(fullResult)) {
                    if (chatActivity.getChatActivityEnterView() != null) {
                        chatActivity.getChatActivityEnterView().setFieldText(fullResult);
                    }
                    dismiss();
                }
            });
            buttonsLayout.addView(insertButton, LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, 0, 0));
        } else {
            insertButton = null;
        }

        contentView.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 4, 0, 0));

        setCustomView(contentView);

        // Start generation
        startGeneration(prompt, role);
    }

    private void startGeneration(String prompt, Role role) {
        progressView.setVisibility(View.VISIBLE);
        responseTextView.setText("");

        activeCancellable = HundredGramAiClient.request(prompt, role, true, new HundredGramAiClient.GenerationCallback() {
            @Override
            public void onChunk(String accumulatedText, String newDelta) {
                fullResult = accumulatedText;
                responseTextView.setText(accumulatedText);
                if (progressView.getVisibility() == View.VISIBLE) {
                    progressView.setVisibility(View.GONE);
                }
                copyButton.setVisibility(View.VISIBLE);
                if (insertButton != null) {
                    insertButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onResponse(String fullText) {
                fullResult = fullText;
                responseTextView.setText(fullText);
                progressView.setVisibility(View.GONE);
                copyButton.setVisibility(View.VISIBLE);
                if (insertButton != null) {
                    insertButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int code, String message) {
                progressView.setVisibility(View.GONE);
                responseTextView.setTextColor(getThemedColor(Theme.key_text_RedBold));
                responseTextView.setText(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + message);
            }
        });
    }

    @Override
    public void dismiss() {
        if (activeCancellable != null) {
            activeCancellable.cancel();
            activeCancellable = null;
        }
        super.dismiss();
    }
}
