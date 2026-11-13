package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 检查项8：成型未排产检查（提示级）。
 *
 * <p>校验口径：</p>
 * <ul>
 *   <li><b>硫化侧</b>：排程结果中 class1~class8 计划量合计 &gt; 0 的胎胚，按「胎胚描述 + 产品状态」聚合；</li>
 *   <li><b>成型侧</b>：同工厂、同排程日的成型排程结果（{@code T_CX_SCHEDULE_RESULT}）中
 *       class1~class8 计划量合计 &gt; 0 的行，同样按「胎胚描述 + 产品状态」建集合；</li>
 *   <li><b>判定</b>：硫化有排产、成型侧查不到对应排产 → 记一条异常；同一「描述 + 状态」多机台只报一条；</li>
 *   <li><b>收尾说明</b>：硫化结果行 {@code IS_EMBRYO_ENDING=1}（成型本批不再安排、胎胚已收尾）时，
 *       该异常仍保留展示，但明细标记 {@code embryoEnding=true}，并由系统直接置为已了解（可解释异常，不阻塞操作）；</li>
 *   <li><b>状态兜底</b>：硫化行产品状态为空时只按胎胚描述匹配，避免状态缺失造成误报；</li>
 *   <li><b>跳过</b>：成型排程结果整体为空时跳过，避免全量误报；胎胚描述为空的行无法判定，跳过并计数。</li>
 * </ul>
 *
 * <p>本项只判定「成型是否存在排产」，不校验成型排产时间与硫化消耗的先后顺序，也不校验成型数量是否够用。</p>
 *
 * @author APS
 * @date 2026/11/05
 */
@Slf4j
@Component
@Order(8)
public class CxNoScheduleChecker implements LhScheduleChecker {

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.cxNoSchedule";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.cxNoSchedule";

    /** 描述与产品状态的组合键分隔符 */
    private static final String KEY_SEPARATOR = "|";

    /** 产品状态为空时的展示占位 */
    private static final String BLANK_STATUS_DISPLAY = "-";

    /** 胎胚收尾标识：是（IS_EMBRYO_ENDING=1，表示成型本批不再安排该胎胚、胎胚收尾） */
    private static final String EMBRYO_ENDING_YES = "1";

    /** 产品状态字典类型（与硫化排程结果列 PRODUCT_STATUS 的 @Excel dictType 一致） */
    private static final String PRODUCT_STATUS_DICT_TYPE = "lh_trial_status";

    /** 成型导入批次的数据来源标识：导入行不是算法排产，不能作为“成型有排产”的依据 */
    private static final String CX_DATA_SOURCE_IMPORT = "2";

    /** 全角空格 */
    private static final String FULL_WIDTH_SPACE = "\u3000";

    @Override
    public String getCode() {
        return CODE_CX_NO_SCHEDULE;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    @Override
    public boolean isHardControl() {
        return false;
    }

    @Override
    public List<LhScheduleCheckResult> check(LhScheduleCheckContext context) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        if (!context.hasResults()) {
            return abnormalList;
        }
        List<CxScheduleResult> cxScheduleResultList = context.getCxScheduleResultList();
        if (cxScheduleResultList == null || cxScheduleResultList.isEmpty()) {
            log.warn("成型未排产检查：未加载到成型排程结果，跳过校验, factoryCode={}", context.getFactoryCode());
            return abnormalList;
        }
        // 1. 成型侧建索引：有排产的「描述+状态」与「描述」两个集合
        CxScheduleIndex cxIndex = this.buildCxScheduleIndex(cxScheduleResultList);
        // 2. 硫化侧聚合：按「描述+状态」汇总计划量
        Map<String, LhEmbryoAggregate> aggregateMap = this.buildLhAggregateMap(context);
        if (aggregateMap.isEmpty()) {
            return abnormalList;
        }
        // 3. 逐组比对，成型侧查不到排产的即记异常
        for (Map.Entry<String, LhEmbryoAggregate> entry : aggregateMap.entrySet()) {
            LhEmbryoAggregate aggregate = entry.getValue();
            if (this.hasCxSchedule(cxIndex, aggregate)) {
                continue;
            }
            abnormalList.add(this.buildAbnormal(aggregate));
        }
        return abnormalList;
    }

    /**
     * 构建成型侧排产索引（仅统计有计划量的行）。
     *
     * @param cxScheduleResultList 成型排程结果
     * @return 成型排产索引
     */
    private CxScheduleIndex buildCxScheduleIndex(List<CxScheduleResult> cxScheduleResultList) {
        CxScheduleIndex cxIndex = new CxScheduleIndex();
        int importRowCount = 0;
        for (CxScheduleResult cxResult : cxScheduleResultList) {
            if (cxResult == null || this.sumPlanQty(cxResult) <= 0) {
                continue;
            }
            // 导入行（如 CXDR 批次）虽然有描述、状态和计划量，但不是算法排产，
            // 不能把“成型未排产”误判成“成型已有计划”。
            if (CX_DATA_SOURCE_IMPORT.equals(StringUtils.trimToEmpty(cxResult.getDataSource()))) {
                importRowCount++;
                continue;
            }
            String embryoDesc = this.normalizeText(cxResult.getMainMaterialDesc());
            if (StringUtils.isEmpty(embryoDesc)) {
                continue;
            }
            cxIndex.getDescKeys().add(embryoDesc);
            cxIndex.getDescStatusKeys().add(this.buildKey(embryoDesc, this.normalizeText(cxResult.getProductStatus())));
        }
        if (importRowCount > 0) {
            log.info("成型未排产检查：排除成型导入行 {} 条，不作为成型排产依据", importRowCount);
        }
        return cxIndex;
    }

    /**
     * 聚合硫化侧有排产的胎胚（按「胎胚描述 + 产品状态」）。
     *
     * @param context 校验上下文
     * @return 胎胚聚合结果
     */
    private Map<String, LhEmbryoAggregate> buildLhAggregateMap(LhScheduleCheckContext context) {
        Map<String, LhEmbryoAggregate> aggregateMap = new LinkedHashMap<>(16);
        int blankDescRowCount = 0;
        for (LhScheduleResult result : context.getResults()) {
            if (result == null || this.sumPlanQty(context, result) <= 0) {
                continue;
            }
            String embryoDesc = this.normalizeText(result.getMainMaterialDesc());
            if (StringUtils.isEmpty(embryoDesc)) {
                // 胎胚描述缺失时无法与成型侧对齐，跳过避免误报
                blankDescRowCount++;
                continue;
            }
            String productStatus = this.normalizeText(result.getProductStatus());
            String aggregateKey = this.buildKey(embryoDesc, productStatus);
            LhEmbryoAggregate aggregate = aggregateMap.computeIfAbsent(aggregateKey, key -> {
                LhEmbryoAggregate created = new LhEmbryoAggregate();
                created.setEmbryoDesc(embryoDesc);
                created.setProductStatus(productStatus);
                return created;
            });
            aggregate.setPlanQty(aggregate.getPlanQty() + this.sumPlanQty(context, result));
            aggregate.setRowCount(aggregate.getRowCount() + 1);
            String machineCode = StringUtils.trimToEmpty(result.getLhMachineCode());
            if (StringUtils.isNotEmpty(machineCode)) {
                aggregate.getMachineCodes().add(machineCode);
            }
            // 胎胚收尾标识：成型本批不再安排该胎胚，成型未排产属可解释异常
            if (EMBRYO_ENDING_YES.equals(StringUtils.trimToEmpty(result.getIsEmbryoEnding()))) {
                aggregate.setEmbryoEnding(true);
            }
        }
        if (blankDescRowCount > 0) {
            log.debug("成型未排产检查：{} 条硫化结果行胎胚描述为空，已跳过, factoryCode={}",
                    blankDescRowCount, context.getFactoryCode());
        }
        return aggregateMap;
    }

    /**
     * 判断某胎胚组在成型侧是否存在排产。
     *
     * <p>产品状态非空时按「描述+状态」精确匹配；产品状态为空时只按描述匹配，避免状态缺失误报。</p>
     *
     * @param cxIndex   成型排产索引
     * @param aggregate 硫化胎胚聚合
     * @return true-成型有排产
     */
    private boolean hasCxSchedule(CxScheduleIndex cxIndex, LhEmbryoAggregate aggregate) {
        if (StringUtils.isEmpty(aggregate.getProductStatus())) {
            return cxIndex.getDescKeys().contains(aggregate.getEmbryoDesc());
        }
        String descStatusKey = this.buildKey(aggregate.getEmbryoDesc(), aggregate.getProductStatus());
        return cxIndex.getDescStatusKeys().contains(descStatusKey);
    }

    /**
     * 构建异常记录。
     *
     * @param aggregate 硫化胎胚聚合
     * @return 异常记录
     */
    private LhScheduleCheckResult buildAbnormal(LhEmbryoAggregate aggregate) {
        String productStatusDisplay = this.resolveProductStatusLabel(aggregate.getProductStatus());
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(aggregate.getEmbryoDesc());
        abnormal.setMessage(MessageFormat.format(I18nUtil.getMessage(MESSAGE_KEY),
                aggregate.getEmbryoDesc(), productStatusDisplay, String.valueOf(aggregate.getPlanQty())));
        Map<String, Object> detail = new LinkedHashMap<>(8);
        detail.put("embryoDesc", aggregate.getEmbryoDesc());
        detail.put("productStatus", aggregate.getProductStatus());
        detail.put("lhPlanQty", aggregate.getPlanQty());
        detail.put("lhRowCount", aggregate.getRowCount());
        detail.put("lhMachineCodes", String.join(",", aggregate.getMachineCodes()));
        detail.put("embryoEnding", aggregate.isEmbryoEnding());
        abnormal.setDetailJson(JSONUtil.toJsonStr(detail));
        if (aggregate.isEmbryoEnding()) {
            // 成型已收尾（本批不再安排该胎胚）属可解释异常：仍保留记录供前端展示，
            // 但由系统直接标记已了解，避免这类已知原因阻塞发布/导出等操作
            abnormal.setAcknowledged(LhScheduleCheckResult.ACKNOWLEDGED_YES);
        }
        return abnormal;
    }

    /**
     * 汇总硫化结果行的班次计划量（class1~class8）。
     *
     * @param context 校验上下文
     * @param result  排程结果行
     * @return 计划量合计
     */
    private int sumPlanQty(LhScheduleCheckContext context, LhScheduleResult result) {
        int totalPlanQty = 0;
        for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = context.getShiftPlanQty(result, shiftIndex);
            if (planQty != null && planQty > 0) {
                totalPlanQty += planQty;
            }
        }
        return totalPlanQty;
    }

    /**
     * 汇总成型结果行的班次计划量（class1~class8）。
     *
     * @param cxResult 成型排程结果行
     * @return 计划量合计
     */
    private int sumPlanQty(CxScheduleResult cxResult) {
        return Stream.of(cxResult.getClass1PlanQty(), cxResult.getClass2PlanQty(),
                        cxResult.getClass3PlanQty(), cxResult.getClass4PlanQty(),
                        cxResult.getClass5PlanQty(), cxResult.getClass6PlanQty(),
                        cxResult.getClass7PlanQty(), cxResult.getClass8PlanQty())
                .filter(Objects::nonNull)
                .mapToInt(BigDecimal::intValue)
                .sum();
    }

    /**
     * 产品状态编码转字典名称（如 S→量产），字典未配置时回退原始编码。
     *
     * <p>字典读取失败只影响文案展示，不能让整个检查项抛异常被框架降级，因此这里做一次兜底。</p>
     *
     * @param productStatus 产品状态编码
     * @return 展示名称
     */
    private String resolveProductStatusLabel(String productStatus) {
        if (StringUtils.isEmpty(productStatus)) {
            return BLANK_STATUS_DISPLAY;
        }
        String dictLabel;
        try {
            dictLabel = DictUtils.getLabel(PRODUCT_STATUS_DICT_TYPE, productStatus);
        } catch (Exception exception) {
            log.warn("成型未排产检查：产品状态字典翻译失败，回退原始编码, productStatus={}", productStatus, exception);
            return productStatus;
        }
        return StringUtils.defaultIfBlank(dictLabel, productStatus);
    }

    /**
     * 归一化文本：null 转空串、全角空格转半角后去首尾空白。
     *
     * @param text 原始文本
     * @return 归一化文本
     */
    private String normalizeText(String text) {
        String replaced = StringUtils.replace(StringUtils.defaultString(text), FULL_WIDTH_SPACE, StringUtils.SPACE);
        return StringUtils.trimToEmpty(replaced);
    }

    /**
     * 构建「胎胚描述 + 产品状态」组合键。
     *
     * @param embryoDesc    胎胚描述
     * @param productStatus 产品状态
     * @return 组合键
     */
    private String buildKey(String embryoDesc, String productStatus) {
        return embryoDesc + KEY_SEPARATOR + productStatus;
    }

    /**
     * 成型侧排产索引（描述集合 + 描述状态组合集合）。
     */
    @Data
    private static class CxScheduleIndex {

        /** 有排产的「描述 + 状态」组合键集合 */
        private final Set<String> descStatusKeys = new LinkedHashSet<>(64);

        /** 有排产的胎胚描述集合（产品状态缺失时的兜底匹配） */
        private final Set<String> descKeys = new LinkedHashSet<>(64);
    }

    /**
     * 硫化侧胎胚聚合（描述、状态、计划量、机台）。
     */
    @Data
    private static class LhEmbryoAggregate {

        /** 胎胚描述 */
        private String embryoDesc;

        /** 产品状态（可能为空） */
        private String productStatus;

        /** class1~class8 计划量合计 */
        private int planQty;

        /** 结果行数 */
        private int rowCount;

        /** 涉及机台 */
        private final Set<String> machineCodes = new LinkedHashSet<>(8);

        /** 是否命中胎胚收尾（IS_EMBRYO_ENDING=1）：成型本批不再安排，属可解释异常 */
        private boolean embryoEnding;
    }
}
