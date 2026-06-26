package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyShiftConfig;
import com.zlt.aps.gdyy.mapper.GdyyShiftConfigMapper;
import com.zlt.aps.gdyy.service.IGdyyShiftConfigService;
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
 * 钢带压延班次配置 服务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class GdyyShiftConfigServiceImpl extends AbstractDocService<GdyyShiftConfig> implements IGdyyShiftConfigService {

    @Resource
    private GdyyShiftConfigMapper gdyyShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "GDYY_SHIFT_CONFIG";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("GDYY_SHIFT_CONFIG");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "shiftCode");
    }

    @Override
    public String checkUnique(GdyyShiftConfig entity) {
        LambdaQueryWrapper<GdyyShiftConfig> w = new LambdaQueryWrapper<>();
        w.eq(GdyyShiftConfig::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyShiftConfig::getShiftCode, entity.getShiftCode());
        w.ne(entity.getId() != null, GdyyShiftConfig::getId, entity.getId());
        return gdyyShiftConfigMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<GdyyShiftConfig> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<GdyyShiftConfig> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            GdyyShiftConfig de = list.get(i);
            List<ImportErrorLog> v = ImportExcelValidatedUtils.validated(importLogId, en, de);
            ImportExcelValidatedUtils.validatedRepeat(list, de, i, 2, importLogId, v,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (de.getShiftHours() != null && de.getShiftHours() <= 0) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        en, I18nUtil.getMessage("ui.data.column.gdyyShiftConfig.shiftHoursInvalid"), v);
            }
            if (CollectionUtils.isNotEmpty(v)) {
                fn++;
                de.setId(-999L);
                el.addAll(v);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            GdyyShiftConfig de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            GdyyShiftConfig ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                ex.setShiftName(de.getShiftName());
                ex.setShiftOrder(de.getShiftOrder());
                ex.setStartTime(de.getStartTime());
                ex.setEndTime(de.getEndTime());
                ex.setShiftHours(de.getShiftHours());
                ex.setIsCrossDay(de.getIsCrossDay());
                ex.setScheduleDay(de.getScheduleDay());
                ex.setDayShiftOrder(de.getDayShiftOrder());
                ex.setClassField(de.getClassField());
                ex.setIsActive(de.getIsActive());
                ex.setRemark(de.getRemark());
                gdyyShiftConfigMapper.updateById(ex);
                sn++;
            } else {
                fn++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, en, String.format(um, en), el);
            }
        }

        if (PubUtil.isEmpty(il) && sn == 0)
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        if (CollectionUtils.isNotEmpty(il)) sn += baseDao.saveBatch(il);
        if (fn > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + sn);
    }

    private GdyyShiftConfig getExist(GdyyShiftConfig entity) {
        LambdaQueryWrapper<GdyyShiftConfig> w = new LambdaQueryWrapper<>();
        w.eq(GdyyShiftConfig::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyShiftConfig::getShiftCode, entity.getShiftCode());
        return gdyyShiftConfigMapper.selectOne(w);
    }
}
