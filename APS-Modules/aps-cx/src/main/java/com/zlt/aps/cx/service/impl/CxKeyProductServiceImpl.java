package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.service.CxKeyProductService;
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
 * 关键产品配置服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxKeyProductServiceImpl extends AbstractDocService<CxKeyProduct> implements CxKeyProductService {

    @Autowired
    private CxKeyProductMapper cxKeyProductMapper;

    @Override
    public AjaxResult importData(List<CxKeyProduct> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxKeyProduct> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // Step1: 数据校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxKeyProduct docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated, "embryoCode", "structureName");
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: 处理有效数据
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxKeyProduct docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 必填字段校验
            if (StringUtil.isBlank(docEntity.getEmbryoCode())) {
                failureNum++;
                String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.embryoCodeRequired"),errorNum);
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, message, importErrorLogs);
                continue;
            }
            if (StringUtil.isBlank(docEntity.getStructureName())) {
                failureNum++;
                String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.embryoCodeRequired"),errorNum);
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, message, importErrorLogs);
                continue;
            }

            // 唯一性校验
            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxKeyProduct> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("EMBRYO_CODE", docEntity.getEmbryoCode());
                    queryWrapper.eq("STRUCTURE_NAME", docEntity.getStructureName());
                    CxKeyProduct existEntity = cxKeyProductMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
                        importList.add(docEntity);
                        successNum++;
                    }
                } else {
                    failureNum++;
                    String notUniqueMsg = I18nUtil.getMessage("ui.data.alert.cxKeyProduct.notUnique.withRow");
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            notUniqueMsg, importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        // Step3: 批量导入
        for (CxKeyProduct entity : importList) {
            if (entity.getId() != null) {
                cxKeyProductMapper.updateById(entity);
            } else {
                cxKeyProductMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxKeyProduct entity) {
        QueryWrapper<CxKeyProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("EMBRYO_CODE", entity.getEmbryoCode());
        queryWrapper.eq("STRUCTURE_NAME", entity.getStructureName());

        if (cxKeyProductMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("embryoCode", "structureName");
    }

    @Override
    protected String getDocTypeCode() {
        return "CX_KEY_PRODUCT";
    }
}
