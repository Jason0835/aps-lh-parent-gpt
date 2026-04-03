package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmStructureTreadConfigEntityMapper;
import com.zlt.aps.maindata.service.IMdmStructureTreadConfigService;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
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

/**
 * APS结构整车胎面配置Service实现
 *
 * @author zlt
 * @since 2025/12/25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmStructureTreadConfigServiceImpl extends AbstractDocService<MdmStructureTreadConfig> implements IMdmStructureTreadConfigService {

    @Autowired
    private MdmStructureTreadConfigEntityMapper mdmStructureTreadConfigEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    @Override
    public AjaxResult importData(List<MdmStructureTreadConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<MdmStructureTreadConfig> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmStructureTreadConfig docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmStructureTreadConfig docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtil.isBlank(docEntity.getStructureCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.mdmStructureTreadConfig.structureCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            }

        if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
            if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            }
            importList.add(docEntity);
            successNum++;
        } else {
            if (updateSupport) {
                QueryWrapper<MdmStructureTreadConfig> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("FACTORY_CODE", docEntity.getFactoryCode());
                queryWrapper.eq("STRUCTURE_CODE", docEntity.getStructureCode());
                MdmStructureTreadConfig existEntity = mdmStructureTreadConfigEntityMapper.selectOne(queryWrapper);
                if (existEntity != null) {
                    docEntity.setId(existEntity.getId());
                    importList.add(docEntity);
                    successNum++;
                }
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                    String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        for (MdmStructureTreadConfig entity : importList) {
            if (entity.getId() != null) {
                mdmStructureTreadConfigEntityMapper.updateById(entity);
            } else {
                mdmStructureTreadConfigEntityMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(MdmStructureTreadConfig entity) {
        Long id = entity.getId() == null ? -1L : entity.getId();
        QueryWrapper<MdmStructureTreadConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("STRUCTURE_CODE", entity.getStructureCode());

        if (mdmStructureTreadConfigEntityMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "structureCode");
    }
}
