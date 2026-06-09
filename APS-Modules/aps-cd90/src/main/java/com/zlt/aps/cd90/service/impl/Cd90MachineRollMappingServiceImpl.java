package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.mapper.Cd90MachineRollMappingMapper;
import com.zlt.aps.cd90.service.ICd90MachineRollMappingService;
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

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90MachineRollMappingServiceImpl extends AbstractDocService<Cd90MachineRollMapping> implements ICd90MachineRollMappingService {

    @Resource
    private Cd90MachineRollMappingMapper cd90MachineRollMappingMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() { return "CD90_MACHINE_ROLL_MAPPING"; }

    @Override
    public String checkUnique(Cd90MachineRollMapping entity) {
        LambdaQueryWrapper<Cd90MachineRollMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineRollMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineRollMapping::getBigRollCode, entity.getBigRollCode());
        wrapper.eq(Cd90MachineRollMapping::getCordFabricCode, entity.getCordFabricCode());
        wrapper.eq(Cd90MachineRollMapping::getMachineCode, entity.getMachineCode());
        wrapper.ne(entity.getId() != null, Cd90MachineRollMapping::getId, entity.getId());
        return cd90MachineRollMappingMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90MachineRollMapping> list, boolean updateSupport, Long importLogId) {
        int successNum = 0, failureNum = 0;
        List<Cd90MachineRollMapping> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        List<String> tireFabricCodeList = mdmConstructionInfoService.listTireFabricCodes();
        Set<String> tireFabricCodes = CollectionUtils.isEmpty(tireFabricCodeList)
                ? new HashSet<>()
                : new HashSet<>(tireFabricCodeList);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineRollMapping docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (!isTireFabricCodeExists(docEntity, tireFabricCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.clothInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) { failureNum++; docEntity.setId(-999L); importErrorLogs.addAll(validated); }
        }
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineRollMapping docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) continue;
            Cd90MachineRollMapping exist = getExistMapping(docEntity);
            if (exist == null) { docEntity.setRowState(RowStateEnum.ADDED); importList.add(docEntity); }
            else if (updateSupport) { exist.setCordFabricCode(docEntity.getCordFabricCode()); exist.setRemark(docEntity.getRemark()); cd90MachineRollMappingMapper.updateById(exist); successNum++; }
            else { failureNum++; ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, String.format(uniqueMsg, errorNum), importErrorLogs); }
        }
        if (PubUtil.isEmpty(importList) && successNum == 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        if (CollectionUtils.isNotEmpty(importList)) successNum += baseDao.saveBatch(importList);
        if (failureNum > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd90MachineRollMapping getExistMapping(Cd90MachineRollMapping entity) {
        LambdaQueryWrapper<Cd90MachineRollMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineRollMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineRollMapping::getBigRollCode, entity.getBigRollCode());
        wrapper.eq(Cd90MachineRollMapping::getCordFabricCode, entity.getCordFabricCode());
        wrapper.eq(Cd90MachineRollMapping::getMachineCode, entity.getMachineCode());
        return cd90MachineRollMappingMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_MACHINE_ROLL_MAPPING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() { return Arrays.asList("factoryCode", "bigRollCode", "cordFabricCode", "machineCode"); }

    private boolean isTireFabricCodeExists(Cd90MachineRollMapping entity, Set<String> tireFabricCodes) {
        if (StringUtils.isBlank(entity.getCordFabricCode())) {
            return false;
        }
        return tireFabricCodes.contains(entity.getCordFabricCode());
    }
}