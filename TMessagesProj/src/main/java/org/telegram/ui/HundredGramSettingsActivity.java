package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramActivitySimulator;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramSettingsSearchIndex;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.HundredGramCardCell;
import org.telegram.ui.Components.HundredGramSearchResultCell;
import org.telegram.ui.Components.HundredGramSettingsHeroCell;
import org.telegram.ui.Components.HundredGramVaultStorageCard;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HundredGramSettingsActivity extends BaseFragment {

    private static final int SEARCH_BUTTON_ID = 100;

    private static final int ITEM_SEARCH_HEADER = 0;
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

    private UniversalRecyclerView listView;
    private ActionBarMenuItem searchItem;
    private HorizontalScrollView chipsScrollView;
    private LinearLayout chipsContainer;

    private boolean isSearchActive = false;
    private String searchQuery = "";
    private int selectedCategoryFilter = -1; // -1 for ALL

    private static class ChipFilter {
        final int category;
        final String titleKey;
        final String fallbackTitle;
        final int iconRes;

        ChipFilter(int category, String titleKey, String fallbackTitle, int iconRes) {
            this.category = category;
            this.titleKey = titleKey;
            this.fallbackTitle = fallbackTitle;
            this.iconRes = iconRes;
        }
    }

    private final List<ChipFilter> chipFilters = new ArrayList<>();

    private void initChipFilters() {
        chipFilters.clear();
        chipFilters.add(new ChipFilter(-1, "SettingsSearchChipAll", "Все", R.drawable.msg_settings));
        chipFilters.add(new ChipFilter(HundredGramSubSettingsActivity.CATEGORY_GHOST, "SettingsSearchChipGhost", "Призрак", R.drawable.msg_secret));
        chipFilters.add(new ChipFilter(HundredGramSubSettingsActivity.CATEGORY_VAULT, "SettingsSearchChipVault", "Хранилище", R.drawable.msg_archive));
        chipFilters.add(new ChipFilter(HundredGramSubSettingsActivity.CATEGORY_INTERFACE, "SettingsSearchChipInterface", "Интерфейс", R.drawable.msg_discussion));
        chipFilters.add(new ChipFilter(1001, "SettingsSearchChipNetwork", "Сеть", R.drawable.msg_stats));
        chipFilters.add(new ChipFilter(1002, "SettingsSearchChipSimulator", "Имитатор", R.drawable.msg_usersearch));
        chipFilters.add(new ChipFilter(1003, "SettingsSearchChipAI", "ИИ", R.drawable.msg_bot));
    }

    @Override
    public View createView(Context context) {
        initChipFilters();

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

        // Search Menu Item in ActionBar
        searchItem = actionBar.createMenu().addItem(SEARCH_BUTTON_ID, R.drawable.outline_header_search);
        searchItem.setIsSearchField(true);
        searchItem.setSearchFieldHint(LocaleController.getString("SettingsSearchHint", R.string.SettingsSearchHint));
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                isSearchActive = true;
                if (chipsScrollView != null) {
                    chipsScrollView.setVisibility(View.VISIBLE);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }

            @Override
            public void onSearchCollapse() {
                isSearchActive = false;
                searchQuery = "";
                selectedCategoryFilter = -1;
                updateChipsVisualState();
                if (chipsScrollView != null) {
                    chipsScrollView.setVisibility(View.GONE);
                }
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

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // Horizontal Category Chips Bar (visible during search)
        createChipsBar(context, rootLayout);

        // Main List
        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        rootLayout.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionBar.setAdaptiveBackground(listView);
        return fragmentView = rootLayout;
    }

    private void createChipsBar(Context context, LinearLayout parent) {
        chipsScrollView = new HorizontalScrollView(context);
        chipsScrollView.setHorizontalScrollBarEnabled(false);
        chipsScrollView.setVisibility(View.GONE);
        chipsScrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        chipsScrollView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

        chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setGravity(Gravity.CENTER_VERTICAL);
        chipsScrollView.addView(chipsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        renderChips();

        parent.addView(chipsScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void renderChips() {
        if (chipsContainer == null) return;
        Context context = getContext();
        if (context == null) return;

        chipsContainer.removeAllViews();
        for (ChipFilter filter : chipFilters) {
            final int filterCat = filter.category;
            boolean isSelected = selectedCategoryFilter == filterCat;

            TextView chip = new TextView(context);
            chip.setTextSize(13);
            chip.setTypeface(AndroidUtilities.bold());
            String title = filter.fallbackTitle;
            if (!TextUtils.isEmpty(filter.titleKey)) {
                int resId = context.getResources().getIdentifier(filter.titleKey, "string", context.getPackageName());
                if (resId != 0) {
                    title = LocaleController.getString(filter.titleKey, resId);
                }
            }
            chip.setText(title);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(AndroidUtilities.dp(16));

            if (isSelected) {
                int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
                chipBg.setColor(accent);
                chip.setTextColor(0xFFFFFFFF);
            } else {
                chipBg.setColor(Theme.getColor(Theme.key_chat_serviceBackground));
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            chip.setBackground(chipBg);
            chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

            chip.setOnClickListener(v -> {
                selectedCategoryFilter = filterCat;
                updateChipsVisualState();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            });

            chipsContainer.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 0));
        }
    }

    private void updateChipsVisualState() {
        renderChips();
    }

    private View createFloatingSearchHeader(Context context) {
        FrameLayout card = new FrameLayout(context);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        FrameLayout inner = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        boolean isDark = Theme.isCurrentThemeDark();
        bg.setColor(isDark ? 0xFF1E2834 : 0xFFFFFFFF);
        bg.setStroke(AndroidUtilities.dp(1), isDark ? 0x22FFFFFF : 0x1A000000);
        inner.setBackground(bg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.outline_header_search);
        icon.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        inner.addView(icon, LayoutHelper.createFrame(20, 20, Gravity.CENTER_VERTICAL | Gravity.START, 14, 0, 0, 0));

        TextView hint = new TextView(context);
        hint.setText(LocaleController.getString("SettingsSearchHint", R.string.SettingsSearchHint));
        hint.setTextSize(14);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        inner.addView(hint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 46, 0, 14, 0));

        card.addView(inner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44));
        card.setOnClickListener(v -> {
            if (searchItem != null) {
                searchItem.openSearch(true);
            }
        });
        return card;
    }

    private View createCard(Context context, int itemId, int iconRes, int badgeColor, CharSequence title, CharSequence subtitle, CharSequence badge, boolean needDivider) {
        HundredGramCardCell cell = new HundredGramCardCell(context);
        int alphaBadge = Color.argb(35, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor));
        cell.setData(iconRes, alphaBadge, badgeColor, title, subtitle, badge, needDivider);
        cell.setOnClickListener(v -> onClick(UItem.asButton(itemId, title), v, 0, 0, 0));
        return cell;
    }

    private View createRecentSearchCell(Context context, String query) {
        FrameLayout layout = new FrameLayout(context);
        layout.setBackground(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_recent);
        icon.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        layout.addView(icon, LayoutHelper.createFrame(20, 20, Gravity.CENTER_VERTICAL | Gravity.START, 18, 0, 0, 0));

        TextView text = new TextView(context);
        text.setText(query);
        text.setTextSize(15);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        layout.addView(text, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 56, 0, 16, 0));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        layout.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 56, 0, 0, 0));

        layout.setOnClickListener(v -> {
            if (searchItem != null) {
                searchItem.setSearchFieldText(query, true);
            }
        });

        return layout;
    }

    private View createEmptySearchCell(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(60), AndroidUtilities.dp(24), AndroidUtilities.dp(60));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_search);
        icon.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        layout.addView(icon, LayoutHelper.createLinear(48, 48, 0, 0, 0, 12));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("SettingsSearchNoResults", R.string.SettingsSearchNoResults));
        title.setTextSize(16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        layout.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString("SettingsSearchNoResultsSub", R.string.SettingsSearchNoResultsSub));
        subtitle.setTextSize(13);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        return layout;
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
                        items.add(UItem.asCustom(createRecentSearchCell(context, query)));
                    }
                    items.add(UItem.asButton(ITEM_CLEAR_RECENT_SEARCHES, LocaleController.getString("SettingsSearchClearRecent", R.string.SettingsSearchClearRecent)));
                    items.add(UItem.asShadow(null));
                }
            } else {
                // Perform Search
                List<HundredGramSettingsSearchIndex.MatchResult> allResults = HundredGramSettingsSearchIndex.getInstance().search(trimmedQuery);
                List<HundredGramSettingsSearchIndex.MatchResult> filtered = new ArrayList<>();

                for (HundredGramSettingsSearchIndex.MatchResult mr : allResults) {
                    if (selectedCategoryFilter == -1) {
                        filtered.add(mr);
                    } else if (selectedCategoryFilter == 1001 && mr.entry.targetClass == HundredGramNetworkBoostActivity.class) {
                        filtered.add(mr);
                    } else if (selectedCategoryFilter == 1002 && mr.entry.targetClass == HundredGramActivitySimulatorActivity.class) {
                        filtered.add(mr);
                    } else if (selectedCategoryFilter == 1003 && mr.entry.targetClass == HundredGramAiSettingsActivity.class) {
                        filtered.add(mr);
                    } else if (mr.entry.category == selectedCategoryFilter) {
                        filtered.add(mr);
                    }
                }

                if (filtered.isEmpty()) {
                    items.add(UItem.asCustom(createEmptySearchCell(context)));
                } else {
                    items.add(UItem.asHeader(String.format(LocaleController.getString("SettingsSearchFoundCount", R.string.SettingsSearchFoundCount), filtered.size())));
                    for (int i = 0; i < filtered.size(); i++) {
                        HundredGramSettingsSearchIndex.MatchResult match = filtered.get(i);
                        HundredGramSearchResultCell cell = new HundredGramSearchResultCell(context);
                        cell.setData(match, i < filtered.size() - 1);
                        cell.setOnClickListener(v -> onSearchResultClicked(match));
                        items.add(UItem.asCustom(cell));
                    }
                    items.add(UItem.asShadow(null));
                }
            }
            return;
        }

        // ==========================================
        // 2. NORMAL SETTINGS MODE
        // ==========================================

        // Hero Profile & Status Card
        items.add(UItem.asCustom(new HundredGramSettingsHeroCell(context, this)));

        // Floating Search Bar Card
        items.add(UItem.asCustom(createFloatingSearchHeader(context)));

        // What's New Feature Card
        items.add(UItem.asCustom(createCard(context, ITEM_WHATS_NEW, R.drawable.msg_info, 0xFF7C4DFF,
                LocaleController.getString("HundredGramWhatsNew", R.string.HundredGramWhatsNew),
                LocaleController.getString("HundredGramWhatsNewSub", R.string.HundredGramWhatsNewSub),
                "v1.3", false)));
        items.add(UItem.asShadow(null));

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

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_INTERFACE, R.drawable.msg_discussion, 0xFF00B0FF,
                LocaleController.getString("HundredGramCategoryInterface", R.string.HundredGramCategoryInterface),
                LocaleController.getString("HundredGramCategoryInterfaceSub", R.string.HundredGramCategoryInterfaceSub),
                transitionBadge, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_ICON_PACKS, R.drawable.msg_theme, 0xFF7C4DFF,
                LocaleController.getString("HundredGramIconPacks", R.string.HundredGramIconPacks),
                LocaleController.getString("HundredGramIconPacksSub", R.string.HundredGramIconPacksSub),
                HundredGramConfig.iconPacksEnabled ? HundredGramConfig.selectedIconPack : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_PILL_STACK, R.drawable.weather_cloudy, 0xFFFFB300,
                LocaleController.getString("PillStackPills", R.string.PillStackPills),
                LocaleController.getString("PillStackPillsInfo", R.string.PillStackPillsInfo),
                HundredGramConfig.pillStackEnabled ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), false)));
        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 2: ЧАТЫ, МЕДИА И КАМЕРА (CHATS & MEDIA)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionChatsMedia", R.string.HundredGramSectionChatsMedia)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_ACTIONS, R.drawable.msg_actions, 0xFF2979FF,
                LocaleController.getString("HundredGramCategoryActions", R.string.HundredGramCategoryActions),
                LocaleController.getString("HundredGramCategoryActionsSub", R.string.HundredGramCategoryActionsSub),
                null, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_MEDIA, R.drawable.msg_media, 0xFFFF5722,
                LocaleController.getString("HundredGramCategoryMediaQuality", R.string.HundredGramCategoryMediaQuality),
                LocaleController.getString("HundredGramCategoryMediaQualitySub", R.string.HundredGramCategoryMediaQualitySub),
                null, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_CAMERAX, R.drawable.msg_camera, 0xFFE91E63,
                LocaleController.getString("CameraEngineTitle", R.string.CameraEngineTitle),
                LocaleController.getString("CameraEngineSub", R.string.CameraEngineSub),
                HundredGramConfig.cameraXEnabled ? "CameraX" : "Legacy", true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CHANNELS_FEED, R.drawable.msg_channel, 0xFF00ACC1,
                LocaleController.getString("FeedTitle", R.string.FeedTitle),
                LocaleController.getString("FeedSettings", R.string.FeedSettings),
                null, false)));
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

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_GHOST, R.drawable.msg_secret, 0xFF9C27B0,
                LocaleController.getString("HundredGramCategoryGhost", R.string.HundredGramCategoryGhost),
                LocaleController.getString("HundredGramCategoryGhostSub", R.string.HundredGramCategoryGhostSub),
                ghostCount > 0 ? ghostCount + " active" : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), true)));

        int shadowbanCount = org.telegram.messenger.HundredGramShadowbanController.getShadowBannedCount();
        String privacyBadge = shadowbanCount > 0 ? LocaleController.formatPluralString("ShadowbanUsersCount", shadowbanCount, shadowbanCount) : null;
        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_PRIVACY, R.drawable.msg_permissions, 0xFF4CAF50,
                LocaleController.getString("HundredGramCategoryPrivacy", R.string.HundredGramCategoryPrivacy),
                LocaleController.getString("HundredGramCategoryPrivacySub", R.string.HundredGramCategoryPrivacySub),
                privacyBadge, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_STREAMER, R.drawable.msg_channel, 0xFFF44336,
                LocaleController.getString("HundredGramCategoryStreamer", R.string.HundredGramCategoryStreamer),
                LocaleController.getString("HundredGramCategoryStreamerSub", R.string.HundredGramCategoryStreamerSub),
                HundredGramConfig.streamerMode ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), false)));
        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 4: ХРАНИЛИЩЕ И СЕТЬ (STORAGE & NETWORK)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionData", R.string.HundredGramSectionData)));

        // Vault Widget Card
        items.add(UItem.asCustom(new HundredGramVaultStorageCard(context, this)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_NETWORK, R.drawable.msg_stats, 0xFF009688,
                LocaleController.getString("NetworkBoostTitle", R.string.NetworkBoostTitle),
                LocaleController.getString("NetworkBoostSub", R.string.NetworkBoostSub),
                null, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_BACKUP, R.drawable.msg_archive, 0xFF3F51B5,
                LocaleController.getString("HundredGramBackupTitle", R.string.HundredGramBackupTitle),
                LocaleController.getString("HundredGramBackupSubtitle", R.string.HundredGramBackupSubtitle),
                "JSON", false)));
        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 5: ИНТЕЛЛЕКТ И ИНСТРУМЕНТЫ (SMART TOOLS)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionSmartTools", R.string.HundredGramSectionSmartTools)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_AI, R.drawable.msg_bot, 0xFF00C853,
                LocaleController.getString("AiSettingsTitle", R.string.AiSettingsTitle),
                LocaleController.getString("AiSettingsSubtitle", R.string.AiSettingsSubtitle),
                org.telegram.messenger.ai.HundredGramAiConfig.isAiEnabled()
                        ? org.telegram.messenger.ai.HundredGramAiConfig.getSelectedService().getName()
                        : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_SERVICES, R.drawable.msg_translate, 0xFF03A9F4,
                LocaleController.getString("ServicesSettingsTitle", R.string.ServicesSettingsTitle),
                LocaleController.getString("ServicesSettingsSubtitle", R.string.ServicesSettingsSubtitle),
                org.telegram.messenger.translator.HundredGramTranslator.getInstance().getProviderName(HundredGramConfig.translationProvider), true)));

        int pluginsCount = org.telegram.messenger.plugins.PluginsController.getInstance().getPlugins().size();
        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_PLUGINS, R.drawable.msg_bot, 0xFFFF9800,
                LocaleController.getString("Plugins", R.string.Plugins),
                LocaleController.getString("PluginsSubtitle", R.string.PluginsSubtitle),
                HundredGramConfig.pluginsEngine ? String.valueOf(pluginsCount) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff), true)));

        int trackedCount = HundredGramConfig.trackedOnlineUsers.size();
        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_TRACKER, R.drawable.msg_usersearch, 0xFF607D8B,
                LocaleController.getString("HundredGramCategoryTracker", R.string.HundredGramCategoryTracker),
                LocaleController.getString("HundredGramCategoryTrackerSub", R.string.HundredGramCategoryTrackerSub),
                trackedCount > 0 ? trackedCount + " users" : null, true)));

        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_AUTOMATION, R.drawable.msg_bot, 0xFF795548,
                LocaleController.getString("AutomationTitle", R.string.AutomationTitle),
                LocaleController.getString("AutomationSubtitle", R.string.AutomationSubtitle),
                null, false)));

        boolean simRunning = HundredGramActivitySimulator.getInstance(currentAccount).isRunning();
        items.add(UItem.asCustom(createCard(context, ITEM_CATEGORY_SIMULATOR, R.drawable.msg_usersearch, 0xFF00B0FF,
                LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle),
                LocaleController.getString("ActivitySimulatorSubtitle", R.string.ActivitySimulatorSubtitle),
                simRunning ? LocaleController.getString("ActivitySimulatorActiveBadge", R.string.ActivitySimulatorActiveBadge) : null, false)));
        items.add(UItem.asShadow(null));

        // ==========================================
        // SECTION 6: О ПРИЛОЖЕНИИ И АВТОР (ABOUT)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("HundredGramAboutSection", R.string.HundredGramAboutSection)));

        items.add(UItem.asCustom(createCard(context, ITEM_AUTHOR, R.drawable.msg_openprofile, 0xFF607D8B,
                LocaleController.getString("HundredGramAuthor", R.string.HundredGramAuthor),
                LocaleController.getString("HundredGramAuthorSub", R.string.HundredGramAuthorSub),
                "@w1ckeddamn", true)));

        items.add(UItem.asCustom(createCard(context, ITEM_GITHUB, R.drawable.msg_link, 0xFF2196F3,
                LocaleController.getString("HundredGramGitHub", R.string.HundredGramGitHub),
                LocaleController.getString("HundredGramGitHubSub", R.string.HundredGramGitHubSub),
                "GitHub", false)));
        items.add(UItem.asShadow(null));
    }

    private void onSearchResultClicked(HundredGramSettingsSearchIndex.MatchResult result) {
        if (result == null || result.entry == null) return;
        HundredGramSettingsSearchIndex.Entry entry = result.entry;

        // Save to recent searches
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
