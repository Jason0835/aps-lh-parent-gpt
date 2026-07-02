package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineMaintenancePlan;
import com.zlt.aps.gsq.mapper.GsqMachineMaintenancePlanMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqMachineMaintenancePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        // 保存：updateSupport=true 走 mergeSql（存在则更新）；否则逐条校验唯一后 save
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                gsqMachineMaintenancePlanMapper.mergeSql(importList);
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
}
