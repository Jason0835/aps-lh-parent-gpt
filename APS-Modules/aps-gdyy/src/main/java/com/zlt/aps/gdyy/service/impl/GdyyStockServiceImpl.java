package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.mapper.GdyyStockMapper;
import com.zlt.aps.gdyy.service.IGdyyStockService;
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
 * 钢带压延库存 服务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class GdyyStockServiceImpl extends AbstractDocService<GdyyStock> implements IGdyyStockService {

    @Resource
    private GdyyStockMapper gdyyStockMapper;

    @Override
    protected String getDocTypeCode() {
        return "GDYY_STOCK";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("GDYY_STOCK");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "bigRollCode");
    }

    @Override
    public String checkUnique(GdyyStock entity) {
        LambdaQueryWrapper<GdyyStock> w = new LambdaQueryWrapper<>();
        w.eq(GdyyStock::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyStock::getStockDate, entity.getStockDate());
        w.eq(GdyyStock::getBigRollCode, entity.getBigRollCode());
        w.ne(entity.getId() != null, GdyyStock::getId, entity.getId());
        return gdyyStockMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<GdyyStock> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<GdyyStock> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            GdyyStock de = list.get(i);
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
            GdyyStock de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            GdyyStock ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                ex.setBigRollBarcode(de.getBigRollBarcode());
                ex.setInboundTime(de.getInboundTime());
                ex.setStockNum(de.getStockNum());
                ex.setStockRollNum(de.getStockRollNum());
                ex.setStockMeters(de.getStockMeters());
                ex.setModifyNum(de.getModifyNum());
                ex.setBadNum(de.getBadNum());
                ex.setEstimateStockFlag(de.getEstimateStockFlag());
                ex.setRemark(de.getRemark());
                gdyyStockMapper.updateById(ex);
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

    private GdyyStock getExist(GdyyStock entity) {
        LambdaQueryWrapper<GdyyStock> w = new LambdaQueryWrapper<>();
        w.eq(GdyyStock::getFactoryCode, entity.getFactoryCode());
        w.eq(GdyyStock::getStockDate, entity.getStockDate());
        w.eq(GdyyStock::getBigRollCode, entity.getBigRollCode());
        return gdyyStockMapper.selectOne(w);
    }
}
