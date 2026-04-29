package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;
import com.zlt.aps.tq.mapper.TqMachineMaintenancePlanMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMachineMaintenancePlanService;
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
public class TqMachineMaintenancePlanServiceImpl extends AbstractDocService<TqMachineMaintenancePlan> implements ITqMachineMaintenancePlanService {

    @Resource
    private TqMachineMaintenancePlanMapper tqMachineMaintenancePlanMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_MAINTENANCE_PLAN";
    }

    @Override
    public String checkUnique(TqMachineMaintenancePlan entity) {
        QueryWrapper<TqMachineMaintenancePlan> wrapper = new QueryWrapper<>();
        wrapper.ne(entity.getId() != null, "ID", entity.getId());
        wrapper.eq("DOWNTIME_DATE", entity.getDowntimeDate());
        wrapper.eq("MACHINE_ID", entity.getMachineId());
        wrapper.eq("DOWNTIME_SHIFT", entity.getDowntimeShift());
        wrapper.eq("IS_DELETE", 0);
        if (tqMachineMaintenancePlanMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("downtimeDate", "machineId", "downtimeShift");
    }

    @Override
    public void deleteAllMachineMaintenancePlan() {
        tqMachineMaintenancePlanMapper.deleteAllMachineMaintenancePlan();
    }

    @Override
    public AjaxResult importData(List<TqMachineMaintenancePlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineMaintenancePlan> importList = new ArrayList<>();

        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId, (v1, v2) -> v1));

        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getMachineName() + "_" + a.getDowntimeDate() + "_" + a.getDowntimeShift()),
                        Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqMachineMaintenancePlan entity = list.get(i);

            String groupKey = entity.getMachineName() + "_" + entity.getDowntimeDate() + "_" + entity.getDowntimeShift();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.tq.machineMaintenancePlan.column.downtimeDate")
                        + "+" + I18nUtil.getMessage("ui.tq.machineMaintenancePlan.column.machineName")
                        + "+" + I18nUtil.getMessage("ui.tq.machineMaintenancePlan.column.downtimeShift");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            String machineName = entity.getMachineName();
            Long machineId = machineNameMap.get(machineName);
            if (machineId == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (validated.isEmpty()) {
                entity.setMachineId(machineId);
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                tqMachineMaintenancePlanMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqMachineMaintenancePlan excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = tqMachineMaintenancePlanMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.machineMaintenancePlan.column.conflict"), importErrorLogs);
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
