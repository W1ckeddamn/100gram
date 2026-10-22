package org.telegram.messenger.icons;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import org.telegram.messenger.FileLog;

/**
 * Кастомный перехватчик ресурсов Android Resources для форка 100gram.
 * Бесшовно подменяет иконки интерфейса на лету без необходимости модификации вызывающего кода.
 */
public class HundredGramResources extends Resources {

    private final Resources mResources;
    private final AssetManager sourceAssets;

    public HundredGramResources(Resources resources) {
        super(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
        this.mResources = resources;
        this.sourceAssets = resources.getAssets();
    }

    public AssetManager getSourceAssets() {
        return this.sourceAssets;
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density, Resources.Theme theme) {
        try {
            Drawable customDrawable = HundredGramIconManager.getInstance().getDrawable(id, density, theme, this.mResources);
            if (customDrawable != null) {
                return customDrawable;
            }
        } catch (Throwable th) {
            FileLog.e(th);
        }
        return this.mResources.getDrawableForDensity(id, density, theme);
    }

    public Drawable getOriginalDrawable(int id) {
        try {
            return this.mResources.getDrawable(id, null);
        } catch (Resources.NotFoundException unused) {
            return null;
        }
    }
}
