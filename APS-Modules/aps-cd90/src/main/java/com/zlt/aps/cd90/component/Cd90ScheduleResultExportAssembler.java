package com.zlt.aps.cd90.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
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
 * 直裁排程结果模板导出数据组装器。
 * 只负责内存数据合并和计算，不访问数据库、不修改业务实体。
 */
@Component
public class Cd90ScheduleResultExportAssembler {

    /** 模板中需要参与展示判断的8个数量字段名（4班次 × 计划/完成）。 */
    private static final List<String> DISPLAY_QUANTITY_FIELDS = Arrays.asList(
            "previousClass3PlanQty", "previousClass3FinishQty",
            "class1PlanQty", "class1FinishQty",
            "class2PlanQty", "class2FinishQty",
            "class3PlanQty", "class3FinishQty"
    );

    /** 成型排程各班的计划量读取器（CLASS1~4）。 */
    private static final List<Function<CxScheduleResult, BigDecimal>> FORMING_PLAN_READERS = Arrays.asList(
            CxScheduleResult::getClass1PlanQty,
            CxScheduleResult::getClass2PlanQty,
            CxScheduleResult::getClass3PlanQty,
            CxScheduleResult::getClass4PlanQty
    );

    /** 成型排程各班的配方版本号读取器（CLASS1~4）。 */
    private static final List<Function<CxScheduleResult, String>> FORMING_RECIPE_READERS = Arrays.asList(
            CxScheduleResult::getClass1RecipeNo,
            CxScheduleResult::getClass2RecipeNo,
            CxScheduleResult::getClass3RecipeNo,
            CxScheduleResult::getClass4RecipeNo
    );

    /**
     * 组装模板明细行。
     *
     * @param previousResults 上一批排程结果，仅使用CLASS3
     * @param currentResults 本批排程结果，使用CLASS1至CLASS3
     * @param stocks D日早班库存快照
     * @param formingResults 同本批排程日的成型排程结果
     * @param constructions 成型排程所引用的施工版本
     * @return 模板明细行
     */
    public List<Map<String, Object>> assembleRows(List<Cd90ScheduleResult> previousResults,
                                                   List<Cd90ScheduleResult> currentResults,
                                                   List<Cd90Stock> stocks,
                                                   List<CxScheduleResult> formingResults,
                                                   List<MdmConstructionInfo> constructions) {
        LinkedHashMap<RowKey, RowState> rowStates = new LinkedHashMap<>();

        this.safeList(previousResults).forEach(result -> {
            RowState rowState = rowStates.computeIfAbsent(RowKey.from(result), RowState::new);
            this.mergePreviousResult(rowState, result);
        });
        this.safeList(currentResults).forEach(result -> {
            RowState rowState = rowStates.computeIfAbsent(RowKey.from(result), RowState::new);
            this.mergeCurrentResult(rowState, result);
        });

        Map<String, BigDecimal> stockByCloth = this.aggregateStocks(stocks);
        Map<ConstructionKey, List<MdmConstructionInfo>> constructionsByKey = this.indexConstructions(constructions);
        Map<String, String> structureNameByCloth = this.aggregateStructureNames(constructions);

        return rowStates.values().stream()
                .filter(this::hasDisplayedQuantity)
                .map(rowState -> this.completeRow(
                        rowState, stockByCloth, formingResults, constructionsByKey, structureNameByCloth))
                .collect(Collectors.toList());
    }

    /**
     * 构建模板普通占位符。
     *
     * @param scheduleDate 本批排程日期
     * @return 可供ExcelUtils修改的表头Map
     */
    public Map<String, Object> buildTableMap(Date scheduleDate) {
        Date previousDate = DateUtil.offsetDay(scheduleDate, -1);
        Map<String, Object> tableMap = new HashMap<>();
        String previousDateText = DateUtil.format(previousDate, "MM/dd");
        String scheduleDateText = DateUtil.format(scheduleDate, "MM/dd");
        tableMap.put("planDate", DateUtil.format(scheduleDate, "yyyy年MM月dd日"));
        tableMap.put("previousDate1", previousDateText);
        tableMap.put("previousDate2", previousDateText);
        tableMap.put("scheduleDate1", scheduleDateText);
        tableMap.put("scheduleDate2", scheduleDateText);
        return tableMap;
    }

    /** 合并上一批排程结果的 CLASS3 计划/完成量到行状态。 */
    private void mergePreviousResult(RowState rowState, Cd90ScheduleResult result) {
        this.mergePreviousBaseFields(rowState, result);
        this.addQuantity(rowState.values, "previousClass3PlanQty",
                this.readCd90Quantity(result, 3, "PlanQty"));
        this.addQuantity(rowState.values, "previousClass3FinishQty",
                this.readCd90Quantity(result, 3, "FinishQty"));
    }

    /** 合并本批排程结果的 CLASS1~CLASS3 计划/完成量到行状态。 */
    private void mergeCurrentResult(RowState rowState, Cd90ScheduleResult result) {
        this.mergeCurrentBaseFields(rowState, result);
        for (int classIndex = 1; classIndex <= 3; classIndex++) {
            this.addQuantity(rowState.values, String.format("class%dPlanQty", classIndex),
                    this.readCd90Quantity(result, classIndex, "PlanQty"));
            this.addQuantity(rowState.values, String.format("class%dFinishQty", classIndex),
                    this.readCd90Quantity(result, classIndex, "FinishQty"));
        }
    }

    /** 合并上一批排程结果的公共基础字段（仅空位填充，不覆盖已有值）。 */
    private void mergePreviousBaseFields(RowState rowState, Cd90ScheduleResult result) {
        this.putPreviousBase(rowState, "machineCode", result.getMachineCode());
        this.putPreviousBase(rowState, "unitConsume", this.toMeters(result.getUnitConsume()));
        this.putPreviousBase(rowState, "planSurplusQty", result.getPlanSurplusQty());
        this.putPreviousBase(rowState, "clothCode", result.getClothCode());
        this.putPreviousBase(rowState, "bigRollCode", result.getBigRollCode());
        this.putPreviousBase(rowState, "storageLaneCode", result.getStorageLaneCode());
        this.putPreviousBase(rowState, "cxMachineCodes", result.getCxMachineCodes());
    }

    /** 合并本批排程结果的公共基础字段（首次写入后锁定，后续批次不再覆盖）。 */
    private void mergeCurrentBaseFields(RowState rowState, Cd90ScheduleResult result) {
        this.putCurrentBase(rowState, "machineCode", result.getMachineCode());
        this.putCurrentBase(rowState, "unitConsume", this.toMeters(result.getUnitConsume()));
        this.putCurrentBase(rowState, "planSurplusQty", result.getPlanSurplusQty());
        this.putCurrentBase(rowState, "clothCode", result.getClothCode());
        this.putCurrentBase(rowState, "bigRollCode", result.getBigRollCode());
        this.putCurrentBase(rowState, "storageLaneCode", result.getStorageLaneCode());
        this.putCurrentBase(rowState, "cxMachineCodes", result.getCxMachineCodes());
    }

    /** 以"空位填充"策略写入上一批基础字段（已有值不覆盖）。 */
    private void putPreviousBase(RowState rowState, String fieldName, Object value) {
        if (value != null && rowState.values.get(fieldName) == null) {
            rowState.values.put(fieldName, value);
        }
    }

    /** 以"首次写入锁定"策略写入本批基础字段（每个字段仅写一次）。 */
    private void putCurrentBase(RowState rowState, String fieldName, Object value) {
        if (value != null && rowState.currentBaseFields.add(fieldName)) {
            rowState.values.put(fieldName, value);
        }
    }

    /** 累加数量到指定字段（空值跳过，已存在则求和）。 */
    private void addQuantity(Map<String, Object> values, String fieldName, BigDecimal quantity) {
        if (quantity == null) {
            return;
        }
        BigDecimal current = (BigDecimal) values.get(fieldName);
        values.put(fieldName, current == null ? quantity : current.add(quantity));
    }

    /** 通过动态字段名读取直裁排程结果的某班次数量字段。 */
    private BigDecimal readCd90Quantity(Cd90ScheduleResult result, int classIndex, String suffix) {
        Object value = result.getFieldValueByFieldName(String.format("class%d%s", classIndex, suffix));
        return value == null ? null : BigDecimalUtils.valueOf(value);
    }

    /** 单耗从毫米转换为米（除以1000）。 */
    private BigDecimal toMeters(Double unitConsume) {
        if (unitConsume == null) {
            return null;
        }
        return BigDecimalUtils.valueOf(unitConsume).divide(new BigDecimal("1000"));
    }

    /** 按物料编码汇总可用库存（库存量 + 修改量 - 不良数量）。 */
    private Map<String, BigDecimal> aggregateStocks(List<Cd90Stock> stocks) {
        Map<String, BigDecimal> result = new HashMap<>();
        this.safeList(stocks).stream()
                .filter(stock -> stock.getMaterialCode() != null)
                .forEach(stock -> result.merge(
                        stock.getMaterialCode(), this.availableStock(stock), BigDecimal::add));
        return result;
    }

    /** 计算单个库存记录的可用量：库存量 + 修改量 - 不良数。 */
    private BigDecimal availableStock(Cd90Stock stock) {
        return BigDecimalUtils.valueOf(stock.getStockNum())
                .add(BigDecimalUtils.valueOf(stock.getModifyNum()))
                .subtract(BigDecimalUtils.valueOf(stock.getBadNum()));
    }

    /** 按施工编码+版本号对施工主数据建立索引，用于成型计划的帘布匹配。 */
    private Map<ConstructionKey, List<MdmConstructionInfo>> indexConstructions(
            List<MdmConstructionInfo> constructions) {
        return this.safeList(constructions).stream()
                .filter(construction -> construction.getConstructionCode() != null)
                .filter(construction -> construction.getConstructionVersion() != null)
                .collect(Collectors.groupingBy(
                        ConstructionKey::from,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /** 汇总帘布对应的骨架材料名称（多值逗号拼接）。 */
    private Map<String, String> aggregateStructureNames(List<MdmConstructionInfo> constructions) {
        Map<String, Set<String>> namesByCloth = new LinkedHashMap<>();
        this.safeList(constructions).forEach(construction -> {
            this.addStructureName(namesByCloth,
                    construction.getTireFabricCode1(), construction.getTireFabricName1());
            this.addStructureName(namesByCloth,
                    construction.getTireFabricCode2(), construction.getTireFabricName2());
            this.addStructureName(namesByCloth,
                    construction.getTireFabricCode3(), construction.getTireFabricName3());
        });
        return namesByCloth.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> String.join(",", entry.getValue()),
                (first, second) -> first,
                LinkedHashMap::new));
    }

    /** 添加帘布的骨架材料名称到映射（自动去重）。 */
    private void addStructureName(Map<String, Set<String>> namesByCloth,
                                   String clothCode,
                                   String structureName) {
        if (clothCode == null || structureName == null || structureName.trim().isEmpty()) {
            return;
        }
        namesByCloth.computeIfAbsent(clothCode, ignored -> new LinkedHashSet<>())
                .add(structureName.trim());
    }

    /** 判断行是否存在至少一个有值的展示数量字段，用于过滤空行。 */
    private boolean hasDisplayedQuantity(RowState rowState) {
        return DISPLAY_QUANTITY_FIELDS.stream().anyMatch(field -> rowState.values.get(field) != null);
    }

    /** 补全单行：库存量、成型计划量、四班计划量、骨架材料名称。 */
    private Map<String, Object> completeRow(RowState rowState,
                                            Map<String, BigDecimal> stockByCloth,
                                            List<CxScheduleResult> formingResults,
                                            Map<ConstructionKey, List<MdmConstructionInfo>> constructionsByKey,
                                            Map<String, String> structureNameByCloth) {
        String clothCode = (String) rowState.values.get("clothCode");
        rowState.values.put("stockQty", stockByCloth.get(clothCode));
        rowState.values.put("formingPlanQty", this.formingPlanQty(
                clothCode, formingResults, constructionsByKey));
        rowState.values.put("fourShiftPlanQty", this.sumDisplayedPlans(rowState.values));
        rowState.values.put("structureName", structureNameByCloth.get(clothCode));
        return rowState.values;
    }

    /** 计算指定帘布对应的成型计划量总和（按施工编码+配方版本匹配）。 */
    private BigDecimal formingPlanQty(String clothCode,
                                       List<CxScheduleResult> formingResults,
                                       Map<ConstructionKey, List<MdmConstructionInfo>> constructionsByKey) {
        BigDecimal total = null;
        for (CxScheduleResult formingResult : this.safeList(formingResults)) {
            for (int classIndex = 0; classIndex < FORMING_PLAN_READERS.size(); classIndex++) {
                BigDecimal planQty = FORMING_PLAN_READERS.get(classIndex).apply(formingResult);
                String recipeNo = FORMING_RECIPE_READERS.get(classIndex).apply(formingResult);
                if (planQty == null || formingResult.getEmbryoCode() == null || recipeNo == null) {
                    continue;
                }
                List<MdmConstructionInfo> matches = constructionsByKey.getOrDefault(
                        new ConstructionKey(formingResult.getEmbryoCode(), recipeNo), Collections.emptyList());
                if (matches.stream().anyMatch(construction -> this.containsCloth(construction, clothCode))) {
                    total = total == null ? planQty : total.add(planQty);
                }
            }
        }
        return total;
    }

    /** 判断施工主数据中是否包含指定帘布编码（检查 tireFabricCode1~3）。 */
    private boolean containsCloth(MdmConstructionInfo construction, String clothCode) {
        return clothCode != null && (clothCode.equals(construction.getTireFabricCode1())
                || clothCode.equals(construction.getTireFabricCode2())
                || clothCode.equals(construction.getTireFabricCode3()));
    }

    /** 汇总四个班次的计划量：前一CLASS3 + CLASS1~CLASS3。 */
    private BigDecimal sumDisplayedPlans(Map<String, Object> values) {
        List<String> planFields = Arrays.asList(
                "previousClass3PlanQty", "class1PlanQty", "class2PlanQty", "class3PlanQty");
        BigDecimal total = null;
        for (String field : planFields) {
            BigDecimal quantity = (BigDecimal) values.get(field);
            if (quantity != null) {
                total = total == null ? quantity : total.add(quantity);
            }
        }
        return total;
    }

    /** 安全处理空集合，避免 NPE。 */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 模板单行的合并状态：包含所有字段值和已写入的本批基础字段记录。 */
    private static final class RowState {
        /** 行数据（顺序写入，保持列次序） */
        private final Map<String, Object> values = new LinkedHashMap<>();
        /** 已写入的本批基础字段名集合，用于防止后续批次覆盖 */
        private final Set<String> currentBaseFields = new LinkedHashSet<>();

        /** 从行键初始化基础维度的默认值。 */
        private RowState(RowKey rowKey) {
            this.values.put("machineCode", rowKey.machineCode);
            this.values.put("clothCode", rowKey.clothCode);
            this.values.put("bigRollCode", rowKey.bigRollCode);
        }
    }

    /**
     * 模板明细行唯一键：机台 + 帘布 + 大卷，用于合并上下批相同维度的行。
     */
    private static final class RowKey {
        /** 机台编码 */
        private final String machineCode;
        /** 帘布编码 */
        private final String clothCode;
        /** 大卷编码 */
        private final String bigRollCode;

        /** 以机台、帘布、大卷编码构造行键。 */
        private RowKey(String machineCode, String clothCode, String bigRollCode) {
            this.machineCode = machineCode;
            this.clothCode = clothCode;
            this.bigRollCode = bigRollCode;
        }

        /** 从排程结果构建行键。 */
        private static RowKey from(Cd90ScheduleResult result) {
            return new RowKey(result.getMachineCode(), result.getClothCode(), result.getBigRollCode());
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
                    && Objects.equals(this.clothCode, rowKey.clothCode)
                    && Objects.equals(this.bigRollCode, rowKey.bigRollCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.machineCode, this.clothCode, this.bigRollCode);
        }
    }

    /**
     * 成型施工版本唯一键：施工编码 + 配方版本，用于匹配帘布对应的成型排程计划量。
     */
    private static final class ConstructionKey {
        /** 施工编码（胎胚编码） */
        private final String constructionCode;
        /** 配方版本号 */
        private final String constructionVersion;

        /** 以施工编码和配方版本构造版本键。 */
        private ConstructionKey(String constructionCode, String constructionVersion) {
            this.constructionCode = constructionCode;
            this.constructionVersion = constructionVersion;
        }

        /** 从施工主数据构建版本键。 */
        private static ConstructionKey from(MdmConstructionInfo construction) {
            return new ConstructionKey(
                    construction.getConstructionCode(), construction.getConstructionVersion());
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
            return Objects.equals(this.constructionCode, that.constructionCode)
                    && Objects.equals(this.constructionVersion, that.constructionVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.constructionCode, this.constructionVersion);
        }
    }
}
