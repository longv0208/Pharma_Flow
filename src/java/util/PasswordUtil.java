package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Minimal salted-SHA-256 password hasher.
 *
 * Stored format: "<base64-salt>:<base64-digest>" — both halves needed to
 * verify. Not as strong as BCrypt but dependency-free (no extra jar needed).
 * When the team later adopts BCrypt/Argon2, replace this file only.
 */
public final class PasswordUtil {

    private static final int SALT_BYTES = 16;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Hash a plaintext password. Returns "salt:hash" (both base64).
     */
    public static String hash(String plain) {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] digest = digest(salt, plain);
        return Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(digest);
    }

    /**
     * Verify a plaintext password against a stored "salt:hash".
     */
    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null) {
            return false;
        }
        int sep = stored.indexOf(':');
        if (sep <= 0) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
            byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
            byte[] actual = digest(salt, plain);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] digest(byte[] salt, String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed on every JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
