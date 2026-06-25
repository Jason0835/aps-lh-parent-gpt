package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import com.zlt.aps.tq.mapper.TqSpecifyMachineMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqSpecifyMachineService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqSpecifyMachineServiceImpl extends AbstractDocService<TqSpecifyMachine> implements ITqSpecifyMachineService {

    @Resource
    private TqSpecifyMachineMapper tqSpecifyMachineMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_SPECIFY_MACHINE";
    }

    @Override
    public String checkUnique(TqSpecifyMachine specifyMachine) {
        QueryWrapper<TqSpecifyMachine> wrapper = new QueryWrapper<>();
        wrapper.ne(specifyMachine.getId() != null, "id", specifyMachine.getId());
        wrapper.eq("bead_code", specifyMachine.getBeadCode());
        wrapper.eq("machine_code", specifyMachine.getMachineCode());
        wrapper.eq("is_delete", 0);
        if (tqSpecifyMachineMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("beadCode", "machineCode");
    }

    @Override
    public void deleteAllSpecifyMachine() {
        tqSpecifyMachineMapper.deleteAllSpecifyMachine();
    }

    @Override
    public AjaxResult importData(List<TqSpecifyMachine> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqSpecifyMachine> importList = new ArrayList<>();

        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineCodeMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getMachineCode));

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getBeadCode() + a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqSpecifyMachine specifyMachine = list.get(i);

            Long hasValue = groupMap.get(specifyMachine.getBeadCode() + specifyMachine.getMachineName());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                specifyMachine.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.beadCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, specifyMachine);
            String machineName = specifyMachine.getMachineName();
            String machineCode = machineCodeMap.get(machineName);
            if (machineCode == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                specifyMachine.setMachineCode(machineCode);
                importList.add(specifyMachine);
            } else {
                failureNum++;
                specifyMachine.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqSpecifyMachineMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqSpecifyMachine excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    if (UserConstants.UNIQUE.equals(checkUnique(excelItem))) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.specifyMachine.column.conflict"), importErrorLogs);
                    }
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
