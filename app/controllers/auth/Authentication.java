package controllers.auth;

import models.AlarmAttendant;
import play.data.Form;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

/**
 * Created by Aleksander Skraastad (myth) on 6/8/15.
 */
public class Authentication {

    // Used to generate iteration counts for PBKDF2
    public static Random nonsecureRandom = new Random();

    public static Form<Login> loginForm = Form.form(Login.class);

    // Credentials class used in the login form
    public static class Login {
        public String username, password;
        public String validate() {
            if (Authentication.validate(this)) return null;
            return "Invalid username or password";
        }
    }

    /**
     * Performs validation of a user login based on the provided login form
     * @param credentials A Login object containing the specified user data
     * @return An error message if
     */
    public static boolean validate(Login credentials) {

        // Attempt to fetch the user from database
        AlarmAttendant user = AlarmAttendant.getAttendantFromUsername(credentials.username);

        // If user does not exist
        if (user == null) return false;

        try {
            return validatePassword(credentials.password, user.password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    /* PBKDF2 Hash Support */

    /**
     * Generate a strong password hash using the PBKDF2 Algorithm.
     *
     * @param password The raw password string as supplied by the user
     * @return A string representing the workload, salt and final hash
     * @throws NoSuchAlgorithmException If algorithm is not provided in the stdlib
     * @throws InvalidKeySpecException If provided key specification is not provided in the stdlib
     */
    public static String generatePasswordHash(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        // Give it a nice juicy workload
        int iterations = nonsecureRandom.nextInt(500) + 1000;
        char[] chars = password.toCharArray();
        byte[] salt = generateSalt().getBytes();

        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }

    /**
     * Generate a salt using a cryptographically secure pseudo-random number generator
     * @return A string containing the generated password salt
     * @throws NoSuchAlgorithmException If specified algorithm is not provided in the stdlib
     */
    private static String generateSalt() throws NoSuchAlgorithmException {
        // Generate some randomness
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }

    /**
     * Helper method that converts a byte array to a hexadecimal string
     * @param array The array of bytes that is to be converted to a hex-string
     * @return A hexadecimal representation of the contents of the provided byte array
     */
    private static String toHex(byte[] array) {

        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();

        if (paddingLength > 0) return String.format("%0"  +paddingLength + "d", 0) + hex;
        else return hex;
    }

    /**
     * Helper method that performs a time-constant comparison of two PBDKF2 password hashes
     * @param originalPassword The password hash as it is provided by the user
     * @param storedPassword The password hash as it is stored in the database
     * @return true if the hashes match, false otherwise
     * @throws NoSuchAlgorithmException If provided algorithm is not present in stdlib
     * @throws InvalidKeySpecException If provided keyspec is invalid or not provided in stdlib
     */
    private static boolean validatePassword(String originalPassword, String storedPassword)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        String[] parts = storedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        // Initialize the spec with the stored data
        PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        // Perform time-constant hash comparison
        int diff = hash.length ^ testHash.length;
        for(int i = 0; i < hash.length && i < testHash.length; i++) {
            diff |= hash[i] ^ testHash[i];
        }
        return diff == 0;
    }

    /**
     * Helper method that converts a hexadecimal string to a byte array
     * @param hex The hex-string to be converted to a byte array
     * @return A byte array representation of the provided hexadecimal string
     */
    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i<bytes.length ;i++) {
            bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}