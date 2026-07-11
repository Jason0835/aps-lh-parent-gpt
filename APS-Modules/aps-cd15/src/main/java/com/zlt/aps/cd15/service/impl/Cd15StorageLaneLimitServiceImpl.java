package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.mapper.Cd15StorageLaneLimitMapper;
import com.zlt.aps.cd15.service.ICd15StorageLaneLimitService;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
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

/**
 * 斜裁库排限制 Service 实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15StorageLaneLimitServiceImpl extends AbstractDocService<Cd15StorageLaneLimit> implements ICd15StorageLaneLimitService {

    @Resource
    private Cd15StorageLaneLimitMapper cd15StorageLaneLimitMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD15_STORAGE_LANE_LIMIT";
    }

    @Override
    public int save(Cd15StorageLaneLimit entity) {
        this.normalize(entity);
        this.fillDerivedFields(entity);
        return super.save(entity);
    }

    @Override
    public String checkUnique(Cd15StorageLaneLimit entity) {
        this.normalize(entity);
        LambdaQueryWrapper<Cd15StorageLaneLimit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15StorageLaneLimit::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15StorageLaneLimit::getLaneDate, entity.getLaneDate());
        wrapper.eq(Cd15StorageLaneLimit::getShiftCode, entity.getShiftCode());
        wrapper.eq(Cd15StorageLaneLimit::getStorageLaneCode, entity.getStorageLaneCode());
        wrapper.ne(entity.getId() != null, Cd15StorageLaneLimit::getId, entity.getId());
        return cd15StorageLaneLimitMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public String validateBusiness(Cd15StorageLaneLimit entity) {
        this.normalize(entity);
        this.fillDerivedFields(entity);
        if (entity.getCarNum() == null || entity.getCarNum() < 0) {
            return "ui.data.column.cd15StorageLaneLimit.carNumNonNegative";
        }
        if (entity.getMaxCarNum() == null || entity.getMaxCarNum() <= 0) {
            return "ui.data.column.cd15StorageLaneLimit.maxCarNumPositive";
        }
        if (entity.getCarNum() > entity.getMaxCarNum()) {
            return "ui.data.column.cd15StorageLaneLimit.carNumExceedMax";
        }
        if (StringUtils.isBlank(entity.getMaterialCode()) && entity.getCarNum() != 0) {
            return "ui.data.column.cd15StorageLaneLimit.emptyLaneCarNumZero";
        }
        if (StringUtils.isNotBlank(entity.getMaterialCode()) && !this.isSteelStripCodeExists(entity.getMaterialCode())) {
            return "ui.data.column.cd15StorageLaneLimit.materialCodeInvalid";
        }
        return null;
    }

    @Override
    public AjaxResult importData(List<Cd15StorageLaneLimit> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15StorageLaneLimit> insertList = new ArrayList<>();
        List<ImportErrorLog> errorList = new ArrayList<>();
        Set<String> steelStripCodes = this.loadSteelStripCodes();
        String uniqueMessage = I18nUtil.getMessage("import.validated.unique");

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd15StorageLaneLimit importEntity = list.get(index);
            this.normalize(importEntity);
            this.fillDerivedFields(importEntity);
            List<ImportErrorLog> validateList = ImportExcelValidatedUtils.validated(importLogId, rowNum, importEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, importEntity, index, 2, importLogId, validateList,
                    this.getCheckUniqueFields().toArray(new String[0]));
            this.addBusinessImportErrors(importLogId, rowNum, importEntity, validateList, steelStripCodes);
            if (CollectionUtils.isNotEmpty(validateList)) {
                failureNum++;
                importEntity.setId(-999L);
                errorList.addAll(validateList);
            }
        }

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd15StorageLaneLimit importEntity = list.get(index);
            if (importEntity.getId() != null && importEntity.getId() == -999L) {
                continue;
            }
            Cd15StorageLaneLimit existEntity = this.getExist(importEntity);
            if (existEntity == null) {
                importEntity.setRowState(RowStateEnum.ADDED);
                insertList.add(importEntity);
            } else if (updateSupport) {
                this.copyImportValues(existEntity, importEntity);
                cd15StorageLaneLimitMapper.updateById(existEntity);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowNum,
                        MessageFormat.format(uniqueMessage, rowNum), errorList);
            }
        }

        if (PubUtil.isEmpty(insertList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, errorList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            successNum += baseDao.saveBatch(insertList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, errorList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "laneDate", "shiftCode", "storageLaneCode");
    }

    private void addBusinessImportErrors(Long importLogId, int rowNum, Cd15StorageLaneLimit entity,
                                         List<ImportErrorLog> validateList, Set<String> steelStripCodes) {
        if (entity.getCarNum() == null || entity.getCarNum() < 0) {
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    rowNum, I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.carNumNonNegative"), validateList);
        }
        if (entity.getMaxCarNum() == null || entity.getMaxCarNum() <= 0) {
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    rowNum, I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.maxCarNumPositive"), validateList);
        }
        if (entity.getCarNum() != null && entity.getMaxCarNum() != null && entity.getCarNum() > entity.getMaxCarNum()) {
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    rowNum, I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.carNumExceedMax"), validateList);
        }
        if (StringUtils.isBlank(entity.getMaterialCode()) && entity.getCarNum() != null && entity.getCarNum() != 0) {
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    rowNum, I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.emptyLaneCarNumZero"), validateList);
        }
        if (StringUtils.isNotBlank(entity.getMaterialCode()) && !steelStripCodes.contains(entity.getMaterialCode())) {
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    rowNum, I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.materialCodeInvalid"), validateList);
        }
    }

    private Cd15StorageLaneLimit getExist(Cd15StorageLaneLimit entity) {
        LambdaQueryWrapper<Cd15StorageLaneLimit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15StorageLaneLimit::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15StorageLaneLimit::getLaneDate, entity.getLaneDate());
        wrapper.eq(Cd15StorageLaneLimit::getShiftCode, entity.getShiftCode());
        wrapper.eq(Cd15StorageLaneLimit::getStorageLaneCode, entity.getStorageLaneCode());
        return cd15StorageLaneLimitMapper.selectOne(wrapper);
    }

    private void copyImportValues(Cd15StorageLaneLimit target, Cd15StorageLaneLimit source) {
        target.setMaterialCode(source.getMaterialCode());
        target.setCarNum(source.getCarNum());
        target.setMaxCarNum(source.getMaxCarNum());
        target.setAvailableCarNum(source.getAvailableCarNum());
        target.setDataSource(source.getDataSource());
        target.setMesSyncTime(source.getMesSyncTime());
        target.setRemark(source.getRemark());
    }

    private boolean isSteelStripCodeExists(String materialCode) {
        if (StringUtils.isBlank(materialCode)) {
            return true;
        }
        return this.loadSteelStripCodes().contains(materialCode);
    }

    private Set<String> loadSteelStripCodes() {
        List<String> steelStripCodeList = mdmConstructionInfoService.listSteelStripCodes();
        return CollectionUtils.isEmpty(steelStripCodeList) ? new HashSet<>() : new HashSet<>(steelStripCodeList);
    }

    private void normalize(Cd15StorageLaneLimit entity) {
        if (entity == null) {
            return;
        }
        entity.setFactoryCode(StringUtils.trimToEmpty(entity.getFactoryCode()));
        entity.setShiftCode(StringUtils.trimToEmpty(entity.getShiftCode()));
        entity.setStorageLaneCode(StringUtils.trimToEmpty(entity.getStorageLaneCode()));
        entity.setMaterialCode(StringUtils.trimToNull(entity.getMaterialCode()));
        entity.setDataSource(StringUtils.trimToNull(entity.getDataSource()));
        entity.setRemark(StringUtils.trimToNull(entity.getRemark()));
    }

    private void fillDerivedFields(Cd15StorageLaneLimit entity) {
        if (entity == null) {
            return;
        }
        if (entity.getCarNum() == null) {
            entity.setCarNum(0);
        }
        if (entity.getMaxCarNum() != null && entity.getCarNum() != null) {
            entity.setAvailableCarNum(entity.getMaxCarNum() - entity.getCarNum());
        }
    }
}