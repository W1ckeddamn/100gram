package com.exteragram.messenger.feed.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.text.TextUtils;
import android.widget.EditText;
import com.exteragram.messenger.feed.CustomFeed;
import com.exteragram.messenger.feed.CustomFeedManager;
import com.exteragram.messenger.feed.FeedConfig;
import com.exteragram.messenger.feed.FeedController;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatActivityContainer;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.MainTabsActivityController;

/* loaded from: D:\extragram\dex\classes4.dex */
public class FeedActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, MainTabsActivity.TabFragmentDelegate {
    private ChatActivityContainer chatContainer;
    private boolean embeddedChatCreated;
    private boolean hasMainTabs;
    private int lastConfigGeneration;
    private WindowInsetsCompat lastWindowInsets;
    private final Runnable loadNewPosts;
    private MainTabsActivityController mainTabsActivityController;
    private Runnable parentTabsGlassInvalidationCallback;
    private boolean resumedOnce;
    private boolean uiActiveHeld;
    private boolean uiResumedHeld;
    private boolean viewportFullyVisible;

    public boolean drawEdgeNavigationBar() {
        return false;
    }

    public boolean isSupportEdgeToEdge() {
        return true;
    }

    public FeedActivity() {
        this(null);
    }

    public FeedActivity(Bundle bundle) {
        super(bundle);
        this.loadNewPosts = new Runnable() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                FeedActivity.this.lambda$new$0();
            }
        };
    }

    public void setMainTabsActivityController(MainTabsActivityController mainTabsActivityController) {
        this.mainTabsActivityController = mainTabsActivityController;
    }

    public void updateMainTabsVisibility() {
        MainTabsActivityController mainTabsActivityController = this.mainTabsActivityController;
        if (mainTabsActivityController != null) {
            mainTabsActivityController.setTabsVisible(true);
        }
    }

    public static void presentFeed(BaseFragment baseFragment) {
        LaunchActivity launchActivity;
        if (!AndroidUtilities.isTablet() || (launchActivity = LaunchActivity.instance) == null || launchActivity.getRightActionBarLayout() == null) {
            if (baseFragment != null) {
                baseFragment.presentFragment(new FeedActivity());
                return;
            }
            return;
        }
        INavigationLayout rightActionBarLayout = LaunchActivity.instance.getRightActionBarLayout();
        if (rightActionBarLayout.getLastFragment() instanceof FeedActivity) {
            return;
        }
        if (!rightActionBarLayout.getFragmentStack().isEmpty()) {
            while (rightActionBarLayout.getFragmentStack().size() - 1 > 0) {
                rightActionBarLayout.removeFragmentFromStack((BaseFragment) rightActionBarLayout.getFragmentStack().get(0));
            }
            rightActionBarLayout.closeLastFragment(false);
        }
        rightActionBarLayout.presentFragment(new INavigationLayout.NavigationParams(new FeedActivity()).setNoAnimation(true).forceRightLayout());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || !this.uiResumedHeld) {
            return;
        }
        chatActivity.loadNewerFeed(true);
    }

    public boolean onFragmentCreate() {
        Bundle bundle = this.arguments;
        boolean z = false;
        if (bundle != null && bundle.getBoolean("hasMainTabs", false)) {
            z = true;
        }
        this.hasMainTabs = z;
        this.viewportFullyVisible = !z;
        NotificationCenter.getInstance(this.currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(this.currentAccount).addObserver(this, NotificationCenter.feedNeedReload);
        this.lastConfigGeneration = FeedConfig.getInstance(this.currentAccount).getGeneration();
        return super.onFragmentCreate();
    }

    public void onFragmentDestroy() {
        AndroidUtilities.cancelRunOnUIThread(this.loadNewPosts);
        destroyEmbeddedChat();
        if (this.uiResumedHeld) {
            this.uiResumedHeld = false;
            FeedController.getInstance(this.currentAccount).setUiResumed(false);
        }
        if (this.uiActiveHeld) {
            this.uiActiveHeld = false;
            FeedController.getInstance(this.currentAccount).setUiActive(false);
        }
        Bulletin.removeDelegate(this);
        NotificationCenter.getInstance(this.currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(this.currentAccount).removeObserver(this, NotificationCenter.feedNeedReload);
        super.onFragmentDestroy();
    }

    private void destroyEmbeddedChat() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer != null && (chatActivity = chatActivityContainer.chatActivity) != null) {
            if (!this.hasMainTabs && this.embeddedChatCreated) {
                chatActivity.saveFeedScrollPosition();
            }
            this.chatContainer.chatActivity.setFeedChannelsChangedCallback((Runnable) null);
            this.chatContainer.chatActivity.setGlassSourceInvalidationCallback((Runnable) null);
            if (this.embeddedChatCreated) {
                this.chatContainer.chatActivity.onFragmentDestroy();
            }
        }
        this.embeddedChatCreated = false;
        this.chatContainer = null;
    }

    public boolean onBackPressed(boolean z) {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || chatActivity.getActionBar() == null || !this.chatContainer.chatActivity.getActionBar().isActionModeShowed()) {
            return super.onBackPressed(z);
        }
        if (!z) {
            return false;
        }
        this.chatContainer.chatActivity.clearSelectionMode();
        return false;
    }

    @Override // org.telegram.messenger.NotificationCenter.NotificationCenterDelegate
    public void didReceivedNotification(int i, int i2, Object... objArr) {
        AndroidUtilities.runOnUIThread(() -> {
            boolean z = false;
            if (i == NotificationCenter.didReceiveNewMessages) {
                if (objArr == null || objArr.length < 3 || Boolean.TRUE.equals(objArr[2]) || this.chatContainer == null) {
                    return;
                }
                long dialogId = objArr[0] instanceof Long ? ((Long) objArr[0]).longValue() : 0;
                if (!FeedController.getInstance(this.currentAccount).isIncludedChannelPost(dialogId)) {
                    return;
                }
                AndroidUtilities.cancelRunOnUIThread(this.loadNewPosts);
                AndroidUtilities.runOnUIThread(this.loadNewPosts, 1000L);
                return;
            }
            if (i == NotificationCenter.feedNeedReload) {
                ChatActivityContainer chatActivityContainer = this.chatContainer;
                if (chatActivityContainer != null && chatActivityContainer.chatActivity != null) {
                    if (objArr != null && objArr.length > 0 && Boolean.TRUE.equals(objArr[0])) {
                        z = true;
                    }
                    this.chatContainer.chatActivity.onFeedChannelsChanged(z);
                }
                updateFeedSubtitle();
            }
        });
    }

    public View createView(Context context) {
        destroyEmbeddedChat();
        this.lastWindowInsets = null;
        this.actionBar.setAddToContainer(false);
        this.actionBar.setVisibility(8);
        FrameLayout frameLayout = new FrameLayout(context);
        this.fragmentView = frameLayout;
        frameLayout.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        if (this.hasMainTabs) {
            ViewCompat.setOnApplyWindowInsetsListener(frameLayout, new OnApplyWindowInsetsListener() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda2
                @Override // androidx.core.view.OnApplyWindowInsetsListener
                public final WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
                    WindowInsetsCompat lambda$createView$1;
                    lambda$createView$1 = FeedActivity.this.lambda$createView$1(view, windowInsetsCompat);
                    return lambda$createView$1;
                }
            });
            frameLayout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: com.exteragram.messenger.feed.ui.FeedActivity.1
                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewDetachedFromWindow(View view) {
                }

                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewAttachedToWindow(View view) {
                    if (FeedActivity.this.lastWindowInsets != null) {
                        ViewCompat.dispatchApplyWindowInsets(view, FeedActivity.this.lastWindowInsets);
                    } else {
                        view.requestApplyInsets();
                    }
                }
            });
        }
        FrameLayout frameLayout2 = new FrameLayout(context);
        frameLayout.addView(frameLayout2, LayoutHelper.createFrame(-1, -1, 119));
        Bundle bundle = new Bundle();
        bundle.putInt("chatMode", 7);
        bundle.putInt("searchType", 4);
        bundle.putBoolean("hasMainTabs", this.hasMainTabs);
        INavigationLayout layout = getParentLayout();
        if (layout == null && LaunchActivity.instance != null) {
            layout = LaunchActivity.instance.getActionBarLayout();
        }
        ChatActivityContainer chatActivityContainer = new ChatActivityContainer(context, layout, bundle) { // from class: com.exteragram.messenger.feed.ui.FeedActivity.2
            boolean activityCreated = false;

            public void initChatActivity() {
                if (this.activityCreated) {
                    return;
                }
                this.activityCreated = true;
                FeedActivity.this.embeddedChatCreated = true;
                super.initChatActivity();
                FeedActivity.this.applyFloatingWindowLayout();
                FeedActivity.this.setupChatActionBar();
                FeedActivity.this.setupChatTitle();
                if (FeedActivity.this.lastWindowInsets != null) {
                    FeedActivity feedActivity = FeedActivity.this;
                    View view = ((BaseFragment) feedActivity).fragmentView;
                    if (view != null) {
                        ViewCompat.dispatchApplyWindowInsets(view, feedActivity.lastWindowInsets);
                    }
                }
                FeedActivity.this.invalidateParentTabsGlass();
            }
        };
        this.chatContainer = chatActivityContainer;
        ChatActivity chatActivity = chatActivityContainer.chatActivity;
        chatActivity.isInsideContainer = false;
        chatActivity.setFeedChannelsChangedCallback(new Runnable() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                FeedActivity.this.updateFeedSubtitle();
            }
        });
        this.chatContainer.chatActivity.setGlassSourceInvalidationCallback(new Runnable() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                FeedActivity.this.invalidateParentTabsGlass();
            }
        });
        updateFeedViewportActive(this.viewportFullyVisible);
        if (!this.uiResumedHeld) {
            this.chatContainer.onPause();
        }
        frameLayout2.addView((View) this.chatContainer, (ViewGroup.LayoutParams) LayoutHelper.createFrame(-1, -1, 119));
        if (!this.uiActiveHeld) {
            this.uiActiveHeld = true;
            FeedController.getInstance(this.currentAccount).setUiActive(true);
        }
        Bulletin.addDelegate(this, new Bulletin.Delegate() { // from class: com.exteragram.messenger.feed.ui.FeedActivity.3
            public int getTopOffset(int i) {
                if (FeedActivity.this.chatContainer != null && FeedActivity.this.chatContainer.chatActivity != null) {
                    return FeedActivity.this.chatContainer.chatActivity.getBulletinTopOffset();
                }
                return AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight();
            }

            public int getBottomOffset(int i) {
                if (FeedActivity.this.chatContainer == null || FeedActivity.this.chatContainer.chatActivity == null) {
                    return 0;
                }
                return FeedActivity.this.chatContainer.chatActivity.getBulletinBottomOffset();
            }
        });
        return this.fragmentView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ WindowInsetsCompat lambda$createView$1(View view, WindowInsetsCompat windowInsetsCompat) {
        this.lastWindowInsets = windowInsetsCompat;
        return windowInsetsCompat;
    }

    private static WindowInsetsCompat addTabsBottomInset(WindowInsetsCompat windowInsetsCompat, int i) {
        int systemBars = WindowInsetsCompat.Type.systemBars();
        int navigationBars = WindowInsetsCompat.Type.navigationBars();
        return new WindowInsetsCompat.Builder(windowInsetsCompat).setStableInsets(addBottomInset(windowInsetsCompat.getInsetsIgnoringVisibility(systemBars), i)).setInsets(systemBars, addBottomInset(windowInsetsCompat.getInsets(systemBars), i)).setInsets(navigationBars, addBottomInset(windowInsetsCompat.getInsets(navigationBars), i)).setInsetsIgnoringVisibility(systemBars, addBottomInset(windowInsetsCompat.getInsetsIgnoringVisibility(systemBars), i)).setInsetsIgnoringVisibility(navigationBars, addBottomInset(windowInsetsCompat.getInsetsIgnoringVisibility(navigationBars), i)).build();
    }

    private static Insets addBottomInset(Insets insets, int i) {
        return Insets.of(insets.left, insets.top, insets.right, insets.bottom + i);
    }

    public void onResume() {
        ChatActivityContainer chatActivityContainer;
        ChatActivity chatActivity;
        ChatActivity chatActivity2;
        View view;
        WindowInsetsCompat windowInsetsCompat;
        super.onResume();
        updateMainTabsVisibility();
        ChatActivityContainer chatActivityContainer2 = this.chatContainer;
        if (chatActivityContainer2 != null) {
            chatActivityContainer2.onResume();
            updateFeedViewportActive(this.viewportFullyVisible);
        }
        if (!this.uiResumedHeld) {
            this.uiResumedHeld = true;
            FeedController.getInstance(this.currentAccount).setUiResumed(true);
        }
        if (this.hasMainTabs && (view = this.fragmentView) != null && (windowInsetsCompat = this.lastWindowInsets) != null) {
            ViewCompat.dispatchApplyWindowInsets(view, windowInsetsCompat);
        }
        reattachCurrentFeedVideoTexture();
        int generation = FeedConfig.getInstance(this.currentAccount).getGeneration();
        if (generation != this.lastConfigGeneration) {
            this.lastConfigGeneration = generation;
            ChatActivityContainer chatActivityContainer3 = this.chatContainer;
            if (chatActivityContainer3 != null && (chatActivity2 = chatActivityContainer3.chatActivity) != null) {
                chatActivity2.applyFeedConfigChange();
            }
        } else if (this.resumedOnce && (chatActivityContainer = this.chatContainer) != null && (chatActivity = chatActivityContainer.chatActivity) != null) {
            chatActivity.reconcileFeedList();
            this.chatContainer.chatActivity.refreshFeedUnreadDivider();
            if (!FeedController.getInstance(this.currentAccount).getMessages().isEmpty()) {
                this.chatContainer.chatActivity.loadNewerFeed(true);
            }
        }
        this.resumedOnce = true;
        updateFeedSubtitle();
    }

    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        this.viewportFullyVisible = true;
        updateMainTabsVisibility();
        updateFeedViewportActive(true);
        reattachCurrentFeedVideoTexture();
    }

    public void onBecomeFullyHidden() {
        this.viewportFullyVisible = false;
        updateFeedViewportActive(false);
        super.onBecomeFullyHidden();
    }

    public void onTransitionAnimationStart(boolean z, boolean z2) {
        if (this.hasMainTabs) {
            this.viewportFullyVisible = false;
            updateFeedViewportActive(false);
        }
        super.onTransitionAnimationStart(z, z2);
    }

    public void onTransitionAnimationEnd(boolean z, boolean z2) {
        super.onTransitionAnimationEnd(z, z2);
        if (this.hasMainTabs) {
            this.viewportFullyVisible = z;
            updateFeedViewportActive(z);
        }
    }

    public void onParentBecomeFullyVisible() {
        reattachCurrentFeedVideoTexture();
    }

    public void onPause() {
        super.onPause();
        if (this.chatContainer != null) {
            updateFeedViewportActive(false);
            this.chatContainer.onPause();
        }
        if (this.uiResumedHeld) {
            this.uiResumedHeld = false;
            FeedController.getInstance(this.currentAccount).setUiResumed(false);
        }
    }

    private void updateFeedViewportActive(boolean z) {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null) {
            return;
        }
        chatActivity.setFeedViewportActive(z);
    }

    public boolean canParentTabsSlide(MotionEvent motionEvent, boolean z) {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        return chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || chatActivity.getActionBar() == null || !this.chatContainer.chatActivity.getActionBar().isActionModeShowed();
    }

    public boolean isLightStatusBar() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer != null && (chatActivity = chatActivityContainer.chatActivity) != null) {
            return chatActivity.isLightStatusBar();
        }
        return !Theme.isCurrentThemeDark();
    }

    private void reattachCurrentFeedVideoTexture() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null) {
            return;
        }
        chatActivity.reattachCurrentFeedVideoTexture();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupChatActionBar() {
        ChatActivity chatActivity;
        final ActionBar actionBar;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || (actionBar = chatActivity.getActionBar()) == null) {
            return;
        }
        ActionBarMenu createMenu = actionBar.createMenu();
        if (createMenu.getItem(77) == null) {
            createMenu.addItem(77, R.drawable.msg_folders, this.chatContainer.chatActivity.themeDelegate).setContentDescription(LocaleController.getString(R.string.CustomFeeds));
        }
        if (createMenu.getItem(76) == null) {
            createMenu.addItem(76, R.drawable.msg_markread, this.chatContainer.chatActivity.themeDelegate).setContentDescription(LocaleController.getString(R.string.FeedMarkAllRead));
        }
        if (createMenu.getItem(75) == null) {
            createMenu.addItem(75, R.drawable.msg_settings, this.chatContainer.chatActivity.themeDelegate).setContentDescription(LocaleController.getString(R.string.FeedSettings));
        }
        if (this.hasMainTabs) {
            actionBar.setBackButtonDrawable(null);
        } else {
            actionBar.setBackButtonDrawable(new BackDrawable(false));
        }
        applyMainTabsHeaderLayout();
        final ActionBar.ActionBarMenuOnItemClick actionBarMenuOnItemClick = actionBar.getActionBarMenuOnItemClick();
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: com.exteragram.messenger.feed.ui.FeedActivity.4
            public void onItemClick(int i) {
                if (i == -1 && FeedActivity.this.hasMainTabs && !actionBar.isActionModeShowed()) {
                    return;
                }
                if (i == 77) {
                    FeedActivity.this.showCustomFeedsMenu(actionBar.createMenu().getItem(77));
                    return;
                }
                if (i == 76) {
                    FeedActivity.this.showMarkAllReadDialog();
                    return;
                }
                if (i == 75) {
                    CustomFeed active = CustomFeedManager.getInstance(FeedActivity.this.currentAccount).getActiveFeed();
                    if (active != null) {
                        FeedActivity.this.presentFragment(new FeedChannelsActivity(active.id));
                    } else {
                        FeedActivity.this.presentFragment(new FeedChannelsActivity());
                    }
                    return;
                }
                ActionBar.ActionBarMenuOnItemClick actionBarMenuOnItemClick2 = actionBarMenuOnItemClick;
                if (actionBarMenuOnItemClick2 != null) {
                    actionBarMenuOnItemClick2.onItemClick(i);
                }
            }

            public boolean canOpenMenu() {
                ActionBar.ActionBarMenuOnItemClick actionBarMenuOnItemClick2 = actionBarMenuOnItemClick;
                return actionBarMenuOnItemClick2 == null || actionBarMenuOnItemClick2.canOpenMenu();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyFloatingWindowLayout() {
        ChatActivityContainer chatActivityContainer;
        ChatActivity chatActivity;
        if (getParentLayout() == null || !getParentLayout().isLayersLayout() || (chatActivityContainer = this.chatContainer) == null || (chatActivity = chatActivityContainer.chatActivity) == null) {
            return;
        }
        if (chatActivity.getActionBar() != null) {
            chatActivity.getActionBar().setOccupyStatusBar(false);
        }
        ChatAvatarContainer chatAvatarContainer = chatActivity.avatarContainer;
        if (chatAvatarContainer != null) {
            chatAvatarContainer.setOccupyStatusBar(false);
        }
        ChatActivity.ChatActivityFragmentView chatActivityFragmentView = chatActivity.contentView;
        if (chatActivityFragmentView != null) {
            chatActivityFragmentView.setOccupyStatusBar(false);
        }
    }

    private void applyMainTabsHeaderLayout() {
        ChatActivity chatActivity;
        ChatAvatarContainer chatAvatarContainer;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || (chatAvatarContainer = chatActivity.avatarContainer) == null) {
            return;
        }
        if (this.hasMainTabs && chatActivity.getActionBar() != null) {
            chatActivity.getActionBar().setBackButtonDrawable(null);
        }
        ViewGroup.LayoutParams layoutParams = chatAvatarContainer.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            int margin = this.hasMainTabs ? AndroidUtilities.dp(16) : AndroidUtilities.dp(56);
            if (marginLayoutParams.leftMargin != margin) {
                marginLayoutParams.leftMargin = margin;
                chatAvatarContainer.setLayoutParams(marginLayoutParams);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showMarkAllReadDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FeedMarkAllRead));
        builder.setMessage(LocaleController.getString(R.string.FeedMarkAllReadConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.MarkAsRead), new AlertDialog.OnButtonClickListener() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda5
            public final void onClick(AlertDialog alertDialog, int i) {
                FeedActivity.this.lambda$showMarkAllReadDialog$2(alertDialog, i);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (AlertDialog.OnButtonClickListener) null);
        showDialog(builder.create());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMarkAllReadDialog$2(AlertDialog alertDialog, int i) {
        markAllRead();
        BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.FeedMarkAllReadDone)).show();
    }

    public void markAllRead() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer != null && (chatActivity = chatActivityContainer.chatActivity) != null) {
            chatActivity.markFeedAsRead();
        } else {
            FeedController.getInstance(this.currentAccount).markAllRead();
        }
    }

    public void onParentScrollToTop() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null) {
            return;
        }
        chatActivity.onPageDownClicked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupChatTitle() {
        ChatActivity chatActivity;
        ChatAvatarContainer chatAvatarContainer;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || (chatAvatarContainer = chatActivity.avatarContainer) == null) {
            return;
        }
        CustomFeed activeFeed = CustomFeedManager.getInstance(this.currentAccount).getActiveFeed();
        if (activeFeed != null) {
            chatAvatarContainer.setTitle(activeFeed.getName());
        } else {
            chatAvatarContainer.setTitle(LocaleController.getString(R.string.Feed));
        }
        this.chatContainer.chatActivity.avatarContainer.setFeedAvatar();
        updateFeedSubtitle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFeedSubtitle() {
        CustomFeed activeFeed = CustomFeedManager.getInstance(this.currentAccount).getActiveFeed();
        if (activeFeed != null) {
            setFeedSubtitle(activeFeed.getChannelCount());
            return;
        }
        final FeedController feedController = FeedController.getInstance(this.currentAccount);
        setFeedSubtitle(feedController.getIncludedChannelCount());
        feedController.loadChannels(new FeedController.ChannelsCallback() { // from class: com.exteragram.messenger.feed.ui.FeedActivity$$ExternalSyntheticLambda0
            @Override // com.exteragram.messenger.feed.FeedController.ChannelsCallback
            public final void onChannels(ArrayList arrayList, int i, boolean z, int i2) {
                FeedActivity.this.lambda$updateFeedSubtitle$3(feedController, arrayList, i, z, i2);
            }
        });
    }

    private void showCustomFeedsMenu(View anchor) {
        if (getParentActivity() == null) {
            return;
        }
        ItemOptions options = ItemOptions.makeOptions(this, anchor != null ? anchor : this.fragmentView);
        final CustomFeedManager manager = CustomFeedManager.getInstance(this.currentAccount);
        List<CustomFeed> feeds = manager.getFeeds();
        String activeId = manager.getActiveFeedId();

        boolean allActive = CustomFeedManager.ALL_FEED_ID.equals(activeId);
        options.add(allActive ? R.drawable.msg_select : 0, LocaleController.getString(R.string.AllChannels), () -> {
            manager.setActiveFeedId(CustomFeedManager.ALL_FEED_ID);
            setupChatTitle();
        });

        for (final CustomFeed feed : feeds) {
            boolean isActive = feed.id.equals(activeId);
            options.add(isActive ? R.drawable.msg_select : 0, feed.getName() + " (" + feed.getChannelCount() + ")", () -> {
                manager.setActiveFeedId(feed.id);
                setupChatTitle();
            });
        }

        options.addGap();
        options.add(R.drawable.msg_add, LocaleController.getString(R.string.CreateCustomFeed), () -> {
            showCreateFeedDialog();
        });

        if (!allActive && manager.getActiveFeed() != null) {
            final CustomFeed currentCustom = manager.getActiveFeed();
            options.add(R.drawable.msg_edit, LocaleController.getString(R.string.EditCustomFeed), () -> {
                presentFragment(new FeedChannelsActivity(currentCustom.id));
            });
            options.add(R.drawable.msg_delete, LocaleController.getString(R.string.DeleteCustomFeed), true, () -> {
                showDeleteFeedDialog(currentCustom);
            });
        }

        options.show();
    }

    private void showCreateFeedDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.CreateCustomFeed));

        final EditText editText = new EditText(getParentActivity());
        editText.setHint(LocaleController.getString(R.string.EnterFeedName));
        editText.setSingleLine(true);

        FrameLayout container = new FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), 0);
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            String name = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                CustomFeed feed = new CustomFeed(UUID.randomUUID().toString(), name, new ArrayList<>());
                CustomFeedManager.getInstance(this.currentAccount).saveFeed(feed);
                CustomFeedManager.getInstance(this.currentAccount).setActiveFeedId(feed.id);
                setupChatTitle();
                presentFragment(new FeedChannelsActivity(feed.id));
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDeleteFeedDialog(final CustomFeed feed) {
        if (getParentActivity() == null || feed == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.DeleteCustomFeed));
        builder.setMessage(LocaleController.getString(R.string.DeleteFeedConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
            CustomFeedManager.getInstance(this.currentAccount).deleteFeed(feed.id);
            setupChatTitle();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateFeedSubtitle$3(FeedController feedController, ArrayList arrayList, int i, boolean z, int i2) {
        if (z) {
            i = feedController.getIncludedChannelCount();
        }
        setFeedSubtitle(i);
    }

    private void setFeedSubtitle(int i) {
        ChatActivity chatActivity;
        ChatAvatarContainer chatAvatarContainer;
        String formatPluralString;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null || (chatAvatarContainer = chatActivity.avatarContainer) == null) {
            return;
        }
        if (i < 0) {
            formatPluralString = LocaleController.getString(R.string.Loading);
        } else {
            formatPluralString = LocaleController.formatPluralString("Channels", i, new Object[0]);
        }
        chatAvatarContainer.setSubtitle(formatPluralString);
        View subtitleTextView = this.chatContainer.chatActivity.avatarContainer.getSubtitleTextView();
        if (subtitleTextView != null) {
            subtitleTextView.setVisibility(0);
        }
    }

    public BlurredBackgroundSourceRenderNode getGlassSource() {
        ChatActivity chatActivity;
        ChatActivityContainer chatActivityContainer = this.chatContainer;
        if (chatActivityContainer == null || (chatActivity = chatActivityContainer.chatActivity) == null) {
            return null;
        }
        return chatActivity.getGlassSource();
    }

    public void setParentTabsGlassInvalidationCallback(Runnable runnable) {
        this.parentTabsGlassInvalidationCallback = runnable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invalidateParentTabsGlass() {
        Runnable runnable = this.parentTabsGlassInvalidationCallback;
        if (runnable != null) {
            runnable.run();
        }
    }
}