package com.zlt.aps.common.core.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * TM/TC定时自动滚动动作标识和物理窗口键工具。
 */
public final class AutoRollingActionUtils {

    /**
     * 动作ID最大长度。
     */
    public static final int CLIENT_REQUEST_ID_MAX_LENGTH = 64;

    private AutoRollingActionUtils() {
    }

    /**
     * 生成无分隔符的默认动作标识。
     *
     * @return 32位小写十六进制UUID
     */
    public static String newClientRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 为自动排程前置滚动生成同任务可重放的稳定动作标识。
     *
     * @param module 模块编码 TM 或 TC
     * @param taskId 自动排程任务编号
     * @return 32位小写十六进制动作标识
     */
    public static String preAutoScheduleRequestId(String module, String taskId) {
        return toHex(digest(module + "|AUTO_PLAN_PRE_ROLLING|" + taskId));
    }

    /**
     * 校验并规范化调用方动作标识。
     *
     * @param clientRequestId 调用方动作标识
     * @return 去除首尾空格后的动作标识
     * @throws IllegalArgumentException 动作标识为空或超过长度上限时抛出
     */
    public static String normalizeClientRequestId(String clientRequestId) {
        if (clientRequestId == null) {
            return newClientRequestId();
        }
        String normalized = clientRequestId.trim();
        if (normalized.isEmpty() || normalized.length() > CLIENT_REQUEST_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("clientRequestId length must be 1-64");
        }
        return normalized;
    }

    /**
     * 生成兼容原revision=0格式的基础物理窗口键。
     *
     * @param module        模块编码TM或TC
     * @param factory       工厂编码
     * @param date          主侧排程日
     * @param shift         主侧班序
     * @param physicalStart 固定物理班开始时间毫秒值
     * @return 16字节MD5键
     */
    public static byte[] baseRequestKey(String module, String factory, LocalDate date, int shift, long physicalStart) {
        String normalizedFactory = factory.trim();
        return digest(module + "|" + normalizedFactory.length() + ":" + normalizedFactory + "|" + date
                + "|" + shift + "|" + physicalStart + "|0");
    }

    /**
     * 生成指定修订的请求键，revision=0保持历史键值不变。
     *
     * @param module        模块编码TM或TC
     * @param factory       工厂编码
     * @param date          主侧排程日
     * @param shift         主侧班序
     * @param physicalStart 固定物理班开始时间毫秒值
     * @param revision      修订号
     * @return 16字节MD5键
     */
    public static byte[] requestKey(String module, String factory, LocalDate date, int shift,
                                    long physicalStart, int revision) {
        String normalizedFactory = factory.trim();
        return digest(module + "|" + normalizedFactory.length() + ":" + normalizedFactory + "|" + date
                + "|" + shift + "|" + physicalStart + "|" + revision);
    }

    /**
     * 生成带基础窗口范围的稳定动作唯一键。
     *
     * @param module          模块编码TM或TC
     * @param baseRequestKey  基础物理窗口键
     * @param clientRequestId 调用方动作标识
     * @return 16字节MD5键
     */
    public static byte[] actionKey(String module, byte[] baseRequestKey, String clientRequestId) {
        return digest(module + "|" + toHex(baseRequestKey) + "|" + clientRequestId);
    }

    /**
     * 将二进制键转换为小写十六进制，供任务幂等字段保存。
     *
     * @param value 二进制键
     * @return 十六进制文本
     */
    public static String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(Character.forDigit((item >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(item & 0x0f, 16));
        }
        return result.toString();
    }

    /**
     * 计算UTF-8文本的MD5摘要。
     *
     * @param value 待摘要文本
     * @return 16字节摘要
     */
    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is unavailable", exception);
        }
    }
}
