package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * OTP helpers: 6-digit numeric codes stored SHA-256-hashed in
 * `verification_tokens` (never plaintext — same rule as passwords).
 */
public final class TokenUtil {

    private static final SecureRandom RNG = new SecureRandom();

    private TokenUtil() {
    }

    /**
     * Random 6-digit code ("000000"–"999999", zero-padded).
     */
    public static String generateCode() {
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    /**
     * SHA-256 hex of the code — what we persist and compare against.
     */
    public static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(code.getBytes(StandardCharsets.UTF_8));

            // Convert each byte to a 2-char hex string ("0a", "ff", ...).
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed on every JVM — this never happens.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
