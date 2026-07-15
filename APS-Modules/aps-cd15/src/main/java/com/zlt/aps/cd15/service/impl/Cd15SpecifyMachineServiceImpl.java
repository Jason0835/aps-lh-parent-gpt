package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.mapper.Cd15SpecifyMachineMapper;
import com.zlt.aps.cd15.service.ICd15SpecifyMachineService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
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
 * 斜裁定点机台业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15SpecifyMachineServiceImpl extends AbstractDocService<Cd15SpecifyMachine> implements ICd15SpecifyMachineService {

    @Resource
    private Cd15SpecifyMachineMapper cd15SpecifyMachineMapper;

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD15_SPECIFY_MACHINE";
    }

    @Override
    public int save(Cd15SpecifyMachine entity) {
        normalize(entity);
        return super.save(entity);
    }

    /**
     * 校验同一工厂、钢带代码、机台下是否已存在配置。
     *
     * @param specifyMachine 定点机台配置
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15SpecifyMachine specifyMachine) {
        normalize(specifyMachine);
        LambdaQueryWrapper<Cd15SpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15SpecifyMachine::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd15SpecifyMachine::getSteelStripCode, specifyMachine.getSteelStripCode());
        wrapper.eq(Cd15SpecifyMachine::getMachineCode, specifyMachine.getMachineCode());
        wrapper.ne(specifyMachine.getId() != null, Cd15SpecifyMachine::getId, specifyMachine.getId());
        return cd15SpecifyMachineMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd15SpecifyMachine> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15SpecifyMachine> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<String> steelStripCodeList = mdmConstructionInfoService.listSteelStripCodes();
        Set<String> steelStripCodes = CollectionUtils.isEmpty(steelStripCodeList)
                ? new HashSet<>()
                : new HashSet<>(steelStripCodeList);
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15SpecifyMachine docEntity = list.get(i);
            normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (!isMachineEnabled(docEntity)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.machineInvalid"), validated);
            }
            if (!isSteelStripCodeExists(docEntity, steelStripCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.steelStripInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15SpecifyMachine docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd15SpecifyMachine exist = getExistSpecifyMachine(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setRemark(docEntity.getRemark());
                cd15SpecifyMachineMapper.updateById(exist);
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

    private Cd15SpecifyMachine getExistSpecifyMachine(Cd15SpecifyMachine specifyMachine) {
        LambdaQueryWrapper<Cd15SpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15SpecifyMachine::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd15SpecifyMachine::getSteelStripCode, specifyMachine.getSteelStripCode());
        wrapper.eq(Cd15SpecifyMachine::getMachineCode, specifyMachine.getMachineCode());
        return cd15SpecifyMachineMapper.selectOne(wrapper);
    }

    private boolean isMachineEnabled(Cd15SpecifyMachine specifyMachine) {
        if (StringUtils.isBlank(specifyMachine.getFactoryCode()) || StringUtils.isBlank(specifyMachine.getMachineCode())) {
            return false;
        }
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, specifyMachine.getMachineCode());
        wrapper.eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1);
        return cd15MachineInfoMapper.selectCount(wrapper) > 0;
    }

    private boolean isSteelStripCodeExists(Cd15SpecifyMachine specifyMachine, Set<String> steelStripCodes) {
        if (StringUtils.isBlank(specifyMachine.getSteelStripCode())) {
            return true;
        }
        return steelStripCodes.contains(specifyMachine.getSteelStripCode());
    }

    private void normalize(Cd15SpecifyMachine specifyMachine) {
        if (specifyMachine == null) {
            return;
        }
        specifyMachine.setFactoryCode(StringUtils.trimToEmpty(specifyMachine.getFactoryCode()));
        specifyMachine.setSteelStripCode(StringUtils.trimToEmpty(specifyMachine.getSteelStripCode()));
        specifyMachine.setMachineCode(StringUtils.trimToEmpty(specifyMachine.getMachineCode()));
        specifyMachine.setLineType(StringUtils.defaultString(specifyMachine.getLineType()));
        specifyMachine.setJobType(StringUtils.defaultString(specifyMachine.getJobType()));
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_SPECIFY_MACHINE");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "steelStripCode", "machineCode");
    }
}
