package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMachineSpecSpeedService;
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
public class TqMachineSpecSpeedServiceImpl extends AbstractDocService<TqMachineSpecSpeed> implements ITqMachineSpecSpeedService {

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_SPEC_SPEED";
    }

    @Override
    public String checkUnique(TqMachineSpecSpeed machineSpecSpeed) {
        QueryWrapper<TqMachineSpecSpeed> wrapper = new QueryWrapper<>();
        wrapper.ne(machineSpecSpeed.getId() != null, "ID", machineSpecSpeed.getId());
        wrapper.eq("MACHINE_CODE", machineSpecSpeed.getMachineCode());
        wrapper.eq("BEAD_CODE", machineSpecSpeed.getBeadCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqMachineSpecSpeedMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("machineCode", "beadCode");
    }

    @Override
    public void deleteAll() {
        tqMachineSpecSpeedMapper.deleteAll();
    }

    @Override
    public AjaxResult importData(List<TqMachineSpecSpeed> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineSpecSpeed> importList = new ArrayList<>();

        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineCodeMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getMachineCode));

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getMachineName() + a.getBeadCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqMachineSpecSpeed machineSpecSpeed = list.get(i);

            Long hasValue = groupMap.get(machineSpecSpeed.getMachineName() + machineSpecSpeed.getBeadCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                machineSpecSpeed.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                String columnName2 = I18nUtil.getMessage("ui.tq.machineSpecSpeed.column.beadCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineSpecSpeed);
            String machineName = machineSpecSpeed.getMachineName();
            String machineCode = machineCodeMap.get(machineName);
            if (machineCode == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                machineSpecSpeed.setMachineCode(machineCode);
                importList.add(machineSpecSpeed);
            } else {
                failureNum++;
                machineSpecSpeed.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqMachineSpecSpeedMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqMachineSpecSpeed excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    if (UserConstants.UNIQUE.equals(checkUnique(excelItem))) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.machineSpecSpeed.column.conflict"), importErrorLogs);
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
