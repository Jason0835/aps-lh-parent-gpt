package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqMachineChuckMapper;
import com.zlt.aps.tq.service.ITqMachineChuckService;
import com.zlt.aps.tq.service.ITqMachineInfoService;
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
public class TqMachineChuckServiceImpl extends AbstractDocService<TqMachineChuck> implements ITqMachineChuckService {

    @Resource
    private TqMachineChuckMapper tqMachineChuckMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_CHUCK";
    }

    @Override
    public String checkUnique(TqMachineChuck machineChuck) {
        QueryWrapper<TqMachineChuck> wrapper = new QueryWrapper<>();
        wrapper.ne(machineChuck.getId() != null, "ID", machineChuck.getId());
        wrapper.eq("MACHINE_ID", machineChuck.getMachineId());
        wrapper.eq("CHUCK_CODE", machineChuck.getChuckCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqMachineChuckMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("machineId", "chuckCode");
    }

    @Override
    public void deleteAllMachineChuck() {
        tqMachineChuckMapper.deleteAllMachineChuck();
    }

    @Override
    public AjaxResult importData(List<TqMachineChuck> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineChuck> importList = new ArrayList<>();

        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId, (v1, v2) -> v1));

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getMachineName() + a.getChuckCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqMachineChuck machineChuck = list.get(i);

            Long hasValue = groupMap.get(machineChuck.getMachineName() + machineChuck.getChuckCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                machineChuck.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                String columnName2 = I18nUtil.getMessage("ui.tq.machineChuck.column.chuckCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineChuck);
            String machineName = machineChuck.getMachineName();
            Long machineId = machineNameMap.get(machineName);
            if (machineId == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                machineChuck.setMachineId(machineId);
                importList.add(machineChuck);
            } else {
                failureNum++;
                machineChuck.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqMachineChuckMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqMachineChuck excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = tqMachineChuckMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.machineChuck.column.conflict"), importErrorLogs);
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
