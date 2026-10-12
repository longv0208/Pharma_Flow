package util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPAY 2.1.0 URL building + signature verification. No DB, no Servlet code —
 * only the crypto and parameter plumbing the payment flow needs.
 *
 * Signing convention used everywhere in this class:
 *   - parameter names sorted ascending
 *   - joined as name=value with '&', each name AND value URL-encoded US-ASCII
 *   - HMAC-SHA512 with the configured hash secret, compared timing-safe
 *
 * The exact same encoding is used for the outgoing payment URL and the return
 * signature check, so the two can never drift apart.
 */
public final class VnpayUtil {

    private static final Logger LOG = Logger.getLogger(VnpayUtil.class.getName());

    /** VNPAY timestamps are GMT+7 wall-clock. */
    private static final TimeZone VN_ZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private static final String DATE_PATTERN = "yyyyMMddHHmmss";
    private static final int EXPIRE_MINUTES = 15;

    private VnpayUtil() {
    }

    /* ==================== payment URL ==================== */
    /**
     * Build the signed Sandbox payment URL for one order.
     *
     * @param orderId   online_orders PK — becomes vnp_TxnRef = "PF" + id
     * @param amount    authoritative online_orders.total_amount (VND)
     * @param returnUrl absolute URL VNPAY redirects the browser back to
     * @param ipAddr    customer IP for VNPAY's fraud log
     */
    public static String buildPaymentUrl(long orderId, BigDecimal amount,
            String returnUrl, String ipAddr) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VnpayConfig.VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", VnpayConfig.tmnCode());
        params.put("vnp_Amount", vnpAmount(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef(orderId));
        params.put("vnp_OrderInfo", "Thanh toan don hang PharmaFlow PF" + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ipAddr == null || ipAddr.isEmpty() ? "127.0.0.1" : ipAddr);

        Date now = new Date();
        params.put("vnp_CreateDate", formatVnTime(now));
        params.put("vnp_ExpireDate", formatVnTime(new Date(now.getTime() + EXPIRE_MINUTES * 60_000L)));

        String hashData = buildHashData(params);
        String secureHash = hmacSha512(VnpayConfig.hashSecret(), hashData);
        return VnpayConfig.PAY_URL + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }

    /** Merchant reference: PF + online_order_id — deterministic and unique. */
    public static String txnRef(long orderId) {
        return "PF" + orderId;
    }

    /** Parse "PF<id>" back to the order id; -1 when the shape is wrong. */
    public static long parseTxnRef(String txnRef) {
        if (txnRef == null || !txnRef.startsWith("PF")) {
            return -1;
        }
        try {
            long id = Long.parseLong(txnRef.substring(2));
            if (id > 0) {
                return id;
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** VNPAY amounts are VND x 100 as an integer string — never a float. */
    public static String vnpAmount(BigDecimal amountVnd) {
        return amountVnd.multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    /* ==================== return verification ==================== */
    /**
     * Verify vnp_SecureHash on a VNPAY return request.
     *
     * @param params all request parameters (vnp_* plus anything else — only
     *               vnp_* names are signed, the rest is ignored)
     * @return true only when a vnp_SecureHash exists and matches the recomputed
     *         HMAC-SHA512 of the sorted signed data.
     */
    public static boolean verifyReturnSignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        Map<String, String> signed = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String name = e.getKey();
            if (!name.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(name) || "vnp_SecureHashType".equals(name)) {
                continue;
            }
            signed.put(name, e.getValue());
        }

        String hashData = buildHashData(signed);
        String expected = hmacSha512(VnpayConfig.hashSecret(), hashData);
        return constantTimeEquals(expected, receivedHash);
    }

    /* ==================== internals ==================== */
    /**
     * Sorted "name=encodedValue" pairs joined by '&' — used verbatim both as
     * the query string (secure hash appended after) and as the HMAC input.
     * VNPAY expects every value URL-encoded before hashing.
     */
    private static String buildHashData(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            String value = params.get(name);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(name);
            sb.append('=');
            sb.append(urlEncode(value));
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            // US-ASCII is always present — encode UTF-8 as a safe fallback.
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }

    /** Lower-hex HMAC-SHA512 of data keyed by secret; "" when crypto fails. */
    private static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            // No SecretKeySpec/Mac failure should ever produce a fake hash —
            // an empty string simply fails every comparison.
            LOG.log(Level.SEVERE, "HMAC-SHA512 failed", ex);
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static String formatVnTime(Date d) {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_PATTERN);
        fmt.setTimeZone(VN_ZONE);
        return fmt.format(d);
    }
}
