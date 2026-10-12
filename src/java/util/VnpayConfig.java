package util;

/**
 * VNPAY Sandbox credentials + non-secret constants.
 *
 * Credentials resolve in this order — first non-empty wins:
 *   1. Environment variables  VNPAY_TMN_CODE / VNPAY_HASH_SECRET
 *   2. JVM properties         -Dvnpay.tmnCode / -Dvnpay.hashSecret
 *   3. The SANDBOX_* constants below  (LOCAL SCHOOL-DEMO fallback)
 *
 * WARNING: the baked-in sandbox values below are only for the classroom demo
 * so the project runs with zero env setup — a secret in source code lands in
 * git history forever. Remove them before this is ever used for real payment.
 * The secret is still never logged, never sent to the browser, never in JSP.
 *
 * When everything is missing the feature is "not configured" — checkout then
 * refuses the VNPAY option with a safe message instead of a broken redirect.
 */
public final class VnpayConfig {

    /** Sandbox payment gateway endpoint — not a secret. */
    public static final String PAY_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /** VNPAY API version this integration speaks — not a secret. */
    public static final String VERSION = "2.1.0";

    /* LOCAL DEMO ONLY — sandbox credentials baked in for zero-setup demo.
       Empty string = not baked in (env/JVM prop then decides). */
    private static final String SANDBOX_TMN_CODE = "04BWLH0E";
    private static final String SANDBOX_HASH_SECRET = "KWWBWANTGOCNUDLBEAYYUBCRKMYDSVEG";

    private VnpayConfig() {
    }

    /** Terminal code: env → JVM property → demo fallback; null when all unset. */
    public static String tmnCode() {
        String v = System.getenv("VNPAY_TMN_CODE");
        if (v == null || v.isEmpty()) {
            v = System.getProperty("vnpay.tmnCode");
        }
        if (v == null || v.isEmpty()) {
            v = SANDBOX_TMN_CODE;
        }
        if (v == null || v.isEmpty()) {
            return null;
        }
        return v;
    }

    /** Hash secret: env → JVM property → demo fallback; null when all unset. */
    public static String hashSecret() {
        String v = System.getenv("VNPAY_HASH_SECRET");
        if (v == null || v.isEmpty()) {
            v = System.getProperty("vnpay.hashSecret");
        }
        if (v == null || v.isEmpty()) {
            v = SANDBOX_HASH_SECRET;
        }
        if (v == null || v.isEmpty()) {
            return null;
        }
        return v;
    }

    /** Both credentials present — required before creating a VNPAY order. */
    public static boolean isConfigured() {
        return tmnCode() != null && hashSecret() != null;
    }
}
