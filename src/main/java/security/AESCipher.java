package security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;


/**
 * Provides an implementation of the AES Cipher
 * for encrypting and decrypting strings.
 *
 * <p>
 * Using an <code>AESCipher</code> is simple.
 * </p>
 *
 * <p>
 * To start, one creates an instance of the cipher:
 * <code>AESCipher aes = new AESCipher(key)</code>, where
 * a <code>String</code> is provided as an argument to
 * produce the symmetric key used for encryption and
 * decryption.
 * </p>
 *
 * <p>
 * One can then encrypt a <code>String</code>, say, <code>text</code>,
 * using:
 * <code>aes.encrypt(text)</code>.
 * Similarly, one can decrypt a <code>String</code>, say, <code>cipherText</code>,
 * using:
 * <code>aes.decrypt(cipherText)</code>.
 * </p>
 *
 * @author Sathish Gopalakrishnan
 * @version 1.1
 */
public class AESCipher {

    private static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String CHARSET_NAME = "UTF-8";

    private SecretKeySpec secretKey;
    private byte[] key;

    /**
     * Create an instance of AESCipher
     *
     * @param key seed to generate a symmetric key, is not null
     */
    public AESCipher(final String key) {
        setKey(key);
    }

    // create the symmetric key
    private void setKey(final String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(CHARSET_NAME);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encrypt a string using this AESCipher instance
     *
     * @param strToEncrypt
     * @return the encrypted version of <code>strToEncrypt</code>
     */
    public String encrypt(final String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(strToEncrypt.getBytes(CHARSET_NAME)));
        }
        catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    /**
     * Decrypt a string using this AESCipher instance
     *
     * @param strToDecrypt
     * @return the decrypted version of <code>strToDecrypt</code>
     */
    public String decrypt(final String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder()
                .decode(strToDecrypt)));
        }
        catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

}