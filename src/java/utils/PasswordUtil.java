package utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Băm mật khẩu PBKDF2 (không cần thư viện ngoài).
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        try {
            byte[] salt = new byte[16];
            SecureRandom rnd = new SecureRandom();
            rnd.nextBytes(salt);
            byte[] hash = pbkdf2(plainPassword.toCharArray(), salt);
            return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Không thể băm mật khẩu", e);
        }
    }

    public static boolean verify(String plainPassword, String stored) {
        if (plainPassword == null || stored == null || !stored.contains("$")) {
            return false;
        }
        try {
            int sep = stored.indexOf('$');
            byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
            byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
            byte[] actual = pbkdf2(plainPassword.toCharArray(), salt);
            return slowEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /** Chạy tay: java ... PasswordUtil [mật_khẩu] */
    public static void main(String[] args) {
        String plain = args.length > 0 ? args[0] : "demo123";
        System.out.println(hash(plain));
    }
}
