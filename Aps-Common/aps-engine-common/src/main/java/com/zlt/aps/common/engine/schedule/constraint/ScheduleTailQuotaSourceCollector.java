package com.zlt.aps.common.engine.schedule.constraint;

import com.zlt.aps.common.engine.schedule.engine.ScheduleTaskDraftModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 当日成型总余量额度来源采集器。
 *
 * <p>按「工厂|排程日期|产品|单位长度」分桶，同一胎胚只计一次，并把每个桶的总余量折算为米。
 * 本类无数据库、Spring 依赖，只做纯计算。</p>
 *
 * <p>采集规则：</p>
 * <ul>
 *     <li>余量为空表示未配置，该产品当日整批退回原口径（不建立不完整额度）；</li>
 *     <li>余量为负表示数据异常，该产品当日不启用额度，保持既有行为；</li>
 *     <li>成型连续停产收尾来源不参与额度，避免把 0 余量当成额度耗尽；</li>
 *     <li>单位长度无效的来源不参与额度；</li>
 *     <li>同一胎胚按去除首尾空格后的编码去重。</li>
 * </ul>
 */
public class ScheduleTailQuotaSourceCollector {

    /**
     * 同一胎胚对应多个产品编码的告警编码
     */
    public static final String EMBRYO_MULTI_PRODUCT = "EMBRYO_MULTI_PRODUCT";

    /**
     * 同产品存在未配置余量来源、当日整批退回原口径的告警编码
     */
    public static final String SOURCE_INCOMPLETE = "TAIL_QUOTA_SOURCE_INCOMPLETE";

    private static final String BUCKET_SEPARATOR = "|";

    /**
     * 构造额度桶键。
     *
     * @param factoryCode   工厂编码
     * @param datePart      排程日期文本
     * @param productCode   产品编码
     * @param unitLengthKey 单位长度文本
     * @return 额度桶键
     */
    public static String buildBucketKey(String factoryCode, String datePart, String productCode,
                                        String unitLengthKey) {
        return safeText(factoryCode) + BUCKET_SEPARATOR + safeText(datePart) + BUCKET_SEPARATOR
                + safeText(productCode) + BUCKET_SEPARATOR + safeText(unitLengthKey);
    }

    /**
     * 构造额度桶键，排程日期与单位长度由本方法归一化。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param productCode  产品编码
     * @param unitLength   单位长度，单位米
     * @return 额度桶键
     */
    public static String resolveBucketKey(String factoryCode, LocalDate scheduleDate, String productCode,
                                          BigDecimal unitLength) {
        return buildBucketKey(factoryCode, scheduleDate == null ? "" : scheduleDate.toString(), productCode,
                unitLength == null ? "" : unitLength.stripTrailingZeros().toPlainString());
    }

    /**
     * 解析任务的产品编码，优先工序编码，回退物料编码。
     *
     * @param sourceTask 来源任务
     * @return 产品编码
     */
    public static String resolveProductCode(ScheduleTaskDraftModel sourceTask) {
        if (sourceTask == null) {
            return "";
        }
        String processCode = sourceTask.getProcessCode();
        if (processCode != null && !processCode.trim().isEmpty()) {
            return processCode;
        }
        return safeText(sourceTask.getMaterialCode());
    }

    /**
     * 将空值转为空串。
     *
     * @param value 原始文本
     * @return 非空文本
     */
    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * 采集额度台账。
     *
     * <p>取值器按任务子类型泛型化：引擎侧传入的是 {@code PlanCalculationPolicy<T>} 的方法引用，
     * 其形参是领域任务子类型，若这里声明成 {@code Function<ScheduleTaskDraftModel, ...>}
     * 会因泛型不可逆变而在调用点编译失败。</p>
     *
     * @param factoryCode        工厂编码
     * @param scheduleDate       排程日期
     * @param sourceTaskList     来源聚合任务列表
     * @param remainQtyResolver  余量条数取值器，未配置时返回空
     * @param unitLengthResolver 单位长度取值器，单位米
     * @param <K>                领域任务类型
     * @return 台账与告警
     */
    public <K extends ScheduleTaskDraftModel> ScheduleTailQuotaCollectResult collect(
            String factoryCode, LocalDate scheduleDate, List<K> sourceTaskList,
            Function<? super K, BigDecimal> remainQtyResolver,
            Function<? super K, BigDecimal> unitLengthResolver) {
        ScheduleTailQuotaCollectResult result = new ScheduleTailQuotaCollectResult();
        if (sourceTaskList == null || sourceTaskList.isEmpty()
                || remainQtyResolver == null || unitLengthResolver == null) {
            return result;
        }
        String datePart = scheduleDate == null ? "" : scheduleDate.toString();
        // 桶键 -> 累计条数
        Map<String, BigDecimal> bucketRemainMap = new LinkedHashMap<>();
        // 桶键 -> 单位长度
        Map<String, BigDecimal> bucketUnitLengthMap = new LinkedHashMap<>();
        // 胎胚 -> 已归属的产品编码，用于多产品告警与去重
        Map<String, String> embryoProductMap = new LinkedHashMap<>();
        // 出现负余量的产品，当日整体不启用额度
        java.util.Set<String> disabledProductSet = new java.util.LinkedHashSet<>();
        // 存在未配置余量来源的产品，当日整批退回原口径，不能用部分来源建立不完整额度
        java.util.Set<String> incompleteProductSet = new java.util.LinkedHashSet<>();

        for (K sourceTask : sourceTaskList) {
            if (sourceTask == null) {
                continue;
            }
            String productCode = resolveProductCode(sourceTask);
            if (Boolean.TRUE.equals(sourceTask.getFormingShutdownCloseOutFlag())) {
                continue;
            }
            BigDecimal remainQty = remainQtyResolver.apply(sourceTask);
            if (remainQty == null) {
                // 未知余量不能被当作「不存在」：同产品只要有一行未配置，整批退回原口径
                incompleteProductSet.add(productCode);
                continue;
            }
            BigDecimal unitLength = unitLengthResolver.apply(sourceTask);
            if (unitLength == null || unitLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (remainQty.compareTo(BigDecimal.ZERO) < 0) {
                disabledProductSet.add(productCode);
                continue;
            }
            String unitLengthKey = this.normalizeScale(unitLength);
            // 胎胚编码必须先去除首尾空格，否则 "EMB-A" 与 "EMB-A " 会被当成两个胎胚重复计入
            String embryoCode = sourceTask.getEmbryoCode() == null
                    ? null : sourceTask.getEmbryoCode().trim();
            if (embryoCode != null && !embryoCode.isEmpty()) {
                String existingProduct = embryoProductMap.get(embryoCode);
                if (existingProduct == null) {
                    embryoProductMap.put(embryoCode, productCode);
                } else if (!existingProduct.equals(productCode)) {
                    // 同一胎胚出现多个产品编码：业务前提被破坏，只告警不阻断
                    if (!result.getWarningList().contains(EMBRYO_MULTI_PRODUCT)) {
                        result.getWarningList().add(EMBRYO_MULTI_PRODUCT);
                    }
                    // 已计入其他产品，本来源不再重复计入
                    continue;
                } else {
                    // 同一胎胚同一产品重复出现：只计一次
                    continue;
                }
            }
            String bucketKey = buildBucketKey(factoryCode, datePart, productCode, unitLengthKey);
            bucketRemainMap.merge(bucketKey, remainQty, BigDecimal::add);
            bucketUnitLengthMap.put(bucketKey, unitLength);
        }

        if (!incompleteProductSet.isEmpty() && !result.getWarningList().contains(SOURCE_INCOMPLETE)) {
            result.getWarningList().add(SOURCE_INCOMPLETE);
        }
        for (Map.Entry<String, BigDecimal> entry : bucketRemainMap.entrySet()) {
            String bucketKey = entry.getKey();
            String productCode = this.resolveProductCodeFromBucketKey(bucketKey);
            if (disabledProductSet.contains(productCode) || incompleteProductSet.contains(productCode)) {
                // 同产品存在负余量或未配置余量来源：当日整体不启用额度
                continue;
            }
            BigDecimal totalQuota = entry.getValue()
                    .multiply(bucketUnitLengthMap.get(bucketKey));
            result.getLedgerMap().put(bucketKey,
                    new ScheduleTailQuotaLedger(bucketKey, totalQuota));
        }
        return result;
    }

    /**
     * 从额度桶键中解析产品编码。
     *
     * @param bucketKey 额度桶键
     * @return 产品编码；解析失败返回空串
     */
    private String resolveProductCodeFromBucketKey(String bucketKey) {
        if (bucketKey == null) {
            return "";
        }
        String[] parts = bucketKey.split("\\" + BUCKET_SEPARATOR, -1);
        return parts.length >= 3 ? parts[2] : "";
    }

    /**
     * 归一化数值文本，避免 3.0 与 3.00 被当成两个桶。
     *
     * @param value 原始数值
     * @return 归一化文本
     */
    private String normalizeScale(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
