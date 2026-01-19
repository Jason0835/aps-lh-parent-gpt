package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmCxMachineFixedService;
import com.zlt.aps.monthplan.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCxMachineFixedServiceImpl.java
 * 描    述：MdmCxMachineFixedServiceImpl成型固定机台业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmCxMachineFixedServiceImpl extends AbstractDocService<MdmCxMachineFixed> implements IMdmCxMachineFixedService {

    @Autowired
    private MdmMoldingMachineEntityMapper mdmMoldingMachineEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0133";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0133");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmCxMachineFixed docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(String.format(I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.notUnique"), docEntityVO.getCxMachineCode()));
        }
        // 校验固定结构1、固定结构2、固定结构3，对比不可作业结构，不可有相同
        String structureErrorMsg = I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.structureRepeat");
        String materialCodeErrorMsg = I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.skuRepeat");
        List<String> errorStructure = this.checkFixedStructure(docEntityVO);
        // 校验固定SKU，对比不可作业SKU，不可有相同
        List<String> errorMaterialCode = this.checkFixedMaterialCode(docEntityVO);
        if (CollectionUtils.isNotEmpty(errorStructure) || CollectionUtils.isNotEmpty(errorMaterialCode)) {
            structureErrorMsg = CollectionUtils.isEmpty(errorStructure) ? "" : structureErrorMsg;
            structureErrorMsg = String.format(structureErrorMsg, String.join(",", errorStructure));

            materialCodeErrorMsg = CollectionUtils.isEmpty(errorMaterialCode) ? "" : materialCodeErrorMsg;
            materialCodeErrorMsg = String.format(materialCodeErrorMsg, String.join(",", errorMaterialCode));
            throw new RuntimeException(structureErrorMsg + materialCodeErrorMsg);
        }
        return unique;
    }

    /**
     * 校验固定SKU，对比不可作业SKU，不可有相同
     *
     * @param docEntityVO 校验对象
     */
    private List<String> checkFixedMaterialCode(MdmCxMachineFixed docEntityVO) {
        String fixedMaterialCode = StringUtils.defaultIfBlank(docEntityVO.getFixedMaterialCode(), "");
        List<String> fixedMaterialCodeList = Arrays.stream(fixedMaterialCode.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());

        String disableMaterialCode = StringUtils.defaultIfBlank(docEntityVO.getDisableMaterialCode(), "");
        List<String> disableMaterialCodeList = Arrays.stream(disableMaterialCode.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(fixedMaterialCodeList) && CollectionUtils.isNotEmpty(disableMaterialCodeList)) {
            List<String> errorMaterialCode = new ArrayList<>();
            for (String materialCode : disableMaterialCodeList) {
                if (StringUtils.isNotBlank(materialCode) && fixedMaterialCodeList.contains(materialCode)) {
                    errorMaterialCode.add(materialCode);
                }
            }
            return errorMaterialCode;
        }
        return Collections.emptyList();
    }

    /**
     * 校验固定结构1、固定结构2、固定结构3，对比不可作业结构，不可有相同
     *
     * @param docEntityVO 校验对象
     */
    private List<String> checkFixedStructure(MdmCxMachineFixed docEntityVO) {
        String fixedStructure1 = StringUtils.defaultIfBlank(docEntityVO.getFixedStructure1(), "");
        List<String> fixedStructureList = Arrays.stream(fixedStructure1.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        String fixedStructure2 = StringUtils.defaultIfBlank(docEntityVO.getFixedStructure2(), "");
        fixedStructureList.addAll(Arrays.stream(fixedStructure2.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        String fixedStructure3 = StringUtils.defaultIfBlank(docEntityVO.getFixedStructure3(), "");
        fixedStructureList.addAll(Arrays.stream(fixedStructure3.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        String disableStructure = StringUtils.defaultIfBlank(docEntityVO.getDisableStructure(), "");
        List<String> disableStructureList = Arrays.stream(disableStructure.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(fixedStructureList) && CollectionUtils.isNotEmpty(disableStructureList)) {
            List<String> errorStructure = new ArrayList<>();
            for (String structure : disableStructureList) {
                if (StringUtils.isNotBlank(structure) && fixedStructureList.contains(structure)) {
                    errorStructure.add(structure);
                }
            }
            return errorStructure;
        }
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "cxMachineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmCxMachineFixed> list, List<MdmCxMachineFixed> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 查询成型机台
        List<String> machineCodeList = list.stream().map(MdmCxMachineFixed::getCxMachineCode).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineCodeList)) {
            LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmMoldingMachine::getCxMachineCode, machineCodeList);
            List<MdmMoldingMachine> moldingMachineList = mdmMoldingMachineEntityMapper.selectList(wrapper);
            Map<String, MdmMoldingMachine> mdmMoldingMachineMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(moldingMachineList)) {
                mdmMoldingMachineMap = moldingMachineList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getCxMachineCode()), Function.identity(), (v1, v2) -> v1));
            }
            serviceCheckParams.put("moldingMachineMap", mdmMoldingMachineMap);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmCxMachineFixed importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验成型机台是否存在
        if (serviceCheckParams.containsKey("moldingMachineMap")) {
            Map<String, MdmMoldingMachine> moldingMachineMap = (Map<String, MdmMoldingMachine>) serviceCheckParams.get("moldingMachineMap");
            String cxMachineCode = importDocEntity.getCxMachineCode();
            String key = GenerageMapKeyUtils.createMapKey(importDocEntity.getFactoryCode(), cxMachineCode);
            if (!moldingMachineMap.containsKey(key)) {
                String message = I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.moldingMachineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, cxMachineCode), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        // 校验固定结构1、固定结构2、固定结构3，对比不可作业结构，不可有相同
        String structureErrorMsg = I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.structureRepeat");
        String materialCodeErrorMsg = I18nUtil.getMessage("ui.data.alert.mdmCxMachineFixed.skuRepeat");
        List<String> errorStructure = this.checkFixedStructure(importDocEntity);
        // 校验固定SKU，对比不可作业SKU，不可有相同
        List<String> errorMaterialCode = this.checkFixedMaterialCode(importDocEntity);
        if (CollectionUtils.isNotEmpty(errorStructure) || CollectionUtils.isNotEmpty(errorMaterialCode)) {
            structureErrorMsg = CollectionUtils.isEmpty(errorStructure) ? "" : structureErrorMsg;
            structureErrorMsg = String.format(structureErrorMsg, String.join(",", errorStructure));

            materialCodeErrorMsg = CollectionUtils.isEmpty(errorMaterialCode) ? "" : materialCodeErrorMsg;
            materialCodeErrorMsg = String.format(materialCodeErrorMsg, String.join(",", errorMaterialCode));
            String message = structureErrorMsg + materialCodeErrorMsg;
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        if (StringUtils.isNotBlank(structureErrorMsg) || StringUtils.isNotBlank(materialCodeErrorMsg)) {
            String message = structureErrorMsg + materialCodeErrorMsg;
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
