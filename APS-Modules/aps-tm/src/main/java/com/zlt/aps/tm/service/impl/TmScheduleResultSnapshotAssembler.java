package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎面排程结果来源快照聚合器。
 *
 * <p>同一结果行可能由多个成型工单和多个班次任务合并而来。本聚合器统一处理文本稳定去重、
 * 成型余量按来源工单去重求和，以及数值快照冲突的确定性取值。</p>
 */
@Slf4j
public final class TmScheduleResultSnapshotAssembler {

    /**
     * 工具类不允许实例化。
     */
    private TmScheduleResultSnapshotAssembler() {
    }

    /**
     * 将来源任务快照聚合到胎面排程结果。
     *
     * @param result         待补充快照的胎面排程结果
     * @param sourceTaskList 归属于该结果行的来源任务
     */
    public static void assemble(TmScheduleResult result, List<TmTaskDraft> sourceTaskList) {
        if (result == null || CollUtil.isEmpty(sourceTaskList)) {
            return;
        }
        List<TmTaskDraft> sortedTaskList = sourceTaskList.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(TmScheduleResultSnapshotAssembler::resolveSourceOrderNo,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(TmTaskDraft::getBusinessKey, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(sortedTaskList)) {
            return;
        }

        result.setTreadShoulderLength(selectStableNumber(result, sortedTaskList,
                "treadShoulderLength", TmTaskDraft::getTreadShoulderLength));
        result.setCxRemainQty(sumRemainQtyBySourceOrder(result, sortedTaskList));
        result.setMaterialCode(joinStableText(sortedTaskList, TmTaskDraft::getMaterialCode));
        result.setMaterialDesc(joinStableText(sortedTaskList, TmTaskDraft::getMaterialDesc));
        result.setEmbryoCode(joinStableText(sortedTaskList, TmTaskDraft::getEmbryoCode));
        result.setMainMaterialDesc(joinStableText(sortedTaskList, TmTaskDraft::getMainMaterialDesc));
        result.setCxMachineCode(joinStableText(sortedTaskList, TmTaskDraft::getCxMachineCode));
        result.setSixClockStockQty(selectStableNumber(result, sortedTaskList,
                "sixClockStockQty", TmTaskDraft::getSixClockStockQty));
        result.setCurlRollLength(selectStableNumber(result, sortedTaskList,
                "curlRollLength", TmScheduleResultSnapshotAssembler::resolveEffectiveCurlRollLength));
    }

    /**
     * 解析任务的来源成型工单号，并将空值留给排序器放到末尾。
     *
     * @param task 胎面任务
     * @return 去除首尾空格后的来源成型工单号；空值返回 null
     */
    private static String resolveSourceOrderNo(TmTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getSourceOrderNos())) {
            return null;
        }
        return task.getSourceOrderNos().trim();
    }

    /**
     * 稳定去重并拼接文本快照。
     *
     * @param sortedTaskList 已按来源工单和业务键排序的任务
     * @param valueGetter    文本字段读取函数
     * @return 逗号分隔的去重文本；没有有效值时返回 null
     */
    private static String joinStableText(List<TmTaskDraft> sortedTaskList,
                                         Function<TmTaskDraft, String> valueGetter) {
        Set<String> valueSet = sortedTaskList.stream()
                .map(valueGetter)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return CollUtil.isEmpty(valueSet) ? null : String.join(",", valueSet);
    }

    /**
     * 按来源成型工单去重后汇总成型余量。
     *
     * <p>同一来源工单因班次拆分产生多条任务时只计一次；同源值冲突时保留稳定首值并记录告警。</p>
     *
     * @param result         胎面排程结果，用于输出冲突定位信息
     * @param sortedTaskList 已按来源工单和业务键排序的任务
     * @return 成型余量合计；没有有效来源工单或余量时返回 null
     */
    private static BigDecimal sumRemainQtyBySourceOrder(TmScheduleResult result,
                                                         List<TmTaskDraft> sortedTaskList) {
        Map<String, BigDecimal> remainQtyMap = new LinkedHashMap<>();
        for (TmTaskDraft task : sortedTaskList) {
            String sourceOrderNo = resolveSourceOrderNo(task);
            BigDecimal remainQty = task.getTailBalanceQty();
            if (StrUtil.isBlank(sourceOrderNo) || remainQty == null) {
                continue;
            }
            BigDecimal selectedValue = remainQtyMap.get(sourceOrderNo);
            if (selectedValue == null) {
                remainQtyMap.put(sourceOrderNo, remainQty);
                continue;
            }
            if (selectedValue.compareTo(remainQty) != 0) {
                logConflict(result, "cxRemainQty[" + sourceOrderNo + "]", selectedValue,
                        java.util.Arrays.asList(selectedValue, remainQty));
            }
        }
        if (remainQtyMap.isEmpty()) {
            return null;
        }
        return remainQtyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 从排序后的任务中选择首个有效数值，存在冲突时记录告警。
     *
     * @param result         胎面排程结果，用于输出冲突定位信息
     * @param sortedTaskList 已按来源工单和业务键排序的任务
     * @param fieldName      结果字段名
     * @param valueGetter    数值字段读取函数
     * @return 稳定首值；没有有效值时返回 null
     */
    private static BigDecimal selectStableNumber(TmScheduleResult result,
                                                  List<TmTaskDraft> sortedTaskList,
                                                  String fieldName,
                                                  Function<TmTaskDraft, BigDecimal> valueGetter) {
        List<BigDecimal> distinctValueList = new ArrayList<>();
        sortedTaskList.stream().map(valueGetter).filter(Objects::nonNull).forEach(value -> {
            boolean exists = distinctValueList.stream().anyMatch(existing -> existing.compareTo(value) == 0);
            if (!exists) {
                distinctValueList.add(value);
            }
        });
        if (distinctValueList.isEmpty()) {
            return null;
        }
        BigDecimal selectedValue = distinctValueList.get(0);
        if (distinctValueList.size() > 1) {
            logConflict(result, fieldName, selectedValue, distinctValueList);
        }
        return selectedValue;
    }

    /**
     * 解析任务有效卷曲长度，实际卷曲长度无效时使用参数默认值。
     *
     * @param task 胎面任务
     * @return 有效卷曲长度；实际值和默认值均无效时返回 null
     */
    private static BigDecimal resolveEffectiveCurlRollLength(TmTaskDraft task) {
        if (task != null && task.getCurlRollLength() != null
                && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        if (task != null && task.getDefaultCurlRollLength() != null
                && task.getDefaultCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getDefaultCurlRollLength();
        }
        return null;
    }

    /**
     * 记录同一结果行数值快照冲突。
     *
     * @param result            胎面排程结果
     * @param fieldName         冲突字段名
     * @param selectedValue     最终选择值
     * @param conflictValueList 冲突值列表
     */
    private static void logConflict(TmScheduleResult result, String fieldName, BigDecimal selectedValue,
                                    List<BigDecimal> conflictValueList) {
        log.warn("[TM_RESULT_SNAPSHOT_CONFLICT] batchNo={}, machineCode={}, treadCode={}, fieldName={}, "
                        + "selectedValue={}, conflictValues={}",
                result == null ? null : result.getBatchNo(),
                result == null ? null : result.getMachineCode(),
                result == null ? null : result.getTreadCode(),
                fieldName, selectedValue, conflictValueList);
    }
}
