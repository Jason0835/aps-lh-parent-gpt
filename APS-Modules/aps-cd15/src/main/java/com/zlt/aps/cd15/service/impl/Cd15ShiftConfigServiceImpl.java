package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.cd15.service.ICd15ShiftConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 班次配置业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ShiftConfigServiceImpl extends AbstractDocService<Cd15ShiftConfig> implements ICd15ShiftConfigService {

    @Resource
    private Cd15ShiftConfigMapper cd15ShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_SHIFT_CONFIG";
    }

    /**
     * 校验同一工厂下班次编码是否唯一。
     *
     * @param shiftConfig 班次配置信息
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15ShiftConfig shiftConfig) {
        LambdaQueryWrapper<Cd15ShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15ShiftConfig::getFactoryCode, shiftConfig.getFactoryCode());
        wrapper.eq(Cd15ShiftConfig::getShiftCode, shiftConfig.getShiftCode());
        wrapper.ne(shiftConfig.getId() != null, Cd15ShiftConfig::getId, shiftConfig.getId());
        return cd15ShiftConfigMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 导入班次配置数据。
     *
     * @param list 导入列表
     * @param updateSupport 是否更新已有数据
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<Cd15ShiftConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15ShiftConfig> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15ShiftConfig docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15ShiftConfig docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            // 校验班次时长大于0
            if (docEntity.getShiftHours() == null || docEntity.getShiftHours() <= 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.cd15ShiftConfig.shiftHoursPositive"), importErrorLogs);
                continue;
            }

            Cd15ShiftConfig exist = getExistShiftConfig(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setShiftName(docEntity.getShiftName());
                exist.setShiftOrder(docEntity.getShiftOrder());
                exist.setStartTime(docEntity.getStartTime());
                exist.setEndTime(docEntity.getEndTime());
                exist.setShiftHours(docEntity.getShiftHours());
                exist.setIsCrossDay(docEntity.getIsCrossDay());
                exist.setScheduleDay(docEntity.getScheduleDay());
                exist.setDayShiftOrder(docEntity.getDayShiftOrder());
                exist.setClassField(docEntity.getClassField());
                exist.setIsActive(docEntity.getIsActive());
                exist.setRemark(docEntity.getRemark());
                cd15ShiftConfigMapper.updateById(exist);
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

    private Cd15ShiftConfig getExistShiftConfig(Cd15ShiftConfig shiftConfig) {
        LambdaQueryWrapper<Cd15ShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15ShiftConfig::getFactoryCode, shiftConfig.getFactoryCode());
        wrapper.eq(Cd15ShiftConfig::getShiftCode, shiftConfig.getShiftCode());
        return cd15ShiftConfigMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_SHIFT_CONFIG");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "shiftCode");
    }
}