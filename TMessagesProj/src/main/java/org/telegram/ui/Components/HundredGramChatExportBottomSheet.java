package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramChatExporter;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.util.ArrayList;

public class HundredGramChatExportBottomSheet extends BottomSheetWithRecyclerListView {

    private static final int ITEM_FORMAT = 1;
    private static final int ITEM_RANGE = 2;
    private static final int ITEM_INCLUDE_DELETED = 3;
    private static final int ITEM_INCLUDE_EDITS = 4;
    private static final int ITEM_INCLUDE_MEDIA = 5;
    private static final int ITEM_START_EXPORT = 6;

    private final long dialogId;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;

    private int selectedFormat = HundredGramChatExporter.FORMAT_HTML;
    private int selectedLimit = 500; // 100, 500, 1000, -1
    private boolean includeDeleted = true;
    private boolean includeEdits = true;
    private boolean includeMedia = true;

    private boolean isExporting = false;
    private AlertDialog progressDialog;

    public HundredGramChatExportBottomSheet(BaseFragment fragment, long dialogId) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.dialogId = dialogId;

        topPadding = 0.35f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        recyclerListView.setOnItemClickListener((view, position, x, y) -> {
            if (adapter == null || isExporting) return;
            UItem item = adapter.getItem(position - 1);
            if (item == null) return;

            switch (item.id) {
                case ITEM_FORMAT:
                    showFormatPickerDialog();
                    break;
                case ITEM_RANGE:
                    showRangePickerDialog();
                    break;
                case ITEM_INCLUDE_DELETED:
                    includeDeleted = !includeDeleted;
                    adapter.update(true);
                    break;
                case ITEM_INCLUDE_EDITS:
                    includeEdits = !includeEdits;
                    adapter.update(true);
                    break;
                case ITEM_INCLUDE_MEDIA:
                    includeMedia = !includeMedia;
                    adapter.update(true);
                    break;
                case ITEM_START_EXPORT:
                    startExportProcess();
                    break;
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString("ChatExportTitle", R.string.ChatExportTitle);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Format selector
        items.add(UItem.asButton(ITEM_FORMAT, R.drawable.msg_log,
                LocaleController.getString("ChatExportFormat", R.string.ChatExportFormat),
                getFormatName(selectedFormat)));

        // Range selector
        items.add(UItem.asButton(ITEM_RANGE, R.drawable.msg_calendar2,
                LocaleController.getString("ChatExportRange", R.string.ChatExportRange),
                getRangeName(selectedLimit)));

        items.add(UItem.asShadow(null));

        // Options
        items.add(UItem.asCheck(ITEM_INCLUDE_DELETED,
                LocaleController.getString("ChatExportIncludeDeleted", R.string.ChatExportIncludeDeleted))
                .setChecked(includeDeleted));

        items.add(UItem.asCheck(ITEM_INCLUDE_EDITS,
                LocaleController.getString("ChatExportIncludeEdits", R.string.ChatExportIncludeEdits))
                .setChecked(includeEdits));

        items.add(UItem.asCheck(ITEM_INCLUDE_MEDIA,
                LocaleController.getString("ChatExportIncludeMediaLinks", R.string.ChatExportIncludeMediaLinks))
                .setChecked(includeMedia));

        items.add(UItem.asShadow(null));

        // Action button
        items.add(UItem.asButton(ITEM_START_EXPORT, R.drawable.msg_download,
                LocaleController.getString("ChatExportStart", R.string.ChatExportStart), ""));
    }

    private String getFormatName(int format) {
        switch (format) {
            case HundredGramChatExporter.FORMAT_HTML:
                return "HTML (Dark Webpage)";
            case HundredGramChatExporter.FORMAT_JSON:
                return "JSON (Data & Metadata)";
            case HundredGramChatExporter.FORMAT_MARKDOWN:
                return "Markdown (.md)";
            case HundredGramChatExporter.FORMAT_TXT:
            default:
                return "Text (.txt)";
        }
    }

    private String getRangeName(int limit) {
        if (limit == 100) {
            return LocaleController.getString("ChatExportRange100", R.string.ChatExportRange100);
        } else if (limit == 500) {
            return LocaleController.getString("ChatExportRange500", R.string.ChatExportRange500);
        } else if (limit == 1000) {
            return LocaleController.getString("ChatExportRange1000", R.string.ChatExportRange1000);
        } else {
            return LocaleController.getString("ChatExportRangeAll", R.string.ChatExportRangeAll);
        }
    }

    private void showFormatPickerDialog() {
        Context context = getContext();
        if (context == null) return;

        CharSequence[] formats = new CharSequence[] {
                "HTML (Dark Telegram Webpage)",
                "JSON (Data & Metadata)",
                "Markdown (.md)",
                "Text (.txt)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("ChatExportFormat", R.string.ChatExportFormat));
        builder.setItems(formats, (dialog, which) -> {
            selectedFormat = which;
            if (adapter != null) adapter.update(true);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showRangePickerDialog() {
        Context context = getContext();
        if (context == null) return;

        final int[] limits = new int[] { 100, 500, 1000, -1 };
        CharSequence[] ranges = new CharSequence[] {
                LocaleController.getString("ChatExportRange100", R.string.ChatExportRange100),
                LocaleController.getString("ChatExportRange500", R.string.ChatExportRange500),
                LocaleController.getString("ChatExportRange1000", R.string.ChatExportRange1000),
                LocaleController.getString("ChatExportRangeAll", R.string.ChatExportRangeAll)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("ChatExportRange", R.string.ChatExportRange));
        builder.setItems(ranges, (dialog, which) -> {
            selectedLimit = limits[which];
            if (adapter != null) adapter.update(true);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void startExportProcess() {
        if (isExporting) return;
        Context context = getContext();
        if (context == null) return;

        isExporting = true;
        progressDialog = new AlertDialog(context, 3);
        progressDialog.setCanCancel(false);
        progressDialog.show();

        HundredGramChatExporter.ExportOptions options = new HundredGramChatExporter.ExportOptions(
                dialogId,
                currentAccount,
                selectedFormat,
                selectedLimit,
                includeDeleted,
                includeEdits,
                includeMedia
        );

        HundredGramChatExporter.exportChat(options, new HundredGramChatExporter.ExportCallback() {
            @Override
            public void onProgress(int current, int total) {
                // Background progress
            }

            @Override
            public void onSuccess(File exportedFile) {
                isExporting = false;
                if (progressDialog != null) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignored) {}
                    progressDialog = null;
                }
                dismiss();
                showExportSuccessDialog(exportedFile);
            }

            @Override
            public void onError(String error) {
                isExporting = false;
                if (progressDialog != null) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignored) {}
                    progressDialog = null;
                }
                if (fragment != null && fragment.getParentActivity() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString("ChatExportTitle", R.string.ChatExportTitle));
                    builder.setMessage(error != null ? error : LocaleController.getString("ChatExportError", R.string.ChatExportError));
                    builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                    builder.show();
                }
            }
        });
    }

    private void showExportSuccessDialog(File exportedFile) {
        if (fragment == null || fragment.getParentActivity() == null || exportedFile == null || !exportedFile.exists()) {
            return;
        }

        Activity activity = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, resourcesProvider);
        builder.setTitle(LocaleController.getString("ChatExportSuccess", R.string.ChatExportSuccess));
        builder.setMessage(exportedFile.getName() + " (" + AndroidUtilities.formatFileSize(exportedFile.length()) + ")");

        builder.setPositiveButton(LocaleController.getString("ChatExportOpen", R.string.ChatExportOpen), (dialog, which) -> {
            openExportedFile(activity, exportedFile);
        });

        builder.setNeutralButton(LocaleController.getString("ChatExportShare", R.string.ChatExportShare), (dialog, which) -> {
            shareExportedFile(activity, exportedFile);
        });

        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void openExportedFile(Activity activity, File file) {
        try {
            String mimeType = getMimeType(file.getName());
            boolean opened = AndroidUtilities.openForView(file, file.getName(), mimeType, activity, resourcesProvider, false);
            if (!opened) {
                shareExportedFile(activity, file);
            }
        } catch (Exception e) {
            FileLog.e(e);
            shareExportedFile(activity, file);
        }
    }

    private void shareExportedFile(Activity activity, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(getMimeType(file.getName()));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, LocaleController.getString("ChatExportShare", R.string.ChatExportShare)));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".md")) return "text/markdown";
        return "text/plain";
    }
}
