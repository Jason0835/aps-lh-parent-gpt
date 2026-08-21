package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.mapper.GsqLossRateMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqLossRateService;
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
 * 钢丝圈损耗率管理Service实现
 * 业务规则：
 * 1. 钢丝圈编码与机台编码至少一个有值
 * 2. 损耗率必填
 * 3. "钢丝圈编码+机台编码"组合唯一
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Service
public class GsqLossRateServiceImpl extends AbstractDocService<GsqLossRate>
        implements IGsqLossRateService {

    @Resource
    private GsqLossRateMapper gsqLossRateMapper;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_LOSS_RATE";
    }

    /**
     * 唯一性校验字段：钢丝圈编码+机台编码
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("steelRingCode", "machineCode");
    }

    /**
     * 校验"钢丝圈编码+机台编码"组合唯一性
     * 同时承担业务前置校验：
     *   1. 钢丝圈编码与机台编码不能同时为空
     *   2. 损耗率必填
     * 框架在 save 时会调用本方法（参考 GsqMachineMaintenancePlanServiceImpl 实现），
     * 因此前置校验放在 checkUnique 开头即可覆盖"新增/编辑保存"与"独立校验"两条路径
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqLossRate entity) {
        // 业务前置校验1：钢丝圈编码与机台编码不能同时为空
        if (StringUtil.isEmpty(entity.getSteelRingCode()) && StringUtil.isEmpty(entity.getMachineCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.error.message.gsq.lossRate.codeMachineEmpty"));
        }
        // 业务前置校验2：损耗率必填
        if (entity.getLossRate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.error.message.gsq.lossRate.lossRateRequired"));
        }
        LambdaQueryWrapper<GsqLossRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqLossRate::getId, entity.getId());
        // 钢丝圈编码：空则按 is null 比较，非空则等值比较
        if (StringUtil.isEmpty(entity.getSteelRingCode())) {
            wrapper.isNull(GsqLossRate::getSteelRingCode);
        } else {
            wrapper.eq(GsqLossRate::getSteelRingCode, entity.getSteelRingCode());
        }
        // 机台编码：空则按 is null 比较，非空则等值比较
        if (StringUtil.isEmpty(entity.getMachineCode())) {
            wrapper.isNull(GsqLossRate::getMachineCode);
        } else {
            wrapper.eq(GsqLossRate::getMachineCode, entity.getMachineCode());
        }
        wrapper.eq(GsqLossRate::getIsDelete, "0");
        if (gsqLossRateMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     * 校验规则：
     * 1. 机台名称、损耗率必填
     * 2. 钢丝圈编码与机台编码至少一个有值（导入时按机台名称回填机台编码，故机台名称必填）
     * 3. 按"钢丝圈编码+机台编码"校验文件内重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqLossRate> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqLossRate> importList = new ArrayList<>();

        // 加载启用的钢丝圈机台信息，构建机台名称到机台编码的映射
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getMachineCode, (v1, v2) -> v1));

        // 按"钢丝圈编码+机台名称"分组，识别文件内重复数据
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (StringUtil.isEmpty(a.getSteelRingCode()) ? "" : a.getSteelRingCode())
                                + "_" + (StringUtil.isEmpty(a.getMachineName()) ? "" : a.getMachineName()),
                        Collectors.counting()));

        // 逐行校验
        for (int i = 0; i < list.size(); i++) {
            GsqLossRate entity = list.get(i);

            // 文件内重复校验
            String groupKey = (StringUtil.isEmpty(entity.getSteelRingCode()) ? "" : entity.getSteelRingCode())
                    + "_" + (StringUtil.isEmpty(entity.getMachineName()) ? "" : entity.getMachineName());
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                entity.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.gsq.lossRate.steelRingCode")
                        + "+" + I18nUtil.getMessage("ui.data.column.gsq.lossRate.machineName");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            // 字段格式校验 + 机台名称是否存在校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            // 机台名称非空时，校验是否存在对应机台编码
            String machineName = entity.getMachineName();
            String machineCode = machineNameMap.get(machineName);
            if (machineCode == null && !StringUtil.isEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            // 钢丝圈编码与机台编码至少一个有值校验（导入场景：机台名称非空→机台编码非空；钢丝圈编码可空）
            if (StringUtil.isEmpty(entity.getSteelRingCode()) && StringUtil.isEmpty(machineCode)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.gsq.lossRate.codeMachineEmpty"), validated);
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
                // 批量预取已存在损耗率（按钢丝圈编码+机台编码匹配），存在则更新原记录，不存在则新增
                Map<String, GsqLossRate> existingMap = this.loadExistingLossRateMap(importList);
                for (GsqLossRate excelItem : importList) {
                    GsqLossRate existing = existingMap.get(this.buildLossRateKey(excelItem));
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        this.setUpdateAuditFields(excelItem);
                        gsqLossRateMapper.updateById(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入
                        this.setInsertAuditFields(excelItem);
                        baseDao.save(excelItem);
                    }
                    successNum++;
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqLossRate excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqLossRateMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.gsq.lossRate.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入钢丝圈损耗率异常", e);
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
     * 批量预取已存在的损耗率数据（导入更新模式使用）
     * 按机台编码批量查询数据库已有记录（导入场景机台编码由机台名称反查必非空），
     * 逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 钢丝圈编码_机台编码 -> 已存在损耗率记录 的映射
     */
    private Map<String, GsqLossRate> loadExistingLossRateMap(List<GsqLossRate> importList) {
        // 提取非空机台编码并去重，用于分批查询
        List<String> machineCodeList = importList.stream()
                .map(GsqLossRate::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(machineCodeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长
        List<GsqLossRate> existingList = CollUtil.split(machineCodeList, 1000).stream()
                .flatMap(batch -> gsqLossRateMapper.selectList(new LambdaQueryWrapper<GsqLossRate>()
                        .in(GsqLossRate::getMachineCode, batch)).stream())
                .collect(Collectors.toList());
        // 按"钢丝圈编码+机台编码"组装映射，同键多条时保留首条
        return existingList.stream()
                .collect(Collectors.toMap(this::buildLossRateKey,
                        Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 构建"钢丝圈编码+机台编码"组合键（编码为空时按空字符串参与拼接）
     *
     * @param entity 损耗率实体
     * @return 组合键字符串
     */
    private String buildLossRateKey(GsqLossRate entity) {
        return (StringUtil.isEmpty(entity.getSteelRingCode()) ? "" : entity.getSteelRingCode())
                + "_" + (StringUtil.isEmpty(entity.getMachineCode()) ? "" : entity.getMachineCode());
    }

    /**
     * 设置导入更新模式的更新审计字段（updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setUpdateAuditFields(GsqLossRate entity) {
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
    private void setInsertAuditFields(GsqLossRate entity) {
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
