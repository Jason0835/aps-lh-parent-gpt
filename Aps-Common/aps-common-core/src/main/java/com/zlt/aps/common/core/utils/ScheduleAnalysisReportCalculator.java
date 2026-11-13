package com.zlt.aps.common.core.utils;

import com.zlt.aps.common.core.domain.dto.ScheduleAnalysisReportQueryDTO;
import com.zlt.aps.common.core.domain.vo.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面、胎侧计划分析报表公共纯计算器。
 */
public final class ScheduleAnalysisReportCalculator {

    /**
     * 报表数量的统一精度。
     */
    private static final int SCALE = 8;

    private ScheduleAnalysisReportCalculator() {
    }

    /**
     * 按成型来源、当日结果、昨日结果和六点库存生成筛选及分页后的报表。
     *
     * @param query              查询条件，导出时可将 pageSize 设为0表示不分页
     * @param demands            通过当前施工完整性规则的逐条成型关联来源
     * @param currentResults     所选排程日期的当前有效结果
     * @param previousResults    所选排程日期前一日的当前有效结果
     * @param sixClockStocks     物料编码到六点净库存的映射
     * @param shiftHeaders       当前配置解析出的八班表头
     * @param machineSourceField 机台字段口径说明
     * @return 报表分页结果及全量匹配统计
     */
    public static ScheduleAnalysisReportVO calculate(ScheduleAnalysisReportQueryDTO query,
                                                     List<ScheduleAnalysisDemandInput> demands,
                                                     List<ScheduleAnalysisPlanInput> currentResults,
                                                     List<ScheduleAnalysisPlanInput> previousResults,
                                                     Map<String, BigDecimal> sixClockStocks,
                                                     List<ScheduleAnalysisShiftHeaderVO> shiftHeaders,
                                                     String machineSourceField) {
        return calculate(query, demands, currentResults, previousResults, sixClockStocks,
                shiftHeaders, machineSourceField, Collections.emptySet());
    }

    /**
     * 为定时滚动保留指定物料，即使没有需求或正计划，也按原报表公式生成库存递推行。
     *
     * @param query                查询条件
     * @param demands              原始需求，空数量按原报表口径参与零值计算
     * @param currentResults       当日有效结果
     * @param previousResults      前一日有效结果
     * @param sixClockStocks       六点净库存，缺失物料以零为递推基数
     * @param shiftHeaders         物理班次表头
     * @param machineSourceField   机台字段口径
     * @param requiredProductCodes 必须参与定时滚动的物料集合
     * @return 包含指定物料的报表结果
     */
    public static ScheduleAnalysisReportVO calculate(ScheduleAnalysisReportQueryDTO query,
                                                     List<ScheduleAnalysisDemandInput> demands,
                                                     List<ScheduleAnalysisPlanInput> currentResults,
                                                     List<ScheduleAnalysisPlanInput> previousResults,
                                                     Map<String, BigDecimal> sixClockStocks,
                                                     List<ScheduleAnalysisShiftHeaderVO> shiftHeaders,
                                                     String machineSourceField,
                                                     Set<String> requiredProductCodes) {
        List<ScheduleAnalysisDemandInput> demandRows = safeList(demands);
        List<ScheduleAnalysisPlanInput> todayRows = safeList(currentResults);
        List<ScheduleAnalysisPlanInput> yesterdayRows = safeList(previousResults);
        Map<String, List<ScheduleAnalysisDemandInput>> demandsByProduct = demandRows.stream()
                .filter(item -> StringUtils.isNotBlank(item.getProductCode()))
                .collect(Collectors.groupingBy(ScheduleAnalysisDemandInput::getProductCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<ScheduleAnalysisPlanInput>> todayByProduct = todayRows.stream()
                .filter(item -> StringUtils.isNotBlank(item.getProductCode()))
                .collect(Collectors.groupingBy(ScheduleAnalysisPlanInput::getProductCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<ScheduleAnalysisPlanInput>> yesterdayByProduct = yesterdayRows.stream()
                .filter(item -> StringUtils.isNotBlank(item.getProductCode()))
                .collect(Collectors.groupingBy(ScheduleAnalysisPlanInput::getProductCode,
                        LinkedHashMap::new, Collectors.toList()));

        Set<String> productCodes = new LinkedHashSet<>(demandsByProduct.keySet());
        if (requiredProductCodes != null) {
            requiredProductCodes.stream().filter(StringUtils::isNotBlank).forEach(productCodes::add);
        }
        todayByProduct.forEach((productCode, rows) -> {
            if (rows.stream().anyMatch(ScheduleAnalysisReportCalculator::hasPositivePlan)) {
                productCodes.add(productCode);
            }
        });

        List<String> matchedProductCodes = productCodes.stream()
                .filter(productCode -> matchesProductCode(productCode, query.getProductCode()))
                .filter(productCode -> matchesName(query.getProductName(),
                        demandsByProduct.getOrDefault(productCode, Collections.emptyList()),
                        todayByProduct.getOrDefault(productCode, Collections.emptyList())))
                .filter(productCode -> matchesEmbryo(query.getEmbryoCode(),
                        demandsByProduct.getOrDefault(productCode, Collections.emptyList())))
                .sorted()
                .collect(Collectors.toList());

        int pageNum = positiveOrDefault(query.getPageNum(), 1);
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        List<String> pageProductCodes = pageSize <= 0 ? matchedProductCodes
                : paginate(matchedProductCodes, pageNum, pageSize);
        List<ScheduleAnalysisReportRowVO> outputRows = pageProductCodes.stream()
                .flatMap(productCode -> buildProductRows(query, productCode,
                        demandsByProduct.getOrDefault(productCode, Collections.emptyList()),
                        todayByProduct.getOrDefault(productCode, Collections.emptyList()),
                        yesterdayByProduct.getOrDefault(productCode, Collections.emptyList()),
                        safeMap(sixClockStocks).get(productCode)).stream())
                .collect(Collectors.toList());

        ScheduleAnalysisReportVO report = new ScheduleAnalysisReportVO();
        report.setFactoryCode(query.getFactoryCode());
        report.setScheduleDate(query.getScheduleDate());
        report.setQueriedAt(new Date());
        report.setMachineSourceField(machineSourceField);
        report.setShiftHeaders(new ArrayList<>(safeList(shiftHeaders)));
        report.setTotalGroups(matchedProductCodes.size());
        report.setTotalRows(matchedProductCodes.stream()
                .mapToLong(productCode -> demandsByProduct.getOrDefault(productCode, Collections.emptyList()).size() + 2L)
                .sum());
        report.setPageNum(pageNum);
        report.setPageSize(pageSize);
        report.setRows(outputRows);
        return report;
    }

    /**
     * 判断一条结果是否贡献当前计划物料集合。
     *
     * @param input 排程结果来源
     * @return 任一CLASS1至CLASS6大于0时返回true
     */
    private static boolean hasPositivePlan(ScheduleAnalysisPlanInput input) {
        return value(input.getClass1PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || value(input.getClass2PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || value(input.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || value(input.getClass4PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || value(input.getClass5PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || value(input.getClass6PlanQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 为一个物料组生成需求、库存和计划三类报表行。
     *
     * @param query            查询条件
     * @param productCode      物料编码
     * @param demands          该物料的有效成型来源行
     * @param todayResults     该物料当日结果行
     * @param yesterdayResults 该物料昨日结果行
     * @param stock            六点净库存
     * @return 该物料完整报表行
     */
    private static List<ScheduleAnalysisReportRowVO> buildProductRows(
            ScheduleAnalysisReportQueryDTO query, String productCode,
            List<ScheduleAnalysisDemandInput> demands, List<ScheduleAnalysisPlanInput> todayResults,
            List<ScheduleAnalysisPlanInput> yesterdayResults, BigDecimal stock) {
        List<ScheduleAnalysisDemandInput> orderedDemands = demands.stream()
                .sorted(Comparator.comparing(ScheduleAnalysisDemandInput::getEmbryoCode,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ScheduleAnalysisDemandInput::getSourceRecordId,
                                Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(ScheduleAnalysisDemandInput::getSkuConstructionRefId,
                                Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(ScheduleAnalysisDemandInput::getConstructionInfoId,
                                Comparator.nullsFirst(Long::compareTo))
                        .thenComparingInt(ScheduleAnalysisDemandInput::getAssociationRowIndex))
                .collect(Collectors.toList());
        String productName = chooseProductName(orderedDemands, todayResults);
        List<ScheduleAnalysisReportRowVO> rows = new ArrayList<>();
        BigDecimal[] demandTotals = zeroArray(8);
        for (ScheduleAnalysisDemandInput demand : orderedDemands) {
            BigDecimal[] classValues = zeroArray(8);
            for (int classIndex = 0; classIndex < 8; classIndex++) {
                BigDecimal formingQty = demand.getFormingClassPlanQty() != null
                        && demand.getFormingClassPlanQty().size() > classIndex
                        ? demand.getFormingClassPlanQty().get(classIndex) : BigDecimal.ZERO;
                classValues[classIndex] = normalize(value(formingQty)
                        .multiply(value(demand.getUnitConsumptionMeters())));
                demandTotals[classIndex] = normalize(demandTotals[classIndex].add(classValues[classIndex]));
            }
            ScheduleAnalysisReportRowVO demandRow = createBaseRow(query, productCode, demand.getProductName(), 1);
            demandRow.setRowKey("D:" + key(demand.getSourceRecordId()) + ":"
                    + key(demand.getSkuConstructionRefId()) + ":" + key(demand.getConstructionInfoId())
                    + ":" + demand.getAssociationRowIndex());
            demandRow.setEmbryoCode(demand.getEmbryoCode());
            demandRow.setEmbryoDesc(demand.getEmbryoDesc());
            demandRow.setMachineCodes(normalizeMachineCodes(demand.getMachineCodes()));
            demandRow.setSourceRecordId(demand.getSourceRecordId());
            demandRow.setSkuConstructionRefId(demand.getSkuConstructionRefId());
            demandRow.setConstructionInfoId(demand.getConstructionInfoId());
            setClasses(demandRow, classValues);
            rows.add(demandRow);
        }

        BigDecimal[] planValues = buildPlanValues(todayResults, yesterdayResults);
        BigDecimal sixClockStock = normalize(value(stock));
        BigDecimal[] inventoryValues = buildInventoryValues(sixClockStock, demandTotals, planValues);
        ScheduleAnalysisReportRowVO stockRow = createBaseRow(query, productCode, productName, 2);
        stockRow.setRowKey("S:" + productCode);
        stockRow.setSixClockStockQty(sixClockStock);
        setClasses(stockRow, inventoryValues);
        rows.add(stockRow);

        Set<String> demandMachines = orderedDemands.stream()
                .map(ScheduleAnalysisDemandInput::getMachineCodes)
                .map(ScheduleAnalysisReportCalculator::splitMachineCodes)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));
        ScheduleAnalysisReportRowVO planRow = createBaseRow(query, productCode, productName, 3);
        planRow.setRowKey("P:" + productCode);
        planRow.setMachineCount(demandMachines.size());
        setClasses(planRow, planValues);
        rows.add(planRow);
        return rows;
    }

    /**
     * 汇总报表八班计划，其中第一班引用昨日CLASS3，第八班保持NULL。
     *
     * @param todayResults     当日有效排程结果
     * @param yesterdayResults 昨日有效排程结果
     * @return CLASS1至CLASS8的报表计划数组
     */
    private static BigDecimal[] buildPlanValues(List<ScheduleAnalysisPlanInput> todayResults,
                                                List<ScheduleAnalysisPlanInput> yesterdayResults) {
        BigDecimal[] planValues = zeroArray(8);
        planValues[0] = normalize(yesterdayResults.stream()
                .map(ScheduleAnalysisPlanInput::getClass3PlanQty).map(ScheduleAnalysisReportCalculator::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        for (ScheduleAnalysisPlanInput result : todayResults) {
            planValues[1] = planValues[1].add(value(result.getClass1PlanQty()));
            planValues[2] = planValues[2].add(value(result.getClass2PlanQty()));
            planValues[3] = planValues[3].add(value(result.getClass3PlanQty()));
            planValues[4] = planValues[4].add(value(result.getClass4PlanQty()));
            planValues[5] = planValues[5].add(value(result.getClass5PlanQty()));
            planValues[6] = planValues[6].add(value(result.getClass6PlanQty()));
        }
        for (int classIndex = 0; classIndex < 7; classIndex++) {
            planValues[classIndex] = normalize(planValues[classIndex]);
        }
        planValues[7] = null;
        return planValues;
    }

    /**
     * 按“上一列库存-本班需求+上一列计划”递推八班库存。
     *
     * @param sixClockStock 排程日前一日六点库存
     * @param demandTotals  按班次汇总的成型需求
     * @param planValues    八班报表计划
     * @return 八班库存数组
     */
    private static BigDecimal[] buildInventoryValues(BigDecimal sixClockStock,
                                                     BigDecimal[] demandTotals, BigDecimal[] planValues) {
        BigDecimal[] inventories = zeroArray(8);
        inventories[0] = normalize(sixClockStock.subtract(demandTotals[0]));
        for (int classIndex = 1; classIndex < 8; classIndex++) {
            inventories[classIndex] = normalize(inventories[classIndex - 1]
                    .subtract(demandTotals[classIndex]).add(planValues[classIndex - 1]));
        }
        return inventories;
    }

    /**
     * 选择库存及计划行的名称。
     *
     * @param demands 按稳定来源顺序排列的需求行
     * @param results 当日结果行
     * @return 首个非空来源名称，无来源名称时取ID最小结果名称
     */
    private static String chooseProductName(List<ScheduleAnalysisDemandInput> demands,
                                            List<ScheduleAnalysisPlanInput> results) {
        String demandName = demands.stream().map(ScheduleAnalysisDemandInput::getProductName)
                .filter(StringUtils::isNotBlank).findFirst().orElse(null);
        if (demandName != null) {
            return demandName;
        }
        return results.stream().sorted(Comparator.comparing(ScheduleAnalysisPlanInput::getResultId,
                        Comparator.nullsLast(Long::compareTo)))
                .map(ScheduleAnalysisPlanInput::getProductName).filter(StringUtils::isNotBlank)
                .findFirst().orElse(null);
    }

    /**
     * 创建通用报表行基础字段。
     *
     * @param query       查询条件
     * @param productCode 物料编码
     * @param productName 物料名称
     * @param dataType    数据类型
     * @return 已赋值的基础行
     */
    private static ScheduleAnalysisReportRowVO createBaseRow(ScheduleAnalysisReportQueryDTO query,
                                                             String productCode, String productName,
                                                             int dataType) {
        ScheduleAnalysisReportRowVO row = new ScheduleAnalysisReportRowVO();
        row.setFactoryCode(query.getFactoryCode());
        row.setScheduleDate(query.getScheduleDate().toString());
        row.setProductCode(productCode);
        row.setProductName(productName);
        row.setDataType(dataType);
        return row;
    }

    /**
     * 将数量数组写入八个班次字段。
     *
     * @param row    报表行
     * @param values CLASS1至CLASS8数量
     */
    private static void setClasses(ScheduleAnalysisReportRowVO row, BigDecimal[] values) {
        row.setClass1(values[0]);
        row.setClass2(values[1]);
        row.setClass3(values[2]);
        row.setClass4(values[3]);
        row.setClass5(values[4]);
        row.setClass6(values[5]);
        row.setClass7(values[6]);
        row.setClass8(values[7]);
    }

    /**
     * 按物料编码包含条件筛选物料组。
     *
     * @param productCode 物料编码
     * @param keyword     编码包含条件
     * @return 条件为空或匹配时返回true
     */
    private static boolean matchesProductCode(String productCode, String keyword) {
        return StringUtils.isBlank(keyword) || containsIgnoreCase(productCode, keyword);
    }

    /**
     * 按任一需求名称或结果名称匹配物料组。
     *
     * @param productCode 物料编码
     * @param keyword     名称包含条件
     * @param demands     物料组需求行
     * @param results     物料组排程结果行
     * @return 条件为空或任一名称匹配时返回true
     */
    private static boolean matchesName(String keyword,
                                       List<ScheduleAnalysisDemandInput> demands,
                                       List<ScheduleAnalysisPlanInput> results) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        return demands.stream().map(ScheduleAnalysisDemandInput::getProductName)
                .anyMatch(name -> containsIgnoreCase(name, keyword))
                || results.stream().map(ScheduleAnalysisPlanInput::getProductName)
                .anyMatch(name -> containsIgnoreCase(name, keyword));
    }

    /**
     * 按有效成型来源的胎胚代码或胎胚描述筛选物料组。
     *
     * @param keyword 胎胚代码或胎胚描述包含条件
     * @param demands 物料组有效需求行
     * @return 条件为空或至少一个胎胚匹配时返回true
     */
    private static boolean matchesEmbryo(String keyword, List<ScheduleAnalysisDemandInput> demands) {
        return StringUtils.isBlank(keyword) || demands.stream()
                .anyMatch(demand -> containsIgnoreCase(demand.getEmbryoCode(), keyword)
                        || containsIgnoreCase(demand.getEmbryoDesc(), keyword));
    }

    /**
     * 执行不含SQL通配符语义的字面包含匹配。
     *
     * @param value   被匹配文本
     * @param keyword 查询文本
     * @return 忽略大小写的字面包含结果
     */
    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 归一化逗号分隔机台代码。
     *
     * @param codes 原始机台代码
     * @return 去空、去重、升序后的机台串
     */
    public static String normalizeMachineCodes(String codes) {
        return splitMachineCodes(codes).stream().collect(Collectors.joining(","));
    }

    /**
     * 将逗号分隔机台代码解析为去重集合。
     *
     * @param codes 原始机台代码
     * @return 机台代码有序集合
     */
    private static Set<String> splitMachineCodes(String codes) {
        if (StringUtils.isBlank(codes)) {
            return Collections.emptySet();
        }
        return Arrays.stream(codes.split(","))
                .map(String::trim).filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * 按物料组分页。
     *
     * @param productCodes 已排序的完整物料编码
     * @param pageNum      当前页码
     * @param pageSize     每页物料组数
     * @return 当前页物料编码
     */
    private static List<String> paginate(List<String> productCodes, int pageNum, int pageSize) {
        long start = (long) (pageNum - 1) * pageSize;
        if (start >= productCodes.size()) {
            return Collections.emptyList();
        }
        int fromIndex = (int) start;
        int toIndex = (int) Math.min((long) productCodes.size(), start + pageSize);
        return productCodes.subList(fromIndex, toIndex);
    }

    /**
     * 创建固定长度的零值数组。
     *
     * @param length 数组长度
     * @return 已设为八位小数的零值数组
     */
    private static BigDecimal[] zeroArray(int length) {
        BigDecimal[] values = new BigDecimal[length];
        Arrays.fill(values, BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        return values;
    }

    /**
     * 将空值按0处理并统一精度。
     *
     * @param value 原始数量
     * @return 八位小数的数值
     */
    private static BigDecimal normalize(BigDecimal value) {
        return value(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 将空数量转换为零。
     *
     * @param value 原始数量
     * @return 非空数量
     */
    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 读取可选列表的安全值。
     *
     * @param values 原始列表
     * @param <T>    列表元素类型
     * @return 非空列表
     */
    private static <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * 读取库存映射的安全值。
     *
     * @param values 原始库存映射
     * @return 非空映射
     */
    private static Map<String, BigDecimal> safeMap(Map<String, BigDecimal> values) {
        return values == null ? Collections.emptyMap() : values;
    }

    /**
     * 读取正整数参数，无效时使用缺省值。
     *
     * @param value        原始数值
     * @param defaultValue 缺省值
     * @return 有效正整数
     */
    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    /**
     * 将可空ID转换为行键文本。
     *
     * @param value ID
     * @return ID文本，无值时为-号
     */
    private static String key(Long value) {
        return value == null ? "-" : value.toString();
    }
}
