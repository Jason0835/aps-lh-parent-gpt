package com.zlt.aps.tc.engine.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 胎侧胶料编码与相似度纯计算工具。
 *
 * <p>统一主胶编码相同判断、基部胶编码集合解析、集合交集数量和相似度分值计算。
 * 本工具不读取排程上下文、参数、数据库或 Spring Bean，也不决定任务排序和机台评分策略。</p>
 */
public final class TcGlueSimilarityUtils {

    /**
     * 工具类不允许实例化。
     */
    private TcGlueSimilarityUtils() {
    }

    /**
     * 判断两个编码是否非空且完全相同。
     *
     * @param left 左侧编码
     * @param right 右侧编码
     * @return 双方编码非空且完全相同时返回 true，否则返回 false
     */
    public static boolean isSameNonBlank(String left, String right) {
        return left != null && !left.trim().isEmpty() && left.equals(right);
    }

    /**
     * 将逗号分隔的编码拆分为去重集合，忽略空值、空白和空元素。
     *
     * @param codeText 逗号分隔的编码文本
     * @return 去除首尾空白和重复元素后的编码集合；原文本为空时返回空集合
     */
    public static Set<String> parseCodeSet(String codeText) {
        if (codeText == null || codeText.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> codeSet = new HashSet<>();
        for (String item : codeText.split(",")) {
            String value = item == null ? "" : item.trim();
            if (!value.isEmpty()) {
                codeSet.add(value);
            }
        }
        return codeSet;
    }

    /**
     * 计算两个去重编码集合的交集元素数量。
     *
     * @param leftSet 左侧编码集合
     * @param rightSet 右侧编码集合
     * @return 交集元素数量；任一集合为空时返回 0
     */
    public static int calculateIntersectionCount(Set<String> leftSet, Set<String> rightSet) {
        if (leftSet == null || leftSet.isEmpty() || rightSet == null || rightSet.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String item : leftSet) {
            if (rightSet.contains(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 按左侧去重编码集合大小计算相似度分值，并保持两位小数。
     *
     * <p>计算公式为：最大分值 × 交集元素数量 ÷ 左侧元素数量。该口径与当前胎侧
     * 链式任务选择和默认机台评分保持一致，不承担最终策略分的组合与比较。</p>
     *
     * @param left 左侧逗号分隔编码
     * @param right 右侧逗号分隔编码
     * @param maxScore 相似度最大分值
     * @return 相似度分值；左侧为空、无交集或最大分值无效时返回 0
     */
    public static BigDecimal calculateSimilarityScore(String left, String right, BigDecimal maxScore) {
        Set<String> leftSet = parseCodeSet(left);
        int intersectionCount = calculateIntersectionCount(leftSet, parseCodeSet(right));
        if (leftSet.isEmpty() || intersectionCount <= 0 || maxScore == null
                || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return maxScore.multiply(BigDecimal.valueOf(intersectionCount))
                .divide(BigDecimal.valueOf(leftSet.size()), 2, RoundingMode.HALF_UP);
    }
}
