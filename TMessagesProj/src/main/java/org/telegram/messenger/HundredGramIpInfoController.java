package org.telegram.messenger;

import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.collection.LruCache;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.json.JSONObject;

public final class HundredGramIpInfoController {

    public static final HundredGramIpInfoController INSTANCE = new HundredGramIpInfoController();

    private static final Pattern IPV4_PATTERN = Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){3}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final LruCache<String, CacheEntry> cache = new LruCache<>(256);

    private HundredGramIpInfoController() {
    }

    public static final class IpAddressInfo {
        public final String ip;
        public final String country;
        public final String countryCode;
        public final String region;
        public final String city;
        public final String isp;
        public final String organization;
        public final String domain;
        public final String timezone;

        public IpAddressInfo(String ip, String country, String countryCode, String region, String city, String isp, String organization, String domain, String timezone) {
            this.ip = ip != null ? ip : "";
            this.country = country != null ? country : "";
            this.countryCode = countryCode != null ? countryCode : "";
            this.region = region != null ? region : "";
            this.city = city != null ? city : "";
            this.isp = isp != null ? isp : "";
            this.organization = organization != null ? organization : "";
            this.domain = domain != null ? domain : "";
            this.timezone = timezone != null ? timezone : "";
        }
    }

    private static final class CacheEntry {
        final IpAddressInfo info;
        final long time;
        final long ttl;

        CacheEntry(IpAddressInfo info, long time, long ttl) {
            this.info = info;
            this.time = time;
            this.ttl = ttl;
        }
    }

    public static String extractIpAddress(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String host = null;
        try {
            host = Uri.parse(url).getHost();
        } catch (Exception e) {
            FileLog.e(e, false);
        }
        if (host == null) {
            if (!url.contains("://")) {
                try {
                    host = Uri.parse("http://" + url).getHost();
                } catch (Exception e) {
                    FileLog.e(e, false);
                }
            }
        }
        if (host == null) {
            int slashIndex = url.indexOf('/');
            host = slashIndex >= 0 ? url.substring(0, slashIndex) : url;
            int colonIndex = host.lastIndexOf(':');
            if (colonIndex > 0 && !host.contains("]")) {
                host = host.substring(0, colonIndex);
            }
        }
        if (TextUtils.isEmpty(host)) {
            return null;
        }
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            host = host.substring(1, host.length() - 1);
        }
        if (!isIpAddress(host)) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                return null;
            }
            return address.getHostAddress();
        } catch (Exception e) {
            FileLog.e(e, false);
            return null;
        }
    }

    private static boolean isIpAddress(String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        if (host.indexOf(':') >= 0) {
            return IPV6_PATTERN.matcher(host).matches();
        }
        if (!IPV4_PATTERN.matcher(host).matches()) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            int val = Utilities.parseInt(part);
            if (val < 0 || val > 255) {
                return false;
            }
        }
        return true;
    }

    public static Utilities.Callback<Void> requestIpAddressInfo(final String ipAddress, final Utilities.Callback<IpAddressInfo> callback) {
        if (TextUtils.isEmpty(ipAddress) || callback == null) {
            if (callback != null) {
                callback.run(null);
            }
            return null;
        }
        CacheEntry cached = INSTANCE.getCachedInfo(ipAddress);
        if (cached != null) {
            callback.run(cached.info);
            return null;
        }

        final boolean[] cancelled = new boolean[]{false};
        Utilities.globalQueue.postRunnable(() -> {
            IpAddressInfo result = null;
            HttpURLConnection conn = null;
            try {
                URL requestUrl = new URL("https://ipwho.is/" + ipAddress);
                conn = (HttpURLConnection) requestUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 100gram)");
                conn.connect();

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    in.close();
                    result = INSTANCE.parseIpAddressInfo(ipAddress, sb.toString());
                }
            } catch (Exception e) {
                FileLog.e(e, false);
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }

            final IpAddressInfo finalResult = result;
            INSTANCE.putCachedInfo(ipAddress, finalResult);

            AndroidUtilities.runOnUIThread(() -> {
                if (!cancelled[0]) {
                    callback.run(finalResult);
                }
            });
        });

        return obj -> cancelled[0] = true;
    }

    private synchronized CacheEntry getCachedInfo(String ipAddress) {
        CacheEntry entry = cache.get(ipAddress);
        if (entry != null) {
            if (SystemClock.elapsedRealtime() - entry.time > entry.ttl) {
                cache.remove(ipAddress);
                return null;
            }
            return entry;
        }
        return null;
    }

    private synchronized void putCachedInfo(String ipAddress, IpAddressInfo info) {
        long ttl = info != null ? 86400000L : 300000L;
        cache.put(ipAddress, new CacheEntry(info, SystemClock.elapsedRealtime(), ttl));
    }

    private IpAddressInfo parseIpAddressInfo(String ipAddress, String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (!obj.optBoolean("success", false)) {
                return null;
            }
            JSONObject connection = obj.optJSONObject("connection");
            JSONObject timezone = obj.optJSONObject("timezone");
            String ip = obj.optString("ip", ipAddress);
            String country = obj.optString("country", "");
            String countryCode = obj.optString("country_code", "");
            String region = obj.optString("region", "");
            String city = obj.optString("city", "");
            String isp = connection != null ? connection.optString("isp", "") : "";
            String org = connection != null ? connection.optString("org", "") : "";
            String domain = connection != null ? connection.optString("domain", "") : "";
            String tz = timezone != null ? timezone.optString("id", "") : "";
            return new IpAddressInfo(ip, country, countryCode, region, city, isp, org, domain, tz);
        } catch (Exception e) {
            FileLog.e(e, false);
            return null;
        }
    }

    public static String joinInfo(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        ArrayList<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!TextUtils.isEmpty(p)) {
                list.add(p);
            }
        }
        return TextUtils.join(", ", list);
    }
}
