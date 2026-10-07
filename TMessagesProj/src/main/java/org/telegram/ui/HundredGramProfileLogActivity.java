package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramProfileLogger;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HundredGramProfileLogActivity extends BaseFragment {

    private static final int MENU_CLEAR = 1;

    private final long userId;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private EmptyTextProgressView emptyView;
    private final ArrayList<HundredGramProfileLogger.ProfileChangeEntry> entries = new ArrayList<>();
    private boolean loading = true;

    public HundredGramProfileLogActivity(long userId) {
        this.userId = userId;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        loadEntries();
        return true;
    }

    private void loadEntries() {
        loading = true;
        HundredGramProfileLogger.getInstance().getProfileChanges(currentAccount, userId, 200, result -> {
            loading = false;
            entries.clear();
            if (result != null) {
                entries.addAll(result);
            }
            if (emptyView != null) {
                emptyView.showTextView();
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ProfileChangeLogTitle", R.string.ProfileChangeLogTitle));

        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        if (user != null) {
            actionBar.setSubtitle(UserObject.getUserName(user));
        }

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_CLEAR, R.drawable.msg_delete);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_CLEAR) {
                    if (getParentActivity() == null || entries.isEmpty()) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("ClearHistory", R.string.ClearHistory));
                    builder.setMessage(LocaleController.getString("ProfileDeleteHistoryConfirm", R.string.ProfileDeleteHistoryConfirm));
                    builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                        HundredGramProfileLogger.getInstance().clearProfileChanges(currentAccount, userId, () -> {
                            entries.clear();
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            BulletinFactory.of(HundredGramProfileLogActivity.this).createSimpleBulletin(
                                    R.raw.chats_infotip,
                                    LocaleController.getString("HistoryCleared", R.string.HistoryCleared)
                            ).show();
                        });
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    AlertDialog dialog = builder.create();
                    showDialog(dialog);
                    TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                    }
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString("ProfileChangeLogEmpty", R.string.ProfileChangeLogEmpty));
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setEmptyView(emptyView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= entries.size() || getParentActivity() == null) {
                return;
            }
            HundredGramProfileLogger.ProfileChangeEntry entry = entries.get(position);
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getFieldDisplayName(entry.fieldName));
            CharSequence[] items = new CharSequence[]{
                    LocaleController.getString("Copy", R.string.Copy) + ": " + entry.newValue,
                    !TextUtils.isEmpty(entry.oldValue) ? LocaleController.getString("Copy", R.string.Copy) + " (" + LocaleController.getString("ProfileChangeWas", R.string.ProfileChangeWas) + "): " + entry.oldValue : null
            };
            ArrayList<CharSequence> finalItems = new ArrayList<>();
            for (CharSequence item : items) {
                if (item != null) finalItems.add(item);
            }
            builder.setItems(finalItems.toArray(new CharSequence[0]), (dialog, which) -> {
                String toCopy = which == 0 ? entry.newValue : entry.oldValue;
                AndroidUtilities.addToClipboard(toCopy);
                BulletinFactory.of(HundredGramProfileLogActivity.this).createCopyBulletin(
                        LocaleController.getString("TextCopied", R.string.TextCopied)
                ).show();
            });
            showDialog(builder.create());
        });

        if (loading) {
            emptyView.showProgress();
        } else {
            emptyView.showTextView();
        }

        return fragmentView;
    }

    private String getFieldDisplayName(String field) {
        if ("name".equals(field)) {
            return "👤 " + LocaleController.getString("ProfileFieldName", R.string.ProfileFieldName);
        } else if ("username".equals(field)) {
            return "@ " + LocaleController.getString("ProfileFieldUsername", R.string.ProfileFieldUsername);
        } else if ("phone".equals(field)) {
            return "📱 " + LocaleController.getString("ProfileFieldPhone", R.string.ProfileFieldPhone);
        } else if ("bio".equals(field)) {
            return "📝 " + LocaleController.getString("ProfileFieldBio", R.string.ProfileFieldBio);
        } else if ("birthday".equals(field)) {
            return "🎂 " + LocaleController.getString("ProfileFieldBirthday", R.string.ProfileFieldBirthday);
        } else if ("gifts".equals(field)) {
            return "🎁 " + LocaleController.getString("ProfileFieldGifts", R.string.ProfileFieldGifts);
        }
        return field;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new ProfileChangeCell(mContext));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ProfileChangeCell cell = (ProfileChangeCell) holder.itemView;
            HundredGramProfileLogger.ProfileChangeEntry entry = entries.get(position);
            String fieldTitle = getFieldDisplayName(entry.fieldName);
            String dateFormatted = dateFormat.format(new Date(entry.date * 1000L));
            boolean isLast = position == entries.size() - 1;
            cell.setData(fieldTitle, entry.oldValue, entry.newValue, dateFormatted, !isLast);
        }
    }

    private class ProfileChangeCell extends FrameLayout {

        private final TextView fieldTextView;
        private final TextView newTextView;
        private final TextView oldTextView;
        private final TextView dateTextView;
        private final View divider;

        ProfileChangeCell(Context context) {
            super(context);
            setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

            // Header line: Field name and Date
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            fieldTextView = new TextView(context);
            fieldTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            fieldTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4));
            fieldTextView.setTypeface(AndroidUtilities.bold());
            topRow.addView(fieldTextView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            dateTextView = new TextView(context);
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            dateTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            dateTextView.setGravity(Gravity.END);
            topRow.addView(dateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            layout.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

            // New value
            newTextView = new TextView(context);
            newTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            newTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            newTextView.setTypeface(AndroidUtilities.bold());
            newTextView.setMaxLines(4);
            newTextView.setEllipsize(TextUtils.TruncateAt.END);
            layout.addView(newTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

            // Old value (Was: ...)
            oldTextView = new TextView(context);
            oldTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            oldTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            oldTextView.setMaxLines(3);
            oldTextView.setEllipsize(TextUtils.TruncateAt.END);
            layout.addView(oldTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(layout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            divider = new View(context);
            divider.setBackgroundColor(getThemedColor(Theme.key_divider));
            addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1f / context.getResources().getDisplayMetrics().density,
                    Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 16, 0, 0, 0));
        }

        void setData(String fieldTitle, String oldValue, String newValue, String dateText, boolean showDivider) {
            fieldTextView.setText(fieldTitle);
            dateTextView.setText(dateText);

            String displayNew = !TextUtils.isEmpty(newValue) ? newValue : "—";
            newTextView.setText(displayNew);

            if (!TextUtils.isEmpty(oldValue)) {
                oldTextView.setVisibility(View.VISIBLE);
                oldTextView.setText(LocaleController.getString("ProfileChangeWas", R.string.ProfileChangeWas) + " " + oldValue);
            } else {
                oldTextView.setVisibility(View.GONE);
            }

            divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }
    }
}
