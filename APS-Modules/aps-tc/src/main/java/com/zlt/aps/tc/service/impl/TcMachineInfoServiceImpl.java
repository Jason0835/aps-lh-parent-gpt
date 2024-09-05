package com.zlt.aps.tc.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.TcMachineInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎侧机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class TcMachineInfoServiceImpl implements TcMachineInfoService {
    @Autowired
    private TcMachineInfoMapper machineInfoMapper;

    /**
     * 查询胎侧机台信息
     *
     * @param id 胎侧机台信息ID
     * @return 胎侧机台信息
     */
    @Override
    public TcMachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询胎侧机台信息列表
     *
     * @param MachineInfo 胎侧机台信息
     * @return 胎侧机台信息
     */
    @Override
    public List<TcMachineInfo> selectMachineInfoList(TcMachineInfo MachineInfo) {
        return machineInfoMapper.selectMachineInfoList(MachineInfo);
    }

    /**
     * 新增胎侧机台信息
     *
     * @param machineInfo 胎侧机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(TcMachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改胎侧机台信息
     *
     * @param machineInfo 胎侧机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(TcMachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除胎侧机台信息
     *
     * @param ids 需要删除的胎侧机台信息ID
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
    public String checkMachineCodeUnique(TcMachineInfo MachineInfo) {
        List<TcMachineInfo> list = machineInfoMapper.checkMachineCodeUnique(MachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 根据胎侧、口型板查询机台信息
     *
     * @param MachineInfo 胎侧机台信息
     * @return 胎侧机台信息
     */
    @Override
    public List<TcMachineInfo> selectMachineInfoList2(TcMachineInfo MachineInfo) {
        return machineInfoMapper.selectMachineInfoList2(MachineInfo);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcMachineInfo> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcMachineInfo> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            TcMachineInfo machineInfo = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(machineInfo.getMachineCode());
            if (hasValue > 1) {
                failureNum++;
                machineInfo.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineInfo);

            if (machineInfo.getWidthMin() != null && machineInfo.getWidthMax() != null && machineInfo.getWidthMin().compareTo(machineInfo.getWidthMax()) > 0) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.machine.widthMaxBigThanWidthMin"), validated);
            }

            if (machineInfo.getThickMin() != null && machineInfo.getThickMax() != null && machineInfo.getThickMin().compareTo(machineInfo.getThickMax()) > 0) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.machine.thickMaxBigThanThickMin"), validated);
            }

            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }
            if (StringUtils.isNotBlank(machineInfo.getClassShift()) && machineInfo.getClassShift().indexOf("2") >= 0) {
                if (com.ruoyi.common.utils.StringUtils.isNotBlank(machineInfo.getOpenMachineClass()) && machineInfo.getOpenMachineClass().indexOf("1") >= 0) {
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.machine.ClassShiftMapValidate"), validated);
                }
            }
            //校验Excel机台名称唯一性
            Long hasNameValue = nameMap.get(machineInfo.getMachineName());
            if (hasNameValue > 1) {
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
                TcMachineInfo query = new TcMachineInfo();
                if(updateSupport){ //勾选更新时只校验机台名称
                    query.setMachineCode(machineInfo.getMachineCode());
                    query.setMachineName(machineInfo.getMachineName());
                    List<TcMachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }else{ //不勾选更新时两个都校验
                    query.setMachineCode(machineInfo.getMachineCode());
                    List<TcMachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(machineInfo.getMachineName());
                    List<TcMachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
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

                machineInfo.setBaseVale(null);
                newList.add(machineInfo);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                machineInfoMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcMachineInfo excelItem = list.get(i);
                    // 错误跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }

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
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

}
