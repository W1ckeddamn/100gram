package com.exteragram.messenger.feed;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/* loaded from: D:\extragram\dex\classes.dex */
public abstract class FeedRequestNormalizer {
    private static final Field[] EMPTY_FIELDS;
    private static final ClassMetadata EMPTY_METADATA;
    private static final ConcurrentHashMap<Class<?>, ClassMetadata> metadataCache = new ConcurrentHashMap<>();

    private static long mergeResolvedDialogIds(long j, long j2) {
        if (j == 0) {
            return j2;
        }
        if (j2 == 0 || j == j2) {
            return j;
        }
        return 0L;
    }

    static {
        Field[] fieldArr = new Field[0];
        EMPTY_FIELDS = fieldArr;
        EMPTY_METADATA = new ClassMetadata(null, null, null, null, fieldArr);
    }

    public static TLObject normalize(int i, TLObject tLObject) {
        FeedController peekInstance;
        if (tLObject != null && (peekInstance = FeedController.peekInstance(i)) != null && !peekInstance.hasNoSyntheticIds() && tLObject.getClass().getName().startsWith("org.telegram.tgnet.")) {
            ClassMetadata metadata = getMetadata(tLObject);
            if (metadata.messageIdFields.length != 0 || metadata.invoiceField != null) {
                normalizeMessageIds(i, peekInstance, tLObject, metadata);
                normalizeInvoice(i, peekInstance, getFieldValue(metadata.invoiceField, tLObject));
            }
        }
        return tLObject;
    }

    private static ClassMetadata getMetadata(Object obj) {
        if (obj == null) {
            return EMPTY_METADATA;
        }
        return metadataCache.computeIfAbsent(obj.getClass(), new Function() { // from class: com.exteragram.messenger.feed.FeedRequestNormalizer$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj2) {
                FeedRequestNormalizer.ClassMetadata buildMetadata;
                buildMetadata = FeedRequestNormalizer.buildMetadata((Class) obj2);
                return buildMetadata;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ClassMetadata buildMetadata(Class<?> cls) {
        Field[] fieldArr;
        try {
            fieldArr = cls.getFields();
        } catch (Exception e) {
            reportFailure("fields of ".concat(cls.getName()), e);
            fieldArr = EMPTY_FIELDS;
        }
        Field field = null;
        ArrayList arrayList = null;
        Field field2 = null;
        Field field3 = null;
        Field field4 = null;
        for (Field field5 : fieldArr) {
            String name = field5.getName();
            if ("from_peer".equals(name) && field == null) {
                field = field5;
            } else if ("peer".equals(name) && field2 == null) {
                field2 = field5;
            } else if ("channel".equals(name) && field3 == null) {
                field3 = field5;
            } else if ("invoice".equals(name) && field4 == null) {
                field4 = field5;
            }
            if (isMessageIdField(field5)) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(field5);
            }
        }
        return new ClassMetadata(field != null ? field : field2, field2, field3, field4, arrayList != null ? (Field[]) arrayList.toArray(new Field[0]) : EMPTY_FIELDS);
    }

    private static void normalizeMessageIds(int i, FeedController feedController, Object obj) {
        normalizeMessageIds(i, feedController, obj, getMetadata(obj));
    }

    private static void normalizeMessageIds(int i, FeedController feedController, Object obj, ClassMetadata classMetadata) {
        Field field = classMetadata.requestPeerField;
        long dialogId = getDialogId(field, obj);
        if (dialogId == 0) {
            dialogId = getDialogId(classMetadata.peerField, obj);
        }
        if (dialogId == 0) {
            dialogId = getChannelDialogId(classMetadata.channelField, obj);
        }
        long normalizeMessageIdFields = normalizeMessageIdFields(feedController, obj, classMetadata);
        if (normalizeMessageIdFields == 0 || normalizeMessageIdFields == dialogId) {
            return;
        }
        if (field != null) {
            setInputPeer(i, field, obj, normalizeMessageIdFields);
        } else if (classMetadata.channelField != null) {
            setInputChannel(i, classMetadata.channelField, obj, normalizeMessageIdFields);
        }
    }

    private static long normalizeMessageIdFields(FeedController feedController, Object obj, ClassMetadata classMetadata) {
        long j = 0;
        if (obj == null) {
            return 0L;
        }
        for (Field field : classMetadata.messageIdFields) {
            j = mergeResolvedDialogIds(j, normalizeMessageIdField(feedController, obj, field));
        }
        return j;
    }

    private static boolean isMessageIdField(Field field) {
        if (field == null || Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        String name = field.getName();
        return "id".equals(name) || "msg_id".equals(name) || name.endsWith("_msg_id");
    }

    private static void normalizeInvoice(int i, FeedController feedController, Object obj) {
        if (obj instanceof TLRPC.TL_inputInvoiceMessage) {
            normalizeMessageIds(i, feedController, obj);
        }
    }

    private static long normalizeMessageIdField(FeedController feedController, Object obj, Field field) {
        long j = 0;
        try {
            Object obj2 = field.get(obj);
            if (obj2 instanceof Integer) {
                Integer num = (Integer) obj2;
                long resolveRealDialogId = feedController.resolveRealDialogId(num.intValue());
                if (resolveRealDialogId != 0) {
                    field.setInt(obj, feedController.resolveRealMessageId(resolveRealDialogId, num.intValue()));
                    return resolveRealDialogId;
                }
            } else if (obj2 instanceof ArrayList) {
                ArrayList arrayList = (ArrayList) obj2;
                long j2 = 0;
                for (int i = 0; i < arrayList.size(); i++) {
                    try {
                        Object obj3 = arrayList.get(i);
                        if (obj3 instanceof Integer) {
                            Integer num2 = (Integer) obj3;
                            long resolveRealDialogId2 = feedController.resolveRealDialogId(num2.intValue());
                            if (resolveRealDialogId2 != 0) {
                                setListInteger(arrayList, i, feedController.resolveRealMessageId(resolveRealDialogId2, num2.intValue()));
                                j2 = mergeResolvedDialogIds(j2, resolveRealDialogId2);
                            }
                        }
                    } catch (Exception e) {
                        e = e;
                        j = j2;
                        reportFailure("message id field " + field.getName(), e);
                        return j;
                    }
                }
                return j2;
            }
            return 0L;
        } catch (Exception e2) {
            reportFailure("message id field " + field.getName(), e2);
            return j;
        }
    }

    private static void setListInteger(ArrayList arrayList, int i, int i2) {
        arrayList.set(i, Integer.valueOf(i2));
    }

    private static void setInputPeer(int i, Field field, Object obj, long j) {
        if (i < 0) {
            return;
        }
        try {
            TLRPC.InputPeer inputPeer = MessagesController.getInstance(i).getInputPeer(j);
            if (inputPeer != null) {
                field.set(obj, inputPeer);
            }
        } catch (Exception e) {
            reportFailure("peer field " + field.getName(), e);
        }
    }

    private static void setInputChannel(int i, Field field, Object obj, long j) {
        if (i < 0 || j >= 0) {
            return;
        }
        try {
            TLRPC.InputChannel inputChannel = MessagesController.getInstance(i).getInputChannel(-j);
            if (inputChannel != null) {
                field.set(obj, inputChannel);
            }
        } catch (Exception e) {
            reportFailure("channel field " + field.getName(), e);
        }
    }

    private static long getDialogId(Field field, Object obj) {
        if (field == null) {
            return 0L;
        }
        try {
            Object obj2 = field.get(obj);
            if (obj2 instanceof TLRPC.InputPeer) {
                return DialogObject.getPeerDialogId((TLRPC.InputPeer) obj2);
            }
        } catch (Exception e) {
            reportFailure("peer field " + field.getName(), e);
        }
        return 0L;
    }

    private static long getChannelDialogId(Field field, Object obj) {
        Object fieldValue = getFieldValue(field, obj);
        if (fieldValue instanceof TLRPC.InputChannel) {
            return getInputChannelDialogId((TLRPC.InputChannel) fieldValue);
        }
        return 0L;
    }

    private static long getInputChannelDialogId(TLRPC.InputChannel inputChannel) {
        if (inputChannel == null) {
            return 0L;
        }
        long j = inputChannel.channel_id;
        if (j == 0) {
            return 0L;
        }
        return -j;
    }

    private static Object getFieldValue(Field field, Object obj) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(obj);
        } catch (Exception e) {
            reportFailure("field " + field.getName(), e);
            return null;
        }
    }

    private static void reportFailure(String str, Exception exc) {
        FileLog.e("FeedRequestNormalizer failed to normalize " + str, exc);
    }

    public static final class ClassMetadata {
        final Field channelField;
        final Field invoiceField;
        final Field[] messageIdFields;
        final Field peerField;
        final Field requestPeerField;

        private ClassMetadata(Field field, Field field2, Field field3, Field field4, Field[] fieldArr) {
            this.requestPeerField = field;
            this.peerField = field2;
            this.channelField = field3;
            this.invoiceField = field4;
            this.messageIdFields = fieldArr;
        }

        public final boolean equals(Object obj) {
            if (!(obj instanceof ClassMetadata)) {
                return false;
            }
            ClassMetadata classMetadata = (ClassMetadata) obj;
            return Objects.equals(this.requestPeerField, classMetadata.requestPeerField) && Objects.equals(this.peerField, classMetadata.peerField) && Objects.equals(this.channelField, classMetadata.channelField) && Objects.equals(this.invoiceField, classMetadata.invoiceField) && Arrays.equals(this.messageIdFields, classMetadata.messageIdFields);
        }

        public final int hashCode() {
            return Objects.hash(this.requestPeerField, this.peerField, this.channelField, this.invoiceField, Arrays.hashCode(this.messageIdFields));
        }
    }
}