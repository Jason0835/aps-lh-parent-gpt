package com.zlt.aps.xwyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import com.zlt.aps.xwyy.mapper.XwyyShiftConfigMapper;
import com.zlt.aps.xwyy.service.IXwyyShiftConfigService;
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

@Service
@Transactional(rollbackFor = Exception.class)
public class XwyyShiftConfigServiceImpl extends AbstractDocService<XwyyShiftConfig> implements IXwyyShiftConfigService {

    @Resource
    private XwyyShiftConfigMapper xwyyShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "XWYY_SHIFT_CONFIG";
    }

    @Override
    public String checkUnique(XwyyShiftConfig entity) {
        LambdaQueryWrapper<XwyyShiftConfig> w = new LambdaQueryWrapper<>();
        w.eq(XwyyShiftConfig::getFactoryCode, entity.getFactoryCode());
        w.eq(XwyyShiftConfig::getShiftCode, entity.getShiftCode());
        w.ne(entity.getId() != null, XwyyShiftConfig::getId, entity.getId());
        return xwyyShiftConfigMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult changeStatus(XwyyShiftConfig entity) {
        if (entity.getId() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.required.id"));
        }
        XwyyShiftConfig update = new XwyyShiftConfig();
        update.setId(entity.getId());
        update.setIsActive(entity.getIsActive());
        return AjaxResult.success(xwyyShiftConfigMapper.updateById(update));
    }

    @Override
    public AjaxResult importData(List<XwyyShiftConfig> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<XwyyShiftConfig> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            XwyyShiftConfig de = list.get(i);
            List<ImportErrorLog> v = ImportExcelValidatedUtils.validated(importLogId, en, de);
            ImportExcelValidatedUtils.validatedRepeat(list, de, i, 2, importLogId, v,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(v)) {
                fn++;
                de.setId(-999L);
                el.addAll(v);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            XwyyShiftConfig de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            XwyyShiftConfig ex = getExist(de);
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
                xwyyShiftConfigMapper.updateById(ex);
                sn++;
            } else {
                fn++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, en, String.format(um, en), el);
            }
        }
        if (PubUtil.isEmpty(il) && sn == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        }
        if (CollectionUtils.isNotEmpty(il)) sn += baseDao.saveBatch(il);
        if (fn > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + sn);
    }

    private XwyyShiftConfig getExist(XwyyShiftConfig entity) {
        LambdaQueryWrapper<XwyyShiftConfig> w = new LambdaQueryWrapper<>();
        w.eq(XwyyShiftConfig::getFactoryCode, entity.getFactoryCode());
        w.eq(XwyyShiftConfig::getShiftCode, entity.getShiftCode());
        return xwyyShiftConfigMapper.selectOne(w);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("XWYY_SHIFT_CONFIG");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "shiftCode");
    }
}
