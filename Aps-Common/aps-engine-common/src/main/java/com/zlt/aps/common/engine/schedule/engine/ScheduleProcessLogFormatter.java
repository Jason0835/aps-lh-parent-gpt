package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationCalculator;
import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationResult;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/** TM/TC 自动排程过程日志公共格式化组件。 */
public final class ScheduleProcessLogFormatter {

    public String buildToolUsageSummary(ScheduleTaskDraftModel task,
                                        Map<String, ScheduleToolLedgerSnapshot> snapshotMap,
                                        String disabledParameterCode) {
        return this.buildToolUsageSummary(task, snapshotMap, disabledParameterCode, "产品代码");
    }

    /**
     * 构建带产品代码和成型释放明细的工装限制公式。
     *
     * @param task 当前任务
     * @param snapshotMap 工装账本快照
     * @param disabledParameterCode 工装总量参数编码
     * @param productLabel 产品代码中文标签
     * @return 工装限制公式
     */
    public String buildToolUsageSummary(ScheduleTaskDraftModel task,
                                         Map<String, ScheduleToolLedgerSnapshot> snapshotMap,
                                         String disabledParameterCode, String productLabel) {
        if (task.getTotalToolQty() == null || task.getTotalToolQty().compareTo(BigDecimal.ZERO) <= 0) {
            return "工装限制：" + productLabel + "=" + this.displayProductCode(task)
                    + "；可用工装数量=未计算（未启用工装约束），本任务生产占用工装数量=未计算，"
                    + "当班成型消耗=未计算，任务后可用工装数量=未计算，有效卷曲长度=未计算；"
                    + disabledParameterCode + "未配置或非正数，可用工装数量、工装允许最大计划量、本任务生产占用工装数量和剩余工装数量均未计算";
        }
        BigDecimal effectiveCurlLength = task.getCurlRollLength() != null
                && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0
                ? task.getCurlRollLength() : task.getDefaultCurlRollLength();
        if (effectiveCurlLength == null || effectiveCurlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return "工装限制：" + productLabel + "=" + this.displayProductCode(task)
                    + "；可用工装数量=未计算，本任务生产占用工装数量=未计算，当班成型消耗=未计算，"
                    + "任务后可用工装数量=未计算，有效卷曲长度=未计算；任务卷曲长度和默认卷曲长度均未配置或非正数，"
                    + "无法计算可用工装数量对应产量、生产占用工装数量和剩余工装数量";
        }
        ScheduleToolLedgerSnapshot snapshot = snapshotMap == null ? null : snapshotMap.get(task.getBusinessKey());
        BigDecimal available = snapshot == null ? task.getAvailableToolQty() : snapshot.getAvailableToolQty();
        BigDecimal remaining = snapshot == null ? task.getRemainingToolQty() : snapshot.getRemainingToolQty();
        BigDecimal productionOccupationQty = this.resolveProductionOccupationQty(task, snapshot, effectiveCurlLength);
        BigDecimal formingReleaseQty = this.resolveFormingReleaseQty(task, snapshot, effectiveCurlLength);
        BigDecimal productionQty = this.resolveProductionDisplayQty(task, snapshot, effectiveCurlLength,
                productionOccupationQty);
        BigDecimal formingDemandQty = this.resolveFormingDemandDisplayQty(task, snapshot, formingReleaseQty);
        BigDecimal effectiveToolQtyLimit = snapshot == null || snapshot.getEffectiveToolQtyLimit() == null
                ? task.getTotalToolQty() : snapshot.getEffectiveToolQtyLimit();
        return "工装限制：" + productLabel + "=" + this.displayProductCode(task)
                + "；可用工装数量=" + this.displayQuantity(available) + "套"
                + "；本任务生产占用工装数量=" + this.displayQuantity(productionQty) + "米÷"
                + this.displayQuantity(effectiveCurlLength) + "米/套="
                + this.displayQuantity(productionOccupationQty) + "套"
                + "；当班成型消耗=" + this.displayQuantity(formingDemandQty) + "米÷"
                + this.displayQuantity(effectiveCurlLength) + "米/套="
                + this.displayQuantity(formingReleaseQty) + "套"
                + "；任务后可用工装数量=min(max(" + this.displayQuantity(available)
                + "套-" + this.displayQuantity(productionOccupationQty) + "套+"
                + this.displayQuantity(formingReleaseQty) + "套,0),"
                + this.displayQuantity(effectiveToolQtyLimit) + "套)="
                + this.displayQuantity(remaining) + "套";
    }

    /**
     * 构建任务工装结算日志，展示生产占用和当班成型释放的逐项代入值。
     *
     * @param task 当前任务
     * @param snapshotMap 工装账本快照
     * @param disabledParameterCode 工装总量参数编码
     * @param productLabel 产品代码中文标签
     * @param taskSource 任务来源
     * @return 工装任务后结算公式
     */
    public String buildToolSettlementSummary(ScheduleTaskDraftModel task,
                                              Map<String, ScheduleToolLedgerSnapshot> snapshotMap,
                                              String disabledParameterCode, String productLabel,
                                              String taskSource) {
        String toolUsageSummary = this.buildToolUsageSummary(task, snapshotMap, disabledParameterCode, productLabel);
        String detail = toolUsageSummary.startsWith("工装限制：")
                ? toolUsageSummary.substring("工装限制：".length()) : toolUsageSummary;
        detail = this.removeAvailableToolDetail(detail);
        detail = this.insertFormingCode(detail, task);
        detail = detail.replace("本任务生产占用工装数量=", "生产占用工装数量=");
        return "工装任务后结算：来源=" + (taskSource == null ? "未提供" : taskSource) + "；" + detail;
    }

    /**
     * 从工装摘要中移除独立的可用工装数量字段，避免与结算后的公式中的结算前余额重复展示。
     *
     * @param detail 工装摘要明细
     * @return 移除独立可用工装数量后的明细
     */
    private String removeAvailableToolDetail(String detail) {
        int availableStart = detail.indexOf("；可用工装数量=");
        if (availableStart < 0) {
            return detail;
        }
        int productionStart = detail.indexOf("本任务生产占用工装数量=", availableStart);
        if (productionStart < 0) {
            return detail;
        }
        return detail.substring(0, availableStart) + "；" + detail.substring(productionStart);
    }

    /**
     * 在产品代码后补充当前任务对应的成型代码。
     *
     * @param detail 工装结算明细
     * @param task 当前任务
     * @return 补充成型代码后的结算明细
     */
    private String insertFormingCode(String detail, ScheduleTaskDraftModel task) {
        String formingCodeDetail = "成型代码="
                + this.displayEmbryoCode(task == null ? null : task.getEmbryoCode()) + "；";
        int productEnd = detail.indexOf("；");
        if (productEnd < 0) {
            return detail + "；" + formingCodeDetail;
        }
        return detail.substring(0, productEnd + 1) + formingCodeDetail + detail.substring(productEnd + 1);
    }

    /**
     * 解析任务本次生产占用的工装数量。
     *
     * @param task 当前任务
     * @param snapshot 工装账本快照
     * @param effectiveCurlLength 有效卷曲长度
     * @return 生产占用工装数量
     */
    private BigDecimal resolveProductionOccupationQty(ScheduleTaskDraftModel task,
                                                      ScheduleToolLedgerSnapshot snapshot,
                                                      BigDecimal effectiveCurlLength) {
        if (snapshot != null && snapshot.getProductionOccupationQty() != null) {
            return snapshot.getProductionOccupationQty();
        }
        if (this.isNotBlank(task.getUnplannedReasonCode())) {
            return BigDecimal.ZERO;
        }
        return this.calculateToolQty(task.getPlanQty(), effectiveCurlLength);
    }

    /**
     * 解析任务本次释放的成型工装数量。
     *
     * @param task 当前任务
     * @param snapshot 工装账本快照
     * @param effectiveCurlLength 有效卷曲长度
     * @return 成型释放工装数量
     */
    private BigDecimal resolveFormingReleaseQty(ScheduleTaskDraftModel task,
                                                 ScheduleToolLedgerSnapshot snapshot,
                                                 BigDecimal effectiveCurlLength) {
        if (snapshot != null && snapshot.getFormingReleaseQty() != null) {
            return snapshot.getFormingReleaseQty();
        }
        return this.calculateToolQty(task.getCurrentShiftDemandQty(), effectiveCurlLength);
    }

    /**
     * 解析日志中的生产量分子，未排任务必须按零生产量展示。
     *
     * @param task 当前任务
     * @param snapshot 工装账本快照
     * @param effectiveCurlLength 有效卷曲长度
     * @param productionOccupationQty 本次生产占用工装数量
     * @return 日志生产量分子
     */
    private BigDecimal resolveProductionDisplayQty(ScheduleTaskDraftModel task,
                                                   ScheduleToolLedgerSnapshot snapshot,
                                                   BigDecimal effectiveCurlLength,
                                                   BigDecimal productionOccupationQty) {
        if (this.isNotBlank(task.getUnplannedReasonCode())) {
            return BigDecimal.ZERO;
        }
        if (snapshot != null && productionOccupationQty != null) {
            return productionOccupationQty.multiply(effectiveCurlLength)
                    .setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return task.getPlanQty();
    }

    /**
     * 解析日志中的成型需求分子，同班同产品已经释放过时展示零，避免公式与实际账本不一致。
     *
     * @param task 当前任务
     * @param snapshot 工装账本快照
     * @param formingReleaseQty 本次成型释放工装数量
     * @return 日志成型需求分子
     */
    private BigDecimal resolveFormingDemandDisplayQty(ScheduleTaskDraftModel task,
                                                       ScheduleToolLedgerSnapshot snapshot,
                                                       BigDecimal formingReleaseQty) {
        if (snapshot != null && (formingReleaseQty == null
                || formingReleaseQty.compareTo(BigDecimal.ZERO) <= 0)) {
            return BigDecimal.ZERO;
        }
        return task.getCurrentShiftDemandQty();
    }

    /**
     * 按卷曲长度折算工装数量。
     *
     * @param quantity 米数
     * @param curlLength 有效卷曲长度
     * @return 工装套数
     */
    private BigDecimal calculateToolQty(BigDecimal quantity, BigDecimal curlLength) {
        if (quantity == null || curlLength == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return quantity.max(BigDecimal.ZERO).divide(curlLength, 8, RoundingMode.HALF_UP);
    }

    /**
     * 获取日志中的产品编码。
     *
     * @param task 当前任务
     * @return 产品编码或未提供
     */
    private String displayProductCode(ScheduleTaskDraftModel task) {
        return task == null || this.isBlank(task.getProcessCode()) ? "未提供" : task.getProcessCode();
    }

    public String buildSupplyHoursFormula(ScheduleTaskDraftModel task) {
        ScheduleSupplyDurationResult result = ScheduleSupplyDurationCalculator.calculate(task.getRollingStockQty(),
                task.getFormingGuardWindowQtyMap(), task.getFormingGuardWindowHoursMap());
        return result.getCalculationDetail();
    }

    public boolean isNewSpec(ScheduleTaskDraftModel task) {
        return task != null && task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit();
    }

    public boolean isExperimentSpec(ScheduleTaskDraftModel task) {
        return task != null && task.getCommonExperimentSpecInfo() != null
                && task.getCommonExperimentSpecInfo().isExperimentSpecHit();
    }

    public String displayGuardShiftCount(Integer count) {
        return count == null ? "未提供" : String.valueOf(count);
    }

    public String displayMachineSummary(String machineText, String machineLabel) {
        List<String> codes = this.isBlank(machineText) ? Collections.emptyList()
                : Arrays.stream(machineText.split("[,，]")).map(String::trim)
                .filter(value -> !this.isBlank(value)).distinct().collect(Collectors.toList());
        return machineLabel + " " + codes.size() + "台="
                + (codes.isEmpty() ? "未提供" : String.join(",", codes));
    }

    public String displaySupplyHours(BigDecimal hours) {
        return hours == null ? "未提供" : hours.stripTrailingZeros().toPlainString() + "H";
    }

    /**
     * 展示库存供应窗口内的成型需求，保留原有日志格式。
     *
     * @param windowQtyMap 窗口班次到成型需求量的映射
     * @return 窗口需求合计及班次明细
     */
    public String displayGuardWindow(Map<Integer, BigDecimal> windowQtyMap) {
        return this.displayGuardWindow(windowQtyMap, null);
    }

    /**
     * 展示库存供应窗口内的备库需求，日志只列备库窗口，不重复列当班成型消耗。
     *
     * @param windowQtyMap 窗口班次到成型需求量的映射
     * @param currentFormingShiftOrder 当前任务成型班次
     * @return 窗口需求合计及班次明细
     */
    public String displayGuardWindow(Map<Integer, BigDecimal> windowQtyMap, Integer currentFormingShiftOrder) {
        return this.displayGuardWindow(windowQtyMap, currentFormingShiftOrder, null);
    }

    /**
     * 展示当前任务最终有效备库窗口内的成型需求，日志只列备库窗口，不重复列当班成型消耗。
     *
     * @param windowQtyMap 窗口班次到成型需求量的映射
     * @param currentFormingShiftOrder 当前任务成型班次
     * @param effectiveGuardShiftCount 当前任务最终有效备库班数
     * @return 窗口需求合计及班次明细
     */
    public String displayGuardWindow(Map<Integer, BigDecimal> windowQtyMap, Integer currentFormingShiftOrder,
                                     Integer effectiveGuardShiftCount) {
        Map<Integer, BigDecimal> displayWindowQtyMap = windowQtyMap;
        if (windowQtyMap != null && currentFormingShiftOrder != null) {
            long displayGuardWindowLimit = effectiveGuardShiftCount == null || effectiveGuardShiftCount <= 0
                    ? Long.MAX_VALUE : effectiveGuardShiftCount;
            displayWindowQtyMap = windowQtyMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getKey() > currentFormingShiftOrder)
                    .sorted(Map.Entry.comparingByKey())
                    .limit(displayGuardWindowLimit)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (leftValue, rightValue) -> rightValue, LinkedHashMap::new));
        }
        if (displayWindowQtyMap == null || displayWindowQtyMap.isEmpty()) {
            return "库存供应计算窗口内成型需求合计=0";
        }
        BigDecimal total = displayWindowQtyMap.values().stream().filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String detail = displayWindowQtyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> "班" + entry.getKey() + "=" + this.nvl(entry.getValue()).stripTrailingZeros().toPlainString())
                .collect(Collectors.joining(" "));
        return "库存供应计算窗口内成型需求合计=" + total.stripTrailingZeros().toPlainString() + "：" + detail;
    }

    public void appendSignedTerm(List<String> terms, String name, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
            terms.add((value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "-") + name + value.abs().toPlainString());
        }
    }

    public String scoreValue(Map<String, BigDecimal> scoreItems, String scoreKey) {
        return this.nvl(scoreItems.get(scoreKey)).toPlainString();
    }

    public String displayEmbryoCode(String embryoCode) {
        return this.isBlank(embryoCode) ? "未提供" : embryoCode;
    }

    public String formatNewSpecDemandQty(BigDecimal quantity) {
        return this.nvl(quantity).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String displaySubtractedQuantity(BigDecimal quantity) {
        String text = this.displayQuantity(quantity) + "套";
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0 ? "(" + text + ")" : text;
    }

    public String displayQuantity(BigDecimal quantity) {
        return quantity == null ? "未计算" : quantity.stripTrailingZeros().toPlainString();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isNotBlank(String value) {
        return !this.isBlank(value);
    }
}
