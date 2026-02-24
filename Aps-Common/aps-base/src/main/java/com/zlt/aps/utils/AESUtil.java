package com.zlt.aps.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES 工具类
 */
public class AESUtil {

    private static final Logger logger = LoggerFactory.getLogger(AESUtil.class);

    private static final String KEY = "MIICdQIBADANBgkqhkiG9";

    public static final String AES = "AES";
    public static final String INSTANCE = "AES/ECB/PKCS5Padding"; //"算法/模式/补码方式"


    /**
     * 生成密钥
     *
     * @param secretKey 原始密钥
     * @return
     * @throws Exception
     */
    public static SecretKeySpec generateKey(String secretKey) throws Exception {
        // 1.构造密钥生成器，指定为AES算法,不区分大小写
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        // 2. 因为AES要求密钥的长度为128，我们需要固定的密码，因此随机源的种子需要设置为我们的密码数组
        // 生成一个128位的随机源, 根据传入的字节数组
        /**
         * 这种方式 windows 下正常, Linux 环境下会解密失败
         * keyGenerator.init(128, new SecureRandom(password.getBytes()));
         */
        // 兼容 Linux
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(secretKey.getBytes(StandardCharsets.UTF_8));
        keyGenerator.init(128, random);
        // 3.产生原始对称密钥
        SecretKey original_key = keyGenerator.generateKey();
        // 4. 根据字节数组生成AES密钥
        SecretKeySpec secretKeySpec = new SecretKeySpec(original_key.getEncoded(), AES);
        return secretKeySpec;
    }

    /**
     * AES加密
     *
     * @param content   加密内容
     * @param secretKey 密钥
     * @return BASE64转码后的文本
     * @throws Exception
     */
    public static String aesEncrypt(String content, String secretKey) {
        if (secretKey == null) {
            //System.out.print("Key为空null");
            return null;
        }
        try {
            SecretKeySpec keySpec = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(INSTANCE);//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return new BASE64Encoder().encode(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * AES解密
     *
     * @param content   解密内容
     * @param secretKey 密钥
     * @return
     * @throws Exception
     */
    public static String aesDecrypt(String content, String secretKey) {
        // 判断Key是否正确
        if (secretKey == null) {
            //System.out.print("Key为空null");
            return null;
        }
        try {
            SecretKeySpec keySpec = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(INSTANCE);//"算法/模式/补码方式"
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(content);//先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, StandardCharsets.UTF_8);
            return originalString;
        } catch (Exception e) {
            //System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * 加密
     *
     * @param content 加密内容
     * @return
     */
    public static String encrypt(String content) {
        return aesEncrypt(content, KEY);
    }

    /**
     * 解密
     *
     * @param content 解密内容
     * @return
     */
    public static String decrypt(String content) {
        return aesDecrypt(content, KEY);
    }


    public static void main(String[] args) throws Exception {
        String key = "oauthAutoLogiy#0929";
        String content = "oauthAa@123456";
        logger.info("加密前：" + content);

        String encrypt = aesEncrypt(content, key);
        logger.info("加密后：" + encrypt);

        String decrypt = aesDecrypt(encrypt, key);
        logger.info("解密后：" + decrypt);
    }


}

