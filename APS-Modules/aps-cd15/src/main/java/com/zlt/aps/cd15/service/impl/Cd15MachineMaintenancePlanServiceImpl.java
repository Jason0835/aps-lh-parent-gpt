package com.zlt.aps.cd15.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.mapper.Cd15MachineMaintenancePlanMapper;
import com.zlt.aps.cd15.service.ICd15MachineMaintenancePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 斜裁机台检修计划业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15MachineMaintenancePlanServiceImpl extends AbstractDocService<Cd15MachineMaintenancePlan> implements ICd15MachineMaintenancePlanService {

    @Resource
    private Cd15MachineMaintenancePlanMapper cd15MachineMaintenancePlanMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_MACHINE_MAINTENANCE_PLAN";
    }

    @Override
    public int save(Cd15MachineMaintenancePlan entity) {
        normalize(entity);
        return super.save(entity);
    }

    /**
     * 校验同一工厂、机台、停机开始时间、停机结束时间是否重复。
     *
     * @param entity 斜裁机台检修计划
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15MachineMaintenancePlan entity) {
        normalize(entity);
        if (!hasUniqueKey(entity)) {
            return UserConstants.UNIQUE;
        }
        LambdaQueryWrapper<Cd15MachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineMaintenancePlan::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd15MachineMaintenancePlan::getDowntimeStartTime, entity.getDowntimeStartTime());
        wrapper.eq(Cd15MachineMaintenancePlan::getDowntimeEndTime, entity.getDowntimeEndTime());
        wrapper.ne(entity.getId() != null, Cd15MachineMaintenancePlan::getId, entity.getId());
        return cd15MachineMaintenancePlanMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 校验同一工厂同一机台的检修时间段是否与已有记录重叠。
     *
     * @param entity 斜裁机台检修计划
     * @return 唯一性标识
     */
    @Override
    public String checkOverlap(Cd15MachineMaintenancePlan entity) {
        normalize(entity);
        if (!hasUniqueKey(entity)) {
            return UserConstants.UNIQUE;
        }
        LambdaQueryWrapper<Cd15MachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineMaintenancePlan::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.lt(Cd15MachineMaintenancePlan::getDowntimeStartTime, entity.getDowntimeEndTime());
        wrapper.gt(Cd15MachineMaintenancePlan::getDowntimeEndTime, entity.getDowntimeStartTime());
        wrapper.ne(entity.getId() != null, Cd15MachineMaintenancePlan::getId, entity.getId());
        return cd15MachineMaintenancePlanMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 保存前统一校验，保证导入、BootUI 和微服务直调时规则一致。
     *
     * @param entity 斜裁机台检修计划
     * @return 校验失败 AjaxResult，校验通过返回 null
     */
    @Override
    public AjaxResult validateForSave(Cd15MachineMaintenancePlan entity) {
        normalize(entity);
        if (!hasUniqueKey(entity)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.required"));
        }
        if (!isValidTimeRange(entity)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.dateRangeInvalid"));
        }
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.checkUnique"));
        }
        if (UserConstants.NOT_UNIQUE.equals(this.checkOverlap(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.timeOverlap"));
        }
        return null;
    }

    @Override
    public AjaxResult importData(List<Cd15MachineMaintenancePlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15MachineMaintenancePlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15MachineMaintenancePlan docEntity = list.get(i);
            normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (!isValidTimeRange(docEntity)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.dateRangeInvalid"), validated);
            }
            if (getExistMaintenance(docEntity) == null && UserConstants.NOT_UNIQUE.equals(this.checkOverlap(docEntity))) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.timeOverlap"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15MachineMaintenancePlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId().equals(-999L)) {
                continue;
            }
            Cd15MachineMaintenancePlan exist = getExistMaintenance(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setDowntimeDate(docEntity.getDowntimeDate());
                exist.setDowntimeHours(docEntity.getDowntimeHours());
                exist.setRemark(docEntity.getRemark());
                cd15MachineMaintenancePlanMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(uniqueMsg, errorNum), importErrorLogs);
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

    private Cd15MachineMaintenancePlan getExistMaintenance(Cd15MachineMaintenancePlan entity) {
        LambdaQueryWrapper<Cd15MachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineMaintenancePlan::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd15MachineMaintenancePlan::getDowntimeStartTime, entity.getDowntimeStartTime());
        wrapper.eq(Cd15MachineMaintenancePlan::getDowntimeEndTime, entity.getDowntimeEndTime());
        return cd15MachineMaintenancePlanMapper.selectOne(wrapper);
    }

    private boolean hasUniqueKey(Cd15MachineMaintenancePlan entity) {
        return entity != null
                && StringUtils.isNotBlank(entity.getFactoryCode())
                && StringUtils.isNotBlank(entity.getMachineCode())
                && entity.getDowntimeStartTime() != null
                && entity.getDowntimeEndTime() != null;
    }

    private boolean isValidTimeRange(Cd15MachineMaintenancePlan entity) {
        return entity != null
                && entity.getDowntimeStartTime() != null
                && entity.getDowntimeEndTime() != null
                && entity.getDowntimeEndTime().after(entity.getDowntimeStartTime());
    }

    private void normalize(Cd15MachineMaintenancePlan entity) {
        if (entity == null) {
            return;
        }
        entity.setFactoryCode(StringUtils.trimToEmpty(entity.getFactoryCode()));
        entity.setMachineCode(StringUtils.trimToEmpty(entity.getMachineCode()));
        if (entity.getDowntimeStartTime() != null) {
            entity.setDowntimeDate(DateUtil.beginOfDay(entity.getDowntimeStartTime()));
        }
        if (isValidTimeRange(entity)) {
            BigDecimal downtimeHours = BigDecimal.valueOf(entity.getDowntimeEndTime().getTime() - entity.getDowntimeStartTime().getTime())
                    .divide(BigDecimal.valueOf(60L * 60L * 1000L), 2, RoundingMode.HALF_UP);
            entity.setDowntimeHours(downtimeHours);
        } else {
            entity.setDowntimeHours(null);
        }
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_MACHINE_MAINTENANCE_PLAN");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode", "downtimeStartTime", "downtimeEndTime");
    }
}