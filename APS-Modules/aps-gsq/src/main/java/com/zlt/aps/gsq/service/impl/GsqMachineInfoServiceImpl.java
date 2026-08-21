package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.core.utils.MachineShiftDictUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqMachineInfoDocService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈机台信息Service业务层处理
 * 继承AbstractDocService并实现新旧接口，保证对依赖模块的兼容性
 *
 * @author zlt
 * @date 2021-05-28
 */
@Slf4j
@Service
public class GsqMachineInfoServiceImpl extends AbstractDocService<GsqMachineInfo> implements IGsqMachineInfoDocService, GsqMachineInfoService {

    @Resource
    private GsqMachineInfoMapper machineInfoMapper;

    /**
     * 获取单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_MACHINE_INFO";
    }

    /**
     * 查询钢丝圈机台信息
     *
     * @param id 钢丝圈机台信息ID
     * @return 钢丝圈机台信息
     */
    @Override
    public GsqMachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询钢丝圈机台信息列表
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 钢丝圈机台信息
     */
    @Override
    public List<GsqMachineInfo> selectMachineInfoList(GsqMachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增钢丝圈机台信息
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(GsqMachineInfo machineInfo) {
        setBaseFieldValue(machineInfo, null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改钢丝圈机台信息
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(GsqMachineInfo machineInfo) {
        setBaseFieldValue(machineInfo, machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除钢丝圈机台信息
     *
     * @param ids 需要删除的钢丝圈机台信息ID
     * @return 结果
     */
    @Override
    public int deleteMachineInfoByIds(Long[] ids) {
        return machineInfoMapper.deleteMachineInfoByIds(ids);
    }

    /**
     * 校验机台编号唯一性（使用LambdaQueryWrapper，禁止手写IS_DELETE条件）
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 校验结果
     */
    @Override
    public String checkUnique(GsqMachineInfo machineInfo) {
        LambdaQueryWrapper<GsqMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(machineInfo.getId() != null, GsqMachineInfo::getId, machineInfo.getId());
        wrapper.eq(GsqMachineInfo::getMachineCode, machineInfo.getMachineCode());
        if (machineInfoMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验机台编号唯一性
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 校验结果
     */
    @Override
    public String checkMachineCodeUnique(GsqMachineInfo machineInfo) {
        LambdaQueryWrapper<GsqMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(machineInfo.getId() != null, GsqMachineInfo::getId, machineInfo.getId());
        wrapper.eq(GsqMachineInfo::getMachineCode, machineInfo.getMachineCode());
        if (machineInfoMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 机台列表
     */
    @Override
    public List<GsqMachineInfo> listMachineInfo(GsqMachineInfo machineInfo) {
        return machineInfoMapper.listMachineInfo(machineInfo);
    }

    /**
     * 获取需要校验唯一性的字段列表
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("machineCode");
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqMachineInfo> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            GsqMachineInfo machineInfo = list.get(i);

            // 开机班次：导入模板填写班次名称(如 夜班,早班，多个用英文逗号分隔)，转成字典值(01,02)入库；
            // 实体未配置dictType，框架解析层透传原始输入，此处统一转换以支持多选
            machineInfo.setOpenMachineClass(MachineShiftDictUtil.labelsToValues(machineInfo.getOpenMachineClass()));

            //重复记录校验
            Long hasValue = groupMap.get(machineInfo.getMachineCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                machineInfo.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineInfo);

            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }
            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf("2") >= 0) {
                if (StringUtils.isNotBlank(machineInfo.getOpenMachineClass()) && machineInfo.getOpenMachineClass().indexOf("1") >= 0) {
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.machine.ClassShiftMapValidate"), validated);
                }
            }
            //校验Excel机台名称唯一性
            Long hasNameValue = nameMap.get(machineInfo.getMachineName());
            if (hasNameValue != null && hasNameValue > 1) {
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord4Name");
                addImportErrorLog(importLogId, i + 2, message, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                machineInfo.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                // 唯一性校验
                Boolean hasFalse = false;
                GsqMachineInfo query = new GsqMachineInfo();
                if (updateSupport) { //勾选更新时只校验机台名称
                    query.setMachineCode(machineInfo.getMachineCode());
                    query.setMachineName(machineInfo.getMachineName());
                    List<GsqMachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                } else { //不勾选更新时两个都校验
                    query.setMachineCode(machineInfo.getMachineCode());
                    List<GsqMachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(machineInfo.getMachineName());
                    List<GsqMachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }
                if (hasFalse) {
                    machineInfo.setId(-999L);
                    failureNum++;
                    continue;
                }

                setBaseFieldValue(machineInfo, null);
                importList.add(machineInfo);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                // 勾选更新：批量预取已存在机台（按机台编码匹配），存在则在原记录上更新，不存在则新增
                Map<String, GsqMachineInfo> existingMap = this.loadExistingMachineMap(importList);
                for (GsqMachineInfo excelItem : importList) {
                    GsqMachineInfo existing = existingMap.get(excelItem.getMachineCode());
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        this.setBaseFieldValue(excelItem, existing.getId());
                        machineInfoMapper.updateMachineInfo(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入
                        this.setBaseFieldValue(excelItem, null);
                        machineInfoMapper.insertMachineInfo(excelItem);
                    }
                    successNum++;
                }
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    GsqMachineInfo excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }

                    //不存在插入
                    successNum++;
                    machineInfoMapper.insertMachineInfo(excelItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
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
     * 批量预取已存在的机台信息（导入更新模式使用）
     * 按机台编码批量查询数据库已有记录，逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 机台编码 -> 已存在机台信息 的映射
     */
    private Map<String, GsqMachineInfo> loadExistingMachineMap(List<GsqMachineInfo> importList) {
        // 提取非空机台编码并去重
        List<String> machineCodeList = importList.stream()
                .map(GsqMachineInfo::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(machineCodeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长；同编码多条时保留首条
        return CollUtil.split(machineCodeList, 1000).stream()
                .flatMap(batch -> machineInfoMapper.selectList(new LambdaQueryWrapper<GsqMachineInfo>()
                        .in(GsqMachineInfo::getMachineCode, batch)).stream())
                .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 设置基础字段值（替代原ApsBaseEntity.setBaseVale）
     * id为null时为新增操作，设置isDelete、createBy、createTime
     * id不为null时为更新操作，设置updateBy、updateTime
     *
     * @param entity 实体对象
     * @param id     主键ID，null表示新增，非null表示更新
     */
    private void setBaseFieldValue(GsqMachineInfo entity, Long id) {
        try {
            String username = SecurityUtils.getUsername();
            if (id == null) {
                entity.setIsDelete(0);
                entity.setCreateBy(username);
                entity.setCreateTime(new Date());
            } else {
                entity.setUpdateBy(username);
                entity.setUpdateTime(new Date());
            }
        } catch (Exception e) {
            if (id == null) {
                entity.setIsDelete(0);
                entity.setCreateBy("system");
                entity.setCreateTime(new Date());
            } else {
                entity.setUpdateBy("system");
                entity.setUpdateTime(new Date());
            }
        }
    }
}
