package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎圈机台寸口对应Service实现
 * <p>唯一维度：机台编码+寸口编码（表存在唯一索引 UK_TQ_MACHINE_CHUCK 兜底），
 * 因唯一索引被逻辑删除记录占用，新增/导入时对同维度已删除记录做复活处理</p>
 */
@Slf4j
@Service
public class TqMachineChuckServiceImpl extends AbstractDocService<TqMachineChuck> implements ITqMachineChuckService {

    /**
     * 唯一维度键分隔符（机台编码与寸口编码拼接用）
     */
    private static final String UNIQUE_KEY_SEPARATOR = "|";

    @Resource
    private TqMachineChuckMapper tqMachineChuckMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_CHUCK";
    }

    /**
     * 唯一性校验字段：机台编码+寸口编码组合
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("machineCode", "chuckCode");
    }

    /**
     * 校验机台编码+寸口编码组合唯一性（仅校验有效数据，已逻辑删除记录不参与）
     *
     * @param machineChuck 机台寸口对象（编辑时携带ID排除自身）
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(TqMachineChuck machineChuck) {
        LambdaQueryWrapper<TqMachineChuck> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(machineChuck.getId() != null, TqMachineChuck::getId, machineChuck.getId());
        wrapper.eq(TqMachineChuck::getMachineCode, machineChuck.getMachineCode());
        wrapper.eq(TqMachineChuck::getChuckCode, machineChuck.getChuckCode());
        if (tqMachineChuckMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 保存机台寸口对应（带唯一性校验与已删除记录复活处理）
     * <p>处理规则：
     * 1. 机台编码+寸口编码组合在有效数据内必须唯一，不唯一直接返回错误；
     * 2. 新增时若存在同维度的已逻辑删除记录，复活该记录并全覆盖业务字段
     *    （唯一索引被已删除记录占用，直接插入会撞唯一键）；
     * 3. 更新时补齐审计字段后直接保存。</p>
     *
     * @param machineChuck 机台寸口对象（id为空新增，id不为空修改）
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveWithCheck(TqMachineChuck machineChuck) {
        // 1. 组合唯一性校验（有效数据内）
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(machineChuck))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.machineChuck.column.conflict"));
        }
        boolean isNew = machineChuck.getId() == null;
        String username = this.getCurrentUsername();
        Date now = new Date();
        if (isNew) {
            // 2. 新增前检查同维度已删除记录（含已删除数据的查询需绕过框架逻辑删除过滤，走自定义SQL）
            TqMachineChuck deleted = this.getDeletedByUniqueKey(machineChuck.getMachineCode(), machineChuck.getChuckCode());
            if (deleted != null) {
                // 复活已删除记录并覆盖业务字段
                machineChuck.setId(deleted.getId());
                machineChuck.setUpdateBy(username);
                tqMachineChuckMapper.reviveMachineChuck(machineChuck);
                return AjaxResult.success();
            }
            machineChuck.setCreateBy(username);
            machineChuck.setCreateTime(now);
        } else {
            machineChuck.setUpdateBy(username);
            machineChuck.setUpdateTime(now);
        }
        // 3. 保存（id为空新增，id不为空更新，由框架baseDao.save内部判断）
        this.save(machineChuck);
        return AjaxResult.success();
    }

    /**
     * 删除全部机台寸口对应（逻辑删除）
     */
    @Override
    public void deleteAllMachineChuck() {
        tqMachineChuckMapper.deleteAllMachineChuck();
    }

    /**
     * 导入机台寸口对应数据
     * <p>处理规则：
     * 1. 第一轮校验：Excel内重复校验（机台名称+寸口编码）、注解基础校验、
     *    机台名称必填、机台名称反显机台编码（主数据存在性校验）；
     * 2. 已存在数据批量预取（含已逻辑删除记录，同维度已删除记录占用唯一索引）；
     * 3. 第二轮分流（维度键=机台编码+寸口编码）：
     *    已存在且勾选更新 -> 按维度键全覆盖更新（含空值覆盖）；
     *    已存在且未勾选更新 -> 记失败日志并继续下一行；
     *    仅存在已删除记录 -> 复活并覆盖（视为新增成功）；
     *    完全不存在 -> 批量插入。</p>
     *
     * @param list          导入数据
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志ID
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<TqMachineChuck> list, boolean updateSupport, Long importLogId) {
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 待新增/待更新/待复活的记录集合
        List<TqMachineChuck> insertList = new ArrayList<>();
        List<TqMachineChuck> updateList = new ArrayList<>();
        List<TqMachineChuck> reviveList = new ArrayList<>();

        // 1. 机台主数据批量预取（机台名称 -> 机台编码）
        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineNameMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getMachineCode, (v1, v2) -> v1));

        // 2. Excel内重复校验分组（机台名称+寸口编码组合）
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getMachineName() + a.getChuckCode()), Collectors.counting()));

        // 3. 第一轮：基础校验 + 机台名称反显机台编码（保留原始Excel行号供错误日志定位）
        List<ImportRow> canCheckList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            TqMachineChuck machineChuck = list.get(i);
            int rowNum = i + 2;
            // 3.1 Excel内重复校验
            Long hasValue = groupMap.get(machineChuck.getMachineName() + machineChuck.getChuckCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                String columnName2 = I18nUtil.getMessage("ui.tq.machineChuck.column.chuckCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, rowNum, message, importErrorLogs);
                continue;
            }
            // 3.2 注解基础校验（必填/长度/编码格式）
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, rowNum, machineChuck);
            // 3.3 机台名称必填校验（机台编码是唯一维度成员，名称为空无法反显编码）
            if (StringUtil.isEmpty(machineChuck.getMachineName())) {
                failureNum++;
                addImportErrorLog(importLogId, rowNum,
                        I18nUtil.getMessage("ui.tq.machineChuck.column.machineNameRequired"), importErrorLogs);
                continue;
            }
            // 3.4 机台名称反显机台编码（主数据存在性校验）
            String machineCode = machineNameMap.get(machineChuck.getMachineName());
            if (machineCode == null) {
                addImportErrorLog(importLogId, rowNum,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }
            machineChuck.setMachineCode(machineCode);
            canCheckList.add(new ImportRow(rowNum, machineChuck));
        }

        // 4. 已存在数据批量预取（含已逻辑删除记录，按机台编码集合一次性查询）
        Map<String, TqMachineChuck> activeMap = new HashMap<>();
        Map<String, TqMachineChuck> deletedMap = new HashMap<>();
        List<String> machineCodeList = canCheckList.stream()
                .map(row -> row.getData().getMachineCode())
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineCodeList)) {
            List<TqMachineChuck> existList = tqMachineChuckMapper.listIncludeDeleted(machineCodeList);
            for (TqMachineChuck exist : existList) {
                String key = this.buildUniqueKey(exist.getMachineCode(), exist.getChuckCode());
                if (this.isDeleted(exist)) {
                    deletedMap.putIfAbsent(key, exist);
                } else {
                    activeMap.putIfAbsent(key, exist);
                }
            }
        }

        // 5. 第二轮：唯一性判断与更新/新增分流
        for (ImportRow row : canCheckList) {
            TqMachineChuck machineChuck = row.getData();
            String key = this.buildUniqueKey(machineChuck.getMachineCode(), machineChuck.getChuckCode());
            TqMachineChuck active = activeMap.get(key);
            if (active != null) {
                if (updateSupport) {
                    // 勾选更新：按维度键定位记录，全覆盖更新
                    machineChuck.setId(active.getId());
                    updateList.add(machineChuck);
                } else {
                    // 未勾选更新：已存在记失败日志，继续下一行
                    failureNum++;
                    addImportErrorLog(importLogId, row.getRowNum(),
                            I18nUtil.getMessage("ui.tq.machineChuck.column.conflict"), importErrorLogs);
                }
                continue;
            }
            TqMachineChuck deleted = deletedMap.get(key);
            if (deleted != null) {
                // 同维度仅有已删除记录：复活并覆盖（唯一索引被已删除记录占用，不能直接插入）
                machineChuck.setId(deleted.getId());
                reviveList.add(machineChuck);
            } else {
                insertList.add(machineChuck);
            }
        }

        // 6. 落库：新增批量保存，更新/复活逐条执行
        int successNum = 0;
        try {
            String username = this.getCurrentUsername();
            Date now = new Date();
            if (CollectionUtils.isNotEmpty(insertList)) {
                insertList.forEach(item -> {
                    item.setCreateBy(username);
                    item.setCreateTime(now);
                });
                baseDao.saveBatch(insertList);
            }
            for (TqMachineChuck item : updateList) {
                // 勾选更新：业务字段全覆盖（显式set支持空值覆盖）
                LambdaUpdateWrapper<TqMachineChuck> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TqMachineChuck::getId, item.getId());
                updateWrapper.set(TqMachineChuck::getChuckName, item.getChuckName());
                updateWrapper.set(TqMachineChuck::getInchSize, item.getInchSize());
                updateWrapper.set(TqMachineChuck::getRemark, item.getRemark());
                updateWrapper.set(TqMachineChuck::getUpdateBy, username);
                updateWrapper.set(TqMachineChuck::getUpdateTime, now);
                tqMachineChuckMapper.update(null, updateWrapper);
            }
            for (TqMachineChuck item : reviveList) {
                item.setUpdateBy(username);
                tqMachineChuckMapper.reviveMachineChuck(item);
            }
            successNum = insertList.size() + updateList.size() + reviveList.size();
        } catch (Exception e) {
            log.error("机台寸口导入落库异常", e);
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
     * 按唯一维度键查询同维度的已逻辑删除记录
     *
     * @param machineCode 机台编码
     * @param chuckCode   寸口编码
     * @return 已删除记录（无则返回null）
     */
    private TqMachineChuck getDeletedByUniqueKey(String machineCode, String chuckCode) {
        if (StringUtil.isEmpty(machineCode)) {
            return null;
        }
        List<TqMachineChuck> existList = tqMachineChuckMapper.listIncludeDeleted(Collections.singletonList(machineCode));
        return existList.stream()
                .filter(item -> chuckCode != null && chuckCode.equals(item.getChuckCode()))
                .filter(this::isDeleted)
                .findFirst()
                .orElse(null);
    }

    /**
     * 构建唯一维度键（机台编码|寸口编码）
     *
     * @param machineCode 机台编码
     * @param chuckCode   寸口编码
     * @return 唯一维度键
     */
    private String buildUniqueKey(String machineCode, String chuckCode) {
        return machineCode + UNIQUE_KEY_SEPARATOR + chuckCode;
    }

    /**
     * 判断记录是否已逻辑删除
     *
     * @param machineChuck 机台寸口记录
     * @return true-已删除，false-有效
     */
    private boolean isDeleted(TqMachineChuck machineChuck) {
        return machineChuck.getIsDelete() != null && !"0".equals(String.valueOf(machineChuck.getIsDelete()));
    }

    /**
     * 获取当前登录用户名，获取失败时降级为system
     * （兼容Feign调用等无登录上下文场景）
     *
     * @return 用户名
     */
    private String getCurrentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 导入行包装类（保留原始Excel行号供错误日志定位）
     */
    private static class ImportRow {

        /**
         * Excel行号（数据从第2行开始）
         */
        private final int rowNum;

        /**
         * 导入数据
         */
        private final TqMachineChuck data;

        ImportRow(int rowNum, TqMachineChuck data) {
            this.rowNum = rowNum;
            this.data = data;
        }

        public int getRowNum() {
            return rowNum;
        }

        public TqMachineChuck getData() {
            return data;
        }
    }
}
