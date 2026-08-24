package com.zlt.aps.tq.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;
import com.zlt.aps.tq.mapper.TqMachineMaintenancePlanMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMachineMaintenancePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
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

    /**
     * 提供非空的单据类型对象以激活框架通用唯一性校验
     * 框架 checkUnique 依赖 getSysDocType() 非空才会执行校验，默认实现会按单据类型编码查库，
     * 本模块无对应单据类型配置，这里直接构造对象放行校验逻辑
     */
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TQ_MACHINE_MAINTENANCE_PLAN");
        return sysDocType;
    }

    /**
     * 唯一性校验字段：停机日期+机台编号+停机班次
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("downtimeDate", "machineCode", "downtimeShift");
    }

    /**
     * 校验"停机日期+机台编号+停机班次"组合唯一性
     * 框架 save() 会调用本方法但丢弃返回值，必须在检测到重复时抛出异常才能阻断保存；
     * 框架校验自动排除自身ID并过滤逻辑删除数据
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(TqMachineMaintenancePlan entity) {
        String unique = super.checkUnique(entity);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.machineMaintenance.notUnique"));
        }
        return unique;
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
        Map<String, String> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getMachineCode, (v1, v2) -> v1));

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
            String machineCode = machineNameMap.get(machineName);
            if (machineCode == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (validated.isEmpty()) {
                entity.setMachineCode(machineCode);
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        // 保存：updateSupport=true 走查-改-插（存在则更新原记录）；否则逐条校验唯一后 save
        try {
            if (updateSupport && !importList.isEmpty()) {
                // 批量预取已存在维修计划（按停机日期+机台编号+停机班次匹配），存在则更新原记录，不存在则新增
                Map<String, TqMachineMaintenancePlan> existingMap = this.loadExistingMaintenancePlanMap(importList);
                for (TqMachineMaintenancePlan excelItem : importList) {
                    TqMachineMaintenancePlan existing = existingMap.get(this.buildMaintenancePlanKey(excelItem));
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        this.setUpdateAuditFields(excelItem);
                        tqMachineMaintenancePlanMapper.updateById(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入
                        this.setInsertAuditFields(excelItem);
                        baseDao.save(excelItem);
                    }
                    successNum++;
                }
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
            log.error("导入胎圈机台维修计划异常", e);
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

    /**
     * 批量预取已存在的机台维修计划数据（导入更新模式使用）
     * 按机台编号批量查询数据库已有记录，逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 停机日期_机台编号_停机班次 -> 已存在维修计划记录 的映射
     */
    private Map<String, TqMachineMaintenancePlan> loadExistingMaintenancePlanMap(List<TqMachineMaintenancePlan> importList) {
        // 提取非空机台编号并去重，用于分批查询
        List<String> machineCodeList = importList.stream()
                .map(TqMachineMaintenancePlan::getMachineCode)
                .filter(machineCode -> !StringUtil.isEmpty(machineCode))
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(machineCodeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长
        List<TqMachineMaintenancePlan> existingList = CollUtil.split(machineCodeList, 1000).stream()
                .flatMap(batch -> tqMachineMaintenancePlanMapper.selectList(new LambdaQueryWrapper<TqMachineMaintenancePlan>()
                        .in(TqMachineMaintenancePlan::getMachineCode, batch)).stream())
                .collect(Collectors.toList());
        // 按"停机日期+机台编号+停机班次"组装映射，同键多条时保留首条
        return existingList.stream()
                .collect(Collectors.toMap(this::buildMaintenancePlanKey,
                        Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 构建"停机日期+机台编号+停机班次"组合键（停机日期格式化为yyyy-MM-dd，规避时分秒差异）
     *
     * @param entity 维修计划实体
     * @return 组合键字符串
     */
    private String buildMaintenancePlanKey(TqMachineMaintenancePlan entity) {
        return DateUtil.format(entity.getDowntimeDate(), "yyyy-MM-dd")
                + "_" + entity.getMachineCode() + "_" + entity.getDowntimeShift();
    }

    /**
     * 设置导入更新模式的更新审计字段（updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setUpdateAuditFields(TqMachineMaintenancePlan entity) {
        try {
            entity.setUpdateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            entity.setUpdateBy("system");
        }
        entity.setUpdateTime(new Date());
    }

    /**
     * 设置导入新增模式的审计字段（isDelete/createBy/createTime/updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setInsertAuditFields(TqMachineMaintenancePlan entity) {
        entity.setIsDelete(0);
        try {
            entity.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            entity.setCreateBy("system");
        }
        entity.setCreateTime(new Date());
        this.setUpdateAuditFields(entity);
    }
}
