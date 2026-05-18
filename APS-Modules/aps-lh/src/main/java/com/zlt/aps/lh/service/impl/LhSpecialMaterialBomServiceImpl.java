package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import com.zlt.aps.lh.api.enums.LhSpecialMaterialCategoryEnum;
import com.zlt.aps.lh.mapper.LhSpecialMaterialBomEntityMapper;
import com.zlt.aps.lh.service.ILhSpecialMaterialBomService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 特殊物料清单配置服务实现类
 * <p>
 * 唯一性校验逻辑（工厂为必要条件）：
 * - 有物料情况：工厂 + 物料编码 + 分类（分类只有19.5寸宽基和22.5寸宽基互斥，芯片胎可组合）
 * - 有结构无物料情况：工厂 + 结构 + 分类（分类只有19.5寸宽基和22.5寸宽基互斥，芯片胎可组合）
 *
 * @author zlt
 * @date 2026-05-06
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhSpecialMaterialBomServiceImpl extends AbstractDocService<LhSpecialMaterialBom> implements ILhSpecialMaterialBomService {

    @Autowired
    private LhSpecialMaterialBomEntityMapper lhSpecialMaterialBomEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_SPECIAL_MATERIAL_BOM";
    }

    @Override
    public int save(LhSpecialMaterialBom entity) {
        // 分类冲突校验：同一工厂+物料/结构下，19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合
        String conflict = checkCategoryConflict(entity);
        if (conflict != null) {
            throw new ServiceException(conflict);
        }

        if (entity.getId() != null) {
            entity.setBaseVale(entity.getId());
        } else {
            entity.setBaseVale(null);
            entity.setUpdateBy(SecurityUtils.getUsername());
            entity.setUpdateTime(new Date());
        }
        return super.save(entity);
    }

    /**
     * 查询列表
     *
     * @param queryWrapper 查询条件
     * @return 结果列表
     */
    @Override
    public List<LhSpecialMaterialBom> selectList(QueryWrapper<LhSpecialMaterialBom> queryWrapper) {
        return lhSpecialMaterialBomEntityMapper.selectList(queryWrapper);
    }

    /**
     * 导入数据
     * <p>
     * 校验逻辑：
     * 1. 结构和物料编码至少填1个
     * 2. 分类必填
     * 3. 工厂必填
     * 4. 唯一性校验（工厂为必要条件）：
     *    - 有物料：工厂 + 物料编码 + 分类
     *    - 有结构无物料：工厂 + 结构 + 分类
     * 5. 分类冲突校验：19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合
     */
    @Override
    public AjaxResult importData(List<LhSpecialMaterialBom> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<LhSpecialMaterialBom> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // Step1: 基础数据校验 + 设置行号
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhSpecialMaterialBom docEntity = list.get(i);
            docEntity.setRowNo(errorNum);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: Excel内数据重复校验
        List<LhSpecialMaterialBom> validList = list.stream()
                .filter(item -> item.getId() == null || item.getId() != -999L)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(validList)) {
            // 2.1 按工厂+物料编码+分类分组（有物料情况）
            Map<String, List<LhSpecialMaterialBom>> factoryMaterialCategoryRepeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getFactoryCode())
                            && StringUtil.isNotBlank(item.getMaterialCode())
                            && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getFactoryCode() + "_" + item.getMaterialCode() + "_" + item.getCategory()));

            // 2.2 按工厂+结构+分类分组（有结构无物料情况）
            Map<String, List<LhSpecialMaterialBom>> factoryStructureCategoryRepeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getFactoryCode())
                            && StringUtil.isNotBlank(item.getStructureName())
                            && StringUtil.isBlank(item.getMaterialCode())
                            && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getFactoryCode() + "_" + item.getStructureName() + "_" + item.getCategory()));

            // 2.3 按工厂+物料编码分组（分类冲突校验用，有物料情况）
            Map<String, List<LhSpecialMaterialBom>> materialCategoryGroupMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getFactoryCode())
                            && StringUtil.isNotBlank(item.getMaterialCode())
                            && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getFactoryCode() + "_" + item.getMaterialCode()));

            // 2.4 按工厂+结构名称分组（分类冲突校验用，有结构无物料情况）
            Map<String, List<LhSpecialMaterialBom>> structureCategoryGroupMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getFactoryCode())
                            && StringUtil.isNotBlank(item.getStructureName())
                            && StringUtil.isBlank(item.getMaterialCode())
                            && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getFactoryCode() + "_" + item.getStructureName()));

            // 2.6 遍历进行校验
            List<LhSpecialMaterialBom> checkList = new ArrayList<>();
            for (LhSpecialMaterialBom docEntity : validList) {
                int errorNum = docEntity.getRowNo();
                boolean isCan = true;

                // 必填字段校验 - 结构和物料编码至少填1个
                if (StringUtil.isBlank(docEntity.getStructureName()) && StringUtil.isBlank(docEntity.getMaterialCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.structureOrMaterialRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 分类
                if (StringUtil.isBlank(docEntity.getCategory())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.categoryRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 工厂
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.factoryCodeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // Excel内重复校验 - 有物料情况：工厂+物料编码+分类
                if (isCan && StringUtil.isNotBlank(docEntity.getFactoryCode())
                        && StringUtil.isNotBlank(docEntity.getMaterialCode())
                        && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String key = docEntity.getFactoryCode() + "_" + docEntity.getMaterialCode() + "_" + docEntity.getCategory();
                    List<LhSpecialMaterialBom> repeatList = factoryMaterialCategoryRepeatMap.get(key);
                    if (CollectionUtils.isNotEmpty(repeatList) && repeatList.size() > 1) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("import.validated.repeat"), errorNum,
                                repeatList.stream()
                                        .map(item -> String.valueOf(item.getRowNo()))
                                        .filter(row -> !row.equals(String.valueOf(errorNum)))
                                        .collect(Collectors.joining(", ")));
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, message, importErrorLogs);
                    }
                }

                // Excel内重复校验 - 有结构无物料情况：工厂+结构+分类
                if (isCan && StringUtil.isNotBlank(docEntity.getFactoryCode())
                        && StringUtil.isNotBlank(docEntity.getStructureName())
                        && StringUtil.isBlank(docEntity.getMaterialCode())
                        && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String key = docEntity.getFactoryCode() + "_" + docEntity.getStructureName() + "_" + docEntity.getCategory();
                    List<LhSpecialMaterialBom> repeatList = factoryStructureCategoryRepeatMap.get(key);
                    if (CollectionUtils.isNotEmpty(repeatList) && repeatList.size() > 1) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("import.validated.repeat"), errorNum,
                                repeatList.stream()
                                        .map(item -> String.valueOf(item.getRowNo()))
                                        .filter(row -> !row.equals(String.valueOf(errorNum)))
                                        .collect(Collectors.joining(", ")));
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, message, importErrorLogs);
                    }
                }

                // Excel内分类冲突校验 - 按物料编码维度（有物料情况）
                if (isCan && StringUtil.isNotBlank(docEntity.getFactoryCode())
                        && StringUtil.isNotBlank(docEntity.getMaterialCode())
                        && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String key = docEntity.getFactoryCode() + "_" + docEntity.getMaterialCode();
                    List<LhSpecialMaterialBom> groupList = materialCategoryGroupMap.get(key);
                    if (CollectionUtils.isNotEmpty(groupList)) {
                        Set<String> categorySet = groupList.stream()
                                .map(LhSpecialMaterialBom::getCategory)
                                .filter(StringUtil::isNotBlank)
                                .collect(Collectors.toSet());
                        String conflictMsg = checkCategorySetConstraint(categorySet, "物料编码", docEntity.getMaterialCode());
                        if (conflictMsg != null) {
                            isCan = false;
                            String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.categoryConflict.withRow"), errorNum, conflictMsg);
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, message, importErrorLogs);
                        }
                    }
                }

                // Excel内分类冲突校验 - 按结构名称维度（有结构无物料情况）
                if (isCan && StringUtil.isNotBlank(docEntity.getFactoryCode())
                        && StringUtil.isNotBlank(docEntity.getStructureName())
                        && StringUtil.isBlank(docEntity.getMaterialCode())
                        && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String key = docEntity.getFactoryCode() + "_" + docEntity.getStructureName();
                    List<LhSpecialMaterialBom> groupList = structureCategoryGroupMap.get(key);
                    if (CollectionUtils.isNotEmpty(groupList)) {
                        Set<String> categorySet = groupList.stream()
                                .map(LhSpecialMaterialBom::getCategory)
                                .filter(StringUtil::isNotBlank)
                                .collect(Collectors.toSet());
                        String conflictMsg = checkCategorySetConstraint(categorySet, "结构名称", docEntity.getStructureName());
                        if (conflictMsg != null) {
                            isCan = false;
                            String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.categoryConflict.withRow"), errorNum, conflictMsg);
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, message, importErrorLogs);
                        }
                    }
                }

                // 数据库分类冲突校验
                if (isCan) {
                    String dbConflict = checkCategoryConflict(docEntity);
                    if (dbConflict != null) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.categoryConflict.withRow"), errorNum, dbConflict);
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, message, importErrorLogs);
                    }
                }

                // 数据库唯一性校验
                if (isCan) {
                    if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                        checkList.add(docEntity);
                    } else {
                        if (updateSupport) {
                            // 查询已存在记录进行更新（按新的唯一性规则：有物料=工厂+物料+分类，有结构无物料=工厂+结构+分类）
                            LambdaQueryWrapper<LhSpecialMaterialBom> queryWrapper = Wrappers.lambdaQuery();
                            queryWrapper.eq(LhSpecialMaterialBom::getFactoryCode, docEntity.getFactoryCode());
                            queryWrapper.eq(LhSpecialMaterialBom::getCategory, docEntity.getCategory());
                            if (StringUtil.isNotBlank(docEntity.getMaterialCode())) {
                                queryWrapper.eq(LhSpecialMaterialBom::getMaterialCode, docEntity.getMaterialCode());
                            } else {
                                queryWrapper.isNull(LhSpecialMaterialBom::getMaterialCode)
                                        .or().eq(LhSpecialMaterialBom::getMaterialCode, "");
                            }
                            if (StringUtil.isNotBlank(docEntity.getStructureName())) {
                                queryWrapper.eq(LhSpecialMaterialBom::getStructureName, docEntity.getStructureName());
                            } else {
                                queryWrapper.isNull(LhSpecialMaterialBom::getStructureName)
                                        .or().eq(LhSpecialMaterialBom::getStructureName, "");
                            }
                            LhSpecialMaterialBom existEntity = lhSpecialMaterialBomEntityMapper.selectOne(queryWrapper);
                            if (existEntity != null) {
                                docEntity.setId(existEntity.getId());
                                checkList.add(docEntity);
                            }
                        } else {
                            isCan = false;
                            String notUniqueMsg = I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.notUnique.withRow");
                            String message = String.format(notUniqueMsg, errorNum);
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, message, importErrorLogs);
                        }
                    }
                }

                if (!isCan) {
                    failureNum++;
                    docEntity.setId(-999L);
                }
            }
            importList = checkList;
            successNum = importList.size();
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        // Step3: 批量导入 - 分离新增和更新数据
        List<LhSpecialMaterialBom> insertList = importList.stream()
                .filter(entity -> entity.getId() == null)
                .collect(Collectors.toList());
        List<LhSpecialMaterialBom> updateList = importList.stream()
                .filter(entity -> entity.getId() != null)
                .collect(Collectors.toList());

        // 批量插入
        if (CollectionUtils.isNotEmpty(insertList)) {
            lhSpecialMaterialBomEntityMapper.insertBatch(insertList);
        }

        // 批量更新
        if (CollectionUtils.isNotEmpty(updateList)) {
            lhSpecialMaterialBomEntityMapper.updateBatch(updateList);
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     * <p>
     * 唯一性规则（工厂为必要条件）：
     * - 有物料情况：工厂 + 物料编码 + 分类（分类只有19.5寸宽基和22.5寸宽基互斥，芯片胎可组合）
     * - 有结构无物料情况：工厂 + 结构 + 分类（分类只有19.5寸宽基和22.5寸宽基互斥，芯片胎可组合）
     *
     * @param entity 校验对象
     * @return 唯一性结果
     */
    @Override
    public String checkUnique(LhSpecialMaterialBom entity) {
        if (entity == null || StringUtil.isBlank(entity.getFactoryCode()) || StringUtil.isBlank(entity.getCategory())) {
            return UserConstants.NOT_UNIQUE;
        }

        LambdaQueryWrapper<LhSpecialMaterialBom> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(entity.getId() != null, LhSpecialMaterialBom::getId, entity.getId());
        wrapper.eq(LhSpecialMaterialBom::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(LhSpecialMaterialBom::getCategory, entity.getCategory());

        // 有物料情况：工厂 + 物料编码 + 分类
        if (StringUtil.isNotBlank(entity.getMaterialCode())) {
            wrapper.eq(LhSpecialMaterialBom::getMaterialCode, entity.getMaterialCode());
        }
        // 有结构无物料情况：工厂 + 结构 + 分类
        else if (StringUtil.isNotBlank(entity.getStructureName())) {
            wrapper.eq(LhSpecialMaterialBom::getStructureName, entity.getStructureName());
            wrapper.and(w -> w.isNull(LhSpecialMaterialBom::getMaterialCode)
                    .or().eq(LhSpecialMaterialBom::getMaterialCode, ""));
        }

        Long count = lhSpecialMaterialBomEntityMapper.selectCount(wrapper);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }

        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "materialCode", "structureName", "category");
    }

    /**
     * 校验分类冲突。
     * 有物料情况：同一工厂+物料编码下，19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合。
     * 有结构无物料情况：同一工厂+结构下，19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合。
     *
     * @param entity 待校验实体
     * @return 冲突结果，null表示无冲突，非null表示冲突描述
     */
    @Override
    public String checkCategoryConflict(LhSpecialMaterialBom entity) {
        if (entity == null || StringUtil.isBlank(entity.getFactoryCode()) || StringUtil.isBlank(entity.getCategory())) {
            return null;
        }

        // 有物料情况：按物料编码维度校验分类冲突
        if (StringUtil.isNotBlank(entity.getMaterialCode())) {
            String conflict = checkCategoryConflictByMaterial(entity);
            if (conflict != null) {
                return conflict;
            }
        }

        // 有结构无物料情况：按结构名称维度校验分类冲突
        if (StringUtil.isNotBlank(entity.getStructureName()) && StringUtil.isBlank(entity.getMaterialCode())) {
            String conflict = checkCategoryConflictByStructure(entity);
            if (conflict != null) {
                return conflict;
            }
        }

        return null;
    }

    /**
     * 按物料编码维度校验分类冲突。
     * 查询同一工厂+物料编码下已存在的所有分类，与当前实体分类合并后校验约束。
     *
     * @param entity 待校验实体
     * @return 冲突结果，null表示无冲突，非null表示冲突描述
     */
    private String checkCategoryConflictByMaterial(LhSpecialMaterialBom entity) {
        LambdaQueryWrapper<LhSpecialMaterialBom> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(entity.getId() != null, LhSpecialMaterialBom::getId, entity.getId());
        wrapper.eq(LhSpecialMaterialBom::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(LhSpecialMaterialBom::getMaterialCode, entity.getMaterialCode());
        List<LhSpecialMaterialBom> existList = lhSpecialMaterialBomEntityMapper.selectList(wrapper);

        Set<String> categorySet = existList.stream()
                .map(LhSpecialMaterialBom::getCategory)
                .filter(StringUtil::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));
        categorySet.add(entity.getCategory());

        return checkCategorySetConstraint(categorySet, "物料编码", entity.getMaterialCode());
    }

    /**
     * 按结构名称维度校验分类冲突（有结构无物料情况）。
     * 查询同一工厂+结构名称下（物料编码为空）已存在的所有分类，与当前实体分类合并后校验约束。
     *
     * @param entity 待校验实体
     * @return 冲突结果，null表示无冲突，非null表示冲突描述
     */
    private String checkCategoryConflictByStructure(LhSpecialMaterialBom entity) {
        LambdaQueryWrapper<LhSpecialMaterialBom> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(entity.getId() != null, LhSpecialMaterialBom::getId, entity.getId());
        wrapper.eq(LhSpecialMaterialBom::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(LhSpecialMaterialBom::getStructureName, entity.getStructureName());
        wrapper.and(w -> w.isNull(LhSpecialMaterialBom::getMaterialCode)
                .or().eq(LhSpecialMaterialBom::getMaterialCode, ""));
        List<LhSpecialMaterialBom> existList = lhSpecialMaterialBomEntityMapper.selectList(wrapper);

        Set<String> categorySet = existList.stream()
                .map(LhSpecialMaterialBom::getCategory)
                .filter(StringUtil::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));
        categorySet.add(entity.getCategory());

        return checkCategorySetConstraint(categorySet, "结构名称", entity.getStructureName());
    }

    /**
     * 校验分类集合约束。
     * 19.5寸宽基(01)和22.5寸宽基(02)互斥，芯片胎(03)可与它们组合。
     * 即非芯片胎分类最多只能出现一种。
     *
     * @param categorySet 分类编码集合
     * @param dimensionName 维度名称（物料编码/结构名称）
     * @param dimensionValue 维度值
     * @return 冲突结果，null表示无冲突，非null表示冲突描述
     */
    private String checkCategorySetConstraint(Set<String> categorySet, String dimensionName, String dimensionValue) {
        // 统计非芯片胎分类数量（19.5寸宽基01、22.5寸宽基02）
        long nonChipTireCount = categorySet.stream()
                .filter(c -> !StringUtil.equals(LhSpecialMaterialCategoryEnum.CHIP_TIRE.getCode(), c))
                .count();

        if (nonChipTireCount > 1) {
            return String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.categoryConflict"),
                    dimensionName, dimensionValue);
        }

        return null;
    }
}
