package com.zlt.aps.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * BizidUtils 生成类
 */
public class BizIdUtils {


    /**
     * 获取唯一bizId
     * @param clazz 类
     */
    public static <V> String getBizId( Class<V> clazz){
        String[] parts = clazz.getName().split("\\.");
        String lastPart = Arrays.asList(parts).get(parts.length - 1);
        StringBuilder bizIder = new StringBuilder();
        char[] charArray = lastPart.toCharArray();
        for (char c : charArray) {
            if (c >= 'A' && c <= 'Z') {
                bizIder.append(c);
            }
        }
        String pre = bizIder.toString();
        if (pre.length() ==1){
            pre = lastPart;
        }
        //取3位拼接
        String fixedValue = Objects.requireNonNull(generateFixedValue(lastPart)).substring(2,5);
        //返回
        return pre+fixedValue+":"+ UUID.randomUUID().toString() .replace("-", "");
    }

    public static String generateFixedValue(String input) {
        try {
            // 创建 MessageDigest 对象并指定哈希算法
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 计算哈希值
            byte[] hashBytes = digest.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
