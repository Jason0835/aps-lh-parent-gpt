package com.zlt.aps.tq.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.service.TqMachineInfoService;
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
 * 胎圈机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class TqMachineInfoServiceImpl implements TqMachineInfoService {
    @Autowired
    private TqMachineInfoMapper machineInfoMapper;

    /**
     * 查询胎圈机台信息
     *
     * @param id 胎圈机台信息ID
     * @return 胎圈机台信息
     */
    @Override
    public TqMachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询胎圈机台信息列表
     *
     * @param machineInfo 胎圈机台信息
     * @return 胎圈机台信息
     */
    @Override
    public List<TqMachineInfo> selectMachineInfoList(TqMachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增胎圈机台信息
     *
     * @param machineInfo 胎圈机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(TqMachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改胎圈机台信息
     *
     * @param machineInfo 胎圈机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(TqMachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除胎圈机台信息
     *
     * @param ids 需要删除的胎圈机台信息ID
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
    public String checkMachineCodeUnique(TqMachineInfo MachineInfo) {
        List<TqMachineInfo> list = machineInfoMapper.checkMachineCodeUnique(MachineInfo);
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
    public List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo) {
        return machineInfoMapper.listMachineInfo(machineInfo);
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
    public AjaxResult importData(List<TqMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineInfo> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        for (int i = 0; i < list.size(); i++) {
            TqMachineInfo machineInfo = list.get(i);

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
                TqMachineInfo query = new TqMachineInfo();
                if (updateSupport) { //勾选更新时只校验机台名称
                    query.setMachineCode(machineInfo.getMachineCode());
                    query.setMachineName(machineInfo.getMachineName());
                    List<TqMachineInfo> exist2 = machineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                } else { //不勾选更新时两个都校验
                    query.setMachineCode(machineInfo.getMachineCode());
                    List<TqMachineInfo> exist1 = machineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(machineInfo.getMachineName());
                    List<TqMachineInfo> exist2 = machineInfoMapper.checkMachineCodeUnique(query);
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
                machineInfo.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                machineInfoMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TqMachineInfo excelItem = list.get(i);
                    // 错误记录跳过
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
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
