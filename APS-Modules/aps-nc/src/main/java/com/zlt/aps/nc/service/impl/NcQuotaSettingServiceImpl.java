package com.zlt.aps.nc.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 内衬定额设定Service业务层处理
 *
 * @author zlt
 * @date 2021-06-29
 */
@Service
public class NcQuotaSettingServiceImpl implements NcQuotaSettingService {
    @Autowired
    private NcQuotaSettingMapper ncQuotaSettingMapper;

    @Autowired
    private NcMachineInfoService ncMachineInfoService;

    /**
     * 查询内衬定额设定
     *
     * @param id 内衬定额设定ID
     * @return 内衬定额设定
     */
    @Override
    public NcQuotaSetting selectNcQuotaSettingById(Long id) {
        return ncQuotaSettingMapper.selectNcQuotaSettingById(id);
    }

    /**
     * 查询内衬定额设定列表
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 内衬定额设定
     */
    @Override
    public List<NcQuotaSetting> selectNcQuotaSettingList(NcQuotaSetting ncQuotaSetting) {
        return ncQuotaSettingMapper.selectNcQuotaSettingList(ncQuotaSetting);
    }

    /**
     * 新增内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    @Override
    public int insertNcQuotaSetting(NcQuotaSetting ncQuotaSetting) {
        checkParamAndUnique(ncQuotaSetting);
        ncQuotaSetting.setBaseVale(null);
        return ncQuotaSettingMapper.insertNcQuotaSetting(ncQuotaSetting);
    }

    /**
     * 修改内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    @Override
    public int updateNcQuotaSetting(NcQuotaSetting ncQuotaSetting) {
        checkParamAndUnique(ncQuotaSetting);
        ncQuotaSetting.setBaseVale(ncQuotaSetting.getId());
        return ncQuotaSettingMapper.updateNcQuotaSetting(ncQuotaSetting);
    }

    /**
     * 批量删除内衬定额设定
     *
     * @param ids 需要删除的内衬定额设定ID
     * @return 结果
     */
    @Override
    public int deleteNcQuotaSettingByIds(Long[] ids) {
        return ncQuotaSettingMapper.deleteNcQuotaSettingByIds(ids);
    }

    /**
     * 删除内衬定额设定信息
     *
     * @param id 内衬定额设定ID
     * @return 结果
     */
    @Override
    public int deleteNcQuotaSettingById(Long id) {
        return ncQuotaSettingMapper.deleteNcQuotaSettingById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkNcQuotaSettingUnique(NcQuotaSetting ncQuotaSetting) {
        if (ncQuotaSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<NcQuotaSetting> list = ncQuotaSettingMapper.checkNcQuotaSettingUnique(ncQuotaSetting);
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
                ncQuotaSettingMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    NcQuotaSetting excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    List<NcQuotaSetting> quotaSettings = ncQuotaSettingMapper.checkNcQuotaSettingUnique(excelItem);
                    if (CollectionUtils.isEmpty(quotaSettings)) {
                        //不存在插入
                        successNum++;
                        ncQuotaSettingMapper.insertNcQuotaSetting(excelItem);
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
