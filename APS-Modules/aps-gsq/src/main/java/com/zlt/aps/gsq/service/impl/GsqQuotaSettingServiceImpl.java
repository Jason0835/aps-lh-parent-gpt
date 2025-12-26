package com.zlt.aps.gsq.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;
import com.zlt.aps.gsq.mapper.GsqQuotaSettingMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.GsqQuotaSettingService;


/**
 * 钢丝圈定额设定Service业务层处理
 *
 * @author zlt
 * @date 2021-06-29
 */
@Service
public class GsqQuotaSettingServiceImpl implements GsqQuotaSettingService {
    @Autowired
    private GsqQuotaSettingMapper gsqQuotaSettingMapper;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 查询钢丝圈定额设定
     *
     * @param id 钢丝圈定额设定ID
     * @return 钢丝圈定额设定
     */
    @Override
    public GsqQuotaSetting selectGsqQuotaSettingById(Long id) {
        return gsqQuotaSettingMapper.selectGsqQuotaSettingById(id);
    }

    /**
     * 查询钢丝圈定额设定列表
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 钢丝圈定额设定
     */
    @Override
    public List<GsqQuotaSetting> selectGsqQuotaSettingList(GsqQuotaSetting gsqQuotaSetting) {
        return gsqQuotaSettingMapper.selectGsqQuotaSettingList(gsqQuotaSetting);
    }

    /**
     * 新增钢丝圈定额设定
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 结果
     */
    @Override
    public int insertGsqQuotaSetting(GsqQuotaSetting gsqQuotaSetting) {
        checkParamAndUnique(gsqQuotaSetting);
        gsqQuotaSetting.setBaseVale(null);
        return gsqQuotaSettingMapper.insertGsqQuotaSetting(gsqQuotaSetting);
    }

    /**
     * 修改钢丝圈定额设定
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 结果
     */
    @Override
    public int updateGsqQuotaSetting(GsqQuotaSetting gsqQuotaSetting) {
        checkParamAndUnique(gsqQuotaSetting);
        gsqQuotaSetting.setBaseVale(gsqQuotaSetting.getId());
        return gsqQuotaSettingMapper.updateGsqQuotaSetting(gsqQuotaSetting);
    }

    /**
     * 批量删除钢丝圈定额设定
     *
     * @param ids 需要删除的钢丝圈定额设定ID
     * @return 结果
     */
    @Override
    public int deleteGsqQuotaSettingByIds(Long[] ids) {
        return gsqQuotaSettingMapper.deleteGsqQuotaSettingByIds(ids);
    }

    /**
     * 删除钢丝圈定额设定信息
     *
     * @param id 钢丝圈定额设定ID
     * @return 结果
     */
    @Override
    public int deleteGsqQuotaSettingById(Long id) {
        return gsqQuotaSettingMapper.deleteGsqQuotaSettingById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkGsqQuotaSettingUnique(GsqQuotaSetting gsqQuotaSetting) {
        if (gsqQuotaSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<GsqQuotaSetting> list = gsqQuotaSettingMapper.checkGsqQuotaSettingUnique(gsqQuotaSetting);
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
    private void checkParamAndUnique(GsqQuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getSteelRingCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.isAllNull"));
        }
        String unique = checkGsqQuotaSettingUnique(quotaSetting);
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
    public AjaxResult importData(List<GsqQuotaSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqQuotaSetting> importList = new ArrayList<>();
        //将机台名称转为机台code
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSteelRingCode()+a.getMachineName()), Collectors.counting()));

        //将机台名称转换为机台id，并做校验
        for (int i = 0; i < list.size(); i++) {
            GsqQuotaSetting quotaSetting = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(quotaSetting.getSteelRingCode()+quotaSetting.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                quotaSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.steelRingCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = quotaSetting.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            int errorNum = i + 2;
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, quotaSetting);
            // 代码和机台名称不能同时为空校验
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(quotaSetting.getSteelRingCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验失败
                failureNum++;
                quotaSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                // 查询到机台信息，且校验通过
                quotaSetting.setMachineId(machineId);
                quotaSetting.setBaseVale(null);
                importList.add(quotaSetting);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                gsqQuotaSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    GsqQuotaSetting excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    List<GsqQuotaSetting> quotaSettings = gsqQuotaSettingMapper.checkGsqQuotaSettingUnique(excelItem);
                    if (CollectionUtils.isEmpty(quotaSettings)) {
                        //不存在插入
                        successNum++;
                        gsqQuotaSettingMapper.insertGsqQuotaSetting(excelItem);
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
}
