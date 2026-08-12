package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.mapper.Cd15MachineRollMappingMapper;
import com.zlt.aps.cd15.service.ICd15MachineRollMappingService;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 斜裁大卷与机台映射业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15MachineRollMappingServiceImpl extends AbstractDocService<Cd15MachineRollMapping> implements ICd15MachineRollMappingService {

    @Resource
    private Cd15MachineRollMappingMapper cd15MachineRollMappingMapper;

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_MACHINE_ROLL_MAPPING";
    }

    @Override
    public AjaxResult saveWithConfirm(Cd15MachineRollMapping entity) {
        this.normalize(entity);
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.checkUnique"));
        }
        Cd15MachineInfo machineInfo = this.getEnabledMachineInfo(entity);
        if (machineInfo == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.machineInvalid"));
        }
        if (!Boolean.TRUE.equals(entity.getConfirmOutOfOpenShift()) && this.hasOutOfOpenShift(entity, machineInfo)) {
            AjaxResult result = AjaxResult.success(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.shiftOutOfOpenMachineClassConfirm"));
            result.put("needConfirm", true);
            return result;
        }
        super.save(entity);
        return AjaxResult.success();
    }

    @Override
    public int save(Cd15MachineRollMapping entity) {
        this.normalize(entity);
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(entity))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.checkUnique"));
        }
        if (this.getEnabledMachineInfo(entity) == null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.machineInvalid"));
        }
        return super.save(entity);
    }

    /**
     * 校验同一工厂、大卷、机台、班次下是否已存在映射。
     *
     * @param entity 大卷与机台映射
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15MachineRollMapping entity) {
        this.normalize(entity);
        LambdaQueryWrapper<Cd15MachineRollMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineRollMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineRollMapping::getBigRollCode, entity.getBigRollCode());
        wrapper.eq(Cd15MachineRollMapping::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd15MachineRollMapping::getShiftCode, entity.getShiftCode());
        wrapper.ne(entity.getId() != null, Cd15MachineRollMapping::getId, entity.getId());
        return cd15MachineRollMappingMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd15MachineRollMapping> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15MachineRollMapping> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int index = 0; index < list.size(); index++) {
            int errorNum = index + 2;
            Cd15MachineRollMapping docEntity = list.get(index);
            this.normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, index, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (this.getEnabledMachineInfo(docEntity) == null) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.machineInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int index = 0; index < list.size(); index++) {
            int errorNum = index + 2;
            Cd15MachineRollMapping docEntity = list.get(index);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd15MachineRollMapping exist = this.getExistMapping(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setRemark(docEntity.getRemark());
                cd15MachineRollMappingMapper.updateById(exist);
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

    private Cd15MachineRollMapping getExistMapping(Cd15MachineRollMapping entity) {
        LambdaQueryWrapper<Cd15MachineRollMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineRollMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineRollMapping::getBigRollCode, entity.getBigRollCode());
        wrapper.eq(Cd15MachineRollMapping::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd15MachineRollMapping::getShiftCode, entity.getShiftCode());
        return cd15MachineRollMappingMapper.selectOne(wrapper);
    }

    private Cd15MachineInfo getEnabledMachineInfo(Cd15MachineRollMapping entity) {
        if (entity == null || StringUtils.isBlank(entity.getFactoryCode()) || StringUtils.isBlank(entity.getMachineCode())) {
            return null;
        }
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1);
        return cd15MachineInfoMapper.selectOne(wrapper);
    }

    private boolean hasOutOfOpenShift(Cd15MachineRollMapping entity, Cd15MachineInfo machineInfo) {
        Set<String> openShifts = splitShiftCodes(machineInfo.getOpenMachineClass());
        if (openShifts.isEmpty()) {
            return StringUtils.isNotBlank(entity.getShiftCode());
        }
        return !openShifts.containsAll(splitShiftCodes(entity.getShiftCode()));
    }

    private Set<String> splitShiftCodes(String shiftCode) {
        if (StringUtils.isBlank(shiftCode)) {
            return new HashSet<>();
        }
        return Arrays.stream(shiftCode.split(","))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private void normalize(Cd15MachineRollMapping entity) {
        if (entity == null) {
            return;
        }
        entity.setFactoryCode(StringUtils.trimToEmpty(entity.getFactoryCode()));
        entity.setBigRollCode(StringUtils.trimToEmpty(entity.getBigRollCode()));
        entity.setMachineCode(StringUtils.trimToEmpty(entity.getMachineCode()));
        entity.setShiftCode(this.normalizeShiftCode(entity.getShiftCode()));
    }

    private String normalizeShiftCode(String shiftCode) {
        if (StringUtils.isBlank(shiftCode)) {
            return "";
        }
        return Arrays.stream(shiftCode.split(","))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_MACHINE_ROLL_MAPPING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "bigRollCode", "machineCode", "shiftCode");
    }
}
