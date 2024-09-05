package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.entity.Cd15QuotaSetting;
import com.zlt.aps.cd15.mapper.Cd15QuotaSettingMapper;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.cd15.service.Cd15QuotaSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 15度裁断定额设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-28
 */
@Service
public class Cd15QuotaSettingServiceImpl extends ServiceImpl<Cd15QuotaSettingMapper, Cd15QuotaSetting> implements Cd15QuotaSettingService {
    @Autowired
    private Cd15QuotaSettingMapper cd15QuotaSettingMapper;

    @Autowired
    private Cd15MachineInfoService cd15MachineInfoService;

    /**
     * 查询15度定额设定列表
     *
     * @param quotaSetting 15度定额设定
     * @return 15度定额设定集合
     */
    @Override
    public List<Cd15QuotaSettingDto> selectQuotaSettingList(Cd15QuotaSetting quotaSetting) {
        return cd15QuotaSettingMapper.selectQuotaSettingList(quotaSetting);
    }

    /**
     * 查询15度定额设定
     *
     * @param id 15度定额设定ID
     * @return 15度定额设定
     */
    @Override
    public Cd15QuotaSetting selectQuotaSettingById(Long id) {
        LambdaQueryWrapper<Cd15QuotaSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15QuotaSetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(Cd15QuotaSetting::getId, id);
        return cd15QuotaSettingMapper.selectOne(wrapper);
    }

    /**
     * 修改15度定额设定
     *
     * @param quotaSetting 15度定额设定
     */
    @Override
    public AjaxResult saveQuotaSetting(Cd15QuotaSetting quotaSetting) {
        checkParamAndUnique(quotaSetting);
        quotaSetting.setBaseVale(quotaSetting.getId());
        saveOrUpdate(quotaSetting);
        return AjaxResult.success();
    }

    /**
     * 批量删除15度定额设定
     *
     * @param ids 需要删除的15度定额设定ID
     */
    @Override
    public void deleteQuotaSettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        cd15QuotaSettingMapper.deleteCd15QuotaSettingByIds(ids);
    }

    /**
     * 验证定额设定信息唯一性
     */
    @Override
    public String checkUnique(Cd15QuotaSetting quotaSetting) {
        List<Cd15QuotaSetting> list = cd15QuotaSettingMapper.checkUnique(quotaSetting);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 检查参数是否合法，记录是否唯一
     *
     * @param quotaSetting 要检查记录
     */
    private void checkParamAndUnique(Cd15QuotaSetting quotaSetting) {
        if (quotaSetting.getMachineId() == null && StringUtils.isEmpty(quotaSetting.getSteelStripCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.quota.isAllNull"));
        }
        String unique = checkUnique(quotaSetting);
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
    public AjaxResult importData(List<Cd15QuotaSettingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15QuotaSettingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<Cd15MachineInfo> machineInfoList = cd15MachineInfoService.selectMachineInfoList(new Cd15MachineInfo());
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineCode, Cd15MachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSteelStripCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15QuotaSettingDto quotaSettingDto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(quotaSettingDto.getSteelStripCode()+quotaSettingDto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                quotaSettingDto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cd15.setting.steelStripCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, quotaSettingDto);
            String machineName = quotaSettingDto.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(quotaSettingDto.getSteelStripCode())) {
                // 代码和机台名称不能同时为空校验
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isEmpty(validated)) {
                quotaSettingDto.setMachineId(machineId);
                quotaSettingDto.setBaseVale(null);
                importList.add(quotaSettingDto);
            } else {
                failureNum++;
                quotaSettingDto.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    cd15QuotaSettingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15QuotaSettingDto excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        Cd15QuotaSetting quotaSetting = new Cd15QuotaSetting();
                        BeanUtils.copyProperties(excelItem, quotaSetting);
                        List<Cd15QuotaSetting> unique = cd15QuotaSettingMapper.checkUnique(quotaSetting);
                        if (unique.size() == 0) {
                            //不存在插入
                            successNum++;
                            cd15QuotaSettingMapper.insert(quotaSetting);
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
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
