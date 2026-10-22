package com.exteragram.messenger.feed.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.exteragram.messenger.feed.FeedChannelActions;
import com.exteragram.messenger.feed.FeedConfig;
import com.exteragram.messenger.feed.FeedController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import com.exteragram.messenger.feed.CustomFeed;
import com.exteragram.messenger.feed.CustomFeedManager;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class FeedChannelsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private final String customFeedId;
    private final ArrayList<TLRPC.Chat> channels = new ArrayList<>();
    private UniversalRecyclerView listView;
    private ActionBarMenuItem otherItem;
    private String query;
    private boolean searching;

    private static final int ITEM_FEED_TAB = 1001;
    private static final int ITEM_WIDE_POSTS = 1002;
    private static final int ITEM_UNREAD_COUNTER = 1003;
    private static final int ITEM_ARCHIVE = 1004;

    public FeedChannelsActivity() {
        this(null);
    }

    public FeedChannelsActivity(String customFeedId) {
        super();
        this.customFeedId = customFeedId;
    }

    private static final Comparator<TLRPC.Chat> BY_TITLE = (chat1, chat2) -> {
        String title1 = chat1 != null && chat1.title != null ? chat1.title.toLowerCase(Locale.ROOT) : "";
        String title2 = chat2 != null && chat2.title != null ? chat2.title.toLowerCase(Locale.ROOT) : "";
        return title1.compareTo(title2);
    };

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.feedNeedReload);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogDeleted);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.feedNeedReload);
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogDeleted);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.feedNeedReload) {
            reloadChannels();
        } else if (id == NotificationCenter.dialogDeleted) {
            removeChannel(((Long) args[0]));
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        CustomFeed customFeed = customFeedId != null ? CustomFeedManager.getInstance(currentAccount).getFeed(customFeedId) : null;
        actionBar.setTitle(customFeed != null ? customFeed.getName() : LocaleController.getString(R.string.FeedSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    setAllExcluded(false);
                } else if (id == 2) {
                    setAllExcluded(true);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(0, R.drawable.outline_header_search)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchExpand() {
                        searching = true;
                        if (otherItem != null) {
                            otherItem.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onSearchCollapse() {
                        searching = false;
                        query = null;
                        if (otherItem != null) {
                            otherItem.setVisibility(View.VISIBLE);
                        }
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }

                    @Override
                    public void onTextChanged(EditText editText) {
                        String text = editText.getText().toString().trim().toLowerCase(Locale.getDefault());
                        query = TextUtils.isEmpty(text) ? null : text;
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }
                });
        searchItem.setSearchPaddingStart(7);
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));

        otherItem = menu.addItem(3, R.drawable.ic_ab_other);
        otherItem.addSubItem(1, R.drawable.msg_select, LocaleController.getString(R.string.SelectAll));
        otherItem.addSubItem(2, R.drawable.msg_cancel, LocaleController.getString(R.string.DeselectAll));

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, new Utilities.Callback2<ArrayList<UItem>, UniversalAdapter>() {
            @Override
            public void run(ArrayList<UItem> items, UniversalAdapter adapter) {
                fillItems(items, adapter);
            }
        }, new Utilities.Callback5<UItem, View, Integer, Float, Float>() {
            @Override
            public void run(UItem item, View view, Integer position, Float x, Float y) {
                onClick(item, view, position, x, y);
            }
        }, new Utilities.Callback5Return<UItem, View, Integer, Float, Float, Boolean>() {
            @Override
            public Boolean run(UItem item, View view, Integer position, Float x, Float y) {
                return onLongClick(item, view, position, x, y);
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        reloadChannels();
        return fragmentView;
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (searching) {
            actionBar.closeSearchField();
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private void reloadChannels() {
        FeedController.getInstance(currentAccount).loadChannels(true, (loadedList, totalCount, isCached, unread) -> {
            if (isCached) {
                return;
            }
            channels.clear();
            for (int i = 0; i < loadedList.size(); i++) {
                TLRPC.Chat chat = (TLRPC.Chat) loadedList.get(i);
                TLRPC.Chat realChat = getMessagesController().getChat(chat.id);
                channels.add(realChat != null ? realChat : chat);
            }
            Collections.sort(channels, BY_TITLE);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
    }

    private void removeChannel(long dialogId) {
        boolean removed = channels.removeIf(chat -> (-chat.id) == dialogId);
        if (removed && listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        FeedConfig config = FeedConfig.getInstance(currentAccount);
        boolean isSearching = !TextUtils.isEmpty(query);

        if (customFeedId != null) {
            CustomFeed cf = CustomFeedManager.getInstance(currentAccount).getFeed(customFeedId);
            ArrayList<UItem> includedItems = new ArrayList<>();
            ArrayList<UItem> excludedItems = new ArrayList<>();

            for (int i = 0; i < channels.size(); i++) {
                TLRPC.Chat chat = channels.get(i);
                if (!isSearching || (chat.title != null && chat.title.toLowerCase(Locale.getDefault()).contains(query))) {
                    boolean isIncluded = cf != null && cf.containsChannel(-chat.id);
                    UItem userItem = UItem.asUserCheckbox((int) chat.id, chat).setChecked(isIncluded);
                    if (isIncluded) {
                        includedItems.add(userItem);
                    } else {
                        excludedItems.add(userItem);
                    }
                }
            }

            if (!includedItems.isEmpty()) {
                items.add(UItem.asHeader(LocaleController.getString(R.string.CustomFeedChannels)));
                items.addAll(includedItems);
            }
            if (!excludedItems.isEmpty()) {
                if (!includedItems.isEmpty()) {
                    items.add(UItem.asShadow(null));
                }
                items.add(UItem.asHeader(LocaleController.getString(R.string.FeedHiddenChannels)));
                items.addAll(excludedItems);
            }
            return;
        }

        if (!isSearching) {
            items.add(UItem.asHeader(LocaleController.getString(R.string.General)));
            items.add(UItem.asCheck(ITEM_FEED_TAB, LocaleController.getString(R.string.FeedBottomTab)).setChecked(HundredGramConfig.enableFeedTab));
            items.add(UItem.asCheck(ITEM_WIDE_POSTS, LocaleController.getString(R.string.WidePostsInFeed)).setChecked(HundredGramConfig.feedWidePosts));
            items.add(UItem.asCheck(ITEM_UNREAD_COUNTER, LocaleController.getString(R.string.FeedUnreadCounter)).setChecked(HundredGramConfig.feedUnreadCounter));
            items.add(UItem.asCheck(ITEM_ARCHIVE, LocaleController.getString(R.string.FeedIncludeArchived)).setChecked(config.isIncludeArchived()));
            items.add(UItem.asShadow(LocaleController.getString(R.string.FeedIncludeArchivedInfo)));
        }

        ArrayList<UItem> includedItems = new ArrayList<>();
        ArrayList<UItem> excludedItems = new ArrayList<>();

        for (int i = 0; i < channels.size(); i++) {
            TLRPC.Chat chat = channels.get(i);
            if (!isSearching || (chat.title != null && chat.title.toLowerCase(Locale.getDefault()).contains(query))) {
                boolean isExcluded = config.isExcluded(-chat.id);
                UItem userItem = UItem.asUserCheckbox((int) chat.id, chat).setChecked(!isExcluded);
                if (!isExcluded) {
                    includedItems.add(userItem);
                } else {
                    excludedItems.add(userItem);
                }
            }
        }

        if (!includedItems.isEmpty()) {
            items.add(UItem.asHeader(LocaleController.getString(R.string.FeedShownChannels)));
            items.addAll(includedItems);
        }
        if (!excludedItems.isEmpty()) {
            if (!includedItems.isEmpty()) {
                items.add(UItem.asShadow(null));
            }
            items.add(UItem.asHeader(LocaleController.getString(R.string.FeedHiddenChannels)));
            items.addAll(excludedItems);
        }
        if (!isSearching && (!includedItems.isEmpty() || !excludedItems.isEmpty())) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.FeedChannelsInfo)));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof TLRPC.Chat) {
            TLRPC.Chat chat = (TLRPC.Chat) item.object;
            if (customFeedId != null) {
                CustomFeed cf = CustomFeedManager.getInstance(currentAccount).getFeed(customFeedId);
                if (cf != null) {
                    if (cf.containsChannel(-chat.id)) {
                        cf.removeChannel(-chat.id);
                    } else {
                        cf.addChannel(-chat.id);
                    }
                    CustomFeedManager.getInstance(currentAccount).saveFeed(cf);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                }
                return;
            }
            FeedConfig config = FeedConfig.getInstance(currentAccount);
            boolean currentlyExcluded = config.isExcluded(-chat.id);
            config.setExcluded(-chat.id, !currentlyExcluded);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return;
        }

        if (item.id == ITEM_FEED_TAB) {
            HundredGramConfig.toggleEnableFeedTab();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.feedTabVisibleToggled);
        } else if (item.id == ITEM_WIDE_POSTS) {
            HundredGramConfig.toggleFeedWidePosts();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (parentLayout != null) {
                parentLayout.rebuildFragments(AndroidUtilities.isTablet() ? 1 : 0);
            }
        } else if (item.id == ITEM_UNREAD_COUNTER) {
            HundredGramConfig.toggleFeedUnreadCounter();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, 0);
        } else if (item.id == ITEM_ARCHIVE) {
            FeedConfig config = FeedConfig.getInstance(currentAccount);
            config.setIncludeArchived(!config.isIncludeArchived());
            reloadChannels();
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (!(item.object instanceof TLRPC.Chat)) {
            return false;
        }
        TLRPC.Chat chat = (TLRPC.Chat) item.object;
        ItemOptions options = ItemOptions.makeOptions(this, view);
        options.setLongPressSelectionEnabled(false);
        options.add(R.drawable.msg_channel, LocaleController.getString(R.string.OpenChannel2), () -> presentFragment(ChatActivity.of(-chat.id)));
        if (FeedChannelActions.canLeave(chat)) {
            options.add(R.drawable.msg_leave, LocaleController.getString(R.string.LeaveChannelMenu), true, () -> FeedChannelActions.leaveChannel(this, chat, null, null));
        }
        options.show();
        return true;
    }

    private void setAllExcluded(boolean excluded) {
        if (customFeedId != null) {
            CustomFeed cf = CustomFeedManager.getInstance(currentAccount).getFeed(customFeedId);
            if (cf != null) {
                if (excluded) {
                    for (TLRPC.Chat chat : channels) {
                        cf.removeChannel(-chat.id);
                    }
                } else {
                    for (TLRPC.Chat chat : channels) {
                        cf.addChannel(-chat.id);
                    }
                }
                CustomFeedManager.getInstance(currentAccount).saveFeed(cf);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
            return;
        }
        FeedConfig config = FeedConfig.getInstance(currentAccount);
        if (excluded) {
            ArrayList<Long> ids = new ArrayList<>();
            for (TLRPC.Chat chat : channels) {
                ids.add(-chat.id);
            }
            config.excludeAll(ids);
        } else {
            config.clearExcluded();
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}