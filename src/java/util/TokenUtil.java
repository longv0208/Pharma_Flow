package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * OTP helpers: 6-digit numeric codes stored SHA-256-hashed in
 * `verification_tokens` (never plaintext — same rule as passwords).
 */
public final class TokenUtil {

    private static final SecureRandom RNG = new SecureRandom();

    private TokenUtil() {}

    /** Random 6-digit code ("000000"–"999999", zero-padded). */
    public static String generateCode() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    /** SHA-256 hex of the code — what we persist and compare against. */
    public static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
