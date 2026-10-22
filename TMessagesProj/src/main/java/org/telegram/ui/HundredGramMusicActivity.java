package org.telegram.ui;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramPlaylistManager;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LineProgressView;
import org.telegram.ui.Components.PlayPauseDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.SharedMediaLayout;

import java.util.ArrayList;
import java.util.List;

public class HundredGramMusicActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, MainTabsActivity.TabFragmentDelegate {

    private static final int ITEM_SEARCH = 1;
    private static final int ITEM_NEW_PLAYLIST = 2;

    private boolean hasMainTabs;
    private MainTabsActivityController mainTabsActivityController;

    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    private SharedMediaLayout sharedMediaLayout;
    private LinearLayout contentLayout;

    private FrameLayout nowPlayingCard;
    private BackupImageView nowPlayingCover;
    private TextView nowPlayingTitle;
    private TextView nowPlayingArtist;
    private ImageView nowPlayingButton;
    private RLottieImageView nextButton;
    private PlayPauseDrawable playPauseDrawable;
    private LineProgressView nowPlayingProgress;

    private TextView sourceTitle;
    private LinearLayout playlistsContainer;
    private HorizontalScrollView playlistsScrollView;

    public HundredGramMusicActivity() {
        this(null);
    }

    public HundredGramMusicActivity(Bundle args) {
        super(args);
    }

    public void setMainTabsActivityController(MainTabsActivityController controller) {
        this.mainTabsActivityController = controller;
        this.hasMainTabs = true;
    }

    public void updateMainTabsVisibility() {
        if (mainTabsActivityController != null) {
            mainTabsActivityController.setTabsVisible(true);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        Bundle bundle = this.arguments;
        if (bundle != null && bundle.getBoolean("hasMainTabs", false)) {
            hasMainTabs = true;
        }

        sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);

        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.onDestroy(this);
        }
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(hasMainTabs ? 0 : R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("Music", R.string.Music));
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar != null && actionBar.isSearchFieldVisible()) {
                        actionBar.closeSearchField();
                        return;
                    }
                    if (sharedMediaLayout != null && sharedMediaLayout.closeActionMode(true)) {
                        return;
                    }
                    finishFragment();
                } else if (id == ITEM_SEARCH) {
                    if (sharedMediaLayout != null && sharedMediaLayout.getSearchItem() != null) {
                        sharedMediaLayout.getSearchItem().openSearch(true);
                    }
                } else if (id == ITEM_NEW_PLAYLIST) {
                    showCreatePlaylistDialog();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(ITEM_SEARCH, R.drawable.outline_header_search).setContentDescription(LocaleController.getString("Search", R.string.Search));
        menu.addItem(ITEM_NEW_PLAYLIST, R.drawable.msg_add).setContentDescription(LocaleController.getString("NewPlaylist", R.string.NewPlaylist));

        FrameLayout rootFrame = new FrameLayout(context);
        rootFrame.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        fragmentView = rootFrame;

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootFrame, (v, insets) -> {
            updateNowPlayingPosition();
            updateContentPadding();
            return insets;
        });

        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        FrameLayout.LayoutParams contentLp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, actionBarHeight, 0, 0);
        rootFrame.addView(contentLayout, contentLp);

        // 1. Music Source Bar (Saved Messages / Channel)
        buildSourceBar(context, contentLayout);

        // 2. Playlists Horizontal Bar
        buildPlaylistsBar(context, contentLayout);

        // 3. Audio Tracks Library (SharedMediaLayout)
        setupSharedMediaLayout(context, contentLayout, getActiveDialogId());

        // 4. Floating Mini-Player at bottom
        buildNowPlayingCard(context, rootFrame);

        updateNowPlayingPosition();
        updateNowPlayingCard();
        refreshPlaylistsList();
        updateContentPadding();

        return fragmentView;
    }

    private void updateContentTopMargin() {
        if (contentLayout == null || actionBar == null) return;
        int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) contentLayout.getLayoutParams();
        if (lp != null && lp.topMargin != actionBarHeight) {
            lp.topMargin = actionBarHeight;
            contentLayout.setLayoutParams(lp);
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateContentTopMargin();
        updateNowPlayingPosition();
        updateContentPadding();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (actionBar != null && actionBar.isSearchFieldVisible()) {
            if (invoked) {
                actionBar.closeSearchField();
            }
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private void updateContentPadding() {
        if (contentLayout == null) return;
        int bottomPadding = 0;
        if (hasMainTabs) {
            bottomPadding += AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS + 8) + AndroidUtilities.navigationBarHeight;
        } else {
            bottomPadding += AndroidUtilities.navigationBarHeight;
        }
        if (nowPlayingCard != null && nowPlayingCard.getVisibility() == View.VISIBLE) {
            bottomPadding += AndroidUtilities.dp(68);
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.setPagesPaddingBottom(bottomPadding);
        }
    }

    private void updateNowPlayingPosition() {
        if (nowPlayingCard == null) return;
        ViewGroup.LayoutParams lp = nowPlayingCard.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
            int bottomMargin = (hasMainTabs ? AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS + 8) : AndroidUtilities.dp(12)) + AndroidUtilities.navigationBarHeight;
            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                nowPlayingCard.setLayoutParams(params);
            }
        }
    }

    private long getActiveDialogId() {
        long did = HundredGramConfig.musicSourceDialogId;
        if (did != 0) {
            return did;
        }
        return UserConfig.getInstance(currentAccount).getClientUserId();
    }

    private void setupSharedMediaLayout(Context context, LinearLayout root, long dialogId) {
        if (sharedMediaLayout != null) {
            root.removeView(sharedMediaLayout);
            if (sharedMediaPreloader != null) {
                sharedMediaPreloader.onDestroy(this);
            }
            sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);
            sharedMediaLayout = null;
        }
        sharedMediaLayout = new SharedMediaLayout(
                context,
                dialogId,
                sharedMediaPreloader,
                0,
                null,
                null,
                null,
                SharedMediaLayout.TAB_AUDIO,
                0,
                this,
                new SharedMediaLayout.Delegate() {
                    @Override
                    public void scrollToSharedMedia() {}

                    @Override
                    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean b, boolean resultOnly, View vi) {
                        return false;
                    }

                    @Override
                    public TLRPC.Chat getCurrentChat() {
                        return null;
                    }

                    @Override
                    public boolean isFragmentOpened() {
                        return true;
                    }

                    @Override
                    public RecyclerListView getListView() {
                        return null;
                    }

                    @Override
                    public boolean canSearchMembers() {
                        return false;
                    }

                    @Override
                    public void updateSelectedMediaTabText() {}
                },
                SharedMediaLayout.VIEW_TYPE_MEDIA_ACTIVITY,
                getResourceProvider()
        ) {
            @Override
            protected int getInitialTab() {
                return SharedMediaLayout.TAB_AUDIO;
            }

            @Override
            public int getSelectedTab() {
                return SharedMediaLayout.TAB_AUDIO;
            }

            @Override
            public int getClosestTab() {
                return SharedMediaLayout.TAB_AUDIO;
            }

            @Override
            protected boolean customTabs() {
                return true;
            }

            @Override
            public void updateTabs(boolean animated) {
                if (scrollSlidingTextTabStrip != null) {
                    scrollSlidingTextTabStrip.removeTabs();
                    scrollSlidingTextTabStrip.addTextTab(SharedMediaLayout.TAB_AUDIO, LocaleController.getString("SharedMusicTab2", R.string.SharedMusicTab2));
                    scrollSlidingTextTabStrip.setInitialTabId(SharedMediaLayout.TAB_AUDIO);
                    scrollSlidingTextTabStrip.finishAddingTabs();
                }
                if (mediaPages != null) {
                    for (MediaPage page : mediaPages) {
                        if (page != null) {
                            page.selectedType = SharedMediaLayout.TAB_AUDIO;
                        }
                    }
                }
            }

            @Override
            protected void checkCurrentTabValid() {
                if (mediaPages != null) {
                    for (MediaPage page : mediaPages) {
                        if (page != null) {
                            page.selectedType = SharedMediaLayout.TAB_AUDIO;
                        }
                    }
                }
            }

            @Override
            protected void switchToCurrentSelectedMode(boolean animated) {
                if (mediaPages != null) {
                    for (MediaPage page : mediaPages) {
                        if (page != null) {
                            page.selectedType = SharedMediaLayout.TAB_AUDIO;
                        }
                    }
                }
                super.switchToCurrentSelectedMode(animated);
            }

            @Override
            protected void onSearchStateChanged(boolean expanded) {
                super.onSearchStateChanged(expanded);
                if (hasMainTabs) {
                    actionBar.setBackButtonImage(expanded ? R.drawable.ic_ab_back : 0);
                }
            }
        };
        sharedMediaLayout.setPinnedToTop(true);
        sharedMediaLayout.disableScroll(true);
        if (sharedMediaLayout.scrollSlidingTextTabStrip != null) {
            sharedMediaLayout.scrollSlidingTextTabStrip.setVisibility(View.GONE);
            sharedMediaLayout.scrollSlidingTextTabStrip.selectTabWithId(SharedMediaLayout.TAB_AUDIO, 1f);
        }
        root.addView(sharedMediaLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));
        updateContentPadding();
    }

    private void buildSourceBar(Context context, LinearLayout root) {
        FrameLayout sourceBar = new FrameLayout(context);
        sourceBar.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), getThemedColor(Theme.key_windowBackgroundGray)));
        sourceBar.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        LinearLayout sourceInfo = new LinearLayout(context);
        sourceInfo.setOrientation(LinearLayout.HORIZONTAL);
        sourceInfo.setGravity(Gravity.CENTER_VERTICAL);

        ImageView sourceIcon = new ImageView(context);
        sourceIcon.setImageResource(R.drawable.files_music);
        sourceIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4), PorterDuff.Mode.SRC_IN));
        sourceInfo.addView(sourceIcon, LayoutHelper.createLinear(18, 18, 0, 0, 8, 0));

        sourceTitle = new TextView(context);
        sourceTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        sourceTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        sourceTitle.setTypeface(AndroidUtilities.bold());
        sourceTitle.setSingleLine(true);
        sourceTitle.setEllipsize(TextUtils.TruncateAt.END);
        updateSourceTitle();
        sourceInfo.addView(sourceTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        sourceBar.addView(sourceInfo, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0, 72, 0));

        TextView changeBtn = new TextView(context);
        changeBtn.setText(LocaleController.getString("Change", R.string.Change));
        changeBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        changeBtn.setTypeface(AndroidUtilities.bold());
        changeBtn.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4));
        sourceBar.addView(changeBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        sourceBar.setOnClickListener(v -> showSourceSelectDialog());

        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 6, 12, 4);
        root.addView(sourceBar, lp);
    }

    private void updateSourceTitle() {
        if (sourceTitle == null) return;
        long did = HundredGramConfig.musicSourceDialogId;
        String name;
        if (did == 0 || did == UserConfig.getInstance(currentAccount).getClientUserId()) {
            name = LocaleController.getString("SavedMessages", R.string.SavedMessages);
        } else if (did < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
            name = chat != null ? chat.title : String.valueOf(did);
        } else {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
            name = user != null ? ContactsController.formatName(user.first_name, user.last_name) : String.valueOf(did);
        }
        sourceTitle.setText(LocaleController.getString("Music", R.string.Music) + ": " + name);
    }

    private void showSourceSelectDialog() {
        if (getParentActivity() == null) return;
        ItemOptions options = ItemOptions.makeOptions(this, sourceTitle != null ? sourceTitle : fragmentView);
        options.add(R.drawable.msg_saved, LocaleController.getString("SavedMessages", R.string.SavedMessages), () -> {
            HundredGramConfig.setMusicSourceDialogId(0L);
            long myId = UserConfig.getInstance(currentAccount).getClientUserId();
            setupSharedMediaLayout(getContext(), contentLayout, myId);
            updateSourceTitle();
        });
        options.add(R.drawable.msg_channel, LocaleController.getString("SelectChat", R.string.SelectChat), () -> {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_DEFAULT);
            DialogsActivity activity = new DialogsActivity(args);
            activity.setDelegate((dialogsActivity, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                if (dids != null && !dids.isEmpty()) {
                    long did = dids.get(0).dialogId;
                    HundredGramConfig.setMusicSourceDialogId(did);
                    setupSharedMediaLayout(getContext(), contentLayout, did);
                    updateSourceTitle();
                }
                dialogsActivity.finishFragment();
                return true;
            });
            presentFragment(activity);
        });
        options.show();
    }

    private void buildNowPlayingCard(Context context, FrameLayout rootFrame) {
        nowPlayingCard = new FrameLayout(context);
        Drawable bg = Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_chat_inBubble));
        nowPlayingCard.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) {
            nowPlayingCard.setElevation(AndroidUtilities.dp(6));
            nowPlayingCard.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(16));
                }
            });
            nowPlayingCard.setClipToOutline(true);
        }

        // 1. Cover Art
        nowPlayingCover = new BackupImageView(context);
        nowPlayingCover.setRoundRadius(AndroidUtilities.dp(10));
        nowPlayingCard.addView(nowPlayingCover, LayoutHelper.createFrame(44, 44, Gravity.CENTER_VERTICAL | Gravity.LEFT, 8, 0, 0, 0));

        // 2. Track Title & Artist
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        nowPlayingTitle = new TextView(context);
        nowPlayingTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        nowPlayingTitle.setTypeface(AndroidUtilities.bold());
        nowPlayingTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        nowPlayingTitle.setSingleLine(true);
        nowPlayingTitle.setEllipsize(TextUtils.TruncateAt.END);
        infoLayout.addView(nowPlayingTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        nowPlayingArtist = new TextView(context);
        nowPlayingArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        nowPlayingArtist.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        nowPlayingArtist.setSingleLine(true);
        nowPlayingArtist.setEllipsize(TextUtils.TruncateAt.END);
        infoLayout.addView(nowPlayingArtist, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        nowPlayingCard.addView(infoLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.LEFT, 60, 0, 92, 0));

        // 3. Play/Pause and Next buttons
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER_VERTICAL);

        nowPlayingButton = new ImageView(context);
        nowPlayingButton.setScaleType(ImageView.ScaleType.CENTER);
        playPauseDrawable = new PlayPauseDrawable(AndroidUtilities.dp(14));
        nowPlayingButton.setImageDrawable(playPauseDrawable);
        nowPlayingButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN));
        nowPlayingButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        nowPlayingButton.setOnClickListener(v -> {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null) {
                if (MediaController.getInstance().isMessagePaused()) {
                    MediaController.getInstance().playMessage(playing);
                } else {
                    MediaController.getInstance().pauseMessage(playing);
                }
            }
        });
        controlsLayout.addView(nowPlayingButton, LayoutHelper.createLinear(38, 38));

        nextButton = new RLottieImageView(context);
        nextButton.setScaleType(ImageView.ScaleType.CENTER);
        nextButton.setAnimation(R.raw.player_prev, 20, 20);
        int iconColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
        nextButton.setLayerColor("Triangle 3.**", iconColor);
        nextButton.setLayerColor("Triangle 4.**", iconColor);
        nextButton.setLayerColor("Rectangle 4.**", iconColor);
        nextButton.setRotation(180f);
        nextButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        nextButton.setOnClickListener(v -> MediaController.getInstance().playNextMessage());
        controlsLayout.addView(nextButton, LayoutHelper.createLinear(38, 38, 4, 0, 0, 0));

        nowPlayingCard.addView(controlsLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 8, 0));

        // 4. Linear Progress Line at bottom
        nowPlayingProgress = new LineProgressView(context);
        nowPlayingProgress.setProgressColor(getThemedColor(Theme.key_featuredStickers_addButton));
        nowPlayingProgress.setBackColor(0);
        nowPlayingCard.addView(nowPlayingProgress, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2, Gravity.BOTTOM | Gravity.LEFT));

        nowPlayingCard.setOnClickListener(v -> {
            if (getParentActivity() != null) {
                new AudioPlayerAlert(getParentActivity(), getResourceProvider()).show();
            }
        });

        int bottomMargin = hasMainTabs ? AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS + 8) : AndroidUtilities.dp(12);
        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.BOTTOM | Gravity.LEFT, 10, 0, 10, bottomMargin);
        rootFrame.addView(nowPlayingCard, lp);
        nowPlayingCard.setVisibility(View.GONE);
    }

    private void updateCover(MessageObject messageObject) {
        if (nowPlayingCover == null || messageObject == null) return;
        final AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
        if (audioInfo != null && audioInfo.getCover() != null) {
            nowPlayingCover.setImageBitmap(audioInfo.getCover());
        } else {
            String artworkUrl = messageObject.getArtworkUrl(false);
            ImageLocation thumbImageLocation = getArtworkThumbImageLocation(messageObject);
            if (!TextUtils.isEmpty(artworkUrl)) {
                nowPlayingCover.setImage(ImageLocation.getForPath(artworkUrl), null, thumbImageLocation, null, null, 0, 1, messageObject);
            } else if (thumbImageLocation != null) {
                nowPlayingCover.setImage(null, null, thumbImageLocation, null, null, 0, 1, messageObject);
            } else {
                nowPlayingCover.setImageResource(R.drawable.nocover);
            }
        }
    }

    private ImageLocation getArtworkThumbImageLocation(MessageObject messageObject) {
        if (messageObject == null) return null;
        final TLRPC.Document document = messageObject.getDocument();
        TLRPC.PhotoSize thumb = document != null ? FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 360) : null;
        if (!(thumb instanceof TLRPC.TL_photoSize) && !(thumb instanceof TLRPC.TL_photoSizeProgressive)) {
            thumb = null;
        }
        if (thumb != null) {
            return ImageLocation.getForDocument(thumb, document);
        }
        final String smallArtworkUrl = messageObject.getArtworkUrl(true);
        if (smallArtworkUrl != null) {
            return ImageLocation.getForPath(smallArtworkUrl);
        }
        return null;
    }

    private void buildPlaylistsBar(Context context, LinearLayout root) {
        FrameLayout headerLayout = new FrameLayout(context);
        headerLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(4));

        TextView playlistsHeader = new TextView(context);
        playlistsHeader.setText(LocaleController.getString("Playlists", R.string.Playlists));
        playlistsHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        playlistsHeader.setTypeface(AndroidUtilities.bold());
        playlistsHeader.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        headerLayout.addView(playlistsHeader, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.LEFT));

        TextView addPlaylistBtn = new TextView(context);
        addPlaylistBtn.setText("+ " + LocaleController.getString("NewPlaylist", R.string.NewPlaylist));
        addPlaylistBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        addPlaylistBtn.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4));
        addPlaylistBtn.setOnClickListener(v -> showCreatePlaylistDialog());
        headerLayout.addView(addPlaylistBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        root.addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        playlistsScrollView = new HorizontalScrollView(context);
        playlistsScrollView.setHorizontalScrollBarEnabled(false);
        playlistsScrollView.setClipToPadding(false);
        playlistsScrollView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), AndroidUtilities.dp(6));

        playlistsContainer = new LinearLayout(context);
        playlistsContainer.setOrientation(LinearLayout.HORIZONTAL);
        playlistsScrollView.addView(playlistsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        root.addView(playlistsScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void refreshPlaylistsList() {
        if (playlistsContainer == null) return;
        playlistsContainer.removeAllViews();
        Context context = getContext();
        if (context == null) return;

        List<HundredGramPlaylistManager.CustomPlaylist> playlists = HundredGramPlaylistManager.getInstance(currentAccount).getPlaylists();

        if (playlists.isEmpty()) {
            TextView emptyHint = new TextView(context);
            emptyHint.setText(LocaleController.getString("NoPlaylists", R.string.NoPlaylists));
            emptyHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            emptyHint.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            emptyHint.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(6), AndroidUtilities.dp(4), AndroidUtilities.dp(6));
            playlistsContainer.addView(emptyHint);
            return;
        }

        for (final HundredGramPlaylistManager.CustomPlaylist playlist : playlists) {
            FrameLayout pill = new FrameLayout(context);
            pill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), getThemedColor(Theme.key_windowBackgroundGray)));
            pill.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

            LinearLayout pillContent = new LinearLayout(context);
            pillContent.setOrientation(LinearLayout.HORIZONTAL);
            pillContent.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(context);
            icon.setImageResource(R.drawable.files_music);
            icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4), PorterDuff.Mode.SRC_IN));
            pillContent.addView(icon, LayoutHelper.createLinear(18, 18, 0, 0, 6, 0));

            TextView title = new TextView(context);
            title.setText(playlist.getTitle() + " (" + playlist.getTrackCount() + ")");
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            title.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            pillContent.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            pill.addView(pillContent);

            pill.setOnClickListener(v -> {
                if (playlist.getTrackCount() == 0) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString("NoPlaylists", R.string.NoPlaylists)).show();
                    return;
                }
                HundredGramPlaylistManager.getInstance(currentAccount).playPlaylist(playlist, 0);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, playlist.getTitle()).show();
            });

            pill.setOnLongClickListener(v -> {
                showPlaylistOptions(v, playlist);
                return true;
            });

            LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 0);
            playlistsContainer.addView(pill, lp);
        }
    }

    private void showPlaylistOptions(View anchor, final HundredGramPlaylistManager.CustomPlaylist playlist) {
        if (getParentActivity() == null) return;
        ItemOptions options = ItemOptions.makeOptions(this, anchor);
        options.add(R.drawable.player, LocaleController.getString("Music", R.string.Music), () -> {
            HundredGramPlaylistManager.getInstance(currentAccount).playPlaylist(playlist, 0);
        });
        options.add(R.drawable.msg_edit, LocaleController.getString("RenamePlaylist", R.string.RenamePlaylist), () -> {
            showRenamePlaylistDialog(playlist);
        });
        options.add(R.drawable.msg_delete, LocaleController.getString("DeletePlaylist", R.string.DeletePlaylist), () -> {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
            b.setTitle(LocaleController.getString("DeletePlaylist", R.string.DeletePlaylist));
            b.setMessage(LocaleController.getString("DeletePlaylistConfirm", R.string.DeletePlaylistConfirm));
            b.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> {
                HundredGramPlaylistManager.getInstance(currentAccount).deletePlaylist(playlist.getId());
                refreshPlaylistsList();
            });
            b.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(b.create());
        });
        options.show();
    }

    private void showRenamePlaylistDialog(final HundredGramPlaylistManager.CustomPlaylist playlist) {
        if (getParentActivity() == null || playlist == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString("RenamePlaylist", R.string.RenamePlaylist));

        final EditText input = new EditText(getParentActivity());
        input.setText(playlist.getTitle());
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                HundredGramPlaylistManager.getInstance(currentAccount).renamePlaylist(playlist.getId(), name);
                refreshPlaylistsList();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showCreatePlaylistDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString("NewPlaylist", R.string.NewPlaylist));

        final EditText input = new EditText(getParentActivity());
        input.setHint(LocaleController.getString("EnterPlaylistName", R.string.EnterPlaylistName));
        input.setSingleLine(true);
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                HundredGramPlaylistManager.getInstance(currentAccount).createPlaylist(name);
                refreshPlaylistsList();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void updateNowPlayingCard() {
        if (nowPlayingCard == null) return;
        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (playing != null && playing.isMusic()) {
            boolean wasGone = nowPlayingCard.getVisibility() != View.VISIBLE;
            nowPlayingCard.setVisibility(View.VISIBLE);
            if (wasGone) {
                updateContentPadding();
            }
            String title = playing.getMusicTitle();
            String author = playing.getMusicAuthor();
            nowPlayingTitle.setText(!TextUtils.isEmpty(title) ? title : LocaleController.getString("Music", R.string.Music));
            nowPlayingArtist.setText(!TextUtils.isEmpty(author) ? author : LocaleController.getString("Loading", R.string.Loading));
            boolean isPaused = MediaController.getInstance().isMessagePaused();
            if (playPauseDrawable != null) {
                playPauseDrawable.setPause(!isPaused);
            }
            updateCover(playing);
            if (nowPlayingProgress != null) {
                nowPlayingProgress.setProgress(playing.audioProgress, true);
            }
        } else {
            boolean wasVisible = nowPlayingCard.getVisibility() == View.VISIBLE;
            nowPlayingCard.setVisibility(View.GONE);
            if (wasVisible) {
                updateContentPadding();
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && nowPlayingProgress != null) {
                nowPlayingProgress.setProgress(playing.audioProgress, false);
            }
        } else if (id == NotificationCenter.messagePlayingDidStart ||
            id == NotificationCenter.messagePlayingPlayStateChanged ||
            id == NotificationCenter.messagePlayingDidReset) {
            updateNowPlayingCard();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMainTabsVisibility();
        updateContentTopMargin();
        updateNowPlayingPosition();
        updateNowPlayingCard();
        refreshPlaylistsList();
        updateSourceTitle();
        updateContentPadding();
    }

    @Override
    public boolean canParentTabsSlide(MotionEvent ev, boolean forward) {
        return true;
    }

    @Override
    public void onParentScrollToTop() {
        if (sharedMediaLayout != null && sharedMediaLayout.getCurrentListView() != null) {
            sharedMediaLayout.getCurrentListView().smoothScrollToPosition(0);
        }
    }
}
