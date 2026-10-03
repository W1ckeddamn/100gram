package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramAvatarHistory;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HundredGramAvatarHistoryActivity extends BaseFragment {

    private final long dialogId;
    private RecyclerListView listView;
    private AvatarAdapter adapter;
    private LinearLayout emptyView;
    private ArrayList<HundredGramAvatarHistory.AvatarEntry> entries = new ArrayList<>();

    public HundredGramAvatarHistoryActivity(long dialogId) {
        super(createArgs(dialogId));
        this.dialogId = dialogId;
    }

    public HundredGramAvatarHistoryActivity(Bundle args) {
        super(args);
        this.dialogId = args != null ? args.getLong("dialog_id", 0) : 0;
    }

    private static Bundle createArgs(long dialogId) {
        Bundle args = new Bundle();
        args.putLong("dialog_id", dialogId);
        return args;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AvatarHistoryTitle", R.string.AvatarHistoryTitle));
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

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new GridLayoutManager(context, 3));
        listView.setAdapter(adapter = new AvatarAdapter(context));
        listView.setClipToPadding(false);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(16));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);

        ImageView emptyImageView = new ImageView(context);
        emptyImageView.setImageResource(R.drawable.msg_photos);
        emptyImageView.setColorFilter(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyView.addView(emptyImageView, LayoutHelper.createLinear(72, 72, Gravity.CENTER, 0, 0, 0, 16));

        TextView emptyTextView = new TextView(context);
        emptyTextView.setText(LocaleController.getString("AvatarHistoryEmpty", R.string.AvatarHistoryEmpty));
        emptyTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        emptyTextView.setGravity(Gravity.CENTER);
        emptyView.addView(emptyTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 32, 0, 32, 0));

        contentView.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= entries.size()) return;
            HundredGramAvatarHistory.AvatarEntry entry = entries.get(position);
            showAvatarOptions(view, entry);
        });

        actionBar.setAdaptiveBackground(listView);
        loadEntries();

        return fragmentView = contentView;
    }

    private void loadEntries() {
        entries = HundredGramAvatarHistory.getInstance().getAvatarHistory(dialogId);
        if (entries.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            actionBar.setTitle(LocaleController.formatString("AvatarHistoryCount", R.string.AvatarHistoryCount, entries.size()));
        }
    }

    private void showAvatarOptions(View view, HundredGramAvatarHistory.AvatarEntry entry) {
        if (getParentActivity() == null) return;
        final File file = new File(entry.localPath);
        if (!file.exists()) return;

        ItemOptions options = ItemOptions.makeOptions(this, view);
        options.setScrimViewBackground(new ColorDrawable(Color.argb(80, 0, 0, 0)));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(entry.timestamp));
        options.addText(LocaleController.formatString("AvatarHistoryRecordedAt", R.string.AvatarHistoryRecordedAt, dateStr), 13);
        options.addGap();

        options.add(R.drawable.msg_download, LocaleController.getString("AvatarHistorySaveGallery", R.string.AvatarHistorySaveGallery), () -> {
            boolean saved = HundredGramAvatarHistory.saveToGallery(getParentActivity(), file);
            if (saved) {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        LocaleController.getString("AvatarHistorySaved", R.string.AvatarHistorySaved)
                ).show();
            }
        });

        options.add(R.drawable.msg_share, LocaleController.getString("ShareFile", R.string.ShareFile), () -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                // Fix: определяем MIME-тип по расширению файла (аватары бывают .jpg, .png, .webp)
                String lowerPath = file.getName().toLowerCase(Locale.ROOT);
                String mimeType = "image/jpeg";
                if (lowerPath.endsWith(".png")) mimeType = "image/png";
                else if (lowerPath.endsWith(".webp")) mimeType = "image/webp";
                intent.setType(mimeType);
                Uri uri = FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getParentActivity().startActivity(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)));
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        options.show();
    }

    private class AvatarAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());

        public AvatarAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frameLayout = new FrameLayout(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    // Make square cells
                    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
                }
            };
            frameLayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            frameLayout.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(AndroidUtilities.dp(8));
            frameLayout.addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            TextView dateTextView = new TextView(context);
            dateTextView.setTextColor(Color.WHITE);
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            dateTextView.setBackgroundColor(Color.argb(120, 0, 0, 0));
            dateTextView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
            frameLayout.addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT, 4, 0, 4, 4));

            return new RecyclerListView.Holder(frameLayout);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FrameLayout frame = (FrameLayout) holder.itemView;
            BackupImageView imageView = (BackupImageView) frame.getChildAt(0);
            TextView dateTextView = (TextView) frame.getChildAt(1);

            HundredGramAvatarHistory.AvatarEntry entry = entries.get(position);
            imageView.setImage(entry.localPath, null, null);
            dateTextView.setText(dateFormat.format(new Date(entry.timestamp)));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}
