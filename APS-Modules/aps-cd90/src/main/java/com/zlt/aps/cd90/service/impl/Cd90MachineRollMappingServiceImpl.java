package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.mapper.Cd90MachineInfoMapper;
import com.zlt.aps.cd90.mapper.Cd90MachineRollMappingMapper;
import com.zlt.aps.cd90.service.ICd90MachineRollMappingService;
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
 * 直裁大卷与机台映射业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90MachineRollMappingServiceImpl extends AbstractDocService<Cd90MachineRollMapping> implements ICd90MachineRollMappingService {

    @Resource
    private Cd90MachineRollMappingMapper cd90MachineRollMappingMapper;

    @Resource
    private Cd90MachineInfoMapper cd90MachineInfoMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD90_MACHINE_ROLL_MAPPING";
    }

    @Override
    public int save(Cd90MachineRollMapping entity) {
        normalize(entity);
        return super.save(entity);
    }

    /**
     * 校验同一工厂、钢压大卷、帘布、机台下是否已存在映射。
     *
     * @param entity 大卷与机台映射
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd90MachineRollMapping entity) {
        normalize(entity);
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
        int successNum = 0;
        int failureNum = 0;
        List<Cd90MachineRollMapping> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<String> tireFabricCodeList = mdmConstructionInfoService.listTireFabricCodes();
        Set<String> tireFabricCodes = CollectionUtils.isEmpty(tireFabricCodeList)
                ? new HashSet<>()
                : new HashSet<>(tireFabricCodeList);
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineRollMapping docEntity = list.get(i);
            normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (!isMachineEnabled(docEntity)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd90MachineRollMapping.machineInvalid"), validated);
            }
            if (!isTireFabricCodeExists(docEntity, tireFabricCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd90MachineRollMapping.clothInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90MachineRollMapping docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd90MachineRollMapping exist = getExistMapping(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setRemark(docEntity.getRemark());
                cd90MachineRollMappingMapper.updateById(exist);
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

    private Cd90MachineRollMapping getExistMapping(Cd90MachineRollMapping entity) {
        LambdaQueryWrapper<Cd90MachineRollMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineRollMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineRollMapping::getBigRollCode, entity.getBigRollCode());
        wrapper.eq(Cd90MachineRollMapping::getCordFabricCode, entity.getCordFabricCode());
        wrapper.eq(Cd90MachineRollMapping::getMachineCode, entity.getMachineCode());
        return cd90MachineRollMappingMapper.selectOne(wrapper);
    }

    private boolean isMachineEnabled(Cd90MachineRollMapping entity) {
        if (StringUtils.isBlank(entity.getFactoryCode()) || StringUtils.isBlank(entity.getMachineCode())) {
            return false;
        }
        LambdaQueryWrapper<Cd90MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineInfo::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90MachineInfo::getMachineCode, entity.getMachineCode());
        wrapper.eq(Cd90MachineInfo::getStatus, ApsConstant.APS_STRING_1);
        return cd90MachineInfoMapper.selectCount(wrapper) > 0;
    }

    private boolean isTireFabricCodeExists(Cd90MachineRollMapping entity, Set<String> tireFabricCodes) {
        if (StringUtils.isBlank(entity.getCordFabricCode())) {
            return true;
        }
        return tireFabricCodes.contains(entity.getCordFabricCode());
    }

    private void normalize(Cd90MachineRollMapping entity) {
        if (entity == null) {
            return;
        }
        entity.setFactoryCode(StringUtils.trimToEmpty(entity.getFactoryCode()));
        entity.setBigRollCode(StringUtils.trimToEmpty(entity.getBigRollCode()));
        entity.setCordFabricCode(StringUtils.trimToEmpty(entity.getCordFabricCode()));
        entity.setMachineCode(StringUtils.trimToEmpty(entity.getMachineCode()));
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_MACHINE_ROLL_MAPPING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "bigRollCode", "cordFabricCode", "machineCode");
    }
}
