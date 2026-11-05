package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramActivitySimulator;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramSettingsSearchIndex;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.SettingsActivity.SettingCell;

import java.util.ArrayList;
import java.util.List;

public class HundredGramSettingsActivity extends BaseFragment {

    private static final int SEARCH_BUTTON_ID = 100;

    private static final int ITEM_CATEGORY_VAULT = 1;
    private static final int ITEM_CATEGORY_TRACKER = 2;
    private static final int ITEM_CATEGORY_PRIVACY = 3;
    private static final int ITEM_CATEGORY_INTERFACE = 4;
    private static final int ITEM_CATEGORY_CAMERAX = 5;
    private static final int ITEM_CATEGORY_ACTIONS = 6;
    private static final int ITEM_CATEGORY_MEDIA = 7;
    private static final int ITEM_CATEGORY_GHOST = 8;
    private static final int ITEM_CATEGORY_STREAMER = 9;
    private static final int ITEM_CATEGORY_AUTOMATION = 10;
    private static final int ITEM_CATEGORY_NETWORK = 11;
    private static final int ITEM_TRACKED_USERS = 12;
    private static final int ITEM_WHATS_NEW = 13;
    private static final int ITEM_AUTHOR = 14;
    private static final int ITEM_GITHUB = 15;
    private static final int ITEM_CATEGORY_ICON_PACKS = 16;
    private static final int ITEM_CATEGORY_BACKUP = 17;
    private static final int ITEM_CATEGORY_PILL_STACK = 18;
    private static final int ITEM_CATEGORY_AI = 19;
    private static final int ITEM_CATEGORY_SERVICES = 20;
    private static final int ITEM_CATEGORY_PLUGINS = 21;
    private static final int ITEM_CHANNELS_FEED = 22;
    private static final int ITEM_CATEGORY_SIMULATOR = 23;
    private static final int ITEM_CLEAR_RECENT_SEARCHES = 24;
    private static final int ITEM_SEARCH_RESULT_OFFSET = 1000;

    private UniversalRecyclerView listView;
    private ActionBarMenuItem searchItem;

    private boolean isSearchActive = false;
    private String searchQuery = "";

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramSettings", R.string.HundredGramSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // Search in ActionBar
        searchItem = actionBar.createMenu().addItem(SEARCH_BUTTON_ID, R.drawable.outline_header_search);
        searchItem.setIsSearchField(true);
        searchItem.setSearchFieldHint(LocaleController.getString("SettingsSearchHint", R.string.SettingsSearchHint));
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                isSearchActive = true;
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }

            @Override
            public void onSearchCollapse() {
                isSearchActive = false;
                searchQuery = "";
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                searchQuery = editText.getText().toString();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        updateSectionStyle();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionBar.setAdaptiveBackground(listView);
        return fragmentView = contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            updateSectionStyle();
            listView.adapter.update(true);
        }
    }

    private void updateSectionStyle() {
        boolean material = HundredGramConfig.md3Sections || HundredGramConfig.md3MasterEnabled;
        listView.setSections(AndroidUtilities.dp(material ? 16 : 12), AndroidUtilities.dp(material ? 28 : 16), false);
        listView.invalidate();
    }

    private IconBackgroundColors getCategoryColors(HundredGramSettingsSearchIndex.Entry entry) {
        if (entry.targetClass == HundredGramSubSettingsActivity.class) {
            switch (entry.category) {
                case HundredGramSubSettingsActivity.CATEGORY_GHOST:
                    return IconBackgroundColors.PURPLE;
                case HundredGramSubSettingsActivity.CATEGORY_PRIVACY:
                    return IconBackgroundColors.GREEN;
                case HundredGramSubSettingsActivity.CATEGORY_INTERFACE:
                    return IconBackgroundColors.BLUE_LIGHT;
                case HundredGramSubSettingsActivity.CATEGORY_ACTIONS:
                    return IconBackgroundColors.BLUE;
                case HundredGramSubSettingsActivity.CATEGORY_MEDIA:
                    return IconBackgroundColors.ORANGE_DEEP;
                case HundredGramSubSettingsActivity.CATEGORY_STREAMER:
                    return IconBackgroundColors.RED;
                case HundredGramSubSettingsActivity.CATEGORY_VAULT:
                    return IconBackgroundColors.BLUE_DEEP;
                case HundredGramSubSettingsActivity.CATEGORY_TRACKER:
                    return IconBackgroundColors.GRAY;
            }
        } else if (entry.targetClass == HundredGramCameraActivity.class) {
            return IconBackgroundColors.RED;
        } else if (entry.targetClass == HundredGramNetworkBoostActivity.class) {
            return IconBackgroundColors.CYAN;
        } else if (entry.targetClass == HundredGramAiSettingsActivity.class) {
            return IconBackgroundColors.GREEN;
        } else if (entry.targetClass == HundredGramServicesSettingsActivity.class) {
            return IconBackgroundColors.BLUE_LIGHT;
        } else if (entry.targetClass == HundredGramPluginsActivity.class) {
            return IconBackgroundColors.ORANGE;
        } else if (entry.targetClass == HundredGramActivitySimulatorActivity.class) {
            return IconBackgroundColors.BLUE;
        } else if (entry.targetClass == HundredGramBackupActivity.class) {
            return IconBackgroundColors.BLUE_ALT;
        } else if (entry.targetClass == HundredGramIconPacksActivity.class) {
            return IconBackgroundColors.PURPLE;
        } else if (entry.targetClass == HundredGramPillStackSettingsActivity.class) {
            return IconBackgroundColors.ORANGE;
        }
        return IconBackgroundColors.GRAY;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context context = getContext();
        if (context == null) return;

        // ==========================================
        // 1. SEARCH MODE ACTIVE
        // ==========================================
        if (isSearchActive) {
            String trimmedQuery = searchQuery != null ? searchQuery.trim() : "";

            if (TextUtils.isEmpty(trimmedQuery)) {
                // Show Recent Searches
                List<String> recentList = HundredGramSettingsSearchIndex.getInstance().getRecentSearches();
                if (!recentList.isEmpty()) {
                    items.add(UItem.asHeader(LocaleController.getString("SettingsSearchRecent", R.string.SettingsSearchRecent)));
                    for (String query : recentList) {
                        UItem recentItem = UItem.asButton(0, R.drawable.msg_recent, query);
                        recentItem.object = query;
                        items.add(recentItem);
                    }
                    items.add(UItem.asButton(ITEM_CLEAR_RECENT_SEARCHES, LocaleController.getString("SettingsSearchClearRecent", R.string.SettingsSearchClearRecent)).red());
                    items.add(UItem.asShadow(null));
                }
            } else {
                // Perform Search
                List<HundredGramSettingsSearchIndex.MatchResult> allResults = HundredGramSettingsSearchIndex.getInstance().search(trimmedQuery);

                if (allResults.isEmpty()) {
                    items.add(UItem.asTopView(
                            LocaleController.getString("SettingsSearchNoResults", R.string.SettingsSearchNoResults),
                            LocaleController.getString("SettingsSearchNoResultsSub", R.string.SettingsSearchNoResultsSub),
                            R.raw.utyan_empty
                    ));
                } else {
                    items.add(UItem.asHeader(String.format(LocaleController.getString("SettingsSearchFoundCount", R.string.SettingsSearchFoundCount), allResults.size())));
                    for (int i = 0; i < allResults.size(); i++) {
                        HundredGramSettingsSearchIndex.MatchResult match = allResults.get(i);
                        IconBackgroundColors colors = getCategoryColors(match.entry);
                        int iconRes = match.entry.categoryIcon != 0 ? match.entry.categoryIcon : R.drawable.msg_settings;
                        String path = "100gram › " + match.entry.getCategoryTitle();

                        UItem item = SettingCell.Factory.of(
                                ITEM_SEARCH_RESULT_OFFSET + i,
                                colors.top,
                                colors.bottom,
                                iconRes,
                                match.entry.getTitle(),
                                path
                        );
                        item.object = match;
                        items.add(item);
                    }
                    items.add(UItem.asShadow(null));
                }
            }
            return;
        }

        // ==========================================
        // SECTION 1: ВНЕШНИЙ ВИД И СТИЛЬ (APPEARANCE)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionAppearance", R.string.HundredGramSectionAppearance)));

        String transitionBadge;
        switch (HundredGramConfig.transitionAnimation) {
            case HundredGramConfig.TRANSITION_SPRING:
                transitionBadge = "Spring";
                break;
            case HundredGramConfig.TRANSITION_AOSP:
                transitionBadge = "AOSP";
                break;
            case HundredGramConfig.TRANSITION_DEFAULT:
            default:
                transitionBadge = "Default";
                break;
        }

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_INTERFACE,
                IconBackgroundColors.BLUE_LIGHT.top, IconBackgroundColors.BLUE_LIGHT.bottom,
                R.drawable.msg_discussion,
                LocaleController.getString("HundredGramCategoryInterface", R.string.HundredGramCategoryInterface),
                LocaleController.getString("HundredGramCategoryInterfaceSub", R.string.HundredGramCategoryInterfaceSub),
                transitionBadge));

        String iconPackBadge = HundredGramConfig.iconPacksEnabled ? HundredGramConfig.selectedIconPack : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_ICON_PACKS,
                IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom,
                R.drawable.msg_theme,
                LocaleController.getString("HundredGramIconPacks", R.string.HundredGramIconPacks),
                LocaleController.getString("HundredGramIconPacksSub", R.string.HundredGramIconPacksSub),
                iconPackBadge));

        String pillStackBadge = HundredGramConfig.pillStackEnabled ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_PILL_STACK,
                IconBackgroundColors.ORANGE.top, IconBackgroundColors.ORANGE.bottom,
                R.drawable.weather_cloudy,
                LocaleController.getString("PillStackPills", R.string.PillStackPills),
                LocaleController.getString("PillStackPillsInfo", R.string.PillStackPillsInfo),
                pillStackBadge));

        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 2: ЧАТЫ, МЕДИА И КАМЕРА (CHATS & MEDIA)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionChatsMedia", R.string.HundredGramSectionChatsMedia)));

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_ACTIONS,
                IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom,
                R.drawable.msg_actions,
                LocaleController.getString("HundredGramCategoryActions", R.string.HundredGramCategoryActions),
                LocaleController.getString("HundredGramCategoryActionsSub", R.string.HundredGramCategoryActionsSub)));

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_MEDIA,
                IconBackgroundColors.ORANGE_DEEP.top, IconBackgroundColors.ORANGE_DEEP.bottom,
                R.drawable.msg_media,
                LocaleController.getString("HundredGramCategoryMediaQuality", R.string.HundredGramCategoryMediaQuality),
                LocaleController.getString("HundredGramCategoryMediaQualitySub", R.string.HundredGramCategoryMediaQualitySub)));

        String cameraBadge = HundredGramConfig.cameraXEnabled ? "CameraX" : "Legacy";
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_CAMERAX,
                IconBackgroundColors.RED.top, IconBackgroundColors.RED.bottom,
                R.drawable.msg_camera,
                LocaleController.getString("CameraEngineTitle", R.string.CameraEngineTitle),
                LocaleController.getString("CameraEngineSub", R.string.CameraEngineSub),
                cameraBadge));

        items.add(SettingCell.Factory.of(ITEM_CHANNELS_FEED,
                IconBackgroundColors.CYAN.top, IconBackgroundColors.CYAN.bottom,
                R.drawable.msg_channel,
                LocaleController.getString("FeedTitle", R.string.FeedTitle),
                LocaleController.getString("FeedSettings", R.string.FeedSettings)));

        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 3: ПРИВАТНОСТЬ И БЕЗОПАСНОСТЬ (PRIVACY)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionStealth", R.string.HundredGramSectionStealth)));

        int ghostCount = 0;
        if (HundredGramConfig.isGhostModeActive()) ghostCount++;
        if (HundredGramConfig.ghostTyping) ghostCount++;
        if (HundredGramConfig.ghostStoriesMode != HundredGramConfig.GHOST_STORIES_OFF) ghostCount++;
        if (HundredGramConfig.ghostHideReadDate) ghostCount++;
        String ghostBadge = ghostCount > 0 ? ghostCount + " active" : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_GHOST,
                IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom,
                R.drawable.msg_secret,
                LocaleController.getString("HundredGramCategoryGhost", R.string.HundredGramCategoryGhost),
                LocaleController.getString("HundredGramCategoryGhostSub", R.string.HundredGramCategoryGhostSub),
                ghostBadge));

        int shadowbanCount = org.telegram.messenger.HundredGramShadowbanController.getShadowBannedCount();
        String privacyBadge = shadowbanCount > 0 ? LocaleController.formatPluralString("ShadowbanUsersCount", shadowbanCount, shadowbanCount) : null;
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_PRIVACY,
                IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom,
                R.drawable.msg_permissions,
                LocaleController.getString("HundredGramCategoryPrivacy", R.string.HundredGramCategoryPrivacy),
                LocaleController.getString("HundredGramCategoryPrivacySub", R.string.HundredGramCategoryPrivacySub),
                privacyBadge));

        String streamerBadge = HundredGramConfig.streamerMode ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_STREAMER,
                IconBackgroundColors.RED.top, IconBackgroundColors.RED.bottom,
                R.drawable.msg_channel,
                LocaleController.getString("HundredGramCategoryStreamer", R.string.HundredGramCategoryStreamer),
                LocaleController.getString("HundredGramCategoryStreamerSub", R.string.HundredGramCategoryStreamerSub),
                streamerBadge));

        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 4: ХРАНИЛИЩЕ И СЕТЬ (STORAGE & NETWORK)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionData", R.string.HundredGramSectionData)));

        long vaultSize = HundredGramVaultManager.getInstance(currentAccount).getVaultTotalSize();
        String vaultSizeStr = vaultSize > 0 ? AndroidUtilities.formatFileSize(vaultSize) : null;

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_VAULT,
                IconBackgroundColors.BLUE_DEEP.top, IconBackgroundColors.BLUE_DEEP.bottom,
                R.drawable.msg_download,
                LocaleController.getString("HundredGramCategoryVault", R.string.HundredGramCategoryVault),
                LocaleController.getString("HundredGramCategoryVaultSub", R.string.HundredGramCategoryVaultSub),
                vaultSizeStr));

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_NETWORK,
                IconBackgroundColors.CYAN.top, IconBackgroundColors.CYAN.bottom,
                R.drawable.msg_stats,
                LocaleController.getString("NetworkBoostTitle", R.string.NetworkBoostTitle),
                LocaleController.getString("NetworkBoostSub", R.string.NetworkBoostSub)));

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_BACKUP,
                IconBackgroundColors.BLUE_ALT.top, IconBackgroundColors.BLUE_ALT.bottom,
                R.drawable.msg_archive,
                LocaleController.getString("HundredGramBackupTitle", R.string.HundredGramBackupTitle),
                LocaleController.getString("HundredGramBackupSubtitle", R.string.HundredGramBackupSubtitle),
                "JSON"));

        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 5: ИНТЕЛЛЕКТ И ИНСТРУМЕНТЫ (SMART TOOLS)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionSmartTools", R.string.HundredGramSectionSmartTools)));

        String aiBadge = org.telegram.messenger.ai.HundredGramAiConfig.isAiEnabled()
                ? org.telegram.messenger.ai.HundredGramAiConfig.getSelectedService().getName()
                : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_AI,
                IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom,
                R.drawable.msg_bot,
                LocaleController.getString("AiSettingsTitle", R.string.AiSettingsTitle),
                LocaleController.getString("AiSettingsSubtitle", R.string.AiSettingsSubtitle),
                aiBadge));

        String servicesBadge = org.telegram.messenger.translator.HundredGramTranslator.getInstance().getProviderName(HundredGramConfig.translationProvider);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_SERVICES,
                IconBackgroundColors.BLUE_LIGHT.top, IconBackgroundColors.BLUE_LIGHT.bottom,
                R.drawable.msg_translate,
                LocaleController.getString("ServicesSettingsTitle", R.string.ServicesSettingsTitle),
                LocaleController.getString("ServicesSettingsSubtitle", R.string.ServicesSettingsSubtitle),
                servicesBadge));

        int pluginsCount = org.telegram.messenger.plugins.PluginsController.getInstance().getPlugins().size();
        String pluginsBadge = HundredGramConfig.pluginsEngine ? String.valueOf(pluginsCount) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_PLUGINS,
                IconBackgroundColors.ORANGE.top, IconBackgroundColors.ORANGE.bottom,
                R.drawable.msg_bot,
                LocaleController.getString("Plugins", R.string.Plugins),
                LocaleController.getString("PluginsSubtitle", R.string.PluginsSubtitle),
                pluginsBadge));

        int trackedCount = HundredGramConfig.trackedOnlineUsers.size();
        String trackedBadge = trackedCount > 0 ? trackedCount + " users" : null;
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_TRACKER,
                IconBackgroundColors.GRAY.top, IconBackgroundColors.GRAY.bottom,
                R.drawable.msg_usersearch,
                LocaleController.getString("HundredGramCategoryTracker", R.string.HundredGramCategoryTracker),
                LocaleController.getString("HundredGramCategoryTrackerSub", R.string.HundredGramCategoryTrackerSub),
                trackedBadge));

        items.add(SettingCell.Factory.of(ITEM_CATEGORY_AUTOMATION,
                IconBackgroundColors.ORANGE_DEEP.top, IconBackgroundColors.ORANGE_DEEP.bottom,
                R.drawable.msg_bot,
                LocaleController.getString("AutomationTitle", R.string.AutomationTitle),
                LocaleController.getString("AutomationSubtitle", R.string.AutomationSubtitle)));

        boolean simRunning = HundredGramActivitySimulator.getInstance(currentAccount).isRunning();
        String simBadge = simRunning ? LocaleController.getString("ActivitySimulatorActiveBadge", R.string.ActivitySimulatorActiveBadge) : null;
        items.add(SettingCell.Factory.of(ITEM_CATEGORY_SIMULATOR,
                IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom,
                R.drawable.msg_usersearch,
                LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle),
                LocaleController.getString("ActivitySimulatorSubtitle", R.string.ActivitySimulatorSubtitle),
                simBadge));

        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 6: О ПРИЛОЖЕНИИ И АВТОР (ABOUT)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramAboutSection", R.string.HundredGramAboutSection)));

        items.add(SettingCell.Factory.of(ITEM_WHATS_NEW,
                IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom,
                R.drawable.msg_info,
                LocaleController.getString("HundredGramWhatsNew", R.string.HundredGramWhatsNew),
                LocaleController.getString("HundredGramWhatsNewSub", R.string.HundredGramWhatsNewSub),
                "v1.3"));

        items.add(SettingCell.Factory.of(ITEM_AUTHOR,
                IconBackgroundColors.BLUE_DEEP.top, IconBackgroundColors.BLUE_DEEP.bottom,
                R.drawable.msg_openprofile,
                LocaleController.getString("HundredGramAuthor", R.string.HundredGramAuthor),
                LocaleController.getString("HundredGramAuthorSub", R.string.HundredGramAuthorSub),
                "@w1ckeddamn"));

        items.add(SettingCell.Factory.of(ITEM_GITHUB,
                IconBackgroundColors.GRAY.top, IconBackgroundColors.GRAY.bottom,
                R.drawable.msg_link,
                LocaleController.getString("HundredGramGitHub", R.string.HundredGramGitHub),
                LocaleController.getString("HundredGramGitHubSub", R.string.HundredGramGitHubSub),
                "GitHub"));

        items.add(UItem.asShadow(null));
    }

    private void onSearchResultClicked(HundredGramSettingsSearchIndex.MatchResult result) {
        if (result == null || result.entry == null) return;
        HundredGramSettingsSearchIndex.Entry entry = result.entry;

        if (!TextUtils.isEmpty(searchQuery)) {
            HundredGramSettingsSearchIndex.getInstance().addRecentSearch(searchQuery.trim());
        }

        if (entry.targetClass == HundredGramSubSettingsActivity.class) {
            presentFragment(new HundredGramSubSettingsActivity(entry.category, entry.itemId));
        } else {
            try {
                BaseFragment fragment = entry.targetClass.newInstance();
                presentFragment(fragment);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof HundredGramSettingsSearchIndex.MatchResult) {
            onSearchResultClicked((HundredGramSettingsSearchIndex.MatchResult) item.object);
            return;
        }

        if (item.object instanceof String) {
            if (searchItem != null) {
                searchItem.setSearchFieldText((String) item.object, true);
            }
            return;
        }

        if (item.id == ITEM_CLEAR_RECENT_SEARCHES) {
            HundredGramSettingsSearchIndex.getInstance().clearRecentSearches();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return;
        }

        switch (item.id) {
            case ITEM_WHATS_NEW:
                presentFragment(new HundredGramWhatsNewActivity());
                break;
            case ITEM_CATEGORY_GHOST:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_GHOST));
                break;
            case ITEM_CATEGORY_PRIVACY:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_PRIVACY));
                break;
            case ITEM_CATEGORY_STREAMER:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_STREAMER));
                break;
            case ITEM_CATEGORY_INTERFACE:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE));
                break;
            case ITEM_CATEGORY_ICON_PACKS:
                presentFragment(new HundredGramIconPacksActivity());
                break;
            case ITEM_CATEGORY_PILL_STACK:
                presentFragment(new HundredGramPillStackSettingsActivity());
                break;
            case ITEM_CATEGORY_ACTIONS:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_ACTIONS));
                break;
            case ITEM_CATEGORY_MEDIA:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_MEDIA));
                break;
            case ITEM_CATEGORY_CAMERAX:
                presentFragment(new HundredGramCameraActivity());
                break;
            case ITEM_CHANNELS_FEED:
                presentFragment(new com.exteragram.messenger.feed.ui.FeedChannelsActivity());
                break;
            case ITEM_CATEGORY_VAULT:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_VAULT));
                break;
            case ITEM_CATEGORY_NETWORK:
                presentFragment(new HundredGramNetworkBoostActivity());
                break;
            case ITEM_CATEGORY_BACKUP:
                presentFragment(new HundredGramBackupActivity());
                break;
            case ITEM_CATEGORY_TRACKER:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_TRACKER));
                break;
            case ITEM_TRACKED_USERS:
                presentFragment(new HundredGramTrackedUsersActivity());
                break;
            case ITEM_CATEGORY_AUTOMATION:
                presentFragment(new HundredGramAutomationActivity());
                break;
            case ITEM_CATEGORY_AI:
                presentFragment(new HundredGramAiSettingsActivity());
                break;
            case ITEM_CATEGORY_SIMULATOR:
                presentFragment(new HundredGramActivitySimulatorActivity());
                break;
            case ITEM_CATEGORY_SERVICES:
                presentFragment(new HundredGramServicesSettingsActivity());
                break;
            case ITEM_CATEGORY_PLUGINS:
                presentFragment(new HundredGramPluginsActivity());
                break;
            case ITEM_AUTHOR:
                Browser.openUrl(getParentActivity(), "https://t.me/w1ckeddamn");
                break;
            case ITEM_GITHUB:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram");
                break;
        }
    }
}
