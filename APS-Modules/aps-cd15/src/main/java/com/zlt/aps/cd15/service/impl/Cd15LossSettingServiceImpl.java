package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.mapper.Cd15LossSettingMapper;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.service.ICd15LossSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 斜裁损耗率设定业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15LossSettingServiceImpl extends AbstractDocService<Cd15LossSetting> implements ICd15LossSettingService {

    @Resource
    private Cd15LossSettingMapper cd15LossSettingMapper;

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_LOSS_SETTING";
    }

    @Override
    public String checkUnique(Cd15LossSetting entity) {
        LambdaQueryWrapper<Cd15LossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15LossSetting::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15LossSetting::getSteelStripCode, entity.getSteelStripCode());
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getMachineCode())) {
                w.eq(Cd15LossSetting::getMachineCode, entity.getMachineCode());
            } else {
                w.and(wa -> wa.eq(Cd15LossSetting::getMachineCode, "").or().isNull(Cd15LossSetting::getMachineCode));
            }
        });
        wrapper.ne(entity.getId() != null, Cd15LossSetting::getId, entity.getId());
        return cd15LossSettingMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd15LossSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15LossSetting> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        Set<String> enabledMachineKeys = loadEnabledMachineKeys();

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15LossSetting docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (!isMachineCodeValid(docEntity, enabledMachineKeys)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.machineInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15LossSetting docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (docEntity.getLossRate() == null || docEntity.getLossRate() < 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.cd15LossSetting.lossRateInvalid"), importErrorLogs);
                continue;
            }

            Cd15LossSetting exist = getExistLossSetting(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setLossRate(docEntity.getLossRate());
                exist.setRemark(docEntity.getRemark());
                cd15LossSettingMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd15LossSetting getExistLossSetting(Cd15LossSetting entity) {
        LambdaQueryWrapper<Cd15LossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15LossSetting::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15LossSetting::getSteelStripCode, entity.getSteelStripCode());
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getMachineCode())) {
                w.eq(Cd15LossSetting::getMachineCode, entity.getMachineCode());
            } else {
                w.and(wa -> wa.eq(Cd15LossSetting::getMachineCode, "").or().isNull(Cd15LossSetting::getMachineCode));
            }
        });
        return cd15LossSettingMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_LOSS_SETTING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "steelStripCode", "machineCode");
    }

    /**
     * 校验机台编码是否存在于启用机台中（机台编码为空时跳过）。
     */
    private boolean isMachineCodeValid(Cd15LossSetting entity, Set<String> enabledMachineKeys) {
        if (StringUtils.isBlank(entity.getMachineCode())) {
            return true;
        }
        String key = entity.getFactoryCode() + ":" + entity.getMachineCode();
        return enabledMachineKeys.contains(key);
    }

    /**
     * 预加载所有启用状态的机台，构建 factoryCode:machineCode 的 Set 用于导入时 O(1) 校验。
     */
    private Set<String> loadEnabledMachineKeys() {
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1);
        wrapper.select(Cd15MachineInfo::getFactoryCode, Cd15MachineInfo::getMachineCode);
        List<Cd15MachineInfo> machines = cd15MachineInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(machines)) {
            return new HashSet<>();
        }
        Set<String> keys = new HashSet<>(machines.size());
        for (Cd15MachineInfo m : machines) {
            keys.add(m.getFactoryCode() + ":" + m.getMachineCode());
        }
        return keys;
    }
}
