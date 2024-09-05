package com.zlt.aps.tm.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.TmMachineInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎面机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-28
 */
@Service
public class TmMachineInfoServiceImpl implements TmMachineInfoService {
    @Resource
    private TmMachineInfoMapper machineInfoMapper;

    /**
     * 查询胎面机台信息
     *
     * @param id 胎面机台信息ID
     * @return 胎面机台信息
     */
    @Override
    public TmMachineInfo selectMachineInfoById(Long id) {
        return machineInfoMapper.selectMachineInfoById(id);
    }

    /**
     * 查询胎面机台信息列表
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息
     */
    @Override
    public List<TmMachineInfo> selectMachineInfoList(TmMachineInfo machineInfo) {
        return machineInfoMapper.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    @Override
    public int insertMachineInfo(TmMachineInfo machineInfo) {
        machineInfo.setBaseVale(null);
        return machineInfoMapper.insertMachineInfo(machineInfo);
    }

    /**
     * 修改胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    @Override
    public int updateMachineInfo(TmMachineInfo machineInfo) {
        machineInfo.setBaseVale(machineInfo.getId());
        return machineInfoMapper.updateMachineInfo(machineInfo);
    }

    /**
     * 批量删除胎面机台信息
     *
     * @param ids 需要删除的胎面机台信息ID
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
    public String checkMachineCodeUnique(TmMachineInfo machineInfo) {
        List<TmMachineInfo> list = machineInfoMapper.checkMachineCodeUnique(machineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 根据胎面和口型板获取对应机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息集合
     */
    @Override
    public List<TmMachineInfo> selectMachineInfoList2(TmMachineInfo machineInfo) {
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
    public AjaxResult importData(List<TmMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmMachineInfo> importList = new ArrayList<>();
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();
        List<ImportErrorLog> nameUniqueErrorLogs = new ArrayList<>();
        Map<Integer, Long> nameUniqueErrorMap = new HashMap<>();

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.machineInfoMapper.listMachineCodeNotUnique(list, importLogId, I18nUtil.getMessage("ui.data.column.cx.machine.message"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }
            //校验机台名称唯一性
            nameUniqueErrorLogs = this.machineInfoMapper.listMachineNameNotUnique(list, importLogId, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), SecurityUtils.getUsername(), updateSupport);
            importErrorLogs.addAll(nameUniqueErrorLogs);
            nameUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));

            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
            //机台名称分组
            Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));
            for (int i = 0; i < list.size(); i++) {
                TmMachineInfo machineInfo = list.get(i);
                //excel中重复记录校验
                Long hasValue = groupMap.get(machineInfo.getMachineCode());
                if (hasValue > 1) {
                    machineInfo.setId(-999L);
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                    message = String.format(message, columnName);
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //系统中唯一键冲突校验
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    machineInfo.setId(-999L);
                }
                //校验机台名称唯一性
                if(nameUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    machineInfo.setId(-999L);
                }
                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineInfo);

                //校验Excel机台名称唯一性
                Long hasNameValue = nameMap.get(machineInfo.getMachineName());
                if (hasNameValue > 1) {
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord4Name");
                    addImportErrorLog(importLogId, i + 2, message, validated);
                }

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
                    if (StringUtils.isNotBlank(machineInfo.getOpenMachineClass()) && machineInfo.getOpenMachineClass().indexOf("1") >= 0) {
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.machine.ClassShiftMapValidate"), validated);
                    }
                }

                if (CollectionUtils.isEmpty(validated) && machineInfo.getId() == null) {
                    machineInfo.setBaseVale(null);
                    importList.add(machineInfo);
                } else {
                    machineInfo.setId(-999L);
                    importErrorLogs.addAll(validated);
                }
            }
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                machineInfoMapper.mergeSql(importList);
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                machineInfoMapper.batchInsertMachineInfo(importList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
