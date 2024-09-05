package com.zlt.aps.cd15.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
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
 * 15°裁断机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class Cd15MachineInfoServiceImpl implements Cd15MachineInfoService {
    @Autowired
    private Cd15MachineInfoMapper machineInfoMapper;

    /**
     * 查询15°裁断机台信息
     *
     * @param id 15°裁断机台信息ID
     * @return 15°裁断机台信息
     */
    @Override
    public Cd15MachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询15°裁断机台信息列表
     *
     * @param machineInfo 15°裁断机台信息
     * @return 15°裁断机台信息
     */
    @Override
    public List<Cd15MachineInfo> selectMachineInfoList(Cd15MachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增15°裁断机台信息
     *
     * @param machineInfo 15°裁断机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(Cd15MachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改15°裁断机台信息
     *
     * @param machineInfo 15°裁断机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(Cd15MachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除15°裁断机台信息
     *
     * @param ids 需要删除的15°裁断机台信息ID
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
    public String checkMachineCodeUnique(Cd15MachineInfo MachineInfo) {
        List<Cd15MachineInfo> list = machineInfoMapper.checkMachineCodeUnique(MachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    public List<Cd15MachineInfo> selectMachineInfoList2(Cd15MachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList2(machineInfo);
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
    public AjaxResult importData(List<Cd15MachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15MachineInfo> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        for (int i = 0; i < list.size(); i++) {
            Cd15MachineInfo machineInfo = list.get(i);

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

            if (CollectionUtils.isEmpty(validated)) {

                // 唯一性校验
                Boolean hasFalse = false;
                Cd15MachineInfo query = new Cd15MachineInfo();
                if(updateSupport){ //勾选更新时只校验机台名称
                    query.setMachineCode(machineInfo.getMachineCode());
                    query.setMachineName(machineInfo.getMachineName());
                    List<Cd15MachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }else{ //不勾选更新时两个都校验
                    query.setMachineCode(machineInfo.getMachineCode());
                    List<Cd15MachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(machineInfo.getMachineName());
                    List<Cd15MachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
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
                importList.add(machineInfo);
            } else {
                failureNum++;
                // 设置错误标识
                machineInfo.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    machineInfoMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15MachineInfo excelItem = list.get(i);
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
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
