package com.zlt.aps.xwyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import com.zlt.aps.xwyy.mapper.XwyyStockMapper;
import com.zlt.aps.xwyy.service.IXwyyStockService;
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
public class XwyyStockServiceImpl extends AbstractDocService<XwyyStock> implements IXwyyStockService {

    @Resource
    private XwyyStockMapper xwyyStockMapper;

    @Override
    protected String getDocTypeCode() {
        return "XWYY_STOCK";
    }

    @Override
    public String checkUnique(XwyyStock entity) {
        LambdaQueryWrapper<XwyyStock> w = new LambdaQueryWrapper<>();
        w.eq(XwyyStock::getFactoryCode, entity.getFactoryCode());
        w.eq(XwyyStock::getStockDate, entity.getStockDate());
        w.eq(XwyyStock::getBigRollCode, entity.getBigRollCode());
        w.ne(entity.getId() != null, XwyyStock::getId, entity.getId());
        return xwyyStockMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<XwyyStock> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<XwyyStock> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            XwyyStock de = list.get(i);
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
            XwyyStock de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            XwyyStock ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                ex.setStockNum(de.getStockNum());
                ex.setStockRollNum(de.getStockRollNum());
                ex.setModifyNum(de.getModifyNum());
                ex.setRollModifyNum(de.getRollModifyNum());
                ex.setBadNum(de.getBadNum());
                ex.setRollBadNum(de.getRollBadNum());
                ex.setStockMeters(de.getStockMeters());
                ex.setBigRollBarcode(de.getBigRollBarcode());
                ex.setRemark(de.getRemark());
                xwyyStockMapper.updateById(ex);
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

    private XwyyStock getExist(XwyyStock entity) {
        LambdaQueryWrapper<XwyyStock> w = new LambdaQueryWrapper<>();
        w.eq(XwyyStock::getFactoryCode, entity.getFactoryCode());
        w.eq(XwyyStock::getStockDate, entity.getStockDate());
        w.eq(XwyyStock::getBigRollCode, entity.getBigRollCode());
        return xwyyStockMapper.selectOne(w);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("XWYY_STOCK");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "bigRollCode");
    }
}