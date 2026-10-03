package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramSettingsActivity extends BaseFragment {

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

    private UniversalRecyclerView listView;

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
        // Раздел 1: Конфиденциальность и безопасность
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionPrivacy", R.string.HundredGramSectionPrivacy)));

        items.add(UItem.asButton(ITEM_CATEGORY_GHOST, R.drawable.msg_secret,
                LocaleController.getString("HundredGramCategoryGhost", R.string.HundredGramCategoryGhost)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryGhostSub", R.string.HundredGramCategoryGhostSub)));

        items.add(UItem.asButton(ITEM_CATEGORY_PRIVACY, R.drawable.msg_permissions,
                LocaleController.getString("HundredGramCategoryPrivacy", R.string.HundredGramCategoryPrivacy)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryPrivacySub", R.string.HundredGramCategoryPrivacySub)));

        items.add(UItem.asButton(ITEM_CATEGORY_STREAMER, R.drawable.msg_channel,
                LocaleController.getString("HundredGramCategoryStreamer", R.string.HundredGramCategoryStreamer)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryStreamerSub", R.string.HundredGramCategoryStreamerSub)));

        items.add(UItem.asButton(ITEM_CATEGORY_VAULT, R.drawable.msg_download,
                LocaleController.getString("HundredGramCategoryVault", R.string.HundredGramCategoryVault)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryVaultSub", R.string.HundredGramCategoryVaultSub)));

        // Раздел 2: Интерфейс и чаты
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionInterface", R.string.HundredGramSectionInterface)));

        items.add(UItem.asButton(ITEM_CATEGORY_INTERFACE, R.drawable.msg_discussion,
                LocaleController.getString("HundredGramCategoryInterface", R.string.HundredGramCategoryInterface)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryInterfaceSub", R.string.HundredGramCategoryInterfaceSub)));

        items.add(UItem.asButton(ITEM_CATEGORY_ACTIONS, R.drawable.msg_actions,
                LocaleController.getString("HundredGramCategoryActions", R.string.HundredGramCategoryActions)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryActionsSub", R.string.HundredGramCategoryActionsSub)));

        // Раздел 3: Медиа и камера
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionMedia", R.string.HundredGramSectionMedia)));

        items.add(UItem.asButton(ITEM_CATEGORY_MEDIA, R.drawable.msg_media,
                LocaleController.getString("HundredGramCategoryMediaQuality", R.string.HundredGramCategoryMediaQuality)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryMediaQualitySub", R.string.HundredGramCategoryMediaQualitySub)));

        items.add(UItem.asButton(ITEM_CATEGORY_CAMERAX, R.drawable.msg_camera,
                LocaleController.getString("CameraEngineTitle", R.string.CameraEngineTitle)));
        items.add(UItem.asShadow(LocaleController.getString("CameraEngineSub", R.string.CameraEngineSub)));

        // Раздел 4: Сеть и производительность
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionNetwork", R.string.HundredGramSectionNetwork)));

        items.add(UItem.asButton(ITEM_CATEGORY_NETWORK, R.drawable.msg_stats,
                LocaleController.getString("NetworkBoostTitle", R.string.NetworkBoostTitle)));
        items.add(UItem.asShadow(LocaleController.getString("NetworkBoostSub", R.string.NetworkBoostSub)));

        // Раздел 5: Инструменты и утилиты
        items.add(UItem.asHeader(LocaleController.getString("HundredGramSectionTools", R.string.HundredGramSectionTools)));

        items.add(UItem.asButton(ITEM_CATEGORY_TRACKER, R.drawable.msg_usersearch,
                LocaleController.getString("HundredGramCategoryTracker", R.string.HundredGramCategoryTracker)));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramCategoryTrackerSub", R.string.HundredGramCategoryTrackerSub)));

        items.add(UItem.asButton(ITEM_CATEGORY_AUTOMATION, R.drawable.msg_bot,
                LocaleController.getString("AutomationTitle", R.string.AutomationTitle)));
        items.add(UItem.asShadow(LocaleController.getString("AutomationSubtitle", R.string.AutomationSubtitle)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_CATEGORY_GHOST:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_GHOST));
                break;
            case ITEM_CATEGORY_PRIVACY:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_PRIVACY));
                break;
            case ITEM_CATEGORY_STREAMER:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_STREAMER));
                break;
            case ITEM_CATEGORY_VAULT:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_VAULT));
                break;
            case ITEM_CATEGORY_INTERFACE:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_INTERFACE));
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
            case ITEM_CATEGORY_NETWORK:
                presentFragment(new HundredGramNetworkBoostActivity());
                break;
            case ITEM_CATEGORY_TRACKER:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_TRACKER));
                break;
            case ITEM_CATEGORY_AUTOMATION:
                presentFragment(new HundredGramAutomationActivity());
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
