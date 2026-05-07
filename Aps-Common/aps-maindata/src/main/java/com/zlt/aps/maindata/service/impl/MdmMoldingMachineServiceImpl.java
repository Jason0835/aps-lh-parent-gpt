package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineServiceImpl.java
 * 描    述：MdmMoldingMachineServiceImpl基础数据-成型机档案业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMoldingMachineServiceImpl extends AbstractDocService<MdmMoldingMachine> implements IMdmMoldingMachineService {

    @Resource
    private MdmMoldingMachineEntityMapper mdmMoldingMachineEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0138";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0138");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMoldingMachine docEntityVO) {
        // 唯一键字段为空时不做唯一性查询，避免退化成全表重复判断；空值由必填校验处理。
        if (!StringUtils.isNotBlank(docEntityVO.getFactoryCode()) || !StringUtils.isNotBlank(docEntityVO.getCxMachineCode())) {
            return UserConstants.UNIQUE;
        }
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(docEntityVO.getId() != null, MdmMoldingMachine::getId, docEntityVO.getId());
        wrapper.eq(MdmMoldingMachine::getFactoryCode, docEntityVO.getFactoryCode());
        wrapper.eq(MdmMoldingMachine::getCxMachineCode, docEntityVO.getCxMachineCode());
        if (mdmMoldingMachineEntityMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "cxMachineCode"));
    }

    /**
     * 导入基础数据-成型机档案数据。
     *
     * @param list 导入数据
     * @param updateSupport 是否更新已经存在的数据
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<MdmMoldingMachine> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // 第一轮：校验注解必填、字段格式和Excel内工厂+成型机编码重复。
        for (int i = 0; i < list.size(); i++) {
            int errorRowNum = i + 2;
            MdmMoldingMachine docEntity = list.get(i);
            List<ImportErrorLog> validated = validateImportRow(importLogId, errorRowNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        List<MdmMoldingMachine> canImportList = list.stream()
                .filter(item -> item.getId() == null || !Long.valueOf(-999L).equals(item.getId()))
                .collect(Collectors.toList());
        // 第二轮前批量预取数据库已存在数据，避免循环内逐行查询。
        Map<String, MdmMoldingMachine> existMachineMap = buildExistMachineMap(canImportList);

        for (int i = 0; i < list.size(); i++) {
            int errorRowNum = i + 2;
            MdmMoldingMachine docEntity = list.get(i);
            if (docEntity.getId() != null && Long.valueOf(-999L).equals(docEntity.getId())) {
                continue;
            }

            String uniqueKey = buildUniqueKey(docEntity.getFactoryCode(), docEntity.getCxMachineCode());
            MdmMoldingMachine existMachine = existMachineMap.get(uniqueKey);
            try {
                // 数据库不存在相同工厂+成型机编码时，按新增处理。
                if (existMachine == null) {
                    mdmMoldingMachineEntityMapper.insert(docEntity);
                    successNum++;
                    continue;
                }

                // 勾选“更新已存在数据”时，复用原记录ID执行覆盖更新。
                if (updateSupport) {
                    docEntity.setId(existMachine.getId());
                    mdmMoldingMachineEntityMapper.updateById(docEntity);
                    successNum++;
                } else {
                    // 未勾选更新时，已存在数据不允许导入，返回明确的业务唯一键提示。
                    failureNum++;
                    String message = String.format(I18nUtil.getMessage("ui.data.column.mdmMoldingMachine.import.exist"),
                            docEntity.getFactoryCode(), docEntity.getCxMachineCode());
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorRowNum, message, importErrorLogs);
                }
            } catch (Exception e) {
                // 单行落库异常记录到导入明细，继续处理后续行，最终统一返回成功/失败数量。
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, e.getMessage(), importErrorLogs);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 校验单行导入数据。
     *
     * @param importLogId 导入日志ID
     * @param errorRowNum Excel行号
     * @param docEntity 导入行数据
     * @return 错误日志
     */
    protected List<ImportErrorLog> validateImportRow(Long importLogId, int errorRowNum, MdmMoldingMachine docEntity) {
        return ImportExcelValidatedUtils.validated(importLogId, errorRowNum, docEntity);
    }

    /**
     * 批量查询导入数据中已经存在的成型机档案。
     *
     * @param list 可继续导入的数据
     * @return 已存在成型机档案Map
     */
    private Map<String, MdmMoldingMachine> buildExistMachineMap(List<MdmMoldingMachine> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<String, MdmMoldingMachine> existMachineMap = new HashMap<>(16);
        // 先按工厂分组，再按成型机编码分批查询，兼容数据库IN条件长度限制。
        Map<String, List<MdmMoldingMachine>> factoryCodeMap = list.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode()) && StringUtils.isNotBlank(item.getCxMachineCode()))
                .collect(Collectors.groupingBy(MdmMoldingMachine::getFactoryCode));

        for (Map.Entry<String, List<MdmMoldingMachine>> entry : factoryCodeMap.entrySet()) {
            String factoryCode = entry.getKey();
            List<String> machineCodeList = entry.getValue().stream()
                    .map(MdmMoldingMachine::getCxMachineCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 900);
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<MdmMoldingMachine> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(MdmMoldingMachine::getFactoryCode, factoryCode);
                wrapper.in(MdmMoldingMachine::getCxMachineCode, codeList);
                List<MdmMoldingMachine> existList = mdmMoldingMachineEntityMapper.selectList(wrapper);
                if (CollectionUtils.isNotEmpty(existList)) {
                    // Map键和导入行唯一键保持一致，后续逐行只做内存匹配。
                    existMachineMap.putAll(existList.stream()
                            .collect(Collectors.toMap(item -> buildUniqueKey(item.getFactoryCode(), item.getCxMachineCode()),
                                    item -> item, (oldValue, newValue) -> oldValue)));
                }
            }
        }
        return existMachineMap;
    }

    /**
     * 创建工厂+成型机编码唯一键。
     *
     * @param factoryCode 工厂编号
     * @param cxMachineCode 成型机编码
     * @return 唯一键
     */
    private String buildUniqueKey(String factoryCode, String cxMachineCode) {
        return factoryCode + "," + cxMachineCode;
    }

}
