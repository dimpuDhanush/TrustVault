import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AdminSecurity {
    private static final String PREFIX = "PBKDF2";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(PREFIX + "$");
    }

    public static String hashPassword(String rawPassword) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();

            return PREFIX + "$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        }
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        if (!isHashed(storedPassword)) {
            return rawPassword.equals(storedPassword);
        }

        try {
            String[] parts = storedPassword.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            byte[] computedHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
