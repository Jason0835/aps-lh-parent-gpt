package com.zlt.aps.common.core.utils;

import com.zlt.aps.common.core.domain.vo.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 按计划分析报表的库存递推结果提取自动滚动输入，不依赖页面分页或导出格式。
 */
public final class AutomaticRollingReportProjectionUtils {

    private AutomaticRollingReportProjectionUtils() {
    }

    /**
     * 从报表结果中提取一个物料的目标班预计库存和下一班需求起点之后的逐班需求。
     *
     * @param report                已按全量物料计算的计划分析报表
     * @param readData              与报表计算使用的原始需求输入
     * @param productCode           胎面或胎侧编码
     * @param targetShiftStartTime  目标物理班次开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param demandWindowStartTime 下一物理班开始时刻，格式 yyyy-MM-dd HH:mm:ss
     * @return 投影；报表没有同时包含两个物理边界时映射标记为 false
     */
    public static AutomaticRollingReportProjection project(ScheduleAnalysisReportVO report,
                                                           ScheduleAnalysisReportReadData readData,
                                                           String productCode,
                                                           String targetShiftStartTime,
                                                           String demandWindowStartTime) {
        AutomaticRollingReportProjection projection = new AutomaticRollingReportProjection();
        if (report == null || readData == null || productCode == null) {
            return projection;
        }
        List<ScheduleAnalysisShiftHeaderVO> headers = report.getShiftHeaders() == null
                ? Collections.emptyList() : report.getShiftHeaders();
        int targetIndex = findShiftIndex(headers, targetShiftStartTime);
        int demandStartIndex = findShiftIndex(headers, demandWindowStartTime);
        if (targetIndex < 0 || demandStartIndex <= targetIndex) {
            return projection;
        }
        projection.setReportScheduleDate(report.getScheduleDate() == null
                ? null : report.getScheduleDate().toString());
        projection.setTargetColumnIndex(targetIndex);
        // 记录需求窗口起始列，供调用方把补零列索引换算到未来需求下标。
        projection.setDemandStartColumnIndex(demandStartIndex);
        List<ScheduleAnalysisReportRowVO> reportRows = report.getRows() == null
                ? Collections.emptyList() : report.getRows();
        ScheduleAnalysisReportRowVO stockRow = reportRows.stream()
                .filter(row -> Objects.equals(row.getDataType(), 2))
                .filter(row -> Objects.equals(productCode, row.getProductCode()))
                .findFirst().orElse(null);
        if (stockRow == null) {
            return projection;
        }
        projection.setStockSourcePresent(readData.getSixClockStocks() != null
                && readData.getSixClockStocks().containsKey(productCode));
        List<BigDecimal> stockValues = values(stockRow);
        projection.setExpectedStock(stockValues.get(targetIndex));
        projection.setSixClockStockQty(stockRow.getSixClockStockQty());

        List<ScheduleAnalysisReportRowVO> demandRows = reportRows.stream()
                .filter(row -> Objects.equals(row.getDataType(), 1))
                .filter(row -> Objects.equals(productCode, row.getProductCode()))
                .collect(Collectors.toList());
        projection.setTargetDemandQty(demandRows.stream().map(AutomaticRollingReportProjectionUtils::values)
                .map(shiftValues -> shiftValues.get(targetIndex)).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ScheduleAnalysisReportRowVO planRow = reportRows.stream()
                .filter(row -> Objects.equals(row.getDataType(), 3))
                .filter(row -> Objects.equals(productCode, row.getProductCode()))
                .findFirst().orElse(null);
        BigDecimal previousShiftPlanQty = BigDecimal.ZERO;
        if (targetIndex > 0 && planRow != null) {
            previousShiftPlanQty = values(planRow).get(targetIndex - 1);
        }
        projection.setPreviousShiftPlanQty(previousShiftPlanQty);
        List<ScheduleAnalysisDemandInput> demandInputs = readData.getDemands() == null
                ? Collections.emptyList() : readData.getDemands().stream()
                .filter(demand -> Objects.equals(productCode, demand.getProductCode()))
                .collect(Collectors.toList());
        projection.setDemandSourcePresent(!demandRows.isEmpty()
                && hasDemandSourceAtIndex(demandInputs, targetIndex));
        // 只要报表为该物料生成了需求行，即视为有需求数据；单班无成型计划导致的空列不代表来源缺失。
        projection.setDemandRowPresent(!demandRows.isEmpty());
        for (int shiftIndex = 0; shiftIndex < headers.size(); shiftIndex++) {
            if (!hasDemandSourceAtIndex(demandInputs, shiftIndex)) {
                projection.getZeroDefaultDemandColumnIndexes().add(shiftIndex);
            }
        }
        List<BigDecimal> futureDemandQtyList = new ArrayList<>();
        for (int shiftIndex = demandStartIndex; shiftIndex < headers.size(); shiftIndex++) {
            int currentShiftIndex = shiftIndex;
            BigDecimal demandQty = demandRows.stream().map(AutomaticRollingReportProjectionUtils::values)
                    .map(shiftValues -> shiftValues.get(currentShiftIndex))
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            // 保留其他有效需求来源的数量，只有缺失的部分按零，不能把整班有效需求清零。
            futureDemandQtyList.add(demandQty);
        }
        projection.setFutureDemandQtyList(futureDemandQtyList);
        projection.setPhysicalWindowMapped(true);
        return projection;
    }

    /**
     * 汇总原始来源缺失和需求窗口尾部补零信息，不将缺失来源伪装为真实零来源。
     *
     * @param projection    已完成物理窗口映射的报表投影
     * @param upThreshold   上修需求班数
     * @param downThreshold 下修阈值班数
     * @param downTarget    下修目标班数
     * @return 库存、需求列和窗口尾部的补零审计
     */
    public static Map<String, Object> zeroDefaultDetails(AutomaticRollingReportProjection projection,
                                                         BigDecimal upThreshold, BigDecimal downThreshold,
                                                         BigDecimal downTarget) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (projection == null) {
            return details;
        }
        details.put("stockBaseDefaulted", !projection.isStockSourcePresent());
        details.put("demandColumnIndexes", projection.getZeroDefaultDemandColumnIndexes());
        BigDecimal requiredCount = upThreshold.max(downThreshold).max(downTarget)
                .setScale(0, java.math.RoundingMode.CEILING);
        int availableCount = projection.getFutureDemandQtyList() == null
                ? 0 : projection.getFutureDemandQtyList().size();
        BigDecimal tailDefaultCount = requiredCount.subtract(BigDecimalUtils.valueOf(availableCount)).max(BigDecimal.ZERO);
        details.put("futureDemandTailDefaultedCount", tailDefaultCount);
        details.put("applied", !projection.isStockSourcePresent()
                || !projection.getZeroDefaultDemandColumnIndexes().isEmpty() || tailDefaultCount.signum() > 0
                || !projection.isDemandSourcePresent());
        return details;
    }

    /**
     * 汇总定时滚动物料的单侧计算、补零及未发生数量变化情况，供TM日志和TC任务摘要复用。
     *
     * @param evidenceItems 逐物料判定证据集合
     * @return 各类物料数量；未发生变化不包含因校验跳过的物料
     */
    public static Map<String, Long> summarizeEvidence(Collection<?> evidenceItems) {
        List<Map<?, ?>> evidences = evidenceItems.stream().filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item).collect(Collectors.toList());
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("primaryOnlyCount", evidences.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("primarySidePresent"))
                        && Boolean.FALSE.equals(item.get("secondarySidePresent"))).count());
        counts.put("secondaryOnlyCount", evidences.stream()
                .filter(item -> Boolean.FALSE.equals(item.get("primarySidePresent"))
                        && Boolean.TRUE.equals(item.get("secondarySidePresent"))).count());
        counts.put("zeroDefaultCount", evidences.stream().map(item -> item.get("zeroDefaultDetails"))
                .filter(Map.class::isInstance).map(item -> (Map<?, ?>) item)
                .filter(item -> Boolean.TRUE.equals(item.get("applied"))).count());
        counts.put("unchangedCount", evidences.stream().filter(item -> !item.containsKey("skipReason")
                && Boolean.FALSE.equals(item.get("adjusted"))).count());
        return counts;
    }

    /**
     * 按实际开始时刻唯一定位报表动态班次列。
     *
     * @param headers        报表班次表头
     * @param shiftStartTime 目标物理班次开始时刻
     * @return 唯一匹配的零基列索引；缺失或重复时返回-1
     */
    private static int findShiftIndex(List<ScheduleAnalysisShiftHeaderVO> headers, String shiftStartTime) {
        if (shiftStartTime == null) {
            return -1;
        }
        List<Integer> matchingIndexes = headers.stream().filter(Objects::nonNull)
                .filter(header -> Objects.equals(shiftStartTime, header.getShiftStartTime()))
                .map(ScheduleAnalysisShiftHeaderVO::getClassIndex)
                .filter(Objects::nonNull).filter(index -> index >= 1 && index <= 8)
                .map(index -> index - 1).collect(Collectors.toList());
        return matchingIndexes.size() == 1 ? matchingIndexes.get(0) : -1;
    }

    /**
     * 按报表列顺序读取八个动态班次数量。
     *
     * @param row 计划分析报表行
     * @return CLASS1至CLASS8数量
     */
    private static List<BigDecimal> values(ScheduleAnalysisReportRowVO row) {
        return Arrays.asList(row.getClass1(), row.getClass2(), row.getClass3(), row.getClass4(),
                row.getClass5(), row.getClass6(), row.getClass7(), row.getClass8());
    }

    /**
     * 判断报表物料对应的每条需求来源是否都包含指定班次的有效数量。
     *
     * @param demandInputs 物料对应的成型需求来源
     * @param shiftIndex   零基班次列索引
     * @return 至少存在一条来源且每条来源该班数量非空时返回true
     */
    private static boolean hasDemandSourceAtIndex(List<ScheduleAnalysisDemandInput> demandInputs, int shiftIndex) {
        return demandInputs != null && !demandInputs.isEmpty() && demandInputs.stream().allMatch(input ->
                input.getFormingClassPlanQty() != null
                        && input.getFormingClassPlanQty().size() > shiftIndex
                        && input.getFormingClassPlanQty().get(shiftIndex) != null);
    }
}
