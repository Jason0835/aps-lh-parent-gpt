package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.service.ICd15MachineInfoService;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 斜裁机台基础信息业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15MachineInfoServiceImpl extends AbstractDocService<Cd15MachineInfo> implements ICd15MachineInfoService {

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_MACHINE_INFO";
    }

    @Override
    public int save(Cd15MachineInfo machineInfo) {
        this.normalize(machineInfo);
        return super.save(machineInfo);
    }

    /**
     * 校验同一工厂下机台编号是否唯一。
     *
     * @param machineInfo 机台信息
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15MachineInfo machineInfo) {
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, machineInfo.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, machineInfo.getMachineCode());
        wrapper.ne(machineInfo.getId() != null, Cd15MachineInfo::getId, machineInfo.getId());
        return cd15MachineInfoMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 保存前校验裁断模式和模式能力。
     *
     * @param machineInfo 机台信息
     * @return 校验失败结果，校验通过返回null
     */
    @Override
    public AjaxResult validateForSave(Cd15MachineInfo machineInfo) {
        this.normalize(machineInfo);
        String validationMessage = this.validateBusiness(machineInfo);
        return validationMessage == null ? null : AjaxResult.error(validationMessage);
    }

    /**
     * 导入斜裁机台基础信息。
     *
     * @param list 导入列表
     * @param updateSupport 是否更新已有数据
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<Cd15MachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15MachineInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15MachineInfo docEntity = list.get(i);
            this.normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            String validationMessage = this.validateBusiness(docEntity);
            if (validationMessage != null) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId,
                        ImportErrorTypeEnums.OTHERS.getCode(), errorNum,
                        validationMessage, validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15MachineInfo docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd15MachineInfo exist = getExistMachine(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setMachineName(docEntity.getMachineName());
                exist.setClothWidthMax(docEntity.getClothWidthMax());
                exist.setClothWidthMin(docEntity.getClothWidthMin());
                exist.setClassShift(docEntity.getClassShift());
                exist.setOpenMachineClass(docEntity.getOpenMachineClass());
                exist.setIsOutTwo(docEntity.getIsOutTwo());
                exist.setSingleCutFlag(docEntity.getSingleCutFlag());
                exist.setSplitCutFlag(docEntity.getSplitCutFlag());
                exist.setDefaultCutMode(docEntity.getDefaultCutMode());
                exist.setDailyOutputModeThreshold(docEntity.getDailyOutputModeThreshold());
                exist.setSingleShiftCapacity(docEntity.getSingleShiftCapacity());
                exist.setSplitShiftCapacity(docEntity.getSplitShiftCapacity());
                exist.setStatus(docEntity.getStatus());
                exist.setSteelStripWidth(docEntity.getSteelStripWidth());
                exist.setRemark(docEntity.getRemark());
                cd15MachineInfoMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(uniqueMsg, errorNum), importErrorLogs);
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

    private String validateBusiness(Cd15MachineInfo machineInfo) {
        if (machineInfo == null) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.required");
        }
        if (!this.validFlag(machineInfo.getSingleCutFlag())
                || !this.validFlag(machineInfo.getSplitCutFlag())) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.capabilityFlagInvalid");
        }
        boolean singleSupported = "1".equals(machineInfo.getSingleCutFlag());
        boolean splitSupported = "1".equals(machineInfo.getSplitCutFlag());
        if (!singleSupported && !splitSupported) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.cutCapabilityRequired");
        }
        String mode = machineInfo.getDefaultCutMode();
        if (!Cd15CutMode.SINGLE.equals(mode)
                && !Cd15CutMode.SPLIT.equals(mode)
                && !Cd15CutMode.DAILY_OUTPUT.equals(mode)) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.defaultCutModeInvalid");
        }
        if ((Cd15CutMode.SINGLE.equals(mode) && !singleSupported)
                || (Cd15CutMode.SPLIT.equals(mode) && !splitSupported)
                || (Cd15CutMode.DAILY_OUTPUT.equals(mode)
                && (!singleSupported || !splitSupported))) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.modeCapabilityMismatch");
        }
        boolean singleCapacityRequired = Cd15CutMode.SINGLE.equals(mode)
                || Cd15CutMode.DAILY_OUTPUT.equals(mode);
        boolean splitCapacityRequired = Cd15CutMode.SPLIT.equals(mode)
                || Cd15CutMode.DAILY_OUTPUT.equals(mode);
        if ((singleCapacityRequired && !this.positive(machineInfo.getSingleShiftCapacity()))
                || (splitCapacityRequired && !this.positive(machineInfo.getSplitShiftCapacity()))
                || (machineInfo.getSingleShiftCapacity() != null
                && machineInfo.getSingleShiftCapacity() <= 0D)
                || (machineInfo.getSplitShiftCapacity() != null
                && machineInfo.getSplitShiftCapacity() <= 0D)) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.modeCapacityPositive");
        }
        if (machineInfo.getClothWidthMin() != null
                && machineInfo.getClothWidthMax() != null
                && machineInfo.getClothWidthMin() > machineInfo.getClothWidthMax()) {
            return I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.widthRangeInvalid");
        }
        return null;
    }

    private boolean validFlag(String value) {
        return "0".equals(value) || "1".equals(value);
    }

    /** 判断模式班产字段是否已维护有效正数。 */
    private boolean positive(Double value) {
        return value != null && value > 0D;
    }

    private void normalize(Cd15MachineInfo machineInfo) {
        if (machineInfo == null) {
            return;
        }
        machineInfo.setFactoryCode(StringUtils.trimToEmpty(machineInfo.getFactoryCode()));
        machineInfo.setMachineCode(StringUtils.trimToEmpty(machineInfo.getMachineCode()));
        machineInfo.setOpenMachineClass(StringUtils.trimToEmpty(
                machineInfo.getOpenMachineClass()));
        machineInfo.setSingleCutFlag(StringUtils.trimToEmpty(
                machineInfo.getSingleCutFlag()));
        machineInfo.setSplitCutFlag(StringUtils.trimToEmpty(
                machineInfo.getSplitCutFlag()));
        machineInfo.setDefaultCutMode(StringUtils.upperCase(
                StringUtils.trimToEmpty(machineInfo.getDefaultCutMode())));
    }

    private Cd15MachineInfo getExistMachine(Cd15MachineInfo machineInfo) {
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, machineInfo.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, machineInfo.getMachineCode());
        return cd15MachineInfoMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_MACHINE_INFO");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode");
    }
}
