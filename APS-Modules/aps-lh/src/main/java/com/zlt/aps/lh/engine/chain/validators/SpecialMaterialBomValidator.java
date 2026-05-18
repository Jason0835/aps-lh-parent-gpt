package com.zlt.aps.lh.engine.chain.validators;

import com.zlt.aps.lh.api.constant.LhDataValidationGroupConstant;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import com.zlt.aps.lh.api.enums.LhSpecialMaterialCategoryEnum;
import com.zlt.aps.lh.api.enums.ValidationPolicyEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.chain.IDataValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 特殊物料清单配置校验器。
 *
 * @author APS
 */
@Slf4j
@Component
public class SpecialMaterialBomValidator implements IDataValidator {

    private static final String VALIDATOR_KEY = "specialMaterialBomValidator";
    private static final int MAX_ERROR_DETAIL_COUNT = 10;

    @Override
    public boolean validate(LhScheduleContext context) {
        if (CollectionUtils.isEmpty(context.getSpecialMaterialBomList())) {
            log.info("特殊物料清单校验通过, 配置数: 0");
            return true;
        }
        List<String> emptyKeyErrorList = new ArrayList<>(MAX_ERROR_DETAIL_COUNT);
        List<String> invalidCategoryErrorList = new ArrayList<>(MAX_ERROR_DETAIL_COUNT);
        List<String> categoryConflictErrorList = new ArrayList<>(MAX_ERROR_DETAIL_COUNT);
        // 按物料编码维度收集分类
        Map<String, Set<String>> materialCategoryMap = new HashMap<>(16);
        Map<String, Map<String, Integer>> materialCategoryFirstRowMap = new HashMap<>(16);
        Set<String> materialConflictKeySet = new HashSet<>(8);
        // 按结构名称维度收集分类
        Map<String, Set<String>> structureCategoryMap = new HashMap<>(16);
        Map<String, Map<String, Integer>> structureCategoryFirstRowMap = new HashMap<>(16);
        Set<String> structureConflictKeySet = new HashSet<>(8);
        int rowIndex = 0;
        for (LhSpecialMaterialBom bom : context.getSpecialMaterialBomList()) {
            rowIndex++;
            if (Objects.isNull(bom)) {
                continue;
            }
            String materialCode = normalizeText(bom.getMaterialCode());
            String structureName = normalizeText(bom.getStructureName());
            if (StringUtils.isEmpty(materialCode) && StringUtils.isEmpty(structureName)) {
                addErrorDetail(emptyKeyErrorList, buildRowText(rowIndex, bom));
            }
            if (!LhSpecialMaterialCategoryEnum.isValid(bom.getCategory())) {
                addErrorDetail(invalidCategoryErrorList, buildRowText(rowIndex, bom));
                continue;
            }
            // 按物料编码维度收集分类冲突（有物料情况）
            if (StringUtils.isNotEmpty(materialCode)) {
                collectCategoryConflict(categoryConflictErrorList, materialCategoryMap,
                        materialCategoryFirstRowMap, materialConflictKeySet,
                        materialCode, bom.getCategory(), "物料编码", rowIndex);
            }
            // 按结构名称维度收集分类冲突（有结构无物料情况）
            if (StringUtils.isNotEmpty(structureName) && StringUtils.isEmpty(materialCode)) {
                collectCategoryConflict(categoryConflictErrorList, structureCategoryMap,
                        structureCategoryFirstRowMap, structureConflictKeySet,
                        structureName, bom.getCategory(), "结构名称", rowIndex);
            }
        }
        if (!CollectionUtils.isEmpty(emptyKeyErrorList)) {
            context.addValidationError("[" + getValidatorName()
                    + "] 特殊物料清单结构名称和物料编码至少填写一个: "
                    + String.join("；", emptyKeyErrorList));
        }
        if (!CollectionUtils.isEmpty(invalidCategoryErrorList)) {
            context.addValidationError("[" + getValidatorName()
                    + "] 特殊物料清单分类只能为01/02/03: "
                    + String.join("；", invalidCategoryErrorList));
        }
        if (!CollectionUtils.isEmpty(categoryConflictErrorList)) {
            context.addValidationError("[" + getValidatorName()
                    + "] 同一物料/结构下19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合: "
                    + String.join("；", categoryConflictErrorList));
        }
        boolean passed = CollectionUtils.isEmpty(emptyKeyErrorList)
                && CollectionUtils.isEmpty(invalidCategoryErrorList)
                && CollectionUtils.isEmpty(categoryConflictErrorList);
        if (passed) {
            log.info("特殊物料清单校验通过, 配置数: {}", context.getSpecialMaterialBomList().size());
        } else {
            log.warn("特殊物料清单校验未通过, 空键错误: {}, 分类错误: {}, 分类冲突: {}",
                    emptyKeyErrorList.size(), invalidCategoryErrorList.size(),
                    categoryConflictErrorList.size());
        }
        return passed;
    }

    @Override
    public String getValidatorName() {
        return "特殊物料清单校验";
    }

    /**
     * 获取校验器唯一标识。
     *
     * @return 校验器唯一标识
     */
    @Override
    public String getValidatorKey() {
        return VALIDATOR_KEY;
    }

    @Override
    public int getGroup() {
        return LhDataValidationGroupConstant.BASE_DATA_INTEGRITY;
    }

    @Override
    public ValidationPolicyEnum getValidationPolicy() {
        return ValidationPolicyEnum.COLLECT_ALL;
    }

    @Override
    public int getOrder() {
        return 35;
    }

    /**
     * 添加错误明细，控制单条错误长度。
     *
     * @param errorList 错误列表
     * @param errorDetail 错误明细
     */
    private void addErrorDetail(List<String> errorList, String errorDetail) {
        if (errorList.size() < MAX_ERROR_DETAIL_COUNT) {
            errorList.add(errorDetail);
        }
    }

    /**
     * 收集同一维度下分类冲突。
     * 同一维度下只能有一条芯片胎分类和一条非芯片胎分类的数据，
     * 即非芯片胎分类（19.5寸宽基、22.5寸宽基）最多只能出现一种。
     *
     * @param errorList 冲突错误列表
     * @param categoryMap 已命中的分类集合
     * @param firstRowMap 各分类首次出现的行号Map
     * @param conflictKeySet 已记录冲突的键集合
     * @param key 当前命中键（物料编码或结构名称）
     * @param category 当前分类
     * @param dimensionName 维度名称（物料编码/结构名称）
     * @param rowIndex 当前行号
     */
    private void collectCategoryConflict(List<String> errorList,
                                          Map<String, Set<String>> categoryMap,
                                          Map<String, Map<String, Integer>> firstRowMap,
                                          Set<String> conflictKeySet,
                                          String key,
                                          String category,
                                          String dimensionName,
                                          int rowIndex) {
        Set<String> categorySet = categoryMap.computeIfAbsent(key, value -> new HashSet<String>(4));
        categorySet.add(category);
        // 记录各分类首次出现的行号
        Map<String, Integer> rowMap = firstRowMap.computeIfAbsent(key, value -> new HashMap<>(4));
        rowMap.putIfAbsent(category, rowIndex);

        // 统计非芯片胎分类数量
        long nonChipTireCount = categorySet.stream()
                .filter(c -> !StringUtils.equals(LhSpecialMaterialCategoryEnum.CHIP_TIRE.getCode(), c))
                .count();

        // 非芯片胎分类超过1种时存在冲突（同时存在19.5寸宽基和22.5寸宽基）
        if (nonChipTireCount <= 1 || !conflictKeySet.add(key)) {
            return;
        }

        // 构建冲突描述
        StringBuilder detail = new StringBuilder();
        detail.append(dimensionName).append("=").append(key).append("(");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : rowMap.entrySet()) {
            if (!first) {
                detail.append(", ");
            }
            first = false;
            LhSpecialMaterialCategoryEnum categoryEnum = LhSpecialMaterialCategoryEnum.getByCode(entry.getKey());
            String categoryDesc = categoryEnum != null ? categoryEnum.getDescription() : entry.getKey();
            detail.append("第").append(entry.getValue()).append("条分类=").append(categoryDesc);
        }
        detail.append(")");
        addErrorDetail(errorList, detail.toString());
    }

    /**
     * 构建配置行描述。
     *
     * @param rowIndex 行号
     * @param bom 特殊物料清单配置
     * @return 配置行描述
     */
    private String buildRowText(int rowIndex, LhSpecialMaterialBom bom) {
        return "第" + rowIndex + "条"
                + "(物料编码=" + StringUtils.defaultString(bom.getMaterialCode())
                + ", 结构名称=" + StringUtils.defaultString(bom.getStructureName())
                + ", 分类=" + StringUtils.defaultString(bom.getCategory()) + ")";
    }

    /**
     * 清洗配置匹配文本。
     *
     * @param value 原始值
     * @return 清洗后文本
     */
    private String normalizeText(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String trimValue = value.trim();
        return StringUtils.isEmpty(trimValue) ? null : trimValue;
    }
}
