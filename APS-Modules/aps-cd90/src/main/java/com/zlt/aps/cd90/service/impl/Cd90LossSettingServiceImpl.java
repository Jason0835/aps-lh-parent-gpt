package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.mapper.Cd90LossSettingMapper;
import com.zlt.aps.cd90.service.ICd90LossSettingService;
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
 * 直裁损耗率设定业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90LossSettingServiceImpl extends AbstractDocService<Cd90LossSetting> implements ICd90LossSettingService {

    @Resource
    private Cd90LossSettingMapper cd90LossSettingMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD90_LOSS_SETTING";
    }

    @Override
    public String checkUnique(Cd90LossSetting entity) {
        if (StringUtils.isBlank(entity.getClothCode()) && StringUtils.isBlank(entity.getMachineCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<Cd90LossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90LossSetting::getFactoryCode, entity.getFactoryCode());
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getClothCode())) {
                w.eq(Cd90LossSetting::getClothCode, entity.getClothCode());
            } else {
                w.and(wa -> wa.eq(Cd90LossSetting::getClothCode, "").or().isNull(Cd90LossSetting::getClothCode));
            }
        });
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getMachineCode())) {
                w.eq(Cd90LossSetting::getMachineCode, entity.getMachineCode());
            } else {
                w.and(wa -> wa.eq(Cd90LossSetting::getMachineCode, "").or().isNull(Cd90LossSetting::getMachineCode));
            }
        });
        wrapper.ne(entity.getId() != null, Cd90LossSetting::getId, entity.getId());
        return cd90LossSettingMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90LossSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd90LossSetting> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        List<String> tireFabricCodeList = mdmConstructionInfoService.listTireFabricCodes();
        Set<String> tireFabricCodes = CollectionUtils.isEmpty(tireFabricCodeList)
                ? new HashSet<>()
                : new HashSet<>(tireFabricCodeList);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90LossSetting docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (StringUtils.isBlank(docEntity.getClothCode()) && StringUtils.isBlank(docEntity.getMachineCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.lossSetting.clothOrMachineRequired"), validated);
            }
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (!isTireFabricCodeExists(docEntity, tireFabricCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.clothInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90LossSetting docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (docEntity.getLossRate() == null || docEntity.getLossRate() < 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.lossSetting.lossRateInvalid"), importErrorLogs);
                continue;
            }

            Cd90LossSetting exist = getExistLossSetting(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setLossRate(docEntity.getLossRate());
                exist.setRemark(docEntity.getRemark());
                cd90LossSettingMapper.updateById(exist);
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

    private Cd90LossSetting getExistLossSetting(Cd90LossSetting entity) {
        LambdaQueryWrapper<Cd90LossSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90LossSetting::getFactoryCode, entity.getFactoryCode());
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getClothCode())) {
                w.eq(Cd90LossSetting::getClothCode, entity.getClothCode());
            } else {
                w.and(wa -> wa.eq(Cd90LossSetting::getClothCode, "").or().isNull(Cd90LossSetting::getClothCode));
            }
        });
        wrapper.and(w -> {
            if (StringUtils.isNotBlank(entity.getMachineCode())) {
                w.eq(Cd90LossSetting::getMachineCode, entity.getMachineCode());
            } else {
                w.and(wa -> wa.eq(Cd90LossSetting::getMachineCode, "").or().isNull(Cd90LossSetting::getMachineCode));
            }
        });
        return cd90LossSettingMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_LOSS_SETTING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "clothCode", "machineCode");
    }

    private boolean isTireFabricCodeExists(Cd90LossSetting entity, Set<String> tireFabricCodes) {
        if (StringUtils.isBlank(entity.getClothCode())) {
            return true;
        }
        return tireFabricCodes.contains(entity.getClothCode());
    }
}