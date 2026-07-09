package com.zlt.aps.nc.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;
import com.zlt.aps.nc.mapper.NcQuotaSettingMapper;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcQuotaSettingService;
import com.zlt.bill.common.service.AbstractDocService;


/**
 * 内衬定额设定Service业务层处理
 *
 * @author zlt
 * @date 2021-06-29
 */
@Service
public class NcQuotaSettingServiceImpl extends AbstractDocService<NcQuotaSetting> implements NcQuotaSettingService {
    @Autowired
    private NcQuotaSettingMapper ncQuotaSettingMapper;

    @Autowired
    private NcMachineInfoService ncMachineInfoService;

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkNcQuotaSettingUnique(NcQuotaSetting ncQuotaSetting) {
        if (ncQuotaSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<NcQuotaSetting> wrapper = new LambdaQueryWrapper<>();
        if (ncQuotaSetting.getId() != null) {
            wrapper.ne(NcQuotaSetting::getId, ncQuotaSetting.getId());
        }
        if (ncQuotaSetting.getMachineId() == null) {
            wrapper.isNull(NcQuotaSetting::getMachineId);
        } else {
            wrapper.eq(NcQuotaSetting::getMachineId, ncQuotaSetting.getMachineId());
        }
        if (StringUtils.isEmpty(ncQuotaSetting.getLiningCode())) {
            wrapper.isNull(NcQuotaSetting::getLiningCode);
        } else {
            wrapper.eq(NcQuotaSetting::getLiningCode, ncQuotaSetting.getLiningCode());
        }
        List<NcQuotaSetting> list = ncQuotaSettingMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param quotaSetting 要检查记录
     */
    private void checkParamAndUnique(NcQuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getLiningCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        String unique = checkNcQuotaSettingUnique(quotaSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
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
    public AjaxResult importData(List<NcQuotaSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<NcQuotaSetting> importList = new ArrayList<>();
        //将机台名称转为机台code
        List<NcMachineInfo> machineInfoList = ncMachineInfoService.selectMachineInfoList(new NcMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineCode, NcMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getLiningCode()+a.getMachineName()), Collectors.counting()));

        //将机台名称转换为机台id，并做校验
        for (int i = 0; i < list.size(); i++) {
            NcQuotaSetting quotaSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(quotaSetting.getLiningCode()+quotaSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                quotaSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.liningCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = quotaSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            int errorNum = i + 2;
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, quotaSetting);

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(quotaSetting.getLiningCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                quotaSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                quotaSetting.setMachineId(machineId);
                quotaSetting.setBaseVale(null);
                importList.add(quotaSetting);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                baseDao.saveBatch(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    NcQuotaSetting excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    LambdaQueryWrapper<NcQuotaSetting> unicWrapper = new LambdaQueryWrapper<>();
                    if (excelItem.getId() != null) {
                        unicWrapper.ne(NcQuotaSetting::getId, excelItem.getId());
                    }
                    if (excelItem.getMachineId() == null) {
                        unicWrapper.isNull(NcQuotaSetting::getMachineId);
                    } else {
                        unicWrapper.eq(NcQuotaSetting::getMachineId, excelItem.getMachineId());
                    }
                    if (StringUtils.isEmpty(excelItem.getLiningCode())) {
                        unicWrapper.isNull(NcQuotaSetting::getLiningCode);
                    } else {
                        unicWrapper.eq(NcQuotaSetting::getLiningCode, excelItem.getLiningCode());
                    }
                    List<NcQuotaSetting> quotaSettings = ncQuotaSettingMapper.selectList(unicWrapper);
                    if (CollectionUtils.isEmpty(quotaSettings)) {
                        //不存在插入
                        successNum++;
                        ncQuotaSettingMapper.insert(excelItem);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                    }
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

    @Override
    protected String getDocTypeCode() {
        return null;
    }
}
