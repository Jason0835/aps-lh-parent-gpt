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
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;
import com.zlt.aps.gsq.mapper.GsqSpecifyMachineMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqSpecifyMachineService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import cn.hutool.core.collection.CollUtil;
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
 * 钢丝圈定点机台Service实现
 * 唯一性校验字段：钢丝圈代码 + 生产线（机台编码）
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Service
public class GsqSpecifyMachineServiceImpl extends AbstractDocService<GsqSpecifyMachine>
        implements IGsqSpecifyMachineService {

    @Resource
    private GsqSpecifyMachineMapper gsqSpecifyMachineMapper;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_SPECIFY_MACHINE";
    }

    /**
     * 唯一性校验字段：钢丝圈代码 + 生产线
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("steelRingCode", "machineCode");
    }

    /**
     * 校验"钢丝圈代码+生产线"组合唯一性
     * 框架已自动过滤逻辑删除数据，无需手动追加 IS_DELETE 条件
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqSpecifyMachine entity) {
        LambdaQueryWrapper<GsqSpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqSpecifyMachine::getId, entity.getId());
        wrapper.eq(GsqSpecifyMachine::getSteelRingCode, entity.getSteelRingCode());
        wrapper.eq(GsqSpecifyMachine::getMachineCode, entity.getMachineCode());
        wrapper.eq(GsqSpecifyMachine::getIsDelete, "0");
        if (gsqSpecifyMachineMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     * 校验规则：钢丝圈代码、生产线必填；按"钢丝圈代码+生产线"校验重复
     * 导入时生产线按"机台名称"录入，需根据机台名称反查机台编码
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqSpecifyMachine> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqSpecifyMachine> importList = new ArrayList<>();

        // 加载启用的钢丝圈机台信息，构建机台名称到机台编码的映射
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getMachineCode, (v1, v2) -> v1));

        // 按"钢丝圈代码+生产线名称"分组，识别文件内重复数据
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getSteelRingCode() + "_" + a.getMachineName()),
                        Collectors.counting()));

        // 逐行校验
        for (int i = 0; i < list.size(); i++) {
            GsqSpecifyMachine entity = list.get(i);

            // 文件内重复校验
            String groupKey = entity.getSteelRingCode() + "_" + entity.getMachineName();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.gsq.specifyMachine.steelRingCode")
                        + "+" + I18nUtil.getMessage("ui.data.column.gsq.specifyMachine.machineName");
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
                // 批量预取已存在定点机台（按钢丝圈代码+机台编码匹配），存在则更新原记录，不存在则新增
                Map<String, GsqSpecifyMachine> existingMap = this.loadExistingSpecifyMachineMap(importList);
                for (GsqSpecifyMachine excelItem : importList) {
                    GsqSpecifyMachine existing = existingMap.get(
                            excelItem.getSteelRingCode() + "_" + excelItem.getMachineCode());
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        this.setUpdateAuditFields(excelItem);
                        gsqSpecifyMachineMapper.updateById(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入
                        this.setInsertAuditFields(excelItem);
                        baseDao.save(excelItem);
                    }
                    successNum++;
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqSpecifyMachine excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqSpecifyMachineMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.gsq.specifyMachine.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入钢丝圈定点机台异常", e);
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
     * 批量预取已存在的定点机台数据（导入更新模式使用）
     * 按"钢丝圈代码+机台编码"批量查询数据库已有记录，逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 钢丝圈代码_机台编码 -> 已存在定点机台记录 的映射
     */
    private Map<String, GsqSpecifyMachine> loadExistingSpecifyMachineMap(List<GsqSpecifyMachine> importList) {
        // 提取非空钢丝圈代码并去重，用于分批查询
        List<String> codeList = importList.stream()
                .map(GsqSpecifyMachine::getSteelRingCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(codeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长
        List<GsqSpecifyMachine> existingList = CollUtil.split(codeList, 1000).stream()
                .flatMap(batch -> gsqSpecifyMachineMapper.selectList(new LambdaQueryWrapper<GsqSpecifyMachine>()
                        .in(GsqSpecifyMachine::getSteelRingCode, batch)).stream())
                .collect(Collectors.toList());
        // 按"钢丝圈代码+机台编码"组装映射，同键多条时保留首条
        return existingList.stream()
                .collect(Collectors.toMap(
                        sm -> sm.getSteelRingCode() + "_" + sm.getMachineCode(),
                        Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 设置导入更新模式的更新审计字段（updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setUpdateAuditFields(GsqSpecifyMachine entity) {
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
    private void setInsertAuditFields(GsqSpecifyMachine entity) {
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
