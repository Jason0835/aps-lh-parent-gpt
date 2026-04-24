package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqLossSettingMapper;
import com.zlt.aps.tq.service.ITqLossSettingService;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqLossSettingServiceImpl extends AbstractDocService<TqLossSetting> implements ITqLossSettingService {

    @Resource
    private TqLossSettingMapper tqLossSettingMapper;

    @Resource
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_LOSS_SETTING";
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("materialCode", "machineId");
    }

    @Override
    public String checkUnique(TqLossSetting lossSetting) {
        QueryWrapper<TqLossSetting> wrapper = new QueryWrapper<>();
        wrapper.ne(lossSetting.getId() != null, "ID", lossSetting.getId());
        wrapper.eq("MATERIAL_CODE", lossSetting.getMaterialCode());
        wrapper.eq(lossSetting.getMachineId() != null, "MACHINE_ID", lossSetting.getMachineId());
        wrapper.isNull(lossSetting.getMachineId() == null, "MACHINE_ID");
        wrapper.eq("IS_DELETE", 0);
        if (tqLossSettingMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<TqLossSetting> listLossSetting(TqLossSetting lossSetting) {
        return tqLossSettingMapper.selectLossSettingList(lossSetting);
    }

    @Override
    public void deleteAll() {
        QueryWrapper<TqLossSetting> wrapper = new QueryWrapper<>();
        wrapper.eq("IS_DELETE", 0);
        List<TqLossSetting> list = tqLossSettingMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            for (TqLossSetting item : list) {
                item.setIsDelete(1);
                tqLossSettingMapper.updateById(item);
            }
        }
    }

    @Override
    public AjaxResult importData(List<TqLossSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqLossSetting> importList = new ArrayList<>();

        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> machineNameMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId, (a, b) -> a));

        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMaterialCode() == null ? "" : a.getMaterialCode()) + "_" + (a.getMachineName() == null ? "" : a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqLossSetting lossSetting = list.get(i);

            String groupKey = (lossSetting.getMaterialCode() == null ? "" : lossSetting.getMaterialCode()) + "_" + (lossSetting.getMachineName() == null ? "" : lossSetting.getMachineName());
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                lossSetting.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.loss.beadCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lossSetting);

            String machineName = lossSetting.getMachineName();
            Long machineId = machineNameMap.get(machineName);

            if (StringUtils.isEmpty(machineName) && StringUtils.isEmpty(lossSetting.getMaterialCode())) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.loss.isAllNull"), validated);
            }
            if (machineId == null && StringUtils.isNotEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }

            if (CollectionUtils.isEmpty(validated)) {
                lossSetting.setMachineId(machineId);
                if (StringUtils.isBlank(lossSetting.getMaterialCode())) {
                    lossSetting.setMaterialCode(null);
                }
                lossSetting.setIsDelete(0);
                importList.add(lossSetting);
            } else {
                failureNum++;
                lossSetting.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqLossSettingMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqLossSetting excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    QueryWrapper<TqLossSetting> wrapper = new QueryWrapper<>();
                    wrapper.eq("MATERIAL_CODE", excelItem.getMaterialCode());
                    wrapper.eq(excelItem.getMachineId() != null, "MACHINE_ID", excelItem.getMachineId());
                    wrapper.isNull(excelItem.getMachineId() == null, "MACHINE_ID");
                    wrapper.eq("IS_DELETE", 0);
                    Long unique = tqLossSettingMapper.selectCount(wrapper);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.loss.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
