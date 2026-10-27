package org.telegram.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

/**
 * Device-credential / biometric locks for chats, the archive and chat deletion.
 * A single successful unlock opens a grace window (HundredGramConfig.lockGrace).
 */
public class HundredGramLocks {

    private static long lastUnlock;

    public static boolean isSecure() {
        try {
            KeyguardManager km = (KeyguardManager) ApplicationLoader.applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isDeviceSecure()) {
                return true;
            }
        } catch (Throwable ignore) {
        }
        try {
            return BiometricManager.from(ApplicationLoader.applicationContext)
                    .canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static boolean shouldAsk() {
        if (!isSecure()) {
            return false;
        }
        return HundredGramConfig.lockGrace <= 0 || lastUnlock == 0
                || SystemClock.elapsedRealtime() - lastUnlock >= HundredGramConfig.lockGrace * 1000L;
    }

    public static boolean isChatLocked(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        if (HundredGramConfig.isChatLocked(dialogId)) {
            return true;
        }
        if (HundredGramConfig.lockSecret && DialogObject.isEncryptedDialog(dialogId)) {
            return true;
        }
        return HundredGramConfig.lockSaved && dialogId == UserConfig.getInstance(account).getClientUserId();
    }

    /** Shows the system credential prompt; runs onSuccess only after a successful unlock. */
    public static void authenticate(CharSequence title, CharSequence subtitle, Runnable onSuccess) {
        final LaunchActivity activity = LaunchActivity.instance;
        if (activity == null) {
            return;
        }
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle);
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            builder.setDeviceCredentialAllowed(true);
        }
        try {
            new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            lastUnlock = SystemClock.elapsedRealtime();
                            onSuccess.run();
                        }
                    }).authenticate(builder.build());
        } catch (Throwable ignore) {
        }
    }

    public static void guardChat(BaseFragment fragment, long dialogId) {
        if (fragment == null || !shouldAsk() || !isChatLocked(fragment.getCurrentAccount(), dialogId)) {
            return;
        }
        showOverlay(fragment, LocaleController.getString("LockChatLocked", R.string.LockChatLocked));
    }

    public static void guardArchive(BaseFragment fragment) {
        if (fragment == null || !shouldAsk() || !HundredGramConfig.lockArchive) {
            return;
        }
        showOverlay(fragment, LocaleController.getString("LockArchiveLocked", R.string.LockArchiveLocked));
    }

    private static void showOverlay(BaseFragment fragment, CharSequence text) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        View view = fragment.getFragmentView();
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup parent = (ViewGroup) view;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof LockOverlay) {
                return;
            }
        }
        parent.addView(new LockOverlay(fragment.getParentActivity(), fragment, text),
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        if (fragment.getActionBar() != null) {
            fragment.getActionBar().setVisibility(View.INVISIBLE);
        }
    }

    private static class LockOverlay extends FrameLayout {

        LockOverlay(Context context, BaseFragment fragment, CharSequence text) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, fragment.getResourceProvider()));
            setClickable(true);
            setFocusable(true);

            ImageView back = new ImageView(context);
            back.setScaleType(ImageView.ScaleType.CENTER);
            back.setImageResource(R.drawable.ic_ab_back);
            back.setColorFilter(new PorterDuffColorFilter(
                    Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, fragment.getResourceProvider()), PorterDuff.Mode.SRC_IN));
            back.setOnClickListener(v -> fragment.finishFragment());
            addView(back, LayoutHelper.createFrame(54, 54, Gravity.TOP | Gravity.LEFT, 3, 3, 0, 0));

            LinearLayout box = new LinearLayout(context);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);

            RLottieImageView icon = new RLottieImageView(context);
            icon.setAutoRepeat(false);
            icon.setAnimation(R.raw.passcode_lock_close, 112, 112);
            icon.setColorFilter(new PorterDuffColorFilter(
                    Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, fragment.getResourceProvider()), PorterDuff.Mode.SRC_IN));
            box.addView(icon, LayoutHelper.createLinear(112, 112, Gravity.CENTER_HORIZONTAL));

            TextView title = new TextView(context);
            title.setTextSize(16);
            title.setTypeface(AndroidUtilities.bold());
            title.setGravity(Gravity.CENTER);
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, fragment.getResourceProvider()));
            title.setText(text);
            box.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL, 24, 10, 24, 0));

            TextView button = new TextView(context);
            button.setTextSize(15);
            button.setTypeface(AndroidUtilities.bold());
            button.setGravity(Gravity.CENTER);
            button.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
            button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, fragment.getResourceProvider()));
            button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(18),
                    Theme.getColor(Theme.key_featuredStickers_addButton, fragment.getResourceProvider()),
                    Theme.getColor(Theme.key_featuredStickers_addButtonPressed, fragment.getResourceProvider())));
            button.setText(LocaleController.getString("LockUnlock", R.string.LockUnlock));
            button.setOnClickListener(v -> authenticate(text,
                    LocaleController.getString("LockPromptSubtitle", R.string.LockPromptSubtitle), () -> {
                        ViewParent parent = getParent();
                        if (parent instanceof ViewGroup) {
                            ((ViewGroup) parent).removeView(this);
                        }
                        if (fragment.getActionBar() != null) {
                            fragment.getActionBar().setVisibility(View.VISIBLE);
                        }
                    }));
            box.addView(button, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 36,
                    Gravity.CENTER_HORIZONTAL, 0, 18, 0, 0));

            addView(box, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
            post(button::performClick);
        }
    }
}
