package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.service.CxKeyProductService;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
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
 * 关键产品配置服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxKeyProductServiceImpl extends AbstractDocService<CxKeyProduct> implements CxKeyProductService {

    @Autowired
    private CxKeyProductMapper cxKeyProductMapper;

    @Autowired
    private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;

    @Autowired
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

    @Override
    public AjaxResult importData(List<CxKeyProduct> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxKeyProduct> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // Step1: 基础数据校验 + 设置行号
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxKeyProduct docEntity = list.get(i);
            docEntity.setRowNo(errorNum);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: Excel内数据重复校验
        // 2.1 收集所有有效的数据用于重复校验
        List<CxKeyProduct> validList = list.stream()
                .filter(item -> item.getId() == null || item.getId() != -999L)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(validList)) {
            // 2.2 建立重复映射（key: embryoCode + structureName, value: 重复的行列表）
            Map<String, List<CxKeyProduct>> repeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getEmbryoCode()) && StringUtil.isNotBlank(item.getStructureName()))
                    .collect(Collectors.groupingBy(item -> item.getEmbryoCode() + "_" + item.getStructureName()));

            // 2.3 遍历进行Excel内重复校验
            List<CxKeyProduct> checkList = new ArrayList<>();
            for (CxKeyProduct docEntity : validList) {
                int errorNum = docEntity.getRowNo();
                boolean isCan = true;

                // 校验Excel内重复（胎胚编码 + 结构名称）
                if (StringUtil.isNotBlank(docEntity.getEmbryoCode()) && StringUtil.isNotBlank(docEntity.getStructureName())) {
                    String key = docEntity.getEmbryoCode() + "_" + docEntity.getStructureName();
                    List<CxKeyProduct> repeatList = repeatMap.get(key);
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

                // 必填字段校验 - 胎胚编码
                if (StringUtil.isBlank(docEntity.getEmbryoCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.embryoCodeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 结构名称
                if (StringUtil.isBlank(docEntity.getStructureName())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.structureNameRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 校验结构是否存在（structureName对应MdmConstructionInfo的specCode）
                if (StringUtil.isNotBlank(docEntity.getStructureName())) {
                    QueryWrapper<MdmConstructionInfo> constructionQueryWrapper = new QueryWrapper<>();
                    constructionQueryWrapper.eq("SPEC_CODE", docEntity.getStructureName());
                    if (mdmConstructionInfoEntityMapper.selectCount(constructionQueryWrapper) == 0) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.structureNotExist"), errorNum, docEntity.getStructureName());
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, message, importErrorLogs);
                    }
                }

                // 校验胎胚编码是否在该结构下（embryoCode对应constructionCode，structureName对应specCode）
                if (StringUtil.isNotBlank(docEntity.getEmbryoCode()) && StringUtil.isNotBlank(docEntity.getStructureName())) {
                    QueryWrapper<MdmSkuConstructionRef> skuConstructionQueryWrapper = new QueryWrapper<>();
                    skuConstructionQueryWrapper.eq("CONSTRUCTION_CODE", docEntity.getEmbryoCode());
                    skuConstructionQueryWrapper.eq("SPEC_CODE", docEntity.getStructureName());
                    if (mdmSkuConstructionRefEntityMapper.selectCount(skuConstructionQueryWrapper) == 0) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("ui.data.alert.cxKeyProduct.embryoNotInStructure"), errorNum, docEntity.getEmbryoCode(), docEntity.getStructureName());
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
                            QueryWrapper<CxKeyProduct> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("EMBRYO_CODE", docEntity.getEmbryoCode());
                            queryWrapper.eq("STRUCTURE_NAME", docEntity.getStructureName());
                            CxKeyProduct existEntity = cxKeyProductMapper.selectOne(queryWrapper);
                            if (existEntity != null) {
                                docEntity.setId(existEntity.getId());
                                checkList.add(docEntity);
                            }
                        } else {
                            isCan = false;
                            String notUniqueMsg = I18nUtil.getMessage("ui.data.alert.cxKeyProduct.notUnique.withRow");
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
        List<CxKeyProduct> insertList = importList.stream()
                .filter(entity -> entity.getId() == null)
                .collect(Collectors.toList());
        List<CxKeyProduct> updateList = importList.stream()
                .filter(entity -> entity.getId() != null)
                .collect(Collectors.toList());

        // 批量插入
        if (CollectionUtils.isNotEmpty(insertList)) {
            cxKeyProductMapper.insertBatch(insertList);
        }

        // 批量更新
        if (CollectionUtils.isNotEmpty(updateList)) {
            cxKeyProductMapper.updateBatch(updateList);
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
