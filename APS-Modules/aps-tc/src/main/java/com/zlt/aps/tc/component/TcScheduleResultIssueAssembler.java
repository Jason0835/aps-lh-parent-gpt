package com.zlt.aps.tc.component;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.service.mes.TcShiftBusinessDateResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 胎侧六班排程结果MES下发组装器。
 */
@Component
public class TcScheduleResultIssueAssembler {

    /** MES班别计划量写入器。 */
    private static final Map<String, BiConsumer<TcScheduleResultIssue, BigDecimal>> PLAN_QTY_SETTER_MAP;

    /** MES班别顺序写入器。 */
    private static final Map<String, BiConsumer<TcScheduleResultIssue, Integer>> SEQUENCE_SETTER_MAP;

    /** MES班别分析写入器。 */
    private static final Map<String, BiConsumer<TcScheduleResultIssue, String>> ANALYSIS_SETTER_MAP;

    static {
        Map<String, BiConsumer<TcScheduleResultIssue, BigDecimal>> planSetterMap = new LinkedHashMap<>();
        planSetterMap.put("NIGHT", TcScheduleResultIssue::setNightPlanQty);
        planSetterMap.put("DAY", TcScheduleResultIssue::setDayPlanQty);
        planSetterMap.put("MID", TcScheduleResultIssue::setMidPlanQty);
        PLAN_QTY_SETTER_MAP = Collections.unmodifiableMap(planSetterMap);

        Map<String, BiConsumer<TcScheduleResultIssue, Integer>> sequenceSetterMap = new LinkedHashMap<>();
        sequenceSetterMap.put("NIGHT", TcScheduleResultIssue::setNightProduceOrder);
        sequenceSetterMap.put("DAY", TcScheduleResultIssue::setDayProduceOrder);
        sequenceSetterMap.put("MID", TcScheduleResultIssue::setMidProduceOrder);
        SEQUENCE_SETTER_MAP = Collections.unmodifiableMap(sequenceSetterMap);

        Map<String, BiConsumer<TcScheduleResultIssue, String>> analysisSetterMap = new LinkedHashMap<>();
        analysisSetterMap.put("NIGHT", TcScheduleResultIssue::setNightAnalysis);
        analysisSetterMap.put("DAY", TcScheduleResultIssue::setDayAnalysis);
        analysisSetterMap.put("MID", TcScheduleResultIssue::setMidAnalysis);
        ANALYSIS_SETTER_MAP = Collections.unmodifiableMap(analysisSetterMap);
    }

    /**
     * 将六班结果拆为MES三班业务日期记录。
     *
     * @param resultList 待发布结果
     * @param dataVersion 发布数据版本
     * @return MES下发记录
     */
    public List<TcScheduleResultIssue> assemble(List<TcScheduleResult> resultList, String dataVersion) {
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, TcScheduleResultIssue> issueMap = new LinkedHashMap<>();
        resultList.stream().forEach(result -> this.appendResult(issueMap, result, dataVersion));
        List<TcScheduleResultIssue> issueList = new ArrayList<>(issueMap.values());
        issueList.sort(Comparator.comparing(TcScheduleResultIssue::getScheduleDate)
                .thenComparing(item -> StrUtil.blankToDefault(item.getMachineCode(), ""))
                .thenComparing(item -> StrUtil.blankToDefault(item.getSidewallCode(), "")));
        return issueList;
    }

    /**
     * 将一条六班结果追加到MES业务日期集合。
     *
     * @param issueMap MES业务日期集合
     * @param result 排程结果
     * @param dataVersion 发布数据版本
     */
    private void appendResult(Map<String, TcScheduleResultIssue> issueMap, TcScheduleResult result,
                              String dataVersion) {
        if (result == null || result.getId() == null || result.getScheduleDate() == null) {
            return;
        }
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder)));
            if (planQty.signum() <= 0) {
                continue;
            }
            Date mesBusinessDate = TcShiftBusinessDateResolver.resolveMesBusinessDate(
                    result.getScheduleDate(), shiftOrder);
            String issueKey = result.getId() + "|" + DateUtil.formatDate(mesBusinessDate);
            TcScheduleResultIssue issue = issueMap.computeIfAbsent(issueKey,
                    key -> this.createIssue(result, mesBusinessDate, dataVersion));
            String mesShiftCode = TcShiftBusinessDateResolver.resolveMesShiftCode(shiftOrder);
            PLAN_QTY_SETTER_MAP.get(mesShiftCode).accept(issue, planQty);
            Object sequenceValue = result.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
            Integer sequence = sequenceValue instanceof Number ? ((Number) sequenceValue).intValue() : null;
            SEQUENCE_SETTER_MAP.get(mesShiftCode).accept(issue, sequence);
            Object analysisValue = result.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
            ANALYSIS_SETTER_MAP.get(mesShiftCode).accept(issue,
                    analysisValue == null ? null : String.valueOf(analysisValue));
        }
    }

    /**
     * 创建MES业务日期记录并复制工艺快照。
     *
     * @param result 排程结果
     * @param mesBusinessDate MES业务日期
     * @param dataVersion 发布数据版本
     * @return MES下发记录
     */
    private TcScheduleResultIssue createIssue(TcScheduleResult result, Date mesBusinessDate, String dataVersion) {
        TcScheduleResultIssue issue = new TcScheduleResultIssue();
        issue.setScheduleDate(mesBusinessDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        issue.setBatchNo(result.getBatchNo());
        issue.setOrderNo(result.getOrderNo());
        issue.setSidewallCode(result.getSidewallCode());
        issue.setMachineCode(result.getMachineCode());
        issue.setGlueCode(result.getGlueCode());
        issue.setBaseGlueCode(result.getBaseGlueCode());
        issue.setWholeGlueCode(result.getWholeGlueCode());
        issue.setGlueSeq(result.getGlueSeq());
        issue.setMouthPlateCode(result.getMouthPlateCode());
        issue.setConstructionVersion(result.getConstructionVersion());
        issue.setSidewallCraft(result.getSidewallCraft());
        issue.setTailFlag(result.getTailFlag());
        issue.setFactoryCode(result.getFactoryCode());
        // 胎侧结果表不冗余公司编码，MES契约按现有单工厂口径使用工厂编码补齐公司编码。
        issue.setCompanyCode(result.getFactoryCode());
        issue.setDataVersion(dataVersion);
        issue.setTaskVersion(result.getTaskVersion());
        issue.setIdempotencyKey(this.buildIdempotencyKey(result));
        return issue;
    }

    /**
     * 构造结果行发布幂等键。
     *
     * @param result 排程结果
     * @return 幂等键
     */
    public String buildIdempotencyKey(TcScheduleResult result) {
        return StrUtil.blankToDefault(result.getBatchNo(), "") + "|"
                + StrUtil.blankToDefault(result.getOrderNo(), "") + "|"
                + (result.getTaskVersion() == null ? 0L : result.getTaskVersion());
    }
}
