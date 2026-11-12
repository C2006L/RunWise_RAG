package com.runwise.common;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 独立随机盐 + PBKDF2；不存储明文密码。 */
public final class PasswordHasher {
    private static final int ITERATIONS = 210000;
    private PasswordHasher() { }
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            if (iterations < 100000 || iterations > 1000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
                    derive(password, Base64.getDecoder().decode(parts[1]), iterations));
        } catch (RuntimeException e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException("密码编码失败", e); }
        finally { spec.clearPassword(); }
    }
}
