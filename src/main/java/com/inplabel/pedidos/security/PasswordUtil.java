package com.inplabel.pedidos.security;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {
    private static final int ITERATIONS = 600_000;
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    public static String hashPassword(String password, String salt) {
        if (password == null || password.length() > 1024) throw new IllegalArgumentException("Contraseña inválida");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, 256);
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(result);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña", e);
        } finally { spec.clearPassword(); }
    }
    public static boolean needsUpgrade(String hash) { return hash != null && !hash.startsWith("pbkdf2$"); }
    public static boolean verifyPassword(String raw, String salt, String hash) {
        if (raw == null || salt == null || hash == null || raw.length() > 1024) return false;
        try {
            String calculated;
            if (needsUpgrade(hash)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(Base64.getDecoder().decode(salt));
                calculated = Base64.getEncoder().encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
            } else { calculated = hashPassword(raw, salt); }
            return MessageDigest.isEqual(calculated.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return false; }
    }
}
