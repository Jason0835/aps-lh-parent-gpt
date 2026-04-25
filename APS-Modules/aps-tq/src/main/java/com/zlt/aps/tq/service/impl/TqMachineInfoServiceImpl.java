package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqMachineInfoServiceImpl extends AbstractDocService<TqMachineInfo> implements ITqMachineInfoService {

    @Resource
    private TqMachineInfoMapper tqMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_INFO";
    }

    @Override
    public String checkUnique(TqMachineInfo machineInfo) {
        QueryWrapper<TqMachineInfo> wrapper = new QueryWrapper<>();
        wrapper.ne(machineInfo.getId() != null, "ID", machineInfo.getId());
        wrapper.eq("MACHINE_CODE", machineInfo.getMachineCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqMachineInfoMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public String checkMachineCodeUnique(TqMachineInfo machineInfo) {
        QueryWrapper<TqMachineInfo> wrapper = new QueryWrapper<>();
        wrapper.ne(machineInfo.getId() != null, "ID", machineInfo.getId());
        wrapper.eq("MACHINE_CODE", machineInfo.getMachineCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqMachineInfoMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo) {
        return tqMachineInfoMapper.listMachineInfo(machineInfo);
    }

    @Override
    public List<TqMachineInfo> selectMachineInfoList(TqMachineInfo machineInfo) {
        return tqMachineInfoMapper.selectMachineInfoList(machineInfo);
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("machineCode");
    }

    @Override
    public AjaxResult importData(List<TqMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineInfo> importList = new ArrayList<>();

        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqMachineInfo machineInfo = list.get(i);

            Long hasValue = groupMap.get(machineInfo.getMachineCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                machineInfo.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineInfo);

            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }
            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf("2") >= 0) {
                if (com.ruoyi.common.utils.StringUtils.isNotBlank(machineInfo.getOpenMachineClass()) && machineInfo.getOpenMachineClass().indexOf("1") >= 0) {
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.machine.ClassShiftMapValidate"), validated);
                }
            }
            Long hasNameValue = nameMap.get(machineInfo.getMachineName());
            if (hasNameValue != null && hasNameValue > 1) {
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord4Name");
                addImportErrorLog(importLogId, i + 2, message, validated);
            }

            if (CollectionUtils.isEmpty(validated)) {
                Boolean hasFalse = false;
                TqMachineInfo query = new TqMachineInfo();
                if (updateSupport) {
                    query.setMachineCode(machineInfo.getMachineCode());
                    query.setMachineName(machineInfo.getMachineName());
                    List<TqMachineInfo> exist2 = tqMachineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                } else {
                    query.setMachineCode(machineInfo.getMachineCode());
                    List<TqMachineInfo> exist1 = tqMachineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(machineInfo.getMachineName());
                    List<TqMachineInfo> exist2 = tqMachineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }
                if (hasFalse) {
                    machineInfo.setId(-999L);
                    failureNum++;
                    continue;
                }

                importList.add(machineInfo);
            } else {
                failureNum++;
                machineInfo.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqMachineInfoMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqMachineInfo excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    successNum++;
                    baseDao.save(excelItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
