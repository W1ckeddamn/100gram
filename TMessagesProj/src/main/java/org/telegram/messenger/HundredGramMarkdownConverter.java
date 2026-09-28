package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HundredGramMarkdownConverter {

    public static String toMarkdown(MessageObject messageObject) {
        if (messageObject == null) {
            return "";
        }
        CharSequence messageText = messageObject.messageText;
        if (messageText == null) {
            if (messageObject.messageOwner != null && messageObject.messageOwner.message != null) {
                messageText = messageObject.messageOwner.message;
            } else {
                return "";
            }
        }
        ArrayList<TLRPC.MessageEntity> entities = messageObject.messageOwner != null ? messageObject.messageOwner.entities : null;
        return toMarkdown(messageText, entities);
    }

    public static String toMarkdown(CharSequence rawText, ArrayList<TLRPC.MessageEntity> entities) {
        if (rawText == null) {
            return "";
        }
        String text = rawText.toString();
        if (entities == null || entities.isEmpty()) {
            return text;
        }

        int textLength = text.length();

        // Tag points at each index
        class Tag {
            final int index;
            final boolean isClose;
            final int priority;
            final String textTag;

            Tag(int index, boolean isClose, int priority, String textTag) {
                this.index = index;
                this.isClose = isClose;
                this.priority = priority;
                this.textTag = textTag;
            }
        }

        ArrayList<Tag> tags = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            TLRPC.MessageEntity entity = entities.get(i);
            if (entity == null) continue;

            int start = Math.max(0, Math.min(entity.offset, textLength));
            int end = Math.max(start, Math.min(entity.offset + entity.length, textLength));
            if (start == end) continue;

            String openTag = "";
            String closeTag = "";
            int priority = 0;

            if (entity instanceof TLRPC.TL_messageEntityBold) {
                openTag = "**";
                closeTag = "**";
                priority = 10;
            } else if (entity instanceof TLRPC.TL_messageEntityItalic) {
                openTag = "*";
                closeTag = "*";
                priority = 20;
            } else if (entity instanceof TLRPC.TL_messageEntityStrike) {
                openTag = "~~";
                closeTag = "~~";
                priority = 30;
            } else if (entity instanceof TLRPC.TL_messageEntityUnderline) {
                openTag = "<u>";
                closeTag = "</u>";
                priority = 40;
            } else if (entity instanceof TLRPC.TL_messageEntityCode) {
                openTag = "`";
                closeTag = "`";
                priority = 50;
            } else if (entity instanceof TLRPC.TL_messageEntityPre) {
                TLRPC.TL_messageEntityPre pre = (TLRPC.TL_messageEntityPre) entity;
                String lang = pre.language != null ? pre.language : "";
                openTag = "```" + lang + "\n";
                closeTag = "\n```";
                priority = 60;
            } else if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                TLRPC.TL_messageEntityTextUrl textUrl = (TLRPC.TL_messageEntityTextUrl) entity;
                String url = textUrl.url != null ? textUrl.url : "";
                openTag = "[";
                closeTag = "](" + url + ")";
                priority = 70;
            } else if (entity instanceof TLRPC.TL_messageEntityBlockquote) {
                openTag = "> ";
                closeTag = "";
                priority = 80;
            } else if (entity instanceof TLRPC.TL_messageEntitySpoiler) {
                openTag = "||";
                closeTag = "||";
                priority = 90;
            }

            if (!openTag.isEmpty() || !closeTag.isEmpty()) {
                tags.add(new Tag(start, false, priority, openTag));
                tags.add(new Tag(end, true, priority, closeTag));
            }
        }

        if (tags.isEmpty()) {
            return text;
        }

        // Sort tags:
        // 1. By index ascending
        // 2. At same index: closing tags before opening tags
        // 3. For closing tags: higher priority first (LIFO)
        // 4. For opening tags: lower priority first (FIFO)
        Collections.sort(tags, new Comparator<Tag>() {
            @Override
            public int compare(Tag t1, Tag t2) {
                if (t1.index != t2.index) {
                    return Integer.compare(t1.index, t2.index);
                }
                if (t1.isClose != t2.isClose) {
                    return t1.isClose ? -1 : 1;
                }
                if (t1.isClose) {
                    return Integer.compare(t2.priority, t1.priority);
                } else {
                    return Integer.compare(t1.priority, t2.priority);
                }
            }
        });

        StringBuilder sb = new StringBuilder();
        int currentIndex = 0;

        for (Tag tag : tags) {
            if (tag.index > currentIndex && currentIndex < textLength) {
                int clamp = Math.min(tag.index, textLength);
                sb.append(text, currentIndex, clamp);
                currentIndex = clamp;
            }
            sb.append(tag.textTag);
        }

        if (currentIndex < textLength) {
            sb.append(text, currentIndex, textLength);
        }

        return sb.toString();
    }
}
