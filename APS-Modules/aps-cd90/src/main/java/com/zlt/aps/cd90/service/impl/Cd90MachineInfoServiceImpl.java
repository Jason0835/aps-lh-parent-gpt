package com.zlt.aps.cd90.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.mapper.Cd90MachineInfoMapper;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import com.zlt.aps.common.core.utils.ImportUtil;
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
 * 90°裁断机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class Cd90MachineInfoServiceImpl implements Cd90MachineInfoService {
    @Autowired
    private Cd90MachineInfoMapper machineInfoMapper;

    /**
     * 查询90°裁断机台信息
     *
     * @param id 90°裁断机台信息ID
     * @return 90°裁断机台信息
     */
    @Override
    public Cd90MachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询90°裁断机台信息列表
     *
     * @param machineInfo 90°裁断机台信息
     * @return 90°裁断机台信息
     */
    @Override
    public List<Cd90MachineInfo> selectMachineInfoList(Cd90MachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增90°裁断机台信息
     *
     * @param machineInfo 90°裁断机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(Cd90MachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改90°裁断机台信息
     *
     * @param machineInfo 90°裁断机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(Cd90MachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除90°裁断机台信息
     *
     * @param ids 需要删除的90°裁断机台信息ID
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
    public String checkMachineCodeUnique(Cd90MachineInfo MachineInfo) {
        List<Cd90MachineInfo> list = machineInfoMapper.checkMachineCodeUnique(MachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<Cd90MachineInfo> selectMachineInfoList2(Cd90MachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList2(machineInfo);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90MachineInfo> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90MachineInfo> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(Cd90MachineInfo::getMachineCode, Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90MachineInfo dto = list.get(i);

            // excel内业务主键唯一校验
            Long hasValue = groupMap.get(dto.getMachineCode());
            if (hasValue > 1) {
                dto.setId(-999L);
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                addImportErrorLog(importLogId, i + 2,
                        String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
                        importErrorLogs);
                failureNum++;
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            if (dto.getClothWithMin() != null && dto.getClothWithMax() != null && dto.getClothWithMin().compareTo(dto.getClothWithMax()) > 0) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.machine.clothWithMaxBigThanClothWithMin"), validated);
            }
            if (StringUtils.isNotBlank(dto.getClassShift()) && dto.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }
            if (StringUtils.isNotBlank(dto.getClassShift()) && dto.getClassShift().indexOf("2") >= 0) {
                if (com.ruoyi.common.utils.StringUtils.isNotBlank(dto.getOpenMachineClass()) && dto.getOpenMachineClass().indexOf("1") >= 0) {
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
                Cd90MachineInfo query = new Cd90MachineInfo();
                if(updateSupport){ //勾选更新时只校验机台名称
                    query.setMachineCode(dto.getMachineCode());
                    query.setMachineName(dto.getMachineName());
                    List<Cd90MachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }else{ //不勾选更新时两个都校验
                    query.setMachineCode(dto.getMachineCode());
                    List<Cd90MachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(dto.getMachineName());
                    List<Cd90MachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
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

                        Cd90MachineInfo newItem = list.get(i);
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
