package com.zlt.aps.cd15.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 斜裁排程结果模板导出数据组装器。
 * 只负责内存数据合并和计算，不访问数据库、不修改业务实体。
 */
@Component
public class Cd15ScheduleResultExportAssembler {

    /** 模板中参与明细行保留判断的四班计划量和完成量字段。 */
    private static final List<String> DISPLAY_QUANTITY_FIELDS = Arrays.asList(
            "previousClass3PlanQty", "previousClass3FinishQty",
            "class1PlanQty", "class1FinishQty",
            "class2PlanQty", "class2FinishQty",
            "class3PlanQty", "class3FinishQty"
    );

    /** 成型排程 CLASS1 至 CLASS4 的计划量读取器。 */
    private static final List<Function<CxScheduleResult, BigDecimal>> FORMING_PLAN_READERS =
            Arrays.asList(
                    CxScheduleResult::getClass1PlanQty,
                    CxScheduleResult::getClass2PlanQty,
                    CxScheduleResult::getClass3PlanQty,
                    CxScheduleResult::getClass4PlanQty
            );

    /** 成型排程 CLASS1 至 CLASS4 的配方版本读取器。 */
    private static final List<Function<CxScheduleResult, String>> FORMING_RECIPE_READERS =
            Arrays.asList(
                    CxScheduleResult::getClass1RecipeNo,
                    CxScheduleResult::getClass2RecipeNo,
                    CxScheduleResult::getClass3RecipeNo,
                    CxScheduleResult::getClass4RecipeNo
            );

    /**
     * 组装模板明细行。
     *
     * @param previousResults 上一批排程结果，仅使用 CLASS3
     * @param currentResults 本批排程结果，使用 CLASS1 至 CLASS3
     * @param stocks 排程日期前一日库存
     * @param formingResults 本批排程日成型排程结果
     * @param constructions 成型排程引用的施工版本
     * @return 模板明细行
     */
    public List<Map<String, Object>> assembleRows(
            List<Cd15ScheduleResult> previousResults,
            List<Cd15ScheduleResult> currentResults,
            List<Cd15Stock> stocks,
            List<CxScheduleResult> formingResults,
            List<MdmConstructionInfo> constructions) {
        LinkedHashMap<RowKey, RowState> rowStates = new LinkedHashMap<>();

        this.safeList(previousResults).forEach(result -> {
            RowState rowState = rowStates.computeIfAbsent(
                    RowKey.from(result), RowState::new);
            this.mergePreviousResult(rowState, result);
        });
        this.safeList(currentResults).forEach(result -> {
            RowState rowState = rowStates.computeIfAbsent(
                    RowKey.from(result), RowState::new);
            this.mergeCurrentResult(rowState, result);
        });

        Map<String, BigDecimal> stockBySteelStrip = this.aggregateStocks(stocks);
        Map<ConstructionKey, List<MdmConstructionInfo>> constructionsByKey =
                this.indexConstructions(constructions);
        Map<String, String> structureNameBySteelStrip =
                this.aggregateStructureNames(constructions);

        return rowStates.values().stream()
                .filter(this::hasDisplayedQuantity)
                .map(rowState -> this.completeRow(
                        rowState,
                        stockBySteelStrip,
                        formingResults,
                        constructionsByKey,
                        structureNameBySteelStrip))
                .collect(Collectors.toList());
    }

    /**
     * 构建模板表头占位符。
     *
     * @param scheduleDate 本批排程日期
     * @return 表头占位符
     */
    public Map<String, Object> buildTableMap(Date scheduleDate) {
        Date previousDate = DateUtil.offsetDay(scheduleDate, -1);
        String previousDateText = DateUtil.format(previousDate, "MM/dd");
        String scheduleDateText = DateUtil.format(scheduleDate, "MM/dd");
        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("planDate", DateUtil.format(scheduleDate, "yyyy年MM月dd日"));
        tableMap.put("previousDate1", previousDateText);
        tableMap.put("previousDate2", previousDateText);
        tableMap.put("scheduleDate1", scheduleDateText);
        tableMap.put("scheduleDate2", scheduleDateText);
        return tableMap;
    }

    /** 合并上一批 CLASS3 计划量和完成量。 */
    private void mergePreviousResult(RowState rowState, Cd15ScheduleResult result) {
        this.mergePreviousBaseFields(rowState, result);
        this.addQuantity(rowState.values, "previousClass3PlanQty",
                this.readQuantity(result, 3, "PlanQty"));
        this.addQuantity(rowState.values, "previousClass3FinishQty",
                this.readQuantity(result, 3, "FinishQty"));
    }

    /** 合并本批 CLASS1 至 CLASS3 计划量和完成量。 */
    private void mergeCurrentResult(RowState rowState, Cd15ScheduleResult result) {
        this.mergeCurrentBaseFields(rowState, result);
        for (int classIndex = 1; classIndex <= 3; classIndex++) {
            this.addQuantity(rowState.values,
                    String.format("class%dPlanQty", classIndex),
                    this.readQuantity(result, classIndex, "PlanQty"));
            this.addQuantity(rowState.values,
                    String.format("class%dFinishQty", classIndex),
                    this.readQuantity(result, classIndex, "FinishQty"));
        }
    }

    /** 使用上一批结果补齐公共字段，不覆盖已有值。 */
    private void mergePreviousBaseFields(
            RowState rowState, Cd15ScheduleResult result) {
        this.putPreviousBase(rowState, "machineCode", result.getMachineCode());
        this.putPreviousBase(rowState, "unitConsume",
                this.toMeters(result.getUnitConsumeMillimeter()));
        this.putPreviousBase(rowState, "planSurplusQty",
                result.getPlanSurplusQty());
        this.putPreviousBase(rowState, "steelStripCode",
                result.getSteelStripCode());
        this.putPreviousBase(rowState, "cuttingAngle",
                result.getCuttingAngle());
        this.putPreviousBase(rowState, "bigRollCode", result.getBigRollCode());
        this.putPreviousBase(rowState, "storageLaneCode",
                result.getStorageLaneCode());
        this.putPreviousBase(rowState, "cxMachineCodes",
                result.getCxMachineCodes());
    }

    /** 使用本批首条有效结果写入公共字段，优先于上一批字段。 */
    private void mergeCurrentBaseFields(
            RowState rowState, Cd15ScheduleResult result) {
        this.putCurrentBase(rowState, "machineCode", result.getMachineCode());
        this.putCurrentBase(rowState, "unitConsume",
                this.toMeters(result.getUnitConsumeMillimeter()));
        this.putCurrentBase(rowState, "planSurplusQty",
                result.getPlanSurplusQty());
        this.putCurrentBase(rowState, "steelStripCode",
                result.getSteelStripCode());
        this.putCurrentBase(rowState, "cuttingAngle",
                result.getCuttingAngle());
        this.putCurrentBase(rowState, "bigRollCode", result.getBigRollCode());
        this.putCurrentBase(rowState, "storageLaneCode",
                result.getStorageLaneCode());
        this.putCurrentBase(rowState, "cxMachineCodes",
                result.getCxMachineCodes());
    }

    /** 仅在行字段为空时写入上一批公共字段。 */
    private void putPreviousBase(
            RowState rowState, String fieldName, Object value) {
        if (value != null && rowState.values.get(fieldName) == null) {
            rowState.values.put(fieldName, value);
        }
    }

    /** 每个本批公共字段只写入首个非空值。 */
    private void putCurrentBase(
            RowState rowState, String fieldName, Object value) {
        if (value != null && rowState.currentBaseFields.add(fieldName)) {
            rowState.values.put(fieldName, value);
        }
    }

    /** 累加同一模板行的计划量或完成量。 */
    private void addQuantity(
            Map<String, Object> values, String fieldName, BigDecimal quantity) {
        if (quantity == null) {
            return;
        }
        BigDecimal current = (BigDecimal) values.get(fieldName);
        values.put(fieldName,
                current == null ? quantity : current.add(quantity));
    }

    /** 通过动态字段名读取斜裁排程结果数量。 */
    private BigDecimal readQuantity(
            Cd15ScheduleResult result, int classIndex, String suffix) {
        Object value = result.getFieldValueByFieldName(
                String.format("class%d%s", classIndex, suffix));
        return value == null ? null : BigDecimalUtils.valueOf(value);
    }

    /** 单耗由毫米/条转换为米/条。 */
    private BigDecimal toMeters(BigDecimal unitConsumeMillimeter) {
        if (unitConsumeMillimeter == null) {
            return null;
        }
        return unitConsumeMillimeter.divide(new BigDecimal("1000"));
    }

    /** 按钢带编码汇总可用库存。 */
    private Map<String, BigDecimal> aggregateStocks(List<Cd15Stock> stocks) {
        Map<String, BigDecimal> result = new HashMap<>();
        this.safeList(stocks).stream()
                .filter(stock -> stock.getMaterialCode() != null)
                .forEach(stock -> result.merge(
                        stock.getMaterialCode(),
                        this.availableStock(stock),
                        BigDecimal::add));
        return result;
    }

    /** 可用库存等于库存量加修正量减不良量。 */
    private BigDecimal availableStock(Cd15Stock stock) {
        return BigDecimalUtils.valueOf(stock.getStockNum())
                .add(BigDecimalUtils.valueOf(stock.getModifyNum()))
                .subtract(BigDecimalUtils.valueOf(stock.getBadNum()));
    }

    /** 按施工编码和施工版本建立施工信息索引。 */
    private Map<ConstructionKey, List<MdmConstructionInfo>> indexConstructions(
            List<MdmConstructionInfo> constructions) {
        return this.safeList(constructions).stream()
                .filter(construction ->
                        construction.getConstructionCode() != null)
                .filter(construction ->
                        construction.getConstructionVersion() != null)
                .collect(Collectors.groupingBy(
                        ConstructionKey::from,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /** 汇总钢带对应的施工物料名称。 */
    private Map<String, String> aggregateStructureNames(
            List<MdmConstructionInfo> constructions) {
        Map<String, Set<String>> namesBySteelStrip = new LinkedHashMap<>();
        this.safeList(constructions).forEach(construction -> {
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCode1(), construction.getBeltName1());
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCode2(), construction.getBeltName2());
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCode3(), construction.getBeltName3());
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCode4(), construction.getBeltName4());
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCodeLeftCode(),
                    construction.getBeltCodeLeftName());
            this.addStructureName(namesBySteelStrip,
                    construction.getBeltCodeRightCode(),
                    construction.getBeltCodeRightName());
        });
        return namesBySteelStrip.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue()),
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    /** 添加钢带对应的施工物料名称并去重。 */
    private void addStructureName(
            Map<String, Set<String>> namesBySteelStrip,
            String steelStripCode,
            String structureName) {
        if (steelStripCode == null || structureName == null
                || structureName.trim().isEmpty()) {
            return;
        }
        namesBySteelStrip
                .computeIfAbsent(
                        steelStripCode, ignored -> new LinkedHashSet<>())
                .add(structureName.trim());
    }

    /** 判断模板行是否包含至少一个计划量或完成量。 */
    private boolean hasDisplayedQuantity(RowState rowState) {
        return DISPLAY_QUANTITY_FIELDS.stream()
                .anyMatch(fieldName ->
                        rowState.values.get(fieldName) != null);
    }

    /** 补全库存、成型产量、四班计划量和施工物料名称。 */
    private Map<String, Object> completeRow(
            RowState rowState,
            Map<String, BigDecimal> stockBySteelStrip,
            List<CxScheduleResult> formingResults,
            Map<ConstructionKey, List<MdmConstructionInfo>>
                    constructionsByKey,
            Map<String, String> structureNameBySteelStrip) {
        String steelStripCode =
                (String) rowState.values.get("steelStripCode");
        rowState.values.put("stockQty",
                stockBySteelStrip.get(steelStripCode));
        rowState.values.put("formingPlanQty", this.formingPlanQty(
                steelStripCode, formingResults, constructionsByKey));
        rowState.values.put("fourShiftPlanQty",
                this.sumDisplayedPlans(rowState.values));
        rowState.values.put("structureName",
                structureNameBySteelStrip.get(steelStripCode));
        return rowState.values;
    }

    /** 按施工编码和配方版本计算钢带对应的成型计划量。 */
    private BigDecimal formingPlanQty(
            String steelStripCode,
            List<CxScheduleResult> formingResults,
            Map<ConstructionKey, List<MdmConstructionInfo>>
                    constructionsByKey) {
        BigDecimal total = null;
        for (CxScheduleResult formingResult :
                this.safeList(formingResults)) {
            for (int classIndex = 0;
                 classIndex < FORMING_PLAN_READERS.size();
                 classIndex++) {
                BigDecimal planQty =
                        FORMING_PLAN_READERS.get(classIndex)
                                .apply(formingResult);
                String recipeNo =
                        FORMING_RECIPE_READERS.get(classIndex)
                                .apply(formingResult);
                if (planQty == null
                        || formingResult.getEmbryoCode() == null
                        || recipeNo == null) {
                    continue;
                }
                List<MdmConstructionInfo> matches =
                        constructionsByKey.getOrDefault(
                                new ConstructionKey(
                                        formingResult.getEmbryoCode(),
                                        recipeNo),
                                Collections.emptyList());
                if (matches.stream().anyMatch(construction ->
                        this.containsSteelStrip(
                                construction, steelStripCode))) {
                    total = total == null ? planQty : total.add(planQty);
                }
            }
        }
        return total;
    }

    /** 判断施工信息是否包含指定钢带编码。 */
    private boolean containsSteelStrip(
            MdmConstructionInfo construction, String steelStripCode) {
        return steelStripCode != null
                && (steelStripCode.equals(construction.getBeltCode1())
                || steelStripCode.equals(construction.getBeltCode2())
                || steelStripCode.equals(construction.getBeltCode3())
                || steelStripCode.equals(construction.getBeltCode4())
                || steelStripCode.equals(
                        construction.getBeltCodeLeftCode())
                || steelStripCode.equals(
                        construction.getBeltCodeRightCode()));
    }

    /** 汇总上一批 CLASS3 和本批 CLASS1 至 CLASS3 的计划量。 */
    private BigDecimal sumDisplayedPlans(Map<String, Object> values) {
        List<String> planFields = Arrays.asList(
                "previousClass3PlanQty",
                "class1PlanQty",
                "class2PlanQty",
                "class3PlanQty");
        BigDecimal total = null;
        for (String fieldName : planFields) {
            BigDecimal quantity = (BigDecimal) values.get(fieldName);
            if (quantity != null) {
                total = total == null ? quantity : total.add(quantity);
            }
        }
        return total;
    }

    /** 将空集合统一转换为空列表。 */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 模板单行合并状态。 */
    private static final class RowState {

        /** 模板字段值。 */
        private final Map<String, Object> values = new LinkedHashMap<>();

        /** 已由本批结果写入的公共字段。 */
        private final Set<String> currentBaseFields = new LinkedHashSet<>();

        /** 根据明细行唯一键初始化公共维度。 */
        private RowState(RowKey rowKey) {
            this.values.put("machineCode", rowKey.machineCode);
            this.values.put("steelStripCode", rowKey.steelStripCode);
            this.values.put("bigRollCode", rowKey.bigRollCode);
            this.values.put("cuttingAngle", rowKey.cuttingAngle);
        }
    }

    /** 机台、钢带、大卷和角度组成的模板明细行唯一键。 */
    private static final class RowKey {

        /** 机台编码。 */
        private final String machineCode;

        /** 钢带编码。 */
        private final String steelStripCode;

        /** 大卷编码。 */
        private final String bigRollCode;

        /** 裁断角度。 */
        private final String cuttingAngle;

        /** 构造模板明细行唯一键。 */
        private RowKey(
                String machineCode,
                String steelStripCode,
                String bigRollCode,
                String cuttingAngle) {
            this.machineCode = machineCode;
            this.steelStripCode = steelStripCode;
            this.bigRollCode = bigRollCode;
            this.cuttingAngle = cuttingAngle;
        }

        /** 从排程结果构建模板明细行唯一键。 */
        private static RowKey from(Cd15ScheduleResult result) {
            return new RowKey(
                    result.getMachineCode(),
                    result.getSteelStripCode(),
                    result.getBigRollCode(),
                    result.getCuttingAngle());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof RowKey)) {
                return false;
            }
            RowKey rowKey = (RowKey) object;
            return Objects.equals(this.machineCode, rowKey.machineCode)
                    && Objects.equals(
                            this.steelStripCode, rowKey.steelStripCode)
                    && Objects.equals(this.bigRollCode, rowKey.bigRollCode)
                    && Objects.equals(
                            this.cuttingAngle, rowKey.cuttingAngle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    this.machineCode,
                    this.steelStripCode,
                    this.bigRollCode,
                    this.cuttingAngle);
        }
    }

    /** 施工编码和配方版本组成的施工唯一键。 */
    private static final class ConstructionKey {

        /** 施工编码。 */
        private final String constructionCode;

        /** 施工版本。 */
        private final String constructionVersion;

        /** 构造施工唯一键。 */
        private ConstructionKey(
                String constructionCode, String constructionVersion) {
            this.constructionCode = constructionCode;
            this.constructionVersion = constructionVersion;
        }

        /** 从施工信息构建唯一键。 */
        private static ConstructionKey from(
                MdmConstructionInfo construction) {
            return new ConstructionKey(
                    construction.getConstructionCode(),
                    construction.getConstructionVersion());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ConstructionKey)) {
                return false;
            }
            ConstructionKey that = (ConstructionKey) object;
            return Objects.equals(
                    this.constructionCode, that.constructionCode)
                    && Objects.equals(
                            this.constructionVersion,
                            that.constructionVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    this.constructionCode, this.constructionVersion);
        }
    }
}
