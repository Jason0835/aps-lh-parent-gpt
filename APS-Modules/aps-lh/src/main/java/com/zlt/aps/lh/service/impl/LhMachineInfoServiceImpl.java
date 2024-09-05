package com.zlt.aps.lh.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.mapper.LhMachineInfoMapper;
import com.zlt.aps.lh.service.LhMachineInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫化机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class LhMachineInfoServiceImpl implements LhMachineInfoService {
    @Autowired
    private LhMachineInfoMapper machineInfoMapper;

    /**
     * 查询硫化机台信息
     *
     * @param id 硫化机台信息ID
     * @return 硫化机台信息
     */
    @Override
    public LhMachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询硫化机台信息列表
     *
     * @param machineInfo 硫化机台信息
     * @return 硫化机台信息
     */
    @Override
    public List<LhMachineInfo> selectMachineInfoList(LhMachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(LhMachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(LhMachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除硫化机台信息
     *
     * @param ids 需要删除的硫化机台信息ID
     * @return 结果
     */
    @Override
    public int deleteMachineInfoByIds(Long[] ids) {
        return machineInfoMapper.deleteMachineInfoByIds(ids);
    }

    /**
     * 校验机台编号唯一性
     */
    @Override
    public String checkMachineCodeUnique(LhMachineInfo MachineInfo) {
        List<LhMachineInfo> list = machineInfoMapper.checkMachineCodeUnique(MachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    @Override
    public List<LhMachineInfo> listMachineInfo(LhMachineInfo machineInfo) {
        return machineInfoMapper.listMachineInfo(machineInfo);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<LhMachineInfo> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhMachineInfo> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            LhMachineInfo dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getMachineCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            if (dto.getDimensionMinmum() != null && dto.getDimensionMaximum() != null && dto.getDimensionMinmum().compareTo(dto.getDimensionMaximum()) > 0) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.machine.dimensionMaximumBigThanDimensionMinmum"), validated);
            }
            if (StringUtils.isNotBlank(dto.getClassShift()) && dto.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }
            if (StringUtils.isNotBlank(dto.getClassShift()) && dto.getClassShift().indexOf("2") >= 0) {
                if (StringUtils.isNotBlank(dto.getOpenMachineClass()) && dto.getOpenMachineClass().indexOf("1") >= 0) {
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.machine.ClassShiftMapValidate"), validated);
                }
            }
            //校验Excel机台名称唯一性
            Long hasNameValue = nameMap.get(dto.getMachineName());
            if (hasNameValue > 1) {
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord4Name");
                addImportErrorLog(importLogId, i + 2, message, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {

                // 唯一性校验
                Boolean hasFalse = false;
                LhMachineInfo query = new LhMachineInfo();
                if (updateSupport) { //勾选更新时只校验机台名称
                    query.setMachineCode(dto.getMachineCode());
                    query.setMachineName(dto.getMachineName());
                    List<LhMachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                } else { //不勾选更新时两个都校验
                    query.setMachineCode(dto.getMachineCode());
                    List<LhMachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(dto.getMachineName());
                    List<LhMachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }
                if (hasFalse) {
                    dto.setId(-999L);
                    failureNum++;
                    continue;
                }

                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    machineInfoMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        LhMachineInfo newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }

                        successNum++;
                        machineInfoMapper.insertMachineInfo(newItem);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
