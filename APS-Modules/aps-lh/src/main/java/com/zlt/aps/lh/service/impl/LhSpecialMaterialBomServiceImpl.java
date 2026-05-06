package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 特殊物料清单配置服务实现类
 * <p>
 * 参考成型关键产品配置(CxKeyProduct)实现，
 * 唯一性校验逻辑：结构+分类 或 物料编码+分类 不能重复
 * 结构与物料可以只填1个
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
        if (entity.getId() != null) {
            entity.setBaseVale(entity.getId());
        } else {
            entity.setBaseVale(null);
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
     * 1. 结构编码和物料编码至少填1个
     * 2. 分类必填
     * 3. 分厂必填
     * 4. 结构编码+分类 或 物料编码+分类 唯一性校验
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
            // 2.1 按结构编码+分类分组
            Map<String, List<LhSpecialMaterialBom>> structureCategoryRepeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getStructureName()) && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getStructureName() + "_" + item.getCategory()));

            // 2.2 按物料编码+分类分组
            Map<String, List<LhSpecialMaterialBom>> materialCategoryRepeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getMaterialCode()) && StringUtil.isNotBlank(item.getCategory()))
                    .collect(Collectors.groupingBy(item -> item.getMaterialCode() + "_" + item.getCategory()));

            // 2.3 遍历进行校验
            List<LhSpecialMaterialBom> checkList = new ArrayList<>();
            for (LhSpecialMaterialBom docEntity : validList) {
                int errorNum = docEntity.getRowNo();
                boolean isCan = true;

                // 必填字段校验 - 结构编码和物料编码至少填1个
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

                // 必填字段校验 - 分厂
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.factoryCodeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // Excel内重复校验 - 结构编码+分类
                if (StringUtil.isNotBlank(docEntity.getStructureName()) && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String structureCategoryKey = docEntity.getStructureName() + "_" + docEntity.getCategory();
                    List<LhSpecialMaterialBom> repeatList = structureCategoryRepeatMap.get(structureCategoryKey);
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

                // Excel内重复校验 - 物料编码+分类
                if (StringUtil.isNotBlank(docEntity.getMaterialCode()) && StringUtil.isNotBlank(docEntity.getCategory())) {
                    String materialCategoryKey = docEntity.getMaterialCode() + "_" + docEntity.getCategory();
                    List<LhSpecialMaterialBom> repeatList = materialCategoryRepeatMap.get(materialCategoryKey);
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

                // 数据库唯一性校验
                if (isCan) {
                    if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                        checkList.add(docEntity);
                    } else {
                        if (updateSupport) {
                            // 查询已存在记录进行更新
                            QueryWrapper<LhSpecialMaterialBom> queryWrapper = new QueryWrapper<>();
                            if (StringUtil.isNotBlank(docEntity.getStructureName())) {
                                queryWrapper.eq("STRUCTURE_NAME", docEntity.getStructureName());
                            }
                            if (StringUtil.isNotBlank(docEntity.getMaterialCode())) {
                                queryWrapper.eq("MATERIAL_CODE", docEntity.getMaterialCode());
                            }
                            queryWrapper.eq("CATEGORY", docEntity.getCategory());
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
     * 唯一性规则：结构+分类 或 物料编码+分类 不能重复
     * 结构与物料可以只填1个，当填了结构时校验结构+分类唯一，
     * 当填了物料编码时校验物料编码+分类唯一
     *
     * @param entity 校验对象
     * @return 唯一性结果
     */
    @Override
    public String checkUnique(LhSpecialMaterialBom entity) {
        if (entity == null) {
            return UserConstants.NOT_UNIQUE;
        }

        // 校验结构编码+分类唯一性
        if (StringUtil.isNotBlank(entity.getStructureName()) && StringUtil.isNotBlank(entity.getCategory())) {
            LambdaQueryWrapper<LhSpecialMaterialBom> wrapper = Wrappers.lambdaQuery();
            wrapper.ne(entity.getId() != null, LhSpecialMaterialBom::getId, entity.getId());
            wrapper.eq(LhSpecialMaterialBom::getStructureName, entity.getStructureName());
            wrapper.eq(LhSpecialMaterialBom::getCategory, entity.getCategory());
            Long count = lhSpecialMaterialBomEntityMapper.selectCount(wrapper);
            if (count > 0) {
                return UserConstants.NOT_UNIQUE;
            }
        }

        // 校验物料编码+分类唯一性
        if (StringUtil.isNotBlank(entity.getMaterialCode()) && StringUtil.isNotBlank(entity.getCategory())) {
            LambdaQueryWrapper<LhSpecialMaterialBom> wrapper = Wrappers.lambdaQuery();
            wrapper.ne(entity.getId() != null, LhSpecialMaterialBom::getId, entity.getId());
            wrapper.eq(LhSpecialMaterialBom::getMaterialCode, entity.getMaterialCode());
            wrapper.eq(LhSpecialMaterialBom::getCategory, entity.getCategory());
            Long count = lhSpecialMaterialBomEntityMapper.selectCount(wrapper);
            if (count > 0) {
                return UserConstants.NOT_UNIQUE;
            }
        }

        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("structureName", "materialCode", "category");
    }
}
