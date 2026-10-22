package org.telegram.messenger;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import java.util.ArrayList;

public abstract class HundredGramZalgoFilter {

    private static boolean isDirectionControl(int codePoint) {
        if (codePoint == 1564 || codePoint == 8206 || codePoint == 8207) {
            return true;
        }
        if (codePoint < 8234 || codePoint > 8238) {
            return codePoint >= 8294 && codePoint <= 8297;
        }
        return true;
    }

    private static boolean isZalgoMarkRange(int codePoint) {
        if (codePoint >= 768 && codePoint <= 879) {
            return true;
        }
        if (codePoint >= 6832 && codePoint <= 6911) {
            return true;
        }
        if (codePoint >= 7616 && codePoint <= 7679) {
            return true;
        }
        if (codePoint < 8400 || codePoint > 8447) {
            return codePoint >= 65056 && codePoint <= 65071;
        }
        return true;
    }

    public static boolean canFilter(CharSequence charSequence) {
        return HundredGramConfig.filterZalgo && !TextUtils.isEmpty(charSequence) && findReplacementRanges(charSequence) != null;
    }

    public static CharSequence filterSpannable(CharSequence charSequence) {
        if (!canFilter(charSequence)) {
            return charSequence;
        }
        if (!(charSequence instanceof Spannable)) {
            return filter(charSequence);
        }
        Spannable spannable = (Spannable) charSequence;
        SpannableString spannableString = new SpannableString(filterEnabled(spannable.toString()));
        for (Object obj : spannable.getSpans(0, spannable.length(), Object.class)) {
            int min = Math.min(spannable.getSpanStart(obj), spannableString.length());
            int min2 = Math.min(spannable.getSpanEnd(obj), spannableString.length());
            int spanFlags = spannable.getSpanFlags(obj);
            if (min < 0) {
                min = 0;
            }
            if (min2 > spannableString.length()) {
                min2 = spannableString.length();
            }
            if (min > min2) {
                min = min2;
            }
            spannableString.setSpan(obj, min, min2, spanFlags);
        }
        return spannableString;
    }

    public static String filter(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        return filter(charSequence.toString());
    }

    public static String filter(String str) {
        return (!HundredGramConfig.filterZalgo || TextUtils.isEmpty(str)) ? str : filterEnabled(str);
    }

    private static String filterEnabled(String str) {
        ArrayList<int[]> findReplacementRanges = findReplacementRanges(str);
        if (findReplacementRanges == null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        int size = findReplacementRanges.size();
        int i = 0;
        int i2 = 0;
        while (i2 < size) {
            int[] iArr2 = findReplacementRanges.get(i2);
            i2++;
            sb.append((CharSequence) str, i, iArr2[0]);
            appendReplacement(sb, iArr2[1] - iArr2[0]);
            i = iArr2[1];
        }
        sb.append((CharSequence) str, i, str.length());
        return sb.toString();
    }

    private static ArrayList<int[]> findReplacementRanges(CharSequence charSequence) {
        int length = charSequence.length();
        ArrayList<int[]> arrayList = null;
        int i = -1;
        int i2 = 0;
        int i3 = 0;
        int i4 = 4;
        while (i2 < length) {
            int codePointAt = Character.codePointAt(charSequence, i2);
            int charCount = Character.charCount(codePointAt);
            int allowedMarksPerSequence = getAllowedMarksPerSequence(codePointAt);
            if (allowedMarksPerSequence > 0) {
                if (i < 0) {
                    i3 = 0;
                    i = i2;
                } else {
                    allowedMarksPerSequence = Math.min(i4, allowedMarksPerSequence);
                }
                i3++;
                i4 = allowedMarksPerSequence;
            } else {
                arrayList = addMarkSequenceRange(arrayList, i, i2, i3, i4);
                if (isDirectionControl(codePointAt)) {
                    arrayList = addRange(arrayList, i2, i2 + charCount);
                }
                i = -1;
                i3 = 0;
                i4 = 4;
            }
            i2 += charCount;
        }
        return addMarkSequenceRange(arrayList, i, length, i3, i4);
    }

    private static ArrayList<int[]> addMarkSequenceRange(ArrayList<int[]> arrayList, int i, int i2, int i3, int i4) {
        return (i < 0 || i3 <= i4) ? arrayList : addRange(arrayList, i, i2);
    }

    private static ArrayList<int[]> addRange(ArrayList<int[]> arrayList, int i, int i2) {
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(new int[]{i, i2});
        return arrayList;
    }

    private static void appendReplacement(StringBuilder sb, int count) {
        for (int i2 = 0; i2 < count; i2++) {
            sb.append((char) 8288);
        }
    }

    private static int getAllowedMarksPerSequence(int codePoint) {
        if (codePoint < 768) {
            return 0;
        }
        int type = Character.getType(codePoint);
        if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK) {
            return isZalgoMarkRange(codePoint) ? 2 : 4;
        }
        return 0;
    }
}
