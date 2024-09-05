package com.zlt.aps.tc.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcQuotaSetting;
import com.zlt.aps.tc.mapper.TcQuotaSettingMapper;
import com.zlt.aps.tc.service.TcMachineInfoService;
import com.zlt.aps.tc.service.TcQuotaSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 胎侧定额设定Service业务层处理
 *
 * @author zlt
 * @date 2021-06-28
 */
@Service
public class TcQuotaSettingServiceImpl implements TcQuotaSettingService {
    @Autowired
    private TcQuotaSettingMapper tcQuotaSettingMapper;

    @Autowired
    private TcMachineInfoService tcMachineInfoService;


    /**
     * 查询胎侧定额设定
     *
     * @param id 胎侧定额设定ID
     * @return 胎侧定额设定
     */
    @Override
    public TcQuotaSetting selectTcQuotaSettingById(Long id) {
        return tcQuotaSettingMapper.selectTcQuotaSettingById(id);
    }

    /**
     * 查询胎侧定额设定列表
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 胎侧定额设定
     */
    @Override
    public List<TcQuotaSetting> selectTcQuotaSettingList(TcQuotaSetting tcQuotaSetting) {
        return tcQuotaSettingMapper.selectTcQuotaSettingList(tcQuotaSetting);
    }

    /**
     * 新增胎侧定额设定
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 结果
     */
    @Override
    public int insertTcQuotaSetting(TcQuotaSetting tcQuotaSetting) {
        checkParamAndUnique(tcQuotaSetting);
        tcQuotaSetting.setBaseVale(null);
        return tcQuotaSettingMapper.insertTcQuotaSetting(tcQuotaSetting);
    }

    /**
     * 修改胎侧定额设定
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 结果
     */
    @Override
    public int updateTcQuotaSetting(TcQuotaSetting tcQuotaSetting) {
        checkParamAndUnique(tcQuotaSetting);
        tcQuotaSetting.setBaseVale(tcQuotaSetting.getId());
        return tcQuotaSettingMapper.updateTcQuotaSetting(tcQuotaSetting);
    }

    /**
     * 批量删除胎侧定额设定
     *
     * @param ids 需要删除的胎侧定额设定ID
     * @return 结果
     */
    @Override
    public int deleteTcQuotaSettingByIds(Long[] ids) {
        return tcQuotaSettingMapper.deleteTcQuotaSettingByIds(ids);
    }

    /**
     * 删除胎侧定额设定信息
     *
     * @param id 胎侧定额设定ID
     * @return 结果
     */
    @Override
    public int deleteTcQuotaSettingById(Long id) {
        return tcQuotaSettingMapper.deleteTcQuotaSettingById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkTcQuotaSettingUnique(TcQuotaSetting tcQuotaSetting) {
        if (tcQuotaSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<TcQuotaSetting> list = tcQuotaSettingMapper.checkTcQuotaSettingUnique(tcQuotaSetting);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcQuotaSetting> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<TcQuotaSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<TcMachineInfo> machineInfoList = tcMachineInfoService.selectMachineInfoList(new TcMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = new HashMap<>();
        machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSidewallCode()+a.getMachineName()), Collectors.counting()));

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            TcQuotaSetting quotaSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(quotaSetting.getSidewallCode()+quotaSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                quotaSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.sidewallCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = quotaSetting.getMachineName();
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, quotaSetting);

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(quotaSetting.getSidewallCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, errorNum, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                quotaSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                quotaSetting.setMachineId(machineCodeMap.get(machineName));
                quotaSetting.setBaseVale(null);
                newList.add(quotaSetting);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcQuotaSettingMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcQuotaSetting excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    List<TcQuotaSetting> exist = tcQuotaSettingMapper.checkTcQuotaSettingUnique(excelItem);
                    if (CollectionUtils.isEmpty(exist)) {
                        successNum++;
                        tcQuotaSettingMapper.insertTcQuotaSetting(excelItem);
                    } else {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.error.message.quota.unique");
                        ImportUtil.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    }
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

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param quotaSetting 要检查记录
     */
    private void checkParamAndUnique(TcQuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getSidewallCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkTcQuotaSettingUnique(quotaSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
    }
}
