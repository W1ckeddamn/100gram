package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.Service;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramAiServicesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_NEW_SERVICE = 1;

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.aiServicesUpdated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.aiServicesUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.aiServicesUpdated) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Services", R.string.Services));
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

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asTopView(LocaleController.getString("Services", R.string.Services), LocaleController.getString("ServicesInfo", R.string.ServicesInfo), "exteraGramPlaceholders", "🔑"));
        items.add(UItem.asHeader(LocaleController.getString("Services", R.string.Services)));

        ArrayList<Service> services = HundredGramAiConfig.getServices();
        for (int i = 0; i < services.size(); i++) {
            Service service = services.get(i);
            String title = service.getName() + " (" + service.getShortModel() + ")";
            String subtitle = service.getUrl();
            UItem radioItem = UItem.asRadio2(100 + i, title, subtitle).setChecked(service.isSelected());
            radioItem.object = service;
            items.add(radioItem);
        }

        UItem newServiceBtn = UItem.asButton(ITEM_NEW_SERVICE, R.drawable.msg_add, LocaleController.getString("NewService", R.string.NewService)).accent();
        newServiceBtn.pad = 61;
        items.add(newServiceBtn);
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_NEW_SERVICE) {
            presentFragment(new HundredGramEditServiceActivity(null));
            return;
        }

        if (item.object instanceof Service) {
            Service service = (Service) item.object;
            if (!service.isSelected()) {
                HundredGramAiConfig.setSelectedService(service);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.aiServicesUpdated);
            }
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof Service) {
            final Service service = (Service) item.object;
            ItemOptions options = ItemOptions.makeOptions(this, view);
            options.add(R.drawable.msg_edit, LocaleController.getString("Edit", R.string.Edit), () -> {
                presentFragment(new HundredGramEditServiceActivity(service));
            });
            options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
                if (AndroidUtilities.addToClipboard(service.getName() + "\n" + service.getUrl() + "\n" + service.getModel())) {
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            });
            ArrayList<Service> services = HundredGramAiConfig.getServices();
            if (services.size() > 1) {
                options.add(R.drawable.msg_delete, LocaleController.getString("Delete", R.string.Delete), true, () -> {
                    confirmDeleteService(service);
                });
            }
            options.setGravity(LocaleController.isRTL ? 3 : 5);
            options.show();
            return true;
        }
        return false;
    }

    private void confirmDeleteService(final Service service) {
        Activity parentActivity = getParentActivity();
        if (parentActivity == null) return;

        AlertDialog dialog = new AlertDialog.Builder(parentActivity)
                .setTitle(LocaleController.getString("Delete", R.string.Delete))
                .setMessage(LocaleController.getString("DeleteServiceInfo", R.string.DeleteServiceInfo))
                .setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, which) -> {
                    if (HundredGramAiConfig.deleteService(service)) {
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                        getNotificationCenter().postNotificationName(NotificationCenter.aiServicesUpdated);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .create();

        showDialog(dialog);
        View btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn instanceof TextView) {
            ((TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }
}
