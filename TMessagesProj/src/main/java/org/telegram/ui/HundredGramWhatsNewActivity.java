package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramWhatsNewActivity extends BaseFragment {

    private static final int ITEM_CHECK_UPDATES = 1;
    private static final int ITEM_GITHUB_REPO   = 2;
    private static final int ITEM_AUTHOR_LINK   = 3;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramWhatsNew", R.string.HundredGramWhatsNew));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context context = getContext();

        // Верхний баннер с кнопками быстрых ссылок
        items.add(UItem.asHeader("100gram Telegram Fork"));
        items.add(UItem.asButton(ITEM_CHECK_UPDATES, R.drawable.msg_help,
                LocaleController.getString("HundredGramCheckUpdates", R.string.HundredGramCheckUpdates),
                "GitHub Releases"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCheckUpdatesSub", R.string.HundredGramCheckUpdatesSub)));

        items.add(UItem.asButton(ITEM_AUTHOR_LINK, R.drawable.msg_openprofile,
                LocaleController.getString("HundredGramAuthor", R.string.HundredGramAuthor),
                "@w1ckeddamn"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramAuthorSub", R.string.HundredGramAuthorSub)));

        items.add(UItem.asButton(ITEM_GITHUB_REPO, R.drawable.msg_link,
                LocaleController.getString("HundredGramGitHub", R.string.HundredGramGitHub),
                "github.com/W1ckeddamn/100gram"));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramGitHubSub", R.string.HundredGramGitHubSub)));

        // ==========================================
        // ВЕРСИЯ 1.2 — ТЕКУЩАЯ (КАРТОЧКИ С АКЦЕНТОМ)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewCurrentVersion", R.string.WhatsNewCurrentVersion)));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_theme,
                LocaleController.getString("WhatsNewSliderStyle", R.string.WhatsNewSliderStyle),
                LocaleController.getString("WhatsNewSliderStyleDesc", R.string.WhatsNewSliderStyleDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_settings,
                LocaleController.getString("WhatsNewUiElevationAndSearchTitle", R.string.WhatsNewUiElevationAndSearchTitle),
                LocaleController.getString("WhatsNewUiElevationAndSearchDesc", R.string.WhatsNewUiElevationAndSearchDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_usersearch,
                LocaleController.getString("WhatsNewActivitySimulatorTitle", R.string.WhatsNewActivitySimulatorTitle),
                LocaleController.getString("WhatsNewActivitySimulatorDesc", R.string.WhatsNewActivitySimulatorDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramActivitySimulatorActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_folders,
                LocaleController.getString("WhatsNewBatch6Title", R.string.WhatsNewBatch6Title),
                LocaleController.getString("WhatsNewBatch6Desc", R.string.WhatsNewBatch6Desc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_channel,
                LocaleController.getString("WhatsNewBatch5Title", R.string.WhatsNewBatch5Title),
                LocaleController.getString("WhatsNewBatch5Desc", R.string.WhatsNewBatch5Desc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> com.exteragram.messenger.feed.ui.FeedActivity.presentFeed(this)
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.outline_header_search,
                LocaleController.getString("WhatsNewRegexAndMention", R.string.WhatsNewRegexAndMention),
                LocaleController.getString("WhatsNewRegexAndMentionDesc", R.string.WhatsNewRegexAndMentionDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_folders,
                LocaleController.getString("WhatsNewSavePresets", R.string.WhatsNewSavePresets),
                LocaleController.getString("WhatsNewSavePresetsDesc", R.string.WhatsNewSavePresetsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_VAULT))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_views,
                LocaleController.getString("WhatsNewAyuGramSpyGhost", R.string.WhatsNewAyuGramSpyGhost),
                LocaleController.getString("WhatsNewAyuGramSpyGhostDesc", R.string.WhatsNewAyuGramSpyGhostDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_GHOST))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.menu_tag_filter,
                LocaleController.getString("WhatsNewContentFilter", R.string.WhatsNewContentFilter),
                LocaleController.getString("WhatsNewContentFilterDesc", R.string.WhatsNewContentFilterDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramContentFilterActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_clear_input,
                LocaleController.getString("WhatsNewDeletedMark", R.string.WhatsNewDeletedMark),
                LocaleController.getString("WhatsNewDeletedMarkDesc", R.string.WhatsNewDeletedMarkDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_edit,
                LocaleController.getString("WhatsNewEditHistoryDiff", R.string.WhatsNewEditHistoryDiff),
                LocaleController.getString("WhatsNewEditHistoryDiffDesc", R.string.WhatsNewEditHistoryDiffDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_PRIVACY))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_block2,
                LocaleController.getString("WhatsNewShadowban", R.string.WhatsNewShadowban),
                LocaleController.getString("WhatsNewShadowbanDesc", R.string.WhatsNewShadowbanDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramShadowbanActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_channel,
                LocaleController.getString("WhatsNewBatch4Features", R.string.WhatsNewBatch4Features),
                LocaleController.getString("WhatsNewBatch4FeaturesDesc", R.string.WhatsNewBatch4FeaturesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_notifications,
                LocaleController.getString("WhatsNewBatch3Features", R.string.WhatsNewBatch3Features),
                LocaleController.getString("WhatsNewBatch3FeaturesDesc", R.string.WhatsNewBatch3FeaturesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_openprofile,
                LocaleController.getString("WhatsNewBatch2Features", R.string.WhatsNewBatch2Features),
                LocaleController.getString("WhatsNewBatch2FeaturesDesc", R.string.WhatsNewBatch2FeaturesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_bot,
                LocaleController.getString("WhatsNewInlineMathAndIp", R.string.WhatsNewInlineMathAndIp),
                LocaleController.getString("WhatsNewInlineMathAndIpDesc", R.string.WhatsNewInlineMathAndIpDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_notifications,
                LocaleController.getString("WhatsNewAccountNotifications", R.string.WhatsNewAccountNotifications),
                LocaleController.getString("WhatsNewAccountNotificationsDesc", R.string.WhatsNewAccountNotificationsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new NotificationsSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_calendar2,
                LocaleController.getString("WhatsNewExteraFeatures", R.string.WhatsNewExteraFeatures),
                LocaleController.getString("WhatsNewExteraFeaturesDesc", R.string.WhatsNewExteraFeaturesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.menu_reply,
                LocaleController.getString("WhatsNewSwipeActions", R.string.WhatsNewSwipeActions),
                LocaleController.getString("WhatsNewSwipeActionsDesc", R.string.WhatsNewSwipeActionsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSwipeActionsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_theme,
                LocaleController.getString("WhatsNewSettingsRedesign", R.string.WhatsNewSettingsRedesign),
                LocaleController.getString("WhatsNewSettingsRedesignDesc", R.string.WhatsNewSettingsRedesignDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_channel,
                LocaleController.getString("WhatsNewMediaAndStorageErgonomics", R.string.WhatsNewMediaAndStorageErgonomics),
                LocaleController.getString("WhatsNewMediaAndStorageErgonomicsDesc", R.string.WhatsNewMediaAndStorageErgonomicsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_archive,
                LocaleController.getString("WhatsNewNavigationAndStickers", R.string.WhatsNewNavigationAndStickers),
                LocaleController.getString("WhatsNewNavigationAndStickersDesc", R.string.WhatsNewNavigationAndStickersDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_video,
                LocaleController.getString("WhatsNewMediaAndBubbles", R.string.WhatsNewMediaAndBubbles),
                LocaleController.getString("WhatsNewMediaAndBubblesDesc", R.string.WhatsNewMediaAndBubblesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_MEDIA))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_folders,
                LocaleController.getString("WhatsNewHeaderTabs", R.string.WhatsNewHeaderTabs),
                LocaleController.getString("WhatsNewHeaderTabsDesc", R.string.WhatsNewHeaderTabsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_edit,
                LocaleController.getString("WhatsNewChatsInput", R.string.WhatsNewChatsInput),
                LocaleController.getString("WhatsNewChatsInputDesc", R.string.WhatsNewChatsInputDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_ACTIONS))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_bot,
                LocaleController.getString("WhatsNewPlugins", R.string.WhatsNewPlugins),
                LocaleController.getString("WhatsNewPluginsDesc", R.string.WhatsNewPluginsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramPluginsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_translate,
                LocaleController.getString("WhatsNewServices", R.string.WhatsNewServices),
                LocaleController.getString("WhatsNewServicesDesc", R.string.WhatsNewServicesDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramServicesSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_bot,
                LocaleController.getString("WhatsNewAiAssistant", R.string.WhatsNewAiAssistant),
                LocaleController.getString("WhatsNewAiAssistantDesc", R.string.WhatsNewAiAssistantDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramAiSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_theme,
                LocaleController.getString("WhatsNewAvatarCornersAndNav", R.string.WhatsNewAvatarCornersAndNav),
                LocaleController.getString("WhatsNewAvatarCornersAndNavDesc", R.string.WhatsNewAvatarCornersAndNavDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_allowspeak,
                LocaleController.getString("WhatsNewLocalStt", R.string.WhatsNewLocalStt),
                LocaleController.getString("WhatsNewLocalSttDesc", R.string.WhatsNewLocalSttDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_ACTIONS))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_media,
                LocaleController.getString("WhatsNewStickerGifSize", R.string.WhatsNewStickerGifSize),
                LocaleController.getString("WhatsNewStickerGifSizeDesc", R.string.WhatsNewStickerGifSizeDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_camera,
                LocaleController.getString("WhatsNewRoundVideo", R.string.WhatsNewRoundVideo),
                LocaleController.getString("WhatsNewRoundVideoDesc", R.string.WhatsNewRoundVideoDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramCameraActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_bot,
                LocaleController.getString("WhatsNewLayer229", R.string.WhatsNewLayer229),
                LocaleController.getString("WhatsNewLayer229Desc", R.string.WhatsNewLayer229Desc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_archive,
                LocaleController.getString("HundredGramBackupTitle", R.string.HundredGramBackupTitle),
                LocaleController.getString("HundredGramBackupSubtitle", R.string.HundredGramBackupSubtitle),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramBackupActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_contacts,
                LocaleController.getString("ProfileChangeLogTitle", R.string.ProfileChangeLogTitle),
                LocaleController.getString("WhatsNewProfileAuditorDesc", R.string.WhatsNewProfileAuditorDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramProfileLogActivity(getUserConfig().getClientUserId()))
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_usersearch,
                LocaleController.getString("WhatsNewTrackedUsers", R.string.WhatsNewTrackedUsers),
                LocaleController.getString("WhatsNewTrackedUsersDesc", R.string.WhatsNewTrackedUsersDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramTrackedUsersActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_theme,
                LocaleController.getString("HundredGramIconPacks", R.string.HundredGramIconPacks),
                LocaleController.getString("HundredGramIconPacksSub", R.string.HundredGramIconPacksSub),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramIconPacksActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.weather_cloudy,
                LocaleController.getString("WhatsNewPillStackWidgets", R.string.WhatsNewPillStackWidgets),
                LocaleController.getString("WhatsNewPillStackWidgetsDesc", R.string.WhatsNewPillStackWidgetsDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramPillStackSettingsActivity())
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_permissions,
                LocaleController.getString("WhatsNewTelemetryKillswitch", R.string.WhatsNewTelemetryKillswitch),
                LocaleController.getString("WhatsNewTelemetryKillswitchDesc", R.string.WhatsNewTelemetryKillswitchDesc),
                LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew),
                true,
                v -> presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_PRIVACY))
        )));
        items.add(UItem.asShadow(null));

        // ==========================================
        // ВЕРСИЯ 1.1 (КАРТОЧКИ ПРЕДЫДУЩИХ ВЕРСИЙ)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewVersion11", R.string.WhatsNewVersion11)));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_download,
                LocaleController.getString("WhatsNewVault", R.string.WhatsNewVault),
                LocaleController.getString("WhatsNewVaultDesc", R.string.WhatsNewVaultDesc),
                "v1.1",
                false,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_usersearch,
                LocaleController.getString("WhatsNewTracker", R.string.WhatsNewTracker),
                LocaleController.getString("WhatsNewTrackerDesc", R.string.WhatsNewTrackerDesc),
                "v1.1",
                false,
                null
        )));
        items.add(UItem.asShadow(null));

        // ==========================================
        // ВЕРСИЯ 1.0 (КАРТОЧКИ ПЕРВОГО РЕЛИЗА)
        // ==========================================
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewVersion10", R.string.WhatsNewVersion10)));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_secret,
                LocaleController.getString("WhatsNewGhost", R.string.WhatsNewGhost),
                LocaleController.getString("WhatsNewGhostDesc", R.string.WhatsNewGhostDesc),
                "v1.0",
                false,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_edit,
                LocaleController.getString("WhatsNewAntiDelete", R.string.WhatsNewAntiDelete),
                LocaleController.getString("WhatsNewAntiDeleteDesc", R.string.WhatsNewAntiDeleteDesc),
                "v1.0",
                false,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_stats,
                LocaleController.getString("WhatsNewNetworkBoost", R.string.WhatsNewNetworkBoost),
                LocaleController.getString("WhatsNewNetworkBoostDesc", R.string.WhatsNewNetworkBoostDesc),
                "v1.0",
                false,
                null
        )));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCustom(createFeatureCard(context,
                R.drawable.msg_channel,
                LocaleController.getString("WhatsNewStreamer", R.string.WhatsNewStreamer),
                LocaleController.getString("WhatsNewStreamerDesc", R.string.WhatsNewStreamerDesc),
                "v1.0",
                false,
                null
        )));
        items.add(UItem.asShadow(null));
    }

    private View createFeatureCard(Context context, int iconResId, CharSequence title, CharSequence description, String badgeText, boolean isNew, View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);

        int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
        if (clickListener != null) {
            int pressedColor = Theme.getColor(Theme.key_listSelector);
            card.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(14), bgColor, pressedColor));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(clickListener);
        } else {
            card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), bgColor));
        }

        int padH = AndroidUtilities.dp(16);
        int padV = AndroidUtilities.dp(14);
        card.setPadding(padH, padV, padH, padV);

        // Верхняя строка: Иконка + Заголовок + Бейдж
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Иконка в кружке
        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        int softIconBg = isNew ? Theme.multAlpha(accentColor, 0.14f) : Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), 0.12f);
        iconBg.setColor(softIconBg);
        iconWrap.setBackground(iconBg);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconResId);
        iconView.setColorFilter(isNew ? accentColor : Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
        iconWrap.addView(iconView, LayoutHelper.createFrame(20, 20, Gravity.CENTER));

        topRow.addView(iconWrap, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        // Название фичи
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        topRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        // Бейдж (НОВОЕ или Версия)
        if (badgeText != null && !badgeText.isEmpty()) {
            TextView badge = new TextView(context);
            badge.setText(badgeText);
            badge.setTextSize(11);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(AndroidUtilities.dp(8));
            if (isNew) {
                badgeBg.setColor(accentColor);
                badge.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
                Drawable star = context.getResources().getDrawable(R.drawable.input_ai_star);
                if (star != null) {
                    star = star.mutate();
                    star.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_buttonText), PorterDuff.Mode.SRC_IN));
                    star.setBounds(0, 0, AndroidUtilities.dp(10), AndroidUtilities.dp(10));
                    badge.setCompoundDrawables(star, null, null, null);
                    badge.setCompoundDrawablePadding(AndroidUtilities.dp(4));
                }
            } else {
                badgeBg.setColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), 0.15f));
                badge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }
            badge.setBackground(badgeBg);

            topRow.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        }

        card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Описание фичи
        if (description != null && description.length() > 0) {
            TextView descView = new TextView(context);
            descView.setText(description);
            descView.setTextSize(13.5f);
            descView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            card.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 48, 8, 0, 0));
        }

        // Если элемент кликабельный, показываем стрелочку перехода внизу
        if (clickListener != null) {
            TextView actionHint = new TextView(context);
            actionHint.setText(LocaleController.getString("Settings", R.string.Settings));
            actionHint.setTextSize(12.5f);
            actionHint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            actionHint.setTextColor(accentColor);
            actionHint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            Drawable arrow = context.getResources().getDrawable(R.drawable.msg_arrowright);
            if (arrow != null) {
                arrow = arrow.mutate();
                arrow.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
                arrow.setBounds(0, 0, AndroidUtilities.dp(12), AndroidUtilities.dp(12));
                actionHint.setCompoundDrawables(null, null, arrow, null);
                actionHint.setCompoundDrawablePadding(AndroidUtilities.dp(4));
            }
            card.addView(actionHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        }

        FrameLayout outer = new FrameLayout(context);
        outer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(4), AndroidUtilities.dp(14), AndroidUtilities.dp(4));
        outer.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return outer;
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_CHECK_UPDATES:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram/releases");
                break;
            case ITEM_GITHUB_REPO:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram");
                break;
            case ITEM_AUTHOR_LINK:
                Browser.openUrl(getParentActivity(), "https://t.me/w1ckeddamn");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
