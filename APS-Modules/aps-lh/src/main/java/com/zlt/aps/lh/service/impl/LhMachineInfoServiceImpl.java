package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.annotation.DataImportCheck;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
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
 * 文件名称：LhMachineInfoServiceImpl.java
 * 描    述：LhMachineInfoServiceImpl硫化机台信息业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMachineInfoServiceImpl extends AbstractDocService<LhMachineInfo> implements ILhMachineInfoService {


    @Resource
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0114";
    }

    /**
     * 查询硫化机台List
     * @param lhMachineInfo
     * @return
     */
    @Override
    public List<LhMachineInfo> selectList(LhMachineInfo  lhMachineInfo){
        LambdaQueryWrapper<LhMachineInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getFactoryCode()), LhMachineInfo::getFactoryCode, lhMachineInfo.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getStatus()), LhMachineInfo::getStatus, lhMachineInfo.getStatus());
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getMachineCode()), LhMachineInfo::getMachineCode, lhMachineInfo.getMachineCode());
        List<LhMachineInfo> lhMachineInfoList = lhMachineInfoEntityMapper.selectList(queryWrapper);
        return lhMachineInfoList;
    }

    /**
     * 根据条件式查询
     * @param queryWrapper
     * @return
     */
    @Override
    public List<LhMachineInfo> selectListExportData(QueryWrapper<LhMachineInfo> queryWrapper){
        return lhMachineInfoEntityMapper.selectList(queryWrapper);
    }

    /**
     * 查询机台信息
     * @param factoryCode
     * @param machineCode
     * @return
     */
    @Override
    public LhMachineInfo selectOneByMachineCode(String factoryCode,String machineCode){
        LambdaQueryWrapper<LhMachineInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(factoryCode), LhMachineInfo::getFactoryCode,factoryCode);
        queryWrapper.eq(StringUtils.isNotBlank(machineCode), LhMachineInfo::getMachineCode, machineCode);
        LhMachineInfo machineInfo = lhMachineInfoEntityMapper.selectOne(queryWrapper);
        return machineInfo;
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0114");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhMachineInfo lhMachineInfo) {
        LambdaQueryWrapper<LhMachineInfo> lqw = Wrappers.lambdaQuery();
        lqw.ne(lhMachineInfo.getId() != null, LhMachineInfo::getId, lhMachineInfo.getId());
        lqw.eq(lhMachineInfo.getMachineCode() != null, LhMachineInfo::getMachineCode, lhMachineInfo.getMachineCode());
        lqw.eq(lhMachineInfo.getFactoryCode() != null, LhMachineInfo::getFactoryCode, lhMachineInfo.getFactoryCode());
        if (lhMachineInfoEntityMapper.selectCount(lqw) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "machineCode"));
    }


    @DataImportCheck(
            maxCount = 2000,
            messageKey = "ui.data.import.count.exceed",
            params = {"#list.size()", "2000"}
    )


    @Override
    public AjaxResult importData(List<LhMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // 第一轮：校验注解必填、数值范围和 Excel 内唯一键重复。
        for (int i = 0; i < list.size(); i++) {
            int errorRowNum = i + 2;
            LhMachineInfo docEntity = list.get(i);
            List<ImportErrorLog> validated = validateImportRow(importLogId, errorRowNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        List<LhMachineInfo> canImportList = list.stream()
                .filter(item -> item.getId() == null || !Long.valueOf(-999L).equals(item.getId()))
                .collect(Collectors.toList());
        // 第二轮前批量预取数据库已存在数据，避免在逐行导入时按行查询。
        Map<String, LhMachineInfo> existMachineMap = buildExistMachineMap(canImportList);

        for (int i = 0; i < list.size(); i++) {
            int errorRowNum = i + 2;
            LhMachineInfo docEntity = list.get(i);
            if (docEntity.getId() != null && Long.valueOf(-999L).equals(docEntity.getId())) {
                continue;
            }

            String uniqueKey = buildUniqueKey(docEntity.getFactoryCode(), docEntity.getMachineCode());
            LhMachineInfo existMachine = existMachineMap.get(uniqueKey);
            try {
                // 数据库不存在相同工厂+机台编号时，按新增处理。
                if (existMachine == null) {
                    lhMachineInfoEntityMapper.insert(docEntity);
                    successNum++;
                    continue;
                }

                // 勾选“更新已存在数据”时，复用原记录ID执行覆盖更新。
                if (updateSupport) {
                    docEntity.setId(existMachine.getId());
                    lhMachineInfoEntityMapper.updateById(docEntity);
                    successNum++;
                } else {
                    // 未勾选更新时，已存在数据不允许导入，返回明确的业务唯一键提示。
                    failureNum++;
                    String message = String.format(I18nUtil.getMessage("ui.data.column.lhMachineInfo.import.exist"),
                            docEntity.getFactoryCode(), docEntity.getMachineCode());
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
    protected List<ImportErrorLog> validateImportRow(Long importLogId, int errorRowNum, LhMachineInfo docEntity) {
        return ImportExcelValidatedUtils.validated(importLogId, errorRowNum, docEntity);
    }

    /**
     * 批量查询导入数据中已经存在的硫化机台。
     *
     * @param list 可继续导入的数据
     * @return 已存在机台Map
     */
    private Map<String, LhMachineInfo> buildExistMachineMap(List<LhMachineInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<String, LhMachineInfo> existMachineMap = new HashMap<>(16);
        // 先按工厂分组，再按机台编号分批查询，兼容数据库IN条件长度限制。
        Map<String, List<LhMachineInfo>> factoryCodeMap = list.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode()) && StringUtils.isNotBlank(item.getMachineCode()))
                .collect(Collectors.groupingBy(LhMachineInfo::getFactoryCode));

        for (Map.Entry<String, List<LhMachineInfo>> entry : factoryCodeMap.entrySet()) {
            String factoryCode = entry.getKey();
            List<String> machineCodeList = entry.getValue().stream()
                    .map(LhMachineInfo::getMachineCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 900);
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<LhMachineInfo> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(LhMachineInfo::getFactoryCode, factoryCode);
                wrapper.in(LhMachineInfo::getMachineCode, codeList);
                List<LhMachineInfo> existList = lhMachineInfoEntityMapper.selectList(wrapper);
                if (CollectionUtils.isNotEmpty(existList)) {
                    // Map键和导入行唯一键保持一致，后续逐行只做内存匹配。
                    existMachineMap.putAll(existList.stream()
                            .collect(Collectors.toMap(item -> buildUniqueKey(item.getFactoryCode(), item.getMachineCode()),
                                    item -> item, (oldValue, newValue) -> oldValue)));
                }
            }
        }
        return existMachineMap;
    }

    /**
     * 创建工厂+机台编号唯一键。
     *
     * @param factoryCode 工厂编号
     * @param machineCode 机台编号
     * @return 唯一键
     */
    private String buildUniqueKey(String factoryCode, String machineCode) {
        return factoryCode + "," + machineCode;
    }


}
