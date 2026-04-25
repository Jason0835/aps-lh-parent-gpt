package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
import com.zlt.aps.cx.service.ICxStructureTreadConfigService;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import jodd.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构整车胎面配置Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CxStructureTreadConfigServiceImpl extends AbstractDocService<CxStructureTreadConfig> implements ICxStructureTreadConfigService {

    @Autowired
    private BaseDao baseDao;

    @Resource
    private CxStructureTreadConfigMapper cxStructureTreadConfigMapper;

    @Resource
    private MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    /**
     * 导入结构整车胎面配置数据，按工厂+结构校验唯一性，并校验结构是否已在SKU与结构关系中维护。
     */
    @Override
    public AjaxResult importData(List<CxStructureTreadConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxStructureTreadConfig> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxStructureTreadConfig docEntity = list.get(i);
//            fillDefaultFactoryCode(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    "factoryCode", "structureCode");
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        Map<String, Set<String>> structureNameMap = getStructureNameMap(list);
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxStructureTreadConfig docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            boolean isCan = true;
            Set<String> structureNameSet = structureNameMap.get(docEntity.getFactoryCode());
            if (CollectionUtils.isEmpty(structureNameSet) || !structureNameSet.contains(docEntity.getStructureCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.mdmStructureLhRatio.structureNameNotExists"),
                        importErrorLogs);
                isCan = false;
            }
            if (!isCan) {
                failureNum++;
                continue;
            }

            if (UserConstants.UNIQUE.equals(checkUnique(docEntity))) {
                importList.add(docEntity);
                successNum++;
            } else if (updateSupport) {
                CxStructureTreadConfig existEntity = getByUniqueKey(docEntity);
                if (existEntity != null) {
                    docEntity.setId(existEntity.getId());
                    importList.add(docEntity);
                    successNum++;
                }
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        I18nUtil.getMessage("ui.data.alert.cxStructureTreadConfig.structureCodeNotUnique"),
                        importErrorLogs);
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }

        for (CxStructureTreadConfig entity : importList) {
            if (entity.getId() != null) {
                cxStructureTreadConfigMapper.updateById(entity);
            } else {
                cxStructureTreadConfigMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 校验工厂+结构唯一性。
     */
    @Override
    public String checkUnique(CxStructureTreadConfig entity) {
        LambdaQueryWrapper<CxStructureTreadConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")),
                CxStructureTreadConfig::getId, entity.getFieldValueByFieldName("id"));
        queryWrapper.eq(CxStructureTreadConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(CxStructureTreadConfig::getStructureCode, entity.getStructureCode());
        return cxStructureTreadConfigMapper.selectCount(queryWrapper) > 0
                ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "structureCode");
    }

    @Override
    public int saveOrUpdateBatch(List<CxStructureTreadConfig> list) {
        baseDao.saveBatch(list);
        return list.size();
    }

    /**
     * 按工厂批量预取结构名称，避免导入时逐行查询SKU与结构关系。
     */
    private Map<String, Set<String>> getStructureNameMap(List<CxStructureTreadConfig> list) {
        Map<String, Set<String>> structureNameMap = new HashMap<>(16);
        Map<String, List<CxStructureTreadConfig>> factoryCodeMap = list.stream()
                .filter(item -> item.getId() == null || item.getId() != -999L)
                .filter(item -> StringUtil.isNotBlank(item.getFactoryCode()))
                .filter(item -> StringUtil.isNotBlank(item.getStructureCode()))
                .collect(Collectors.groupingBy(CxStructureTreadConfig::getFactoryCode));

        for (Map.Entry<String, List<CxStructureTreadConfig>> entry : factoryCodeMap.entrySet()) {
            List<String> structureCodeList = entry.getValue().stream()
                    .map(CxStructureTreadConfig::getStructureCode)
                    .filter(StringUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(structureCodeList)) {
                continue;
            }

            List<MdmSkuStructureRef> structureRefList = new ArrayList<>();
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(structureCodeList, 900);
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MdmSkuStructureRef::getFactoryCode, entry.getKey());
                queryWrapper.in(MdmSkuStructureRef::getStructureName, codeList);
                structureRefList.addAll(mdmSkuStructureRefEntityMapper.selectList(queryWrapper));
            }
            if (CollectionUtils.isNotEmpty(structureRefList)) {
                structureNameMap.put(entry.getKey(), structureRefList.stream()
                        .map(MdmSkuStructureRef::getStructureName)
                        .filter(StringUtil::isNotBlank)
                        .collect(Collectors.toSet()));
            }
        }
        return structureNameMap;
    }

    private CxStructureTreadConfig getByUniqueKey(CxStructureTreadConfig entity) {
        LambdaQueryWrapper<CxStructureTreadConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CxStructureTreadConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(CxStructureTreadConfig::getStructureCode, entity.getStructureCode());
        return cxStructureTreadConfigMapper.selectList(queryWrapper).stream().findFirst().orElse(null);
    }

    private void fillDefaultFactoryCode(CxStructureTreadConfig entity) {
        if (StringUtil.isBlank(entity.getFactoryCode())) {
            entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
    }
}
