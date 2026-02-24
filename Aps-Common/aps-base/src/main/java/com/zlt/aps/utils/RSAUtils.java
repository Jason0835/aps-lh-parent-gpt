package com.zlt.aps.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtils {
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 获取密钥对
     *
     * @return 密钥对
     */
    public static KeyPair getKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    /**
     * 获取私钥
     *
     * @param privateKey 私钥字符串
     * @return
     */
    public static PrivateKey getPrivateKey(String privateKey) {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = Base64.decodeBase64(privateKey.getBytes());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取公钥
     *
     * @param publicKey 公钥字符串
     * @return
     */
    public static PublicKey getPublicKey(String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = Base64.decodeBase64(publicKey.getBytes());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA加密
     *
     * @param data      待加密数据
     * @param publicKey 公钥
     * @return
     */
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        int inputLen = data.getBytes().length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data.getBytes(), offset, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data.getBytes(), offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        // 获取加密内容使用base64进行编码,并以UTF-8为标准转化成字符串
        // 加密后的字符串
        return new String(Base64.encodeBase64String(encryptedData));
    }

    /**
     * RSA解密
     *
     * @param data       待解密数据
     * @param privateKey 私钥
     * @return
     */
    public static String decrypt(String data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] dataBytes = Base64.decodeBase64(data);
        int inputLen = dataBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(dataBytes, offset, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(dataBytes, offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        // 解密后的内容
        return new String(decryptedData, "UTF-8");
    }

    /**
     * 签名
     *
     * @param data       待签名数据
     * @param privateKey 私钥
     * @return 签名
     */
    public static String sign(String data, PrivateKey privateKey){

        try {
            byte[] keyBytes = privateKey.getEncoded();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey key = keyFactory.generatePrivate(keySpec);
            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initSign(key);
            signature.update(data.getBytes());
            return new String(byte2Hex(signature.sign()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param bytes，输入byte[]数组
     * @return 16进制字符
     * @Description: 将byte[]数组转换成16进制字符。一个byte生成两个字符，长度对应1:2
     * @Author: mastermind
     * @Date: 2020-04-06 12:16
     */
    public static String byte2Hex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        // 遍历byte[]数组，将每个byte数字转换成16进制字符，再拼接起来成字符串
        for (int i = 0; i < bytes.length; i++) {
            // 每个byte转换成16进制字符时，bytes[i] & 0xff如果高位是0，输出将会去掉，所以+0x100(在更高位加1)，再截取后两位字符
            builder.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }

    /**
     * 将16进制字符转换成byte[]数组。与byte2Hex功能相反。
     *
     * @param string 16进制字符串
     * @return byte[]数组
     */
    public static byte[] hex2Byte(String string) {
        if (string == null || string.length() < 1) {
            return null;
        }
        // 因为一个byte生成两个字符，长度对应1:2，所以byte[]数组长度是字符串长度一半
        byte[] bytes = new byte[string.length() / 2];
        // 遍历byte[]数组，遍历次数是字符串长度一半
        for (int i = 0; i < string.length() / 2; i++) {
            // 截取没两个字符的前一个，将其转为int数值
            int high = Integer.parseInt(string.substring(i * 2, i * 2 + 1), 16);
            // 截取没两个字符的后一个，将其转为int数值
            int low = Integer.parseInt(string.substring(i * 2 + 1, i * 2 + 2), 16);
            // 高位字符对应的int值*16+低位的int值，强转成byte数值即可
            // 如dd，高位13*16+低位13=221(强转成byte二进制11011101，对应十进制-35)
            bytes[i] = (byte) (high * 16 + low);
        }
        return bytes;
    }

    /**
     * 验签
     *
     * @param srcData   原始字符串
     * @param publicKey 公钥
     * @param sign      签名
     * @return 是否验签通过
     */
    public static boolean verify(String srcData, PublicKey publicKey, String sign){

        try {
            byte[] keyBytes = publicKey.getEncoded();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initVerify(key);
            signature.update(srcData.getBytes());
            return signature.verify(hex2Byte(sign));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            // 生成密钥对
            KeyPair keyPair = getKeyPair();
//            String privateKey = new String(Base64.encodeBase64(keyPair.getPrivate().getEncoded()));
            String privateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIb3B054H+Yw87yezuwaV0M+TXthoBmWtKHKer5M9u1/t/0/5ZwBDEaWnwowPyhkHWsZk7EPaL8Lgsl0h4/DUgJrH50y6ZvYmVgulxD3EDe46hywzBO62hT2wxEbARmN+8j//1xkTYXVxu7sbNV5d+4SxASe29OBqrukYys+NKuJAgMBAAECgYBh7vCDVRE4lH7YeJgHpNl7NsM8a0ukJcIuwGEuo2RuU8XrYyk2eWAx/GutFfNOWM8r/uQ3j8nfDvg5PHB9tipT9xxCnFgwOeMB2+/ofOXeicKI7UFyW1LWbEYOKe3QQmzWa3vXCn2LsVnBAHcbdHlz134wKr7VJc+t1QeU03EKgQJBAO0zmzFyDIk2iBJOxR5bRWMBmK6I7S9E364X3lEwOkK/WtTF+9H0E6tojxoGAnuLDI+K+Zf0xu5d5YARlWmlYXkCQQCRqTmQGjo9BGGpr2u0oF04yNek3G9e9ysXwQktDPmgnn4GsSrrp3mcb+Z1/2xfmW2fWwCcWiCSFwAo3YSb9KaRAkAB7dqEQ24wq33d0EAwKAPfc0LfoIN1T/UVwGHxfRfsNQwzEM0kfvyt9zK6vnPEt3PJsxKmlroLdD4KlZoGeu7ZAkAmD8cn3YKcURnIAjutrj3NycV3odZERWfwRBPGvt4311JtIzxo6ZFAjIj3CnBiJrBbdKcbM/3QzsvO4dt1+R7RAkEAgMDvRc3Um1mQqt3xoxT3VYUEh8LQxF72x+dsXrPzoggFLvCXSlA7WQJpyDqHK8x2HSXvazB82Ei1uro58sd0Hw==";
//            String publicKey = new String(Base64.encodeBase64(keyPair.getPublic().getEncoded()));
            String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCG9wdOeB/mMPO8ns7sGldDPk17YaAZlrShynq+TPbtf7f9P+WcAQxGlp8KMD8oZB1rGZOxD2i/C4LJdIePw1ICax+dMumb2JlYLpcQ9xA3uOocsMwTutoU9sMRGwEZjfvI//9cZE2F1cbu7GzVeXfuEsQEntvTgaq7pGMrPjSriQIDAQAB";
            System.out.println("私钥:" + privateKey);
            System.out.println("公钥:" + publicKey);
            // RSA加密
            String data = "test122334";
            String encryptData = encrypt(data, getPublicKey(publicKey));
            System.out.println("加密后内容:" + encryptData);
            // RSA解密
            String decryptData = decrypt(encryptData, getPrivateKey(privateKey));
            System.out.println("解密后内容:" + decryptData);
            // RSA签名
            String sign = sign(data, getPrivateKey(privateKey));
            // RSA验签
            boolean result = verify(data, getPublicKey(publicKey), sign);
            System.out.print("验签结果:" + result);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("加解密异常");
        }
    }
}
