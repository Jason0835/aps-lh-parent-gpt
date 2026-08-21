package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineMaintenancePlan;
import com.zlt.aps.gsq.mapper.GsqMachineMaintenancePlanMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqMachineMaintenancePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈机台维修计划Service实现
 *
 * @author zlt
 * @date 2026-07-01
 */
@Slf4j
@Service
public class GsqMachineMaintenancePlanServiceImpl extends AbstractDocService<GsqMachineMaintenancePlan>
        implements IGsqMachineMaintenancePlanService {

    @Resource
    private GsqMachineMaintenancePlanMapper gsqMachineMaintenancePlanMapper;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_MACHINE_MAINTENANCE_PLAN";
    }

    /**
     * 唯一性校验字段：停机日期+机台编码+停机班次
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("downtimeDate", "machineCode", "downtimeShift");
    }

    /**
     * 校验"停机日期+机台编码+停机班次"组合唯一性
     * 框架已自动过滤逻辑删除数据，无需手动追加 IS_DELETE 条件
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqMachineMaintenancePlan entity) {
        LambdaQueryWrapper<GsqMachineMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqMachineMaintenancePlan::getId, entity.getId());
        wrapper.eq(GsqMachineMaintenancePlan::getDowntimeDate, entity.getDowntimeDate());
        wrapper.eq(GsqMachineMaintenancePlan::getMachineCode, entity.getMachineCode());
        wrapper.eq(GsqMachineMaintenancePlan::getDowntimeShift, entity.getDowntimeShift());
        wrapper.eq(GsqMachineMaintenancePlan::getIsDelete, "0");
        if (gsqMachineMaintenancePlanMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     * 校验规则：机台名称、停机日期、停机班次必填；按"停机日期+机台编码+停机班次"校验重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqMachineMaintenancePlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqMachineMaintenancePlan> importList = new ArrayList<>();

        // 加载启用的钢丝圈机台信息，构建机台名称到机台编码的映射
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getMachineCode, (v1, v2) -> v1));

        // 按机台名称+停机日期+停机班次分组，识别文件内重复数据
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getMachineName() + "_" + a.getDowntimeDate() + "_" + a.getDowntimeShift()),
                        Collectors.counting()));

        // 逐行校验
        for (int i = 0; i < list.size(); i++) {
            GsqMachineMaintenancePlan entity = list.get(i);

            // 文件内重复校验
            String groupKey = entity.getMachineName() + "_" + entity.getDowntimeDate() + "_" + entity.getDowntimeShift();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.gsq.machineMaintenancePlan.downtimeDate")
                        + "+" + I18nUtil.getMessage("ui.data.column.gsq.machineMaintenancePlan.machineName")
                        + "+" + I18nUtil.getMessage("ui.data.column.gsq.machineMaintenancePlan.downtimeShift");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            // 字段格式校验 + 机台名称是否存在校验
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
                // 批量预取已存在保养计划（按停机日期+机台编码+停机班次匹配），存在则更新原记录，不存在则新增
                Map<String, GsqMachineMaintenancePlan> existingMap = this.loadExistingMaintenancePlanMap(importList);
                for (GsqMachineMaintenancePlan excelItem : importList) {
                    GsqMachineMaintenancePlan existing = existingMap.get(this.buildMaintenancePlanKey(excelItem));
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        this.setUpdateAuditFields(excelItem);
                        gsqMachineMaintenancePlanMapper.updateById(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入
                        this.setInsertAuditFields(excelItem);
                        baseDao.save(excelItem);
                    }
                    successNum++;
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqMachineMaintenancePlan excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqMachineMaintenancePlanMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.gsq.machineMaintenancePlan.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入钢丝圈机台维修计划异常", e);
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
     * 批量预取已存在的机台保养计划数据（导入更新模式使用）
     * 按机台编码批量查询数据库已有记录，逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 停机日期_机台编码_停机班次 -> 已存在保养计划记录 的映射
     */
    private Map<String, GsqMachineMaintenancePlan> loadExistingMaintenancePlanMap(List<GsqMachineMaintenancePlan> importList) {
        // 提取非空机台编码并去重，用于分批查询
        List<String> machineCodeList = importList.stream()
                .map(GsqMachineMaintenancePlan::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(machineCodeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长
        List<GsqMachineMaintenancePlan> existingList = CollUtil.split(machineCodeList, 1000).stream()
                .flatMap(batch -> gsqMachineMaintenancePlanMapper.selectList(new LambdaQueryWrapper<GsqMachineMaintenancePlan>()
                        .in(GsqMachineMaintenancePlan::getMachineCode, batch)).stream())
                .collect(Collectors.toList());
        // 按"停机日期+机台编码+停机班次"组装映射，同键多条时保留首条
        return existingList.stream()
                .collect(Collectors.toMap(this::buildMaintenancePlanKey,
                        Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 构建"停机日期+机台编码+停机班次"组合键（停机日期格式化为yyyy-MM-dd，规避时分秒差异）
     *
     * @param entity 保养计划实体
     * @return 组合键字符串
     */
    private String buildMaintenancePlanKey(GsqMachineMaintenancePlan entity) {
        return DateUtil.format(entity.getDowntimeDate(), "yyyy-MM-dd")
                + "_" + entity.getMachineCode() + "_" + entity.getDowntimeShift();
    }

    /**
     * 设置导入更新模式的更新审计字段（updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setUpdateAuditFields(GsqMachineMaintenancePlan entity) {
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
    private void setInsertAuditFields(GsqMachineMaintenancePlan entity) {
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
