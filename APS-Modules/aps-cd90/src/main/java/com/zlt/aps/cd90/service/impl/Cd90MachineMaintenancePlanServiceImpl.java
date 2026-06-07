package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.aps.cd90.mapper.Cd90MachineMaintenancePlanMapper;
import com.zlt.aps.cd90.service.ICd90MachineMaintenancePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90MachineMaintenancePlanServiceImpl extends AbstractDocService<Cd90MachineMaintenancePlan> implements ICd90MachineMaintenancePlanService {

    @Resource
    private Cd90MachineMaintenancePlanMapper cd90MachineMaintenancePlanMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD90_MACHINE_MAINTENANCE_PLAN";
    }

    @Override
    public String checkUnique(Cd90MachineMaintenancePlan entity) {
        LambdaQueryWrapper<Cd90MachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineMaintenancePlan::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd90MachineMaintenancePlan::getDowntimeStartTime, entity.getDowntimeStartTime());
        wrapper.eq(Cd90MachineMaintenancePlan::getDowntimeEndTime, entity.getDowntimeEndTime());
        wrapper.ne(entity.getId() != null, Cd90MachineMaintenancePlan::getId, entity.getId());
        return cd90MachineMaintenancePlanMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90MachineMaintenancePlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd90MachineMaintenancePlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineMaintenancePlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineMaintenancePlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd90MachineMaintenancePlan exist = getExistMaintenance(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setDowntimeDate(docEntity.getDowntimeDate());
                exist.setDowntimeHours(docEntity.getDowntimeHours());
                exist.setRemark(docEntity.getRemark());
                cd90MachineMaintenancePlanMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd90MachineMaintenancePlan getExistMaintenance(Cd90MachineMaintenancePlan entity) {
        LambdaQueryWrapper<Cd90MachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineMaintenancePlan::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd90MachineMaintenancePlan::getDowntimeStartTime, entity.getDowntimeStartTime());
        wrapper.eq(Cd90MachineMaintenancePlan::getDowntimeEndTime, entity.getDowntimeEndTime());
        return cd90MachineMaintenancePlanMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_MACHINE_MAINTENANCE_PLAN");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode", "downtimeStartTime", "downtimeEndTime");
    }
}