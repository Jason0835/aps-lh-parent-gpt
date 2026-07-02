package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.mapper.Cd90StockMapper;
import com.zlt.aps.cd90.service.ICd90StockService;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90StockServiceImpl extends AbstractDocService<Cd90Stock> implements ICd90StockService {

    @Resource
    private Cd90StockMapper cd90StockMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD90_STOCK";
    }

    @Override
    public String checkUnique(Cd90Stock entity) {
        LambdaQueryWrapper<Cd90Stock> w = new LambdaQueryWrapper<>();
        w.eq(Cd90Stock::getFactoryCode, entity.getFactoryCode());
        w.eq(Cd90Stock::getStockDate, entity.getStockDate());
        w.eq(Cd90Stock::getShiftCode, entity.getShiftCode());
        w.eq(Cd90Stock::getMaterialCode, entity.getMaterialCode());
        w.ne(entity.getId() != null, Cd90Stock::getId, entity.getId());
        return cd90StockMapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90Stock> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<Cd90Stock> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");
        List<String> tireFabricCodeList = mdmConstructionInfoService.listTireFabricCodes();
        Set<String> tireFabricCodes = CollectionUtils.isEmpty(tireFabricCodeList)
                ? new HashSet<>()
                : new HashSet<>(tireFabricCodeList);

        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            Cd90Stock de = list.get(i);
            List<ImportErrorLog> v = ImportExcelValidatedUtils.validated(importLogId, en, de);
            ImportExcelValidatedUtils.validatedRepeat(list, de, i, 2, importLogId, v,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (!isTireFabricCodeExists(de, tireFabricCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        en, I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.clothInvalid"), v);
            }
            if (CollectionUtils.isNotEmpty(v)) {
                fn++;
                de.setId(-999L);
                el.addAll(v);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            Cd90Stock de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            Cd90Stock ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                ex.setSnapshotTime(de.getSnapshotTime());
                ex.setStockNum(de.getStockNum());
                ex.setModifyNum(de.getModifyNum());
                ex.setBadNum(de.getBadNum());
                ex.setRemark(de.getRemark());
                cd90StockMapper.updateById(ex);
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

    private boolean isTireFabricCodeExists(Cd90Stock entity, Set<String> tireFabricCodes) {
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            return true;
        }
        return tireFabricCodes.contains(entity.getMaterialCode());
    }

    private Cd90Stock getExist(Cd90Stock entity) {
        LambdaQueryWrapper<Cd90Stock> w = new LambdaQueryWrapper<>();
        w.eq(Cd90Stock::getFactoryCode, entity.getFactoryCode());
        w.eq(Cd90Stock::getStockDate, entity.getStockDate());
        w.eq(Cd90Stock::getShiftCode, entity.getShiftCode());
        w.eq(Cd90Stock::getMaterialCode, entity.getMaterialCode());
        return cd90StockMapper.selectOne(w);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("CD90_STOCK");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "shiftCode", "materialCode");
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, String dataSource, Date stockDate, String shiftCode,
                                        String updateBy, List<Cd90Stock> insertList) {
        log.info("直裁库存同步-事务开始：逻辑删除分厂{}数据来源{}库存日期{}班次{}的旧数据，待插入数量={}",
                factoryCode, dataSource, stockDate, shiftCode, CollectionUtils.size(insertList));
        cd90StockMapper.logicDeleteByFactoryCodeAndDataSourceAndShiftCode(factoryCode, dataSource, stockDate, shiftCode, updateBy, new Date());
        log.info("直裁库存同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<Cd90Stock> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("直裁库存同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("直裁库存同步-事务完成：分厂{}，库存日期={}，班次={}，插入数量={}",
                factoryCode, stockDate, shiftCode, CollectionUtils.size(insertList));
    }
}