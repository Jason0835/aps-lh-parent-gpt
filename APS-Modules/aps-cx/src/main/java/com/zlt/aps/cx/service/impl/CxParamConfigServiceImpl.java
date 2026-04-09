package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.service.CxParamConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 排程参数配置服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxParamConfigServiceImpl extends AbstractDocService<CxParamConfig> implements CxParamConfigService {

    @Autowired
    private CxParamConfigMapper cxParamConfigMapper;

    @Override
    public AjaxResult importData(List<CxParamConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxParamConfig> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // Step1: 数据校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxParamConfig docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: 处理有效数据
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxParamConfig docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 必填字段校验
            if (StringUtil.isBlank(docEntity.getParamCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxParamConfig.paramCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            // 唯一性校验
            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxParamConfig> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("PARAM_CODE", docEntity.getParamCode());
                    CxParamConfig existEntity = cxParamConfigMapper.selectOne(queryWrapper);
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

        // Step3: 批量导入
        for (CxParamConfig entity : importList) {
            if (entity.getId() != null) {
                cxParamConfigMapper.updateById(entity);
            } else {
                cxParamConfigMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxParamConfig entity) {
        QueryWrapper<CxParamConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("PARAM_CODE", entity.getParamCode());

        if (cxParamConfigMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("paramCode");
    }

    @Override
    protected String getDocTypeCode() {
        return "CX_PARAM_CONFIG";
    }
}
