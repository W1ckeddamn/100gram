package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

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

    // Quick-action links
    private static final int ITEM_CHECK_UPDATES = 1;
    private static final int ITEM_GITHUB_REPO   = 2;
    private static final int ITEM_AUTHOR_LINK   = 3;

    // v1.2 — кликабельные (переход в нужный раздел)
    private static final int ITEM_WN_STICKER_SIZE = 101;
    private static final int ITEM_WN_ROUND_VIDEO  = 102;
    // v1.2 — информационные (без перехода)
    private static final int ITEM_WN_LAYER229     = 103;
    private static final int ITEM_WN_AUDIT        = 104;

    // v1.1 — информационные
    private static final int ITEM_WN_VAULT        = 201;
    private static final int ITEM_WN_TRACKER      = 202;

    // v1.0 — информационные
    private static final int ITEM_WN_GHOST        = 301;
    private static final int ITEM_WN_ANTI_DELETE  = 302;
    private static final int ITEM_WN_NETWORK      = 303;
    private static final int ITEM_WN_STREAMER     = 304;

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
        final String newBadge = LocaleController.getString("WhatsNewNew", R.string.WhatsNewNew);

        // Раздел: Быстрые действия
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

        // Раздел: Версия 1.2 — Текущая
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewCurrentVersion", R.string.WhatsNewCurrentVersion)));

        items.add(UItem.asButton(ITEM_WN_STICKER_SIZE, R.drawable.msg_media,
                LocaleController.getString("WhatsNewStickerGifSize", R.string.WhatsNewStickerGifSize),
                newBadge));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewStickerGifSizeDesc", R.string.WhatsNewStickerGifSizeDesc)));

        items.add(UItem.asButton(ITEM_WN_ROUND_VIDEO, R.drawable.msg_camera,
                LocaleController.getString("WhatsNewRoundVideo", R.string.WhatsNewRoundVideo),
                newBadge));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewRoundVideoDesc", R.string.WhatsNewRoundVideoDesc)));

        items.add(UItem.asButton(ITEM_WN_LAYER229, R.drawable.msg_bot,
                LocaleController.getString("WhatsNewLayer229", R.string.WhatsNewLayer229),
                newBadge));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewLayer229Desc", R.string.WhatsNewLayer229Desc)));

        items.add(UItem.asButton(ITEM_WN_AUDIT, R.drawable.msg_permissions,
                LocaleController.getString("WhatsNewSettingsAudit", R.string.WhatsNewSettingsAudit),
                newBadge));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewSettingsAuditDesc", R.string.WhatsNewSettingsAuditDesc)));

        // Раздел: Версия 1.1
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewVersion11", R.string.WhatsNewVersion11)));

        items.add(UItem.asButton(ITEM_WN_VAULT, R.drawable.msg_download,
                LocaleController.getString("WhatsNewVault", R.string.WhatsNewVault),
                "v1.1"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewVaultDesc", R.string.WhatsNewVaultDesc)));

        items.add(UItem.asButton(ITEM_WN_TRACKER, R.drawable.msg_usersearch,
                LocaleController.getString("WhatsNewTracker", R.string.WhatsNewTracker),
                "v1.1"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewTrackerDesc", R.string.WhatsNewTrackerDesc)));

        // Раздел: Версия 1.0
        items.add(UItem.asHeader(LocaleController.getString("WhatsNewVersion10", R.string.WhatsNewVersion10)));

        items.add(UItem.asButton(ITEM_WN_GHOST, R.drawable.msg_secret,
                LocaleController.getString("WhatsNewGhost", R.string.WhatsNewGhost),
                "v1.0"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewGhostDesc", R.string.WhatsNewGhostDesc)));

        items.add(UItem.asButton(ITEM_WN_ANTI_DELETE, R.drawable.msg_edit,
                LocaleController.getString("WhatsNewAntiDelete", R.string.WhatsNewAntiDelete),
                "v1.0"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewAntiDeleteDesc", R.string.WhatsNewAntiDeleteDesc)));

        items.add(UItem.asButton(ITEM_WN_NETWORK, R.drawable.msg_stats,
                LocaleController.getString("WhatsNewNetworkBoost", R.string.WhatsNewNetworkBoost),
                "v1.0"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewNetworkBoostDesc", R.string.WhatsNewNetworkBoostDesc)));

        items.add(UItem.asButton(ITEM_WN_STREAMER, R.drawable.msg_channel,
                LocaleController.getString("WhatsNewStreamer", R.string.WhatsNewStreamer),
                "v1.0"));
        items.add(UItem.asShadow(LocaleController.getString("WhatsNewStreamerDesc", R.string.WhatsNewStreamerDesc)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            // Быстрые действия
            case ITEM_CHECK_UPDATES:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram/releases");
                break;
            case ITEM_GITHUB_REPO:
                Browser.openUrl(getParentActivity(), "https://github.com/W1ckeddamn/100gram");
                break;
            case ITEM_AUTHOR_LINK:
                Browser.openUrl(getParentActivity(), "https://t.me/w1ckeddamn");
                break;

            // v1.2 — кликабельные переходы
            case ITEM_WN_STICKER_SIZE:
                presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_MEDIA));
                break;
            case ITEM_WN_ROUND_VIDEO:
                presentFragment(new HundredGramCameraActivity());
                break;
            // v1.2 информационные (ITEM_WN_LAYER229, ITEM_WN_AUDIT) — без действия
            // v1.1/v1.0 — без действия (информационные элементы)
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
