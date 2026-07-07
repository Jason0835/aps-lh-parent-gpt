package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.cx.mapper.CxEmbryoLhTimeMapper;
import com.zlt.aps.cx.service.CxEmbryoLhTimeService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胎胚最早可供硫化时间服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxEmbryoLhTimeServiceImpl extends AbstractDocService<CxEmbryoLhTime> implements CxEmbryoLhTimeService {

    @Autowired
    private CxEmbryoLhTimeMapper cxEmbryoLhTimeMapper;

    @Override
    public AjaxResult importData(List<CxEmbryoLhTime> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxEmbryoLhTime> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // Step1: 基础数据校验 + 设置行号
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxEmbryoLhTime docEntity = list.get(i);
            docEntity.setRowNo(errorNum);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: Excel内数据重复校验
        List<CxEmbryoLhTime> validList = list.stream()
                .filter(item -> item.getId() == null || item.getId() != -999L)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(validList)) {
            // 按结构名称建立重复映射
            Map<String, List<CxEmbryoLhTime>> repeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getStructureName()))
                    .collect(Collectors.groupingBy(item -> item.getStructureName().trim()));

            List<CxEmbryoLhTime> checkList = new ArrayList<>();
            for (CxEmbryoLhTime docEntity : validList) {
                int errorNum = docEntity.getRowNo();
                boolean isCan = true;

                // 校验Excel内重复（结构名称）
                if (StringUtil.isNotBlank(docEntity.getStructureName())) {
                    String key = docEntity.getStructureName().trim();
                    List<CxEmbryoLhTime> repeatList = repeatMap.get(key);
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

                // 必填字段校验 - 结构
                if (StringUtil.isBlank(docEntity.getStructureName())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.cxEmbryoLhTime.structureNameRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 最早可供硫化时间
                if (docEntity.getEarliestLhTime() == null) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.cxEmbryoLhTime.earliestLhTimeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 数据库唯一性校验
                if (isCan) {
                    if (this.checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                        checkList.add(docEntity);
                    } else {
                        if (updateSupport) {
                            LambdaQueryWrapper<CxEmbryoLhTime> queryWrapper = new LambdaQueryWrapper<>();
                            queryWrapper.eq(CxEmbryoLhTime::getStructureName, docEntity.getStructureName().trim());
                            CxEmbryoLhTime existEntity = cxEmbryoLhTimeMapper.selectOne(queryWrapper);
                            if (existEntity != null) {
                                docEntity.setId(existEntity.getId());
                                checkList.add(docEntity);
                            }
                        } else {
                            isCan = false;
                            String notUniqueMsg = I18nUtil.getMessage("ui.data.alert.cxEmbryoLhTime.notUnique.withRow");
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
        List<CxEmbryoLhTime> insertList = importList.stream()
                .filter(entity -> entity.getId() == null)
                .collect(Collectors.toList());
        List<CxEmbryoLhTime> updateList = importList.stream()
                .filter(entity -> entity.getId() != null)
                .collect(Collectors.toList());

        // 批量插入
        if (CollectionUtils.isNotEmpty(insertList)) {
            cxEmbryoLhTimeMapper.insertBatch(insertList);
        }

        // 批量更新
        if (CollectionUtils.isNotEmpty(updateList)) {
            cxEmbryoLhTimeMapper.updateBatch(updateList);
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxEmbryoLhTime entity) {
        LambdaQueryWrapper<CxEmbryoLhTime> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), CxEmbryoLhTime::getId, entity.getFieldValueByFieldName("id"));
        queryWrapper.eq(CxEmbryoLhTime::getStructureName, entity.getStructureName());

        if (cxEmbryoLhTimeMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("structureName");
    }

    @Override
    protected String getDocTypeCode() {
        return "CX_EMBRYO_LH_TIME";
    }
}
