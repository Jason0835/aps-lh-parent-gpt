package com.zlt.aps.tm.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;
import com.zlt.aps.tm.mapper.TmQuotaSettingMapper;
import com.zlt.aps.tm.service.TmMachineInfoService;
import com.zlt.aps.tm.service.TmQuotaSettingService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎面定额设定Service业务层处理
 *
 * @author zlt
 * @date 2021-06-28
 */
@Service
public class TmQuotaSettingServiceImpl implements TmQuotaSettingService {
    @Autowired
    private TmQuotaSettingMapper tmQuotaSettingMapper;
    @Autowired
    private TmMachineInfoService tmMachineInfoService;

    /**
     * 查询胎面定额设定
     *
     * @param id 胎面定额设定ID
     * @return 胎面定额设定
     */
    @Override
    public TmQuotaSetting selectTmQuotaSettingById(Long id) {
        return tmQuotaSettingMapper.selectTmQuotaSettingById(id);
    }

    /**
     * 查询胎面定额设定列表
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 胎面定额设定
     */
    @Override
    public List<TmQuotaSetting> selectTmQuotaSettingList(TmQuotaSetting tmQuotaSetting) {
        return tmQuotaSettingMapper.selectTmQuotaSettingList(tmQuotaSetting);
    }

    /**
     * 新增胎面定额设定
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    @Override
    public int insertTmQuotaSetting(TmQuotaSetting tmQuotaSetting) {
        checkParamAndUnique(tmQuotaSetting);
        tmQuotaSetting.setBaseVale(null);
        return tmQuotaSettingMapper.insertTmQuotaSetting(tmQuotaSetting);
    }

    /**
     * 修改胎面定额设定
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    @Override
    public int updateTmQuotaSetting(TmQuotaSetting tmQuotaSetting) {
        checkParamAndUnique(tmQuotaSetting);
        tmQuotaSetting.setBaseVale(tmQuotaSetting.getId());
        return tmQuotaSettingMapper.updateTmQuotaSetting(tmQuotaSetting);
    }

    /**
     * 批量删除胎面定额设定
     *
     * @param ids 需要删除的胎面定额设定ID
     * @return 结果
     */
    @Override
    public int deleteTmQuotaSettingByIds(Long[] ids) {
        return tmQuotaSettingMapper.deleteTmQuotaSettingByIds(ids);
    }

    /**
     * 删除胎面定额设定信息
     *
     * @param id 胎面定额设定ID
     * @return 结果
     */
    @Override
    public int deleteTmQuotaSettingById(Long id) {
        return tmQuotaSettingMapper.deleteTmQuotaSettingById(id);
    }

    /**
     * 校验胎面定额设定唯一性
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    @Override
    public String checkTmQuotaSettingUnique(TmQuotaSetting tmQuotaSetting) {
        if (tmQuotaSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<TmQuotaSetting> list = tmQuotaSettingMapper.checkTmQuotaSettingUnique(tmQuotaSetting);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<TmQuotaSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmQuotaSetting> importList = new ArrayList<>();
        //将机台名称转为机台code
        List<TmMachineInfo> machineInfoList = tmMachineInfoService.selectMachineInfoList(new TmMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineCode, TmMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getTreadCode()+a.getMachineName()), Collectors.counting()));

        //将机台名称转换为机台id，并做校验
        for (int i = 0; i < list.size(); i++) {
            TmQuotaSetting quotaSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(quotaSetting.getTreadCode()+quotaSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                quotaSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.treadCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = quotaSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, quotaSetting);
            //非空校验，未通过则插入错误详细日志
            int errorNum = i + 2;
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(quotaSetting.getTreadCode())) {
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
                quotaSetting.setBaseVale(null);
                quotaSetting.setMachineId(machineId);
                importList.add(quotaSetting);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tmQuotaSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmQuotaSetting excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TmQuotaSetting dto = tmQuotaSettingMapper.selectByCodeAndMachineId(excelItem);
                    if (dto == null) {
                        //不存在插入
                        successNum++;
                        tmQuotaSettingMapper.insertList(Collections.singletonList(excelItem));
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
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

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param tmQuotaSetting 要检查记录
     */
    private void checkParamAndUnique(TmQuotaSetting tmQuotaSetting) {
        if (tmQuotaSetting.getMachineId() == null && StringUtils.isEmpty(tmQuotaSetting.getTreadCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.isAllNull"));
        }
        String unique = checkTmQuotaSettingUnique(tmQuotaSetting);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
    }
}
