package com.zlt.aps.mp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.mdm.mapper.FactoryNoProductionMapper;
import com.zlt.aps.mp.mdm.service.IFactoryNoProductionService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryNoProductionServiceImpl.java
 * 描    述：FactoryNoProductionServiceImpl基础数据-分厂不排产业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryNoProductionServiceImpl extends ServiceImpl<FactoryNoProductionMapper, FactoryNoProduction> implements IFactoryNoProductionService {

    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final FactoryNoProductionMapper factoryNoProductionMapper;

    @Override
    public List<FactoryNoProduction> selectFactoryNoProductionList(FactoryNoProduction factoryNoProduction) {
        return getBaseMapper().selectFactoryNoProductionList(factoryNoProduction);
    }

    @Override
    public String checkFactoryNotProductionUnique(FactoryNoProduction factoryNoProduction) {
        if (factoryNoProduction == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<FactoryNoProduction> list = getBaseMapper().selectFactoryNoProductionList(factoryNoProduction);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public boolean save(FactoryNoProduction noProduction) {
        String materialCode = noProduction.getMaterialCode();
        String factoryCode = noProduction.getFactoryCode();
        QueryWrapper<MdmMaterialInfo> productQuery = new QueryWrapper<>();
        productQuery.eq("FACTORY_CODE", factoryCode);
        productQuery.eq("MATERIAL_CODE", materialCode);
        productQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MdmMaterialInfo> productInfoList = productInfoEntityMapper.selectList(productQuery);
        if (CollectionUtils.isEmpty(productInfoList)) {
            return super.save(noProduction);
        }
        noProduction.setMaterialDesc(productInfoList.get(0).getMaterialDesc());
        return super.save(noProduction);
    }

    /**
     * 导入不排产设定配置
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    public AjaxResult importData(List<FactoryNoProduction> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<FactoryNoProduction> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String docFactoryNotProductionExistError = I18nUtil.getMessage("ui.data.column.factoryNoProduction.docFactoryNotProductionExist");
        String productCodeError = I18nUtil.getMessage("ui.data.text.confMinProd.productCodeError");

        //过滤掉重复数据
        // 创建一个Set来存储不重复的键
        Set<String> uniqueKeys = new LinkedHashSet<>();
        // 创建一个新列表来保存无重复的数据
        List<FactoryNoProduction> filteredList = new ArrayList<>();
        for (FactoryNoProduction entity : list) {
            // 构建一个基于特定字段的唯一键
            String uniqueKey = entity.getYear() + "-" + entity.getMonth() + "-" + entity.getFactoryCode() + "-" + entity.getMaterialCode();
            // 如果Set中不存在此键，则表示数据未重复，可以添加到新列表中
            if (uniqueKeys.add(uniqueKey)) {
                filteredList.add(entity);
            }
        }
        // 替换原有list
        list = filteredList;
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            FactoryNoProduction info = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, info);
            //查询已存在的分厂不排除 数据
            List<FactoryNoProduction> exists = getBaseMapper().selectFactoryNoProductionList(info);
            if (CollectionUtils.isNotEmpty(exists)) {
                int rowIndex = i + 2;
                String formattedRowIndex;
                if ("第%s行".equals(rowCountStr)) {
                    // 中文环境下
                    formattedRowIndex = String.format("第%s行", rowIndex);
                } else {
                    // 英文环境下
                    formattedRowIndex = String.format("Row %d", rowIndex);
                }

                String errorMessage = formattedRowIndex + docFactoryNotProductionExistError;
                addImportErrorLog(importLogId, errorNum, errorMessage, validated);
            }
            //查询和赋值物料号
            MdmMaterialInfo productInfo = productInfoEntityMapper.selectByProductCode(info.getMaterialCode());
            if (productInfo == null) {
                failureNum++;
                String message = String.format(rowCountStr, i + 2) + productCodeError;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(), i + 2,
                        message, importErrorLogs);
                continue;
            } else {
                info.setMaterialDesc(productInfo.getMaterialDesc());
            }

            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(validated)) {
                info.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                importList.add(info);
            }
        }

        try {
            if (CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                FactoryNoProduction first = importList.get(0);
                String factoryCode = first.getFactoryCode();
                Integer year = first.getYear();
                Integer month = first.getMonth();
                QueryWrapper<FactoryNoProduction> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("FACTORY_CODE", factoryCode);
                queryWrapper.eq("YEAR", year);
                queryWrapper.eq("MONTH", month);
                queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
                List<FactoryNoProduction> dataList = factoryNoProductionMapper.selectList(queryWrapper);
                if (CollectionUtils.isEmpty(dataList)) {
                    saveBatch(importList);
                } else {
                    Set<String> productCodeSet = dataList.stream().map(FactoryNoProduction::getMaterialCode).collect(Collectors.toSet());
                    List<FactoryNoProduction> insertList = new ArrayList<>();
                    importList.stream().forEach(importData -> {
                        if (!productCodeSet.contains(importData.getMaterialCode())) {
                            insertList.add(importData);
                        }
                    });
                    if (!CollectionUtils.isEmpty(insertList)) {
                        saveBatch(insertList);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
